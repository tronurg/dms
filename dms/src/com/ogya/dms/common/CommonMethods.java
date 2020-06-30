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

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public class CommonMethods {

	private static Document confDoc;

	private static ResourceBundle langFile;

	private static Gson gson = new Gson();

	private static Gson gsonDb = new GsonBuilder().addSerializationExclusionStrategy(new ExclusionStrategy() {

		@Override
		public boolean shouldSkipField(FieldAttributes arg0) {
			return arg0.getName().equals("id") || arg0.getName().equals("messageStatus")
					|| arg0.getName().equals("statusReportStr") || arg0.getName().equals("waiting")
					|| arg0.getName().equals("date");
		}

		@Override
		public boolean shouldSkipClass(Class<?> arg0) {
			return false;
		}

	}).create();

	public static String translate(String arg0) {

		try {

			return getLangFile().getString(arg0);

		} catch (Exception e) {

		}

		return arg0;

	}

	public static String toJson(Object src) {

		return gson.toJson(src);

	}

	public static <T> T fromJson(String json, Class<T> classOfT) throws JsonSyntaxException {

		return gson.fromJson(json, classOfT);

	}

	public static String toDbJson(Object src) {

		return gsonDb.toJson(src);

	}

	public static <T> T fromDbJson(String json, Class<T> classOfT) throws JsonSyntaxException {

		return gsonDb.fromJson(json, classOfT);

	}

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

	static int getBeaconIntervalMs() {

		int beaconIntervalMs = 2000;

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/DMS/BEACON_INTERVAL_MS")
					.evaluate(getConfDoc(), XPathConstants.NODE);

			beaconIntervalMs = Integer.parseInt(node.getTextContent());

		} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException e) {

			e.printStackTrace();

		}

		return beaconIntervalMs;

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
