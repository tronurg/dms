package com.ogya.dms.core.common;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ogya.dms.commons.DmsPackingFactory;
import com.ogya.dms.commons.structures.ContentType;
import com.ogya.dms.commons.structures.MessagePojo;
import com.ogya.dms.core.structures.AttachmentPojo;
import com.ogya.dms.core.structures.Chunk;

public class DmsMessageSender implements AutoCloseable {

	private static final int CHUNK_SIZE = 8192;
	private static final byte MESSAGE_POJO_PREFIX = -1;

	private final MessagePojo messagePojo;
	private final AttachmentPojo attachmentPojo;

	protected final AtomicBoolean health = new AtomicBoolean(true);
	private FileChannel fileChannel;
	private int pojoSize = 0;
	private long position = Long.MIN_VALUE;
	private long minPosition = 0;
	private long maxPosition = 0;

	public DmsMessageSender(MessagePojo messagePojo, AttachmentPojo attachmentPojo) {

		this.messagePojo = messagePojo;
		this.attachmentPojo = attachmentPojo;
		init();

	}

	private void init() {
		if (attachmentPojo == null || attachmentPojo.path == null) {
			return;
		}
		try {
			fileChannel = FileChannel.open(attachmentPojo.path, StandardOpenOption.READ);
			if (attachmentPojo.position != null) {
				minPosition = attachmentPojo.position;
			}
			maxPosition = fileChannel.size();
			if (messagePojo.contentType == ContentType.UPLOAD) {
				messagePojo.globalSize = maxPosition;
			}
			if (maxPosition == 0) {
				fileChannel.close();
			}
		} catch (Exception e) {
			markAsDone();
		}
	}

	private ByteBuffer getPojoData() throws Exception {
		byte[] data = DmsPackingFactory.pack(messagePojo);
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

	private void markAsDone() {
		maxPosition = Long.MIN_VALUE;
	}

	public long getFileSize() {
		return maxPosition;
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
			if (position > Long.MIN_VALUE) {
				// Already started sending, so send closure byte
				chunk = new Chunk(ByteBuffer.allocate(0), -1);
			}
		}
		return chunk;
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

	public static enum Direction {

		CLIENT_TO_SERVER, SERVER_TO_SERVER, SERVER_TO_CLIENT

	}

}
