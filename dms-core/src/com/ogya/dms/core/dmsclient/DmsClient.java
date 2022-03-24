package com.ogya.dms.core.dmsclient;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.ogya.dms.commons.DmsMessageReceiver;
import com.ogya.dms.commons.DmsMessageReceiver.DmsMessageReceiverListener;
import com.ogya.dms.commons.DmsMessageSender;
import com.ogya.dms.commons.DmsMessageSender.Chunk;
import com.ogya.dms.commons.DmsMessageSender.Direction;
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

	private final String uuid;

	private final ZContext context = new ZContext();

	private final String serverIp;
	private final int dealerPort;

	private final AtomicBoolean serverConnected = new AtomicBoolean(false);

	private final DmsClientListener listener;

	private final LinkedBlockingQueue<MessagePojo> dealerQueue = new LinkedBlockingQueue<MessagePojo>();
	private final ExecutorService taskQueue = DmsFactory.newSingleThreadExecutorService();
	private final DmsMessageReceiver messageReceiver;
	private final AtomicInteger messageCounter = new AtomicInteger(0);
	private final Map<Integer, AtomicBoolean> stopMap = Collections
			.synchronizedMap(new HashMap<Integer, AtomicBoolean>());

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

		dealerQueue
				.offer(new MessagePojo(DmsPackingFactory.pack(beacon), null, null, ContentType.BCON, null, null, null));

	}

	public void claimStartInfo() {

		dealerQueue.offer(new MessagePojo(null, uuid, null, ContentType.REQ_STRT, null, null, null));

	}

	public void addRemoteIps(InetAddress... ips) {

		dealerQueue
				.offer(new MessagePojo(DmsPackingFactory.pack(ips), null, null, ContentType.ADD_IPS, null, null, null));

	}

	public void removeRemoteIps(InetAddress... ips) {

		dealerQueue.offer(
				new MessagePojo(DmsPackingFactory.pack(ips), null, null, ContentType.REMOVE_IPS, null, null, null));

	}

	public void sendMessage(Message message, Path attachment, boolean linkOnlyAttachment, String receiverUuid,
			Long messageId) {

		MessagePojo messagePojo = new MessagePojo(DmsPackingFactory.pack(message), uuid, receiverUuid,
				ContentType.MESSAGE, messageId, null, null);

		if (attachment != null) {
			messagePojo.attachment = new AttachmentPojo(attachment, linkOnlyAttachment);
		}

		dealerQueue.offer(messagePojo);

	}

	public void cancelMessage(Long trackingId) {

		dealerQueue.offer(new MessagePojo(null, uuid, null, ContentType.CANCEL_MESSAGE, trackingId, null, null));

	}

	public void claimMessageStatus(Long[] messageIds, String receiverUuid) {

		dealerQueue.offer(new MessagePojo(DmsPackingFactory.pack(messageIds), uuid, receiverUuid,
				ContentType.CLAIM_MESSAGE_STATUS, null, null, null));

	}

	public void feedMessageStatus(Map<Long, MessageStatus> messageIdStatusMap, String receiverUuid) {

		dealerQueue.offer(new MessagePojo(DmsPackingFactory.pack(messageIdStatusMap), uuid, receiverUuid,
				ContentType.FEED_MESSAGE_STATUS, null, null, null));

	}

	public void feedGroupMessageStatus(Map<Long, GroupMessageStatus> messageIdGroupStatusMap, String receiverUuid) {

		dealerQueue.offer(new MessagePojo(DmsPackingFactory.pack(messageIdGroupStatusMap), uuid, receiverUuid,
				ContentType.FEED_GROUP_MESSAGE_STATUS, null, null, null));

	}

	public void claimStatusReport(Long[] messageIds, String receiverUuid) {

		dealerQueue.offer(new MessagePojo(DmsPackingFactory.pack(messageIds), uuid, receiverUuid,
				ContentType.CLAIM_STATUS_REPORT, null, null, null));

	}

	public void feedStatusReport(Map<Long, Set<StatusReport>> messageIdStatusReportsMap, String receiverUuid) {

		dealerQueue.offer(new MessagePojo(DmsPackingFactory.pack(messageIdStatusReportsMap), null, receiverUuid,
				ContentType.FEED_STATUS_REPORT, null, null, null));

	}

	public void sendTransientMessage(MessageHandleImpl message, boolean linkOnlyAttachment,
			Iterable<String> receiverUuids, Long trackingId, Long useTimeout, InetAddress useLocalInterface) {

		MessagePojo messagePojo = new MessagePojo(DmsPackingFactory.pack(message), uuid,
				String.join(";", receiverUuids), ContentType.TRANSIENT, trackingId, useTimeout, useLocalInterface);

		FileHandle fileHandle = message.getFileHandle();
		if (fileHandle != null) {
			Path attachment = fileHandle.getPath();
			if (attachment != null) {
				messagePojo.attachment = new AttachmentPojo(attachment, linkOnlyAttachment);
			}
		}

		dealerQueue.offer(messagePojo);

	}

	public void sendTransientMessageStatus(Long trackingId, String receiverUuid) {

		dealerQueue.offer(new MessagePojo(DmsPackingFactory.pack(trackingId), uuid, receiverUuid,
				ContentType.FEED_TRANSIENT_STATUS, null, null, null));

	}

	public void cancelTransientMessage(Long trackingId) {

		dealerQueue.offer(new MessagePojo(null, uuid, null, ContentType.CANCEL_TRANSIENT, trackingId, null, null));

	}

	public void sendDownloadRequest(DownloadPojo downloadPojo, String receiverUuid) {

		dealerQueue.offer(new MessagePojo(DmsPackingFactory.pack(downloadPojo), uuid, receiverUuid,
				ContentType.DOWNLOAD_REQUEST, null, null, null));

	}

	public void cancelDownloadRequest(Long downloadId, String receiverUuid) {

		dealerQueue.offer(new MessagePojo(DmsPackingFactory.pack(downloadId), uuid, receiverUuid,
				ContentType.CANCEL_DOWNLOAD_REQUEST, null, null, null));

	}

	public void sendServerNotFound(Long downloadId, String receiverUuid) {

		dealerQueue.offer(new MessagePojo(DmsPackingFactory.pack(downloadId), null, receiverUuid,
				ContentType.SERVER_NOT_FOUND, null, null, null));

	}

	public void sendFileNotFound(Long downloadId, String receiverUuid) {

		dealerQueue.offer(new MessagePojo(DmsPackingFactory.pack(downloadId), null, receiverUuid,
				ContentType.FILE_NOT_FOUND, null, null, null));

	}

	public void uploadFile(Path path, String receiverUuid, boolean linkOnlyAttachment, Long trackingId) {

		MessagePojo messagePojo = new MessagePojo(DmsPackingFactory.pack(path.getFileName().toString()), uuid,
				receiverUuid, ContentType.UPLOAD, trackingId, null, null);
		messagePojo.attachment = new AttachmentPojo(path, linkOnlyAttachment);
		dealerQueue.offer(messagePojo);

	}

	public void cancelUpload(String receiverUuid, Long trackingId) {

		dealerQueue.offer(new MessagePojo(null, null, receiverUuid, ContentType.CANCEL_UPLOAD, trackingId, null, null));

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
					byte[] receivedMessage = dealerSocket.recv(ZMQ.DONTWAIT);
					synchronized (messageReceiver) {
						messageReceiver.inFeed(messageNumber, receivedMessage);
					}

				} else if (poller.pollin(pollInproc)) {

					byte[] sentMessage = inprocSocket.recv(ZMQ.DONTWAIT);
					dealerSocket.send(sentMessage, ZMQ.DONTWAIT);

				}

			}

		}

	}

	private void inproc() {

		try (ZMQ.Socket inprocSocket = context.createSocket(SocketType.PAIR)) {

			inprocSocket.connect("inproc://dealer");

			while (!Thread.currentThread().isInterrupted()) {

				try {

					MessagePojo messagePojo = dealerQueue.take();

					try (DmsMessageSender messageSender = new DmsMessageSender(messagePojo,
							Direction.CLIENT_TO_SERVER)) {

						Chunk chunk;

						while (serverConnected.get() && (chunk = messageSender.next()) != null) {

							inprocSocket.sendByteBuffer(chunk.dataBuffer, 0);
							if (chunk.progress < 0 && messagePojo.contentType == ContentType.TRANSIENT) {
								progressTransientReceivedToListener(messagePojo.trackingId,
										messagePojo.receiverUuid.split(";"), chunk.progress);
							}

						}

					}

				} catch (InterruptedException e) {

					e.printStackTrace();

				}

			}

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
					serverConnStatusUpdatedToListener();
					break;

				case ZMQ.EVENT_DISCONNECTED:
					serverConnected.set(false);
					synchronized (messageReceiver) {
						messageReceiver.deleteResources();
					}
					serverConnStatusUpdatedToListener();
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

				beaconReceivedToListener(DmsPackingFactory.unpack(payload, Beacon.class));

				break;

			case IPS:

				remoteIpsReceivedToListener(DmsPackingFactory.unpack(payload, InetAddress[].class));

				break;

			case PROGRESS_MESSAGE:

				progressMessageReceivedToListener(messagePojo.trackingId, messagePojo.senderUuid.split(";"),
						DmsPackingFactory.unpack(payload, Integer.class));

				break;

			case PROGRESS_TRANSIENT:

				progressTransientReceivedToListener(messagePojo.trackingId, messagePojo.senderUuid.split(";"),
						DmsPackingFactory.unpack(payload, Integer.class));

				break;

			case MESSAGE:

				Message message = DmsPackingFactory.unpack(payload, Message.class);
				message.setMessageRefId(messagePojo.trackingId);
				messageReceivedToListener(message, messagePojo.getAttachmentLink(), messagePojo.senderUuid);

				break;

			case UUID_DISCONNECTED:

				userDisconnectedToListener(messagePojo.senderUuid);

				break;

			case CLAIM_MESSAGE_STATUS:

				messageStatusClaimedToListener(DmsPackingFactory.unpack(payload, Long[].class), messagePojo.senderUuid);

				break;

			case FEED_MESSAGE_STATUS:

				messageStatusFedToListener(DmsPackingFactory.unpackMap(payload, Long.class, MessageStatus.class),
						messagePojo.senderUuid);

				break;

			case FEED_GROUP_MESSAGE_STATUS:

				groupMessageStatusFedToListener(
						DmsPackingFactory.unpackMap(payload, Long.class, GroupMessageStatus.class),
						messagePojo.senderUuid);

				break;

			case CLAIM_STATUS_REPORT:

				statusReportClaimedToListener(DmsPackingFactory.unpack(payload, Long[].class), messagePojo.senderUuid);

				break;

			case FEED_STATUS_REPORT:

				statusReportFedToListener(DmsPackingFactory.unpackMap(payload, Long.class, StatusReport[].class));

				break;

			case TRANSIENT:

				transientMessageReceivedToListener(DmsPackingFactory.unpack(payload, MessageHandleImpl.class),
						messagePojo.getAttachmentLink(), messagePojo.senderUuid, messagePojo.trackingId);

				break;

			case FEED_TRANSIENT_STATUS:

				transientMessageStatusReceivedToListener(DmsPackingFactory.unpack(payload, Long.class),
						messagePojo.senderUuid);

				break;

			case DOWNLOAD_REQUEST:

				downloadRequestedToListener(DmsPackingFactory.unpack(payload, DownloadPojo.class),
						messagePojo.senderUuid);

				break;

			case CANCEL_DOWNLOAD_REQUEST:

				cancelDownloadRequestedToListener(DmsPackingFactory.unpack(payload, Long.class),
						messagePojo.senderUuid);

				break;

			case SERVER_NOT_FOUND:

				serverNotFoundToListener(DmsPackingFactory.unpack(payload, Long.class));

				break;

			case FILE_NOT_FOUND:

				fileNotFoundToListener(DmsPackingFactory.unpack(payload, Long.class));

				break;

			case PROGRESS_DOWNLOAD:

				downloadingFileToListener(messagePojo.trackingId, DmsPackingFactory.unpack(payload, Integer.class));

				break;

			case UPLOAD:

				fileDownloadedToListener(messagePojo.trackingId, messagePojo.getAttachmentLink(),
						DmsPackingFactory.unpack(payload, String.class));

				break;

			default:

				break;

			}

		} catch (Exception e) {

		}

	}

	private void beaconReceivedToListener(final Beacon beacon) {

		taskQueue.execute(() -> {

			listener.beaconReceived(beacon);

		});

	}

	private void remoteIpsReceivedToListener(final InetAddress[] remoteIps) {

		taskQueue.execute(() -> {

			listener.remoteIpsReceived(remoteIps);

		});

	}

	private void progressMessageReceivedToListener(final Long messageId, final String[] uuids, final int progress) {

		taskQueue.execute(() -> {

			listener.progressMessageReceived(messageId, uuids, progress);

		});

	}

	private void progressTransientReceivedToListener(final Long trackingId, final String[] uuids, final int progress) {

		taskQueue.execute(() -> {

			listener.progressTransientReceived(trackingId, uuids, progress);

		});

	}

	private void messageReceivedToListener(final Message message, final Path attachment, final String remoteUuid) {

		taskQueue.execute(() -> {

			listener.messageReceived(message, attachment, remoteUuid);

		});

	}

	private void userDisconnectedToListener(final String uuid) {

		taskQueue.execute(() -> {

			listener.userDisconnected(uuid);

		});

	}

	private void serverConnStatusUpdatedToListener() {

		taskQueue.execute(() -> {

			listener.serverConnStatusUpdated(serverConnected.get());

		});

	}

	private void messageStatusClaimedToListener(final Long[] messageIds, final String remoteUuid) {

		taskQueue.execute(() -> {

			listener.messageStatusClaimed(messageIds, remoteUuid);

		});

	}

	private void messageStatusFedToListener(final Map<Long, MessageStatus> messageIdStatusMap,
			final String remoteUuid) {

		taskQueue.execute(() -> {

			listener.messageStatusFed(messageIdStatusMap, remoteUuid);

		});

	}

	private void groupMessageStatusFedToListener(final Map<Long, GroupMessageStatus> messageIdGroupStatusMap,
			final String remoteUuid) {

		taskQueue.execute(() -> {

			listener.groupMessageStatusFed(messageIdGroupStatusMap, remoteUuid);

		});

	}

	private void statusReportClaimedToListener(final Long[] messageIds, final String remoteUuid) {

		taskQueue.execute(() -> {

			listener.statusReportClaimed(messageIds, remoteUuid);

		});

	}

	private void statusReportFedToListener(final Map<Long, StatusReport[]> messageIdStatusReportsMap) {

		taskQueue.execute(() -> {

			listener.statusReportFed(messageIdStatusReportsMap);

		});

	}

	private void transientMessageReceivedToListener(final MessageHandleImpl message, final Path attachment,
			final String remoteUuid, final Long trackingId) {

		taskQueue.execute(() -> {

			listener.transientMessageReceived(message, attachment, remoteUuid, trackingId);

		});

	}

	private void transientMessageStatusReceivedToListener(final Long trackingId, final String remoteUuid) {

		taskQueue.execute(() -> {

			listener.transientMessageStatusReceived(trackingId, remoteUuid);

		});

	}

	private void downloadRequestedToListener(final DownloadPojo downloadPojo, final String remoteUuid) {

		taskQueue.execute(() -> {

			listener.downloadRequested(downloadPojo, remoteUuid);

		});

	}

	private void cancelDownloadRequestedToListener(final Long downloadId, final String remoteUuid) {

		taskQueue.execute(() -> {

			listener.cancelDownloadRequested(downloadId, remoteUuid);

		});

	}

	private void serverNotFoundToListener(final Long downloadId) {

		taskQueue.execute(() -> {

			listener.serverNotFound(downloadId);

		});

	}

	private void fileNotFoundToListener(final Long downloadId) {

		taskQueue.execute(() -> {

			listener.fileNotFound(downloadId);

		});

	}

	private void downloadingFileToListener(final Long downloadId, final int progress) {

		taskQueue.execute(() -> {

			listener.downloadingFile(downloadId, progress);

		});

	}

	private void fileDownloadedToListener(final Long downloadId, final Path path, final String fileName) {

		taskQueue.execute(() -> {

			listener.fileDownloaded(downloadId, path, fileName);

		});

	}

	private void downloadFailedToListener(final Long downloadId) {

		taskQueue.execute(() -> {

			listener.downloadFailed(downloadId);

		});

	}

	@Override
	public void messageReceived(MessagePojo messagePojo) {
		processIncomingMessage(messagePojo);
	}

	@Override
	public void messageFailed(int messageNumber) {
		dealerQueue.offer(new MessagePojo(DmsPackingFactory.pack(messageNumber), null, null, ContentType.STOP_SENDING,
				null, null, null));
	}

}
