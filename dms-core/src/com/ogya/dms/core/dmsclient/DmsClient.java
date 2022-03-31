package com.ogya.dms.core.dmsclient;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.ogya.dms.commons.DmsMessageReceiver;
import com.ogya.dms.commons.DmsMessageReceiver.DmsMessageReceiverListener;
import com.ogya.dms.commons.DmsMessageSender;
import com.ogya.dms.commons.DmsMessageSender.Chunk;
import com.ogya.dms.commons.DmsPackingFactory;
import com.ogya.dms.commons.structures.AttachmentPojo;
import com.ogya.dms.commons.structures.Beacon;
import com.ogya.dms.commons.structures.ContentType;
import com.ogya.dms.commons.structures.MessagePojo;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.database.tables.StatusReport;
import com.ogya.dms.core.dmsclient.intf.DmsClientListener;
import com.ogya.dms.core.factory.DmsFactory;
import com.ogya.dms.core.intf.handles.FileHandle;
import com.ogya.dms.core.intf.handles.impl.MessageHandleImpl;
import com.ogya.dms.core.structures.DownloadPojo;
import com.ogya.dms.core.structures.GroupMessageStatus;
import com.ogya.dms.core.structures.MessageStatus;

public class DmsClient implements DmsMessageReceiverListener {

	private static final byte[] SIGNAL = new byte[0];

	private final String uuid;

	private final ZContext context = new ZContext();

	private final String serverIp;
	private final int dealerPort;

	private final AtomicBoolean serverConnected = new AtomicBoolean(false);

	private final DmsClientListener listener;

	private final ExecutorService taskQueue = DmsFactory.newSingleThreadExecutorService();
	private final DmsMessageReceiver messageReceiver;
	private final LinkedBlockingQueue<byte[]> signalQueue = new LinkedBlockingQueue<byte[]>();
	private final AtomicInteger messageCounter = new AtomicInteger(0);
	private final LinkedBlockingDeque<MessageContainer> messageQueue = new LinkedBlockingDeque<MessageContainer>();
	private final List<SendStatus> sendStatuses = Collections.synchronizedList(new ArrayList<SendStatus>());
	private final Map<Integer, MessageContainer> stopMap = Collections
			.synchronizedMap(new HashMap<Integer, MessageContainer>());

	public DmsClient(String uuid, String commIp, int commPort, DmsClientListener listener) {

		this.uuid = uuid;

		this.serverIp = commIp;
		this.dealerPort = commPort;

		this.listener = listener;

		this.messageReceiver = new DmsMessageReceiver(this);

		start();

	}

	private void start() {

		Thread dealerThread = new Thread(this::dealer);
		dealerThread.setDaemon(true);
		dealerThread.start();

		Thread inprocThread = new Thread(this::inproc);
		inprocThread.setDaemon(true);
		inprocThread.start();

		Thread monitorThread = new Thread(this::monitor);
		monitorThread.setDaemon(true);
		monitorThread.start();

	}

	public void sendBeacon(Beacon beacon) {

		sendMessage(new MessagePojo(DmsPackingFactory.pack(beacon), null, null, ContentType.BCON, null, null, null));

	}

	public void claimStartInfo() {

		sendMessage(new MessagePojo(null, uuid, null, ContentType.REQ_STRT, null, null, null));

	}

	public void addRemoteIps(InetAddress... ips) {

		sendMessage(new MessagePojo(DmsPackingFactory.pack(ips), null, null, ContentType.ADD_IPS, null, null, null));

	}

	public void removeRemoteIps(InetAddress... ips) {

		sendMessage(new MessagePojo(DmsPackingFactory.pack(ips), null, null, ContentType.REMOVE_IPS, null, null, null));

	}

	public void sendMessage(Message message, Path attachment, String receiverUuid, Long messageId) {

		MessagePojo messagePojo = new MessagePojo(DmsPackingFactory.pack(message), uuid, receiverUuid,
				ContentType.MESSAGE, messageId, null, null);

		if (attachment != null) {
			messagePojo.attachment = new AttachmentPojo(attachment);
		}

		sendMessage(messagePojo);

	}

	public void cancelMessage(Long trackingId) {

		sendStatuses.forEach(sendStatus -> {
			if (Objects.equals(sendStatus.trackingId, trackingId) && sendStatus.contentType == ContentType.MESSAGE) {
				sendStatus.status.set(false);
			}
		});
		sendMessage(new MessagePojo(null, uuid, null, ContentType.CANCEL_MESSAGE, trackingId, null, null));

	}

	public void claimMessageStatus(Long[] messageIds, String receiverUuid) {

		sendMessage(new MessagePojo(DmsPackingFactory.pack(messageIds), uuid, receiverUuid,
				ContentType.CLAIM_MESSAGE_STATUS, null, null, null));

	}

	public void feedMessageStatus(Map<Long, MessageStatus> messageIdStatusMap, String receiverUuid) {

		sendMessage(new MessagePojo(DmsPackingFactory.pack(messageIdStatusMap), uuid, receiverUuid,
				ContentType.FEED_MESSAGE_STATUS, null, null, null));

	}

	public void feedGroupMessageStatus(Map<Long, GroupMessageStatus> messageIdGroupStatusMap, String receiverUuid) {

		sendMessage(new MessagePojo(DmsPackingFactory.pack(messageIdGroupStatusMap), uuid, receiverUuid,
				ContentType.FEED_GROUP_MESSAGE_STATUS, null, null, null));

	}

	public void claimStatusReport(Long[] messageIds, String receiverUuid) {

		sendMessage(new MessagePojo(DmsPackingFactory.pack(messageIds), uuid, receiverUuid,
				ContentType.CLAIM_STATUS_REPORT, null, null, null));

	}

	public void feedStatusReport(Map<Long, Set<StatusReport>> messageIdStatusReportsMap, String receiverUuid) {

		sendMessage(new MessagePojo(DmsPackingFactory.pack(messageIdStatusReportsMap), null, receiverUuid,
				ContentType.FEED_STATUS_REPORT, null, null, null));

	}

	public void sendTransientMessage(MessageHandleImpl message, Iterable<String> receiverUuids, Long trackingId,
			Long useTimeout, InetAddress useLocalInterface) {

		MessagePojo messagePojo = new MessagePojo(DmsPackingFactory.pack(message), uuid,
				String.join(";", receiverUuids), ContentType.TRANSIENT, trackingId, useTimeout, useLocalInterface);

		FileHandle fileHandle = message.getFileHandle();
		if (fileHandle != null) {
			Path attachment = fileHandle.getPath();
			if (attachment != null) {
				messagePojo.attachment = new AttachmentPojo(attachment);
			}
		}

		sendMessage(messagePojo);

	}

	public void sendTransientMessageStatus(Long trackingId, String receiverUuid) {

		sendMessage(new MessagePojo(DmsPackingFactory.pack(trackingId), uuid, receiverUuid,
				ContentType.FEED_TRANSIENT_STATUS, null, null, null));

	}

	public void cancelTransientMessage(Long trackingId) {

		sendStatuses.forEach(sendStatus -> {
			if (Objects.equals(sendStatus.trackingId, trackingId) && sendStatus.contentType == ContentType.TRANSIENT) {
				sendStatus.status.set(false);
			}
		});
		sendMessage(new MessagePojo(null, uuid, null, ContentType.CANCEL_TRANSIENT, trackingId, null, null));

	}

	public void sendDownloadRequest(DownloadPojo downloadPojo, String receiverUuid) {

		sendMessage(new MessagePojo(DmsPackingFactory.pack(downloadPojo), uuid, receiverUuid,
				ContentType.DOWNLOAD_REQUEST, null, null, null));

	}

	public void cancelDownloadRequest(Long downloadId, String receiverUuid) {

		sendMessage(new MessagePojo(DmsPackingFactory.pack(downloadId), uuid, receiverUuid,
				ContentType.CANCEL_DOWNLOAD_REQUEST, null, null, null));

	}

	public void sendServerNotFound(Long downloadId, String receiverUuid) {

		sendMessage(new MessagePojo(DmsPackingFactory.pack(downloadId), null, receiverUuid,
				ContentType.SERVER_NOT_FOUND, null, null, null));

	}

	public void sendFileNotFound(Long downloadId, String receiverUuid) {

		sendMessage(new MessagePojo(DmsPackingFactory.pack(downloadId), null, receiverUuid, ContentType.FILE_NOT_FOUND,
				null, null, null));

	}

	public void uploadFile(Path path, String receiverUuid, Long trackingId) {

		MessagePojo messagePojo = new MessagePojo(DmsPackingFactory.pack(path.getFileName().toString()), uuid,
				receiverUuid, ContentType.UPLOAD, trackingId, null, null);
		messagePojo.attachment = new AttachmentPojo(path);
		sendMessage(messagePojo);

	}

	public void cancelUpload(String receiverUuid, Long trackingId) {

		sendStatuses.forEach(sendStatus -> {
			if (Objects.equals(sendStatus.trackingId, trackingId) && sendStatus.contentType == ContentType.UPLOAD) {
				sendStatus.status.set(false);
			}
		});
		sendMessage(new MessagePojo(null, null, receiverUuid, ContentType.CANCEL_UPLOAD, trackingId, null, null));

	}

	private void sendMessage(MessagePojo messagePojo) {
		final SendStatus sendStatus = new SendStatus(messagePojo.trackingId, messagePojo.contentType);
		sendStatuses.add(sendStatus);
		MessageContainer messageContainer = new MessageContainer(messageCounter.getAndIncrement(), messagePojo,
				sendStatus.status, progress -> {
					if (progress < 0 || progress == 100) {
						sendStatuses.remove(sendStatus);
					}
					if (progress < 0 && messagePojo.contentType == ContentType.TRANSIENT) {
						listener.progressTransientReceived(messagePojo.trackingId, messagePojo.receiverUuid.split(";"),
								progress);
					}
				});
		stopMap.put(messageContainer.messageNumber, messageContainer);
		messageQueue.offer(messageContainer);
		raiseSignal();
	}

	private void raiseSignal() {
		signalQueue.offer(SIGNAL);
	}

	private void dealer() {

		try (ZMQ.Socket dealerSocket = context.createSocket(SocketType.DEALER);
				ZMQ.Socket inprocSocket = context.createSocket(SocketType.PAIR)) {

			inprocSocket.bind("inproc://dealer");

			dealerSocket.monitor("inproc://monitor", ZMQ.EVENT_HANDSHAKE_PROTOCOL | ZMQ.EVENT_DISCONNECTED);

			dealerSocket.setIdentity(uuid.getBytes(ZMQ.CHARSET));
			dealerSocket.setSndHWM(0);
			dealerSocket.setImmediate(false);
			dealerSocket.connect("tcp://" + serverIp + ":" + dealerPort);

			ZMQ.Poller poller = context.createPoller(2);
			int pollDealer = poller.register(dealerSocket, ZMQ.Poller.POLLIN);
			int pollInproc = poller.register(inprocSocket, ZMQ.Poller.POLLIN);

			while (!Thread.currentThread().isInterrupted()) {

				poller.poll();

				if (poller.pollin(pollDealer)) {

					int messageNumber = Integer.parseInt(dealerSocket.recvStr(ZMQ.DONTWAIT));
					if (!dealerSocket.hasReceiveMore()) {
						raiseSignal();
						continue;
					}
					byte[] receivedMessage = dealerSocket.recv(ZMQ.DONTWAIT);
					taskQueue.execute(() -> messageReceiver.inFeed(messageNumber, receivedMessage));
					dealerSocket.send(String.valueOf(0), ZMQ.DONTWAIT); // "Send more" signal

				} else if (poller.pollin(pollInproc)) {

					inprocSocket.recv(ZMQ.DONTWAIT);
					MessageContainer messageContainer = messageQueue.poll();

					if (messageContainer == null) {
						continue;
					}

					Chunk chunk = messageContainer.next();
					if (chunk == null) {
						raiseSignal(); // Pass the signal
					} else {
						dealerSocket.send(String.valueOf(messageContainer.messageNumber), ZMQ.SNDMORE | ZMQ.DONTWAIT);
						dealerSocket.sendByteBuffer(chunk.dataBuffer, ZMQ.DONTWAIT);
						messageContainer.progressPercent.set(chunk.progress);
					}

					synchronized (serverConnected) {
						if (serverConnected.get() && messageContainer.hasMore()) {
							messageQueue.offerFirst(messageContainer);
						} else {
							closeMessage(messageContainer);
						}
					}

				}

			}

		}

	}

	private void inproc() {

		try (ZMQ.Socket inprocSocket = context.createSocket(SocketType.PAIR)) {

			inprocSocket.connect("inproc://dealer");

			while (!Thread.currentThread().isInterrupted()) {

				inprocSocket.send(signalQueue.take());

			}

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	private void monitor() {

		try (ZMQ.Socket monitorSocket = context.createSocket(SocketType.PAIR)) {

			monitorSocket.connect("inproc://monitor");

			while (!Thread.currentThread().isInterrupted()) {

				ZMQ.Event event = ZMQ.Event.recv(monitorSocket);

				switch (event.getEvent()) {

				case ZMQ.EVENT_HANDSHAKE_PROTOCOL:
					serverConnected.set(true);
					listener.serverConnStatusUpdated(serverConnected.get());
					break;

				case ZMQ.EVENT_DISCONNECTED:
					serverConnected.set(false);
					close();
					listener.serverConnStatusUpdated(serverConnected.get());
					break;

				}

			}

		}

	}

	private void processIncomingMessage(MessagePojo messagePojo) {

		try {

			if (Objects.equals(uuid, messagePojo.senderUuid))
				return;

			byte[] payload = messagePojo.payload;

			switch (messagePojo.contentType) {

			case BCON:

				listener.beaconReceived(DmsPackingFactory.unpack(payload, Beacon.class));

				break;

			case IPS:

				listener.remoteIpsReceived(DmsPackingFactory.unpack(payload, InetAddress[].class));

				break;

			case PROGRESS_MESSAGE:

				listener.progressMessageReceived(messagePojo.trackingId, messagePojo.senderUuid.split(";"),
						DmsPackingFactory.unpack(payload, Integer.class));

				break;

			case PROGRESS_TRANSIENT:

				listener.progressTransientReceived(messagePojo.trackingId, messagePojo.senderUuid.split(";"),
						DmsPackingFactory.unpack(payload, Integer.class));

				break;

			case MESSAGE:

				Message message = DmsPackingFactory.unpack(payload, Message.class);
				message.setMessageRefId(messagePojo.trackingId);
				listener.messageReceived(message, messagePojo.getAttachmentLink(), messagePojo.senderUuid);

				break;

			case UUID_DISCONNECTED:

				listener.userDisconnected(messagePojo.senderUuid);

				break;

			case CLAIM_MESSAGE_STATUS:

				listener.messageStatusClaimed(DmsPackingFactory.unpack(payload, Long[].class), messagePojo.senderUuid);

				break;

			case FEED_MESSAGE_STATUS:

				listener.messageStatusFed(DmsPackingFactory.unpackMap(payload, Long.class, MessageStatus.class),
						messagePojo.senderUuid);

				break;

			case FEED_GROUP_MESSAGE_STATUS:

				listener.groupMessageStatusFed(
						DmsPackingFactory.unpackMap(payload, Long.class, GroupMessageStatus.class),
						messagePojo.senderUuid);

				break;

			case CLAIM_STATUS_REPORT:

				listener.statusReportClaimed(DmsPackingFactory.unpack(payload, Long[].class), messagePojo.senderUuid);

				break;

			case FEED_STATUS_REPORT:

				listener.statusReportFed(DmsPackingFactory.unpackMap(payload, Long.class, StatusReport[].class));

				break;

			case TRANSIENT:

				listener.transientMessageReceived(DmsPackingFactory.unpack(payload, MessageHandleImpl.class),
						messagePojo.getAttachmentLink(), messagePojo.senderUuid, messagePojo.trackingId);

				break;

			case FEED_TRANSIENT_STATUS:

				listener.transientMessageStatusReceived(DmsPackingFactory.unpack(payload, Long.class),
						messagePojo.senderUuid);

				break;

			case DOWNLOAD_REQUEST:

				listener.downloadRequested(DmsPackingFactory.unpack(payload, DownloadPojo.class),
						messagePojo.senderUuid);

				break;

			case CANCEL_DOWNLOAD_REQUEST:

				listener.cancelDownloadRequested(DmsPackingFactory.unpack(payload, Long.class), messagePojo.senderUuid);

				break;

			case SERVER_NOT_FOUND:

				listener.serverNotFound(DmsPackingFactory.unpack(payload, Long.class));

				break;

			case FILE_NOT_FOUND:

				listener.fileNotFound(DmsPackingFactory.unpack(payload, Long.class));

				break;

			case PROGRESS_DOWNLOAD:

				listener.downloadingFile(messagePojo.trackingId, DmsPackingFactory.unpack(payload, Integer.class));

				break;

			case UPLOAD:

				listener.fileDownloaded(messagePojo.trackingId, messagePojo.getAttachmentLink(),
						DmsPackingFactory.unpack(payload, String.class));

				break;

			case UPLOAD_FAILURE:

				listener.downloadFailed(messagePojo.trackingId);

				break;

			default:

				break;

			}

		} catch (Exception e) {

		}

	}

	private void stopSending(Integer messageNumber) {
		MessageContainer messageContainer = stopMap.get(messageNumber);
		if (messageContainer == null) {
			return;
		}
		messageContainer.markAsDone();
	}

	private void closeMessage(MessageContainer messageContainer) {
		messageContainer.close();
		stopMap.remove(messageContainer.messageNumber);
	}

	private void close() {
		taskQueue.execute(() -> messageReceiver.deleteResources());
		synchronized (serverConnected) {
			MessageContainer messageContainer;
			while ((messageContainer = messageQueue.poll()) != null) {
				closeMessage(messageContainer);
			}
		}
	}

	@Override
	public void messageReceived(MessagePojo messagePojo) {
		if (messagePojo.contentType == ContentType.SEND_NOMORE) {
			try {
				Integer messageNumber = DmsPackingFactory.unpack(messagePojo.payload, Integer.class);
				stopSending(messageNumber);
			} catch (Exception e) {

			}
			return;
		}
		processIncomingMessage(messagePojo);
	}

	@Override
	public void messageFailed(int messageNumber) {
		sendMessage(new MessagePojo(DmsPackingFactory.pack(messageNumber), null, null, ContentType.SEND_NOMORE, null,
				null, null));
	}

	private final class MessageContainer extends DmsMessageSender {

		private final int messageNumber;
		public final AtomicBoolean sendStatus;
		private final Consumer<Integer> progressConsumer;

		private final AtomicInteger progressPercent = new AtomicInteger(-1);

		private MessageContainer(int messageNumber, MessagePojo messagePojo, AtomicBoolean sendStatus,
				Consumer<Integer> progressConsumer) {
			super(messagePojo, Direction.CLIENT_TO_SERVER);
			this.messageNumber = messageNumber;
			this.sendStatus = sendStatus;
			this.progressConsumer = progressConsumer;
		}

		@Override
		public Chunk next() {
			health.set((sendStatus == null || sendStatus.get()) && serverConnected.get());
			return super.next();
		}

		@Override
		public void close() {
			super.close();
			if (progressConsumer != null && progressPercent.get() < 100) {
				progressConsumer.accept(-1);
			}
		}

	}

	private class SendStatus {

		private final Long trackingId;
		private final ContentType contentType;
		private final AtomicBoolean status = new AtomicBoolean(true);

		private SendStatus(Long trackingId, ContentType contentType) {
			this.trackingId = trackingId;
			this.contentType = contentType;
		}

	}

}
