package com.ogya.dms.commons;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ogya.dms.commons.structures.MessagePojo;

public class DmsMessageSender implements AutoCloseable {

	private static final int CHUNK_SIZE = 8200;
	private static final byte MESSAGE_POJO_PREFIX = -1;

	private final MessagePojo messagePojo;
	private final Direction direction;

	protected final AtomicBoolean health = new AtomicBoolean(true);
	private FileChannel fileChannel;
	private int pojoSize = 0;
	private long position = Long.MIN_VALUE;
	private long maxPosition = 0;

	public DmsMessageSender(MessagePojo messagePojo, Direction direction) {

		this.messagePojo = messagePojo;
		this.direction = direction;

		init();

	}

	private void init() {
		Path attachment = messagePojo.getAttachmentSource();
		try {
			if (attachment != null) {
				maxPosition = Files.size(attachment);
				messagePojo.attachment.size = maxPosition;
			}
			if (maxPosition > 0) {
				fileChannel = FileChannel.open(attachment, StandardOpenOption.READ);
			}
		} catch (Exception e) {
			maxPosition = Long.MIN_VALUE;
		}
	}

	private ByteBuffer getPojoData() throws Exception {
		byte[] data = null;
		switch (direction) {
		case CLIENT_TO_SERVER: {
			data = DmsPackingFactory.pack(messagePojo);
			break;
		}
		case SERVER_TO_SERVER: {
			data = DmsPackingFactory.packServerToServer(messagePojo);
			break;
		}
		case SERVER_TO_CLIENT: {
			data = DmsPackingFactory.packServerToClient(messagePojo);
			break;
		}
		}
		pojoSize = data.length;
		position = 0;
		if (fileChannel != null) {
			fileChannel.position(position);
		}
		ByteBuffer dataBuffer = ByteBuffer.allocate(1 + pojoSize).put(MESSAGE_POJO_PREFIX).put(data);
		dataBuffer.flip();
		return dataBuffer;
	}

	private ByteBuffer getFileData() throws Exception {
		ByteBuffer dataBuffer = ByteBuffer.allocate(CHUNK_SIZE).putLong(position);
		fileChannel.read(dataBuffer);
		position = fileChannel.position();
		dataBuffer.flip();
		return dataBuffer;
	}

	protected boolean isFileSizeGreaterThan(long limitSize) {
		return maxPosition > limitSize;
	}

	public boolean hasMore() {
		return position < maxPosition;
	}

	public Chunk next() {
		Chunk chunk = null;
		if (hasMore()) {
			try {
				if (!health.get()) {
					throw new Exception();
				}
				ByteBuffer dataBuffer;
				if (position < 0) {
					dataBuffer = getPojoData();
				} else {
					dataBuffer = getFileData();
				}
				int progress = (int) (100.0 * (pojoSize + position) / (pojoSize + maxPosition));
				chunk = new Chunk(dataBuffer, progress);
			} catch (Exception e) {
				maxPosition = Long.MIN_VALUE;
				if (position > Long.MIN_VALUE) {
					// Already started sending, so send closure byte
					chunk = new Chunk(ByteBuffer.allocate(0), -1);
				}
			}
		}
		return chunk;
	}

	public void reset() {
		position = -1;
	}

	@Override
	public void close() {
		position = Long.MAX_VALUE;
		if (fileChannel == null) {
			return;
		}
		try {
			fileChannel.close();
		} catch (Exception e) {

		}
		fileChannel = null;
	}

	public static final class Chunk {

		public final ByteBuffer dataBuffer;
		public final int progress;

		private Chunk(ByteBuffer dataBuffer, int progress) {
			this.dataBuffer = dataBuffer;
			this.progress = progress;
		}

	}

	public static enum Direction {

		CLIENT_TO_SERVER, SERVER_TO_SERVER, SERVER_TO_CLIENT

	}

}
