package com.ogya.dms.core.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.ogya.dms.core.database.tables.Message;
import com.ogya.dms.core.database.tables.StatusReport;
import com.ogya.dms.core.structures.Availability;
import com.ogya.dms.core.structures.MessageStatus;
import com.ogya.dms.core.structures.MessageSubType;
import com.ogya.dms.core.structures.MessageType;
import com.ogya.dms.core.structures.ReceiverType;
import com.ogya.dms.core.view.ReportsPane.ReportTemplate;

public class CommonMethods {

	private static Document confDoc;

	private static ResourceBundle langFile;

	private static final List<String> messageGsonExcludedNames = Arrays.asList("messageDirection", "messageStatus",
			"waitStatus", "statusReports", "date", "contact", "owner", "dgroup", "apiFlag");
	private static final List<String> statusReportGsonExcludedNames = Arrays.asList("id", "message");

	private static Gson gson = new GsonBuilder()
			.registerTypeAdapter(MessageStatus.class, new TypeAdapter<MessageStatus>() {

				@Override
				public MessageStatus read(JsonReader reader) throws IOException {
					if (reader.peek() == JsonToken.NULL) {
						reader.nextNull();
						return null;
					}
					return MessageStatus.values()[reader.nextInt()];
				}

				@Override
				public void write(JsonWriter writer, MessageStatus value) throws IOException {
					if (value == null) {
						writer.nullValue();
						return;
					}
					writer.value(value.ordinal());
				}

			}).registerTypeAdapter(ReceiverType.class, new TypeAdapter<ReceiverType>() {

				@Override
				public ReceiverType read(JsonReader reader) throws IOException {
					if (reader.peek() == JsonToken.NULL) {
						reader.nextNull();
						return null;
					}
					return ReceiverType.values()[reader.nextInt()];
				}

				@Override
				public void write(JsonWriter writer, ReceiverType value) throws IOException {
					if (value == null) {
						writer.nullValue();
						return;
					}
					writer.value(value.ordinal());
				}

			}).create();

	private static Gson gsonMessage = new GsonBuilder().addSerializationExclusionStrategy(new ExclusionStrategy() {

		@Override
		public boolean shouldSkipField(FieldAttributes arg0) {
			return messageGsonExcludedNames.contains(arg0.getName());
		}

		@Override
		public boolean shouldSkipClass(Class<?> arg0) {
			return false;
		}

	}).registerTypeAdapter(Availability.class, new TypeAdapter<Availability>() {

		@Override
		public Availability read(JsonReader reader) throws IOException {
			if (reader.peek() == JsonToken.NULL) {
				reader.nextNull();
				return null;
			}
			return Availability.values()[reader.nextInt()];
		}

		@Override
		public void write(JsonWriter writer, Availability value) throws IOException {
			if (value == null) {
				writer.nullValue();
				return;
			}
			writer.value(value.ordinal());
		}

	}).registerTypeAdapter(MessageType.class, new TypeAdapter<MessageType>() {

		@Override
		public MessageType read(JsonReader reader) throws IOException {
			if (reader.peek() == JsonToken.NULL) {
				reader.nextNull();
				return null;
			}
			return MessageType.values()[reader.nextInt()];
		}

		@Override
		public void write(JsonWriter writer, MessageType value) throws IOException {
			if (value == null) {
				writer.nullValue();
				return;
			}
			writer.value(value.ordinal());
		}

	}).registerTypeAdapter(MessageSubType.class, new TypeAdapter<MessageSubType>() {

		@Override
		public MessageSubType read(JsonReader reader) throws IOException {
			if (reader.peek() == JsonToken.NULL) {
				reader.nextNull();
				return null;
			}
			return MessageSubType.values()[reader.nextInt()];
		}

		@Override
		public void write(JsonWriter writer, MessageSubType value) throws IOException {
			if (value == null) {
				writer.nullValue();
				return;
			}
			writer.value(value.ordinal());
		}

	}).registerTypeAdapter(ReceiverType.class, new TypeAdapter<ReceiverType>() {

		@Override
		public ReceiverType read(JsonReader reader) throws IOException {
			if (reader.peek() == JsonToken.NULL) {
				reader.nextNull();
				return null;
			}
			return ReceiverType.values()[reader.nextInt()];
		}

		@Override
		public void write(JsonWriter writer, ReceiverType value) throws IOException {
			if (value == null) {
				writer.nullValue();
				return;
			}
			writer.value(value.ordinal());
		}

	}).create();

	private static Gson gsonStatusReport = new GsonBuilder().addSerializationExclusionStrategy(new ExclusionStrategy() {

		@Override
		public boolean shouldSkipField(FieldAttributes arg0) {
			return statusReportGsonExcludedNames.contains(arg0.getName());
		}

		@Override
		public boolean shouldSkipClass(Class<?> arg0) {
			return false;
		}

	}).registerTypeAdapter(MessageStatus.class, new TypeAdapter<MessageStatus>() {

		@Override
		public MessageStatus read(JsonReader reader) throws IOException {
			if (reader.peek() == JsonToken.NULL) {
				reader.nextNull();
				return null;
			}
			return MessageStatus.values()[reader.nextInt()];
		}

		@Override
		public void write(JsonWriter writer, MessageStatus value) throws IOException {
			if (value == null) {
				writer.nullValue();
				return;
			}
			writer.value(value.ordinal());
		}

	}).create();

	private static final Map<String, String> genericConversionMap = new HashMap<String, String>();
	private static final Map<String, Map<String, String>> customConversionMap = new HashMap<String, Map<String, String>>();

	static {

		try (Reader reader = Files.newBufferedReader(Paths.get("./plugins/dms/dictionary/dictionary.properties"))) {

			Properties dictionary = new Properties();

			dictionary.load(reader);

			dictionary.stringPropertyNames().forEach(key -> {

				int indexOfDot = key.indexOf('.');

				String convertedName = dictionary.getProperty(key);

				if (indexOfDot < 0) {

					genericConversionMap.put(key, convertedName);

				} else {

					String className = key.substring(0, indexOfDot);

					String fieldName = key.substring(indexOfDot + 1);

					customConversionMap.putIfAbsent(className, new HashMap<String, String>());

					customConversionMap.get(className).put(fieldName, convertedName);

				}

			});

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	public static List<ReportTemplate> getReportTemplates() {

		final List<ReportTemplate> templates = new ArrayList<ReportTemplate>();

		try {

			Files.list(Paths.get("./plugins/dms/templates")).forEach(path -> {

				if (Files.isDirectory(path) || !path.toString().toLowerCase().endsWith(".txt"))
					return;

				try (Reader reader = Files.newBufferedReader(path)) {

					StringBuilder stringBuilder = new StringBuilder();

					int c;

					while ((c = reader.read()) != -1)
						stringBuilder.append((char) c);

					String fileName = path.getFileName().toString();

					templates.add(
							new ReportTemplate(fileName.substring(0, fileName.length() - 4), stringBuilder.toString()));

				} catch (IOException e) {

					e.printStackTrace();

				}

			});

		} catch (IOException e) {

			e.printStackTrace();

		}

		return templates;

	}

	public static void writeReport(Path path, String header, List<String> paragraphs) {

		try (PDDocument document = new PDDocument()) {

			PDFont font = PDType0Font.load(document,
					CommonMethods.class.getResourceAsStream("/resources/font/arial.ttf"));

			final float fontSize = 12f;

			final float lineWidth = 500f;

			List<String> lines = new ArrayList<String>();

			lines.addAll(splitParagraph(header, lineWidth, font, fontSize));

			lines.add("");

			paragraphs.forEach(paragraph -> lines.addAll(splitParagraph(paragraph, lineWidth, font, fontSize)));

			for (int j = 0; j < lines.size(); j += 30) {

				PDPage page = new PDPage();

				document.addPage(page);

				try (PDPageContentStream pageContentStream = new PDPageContentStream(document, page)) {

					pageContentStream.setFont(font, fontSize);

					pageContentStream.setLeading(21f);

					pageContentStream.beginText();

					pageContentStream.newLineAtOffset(50, 700);

					for (int i = j; i < j + 30 && i < lines.size(); ++i) {

						pageContentStream.showText(lines.get(i));
						pageContentStream.newLine();

					}

					pageContentStream.endText();

				} catch (Exception e) {

					e.printStackTrace();

				}

			}

			document.save(path.toFile());

		} catch (Exception e) {

			e.printStackTrace();

		}

	}

	public static String translate(String arg0) {

		try {

			return getLangFile().getString(arg0);

		} catch (Exception e) {

		}

		return arg0;

	}

	public static String convertListJsonToCommon(String listJson, String className) {

		Map<String, String> dictionary = new HashMap<String, String>(genericConversionMap);

		if (customConversionMap.containsKey(className))
			dictionary.putAll(customConversionMap.get(className));

		for (String key : dictionary.keySet()) {

			String value = dictionary.get(key);

			listJson = listJson.replaceAll(String.format("\"%s\":", key), String.format("\"%s\":", value));

		}

		return listJson;

	}

	public static String convertListJsonFromCommon(String commonJson, String className) {

		Map<String, String> dictionary = new HashMap<String, String>(genericConversionMap);

		if (customConversionMap.containsKey(className))
			dictionary.putAll(customConversionMap.get(className));

		for (String key : dictionary.keySet()) {

			String value = dictionary.get(key);

			commonJson = commonJson.replaceAll(String.format("\"%s\":", value), String.format("\"%s\":", key));

		}

		return commonJson;

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

	public static String toMessageJson(Message src) {

		return gsonMessage.toJson(src);

	}

	public static Message fromMessageJson(String json) throws Exception {

		Message result = gsonMessage.fromJson(json, Message.class);

		if (result == null)
			throw new Exception();

		return result;

	}

	public static String toStatusReportJson(Set<StatusReport> src) {

		return gsonStatusReport.toJson(src);

	}

	public static StatusReport[] fromStatusReportJson(String json) throws Exception {

		StatusReport[] result = gsonStatusReport.fromJson(json, StatusReport[].class);

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

	static long getMaxFileLength() {

		long maxFileLength = 1000000;

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/DMS/MAX_FILE_LENGTH")
					.evaluate(getConfDoc(), XPathConstants.NODE);

			maxFileLength = Long.parseLong(node.getTextContent());

		} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException
				| NumberFormatException e) {

			e.printStackTrace();

		}

		return maxFileLength;

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

	private static List<String> splitParagraph(String paragraph, float lineWidth, PDFont font, float fontSize) {

		final String space = " ";

		String[] words = paragraph.split(space);

		List<String> lines = new ArrayList<String>();

		StringBuffer lineBuffer = new StringBuffer();

		for (String word : words) {

			try {

				if (getStringWidth(word, font, fontSize) > lineWidth) {

					for (int index = 0; index < word.length(); ++index) {

						char chr = word.charAt(index);

						if (getStringWidth(lineBuffer.toString() + chr, font, fontSize) > lineWidth) {

							lines.add(lineBuffer.toString());

							lineBuffer.delete(0, lineBuffer.length());

						}

						lineBuffer.append(chr);

					}

					lineBuffer.append(space);

					continue;

				}

				if (getStringWidth(lineBuffer.toString() + word, font, fontSize) > lineWidth) {

					lines.add(lineBuffer.toString());

					lineBuffer.delete(0, lineBuffer.length());

				}

				if (word.isEmpty() && lineBuffer.length() == 0 && lines.size() > 0)
					continue;

				lineBuffer.append(word);
				lineBuffer.append(space);

			} catch (IOException e) {

			}

		}

		if (lineBuffer.length() > 0)
			lines.add(lineBuffer.toString());

		return lines;

	}

	private static float getStringWidth(String str, PDFont font, float fontSize) throws IOException {

		return font.getStringWidth(str) * fontSize / 1000;

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
