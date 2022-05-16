package com.ogya.dms.core.dmsclient;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import com.ogya.dms.commons.DmsPackingFactory;
import com.ogya.dms.commons.structures.ContentType;
import com.ogya.dms.commons.structures.MessagePojo;

public class DmsMessageReceiver {

	private final DmsMessageReceiverListener listener;
	private final Map<Integer, AttachmentReceiver> attachmentReceivers = new HashMap<Integer, AttachmentReceiver>();

	private boolean keepDownloads;

	public DmsMessageReceiver(DmsMessageReceiverListener listener) {
		this.listener = listener;
	}

	public void setKeepDownloads(boolean keepDownloads) {
		this.keepDownloads = keepDownloads;
	}

	public void inFeed(int messageNumber, byte[] data) {

		messageNumber = Math.abs(messageNumber);

		if (data.length == 0) {
			interrupt(messageNumber, keepDownloads);
			return;
		}

		ByteBuffer dataBuffer = ByteBuffer.wrap(data);
		byte sign = dataBuffer.get();

		if (sign < 0) {
			interrupt(messageNumber, false);
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

	private void interrupt(int messageNumber, boolean keepDownload) {
		AttachmentReceiver attachmentReceiver = attachmentReceivers.remove(messageNumber);
		if (attachmentReceiver == null) {
			return;
		}
		attachmentReceiver.interrupt(keepDownload);
	}

	public void interruptAll() {
		attachmentReceivers.forEach((messageNumber, attachmentReceiver) -> attachmentReceiver.interrupt(keepDownloads));
		attachmentReceivers.clear();
	}

	private final class AttachmentReceiver {

		private MessagePojo messagePojo;
		private FileChannel fileChannel;
		private boolean interrupted = false;
		private long fileSize;
		private Long globalSize;
		private long currentSize = 0;
		private int downloadProgress = -1;

		private AttachmentReceiver(MessagePojo messagePojo) {
			try {
				this.messagePojo = messagePojo;
				this.fileSize = messagePojo.attachment.size;
				this.globalSize = messagePojo.attachment.globalSize;
				messagePojo.attachment.path = Files.createTempFile("dms", null);
				fileChannel = FileChannel.open(messagePojo.attachment.path, StandardOpenOption.CREATE,
						StandardOpenOption.WRITE);
				checkDownloadProgress();
			} catch (Exception e) {
				interrupt(false);
			}
		}

		private void interrupt(boolean keepDownload) {

			interrupted = true;

			try {
				fileChannel.close();
			} catch (Exception e) {

			}

			if (messagePojo == null || messagePojo.attachment == null) {
				return;
			}

			if (keepDownload && messagePojo.contentType == ContentType.UPLOAD) {
				messagePojo.attachment.partial = true;
				return;
			}

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
				ContentType contentType = messagePojo.contentType;
				Long trackingId = messagePojo.trackingId;
				interrupt(false);
				if (contentType == ContentType.UPLOAD) {
					listener.downloadFailed(trackingId);
				}
			}

			return true;

		}

		private void checkDownloadProgress() {
			if (globalSize == null || !(globalSize > 0)) {
				return;
			}
			int progress = (int) (100.0 * currentSize / globalSize);
			if (downloadProgress < progress) {
				downloadProgress = progress;
				listener.downloadProgress(messagePojo.trackingId, downloadProgress);
			}
		}

	}

	public static interface DmsMessageReceiverListener {

		void messageReceived(MessagePojo messagePojo);

		void messageFailed(int messageNumber);

		void downloadProgress(Long trackingId, int progress);

		void downloadFailed(Long trackingId);

	}

}
