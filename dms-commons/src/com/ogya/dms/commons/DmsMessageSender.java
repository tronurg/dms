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
	private final AtomicBoolean health;
	private final Direction direction;

	private FileChannel fileChannel;
	private int pojoSize = 0;
	private long fileSize = 0;
	private long position = -1;

	public DmsMessageSender(MessagePojo messagePojo, AtomicBoolean health, Direction direction) {

		this.messagePojo = messagePojo;
		this.health = health;
		this.direction = direction;

		init();

	}

	private void init() {
		Path attachment = messagePojo.getAttachmentSource();
		try {
			if (attachment != null) {
				fileSize = Files.size(attachment);
				messagePojo.attachment.size = fileSize;
			}
			if (fileSize > 0) {
				fileChannel = FileChannel.open(attachment, StandardOpenOption.READ);
			}
		} catch (Exception e) {
			close();
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

	public boolean hasNext() {
		if (!(health.get() && position < fileSize)) {
			close();
		}
		return position < fileSize;
	}

	public Chunk next() {
		if (!(position < fileSize)) {
			return null;
		}
		ByteBuffer dataBuffer;
		try {
			if (position < 0) {
				dataBuffer = getPojoData();
			} else {
				dataBuffer = getFileData();
			}
			int progress = (int) (100.0 * (pojoSize + position) / (pojoSize + fileSize));
			return new Chunk(dataBuffer, progress);
		} catch (Exception e) {

		}
		return new Chunk(ByteBuffer.allocate(0), -1);
	}

	public boolean isFileSizeGreaterThan(long limitSize) {
		return fileSize > limitSize;
	}

	public void reset() {
		position = -1;
	}

	@Override
	public void close() {
		fileSize = -1;
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
