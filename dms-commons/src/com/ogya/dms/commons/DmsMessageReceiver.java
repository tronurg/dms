package com.ogya.dms.commons;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import com.ogya.dms.commons.structures.ContentType;
import com.ogya.dms.commons.structures.MessagePojo;

public class DmsMessageReceiver {

	private final DmsMessageReceiverListener listener;
	private final Map<Integer, AttachmentReceiver> attachmentReceivers = new HashMap<Integer, AttachmentReceiver>();

	public DmsMessageReceiver(DmsMessageReceiverListener listener) {
		this.listener = listener;
	}

	public void inFeed(int messageNumber, byte[] data) {

		if (data.length == 0) {
			cancel(messageNumber);
			return;
		}

		ByteBuffer dataBuffer = ByteBuffer.wrap(data);
		byte sign = dataBuffer.get();

		if (sign < 0) {
			cancel(messageNumber);
			try {
				MessagePojo messagePojo = DmsPackingFactory.unpack(dataBuffer, MessagePojo.class);
				if (messagePojo.attachment == null) {
					listener.messageReceived(messagePojo);
				} else if (messagePojo.attachment.size == 0) {
					messagePojo.attachment.path = Files.createTempFile("dms", null);
					listener.messageReceived(messagePojo);
				} else {
					attachmentReceivers.put(messageNumber, new AttachmentReceiver(messagePojo));
				}
			} catch (Exception e) {

			}
			return;
		}

		AttachmentReceiver attachmentReceiver = attachmentReceivers.get(messageNumber);
		if (attachmentReceiver == null) {
			listener.messageFailed(messageNumber);
			return;
		}
		dataBuffer.rewind();
		boolean attachmentReady = attachmentReceiver.dataReceived(dataBuffer);
		if (attachmentReady) {
			attachmentReceivers.remove(messageNumber);
			MessagePojo messagePojo = attachmentReceiver.messagePojo;
			if (messagePojo != null) {
				listener.messageReceived(messagePojo);
			}
		}

	}

	private void cancel(int messageNumber) {
		AttachmentReceiver attachmentReceiver = attachmentReceivers.remove(messageNumber);
		if (attachmentReceiver == null) {
			return;
		}
		attachmentReceiver.cancel();
	}

	public void deleteResourcesKeepDownloads() {
		attachmentReceivers.forEach((messageNumber, attachmentReceiver) -> {
			MessagePojo messagePojo = attachmentReceiver.messagePojo;
			if (messagePojo == null || messagePojo.contentType != ContentType.UPLOAD) {
				attachmentReceiver.cancel();
				return;
			}
			attachmentReceiver.interrupt();
			messagePojo.contentType = ContentType.UPLOAD_PART;
			listener.messageReceived(messagePojo);
		});
		attachmentReceivers.clear();
	}

	public void deleteResources() {
		attachmentReceivers.forEach((messageNumber, attachmentReceiver) -> attachmentReceiver.cancel());
		attachmentReceivers.clear();
	}

	private final class AttachmentReceiver {

		private MessagePojo messagePojo;
		private FileChannel fileChannel;
		private boolean interrupted = false;
		private long fileSize;
		private long currentSize = 0;
		private int downloadProgress = -1;

		private AttachmentReceiver(MessagePojo messagePojo) {
			this.messagePojo = messagePojo;
			this.fileSize = messagePojo.attachment.size;
			try {
				messagePojo.attachment.path = Files.createTempFile("dms", null);
				fileChannel = FileChannel.open(messagePojo.attachment.path, StandardOpenOption.CREATE,
						StandardOpenOption.WRITE);
				checkDownloadProgress();
			} catch (Exception e) {
				cancel();
			}
		}

		private void interrupt() {

			interrupted = true;

			try {
				fileChannel.close();
			} catch (Exception e) {

			}

		}

		private void cancel() {

			interrupt();

			try {
				Files.deleteIfExists(messagePojo.attachment.path);
			} catch (Exception e) {

			}

			messagePojo = null;

		}

		private boolean dataReceived(ByteBuffer dataBuffer) {

			if (interrupted) {
				return true;
			}

			try {
				long position = dataBuffer.getLong();
				int dataLength = dataBuffer.remaining();
				currentSize = position + dataLength;
				fileChannel.write(dataBuffer, position);
				checkDownloadProgress();
				boolean done = !(currentSize < fileSize);
				if (done) {
					fileChannel.force(true);
					fileChannel.close();
				}
				return done;
			} catch (Exception e) {
				String receiverUuid = messagePojo.receiverUuid;
				ContentType contentType = messagePojo.contentType;
				Long trackingId = messagePojo.trackingId;
				cancel();
				if (contentType == ContentType.UPLOAD) {
					messagePojo = new MessagePojo(null, null, receiverUuid, ContentType.UPLOAD_FAILURE, trackingId,
							null, null);
				}
			}

			return true;

		}

		private void checkDownloadProgress() {
			if (messagePojo.contentType != ContentType.UPLOAD) {
				return;
			}
			int progress = (int) (100.0 * currentSize / fileSize);
			if (downloadProgress < progress) {
				downloadProgress = progress;
				listener.downloadProgress(messagePojo.receiverUuid, messagePojo.trackingId, downloadProgress);
			}
		}

	}

	public static interface DmsMessageReceiverListener {

		void messageReceived(MessagePojo messagePojo);

		void messageFailed(int messageNumber);

		void downloadProgress(String receiverUuid, Long trackingId, int progress);

	}

}
