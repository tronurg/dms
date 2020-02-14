package com.onurg.mc.istemci;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class McIstemci {

	private final String id;

	private final ZContext context = new ZContext();

	private final int dealerPort;
	private final int subPort;

	private final Consumer<MesajNesnesi> dinleyici;

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

	public McIstemci(String id, int comPort, Consumer<MesajNesnesi> dinleyici) {

		this.id = id;

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

	public void mesajGonder(byte[] mesaj, String gonderenIp, String aliciIp) {

		dealerQueue.offer(gson.toJson(new MesajNesnesi(mesaj, gonderenIp, id, aliciIp)));

	}

	public void mesajGonder(byte[] mesaj, String gonderenIp, String aliciIp, String aliciId) {

		dealerQueue.offer(gson.toJson(new MesajNesnesi(mesaj, gonderenIp, id, aliciIp, aliciId)));

	}

	private void dinleyiciyeMesajAlindi(MesajNesnesi mesajNesnesi) {

		out.execute(() -> {

			dinleyici.accept(mesajNesnesi);

		});

	}

	private void dealer() {

		try (ZMQ.Socket dealerSocket = context.createSocket(SocketType.DEALER)) {

			dealerSocket.setIdentity(id.getBytes(ZMQ.CHARSET));
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
			subSocket.subscribe(id + "\n");

			while (!Thread.currentThread().isInterrupted()) {

				subSocket.recvStr();
				String receiveStr = subSocket.recvStr();

				try {

					MesajNesnesi mesajNesnesi = gson.fromJson(receiveStr, MesajNesnesi.class);

					dinleyiciyeMesajAlindi(mesajNesnesi);

				} catch (JsonSyntaxException e) {

				}

			}

		}

	}

}
