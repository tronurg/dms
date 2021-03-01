package com.ogya.dms.core.dmsclient;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.ogya.dms.commons.DmsMessageFactory;
import com.ogya.dms.commons.DmsPackingFactory;
import com.ogya.dms.commons.structures.Beacon;
import com.ogya.dms.commons.structures.ContentType;
import com.ogya.dms.commons.structures.MessagePojo;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.database.tables.StatusReport;
import com.ogya.dms.core.dmsclient.intf.DmsClientListener;
import com.ogya.dms.core.factory.DmsFactory;
import com.ogya.dms.core.intf.handles.impl.MessageHandleImpl;
import com.ogya.dms.core.structures.GroupMessageStatus;
import com.ogya.dms.core.structures.MessageStatus;

public class DmsClient {

	private static final int CHUNK_SIZE = 8192;

	private final String uuid;

	private final ZContext context = new ZContext();

	private final String serverIp;
	private final int dealerPort;

	private final AtomicBoolean serverConnected = new AtomicBoolean(false);

	private final DmsClientListener listener;

	private final LinkedBlockingQueue<MessagePojo> dealerQueue = new LinkedBlockingQueue<MessagePojo>();

	private final ExecutorService taskQueue = DmsFactory.newSingleThreadExecutorService();

	private final DmsMessageFactory messageFactory;

	public DmsClient(String uuid, String commIp, int commPort, DmsClientListener listener) {

		this.uuid = uuid;

		this.serverIp = commIp;
		this.dealerPort = commPort;

		this.listener = listener;

		this.messageFactory = new DmsMessageFactory(this::processIncomingMessage);

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

		dealerQueue.offer(
				new MessagePojo(DmsPackingFactory.pack(beacon), null, null, ContentType.BCON, null, null, null, null));

	}

	public void claimStartInfo() {

		dealerQueue.offer(new MessagePojo(null, uuid, null, ContentType.REQ_STRT, null, null, null, null));

	}

	public void addRemoteIps(InetAddress... ips) {

		dealerQueue.offer(
				new MessagePojo(DmsPackingFactory.pack(ips), null, null, ContentType.ADD_IPS, null, null, null, null));

	}

	public void removeRemoteIps(InetAddress... ips) {

		dealerQueue.offer(new MessagePojo(DmsPackingFactory.pack(ips), null, null, ContentType.REMOVE_IPS, null, null,
				null, null));

	}

	public void sendMessage(Message message, Path attachment, String receiverUuid, Long trackingId) {

		MessagePojo messagePojo = new MessagePojo(DmsPackingFactory.pack(message), uuid, receiverUuid,
				ContentType.MESSAGE, null, trackingId, null, null);

		messagePojo.attachment = attachment;

		dealerQueue.offer(messagePojo);

	}

	public void cancelMessage(Long trackingId) {

		dealerQueue.offer(new MessagePojo(null, uuid, null, ContentType.CANCEL, null, trackingId, null, null));

	}

	public void claimMessageStatus(Long messageId, String receiverUuid) {

		dealerQueue.offer(new MessagePojo(null, uuid, receiverUuid, ContentType.CLAIM_MESSAGE_STATUS, messageId, null,
				null, null));

	}

	public void feedMessageStatus(MessageStatus messageStatus, String receiverUuid, Long messageId) {

		dealerQueue.offer(new MessagePojo(DmsPackingFactory.pack(messageStatus), uuid, receiverUuid,
				ContentType.FEED_MESSAGE_STATUS, messageId, null, null, null));

	}

	public void feedGroupMessageStatus(GroupMessageStatus groupMessageStatus, String receiverUuid, Long messageId) {

		dealerQueue.offer(new MessagePojo(DmsPackingFactory.pack(groupMessageStatus), uuid, receiverUuid,
				ContentType.FEED_GROUP_MESSAGE_STATUS, messageId, null, null, null));

	}

	public void claimStatusReport(Long messageId, String receiverUuid) {

		dealerQueue.offer(new MessagePojo(null, uuid, receiverUuid, ContentType.CLAIM_STATUS_REPORT, messageId, null,
				null, null));

	}

	public void feedStatusReport(Set<StatusReport> statusReports, String receiverUuid, Long messageId) {

		dealerQueue.offer(new MessagePojo(DmsPackingFactory.pack(statusReports), null, receiverUuid,
				ContentType.FEED_STATUS_REPORT, messageId, null, null, null));

	}

	public void sendTransientMessage(MessageHandleImpl message, Iterable<String> receiverUuids, Long useTrackingId,
			Long useTimeout, InetAddress useLocalInterface) {

		MessagePojo messagePojo = new MessagePojo(DmsPackingFactory.pack(message), uuid,
				String.join(";", receiverUuids), ContentType.TRANSIENT, null, useTrackingId, useTimeout,
				useLocalInterface);

		if (message.getFileHandle() != null)
			messagePojo.attachment = message.getFileHandle().getPath();

		dealerQueue.offer(messagePojo);

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

					byte[] receivedMessage = dealerSocket.recv(ZMQ.DONTWAIT);
					synchronized (messageFactory) {
						messageFactory.inFeed(receivedMessage);
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

					DmsMessageFactory.outFeed(messagePojo, CHUNK_SIZE, serverConnected, (data, progress) -> {
						inprocSocket.send(data);
						if (progress < 0 && messagePojo.useTrackingId != null
								&& Objects.equals(messagePojo.contentType, ContentType.TRANSIENT))
							progressTransientReceivedToListener(messagePojo.useTrackingId,
									messagePojo.receiverUuid.split(";"), progress);
					});

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
					synchronized (messageFactory) {
						messageFactory.deleteResources();
						messageFactory.reset();
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

				progressMessageReceivedToListener(messagePojo.useTrackingId, messagePojo.senderUuid.split(";"),
						DmsPackingFactory.unpack(payload, Integer.class));

				break;

			case PROGRESS_TRANSIENT:

				progressTransientReceivedToListener(messagePojo.useTrackingId, messagePojo.senderUuid.split(";"),
						DmsPackingFactory.unpack(payload, Integer.class));

				break;

			case MESSAGE:

				messageReceivedToListener(DmsPackingFactory.unpack(payload, Message.class), messagePojo.attachment,
						messagePojo.senderUuid);

				break;

			case UUID_DISCONNECTED:

				userDisconnectedToListener(messagePojo.senderUuid);

				break;

			case CLAIM_MESSAGE_STATUS:

				messageStatusClaimedToListener(messagePojo.messageId, messagePojo.senderUuid);

				break;

			case FEED_MESSAGE_STATUS:

				messageStatusFedToListener(messagePojo.messageId,
						DmsPackingFactory.unpack(payload, MessageStatus.class), messagePojo.senderUuid);

				break;

			case FEED_GROUP_MESSAGE_STATUS:

				groupMessageStatusFedToListener(messagePojo.messageId,
						DmsPackingFactory.unpack(payload, GroupMessageStatus.class), messagePojo.senderUuid);

				break;

			case CLAIM_STATUS_REPORT:

				statusReportClaimedToListener(messagePojo.messageId, messagePojo.senderUuid);

				break;

			case FEED_STATUS_REPORT:

				statusReportFedToListener(messagePojo.messageId,
						DmsPackingFactory.unpack(payload, StatusReport[].class));

				break;

			case TRANSIENT:

				transientMessageReceivedToListener(DmsPackingFactory.unpack(payload, MessageHandleImpl.class),
						messagePojo.attachment, messagePojo.senderUuid);

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

	private void progressMessageReceivedToListener(final Long messageId, final String[] uuids, int progress) {

		taskQueue.execute(() -> {

			listener.progressMessageReceived(messageId, uuids, progress);

		});

	}

	private void progressTransientReceivedToListener(final Long trackingId, final String[] uuids, int progress) {

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

	private void messageStatusClaimedToListener(final Long messageId, final String remoteUuid) {

		taskQueue.execute(() -> {

			listener.messageStatusClaimed(messageId, remoteUuid);

		});

	}

	private void messageStatusFedToListener(final Long messageId, final MessageStatus messageStatus,
			String remoteUuid) {

		taskQueue.execute(() -> {

			listener.messageStatusFed(messageId, messageStatus, remoteUuid);

		});

	}

	private void groupMessageStatusFedToListener(final Long messageId, final GroupMessageStatus groupMessageStatus,
			String remoteUuid) {

		taskQueue.execute(() -> {

			listener.groupMessageStatusFed(messageId, groupMessageStatus, remoteUuid);

		});

	}

	private void statusReportClaimedToListener(final Long messageId, final String remoteUuid) {

		taskQueue.execute(() -> {

			listener.statusReportClaimed(messageId, remoteUuid);

		});

	}

	private void statusReportFedToListener(final Long messageId, final StatusReport[] statusReports) {

		taskQueue.execute(() -> {

			listener.statusReportFed(messageId, statusReports);

		});

	}

	private void transientMessageReceivedToListener(final MessageHandleImpl message, final Path attachment,
			final String remoteUuid) {

		taskQueue.execute(() -> {

			listener.transientMessageReceived(message, attachment, remoteUuid);

		});

	}

}
