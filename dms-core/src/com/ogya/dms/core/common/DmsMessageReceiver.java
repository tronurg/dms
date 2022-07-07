package com.ogya.dms.core.common;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.ogya.dms.commons.DmsPackingFactory;
import com.ogya.dms.commons.structures.ContentType;
import com.ogya.dms.commons.structures.MessagePojo;

public class DmsMessageReceiver {

	private final DmsMessageReceiverListener listener;
	private final Map<Integer, AttachmentReceiver> attachmentReceivers = new HashMap<Integer, AttachmentReceiver>();

	public DmsMessageReceiver(DmsMessageReceiverListener listener) {
		this.listener = listener;
	}

	public void inFeed(int messageNumber, byte[] data) {

		int sign = Integer.signum(messageNumber);
		int absMessageNumber = Math.abs(messageNumber);
		ByteBuffer messageBuffer = ByteBuffer.wrap(data);

		try {
			inFeed(sign, absMessageNumber, messageBuffer);
		} catch (Exception e) {
			destroy(absMessageNumber);
			if (sign > 0) {
				listener.cannotReceiveMore(absMessageNumber);
			}
		}

	}

	public void interruptDownload(Long downloadId) {
		if (downloadId == null) {
			return;
		}
		List<Integer> messageNumbersToRemove = new ArrayList<Integer>();
		attachmentReceivers.forEach((messageNumber, attachmentReceiver) -> {
			MessagePojo messagePojo = attachmentReceiver.messagePojo;
			if (messagePojo == null || messagePojo.contentType != ContentType.UPLOAD
					|| !downloadId.equals(messagePojo.trackingId)) {
				return;
			}
			messageNumbersToRemove.add(messageNumber);
			attachmentReceiver.interrupt();
			listener.messageReceived(messagePojo, attachmentReceiver.path, true);
		});
		messageNumbersToRemove.forEach(messageNumber -> attachmentReceivers.remove(messageNumber));
	}

	public void closeMessagesFrom(String senderUuid) {
		if (senderUuid == null) {
			return;
		}
		List<Integer> messageNumbersToRemove = new ArrayList<Integer>();
		attachmentReceivers.forEach((messageNumber, attachmentReceiver) -> {
			MessagePojo messagePojo = attachmentReceiver.messagePojo;
			if (messagePojo == null || !senderUuid.equals(messagePojo.senderUuid)) {
				return;
			}
			messageNumbersToRemove.add(messageNumber);
			if (messagePojo.contentType == ContentType.UPLOAD) {
				attachmentReceiver.interrupt();
				listener.messageReceived(messagePojo, attachmentReceiver.path, true);
			} else {
				attachmentReceiver.destroy();
			}
		});
		messageNumbersToRemove.forEach(messageNumber -> attachmentReceivers.remove(messageNumber));
	}

	public void close() {
		attachmentReceivers.forEach((messageNumber, attachmentReceiver) -> {
			MessagePojo messagePojo = attachmentReceiver.messagePojo;
			if (messagePojo != null && messagePojo.contentType == ContentType.UPLOAD) {
				attachmentReceiver.interrupt();
				listener.messageReceived(messagePojo, attachmentReceiver.path, true);
			} else {
				attachmentReceiver.destroy();
			}
		});
		attachmentReceivers.clear();
	}

	private void inFeed(int sign, int absMessageNumber, ByteBuffer messageBuffer) throws Exception {

		if (!messageBuffer.hasRemaining()) {
			destroy(absMessageNumber);
			return;
		}

		if (messageBuffer.get() < 0) {
			destroy(absMessageNumber);
			MessagePojo messagePojo = DmsPackingFactory.unpack(messageBuffer, MessagePojo.class);
			if (sign > 0) {
				attachmentReceivers.put(absMessageNumber, new AttachmentReceiver(messagePojo));
			} else if (messagePojo.contentType == ContentType.UPLOAD && Objects.equals(messagePojo.globalSize, 0L)) {
				listener.messageReceived(messagePojo, Files.createTempFile("dms", null), false);
			} else {
				listener.messageReceived(messagePojo, null, false);
			}
			return;
		}

		AttachmentReceiver attachmentReceiver = attachmentReceivers.get(absMessageNumber);
		if (attachmentReceiver == null) {
			return;
		}
		messageBuffer.rewind();
		boolean hasMore = sign > 0;
		attachmentReceiver.dataReceived(messageBuffer, hasMore);
		if (hasMore) {
			return;
		}
		attachmentReceivers.remove(absMessageNumber);
		MessagePojo messagePojo = attachmentReceiver.messagePojo;
		if (messagePojo != null) {
			listener.messageReceived(messagePojo, attachmentReceiver.path, false);
		}

	}

	private void destroy(int messageNumber) {
		AttachmentReceiver attachmentReceiver = attachmentReceivers.remove(messageNumber);
		if (attachmentReceiver == null) {
			return;
		}
		attachmentReceiver.destroy();
	}

	private final class AttachmentReceiver {

		private MessagePojo messagePojo;
		private Long globalSize;
		private Path path;
		private FileChannel fileChannel;

		private long currentSize = 0;
		private int downloadProgress = -1;

		private AttachmentReceiver(MessagePojo messagePojo) throws Exception {
			try {
				this.messagePojo = messagePojo;
				this.globalSize = messagePojo.globalSize;
				path = Files.createTempFile("dms", null);
				path.toFile().deleteOnExit();
				fileChannel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			} catch (Exception e) {
				destroy();
				throw e;
			}
		}

		private void interrupt() {

			try {
				fileChannel.close();
			} catch (Exception e) {

			}

		}

		private void destroy() {

			interrupt();

			try {
				Files.deleteIfExists(path);
			} catch (Exception e) {

			}

			if (messagePojo == null) {
				return;
			}

			ContentType contentType = messagePojo.contentType;
			Long trackingId = messagePojo.trackingId;
			messagePojo = null;
			if (contentType == ContentType.UPLOAD) {
				listener.downloadFailed(trackingId);
			}

		}

		private void dataReceived(ByteBuffer dataBuffer, boolean hasMore) throws Exception {

			long position = dataBuffer.getLong();
			int dataLength = dataBuffer.remaining();
			currentSize = position + dataLength;
			fileChannel.write(dataBuffer, position);
			checkDownloadProgress();
			if (!hasMore) {
				fileChannel.force(true);
				fileChannel.close();
			}

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

		void cannotReceiveMore(int messageNumber);

		void downloadProgress(Long trackingId, int progress);

		void downloadFailed(Long trackingId);

	}

}
