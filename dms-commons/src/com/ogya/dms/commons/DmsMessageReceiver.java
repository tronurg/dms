package com.ogya.dms.commons;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import com.ogya.dms.commons.structures.MessagePojo;

public class DmsMessageReceiver {

	private final DmsMessageReceiverListener listener;
	private final Map<Integer, AttachmentReceiver> attachmentReceivers = new HashMap<Integer, AttachmentReceiver>();

	public DmsMessageReceiver(DmsMessageReceiverListener listener) {
		this.listener = listener;
	}

	public void inFeed(int messageNumber, byte[] data) {

		if (data.length == 0) {
			interrupt(messageNumber);
			return;
		}

		ByteBuffer dataBuffer = ByteBuffer.wrap(data);
		byte sign = dataBuffer.get();

		if (sign < 0) {
			interrupt(messageNumber);
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
			MessagePojo messagePojo = attachmentReceiver.getMessagePojo();
			if (messagePojo != null) {
				listener.messageReceived(messagePojo);
			}
		}

	}

	private void interrupt(int messageNumber) {
		AttachmentReceiver attachmentReceiver = attachmentReceivers.remove(messageNumber);
		if (attachmentReceiver == null) {
			return;
		}
		attachmentReceiver.interrupt();
	}

	public void deleteResources() {
		attachmentReceivers.forEach((messageNumber, attachmentReceiver) -> attachmentReceiver.interrupt());
		attachmentReceivers.clear();
	}

	private final class AttachmentReceiver {

		private MessagePojo messagePojo;
		private FileChannel fileChannel;
		private boolean interrupted = false;
		private long fileSize;

		private AttachmentReceiver(MessagePojo messagePojo) {
			this.messagePojo = messagePojo;
			this.fileSize = messagePojo.attachment.size;
			try {
				messagePojo.attachment.path = Files.createTempFile("dms", null);
				fileChannel = FileChannel.open(messagePojo.attachment.path, StandardOpenOption.CREATE,
						StandardOpenOption.WRITE);
			} catch (Exception e) {
				interrupt();
			}
		}

		private void interrupt() {

			interrupted = true;

			try {
				fileChannel.close();
			} catch (Exception e) {

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
				long currentSize = position + dataLength;
				fileChannel.write(dataBuffer, position);
				boolean done = !(currentSize < fileSize);
				if (done) {
					fileChannel.force(true);
					fileChannel.close();
				}
				return done;
			} catch (Exception e) {
				interrupt();
			}

			return true;

		}

		private MessagePojo getMessagePojo() {
			return messagePojo;
		}

	}

	public static interface DmsMessageReceiverListener {

		void messageReceived(MessagePojo messagePojo);

		void messageFailed(int messageNumber);

	}

}
