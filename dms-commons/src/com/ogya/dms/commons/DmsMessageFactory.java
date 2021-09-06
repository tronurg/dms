package com.ogya.dms.commons;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
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
		private OutputStream outputStream;
		private boolean interrupted = false;
		private long remaining;

		private AttachmentReceiver(MessagePojo messagePojo) {
			this.messagePojo = messagePojo;
			this.remaining = messagePojo.attachment.size;
			try {
				messagePojo.attachment.link = Files.createTempFile("dms", null);
				outputStream = new BufferedOutputStream(Files.newOutputStream(messagePojo.attachment.link));
			} catch (Exception e) {
				interrupt();
			}
		}

		private void interrupt() {

			interrupted = true;

			try {
				outputStream.close();
			} catch (Exception e) {

			}

			try {
				Files.deleteIfExists(messagePojo.attachment.link);
			} catch (Exception e) {

			}

			messagePojo = null;

		}

		private boolean dataReceived(byte[] data) {

			int dataLength = data.length;

			if (dataLength == 0) {
				interrupt();
				return true;
			}

			remaining -= dataLength;
			boolean done = !(remaining > 0);

			if (!interrupted) {
				try {
					outputStream.write(data);
					if (done) {
						outputStream.flush();
						outputStream.close();
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

		private static final int CHUNK_SIZE = 8192;

		private final MessagePojo messagePojo;
		private final AtomicBoolean health;
		private final Function<MessagePojo, byte[]> serializer;

		private Path attachment;
		private InputStream inputStream;
		private byte[] buffer;
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
					inputStream = new BufferedInputStream(Files.newInputStream(attachment));
					fileSize = Files.size(attachment);
					messagePojo.attachment.size = fileSize;
					if (fileSize == 0) {
						closeFile();
					} else if (buffer == null) {
						buffer = new byte[CHUNK_SIZE];
					}
				}
				byte[] data = serializer.apply(messagePojo);
				bytesProcessed = data.length;
				totalBytes = fileSize + data.length;
				nextChunk = new Chunk(data, (int) (100 * (bytesProcessed / totalBytes)));
			} catch (Exception e) {
				close();
			}
		}

		private void closeFile() {
			attachment = null;
			if (inputStream == null)
				return;
			try {
				inputStream.close();
			} catch (Exception e) {

			}
			inputStream = null;
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
				return new Chunk(new byte[0], -1);
			}

			Chunk result = nextChunk;

			if (attachment == null) {
				nextChunk = null;
			} else {
				try {
					int bytesRead = inputStream.read(buffer);
					if (bytesRead > 0) {
						bytesProcessed += bytesRead;
						nextChunk = new Chunk(Arrays.copyOf(buffer, bytesRead),
								(int) (100 * (bytesProcessed / totalBytes)));
					} else {
						close();
					}
				} catch (Exception e) {
					closeFile();
					nextChunk = new Chunk(new byte[0], -1);
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

		public final byte[] data;
		public final int progress;

		private Chunk(byte[] data, int progress) {
			this.data = data;
			this.progress = progress;
		}

	}

}
