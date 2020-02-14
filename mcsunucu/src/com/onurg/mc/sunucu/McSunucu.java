package com.onurg.mc.sunucu;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class McSunucu {

	private final ZContext context = new ZContext();

	private final int routerPort;
	private final int pubPort;

	private final Map<String, UdpYonetici> udpYoneticiler = new HashMap<String, UdpYonetici>();

	private final LinkedBlockingQueue<MesajNesnesi> pubQueue = new LinkedBlockingQueue<MesajNesnesi>();

	private final Gson gson = new Gson();

	public static void main(String[] args) {

		try {

			new McSunucu();

		} catch (Exception e) {

			System.exit(-1);

		}

	}

	private McSunucu() throws Exception {

		try (InputStream is = Files.newInputStream(Paths.get("./conf/mcsunucu.xml"))) {

			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(is));

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/MC_SUNUCU/PORT").evaluate(doc,
					XPathConstants.NODE);

			int comPort = Integer.parseInt(node.getTextContent());

			routerPort = comPort;
			pubPort = comPort + 1;

			NodeList nodeList = (NodeList) XPathFactory.newInstance().newXPath().compile("/MC_SUNUCU/SOKETLER/SOKET")
					.evaluate(doc, XPathConstants.NODESET);

			for (int i = 0; i < nodeList.getLength(); i++) {

				NamedNodeMap nnm = nodeList.item(i).getAttributes();

				try {

					String ip = nnm.getNamedItem("ip").getNodeValue();
					String port = nnm.getNamedItem("port").getNodeValue();

					if (!udpYoneticiler.containsKey(ip)) {

						udpYoneticiler.put(ip, new UdpYonetici(ip, Integer.parseInt(port), this::receiveUdpMessage));

					}

				} catch (Exception e) {

				}

			}

		}

		new Thread(this::router).start();
		new Thread(this::pub).start();

	}

	private void receiveUdpMessage(final UdpNesnesi udpNesnesi) {

		try {

			MesajNesnesi mesajNesnesi = new MesajNesnesi(gson.fromJson(udpNesnesi.mesaj, MesajNesnesi.class),
					udpNesnesi.gonderenPort, udpNesnesi.aliciPort);

			pubQueue.offer(mesajNesnesi);

		} catch (JsonSyntaxException e) {

		}

	}

	private void router() {

		try (ZMQ.Socket routerSocket = context.createSocket(SocketType.ROUTER)) {

			routerSocket.bind("tcp://*:" + routerPort);

			while (!Thread.currentThread().isInterrupted()) {

				routerSocket.recvStr();
				String mesaj = routerSocket.recvStr();

				try {

					MesajNesnesi mesajNesnesi = gson.fromJson(mesaj, MesajNesnesi.class);

					UdpYonetici udpYonetici = udpYoneticiler.get(mesajNesnesi.gonderenIp);

					if (udpYonetici == null)
						continue;

					udpYonetici.gonder(mesaj, mesajNesnesi.aliciIp);

				} catch (JsonSyntaxException e) {

				}

			}

		} catch (Exception e) {

			System.out.println(routerPort + " portu kullaniliyor. Istemcilerden veri alinamayacak!");

		}

	}

	private void pub() {

		try (ZMQ.Socket pubSocket = context.createSocket(SocketType.PUB)) {

			pubSocket.bind("tcp://*:" + pubPort);

			while (!Thread.currentThread().isInterrupted()) {

				try {

					MesajNesnesi mesajNesnesi = pubQueue.take();

					String mesaj = gson.toJson(mesajNesnesi);

					pubSocket.sendMore(mesajNesnesi.aliciId + "\n");
					pubSocket.send(mesaj);

				} catch (InterruptedException e) {

				}

			}

		} catch (Exception e) {

			System.out.println(pubPort + " portu kullaniliyor. Istemcilere veri gonderilemeyecek!");

		}

	}

}
