package com.ogya.dms.commons;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import com.ogya.dms.commons.structures.MessagePojo;

public class DmsMessageFactory {

	private final Consumer<MessagePojo> messageConsumer;
	private final Map<Integer, AttachmentReceiver> attachmentReceivers = new HashMap<Integer, AttachmentReceiver>();

	public DmsMessageFactory(Consumer<MessagePojo> messageConsumer) {
		this.messageConsumer = messageConsumer;
	}

	public void inFeed(int messageNumber, byte[] data) {

		AttachmentReceiver attachmentReceiver = attachmentReceivers.get(messageNumber);

		if (attachmentReceiver == null) {
			try {
				MessagePojo messagePojo = DmsPackingFactory.unpack(data, MessagePojo.class);
				if (messagePojo.attachment == null || messagePojo.attachment.link != null) {
					messageConsumer.accept(messagePojo);
				} else if (messagePojo.attachment.size == 0) {
					messagePojo.attachment.link = Files.createTempFile("dms", null);
					messageConsumer.accept(messagePojo);
				} else {
					attachmentReceiver = new AttachmentReceiver(messagePojo);
					attachmentReceivers.put(messageNumber, attachmentReceiver);
				}
			} catch (Exception e) {

			}
			return;
		}

		boolean attachmentReady = attachmentReceiver.dataReceived(data);
		if (attachmentReady) {
			attachmentReceivers.remove(messageNumber);
			MessagePojo messagePojo = attachmentReceiver.getMessagePojo();
			if (messagePojo != null) {
				messageConsumer.accept(messagePojo);
			}
		}

	}

	public void deleteResources() {
		attachmentReceivers.forEach((messageNumber, attachmentReceiver) -> attachmentReceiver.interrupt());
		attachmentReceivers.clear();
	}

	public static MessageSender outFeed(MessagePojo messagePojo, AtomicBoolean health) {
		return new MessageSender(messagePojo, health, DmsPackingFactory::pack);
	}

	public static MessageSender outFeedServerToClient(MessagePojo messagePojo, AtomicBoolean health) {
		return new MessageSender(messagePojo, health, DmsPackingFactory::packServerToClient);
	}

	public static MessageSender outFeedServerToServer(MessagePojo messagePojo, AtomicBoolean health) {
		return new MessageSender(messagePojo, health, DmsPackingFactory::packServerToServer);
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

		private boolean dataReceived(byte[] data) {

			if (data.length == 0) {
				interrupt();
				return true;
			}

			ByteBuffer dataBuffer = ByteBuffer.wrap(data);
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

	public static final class MessageSender {

		private static final int CHUNK_SIZE = 8200;

		private final MessagePojo messagePojo;
		private final AtomicBoolean health;
		private final Function<MessagePojo, byte[]> serializer;

		private Path attachment;
		private FileChannel fileChannel;
		private long fileSize = 0;
		private long bytesProcessed = 0;
		private double totalBytes = 0.0;
		private boolean started;
		private Chunk nextChunk;

		private MessageSender(MessagePojo messagePojo, AtomicBoolean health, Function<MessagePojo, byte[]> serializer) {

			this.messagePojo = messagePojo;
			this.health = health;
			this.serializer = serializer;

			init();

		}

		private void init() {
			started = false;
			try {
				attachment = messagePojo.getAttachmentSource();
				if (attachment != null) {
					fileChannel = FileChannel.open(attachment, StandardOpenOption.READ);
					fileSize = fileChannel.size();
					messagePojo.attachment.size = fileSize;
					if (fileSize == 0) {
						closeFile();
					}
				}
				byte[] data = serializer.apply(messagePojo);
				bytesProcessed = data.length;
				totalBytes = fileSize + data.length;
				nextChunk = new Chunk(ByteBuffer.wrap(data), (int) (100 * (bytesProcessed / totalBytes)));
			} catch (Exception e) {
				close();
			}
		}

		private void closeFile() {
			attachment = null;
			if (fileChannel == null)
				return;
			try {
				fileChannel.close();
			} catch (Exception e) {

			}
			fileChannel = null;
		}

		public boolean hasNext() {
			if (!(started || health.get())) {
				close();
			}
			return nextChunk != null;
		}

		public Chunk next() {

			if (nextChunk == null)
				return null;

			started = true;

			if (!health.get()) {
				close();
				return new Chunk(ByteBuffer.allocate(0), -1);
			}

			Chunk result = nextChunk;

			if (attachment == null) {
				nextChunk = null;
			} else {
				try {
					ByteBuffer dataBuffer = ByteBuffer.allocate(CHUNK_SIZE);
					dataBuffer.putLong(fileChannel.position());
					int bytesRead = fileChannel.read(dataBuffer);
					dataBuffer.flip();
					if (bytesRead > 0) {
						bytesProcessed += bytesRead;
						nextChunk = new Chunk(dataBuffer, (int) (100 * (bytesProcessed / totalBytes)));
					} else {
						close();
					}
				} catch (Exception e) {
					closeFile();
					nextChunk = new Chunk(ByteBuffer.allocate(0), -1);
				}
			}

			return result;

		}

		public boolean fileSizeGreaterThan(long limitSize) {
			return fileSize > limitSize;
		}

		public void reset() {
			close();
			init();
		}

		public void close() {
			closeFile();
			nextChunk = null;
		}

	}

	public static final class Chunk {

		public final ByteBuffer dataBuffer;
		public final int progress;

		private Chunk(ByteBuffer dataBuffer, int progress) {
			this.dataBuffer = dataBuffer;
			this.progress = progress;
		}

	}

}
