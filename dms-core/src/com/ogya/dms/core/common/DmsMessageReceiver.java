package com.ogya.dms.core.common;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
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

		int sign = Integer.signum(messageNumber);
		int absMessageNumber = Math.abs(messageNumber);
		ByteBuffer messageBuffer = ByteBuffer.wrap(data);

		if (!messageBuffer.hasRemaining()) {
			interrupt(absMessageNumber, keepDownloads);
			return;
		}

		if (messageBuffer.get() < 0) {
			interrupt(absMessageNumber, false);
			try {
				MessagePojo messagePojo = DmsPackingFactory.unpack(messageBuffer, MessagePojo.class);
				if (sign == 0) {
					listener.messageReceived(messagePojo, null, false);
				} else if (sign < 0) {
					Path path = Files.createTempFile("dms", null);
					listener.messageReceived(messagePojo, path, false);
				} else {
					attachmentReceivers.put(absMessageNumber, new AttachmentReceiver(messagePojo));
				}
			} catch (Exception e) {

			}
			return;
		}

		AttachmentReceiver attachmentReceiver = attachmentReceivers.get(absMessageNumber);
		if (attachmentReceiver == null) {
			listener.messageFailed(absMessageNumber);
			return;
		}
		messageBuffer.rewind();
		boolean attachmentReady = attachmentReceiver.dataReceived(messageBuffer, sign < 0);
		if (attachmentReady) {
			attachmentReceivers.remove(absMessageNumber);
			MessagePojo messagePojo = attachmentReceiver.messagePojo;
			if (messagePojo != null) {
				listener.messageReceived(messagePojo, attachmentReceiver.path, attachmentReceiver.partial);
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
		private Long globalSize;
		private Path path;
		private FileChannel fileChannel;

		private boolean interrupted = false;
		private long currentSize = 0;
		private int downloadProgress = -1;
		private boolean partial = false;

		private AttachmentReceiver(MessagePojo messagePojo) {
			try {
				this.messagePojo = messagePojo;
				this.globalSize = messagePojo.globalSize;
				path = Files.createTempFile("dms", null);
				fileChannel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
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

			if (messagePojo == null || path == null) {
				return;
			}

			if (keepDownload && messagePojo.contentType == ContentType.UPLOAD) {
				partial = true;
				return;
			}

			try {
				Files.deleteIfExists(path);
			} catch (Exception e) {

			}

			messagePojo = null;

		}

		private boolean dataReceived(ByteBuffer dataBuffer, boolean done) {

			if (interrupted) {
				return true;
			}

			try {
				long position = dataBuffer.getLong();
				int dataLength = dataBuffer.remaining();
				currentSize = position + dataLength;
				fileChannel.write(dataBuffer, position);
				checkDownloadProgress();
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

		void messageReceived(MessagePojo messagePojo, Path attachment, boolean partial);

		void messageFailed(int messageNumber);

		void downloadProgress(Long trackingId, int progress);

		void downloadFailed(Long trackingId);

	}

}
