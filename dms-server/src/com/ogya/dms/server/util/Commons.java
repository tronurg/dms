package com.ogya.dms.server.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Commons {

	public static final String DMS_UUID = UUID.randomUUID().toString();

	public static final int INTERCOM_PORT = getIntercomPort();
	public static final String MULTICAST_GROUP = getMulticastGroup();
	public static final int BEACON_PORT = getBeaconPort();
	public static final int SERVER_PORT = getServerPort();
	public static final int CLIENT_PORT_FROM = getClientPortFrom();
	public static final int CLIENT_PORT_TO = getClientPortTo();
	public static final int BEACON_INTERVAL_MS = getBeaconIntervalMs();
	public static final List<InetAddress> PREFERRED_IPS = getPreferredIps();

	public static final int CONN_TIMEOUT_MS = 5000;

	private static Document confDoc;

	public static List<InetAddress> getLocalIPv4Addresses() {

		List<InetAddress> addrs = new ArrayList<InetAddress>();

		try {
			for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
				for (InetAddress ia : Collections.list(ni.getInetAddresses())) {
					if ((ia instanceof Inet4Address) && !ia.isLoopbackAddress()) {
						addrs.add(ia);
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}

		return addrs;

	}

	public static int getLocalAddressPriority(InetAddress localAddress) {

		int priority = PREFERRED_IPS.indexOf(localAddress);

		if (priority < 0)
			priority = PREFERRED_IPS.size();

		return priority;

	}

	private static Document getConfDoc() throws SAXException, IOException, ParserConfigurationException {

		if (confDoc == null) {

			try (InputStream is = Files.newInputStream(Paths.get("./conf/dms_server.xml"))) {

				confDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(is));

			}

		}

		return confDoc;

	}

	private static int getIntercomPort() {

		int intercomPort = -1;

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/DMS_SERVER/INTERCOM_PORT")
					.evaluate(getConfDoc(), XPathConstants.NODE);

			intercomPort = Integer.parseInt(node.getTextContent());

		} catch (Exception e) {
			e.printStackTrace();
		}

		return intercomPort;

	}

	private static String getMulticastGroup() {

		String multicastGroup = "";

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/DMS_SERVER/MULTICAST_GROUP")
					.evaluate(getConfDoc(), XPathConstants.NODE);

			multicastGroup = node.getTextContent();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return multicastGroup;

	}

	private static int getBeaconPort() {

		int beaconPort = -1;

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/DMS_SERVER/BEACON_PORT")
					.evaluate(getConfDoc(), XPathConstants.NODE);

			beaconPort = Integer.parseInt(node.getTextContent());

		} catch (Exception e) {
			e.printStackTrace();
		}

		return beaconPort;

	}

	private static int getServerPort() {

		int serverPort = -1;

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/DMS_SERVER/SERVER_PORT")
					.evaluate(getConfDoc(), XPathConstants.NODE);

			serverPort = Integer.parseInt(node.getTextContent());

		} catch (Exception e) {
			e.printStackTrace();
		}

		return serverPort;

	}

	private static int getClientPortFrom() {

		int clientPortFrom = -1;

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/DMS_SERVER/CLIENT_PORT_FROM")
					.evaluate(getConfDoc(), XPathConstants.NODE);

			clientPortFrom = Integer.parseInt(node.getTextContent());

		} catch (Exception e) {
			e.printStackTrace();
		}

		return clientPortFrom;

	}

	private static int getClientPortTo() {

		int clientPortTo = -1;

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/DMS_SERVER/CLIENT_PORT_TO")
					.evaluate(getConfDoc(), XPathConstants.NODE);

			clientPortTo = Integer.parseInt(node.getTextContent());

		} catch (Exception e) {
			e.printStackTrace();
		}

		return clientPortTo;

	}

	private static int getBeaconIntervalMs() {

		int beaconIntervalMs = 5000;

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/DMS_SERVER/BEACON_INTERVAL_MS")
					.evaluate(getConfDoc(), XPathConstants.NODE);

			beaconIntervalMs = Integer.parseInt(node.getTextContent());

			if (beaconIntervalMs < 1000)
				beaconIntervalMs = 1000;

		} catch (NullPointerException e) {
			System.out.println(String.format("BEACON_INTERVAL_MS set to default: %d", beaconIntervalMs));
		} catch (Exception e) {

		}

		return beaconIntervalMs;

	}

	private static List<InetAddress> getPreferredIps() {

		List<InetAddress> preferredIps = new ArrayList<InetAddress>();

		try {

			NodeList nodeList = (NodeList) XPathFactory.newInstance().newXPath().compile("/DMS_SERVER/PREFERRED_IPS/IP")
					.evaluate(getConfDoc(), XPathConstants.NODESET);

			for (int i = 0; i < nodeList.getLength(); ++i) {
				Node node = nodeList.item(i);
				try {
					preferredIps.add(InetAddress.getByName(node.getTextContent()));
				} catch (UnknownHostException e) {

				}
			}

		} catch (Exception e) {

		}

		return preferredIps;

	}

}
