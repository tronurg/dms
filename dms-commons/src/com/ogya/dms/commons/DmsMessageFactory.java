package com.ogya.dms.commons;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.ogya.dms.commons.structures.MessagePojo;

public class DmsMessageFactory {

	private static final int CHUNK_SIZE = 8192;

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

		if (attachment == null) {

			dataConsumer.accept(ByteBuffer.allocate(8).putLong(0L).array(), 0);

			dataConsumer.accept(data, 100);

		} else {

			try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(attachment))) {

				long fileSize = Files.size(attachment);
				double totalBytes = fileSize + data.length;

				dataConsumer.accept(ByteBuffer.allocate(8).putLong(fileSize).array(), 0);

				byte[] buffer = new byte[CHUNK_SIZE];

				int bytesRead;
				long totalBytesRead = 0;

				while ((bytesRead = inputStream.read(buffer)) > 0) {

					if (!health.get())
						throw new IOException();

					totalBytesRead += bytesRead;

					dataConsumer.accept(Arrays.copyOf(buffer, bytesRead), (int) (100 * (totalBytesRead / totalBytes)));

				}

				dataConsumer.accept(data, 100);

			} catch (IOException e) {

				dataConsumer.accept(new byte[0], -1);

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
