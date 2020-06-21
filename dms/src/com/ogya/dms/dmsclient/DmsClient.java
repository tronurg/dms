package com.ogya.dms.dmsclient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.ogya.dms.common.structures.ContentType;
import com.ogya.dms.common.structures.MessagePojo;
import com.ogya.dms.dmsclient.intf.DmsClientListener;

public class DmsClient {

	private final String uuid;

	private final ZContext context = new ZContext();

	private final String serverIp;
	private final int dealerPort;

	private final DmsClientListener listener;

	private final LinkedBlockingQueue<String> dealerQueue = new LinkedBlockingQueue<String>();

	private final Gson gson = new Gson();

	private final ExecutorService taskQueue = Executors.newSingleThreadExecutor(new ThreadFactory() {

		@Override
		public Thread newThread(Runnable arg0) {

			Thread thread = new Thread(arg0);

			thread.setDaemon(true);

			return thread;

		}

	});

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

		dealerQueue.offer(gson.toJson(new MessagePojo(message, uuid, ContentType.BCON)));

	}

	public void claimAllBeacons() {

		dealerQueue.offer(gson.toJson(new MessagePojo("", uuid, ContentType.REQ_BCON)));

	}

	public void sendMessage(String message, String receiverUuid) {

		dealerQueue.offer(gson.toJson(new MessagePojo(message, uuid, receiverUuid, ContentType.MESSAGE)));

	}

	public void sendMessage(String message, String proxyUuid, String receiverUuid) {

		dealerQueue.offer(gson.toJson(new MessagePojo(message, uuid, proxyUuid, receiverUuid, ContentType.MESSAGE)));

	}

	public void sendMessage(String message, String proxyUuid, String... receiverUuids) {

		dealerQueue.offer(gson.toJson(
				new MessagePojo(message, uuid, proxyUuid, String.join(";", receiverUuids), ContentType.MESSAGE)));

	}

	public void claimMessageStatus(String message, String receiverUuid) {

		dealerQueue.offer(gson.toJson(new MessagePojo(message, uuid, receiverUuid, ContentType.CLAIM_MESSAGE_STATUS)));

	}

	public void claimMessageStatus(String message, String proxyUuid, String receiverUuid) {

		dealerQueue.offer(
				gson.toJson(new MessagePojo(message, uuid, proxyUuid, receiverUuid, ContentType.CLAIM_MESSAGE_STATUS)));

	}

	public void claimMessageStatus(String message, String proxyUuid, String... receiverUuids) {

		dealerQueue.offer(gson.toJson(new MessagePojo(message, uuid, proxyUuid, String.join(";", receiverUuids),
				ContentType.CLAIM_MESSAGE_STATUS)));

	}

	public void sendNotReceived(String message, String receiverUuid) {

		dealerQueue.offer(gson.toJson(new MessagePojo(message, uuid, receiverUuid, ContentType.NOT_RECEIVED)));

	}

	public void sendReceived(String message, String receiverUuid) {

		dealerQueue.offer(gson.toJson(new MessagePojo(message, uuid, receiverUuid, ContentType.RECEIVED)));

	}

	public void sendRead(String message, String receiverUuid) {

		dealerQueue.offer(gson.toJson(new MessagePojo(message, uuid, receiverUuid, ContentType.READ)));

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

			MessagePojo messagePojo = gson.fromJson(message, MessagePojo.class);

			if (uuid.equals(messagePojo.senderUuid))
				return;

			switch (messagePojo.contentType) {

			case BCON:

				beaconReceivedToListener(messagePojo.message);

				break;

			case MESSAGE:

				messageReceivedToListener(messagePojo.message);

				break;

			case UUID_DISCONNECTED:

				userDisconnectedToListener(messagePojo.message);

				break;

			case CLAIM_MESSAGE_STATUS:

				messageStatusClaimedToListener(messagePojo.message, messagePojo.senderUuid);

				break;

			case NOT_RECEIVED:

				messageNotReceivedRemotelyToListener(messagePojo.message, messagePojo.senderUuid);

				break;

			case RECEIVED:

				messageReceivedRemotelyToListener(messagePojo.message, messagePojo.senderUuid);

				break;

			case READ:

				messageReadRemotelyToListener(messagePojo.message, messagePojo.senderUuid);

				break;

			default:

			}

		} catch (JsonSyntaxException e) {

			e.printStackTrace();

		}

	}

	private void beaconReceivedToListener(final String message) {

		taskQueue.execute(() -> {

			listener.beaconReceived(message);

		});

	}

	private void messageReceivedToListener(final String message) {

		taskQueue.execute(() -> {

			listener.messageReceived(message);

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

	private void messageStatusClaimedToListener(final String message, final String remoteUuid) {

		taskQueue.execute(() -> {

			listener.messageStatusClaimed(message, remoteUuid);

		});

	}

	private void messageNotReceivedRemotelyToListener(final String message, final String remoteUuid) {

		taskQueue.execute(() -> {

			listener.messageNotReceivedRemotely(message, remoteUuid);

		});

	}

	private void messageReceivedRemotelyToListener(final String message, final String remoteUuid) {

		taskQueue.execute(() -> {

			listener.messageReceivedRemotely(message, remoteUuid);

		});

	}

	private void messageReadRemotelyToListener(final String message, final String remoteUuid) {

		taskQueue.execute(() -> {

			listener.messageReadRemotely(message, remoteUuid);

		});

	}

}
