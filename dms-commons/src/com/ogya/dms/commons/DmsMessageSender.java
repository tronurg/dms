package com.ogya.dms.commons;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ogya.dms.commons.structures.AttachmentPojo;
import com.ogya.dms.commons.structures.ContentType;
import com.ogya.dms.commons.structures.MessagePojo;

public class DmsMessageSender implements AutoCloseable {

	private static final int CHUNK_SIZE = 8192;
	private static final byte MESSAGE_POJO_PREFIX = -1;

	private final MessagePojo messagePojo;
	private final Direction direction;

	protected final AtomicBoolean health = new AtomicBoolean(true);
	private FileChannel fileChannel;
	private int pojoSize = 0;
	private long position = Long.MIN_VALUE;
	private long minPosition = 0;
	private long maxPosition = 0;

	public DmsMessageSender(MessagePojo messagePojo, Direction direction) {

		this.messagePojo = messagePojo;
		this.direction = direction;

		init();

	}

	private void init() {
		AttachmentPojo attachmentPojo = messagePojo.attachment;
		if (attachmentPojo == null) {
			return;
		}
		Path attachment = attachmentPojo.path;
		if (attachment == null) {
			messagePojo.attachment = null;
			return;
		}
		try {
			fileChannel = FileChannel.open(attachment, StandardOpenOption.READ);
			if (attachmentPojo.position != null) {
				minPosition = attachmentPojo.position;
			}
			maxPosition = fileChannel.size();
			messagePojo.attachment.size = maxPosition;
			if (maxPosition == 0) {
				fileChannel.close();
			}
		} catch (Exception e) {
			markAsDone();
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
		position = minPosition;
		ByteBuffer dataBuffer = ByteBuffer.allocate(Byte.BYTES + pojoSize).put(MESSAGE_POJO_PREFIX).put(data);
		dataBuffer.flip();
		return dataBuffer;
	}

	private ByteBuffer getFileData() throws Exception {
		ByteBuffer dataBuffer = ByteBuffer.allocate(Long.BYTES + CHUNK_SIZE).putLong(position);
		fileChannel.position(position);
		fileChannel.read(dataBuffer);
		position = fileChannel.position();
		dataBuffer.flip();
		return dataBuffer;
	}

	private Chunk getEmptyChunk() {
		return new Chunk(ByteBuffer.allocate(0), -1);
	}

	private Chunk getUploadFailureChunk() {
		try {
			MessagePojo messagePojo = new MessagePojo(null, null, this.messagePojo.receiverUuid,
					ContentType.UPLOAD_FAILURE, this.messagePojo.trackingId, null, null);
			byte[] data = DmsPackingFactory.pack(messagePojo);
			ByteBuffer dataBuffer = ByteBuffer.allocate(Byte.BYTES + data.length).put(MESSAGE_POJO_PREFIX).put(data);
			dataBuffer.flip();
			return new Chunk(dataBuffer, -1);
		} catch (Exception e) {

		}
		return getEmptyChunk();
	}

	protected boolean isFileSizeGreaterThan(long limitSize) {
		return maxPosition > limitSize;
	}

	public boolean hasMore() {
		return position < maxPosition;
	}

	public Chunk next() {
		if (!hasMore()) {
			return null;
		}
		boolean healthy = health.get();
		Chunk chunk = null;
		try {
			if (!healthy) {
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
			markAsDone();
			if (messagePojo.contentType == ContentType.UPLOAD && healthy) {
				chunk = getUploadFailureChunk();
			} else if (position > Long.MIN_VALUE) {
				// Already started sending, so send closure byte
				chunk = getEmptyChunk();
			}
		}
		return chunk;
	}

	public void rewind() {
		if (position < minPosition) {
			return;
		}
		if (position == minPosition) {
			position = -1;
			return;
		}
		long remainder = position % CHUNK_SIZE;
		if (remainder == 0) {
			remainder = CHUNK_SIZE;
		}
		position -= remainder;
	}

	public void rewind(int times) {
		for (int i = 0; i < times; ++i) {
			rewind();
		}
	}

	public void markAsDone() {
		maxPosition = Long.MIN_VALUE;
	}

	@Override
	public void close() {
		markAsDone();
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
