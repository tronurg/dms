package com.ogya.dms.commons;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.ogya.dms.commons.structures.MessagePojo;

public class DmsMessageReceiver {

	private final Consumer<MessagePojo> messageConsumer;
	private final Map<Integer, AttachmentReceiver> attachmentReceivers = new HashMap<Integer, AttachmentReceiver>();

	public DmsMessageReceiver(Consumer<MessagePojo> messageConsumer) {
		this.messageConsumer = messageConsumer;
	}

	public void inFeed(int messageNumber, byte[] data) {

		ByteBuffer dataBuffer = ByteBuffer.wrap(data);
		byte sign = dataBuffer.get();

		if (sign < 0) {
			try {
				MessagePojo messagePojo = DmsPackingFactory.unpack(dataBuffer, MessagePojo.class);
				if (messagePojo.attachment == null || messagePojo.attachment.link != null) {
					messageConsumer.accept(messagePojo);
				} else if (messagePojo.attachment.size == 0) {
					messagePojo.attachment.link = Files.createTempFile("dms", null);
					messageConsumer.accept(messagePojo);
				} else {
					newAttachmentReceiver(messageNumber, messagePojo);
				}
			} catch (Exception e) {

			}
			return;
		}

		AttachmentReceiver attachmentReceiver = attachmentReceivers.get(messageNumber);
		if (attachmentReceiver == null) {
			return;
		}
		dataBuffer.rewind();
		boolean attachmentReady = attachmentReceiver.dataReceived(dataBuffer);
		if (attachmentReady) {
			attachmentReceivers.remove(messageNumber);
			MessagePojo messagePojo = attachmentReceiver.getMessagePojo();
			if (messagePojo != null) {
				messageConsumer.accept(messagePojo);
			}
		}

	}

	private void newAttachmentReceiver(int messageNumber, MessagePojo messagePojo) {
		AttachmentReceiver attachmentReceiver = attachmentReceivers.get(messageNumber);
		if (attachmentReceiver != null) {
			attachmentReceiver.interrupt();
		}
		attachmentReceiver = new AttachmentReceiver(messagePojo);
		attachmentReceivers.put(messageNumber, attachmentReceiver);
	}

	public void deleteResources() {
		attachmentReceivers.forEach((messageNumber, attachmentReceiver) -> attachmentReceiver.interrupt());
		attachmentReceivers.clear();
	}

	private final class AttachmentReceiver {

		private MessagePojo messagePojo;
		private FileChannel fileChannel;
		private boolean interrupted = false;
		private long remaining;

		private AttachmentReceiver(MessagePojo messagePojo) {
			this.messagePojo = messagePojo;
			this.remaining = messagePojo.attachment.size;
			try {
				messagePojo.attachment.link = Files.createTempFile("dms", null);
				fileChannel = FileChannel.open(messagePojo.attachment.link, StandardOpenOption.CREATE,
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
				Files.deleteIfExists(messagePojo.attachment.link);
			} catch (Exception e) {

			}

			messagePojo = null;

		}

		private boolean dataReceived(ByteBuffer dataBuffer) {

			if (!dataBuffer.hasRemaining()) {
				interrupt();
				return true;
			}

			long position = dataBuffer.getLong();
			int dataLength = dataBuffer.remaining();

			remaining -= dataLength;
			boolean done = !(remaining > 0);

			if (!interrupted) {
				try {
					fileChannel.write(dataBuffer, position);
					if (done) {
						fileChannel.force(true);
						fileChannel.close();
					}
				} catch (Exception e) {
					interrupt();
				}
			}

			return done;

		}

		private MessagePojo getMessagePojo() {
			return messagePojo;
		}

	}

}
