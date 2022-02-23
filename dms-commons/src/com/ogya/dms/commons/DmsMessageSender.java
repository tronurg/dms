package com.ogya.dms.commons;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ogya.dms.commons.structures.MessagePojo;

public class DmsMessageSender {

	private static final int CHUNK_SIZE = 8200;
	private static final byte MESSAGE_POJO_PREFIX = -1;

	private final MessagePojo messagePojo;
	private final AtomicBoolean health;
	private final Direction direction;

	private Path attachment;
	private FileChannel fileChannel;
	private int pojoSize = 0;
	private long fileSize = 0;
	private long position = -1;
	private long bytesProcessed = 0;
	private double totalBytes = 0.0;
	private boolean started;
	private Chunk nextChunk;

	public DmsMessageSender(MessagePojo messagePojo, AtomicBoolean health, Direction direction) {

		this.messagePojo = messagePojo;
		this.health = health;
		this.direction = direction;

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
			byte[] data;
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
			default: {
				throw new Exception();
			}
			}
			int dataLen = data.length;
			ByteBuffer dataBuffer = ByteBuffer.allocate(1 + dataLen).put(MESSAGE_POJO_PREFIX).put(data);
			dataBuffer.flip();
			bytesProcessed = dataLen;
			totalBytes = fileSize + dataLen;
			nextChunk = new Chunk(dataBuffer, (int) (100 * (bytesProcessed / totalBytes)));
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
		fileChannel.position(position);
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
		if (!(started || health.get())) {
			close();
		}
		return nextChunk != null;
	}

	public Chunk next() {
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

//	public Chunk next() {
//
//		if (nextChunk == null)
//			return null;
//
//		started = true;
//
//		if (!health.get()) {
//			close();
//			return new Chunk(ByteBuffer.allocate(0), -1);
//		}
//
//		Chunk result = nextChunk;
//
//		if (attachment == null) {
//			nextChunk = null;
//		} else {
//			try {
//				ByteBuffer dataBuffer = ByteBuffer.allocate(CHUNK_SIZE);
//				dataBuffer.putLong(fileChannel.position());
//				int bytesRead = fileChannel.read(dataBuffer);
//				dataBuffer.flip();
//				if (bytesRead > 0) {
//					bytesProcessed += bytesRead;
//					nextChunk = new Chunk(dataBuffer, (int) (100 * (bytesProcessed / totalBytes)));
//				} else {
//					close();
//				}
//			} catch (Exception e) {
//				closeFile();
//				nextChunk = new Chunk(ByteBuffer.allocate(0), -1);
//			}
//		}
//
//		return result;
//
//	}

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
