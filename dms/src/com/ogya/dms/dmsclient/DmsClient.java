package com.ogya.dms.dmsclient;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.ogya.dms.common.structures.ContentType;
import com.ogya.dms.common.structures.MessagePojo;
import com.ogya.dms.dmsclient.intf.DmsClientListener;
import com.ogya.dms.factory.DmsFactory;
import com.ogya.dms.structures.GroupMessageStatus;
import com.ogya.dms.structures.MessageStatus;

public class DmsClient {

	private final String uuid;

	private final ZContext context = new ZContext();

	private final String serverIp;
	private final int dealerPort;

	private final DmsClientListener listener;

	private final LinkedBlockingQueue<String> dealerQueue = new LinkedBlockingQueue<String>();

	private final ExecutorService taskQueue = DmsFactory.newSingleThreadExecutorService();

	public DmsClient(String uuid, String commIp, int commPort, DmsClientListener listener) {

		this.uuid = uuid;

		this.serverIp = commIp;
		this.dealerPort = commPort;

		this.listener = listener;

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

	public void sendBeacon(String message) {

		dealerQueue.offer(new MessagePojo(message, null, ContentType.BCON, null).toJson());

	}

	public void claimStartInfo() {

		dealerQueue.offer(new MessagePojo(null, uuid, ContentType.REQ_STRT, null).toJson());

	}

	public void addRemoteIp(String message) {

		dealerQueue.offer(new MessagePojo(message, uuid, ContentType.ADD_IP, null).toJson());

	}

	public void removeRemoteIp(String message) {

		dealerQueue.offer(new MessagePojo(message, uuid, ContentType.REMOVE_IP, null).toJson());

	}

	public void sendMessage(String message, String receiverUuid, Long messageId) {

		dealerQueue.offer(new MessagePojo(message, uuid, receiverUuid, ContentType.MESSAGE, messageId).toJson());

	}

	public void sendMessage(String message, Iterable<String> receiverUuids, Long messageId) {

		dealerQueue
				.offer(new MessagePojo(message, uuid, String.join(";", receiverUuids), ContentType.MESSAGE, messageId)
						.toJson());

	}

	public void cancelMessage(Long messageId) {

		dealerQueue.offer(new MessagePojo(null, uuid, ContentType.CANCEL, messageId).toJson());

	}

	public void claimMessageStatus(Long messageId, String receiverUuid) {

		dealerQueue
				.offer(new MessagePojo(null, uuid, receiverUuid, ContentType.CLAIM_MESSAGE_STATUS, messageId).toJson());

	}

	public void feedMessageStatus(String receiverUuid, Long messageId, MessageStatus messageStatus) {

		dealerQueue.offer(
				new MessagePojo(messageStatus.toJson(), uuid, receiverUuid, ContentType.FEED_MESSAGE_STATUS, messageId)
						.toJson());

	}

	public void feedGroupMessageStatus(String receiverUuid, Long messageId, GroupMessageStatus groupMessageStatus) {

		dealerQueue.offer(new MessagePojo(groupMessageStatus.toJson(), uuid, receiverUuid,
				ContentType.FEED_GROUP_MESSAGE_STATUS, messageId).toJson());

	}

	public void claimStatusReport(Long messageId, String receiverUuid) {

		dealerQueue
				.offer(new MessagePojo(null, uuid, receiverUuid, ContentType.CLAIM_STATUS_REPORT, messageId).toJson());

	}

	public void feedStatusReport(Long messageId, String message, String receiverUuid) {

		dealerQueue.offer(
				new MessagePojo(message, null, receiverUuid, ContentType.FEED_STATUS_REPORT, messageId).toJson());

	}

	public void sendTransientMessage(String message, Iterable<String> receiverUuids) {

		dealerQueue.offer(
				new MessagePojo(message, uuid, String.join(";", receiverUuids), ContentType.TRANSIENT, null).toJson());

	}

	private void dealer() {

		try (ZMQ.Socket dealerSocket = context.createSocket(SocketType.DEALER);
				ZMQ.Socket inprocSocket = context.createSocket(SocketType.PAIR)) {

			inprocSocket.bind("inproc://dealer");

			dealerSocket.monitor("inproc://monitor", ZMQ.EVENT_HANDSHAKE_PROTOCOL | ZMQ.EVENT_DISCONNECTED);

			dealerSocket.setIdentity(uuid.getBytes(ZMQ.CHARSET));
			dealerSocket.setImmediate(false);
			dealerSocket.connect("tcp://" + serverIp + ":" + dealerPort);

			ZMQ.Poller poller = context.createPoller(2);
			int pollDealer = poller.register(dealerSocket, ZMQ.Poller.POLLIN);
			int pollInproc = poller.register(inprocSocket, ZMQ.Poller.POLLIN);

			while (!Thread.currentThread().isInterrupted()) {

				poller.poll();

				if (poller.pollin(pollDealer)) {

					String receivedMessage = dealerSocket.recvStr(ZMQ.DONTWAIT);
					processIncomingMessage(receivedMessage);

				} else if (poller.pollin(pollInproc)) {

					String sentMessage = inprocSocket.recvStr(ZMQ.DONTWAIT);
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

					inprocSocket.send(dealerQueue.take());

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

					serverConnStatusUpdatedToListener(true);
					break;

				case ZMQ.EVENT_DISCONNECTED:
					serverConnStatusUpdatedToListener(false);
					break;

				}

			}

		}

	}

	private void processIncomingMessage(String message) {

		if (message.isEmpty())
			return;

		try {

			MessagePojo messagePojo = MessagePojo.fromJson(message);

			if (Objects.equals(uuid, messagePojo.senderUuid))
				return;

			switch (messagePojo.contentType) {

			case BCON:

				beaconReceivedToListener(messagePojo.message);

				break;

			case IP:

				remoteIpsReceivedToListener(messagePojo.message);

				break;

			case PROGRESS:

				progressReceivedToListener(messagePojo.messageId, messagePojo.senderUuid.split(";"),
						Integer.parseInt(messagePojo.message));

				break;

			case MESSAGE:

				messageReceivedToListener(messagePojo.message, messagePojo.senderUuid);

				break;

			case UUID_DISCONNECTED:

				userDisconnectedToListener(messagePojo.senderUuid);

				break;

			case CLAIM_MESSAGE_STATUS:

				messageStatusClaimedToListener(messagePojo.messageId, messagePojo.senderUuid);

				break;

			case FEED_MESSAGE_STATUS:

				messageStatusFedToListener(messagePojo.messageId, MessageStatus.fromJson(messagePojo.message),
						messagePojo.senderUuid);

				break;

			case FEED_GROUP_MESSAGE_STATUS:

				groupMessageStatusFedToListener(messagePojo.messageId, GroupMessageStatus.fromJson(messagePojo.message),
						messagePojo.senderUuid);

				break;

			case CLAIM_STATUS_REPORT:

				statusReportClaimedToListener(messagePojo.messageId, messagePojo.senderUuid);

				break;

			case FEED_STATUS_REPORT:

				statusReportFedToListener(messagePojo.messageId, messagePojo.message);

				break;

			case TRANSIENT:

				transientMessageReceivedToListener(messagePojo.message, messagePojo.senderUuid);

				break;

			default:

				break;

			}

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	private void beaconReceivedToListener(final String message) {

		taskQueue.execute(() -> {

			listener.beaconReceived(message);

		});

	}

	private void remoteIpsReceivedToListener(final String message) {

		taskQueue.execute(() -> {

			listener.remoteIpsReceived(message);

		});

	}

	private void progressReceivedToListener(final Long messageId, final String[] uuids, int progress) {

		taskQueue.execute(() -> {

			listener.progressReceived(messageId, uuids, progress);

		});

	}

	private void messageReceivedToListener(final String message, final String remoteUuid) {

		taskQueue.execute(() -> {

			listener.messageReceived(message, remoteUuid);

		});

	}

	private void userDisconnectedToListener(final String uuid) {

		taskQueue.execute(() -> {

			listener.userDisconnected(uuid);

		});

	}

	private void serverConnStatusUpdatedToListener(final boolean connStatus) {

		taskQueue.execute(() -> {

			listener.serverConnStatusUpdated(connStatus);

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

	private void statusReportFedToListener(final Long messageId, final String message) {

		taskQueue.execute(() -> {

			listener.statusReportFed(messageId, message);

		});

	}

	private void transientMessageReceivedToListener(final String message, final String remoteUuid) {

		taskQueue.execute(() -> {

			listener.transientMessageReceived(message, remoteUuid);

		});

	}

}
