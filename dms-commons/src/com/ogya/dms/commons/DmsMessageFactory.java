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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.ogya.dms.commons.structures.MessagePojo;

public class DmsMessageFactory {

	private final Consumer<MessagePojo> messageConsumer;

	private boolean inProgress = false;
	private boolean error = false;
	private long remaining = 0;
	private Path path;
	private OutputStream outputStream;

	public DmsMessageFactory(Consumer<MessagePojo> messageConsumer) {

		this.messageConsumer = messageConsumer;

	}

	public void inFeed(byte[] data) {

		if (data.length == 0) {

			deleteResources();
			reset();

			return;

		}

		try {

			if (inProgress) {
				bodyReceived(data);
			} else {
				headerReceived(data);
			}

		} catch (IOException e) {

			error = true;

		}

	}

	public void deleteResources() {

		if (outputStream != null) {
			try {
				outputStream.flush();
				outputStream.close();
			} catch (IOException e) {

			}
		}

		if (path != null) {
			try {
				Files.deleteIfExists(path);
			} catch (IOException e) {

			}
		}

	}

	private void reset() {

		inProgress = false;
		error = false;
		remaining = 0;
		path = null;
		outputStream = null;

	}

	private void headerReceived(byte[] data) throws IOException {

		inProgress = true;
		remaining = ByteBuffer.wrap(data).getLong();

		if (remaining > 0) {

			path = Files.createTempFile("dms", null);
			outputStream = new BufferedOutputStream(Files.newOutputStream(path));

		}

	}

	private void bodyReceived(byte[] data) throws IOException {

		if (remaining > 0) {

			remaining -= data.length;

			if (error)
				return;

			outputStream.write(data);

			if (remaining == 0) {
				outputStream.flush();
				outputStream.close();
			}

		} else {

			try {

				MessagePojo messagePojo = DmsPackingFactory.unpack(data, MessagePojo.class);
				messagePojo.attachment = path;

				if (!error)
					messageConsumer.accept(messagePojo);

			} catch (Exception e) {

			}

			reset();

		}

	}

	public static void outFeed(MessagePojo messagePojo, int chunkSize, Consumer<byte[]> dataConsumer) {

		outFeed(messagePojo, chunkSize, new AtomicBoolean(true), (data, progress) -> dataConsumer.accept(data));

	}

	public static void outFeed(MessagePojo messagePojo, int chunkSize, AtomicBoolean health,
			BiConsumer<byte[], Integer> dataConsumer) {

		Path path = messagePojo.attachment;

		byte[] messageBytes = DmsPackingFactory.pack(messagePojo);

		if (path == null) {

			dataConsumer.accept(ByteBuffer.allocate(8).putLong(0L).array(), 0);

			dataConsumer.accept(messageBytes, 100);

		} else {

			try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(path))) {

				long fileSize = Files.size(path);
				double totalBytes = fileSize + messageBytes.length;

				dataConsumer.accept(ByteBuffer.allocate(8).putLong(fileSize).array(), 0);

				byte[] buffer = new byte[chunkSize];

				int bytesRead;
				int totalBytesRead = 0;

				while ((bytesRead = inputStream.read(buffer)) > 0) {

					if (!health.get())
						throw new IOException();

					totalBytesRead += bytesRead;

					dataConsumer.accept(Arrays.copyOf(buffer, chunkSize), (int) (100 * (totalBytesRead / totalBytes)));

				}

				dataConsumer.accept(messageBytes, 100);

			} catch (IOException e) {

				dataConsumer.accept(new byte[0], -1);

			}

		}

	}

}