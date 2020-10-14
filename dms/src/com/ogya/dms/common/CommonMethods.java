package com.ogya.dms.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
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

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class CommonMethods {

	private static Document confDoc;

	private static ResourceBundle langFile;

	private static final List<String> gsonExcludedNames = Arrays.asList("id", "senderUuid", "messageStatus",
			"statusReportStr", "waitStatus", "date");

	private static Gson gson = new Gson();

	private static Gson gsonDb = new GsonBuilder().addSerializationExclusionStrategy(new ExclusionStrategy() {

		@Override
		public boolean shouldSkipField(FieldAttributes arg0) {
			return gsonExcludedNames.contains(arg0.getName());
		}

		@Override
		public boolean shouldSkipClass(Class<?> arg0) {
			return false;
		}

	}).create();

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

	public static <T> T fromJson(String json, Class<T> classOfT) throws Exception {

		T result = gson.fromJson(json, classOfT);

		if (result == null)
			throw new Exception();

		return result;

	}

	public static String toDbJson(Object src) {

		return gsonDb.toJson(src);

	}

	public static <T> T fromDbJson(String json, Class<T> classOfT) throws Exception {

		T result = gsonDb.fromJson(json, classOfT);

		if (result == null)
			throw new Exception();

		return result;

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

		} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException
				| NumberFormatException e) {

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

	static String getFileExplorerPath() {

		String fileExplorerPath = "./";

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/DMS/FILE_EXPLORER_PATH")
					.evaluate(getConfDoc(), XPathConstants.NODE);

			fileExplorerPath = node.getTextContent();

		} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException e) {

			e.printStackTrace();

		}

		return fileExplorerPath;

	}

	static long getMaxFileLenght() {

		long maxFileLenght = 1000000;

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/DMS/MAX_FILE_LENGHT")
					.evaluate(getConfDoc(), XPathConstants.NODE);

			maxFileLenght = Long.parseLong(node.getTextContent());

		} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException
				| NumberFormatException e) {

			e.printStackTrace();

		}

		return maxFileLenght;

	}

	static String getSendFolder() {

		String sendFolder = "./sent";

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/DMS/SEND_FOLDER").evaluate(getConfDoc(),
					XPathConstants.NODE);

			sendFolder = node.getTextContent();

		} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException e) {

			e.printStackTrace();

		}

		return sendFolder;

	}

	static String getReceiveFolder() {

		String receiveFolder = "./receive";

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/DMS/RECEIVE_FOLDER")
					.evaluate(getConfDoc(), XPathConstants.NODE);

			receiveFolder = node.getTextContent();

		} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException e) {

			e.printStackTrace();

		}

		return receiveFolder;

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
