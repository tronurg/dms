package com.ogya.dms.commons;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.ogya.dms.commons.structures.MessagePojo;

public class DmsMessageFactory {

	private static final int CHUNK_SIZE = 8192;
	private static final AtomicInteger MESSAGE_COUNTER = new AtomicInteger(0);

	private final Consumer<MessagePojo> messageConsumer;

	private final Map<Integer, MessageReceiver> messageReceivers = new HashMap<Integer, MessageReceiver>();

	public DmsMessageFactory(Consumer<MessagePojo> messageConsumer) {

		this.messageConsumer = messageConsumer;

	}

	public void inFeed(byte[] data) {

		ByteBuffer dataBuffer = ByteBuffer.wrap(data);
		int messageNumber = dataBuffer.getInt();

		MessageReceiver messageReceiver = messageReceivers.get(messageNumber);
		if (messageReceiver == null) {
			messageReceiver = new MessageReceiver(dataBuffer.getLong());
			messageReceivers.put(messageNumber, messageReceiver);
			return;
		}

		boolean messageReady = messageReceiver.dataReceived(dataBuffer);
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

	public static void outFeed(MessagePojo messagePojo, AtomicBoolean health,
			BiConsumer<byte[], Integer> dataConsumer) {

		outFeed(DmsPackingFactory.pack(messagePojo), messagePojo.attachment, health, dataConsumer);

	}

	public static void outFeedRemote(MessagePojo messagePojo, AtomicBoolean health,
			BiConsumer<byte[], Integer> dataConsumer) {

		outFeed(DmsPackingFactory.packRemote(messagePojo), messagePojo.attachment, health, dataConsumer);

	}

	private static void outFeed(byte[] data, Path attachment, AtomicBoolean health,
			BiConsumer<byte[], Integer> dataConsumer) {

		final int messageNumber = MESSAGE_COUNTER.getAndIncrement();
		final int dataLength = data.length;

		if (attachment == null) {

			dataConsumer.accept(ByteBuffer.allocate(12).putInt(messageNumber).putLong(0L).array(), 0);

			dataConsumer.accept(ByteBuffer.allocate(4 + dataLength).putInt(messageNumber).put(data).array(), 100);

		} else {

			try (ByteChannel fileChannel = Files.newByteChannel(attachment)) {

				long fileSize = Files.size(attachment);
				double totalBytes = fileSize + dataLength;

				dataConsumer.accept(ByteBuffer.allocate(12).putInt(messageNumber).putLong(fileSize).array(), 0);

				ByteBuffer buffer = ByteBuffer.allocate(CHUNK_SIZE);

				int bytesRead;
				long totalBytesRead = 0;

				while ((bytesRead = fileChannel.read(buffer.putInt(messageNumber))) > 0) {

					if (!health.get())
						throw new IOException();

					totalBytesRead += bytesRead;
					buffer.flip();
					byte[] chunk = new byte[buffer.limit()];
					buffer.get(chunk);
					buffer.clear();

					dataConsumer.accept(chunk, (int) (100 * (totalBytesRead / totalBytes)));

				}

				dataConsumer.accept(ByteBuffer.allocate(4 + dataLength).putInt(messageNumber).put(data).array(), 100);

			} catch (IOException e) {

				dataConsumer.accept(ByteBuffer.allocate(4).putInt(messageNumber).array(), -1);

			}

		}

	}

	private final class MessageReceiver {

		private boolean fileError = false;
		private long remaining = 0;
		private Path path;
		private ByteChannel fileChannel;
		private MessagePojo messagePojo;

		private MessageReceiver(long remaining) {
			this.remaining = remaining;
			if (remaining > 0) {
				try {
					path = Files.createTempFile("dms", null);
					fileChannel = Files.newByteChannel(path);
				} catch (IOException e) {
					fileError = true;
					clearResources();
				}
			}
		}

		private void clearResources() {

			if (fileChannel != null) {
				try {
					fileChannel.close();
					fileChannel = null;
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

		private boolean dataReceived(ByteBuffer dataBuffer) {

			int dataLength = dataBuffer.remaining();

			if (dataLength == 0) {
				clearResources();
				return true;
			}

			if (remaining > 0) {

				remaining -= dataLength;

				if (fileError)
					return false;

				try {

					fileChannel.write(dataBuffer);

					if (!(remaining > 0)) {
						fileChannel.close();
					}

				} catch (IOException e) {
					fileError = true;
					clearResources();
				}

				return false;

			}

			try {

				byte[] data = new byte[dataLength];
				dataBuffer.get(data);
				MessagePojo messagePojo = DmsPackingFactory.unpack(data, MessagePojo.class);
				messagePojo.attachment = path;

				if (!fileError)
					this.messagePojo = messagePojo;

			} catch (Exception e) {
				clearResources();
			}

			return true;

		}

		private MessagePojo getMessagePojo() {

			return messagePojo;

		}

	}

}
