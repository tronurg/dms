package com.ogya.dms.core.dmsclient;

import java.net.InetAddress;
import java.nio.ByteBuffer;
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
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.ogya.dms.commons.DmsPackingFactory;
import com.ogya.dms.commons.structures.Beacon;
import com.ogya.dms.commons.structures.ContentType;
import com.ogya.dms.commons.structures.MessagePojo;
import com.ogya.dms.core.common.CommonConstants;
import com.ogya.dms.core.common.DmsMessageReceiver;
import com.ogya.dms.core.common.DmsMessageReceiver.DmsMessageReceiverListener;
import com.ogya.dms.core.common.DmsMessageSender;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.database.tables.StatusReport;
import com.ogya.dms.core.dmsclient.intf.DmsClientListener;
import com.ogya.dms.core.factory.DmsFactory;
import com.ogya.dms.core.intf.handles.FileHandle;
import com.ogya.dms.core.intf.handles.impl.MessageHandleImpl;
import com.ogya.dms.core.structures.AttachmentPojo;
import com.ogya.dms.core.structures.Chunk;
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
			if (result == 0 && m1.isSecondary()) {
				result = Long.compare(m1.checkInTime, m2.checkInTime);
			}
			if (result == 0) {
				result = Integer.compare(m1.messageNumber, m2.messageNumber);
			}
			return result;
		}
	};
	private final Map<String, DmsServer> dmsServers = Collections.synchronizedMap(new HashMap<String, DmsServer>());
	private final Map<String, DmsServer> userServerMap = Collections.synchronizedMap(new HashMap<String, DmsServer>());
	private final DmsMessageReceiver messageReceiver;
	private final AtomicInteger attachmentCounter = new AtomicInteger(1);
	private final LinkedBlockingQueue<String> signalQueue = new LinkedBlockingQueue<String>();
	private final List<SendStatus> sendStatuses = Collections.synchronizedList(new ArrayList<SendStatus>());

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

		AttachmentPojo attachmentPojo = null;
		if (attachmentPath != null) {
			attachmentPojo = new AttachmentPojo(attachmentPath, null);
		}

		sendMessage(DmsPackingFactory.pack(message), uuid, receiverUuids, ContentType.MESSAGE, messageId, null, null,
				attachmentPojo);

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

		AttachmentPojo attachmentPojo = null;
		FileHandle fileHandle = message.getFileHandle();
		if (fileHandle != null) {
			Path attachmentPath = fileHandle.getPath();
			if (attachmentPath != null) {
				attachmentPojo = new AttachmentPojo(attachmentPath, null);
			}
		}

		sendMessage(DmsPackingFactory.pack(message), uuid, receiverUuids, ContentType.TRANSIENT, trackingId, useTimeout,
				useLocalInterface, attachmentPojo);

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

		AttachmentPojo attachmentPojo = new AttachmentPojo(path, position);
		sendMessage(DmsPackingFactory.pack(path.getFileName().toString()), uuid, Arrays.asList(receiverUuid),
				ContentType.UPLOAD, trackingId, null, null, attachmentPojo);

	}

	public void cancelUpload(String receiverUuid, Long trackingId) {

		sendStatuses.forEach(sendStatus -> {
			if (Objects.equals(sendStatus.trackingId, trackingId) && sendStatus.contentType == ContentType.UPLOAD) {
				sendStatus.status.set(false);
			}
		});

	}

	private void sendMessage(byte[] payload, String senderUuid, List<String> receiverUuids, ContentType contentType,
			Long trackingId, Long useTimeout, InetAddress useLocalAddress, AttachmentPojo attachmentPojo) {
		if (receiverUuids == null) {
			// Message destined to the local server
			MessagePojo messagePojo = new MessagePojo(payload, senderUuid, null, null, contentType, null, null);
			MessageContainer messageContainer = new MessageContainer(messagePojo, attachmentPojo, useTimeout, null,
					null);
			DmsServer dmsServer = dmsServers.get(LOCAL_SERVER);
			if (dmsServer == null) {
				dmsServer = new DmsServer(LOCAL_SERVER);
				dmsServers.put(LOCAL_SERVER, dmsServer);
			}
			dmsServer.queueMessage(messageContainer);
			return;
		}
		Map<DmsServer, List<String>> serverReceiversMap = new HashMap<DmsServer, List<String>>();
		List<String> unsuccessfulUuids = new ArrayList<String>();
		for (String receiverUuid : receiverUuids) {
			DmsServer dmsServer = userServerMap.get(receiverUuid);
			if (dmsServer == null) {
				unsuccessfulUuids.add(receiverUuid);
			} else {
				serverReceiversMap.putIfAbsent(dmsServer, new ArrayList<String>());
				serverReceiversMap.get(dmsServer).add(receiverUuid);
			}
		}
		if (!unsuccessfulUuids.isEmpty() && contentType == ContentType.TRANSIENT) {
			sendProgress(unsuccessfulUuids, -1, trackingId, contentType);
		}
		serverReceiversMap.forEach((dmsServer, uuidList) -> {
			final SendStatus sendStatus = new SendStatus(trackingId, contentType);
			sendStatuses.add(sendStatus);
			MessagePojo messagePojo = new MessagePojo(payload, senderUuid, uuidList, dmsServer.uuid, contentType,
					trackingId, useLocalAddress);
			MessageContainer messageContainer = new MessageContainer(messagePojo, attachmentPojo, useTimeout,
					sendStatus.status, (progress, uuids) -> {
						if (progress < 0 || progress == 100) {
							sendStatuses.remove(sendStatus);
						}
						sendProgress(uuids, progress, trackingId, contentType);
					});
			dmsServer.queueMessage(messageContainer);
		});
	}

	private int getMessageNumber(boolean messageOnly) {
		if (messageOnly) {
			return 0;
		}
		int messageNumber = attachmentCounter.getAndIncrement();
		if (attachmentCounter.get() < 0) {
			attachmentCounter.set(1);
		}
		return messageNumber;
	}

	private void sendProgress(List<String> remoteUuids, int progress, Long trackingId, ContentType contentType) {

		if (trackingId == null) {
			return;
		}

		if (remoteUuids == null || remoteUuids.isEmpty()) {
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
					DmsServer dmsServer = dmsServers.get(serverUuid);
					if (dmsServer == null) {
						continue;
					}

					dmsServer.sendMore((messageNumber, dataBuffer) -> {
						dealerSocket.send(String.valueOf(messageNumber), ZMQ.SNDMORE | ZMQ.DONTWAIT);
						dealerSocket.sendByteBuffer(dataBuffer, ZMQ.DONTWAIT);
					});

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
					updateServerConnStatus(true);
					break;

				case ZMQ.EVENT_DISCONNECTED:
					updateServerConnStatus(false);
					break;

				}

			}

		}

	}

	private void sendNoMore(int messageNumber, String receiverUuid, String receiverAddress) {
		if (receiverAddress == null) {
			dmsServers.forEach((serverUuid, dmsServer) -> dmsServer.sendNoMore(messageNumber, receiverUuid));
			return;
		}
		DmsServer dmsServer = dmsServers.get(receiverAddress);
		if (dmsServer == null) {
			return;
		}
		dmsServer.sendNoMore(messageNumber, receiverUuid);
	}

	private void updateServerConnStatus(boolean serverConnStatus) {

		serverConnected.set(serverConnStatus);
		listener.serverConnStatusUpdated(serverConnStatus);

		if (serverConnStatus) {
			return;
		}

		taskQueue.execute(() -> messageReceiver.close());
		userServerMap.clear();
		dmsServers.forEach((serverUuid, dmsServer) -> dmsServer.close());
		dmsServers.clear();

	}

	@Override
	public void messageReceived(MessagePojo messagePojo, Path attachment, boolean partial) {

		try {

			if (Objects.equals(uuid, messagePojo.senderUuid))
				return;

			byte[] payload = messagePojo.payload;

			switch (messagePojo.contentType) {

			case BCON: {

				Beacon beacon = DmsPackingFactory.unpack(payload, Beacon.class);
				String userUuid = beacon.uuid;
				String serverUuid = beacon.serverUuid;
				if (serverUuid == null) {
					break;
				}
				DmsServer dmsServer = dmsServers.get(serverUuid);
				if (dmsServer == null) {
					dmsServer = new DmsServer(serverUuid);
					dmsServers.put(serverUuid, dmsServer);
				}
				dmsServer.addUser(userUuid);
				listener.beaconReceived(beacon);

				break;

			}

			case IPS: {

				listener.remoteIpsReceived(DmsPackingFactory.unpack(payload, InetAddress[].class));

				break;

			}

			case MESSAGE: {

				Message message = DmsPackingFactory.unpack(payload, Message.class);
				message.setMessageRefId(messagePojo.trackingId);
				listener.messageReceived(message, attachment, messagePojo.senderUuid);

				break;

			}

			case UUID_DISCONNECTED: {

				String userUuid = messagePojo.senderUuid;
				messageReceiver.closeMessagesFrom(userUuid);
				listener.userDisconnected(userUuid);
				DmsServer dmsServer = userServerMap.remove(userUuid);
				if (dmsServer == null) {
					break;
				}
				dmsServer.removeUser(userUuid);

				break;

			}

			case CLAIM_MESSAGE_STATUS: {

				listener.messageStatusClaimed(DmsPackingFactory.unpack(payload, Long[].class), messagePojo.senderUuid);

				break;

			}

			case FEED_MESSAGE_STATUS: {

				listener.messageStatusFed(DmsPackingFactory.unpackMap(payload, Long.class, MessageStatus.class),
						messagePojo.senderUuid);

				break;

			}

			case FEED_GROUP_MESSAGE_STATUS: {

				listener.groupMessageStatusFed(
						DmsPackingFactory.unpackMap(payload, Long.class, GroupMessageStatus.class),
						messagePojo.senderUuid);

				break;

			}

			case CLAIM_STATUS_REPORT: {

				listener.statusReportClaimed(DmsPackingFactory.unpack(payload, Long[].class), messagePojo.senderUuid);

				break;

			}

			case FEED_STATUS_REPORT: {

				listener.statusReportFed(DmsPackingFactory.unpackMap(payload, Long.class, StatusReport[].class));

				break;

			}

			case TRANSIENT: {

				listener.transientMessageReceived(DmsPackingFactory.unpack(payload, MessageHandleImpl.class),
						attachment, messagePojo.senderUuid, messagePojo.trackingId);

				break;

			}

			case FEED_TRANSIENT_STATUS: {

				listener.transientMessageStatusReceived(DmsPackingFactory.unpack(payload, Long.class),
						messagePojo.senderUuid);

				break;

			}

			case DOWNLOAD_REQUEST: {

				listener.downloadRequested(DmsPackingFactory.unpack(payload, DownloadPojo.class),
						messagePojo.senderUuid);

				break;

			}

			case CANCEL_DOWNLOAD_REQUEST: {

				listener.cancelDownloadRequested(DmsPackingFactory.unpack(payload, Long.class), messagePojo.senderUuid);

				break;

			}

			case SERVER_NOT_FOUND: {

				listener.serverNotFound(DmsPackingFactory.unpack(payload, Long.class));

				break;

			}

			case FILE_NOT_FOUND: {

				listener.fileNotFound(DmsPackingFactory.unpack(payload, Long.class));

				break;

			}

			case UPLOAD: {

				listener.fileDownloaded(messagePojo.trackingId, attachment,
						DmsPackingFactory.unpack(payload, String.class), partial);

				break;

			}

			case SEND_NOMORE: {

				int messageNumber = DmsPackingFactory.unpack(messagePojo.payload, Integer.class);
				sendNoMore(messageNumber, messagePojo.senderUuid, messagePojo.address);

				break;

			}

			default:

				break;

			}

		} catch (Exception e) {

		}

	}

	@Override
	public void messageFailed(int messageNumber) {
		sendMessage(DmsPackingFactory.pack(messageNumber), null, null, ContentType.SEND_NOMORE, null, null, null, null);
	}

	@Override
	public void downloadProgress(Long trackingId, int progress) {
		listener.downloadingFile(trackingId, progress);
	}

	@Override
	public void downloadFailed(Long trackingId) {
		listener.downloadFailed(trackingId);
	}

	private final class DmsServer {

		private final String uuid;
		private final AtomicBoolean closed = new AtomicBoolean(false);
		private final Set<String> users = Collections.synchronizedSet(new HashSet<String>());
		private final PriorityBlockingQueue<MessageContainer> messageQueue = new PriorityBlockingQueue<MessageContainer>(
				11, messageSorter);

		private DmsServer(String uuid) {
			this.uuid = uuid;
		}

		private synchronized void queueMessage(MessageContainer messageContainer) {
			boolean nok = closed.get();
			if (!(nok || messageContainer.receiverUuids == null)) {
				messageContainer.receiverUuids.removeIf(uuid -> !users.contains(uuid));
				nok = messageContainer.receiverUuids.isEmpty();
			}
			if (nok) {
				messageContainer.close();
				return;
			}
			messageQueue.offer(messageContainer);
			raiseSignal(uuid);
		}

		private synchronized void sendMore(BiConsumer<Integer, ByteBuffer> bc) {

			MessageContainer messageContainer = messageQueue.poll();
			if (messageContainer == null) {
				return;
			}

			Chunk chunk = messageContainer.next();
			if (chunk == null) {
				messageContainer.close();
				return;
			}

			int messageNumber = messageContainer.messageNumber;
			if (!messageContainer.hasMore()) {
				messageNumber = -messageNumber;
			}

			bc.accept(messageNumber, chunk.dataBuffer);

			boolean progressUpdated = chunk.progress > messageContainer.progressPercent.getAndSet(chunk.progress);
			if (progressUpdated && messageContainer.progressConsumer != null) {
				messageContainer.progressConsumer.accept(chunk.progress, messageContainer.receiverUuids);
			}

			if (messageContainer.hasMore()) {
				messageQueue.offer(messageContainer);
			} else {
				messageContainer.close();
			}

		}

		private synchronized void sendNoMore(int messageNumber, String receiverUuid) {
			messageQueue.forEach(messageContainer -> {
				if (messageContainer.messageNumber != messageNumber) {
					return;
				}
				if (receiverUuid == null) {
					messageQueue.remove(messageContainer);
					messageContainer.close();
					return;
				}
				if (messageContainer.receiverUuids == null) {
					return;
				}
				messageContainer.removeReceiver(receiverUuid);
				if (messageContainer.receiverUuids.isEmpty()) {
					messageQueue.remove(messageContainer);
					messageContainer.close();
				}
			});
		}

		private synchronized void addUser(String userUuid) {
			users.add(userUuid);
			userServerMap.put(userUuid, this);
		}

		private synchronized void removeUser(String userUuid) {
			messageQueue.forEach(messageContainer -> {
				if (messageContainer.receiverUuids == null) {
					return;
				}
				messageContainer.removeReceiver(userUuid);
				if (messageContainer.receiverUuids.isEmpty()) {
					messageQueue.remove(messageContainer);
					messageContainer.close();
				}
			});
			users.remove(userUuid);
			if (users.isEmpty()) {
				close();
				dmsServers.remove(uuid);
			}
		}

		private synchronized void close() {
			closed.set(true);
			MessageContainer messageContainer;
			while ((messageContainer = messageQueue.poll()) != null) {
				messageContainer.close();
			}
		}

	}

	private final class MessageContainer extends DmsMessageSender {

		private final int messageNumber;
		private final AtomicBoolean sendStatus;
		private final BiConsumer<Integer, List<String>> progressConsumer;
		private final Long useTimeout;
		private final boolean bigFile;
		private final List<String> receiverUuids;

		private final long startTime = System.currentTimeMillis();
		private final AtomicInteger progressPercent = new AtomicInteger(-1);
		private long checkInTime = startTime;

		private MessageContainer(MessagePojo messagePojo, AttachmentPojo attachmentPojo, Long useTimeout,
				AtomicBoolean sendStatus, BiConsumer<Integer, List<String>> progressConsumer) {
			super(messagePojo, attachmentPojo);
			this.messageNumber = getMessageNumber(attachmentPojo == null);
			this.sendStatus = sendStatus;
			this.progressConsumer = progressConsumer;
			this.useTimeout = useTimeout;
			this.bigFile = getFileSize() > CommonConstants.SMALL_FILE_LIMIT;
			this.receiverUuids = messagePojo.receiverUuids;
		}

		private boolean isSecondary() {
			return bigFile && health.get();
		}

		private void removeReceiver(String receiverUuid) {
			if (receiverUuids == null || !receiverUuids.remove(receiverUuid)) {
				return;
			}
			if (progressConsumer != null && progressPercent.get() < 100) {
				progressConsumer.accept(-1, Arrays.asList(receiverUuid));
			}
		}

		@Override
		public Chunk next() {
			checkInTime = System.currentTimeMillis();
			health.set((sendStatus == null || sendStatus.get())
					&& (useTimeout == null || checkInTime - startTime < useTimeout));
			return super.next();
		}

		@Override
		public void close() {
			super.close();
			if (progressConsumer != null && progressPercent.get() < 100) {
				progressConsumer.accept(-1, receiverUuids);
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
