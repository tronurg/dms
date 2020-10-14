package com.ogya.dms.server.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class CommonMethods {

	private static Document confDoc;

	public static ExecutorService newSingleThreadExecutorService() {

		return Executors.newSingleThreadExecutor(new ThreadFactory() {

			@Override
			public Thread newThread(Runnable arg0) {

				Thread thread = new Thread(arg0);

				thread.setDaemon(true);

				return thread;

			}

		});

	}

	static int getIntercomPort() {

		int intercomPort = -1;

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/DMS_SERVER/INTERCOM_PORT")
					.evaluate(getConfDoc(), XPathConstants.NODE);

			intercomPort = Integer.parseInt(node.getTextContent());

		} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException e) {

		}

		return intercomPort;

	}

	static String getMulticastIp() {

		String multicastIp = "";

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/DMS_SERVER/MULTICAST_IP")
					.evaluate(getConfDoc(), XPathConstants.NODE);

			multicastIp = node.getTextContent();

		} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException e) {

		}

		return multicastIp;

	}

	static int getMulticastPort() {

		int multicastPort = -1;

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/DMS_SERVER/MULTICAST_PORT")
					.evaluate(getConfDoc(), XPathConstants.NODE);

			multicastPort = Integer.parseInt(node.getTextContent());

		} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException e) {

		}

		return multicastPort;

	}

	static int getBeaconIntervalMs() {

		int beaconIntervalMs = 2000;

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/DMS_SERVER/BEACON_INTERVAL_MS")
					.evaluate(getConfDoc(), XPathConstants.NODE);

			beaconIntervalMs = Integer.parseInt(node.getTextContent());

		} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException
				| NumberFormatException e) {

			e.printStackTrace();

		}

		return beaconIntervalMs;

	}

	static int getServerPort() {

		int serverPort = -1;

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/DMS_SERVER/SERVER_PORT")
					.evaluate(getConfDoc(), XPathConstants.NODE);

			serverPort = Integer.parseInt(node.getTextContent());

		} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException e) {

		}

		return serverPort;

	}

	static int getClientPortFrom() {

		int clientPortFrom = -1;

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/DMS_SERVER/CLIENT_PORT_FROM")
					.evaluate(getConfDoc(), XPathConstants.NODE);

			clientPortFrom = Integer.parseInt(node.getTextContent());

		} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException e) {

		}

		return clientPortFrom;

	}

	static int getClientPortTo() {

		int clientPortTo = -1;

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/DMS_SERVER/CLIENT_PORT_TO")
					.evaluate(getConfDoc(), XPathConstants.NODE);

			clientPortTo = Integer.parseInt(node.getTextContent());

		} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException e) {

		}

		return clientPortTo;

	}

	static int getPacketSize() {

		int packetSize = -1;

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/DMS_SERVER/PACKET_SIZE")
					.evaluate(getConfDoc(), XPathConstants.NODE);

			packetSize = Integer.parseInt(node.getTextContent());

		} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException e) {

		}

		return packetSize;

	}

	private static Document getConfDoc() throws SAXException, IOException, ParserConfigurationException {

		if (confDoc == null) {

			try (InputStream is = Files.newInputStream(Paths.get("./conf/dms_server.xml"))) {

				confDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(is));

			}

		}

		return confDoc;

	}

}
