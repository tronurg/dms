package com.ogya.dms.commons;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.ogya.dms.commons.structures.MessagePojo;

public class DmsMessageFactory {

	private final Consumer<MessagePojo> messageConsumer;
	private final Map<Integer, MessageReceiver> messageReceivers = new HashMap<Integer, MessageReceiver>();

	public DmsMessageFactory(Consumer<MessagePojo> messageConsumer) {
		this.messageConsumer = messageConsumer;
	}

	public void inFeed(int messageNumber, byte[] data) {

		MessageReceiver messageReceiver = messageReceivers.get(messageNumber);
		if (messageReceiver == null) {
			if (data.length > 0) {
				messageReceiver = new MessageReceiver(ByteBuffer.wrap(data).getLong());
				messageReceivers.put(messageNumber, messageReceiver);
			}
			return;
		}

		boolean messageReady = messageReceiver.dataReceived(data);
		if (messageReady) {
			messageReceivers.remove(messageNumber);
			MessagePojo messagePojo = messageReceiver.getMessagePojo();
			if (messagePojo != null) {
				messageConsumer.accept(messagePojo);
			}
		}

	}

	public void deleteResources() {
		messageReceivers.forEach((messageNumber, messageReceiver) -> messageReceiver.clearResources());
		messageReceivers.clear();
	}

	public static MessageSender outFeed(MessagePojo messagePojo, AtomicBoolean health) {
		return outFeed(DmsPackingFactory.pack(messagePojo), messagePojo.attachment, health);
	}

	public static MessageSender outFeedServerToClient(MessagePojo messagePojo, AtomicBoolean health) {
		return outFeed(DmsPackingFactory.packServerToClient(messagePojo), messagePojo.attachment, health);
	}

	public static MessageSender outFeedServerToServer(MessagePojo messagePojo, AtomicBoolean health) {
		return outFeed(DmsPackingFactory.packServerToServer(messagePojo), messagePojo.attachment, health);
	}

	private static MessageSender outFeed(byte[] data, Path attachment, AtomicBoolean health) {
		return new MessageSender(data, attachment, health);
	}

	private final class MessageReceiver {

		private boolean fileError = false;
		private long remaining;
		private Path path;
		private OutputStream outputStream;
		private MessagePojo messagePojo;

		private MessageReceiver(long remaining) {
			this.remaining = remaining;
			if (remaining > 0) {
				try {
					path = Files.createTempFile("dms", null);
					outputStream = new BufferedOutputStream(Files.newOutputStream(path));
				} catch (IOException e) {
					fileError = true;
					clearResources();
				}
			}
		}

		private void clearResources() {

			if (outputStream != null) {
				try {
					outputStream.flush();
					outputStream.close();
					outputStream = null;
				} catch (IOException e) {

				}
			}

			if (path != null) {
				try {
					Files.deleteIfExists(path);
					path = null;
				} catch (IOException e) {

				}
			}

		}

		private boolean dataReceived(byte[] data) {

			int dataLength = data.length;

			if (dataLength == 0) {
				clearResources();
				return true;
			}

			if (remaining > 0) {

				remaining -= dataLength;

				if (fileError)
					return false;

				try {

					outputStream.write(data);

					if (!(remaining > 0)) {
						outputStream.flush();
						outputStream.close();
					}

				} catch (IOException e) {
					fileError = true;
					clearResources();
				}

				return false;

			}

			try {

				MessagePojo messagePojo = DmsPackingFactory.unpack(data, MessagePojo.class);

				if (path != null) {
					messagePojo.attachmentLink = path;
				}

				if (!fileError) {
					this.messagePojo = messagePojo;
				}

			} catch (Exception e) {
				clearResources();
			}

			return true;

		}

		private MessagePojo getMessagePojo() {
			return messagePojo;
		}

	}

	public static final class MessageSender {

		private static final int CHUNK_SIZE = 8192;

		private final byte[] data;
		private final Path attachment;
		private final AtomicBoolean health;
		private InputStream inputStream;
		private byte[] buffer;
		private long fileSize = 0;
		private double totalBytes = 0.0;
		private long totalBytesRead = 0;
		private boolean started = false;
		private boolean ended = false;

		private MessageSender(byte[] data, Path attachment, AtomicBoolean health) {
			this.data = data;
			this.attachment = attachment;
			this.health = health;
			if (attachment != null) {
				initResources();
			}
		}

		private void initResources() {
			try {
				inputStream = new BufferedInputStream(Files.newInputStream(attachment));
				fileSize = Files.size(attachment);
				totalBytes = fileSize + data.length;
			} catch (Exception e) {

			}
			if (buffer == null) {
				buffer = new byte[CHUNK_SIZE];
			}
		}

		private void closeResources() {
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
			return !ended;
		}

		public Chunk next() {

			if (ended) {
				return null;
			}

			if (!started) {
				started = true;
				return new Chunk(ByteBuffer.allocate(8).putLong(fileSize).array(), 0);
			}

			if (attachment != null) {

				try {
					if (!health.get())
						throw new Exception();
					int bytesRead = inputStream.read(buffer);
					if (bytesRead > 0) {
						totalBytesRead += bytesRead;
						return new Chunk(Arrays.copyOf(buffer, bytesRead), (int) (100 * (totalBytesRead / totalBytes)));
					}
				} catch (Exception e) {
					ended = true;
				}

				closeResources();

				if (ended) {
					return new Chunk(new byte[0], -1);
				}

			}

			ended = true;

			return new Chunk(data, 100);

		}

		public boolean fileSizeGreaterThan(long limitSize) {
			return fileSize > limitSize;
		}

		public void reset() {
			started = false;
			ended = false;
			if (attachment == null)
				return;
			closeResources();
			initResources();
		}

		public void close() {
			closeResources();
			ended = true;
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
