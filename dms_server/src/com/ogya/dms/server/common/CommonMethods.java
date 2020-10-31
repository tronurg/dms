package com.ogya.dms.server.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class CommonMethods {

	private static Document confDoc;

	private static final List<String> gsonExcludedNames = Arrays.asList("id", "senderUuid", "messageStatus",
			"statusReportStr", "waitStatus", "date");

	private static Gson gson = new Gson();

	private static Gson gsonRemote = new GsonBuilder().addSerializationExclusionStrategy(new ExclusionStrategy() {

		@Override
		public boolean shouldSkipField(FieldAttributes arg0) {
			return gsonExcludedNames.contains(arg0.getName());
		}

		@Override
		public boolean shouldSkipClass(Class<?> arg0) {
			return false;
		}

	}).create();

	public static String toJson(Object src) {

		return gson.toJson(src);

	}

	public static <T> T fromJson(String json, Class<T> classOfT) throws Exception {

		T result = gson.fromJson(json, classOfT);

		if (result == null)
			throw new Exception();

		return result;

	}

	public static String toRemoteJson(Object src) {

		return gsonRemote.toJson(src);

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
