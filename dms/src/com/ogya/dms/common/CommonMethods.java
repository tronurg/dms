package com.ogya.dms.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

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

	private static ResourceBundle langFile;

	static String getServerIp() {

		String serverIp = "localhost";

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/DMS/SERVER_IP").evaluate(getConfDoc(),
					XPathConstants.NODE);

			serverIp = node.getTextContent();

		} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException e) {

		}

		return serverIp;

	}

	static int getServerPort() {

		int serverPort = 0;

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/DMS/SERVER_PORT").evaluate(getConfDoc(),
					XPathConstants.NODE);

			serverPort = Integer.parseInt(node.getTextContent());

		} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException e) {

			e.printStackTrace();

		}

		return serverPort;

	}

	static String getDbPath() {

		String dbPath = "./h2";

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/DMS/DB_PATH").evaluate(getConfDoc(),
					XPathConstants.NODE);

			dbPath = node.getTextContent();

		} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException e) {

		}

		return dbPath;

	}

	static int getBeaconTimeoutMs() {

		int beaconTimeoutMs = 2000;

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/DMS/BEACON_TIMEOUT_MS")
					.evaluate(getConfDoc(), XPathConstants.NODE);

			beaconTimeoutMs = Integer.parseInt(node.getTextContent());

		} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException e) {

			e.printStackTrace();

		}

		return beaconTimeoutMs;

	}

	public static String translate(String arg0) {

		try {

			return getLangFile().getString(arg0);

		} catch (Exception e) {

		}

		return arg0;

	}

	private static Document getConfDoc() throws SAXException, IOException, ParserConfigurationException {

		if (confDoc == null) {

			try (InputStream is = Files.newInputStream(Paths.get("./plugins/dms/conf/dms.xml"))) {

				confDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(is));

			}

		}

		return confDoc;

	}

	private static ResourceBundle getLangFile() throws IOException {

		if (langFile == null) {

			try (Reader reader = Files.newBufferedReader(Paths.get(
					"./plugins/dms/lang/dms_" + Locale.getDefault().getLanguage().toUpperCase() + ".properties"))) {

				langFile = new PropertyResourceBundle(reader);

			}

		}

		return langFile;

	}

}
