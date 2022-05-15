package com.ogya.dms.core.dmsclient;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.ogya.dms.commons.DmsPackingFactory;
import com.ogya.dms.commons.structures.AttachmentPojo;
import com.ogya.dms.commons.structures.Beacon;
import com.ogya.dms.commons.structures.ContentType;
import com.ogya.dms.commons.structures.MessagePojo;
import com.ogya.dms.core.common.CommonConstants;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.database.tables.StatusReport;
import com.ogya.dms.core.dmsclient.DmsMessageReceiver.DmsMessageReceiverListener;
import com.ogya.dms.core.dmsclient.DmsMessageSender.Chunk;
import com.ogya.dms.core.dmsclient.intf.DmsClientListener;
import com.ogya.dms.core.factory.DmsFactory;
import com.ogya.dms.core.intf.handles.FileHandle;
import com.ogya.dms.core.intf.handles.impl.MessageHandleImpl;
import com.ogya.dms.core.structures.DownloadPojo;
import com.ogya.dms.core.structures.GroupMessageStatus;
import com.ogya.dms.core.structures.MessageStatus;

public class DmsClient implements DmsMessageReceiverListener {

	private static final String LOCAL_SERVER = "";

	private final String uuid;

	private final ZContext context = new ZContext();

	private final String serverIp;
	private final int dealerPort;

	private final AtomicBoolean serverConnected = new AtomicBoolean(false);

	private final DmsClientListener listener;

	private final ExecutorService taskQueue = DmsFactory.newSingleThreadExecutorService();
	private final Comparator<MessageContainer> messageSorter = new Comparator<MessageContainer>() {
		@Override
		public int compare(MessageContainer m1, MessageContainer m2) {
			int result = Boolean.compare(m1.isSecondary(), m2.isSecondary());
			if (result == 0) {
				result = Long.compare(m1.checkInTime, m2.checkInTime);
			}
			if (result == 0) {
				result = Integer.compare(m1.messageNumber, m2.messageNumber);
			}
			return result;
		}
	};
	private final Map<String, String> userServerMap = Collections.synchronizedMap(new HashMap<String, String>());
	private final Map<String, PriorityQueue<MessageContainer>> serverMessageMap = Collections
			.synchronizedMap(new HashMap<String, PriorityQueue<MessageContainer>>());
	private final DmsMessageReceiver messageReceiver;
	private final AtomicInteger messageCounter = new AtomicInteger(1);
	private final LinkedBlockingQueue<String> signalQueue = new LinkedBlockingQueue<String>();
	private final List<SendStatus> sendStatuses = Collections.synchronizedList(new ArrayList<SendStatus>());
//	private final Map<Integer, MessageContainer> stopMap = Collections
//			.synchronizedMap(new HashMap<Integer, MessageContainer>());

	public DmsClient(String uuid, String commIp, int commPort, DmsClientListener listener) {

		this.uuid = uuid;

		this.serverIp = commIp;
		this.dealerPort = commPort;

		this.listener = listener;

		this.messageReceiver = new DmsMessageReceiver(this);
		this.messageReceiver.setKeepDownloads(true);

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

		sendMessage(DmsPackingFactory.pack(beacon), null, null, ContentType.BCON, null, null, null, null);

	}

	public void claimStartInfo() {

		sendMessage(null, uuid, null, ContentType.REQ_STRT, null, null, null, null);

	}

	public void addRemoteIps(InetAddress... ips) {

		sendMessage(DmsPackingFactory.pack(ips), null, null, ContentType.ADD_IPS, null, null, null, null);

	}

	public void removeRemoteIps(InetAddress... ips) {

		sendMessage(DmsPackingFactory.pack(ips), null, null, ContentType.REMOVE_IPS, null, null, null, null);

	}

	public void sendMessage(Message message, Path attachmentPath, List<String> receiverUuids, Long messageId) {

		AttachmentPojo attachment = null;
		if (attachmentPath != null) {
			attachment = new AttachmentPojo(attachmentPath);
		}

		sendMessage(DmsPackingFactory.pack(message), uuid, receiverUuids, ContentType.MESSAGE, messageId, null, null,
				attachment);

	}

	public void cancelMessage(Long trackingId) {

		sendStatuses.forEach(sendStatus -> {
			if (Objects.equals(sendStatus.trackingId, trackingId) && sendStatus.contentType == ContentType.MESSAGE) {
				sendStatus.status.set(false);
			}
		});

	}

	public void claimMessageStatus(Long[] messageIds, String receiverUuid) {

		sendMessage(DmsPackingFactory.pack(messageIds), uuid, Arrays.asList(receiverUuid),
				ContentType.CLAIM_MESSAGE_STATUS, null, null, null, null);

	}

	public void feedMessageStatus(Map<Long, MessageStatus> messageIdStatusMap, String receiverUuid) {

		sendMessage(DmsPackingFactory.pack(messageIdStatusMap), uuid, Arrays.asList(receiverUuid),
				ContentType.FEED_MESSAGE_STATUS, null, null, null, null);

	}

	public void feedGroupMessageStatus(Map<Long, GroupMessageStatus> messageIdGroupStatusMap, String receiverUuid) {

		sendMessage(DmsPackingFactory.pack(messageIdGroupStatusMap), uuid, Arrays.asList(receiverUuid),
				ContentType.FEED_GROUP_MESSAGE_STATUS, null, null, null, null);

	}

	public void claimStatusReport(Long[] messageIds, String receiverUuid) {

		sendMessage(DmsPackingFactory.pack(messageIds), uuid, Arrays.asList(receiverUuid),
				ContentType.CLAIM_STATUS_REPORT, null, null, null, null);

	}

	public void feedStatusReport(Map<Long, Set<StatusReport>> messageIdStatusReportsMap, String receiverUuid) {

		sendMessage(DmsPackingFactory.pack(messageIdStatusReportsMap), null, Arrays.asList(receiverUuid),
				ContentType.FEED_STATUS_REPORT, null, null, null, null);

	}

	public void sendTransientMessage(MessageHandleImpl message, List<String> receiverUuids, Long trackingId,
			Long useTimeout, InetAddress useLocalInterface) {

		AttachmentPojo attachment = null;
		FileHandle fileHandle = message.getFileHandle();
		if (fileHandle != null) {
			Path attachmentPath = fileHandle.getPath();
			if (attachmentPath != null) {
				attachment = new AttachmentPojo(attachmentPath);
			}
		}

		sendMessage(DmsPackingFactory.pack(message), uuid, receiverUuids, ContentType.TRANSIENT, trackingId, useTimeout,
				useLocalInterface, attachment);

	}

	public void sendTransientMessageStatus(Long trackingId, String receiverUuid) {

		sendMessage(DmsPackingFactory.pack(trackingId), uuid, Arrays.asList(receiverUuid),
				ContentType.FEED_TRANSIENT_STATUS, null, null, null, null);

	}

	public void cancelTransientMessage(Long trackingId) {

		sendStatuses.forEach(sendStatus -> {
			if (Objects.equals(sendStatus.trackingId, trackingId) && sendStatus.contentType == ContentType.TRANSIENT) {
				sendStatus.status.set(false);
			}
		});

	}

	public void sendDownloadRequest(DownloadPojo downloadPojo, String receiverUuid) {

		sendMessage(DmsPackingFactory.pack(downloadPojo), uuid, Arrays.asList(receiverUuid),
				ContentType.DOWNLOAD_REQUEST, null, null, null, null);

	}

	public void cancelDownloadRequest(Long downloadId, String receiverUuid) {

		sendMessage(DmsPackingFactory.pack(downloadId), uuid, Arrays.asList(receiverUuid),
				ContentType.CANCEL_DOWNLOAD_REQUEST, null, null, null, null);

	}

	public void sendServerNotFound(Long downloadId, String receiverUuid) {

		sendMessage(DmsPackingFactory.pack(downloadId), null, Arrays.asList(receiverUuid), ContentType.SERVER_NOT_FOUND,
				null, null, null, null);

	}

	public void sendFileNotFound(Long downloadId, String receiverUuid) {

		sendMessage(DmsPackingFactory.pack(downloadId), null, Arrays.asList(receiverUuid), ContentType.FILE_NOT_FOUND,
				null, null, null, null);

	}

	public void uploadFile(Path path, String receiverUuid, Long trackingId, Long position) {

		AttachmentPojo attachment = new AttachmentPojo(path);
		attachment.position = position;
		sendMessage(DmsPackingFactory.pack(path.getFileName().toString()), uuid, Arrays.asList(receiverUuid),
				ContentType.UPLOAD, trackingId, null, null, attachment);

	}

	public void cancelUpload(String receiverUuid, Long trackingId) {

		sendStatuses.forEach(sendStatus -> {
			if (Objects.equals(sendStatus.trackingId, trackingId) && sendStatus.contentType == ContentType.UPLOAD) {
				sendStatus.status.set(false);
			}
		});

	}

	private void sendMessage(byte[] payload, String senderUuid, List<String> receiverUuids, ContentType contentType,
			Long trackingId, Long useTimeout, InetAddress useLocalAddress, AttachmentPojo attachment) {
		if (receiverUuids == null) {
			// Message destined to the local server
			String serverUuid = LOCAL_SERVER;
			MessagePojo messagePojo = new MessagePojo(payload, senderUuid, null, serverUuid, contentType, null, null);
			MessageContainer messageContainer = new MessageContainer(messagePojo, useTimeout, null, null);
			PriorityQueue<MessageContainer> messageQueue = serverMessageMap.get(serverUuid);
			if (messageQueue == null) {
				messageQueue = new PriorityQueue<MessageContainer>(11, messageSorter);
				serverMessageMap.put(serverUuid, messageQueue);
			}
			messageQueue.offer(messageContainer);
			raiseSignal(serverUuid);
			return;
		}
		Map<String, List<String>> serverReceiversMap = new HashMap<String, List<String>>();
		List<String> unsuccessfulUuids = new ArrayList<String>();
		for (String receiverUuid : receiverUuids) {
			String serverUuid = userServerMap.get(receiverUuid);
			if (serverUuid == null) {
				unsuccessfulUuids.add(receiverUuid);
			} else {
				serverReceiversMap.putIfAbsent(serverUuid, new ArrayList<String>());
				serverReceiversMap.get(serverUuid).add(receiverUuid);
			}
		}
		if (!unsuccessfulUuids.isEmpty() && contentType == ContentType.TRANSIENT) {
			sendProgress(unsuccessfulUuids, -1, trackingId, contentType);
		}
		serverReceiversMap.forEach((serverUuid, uuidList) -> {
			final SendStatus sendStatus = new SendStatus(trackingId, contentType);
			sendStatus.receiverUuids.addAll(uuidList);
			sendStatuses.add(sendStatus);
			MessagePojo messagePojo = new MessagePojo(payload, senderUuid, uuidList, serverUuid, contentType,
					trackingId, useLocalAddress);
			messagePojo.attachment = attachment;
			MessageContainer messageContainer = new MessageContainer(messagePojo, useTimeout, sendStatus.status,
					progress -> {
						if (progress < 0 || progress == 100) {
							sendStatuses.remove(sendStatus);
						}
						sendProgress(uuidList, progress, trackingId, contentType);
					});
			PriorityQueue<MessageContainer> messageQueue = serverMessageMap.get(serverUuid);
			if (messageQueue == null) {
				messageQueue = new PriorityQueue<MessageContainer>(11, messageSorter);
				serverMessageMap.put(serverUuid, messageQueue);
			}
			messageQueue.offer(messageContainer);
			raiseSignal(serverUuid);
		});
	}

	private int getMessageNumber(MessagePojo messagePojo) {
		try {
			if (messagePojo.attachment == null || Files.size(messagePojo.attachment.path) == 0) {
				return 0;
			}
		} catch (Exception e) {
			return 0;
		}
		int messageNumber = messageCounter.getAndIncrement();
		if (messageCounter.get() < 0) {
			messageCounter.set(1);
		}
		return messageNumber;
	}

	private void sendProgress(List<String> remoteUuids, int progress, Long trackingId, ContentType contentType) {

		if (trackingId == null) {
			return;
		}

		switch (contentType) {
		case MESSAGE: {
			listener.progressMessageReceived(trackingId, remoteUuids, progress);
			break;
		}
		case TRANSIENT: {
			if (progress < 0) {
				listener.progressTransientReceived(trackingId, remoteUuids, progress);
			}
			break;
		}
		default: {
			break;
		}
		}

	}

	private void raiseSignal(String serverUuid) {
		try {
			signalQueue.put(serverUuid);
		} catch (InterruptedException e) {

		}
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

					String head = dealerSocket.recvStr(ZMQ.DONTWAIT);
					if (!dealerSocket.hasReceiveMore()) {
						String serverUuid = head;
						raiseSignal(serverUuid);
						continue;
					}
					byte[] receivedMessage = dealerSocket.recv(ZMQ.DONTWAIT);
					try {
						int messageNumber = Integer.parseInt(head);
						taskQueue.execute(() -> messageReceiver.inFeed(messageNumber, receivedMessage));
					} catch (Exception e) {

					}

				} else if (poller.pollin(pollInproc)) {

					String serverUuid = inprocSocket.recvStr(ZMQ.DONTWAIT);
					PriorityQueue<MessageContainer> messageQueue = serverMessageMap.get(serverUuid);
					if (messageQueue == null) {
						continue;
					}
					MessageContainer messageContainer = messageQueue.poll();

					if (messageContainer == null) {
						continue;
					}

					Chunk chunk = messageContainer.next();
					if (chunk == null) {
						raiseSignal(serverUuid); // Pass the signal
					} else {
						int messageNumber = messageContainer.messageNumber;
						if (!messageContainer.hasMore()) {
							messageNumber = -messageNumber;
						}
						dealerSocket.send(String.valueOf(messageNumber), ZMQ.SNDMORE | ZMQ.DONTWAIT);
						dealerSocket.sendByteBuffer(chunk.dataBuffer, ZMQ.DONTWAIT);
						boolean progressUpdated = chunk.progress > messageContainer.progressPercent
								.getAndSet(chunk.progress);
						if (progressUpdated && messageContainer.progressConsumer != null) {
							messageContainer.progressConsumer.accept(chunk.progress);
						}
					}

					synchronized (serverConnected) {
						if (serverConnected.get() && messageContainer.hasMore()) {
							messageQueue.offer(messageContainer);
						} else {
							messageContainer.close();
						}
					}

					if (messageQueue.isEmpty()) {
						serverMessageMap.remove(serverUuid);
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

				Beacon beacon = DmsPackingFactory.unpack(payload, Beacon.class);
				userServerMap.put(beacon.uuid, beacon.serverUuid);
				listener.beaconReceived(beacon);

				break;

			case IPS:

				listener.remoteIpsReceived(DmsPackingFactory.unpack(payload, InetAddress[].class));

				break;

			case MESSAGE:

				Message message = DmsPackingFactory.unpack(payload, Message.class);
				message.setMessageRefId(messagePojo.trackingId);
				listener.messageReceived(message, messagePojo.getAttachmentLink(), messagePojo.senderUuid);

				break;

			case UUID_DISCONNECTED:

				userServerMap.remove(messagePojo.senderUuid);
				// TODO: Cancel messages, update progresses
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

			case UPLOAD:

				listener.fileDownloaded(messagePojo.trackingId, messagePojo.getAttachmentLink(),
						DmsPackingFactory.unpack(payload, String.class), messagePojo.isAttachmentPartial());

				break;

			default:

				break;

			}

		} catch (Exception e) {

		}

	}

	private void stopSending(Integer messageNumber) {
//		MessageContainer messageContainer = stopMap.get(messageNumber);
//		if (messageContainer == null) {
//			return;
//		}
//		messageContainer.markAsDone();
	}

//	private void closeMessage(MessageContainer messageContainer) {
//		messageContainer.close();
//		stopMap.remove(messageContainer.messageNumber);
//	}

	private void close() {
		taskQueue.execute(() -> messageReceiver.interruptAll());
		synchronized (serverConnected) {
			serverMessageMap.forEach((serverUuid, messageQueue) -> {
				while (!messageQueue.isEmpty()) {
					messageQueue.poll().close();
				}
			});
			serverMessageMap.clear();
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
		sendMessage(DmsPackingFactory.pack(messageNumber), null, null, ContentType.SEND_NOMORE, null, null, null, null);
	}

	@Override
	public void downloadProgress(Long trackingId, int progress) {
		// TODO Auto-generated method stub
		listener.downloadingFile(trackingId, progress);
	}

	@Override
	public void downloadFailed(Long trackingId) {
		// TODO Auto-generated method stub
		listener.downloadFailed(trackingId);
	}

	private final class MessageContainer extends DmsMessageSender {

		private final int messageNumber;
		private final AtomicBoolean sendStatus;
		private final Consumer<Integer> progressConsumer;
		private final Long useTimeout;

		private final boolean bigFile;
		private final long startTime = System.currentTimeMillis();
		private final AtomicInteger progressPercent = new AtomicInteger(-1);
		private long checkInTime = startTime;

		private MessageContainer(MessagePojo messagePojo, Long useTimeout, AtomicBoolean sendStatus,
				Consumer<Integer> progressConsumer) {
			super(messagePojo);
			this.messageNumber = getMessageNumber(messagePojo);
			this.sendStatus = sendStatus;
			this.progressConsumer = progressConsumer;
			this.useTimeout = useTimeout;
			this.bigFile = getFileSize() > CommonConstants.SMALL_FILE_LIMIT;
		}

		private boolean isSecondary() {
			return bigFile && health.get();
		}

		@Override
		public Chunk next() {
			checkInTime = System.currentTimeMillis();
			health.set((sendStatus == null || sendStatus.get())
					&& (useTimeout == null || checkInTime - startTime < useTimeout) && serverConnected.get());
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
		private final Set<String> receiverUuids = Collections.synchronizedSet(new HashSet<String>());

		private SendStatus(Long trackingId, ContentType contentType) {
			this.trackingId = trackingId;
			this.contentType = contentType;
		}

	}

}
