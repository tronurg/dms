package com.aselsan.rehis.reform.mcsy.mcistemci;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class McIstemci {

	private final String uuid;

	private final ZContext context = new ZContext();

	private final int dealerPort;
	private final int subPort;

	private final McIstemciDinleyici dinleyici;

	private final LinkedBlockingQueue<String> dealerQueue = new LinkedBlockingQueue<String>();

	private final Gson gson = new Gson();

	private final ExecutorService out = Executors.newSingleThreadExecutor(new ThreadFactory() {

		@Override
		public Thread newThread(Runnable arg0) {

			Thread thread = new Thread(arg0);

			thread.setDaemon(true);

			return thread;

		}

	});

	public McIstemci(String uuid, int comPort, McIstemciDinleyici dinleyici) {

		this.uuid = uuid;

		this.dealerPort = comPort;
		this.subPort = comPort + 1;

		this.dinleyici = dinleyici;

		start();

	}

	private void start() {

		Thread dealerThread = new Thread(this::dealer);
		dealerThread.setDaemon(true);
		dealerThread.start();

		Thread subThread = new Thread(this::sub);
		subThread.setDaemon(true);
		subThread.start();

	}

	public void beaconGonder(String mesaj) {

		dealerQueue.offer(gson.toJson(new MesajNesnesi(mesaj, "BCON")));

	}

	private void dinleyiciyeBeaconAlindi(String mesaj) {

		out.execute(() -> {

			dinleyici.beaconAlindi(mesaj);

		});

	}

	private void dealer() {

		try (ZMQ.Socket dealerSocket = context.createSocket(SocketType.DEALER)) {

			dealerSocket.setIdentity(uuid.getBytes(ZMQ.CHARSET));
			dealerSocket.setImmediate(false);
			dealerSocket.connect("tcp://localhost:" + dealerPort);

			while (!Thread.currentThread().isInterrupted()) {

				try {

					String mesaj = dealerQueue.take();

					dealerSocket.send(mesaj, ZMQ.DONTWAIT);

				} catch (InterruptedException e) {

				}

			}

		}

	}

	private void sub() {

		try (ZMQ.Socket subSocket = context.createSocket(SocketType.SUB)) {

			subSocket.connect("tcp://localhost:" + subPort);
			subSocket.subscribe("\n");
			subSocket.subscribe(uuid + "\n");

			while (!Thread.currentThread().isInterrupted()) {

				subSocket.recvStr();
				String receiveStr = subSocket.recvStr();

				try {

					MesajNesnesi mesajNesnesi = gson.fromJson(receiveStr, MesajNesnesi.class);

					switch (mesajNesnesi.tip) {

					case "BCON":

						dinleyiciyeBeaconAlindi(mesajNesnesi.mesaj);

						break;

					default:

					}

				} catch (JsonSyntaxException e) {

				}

			}

		}

	}

}
