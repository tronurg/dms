package com.dms.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import com.dms.core.view.ReportsPane.ReportTemplate;

public class Commons {

	public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("uuuuMMddHHmmss");

	public static final int UNITS_PER_PAGE = 50;
	public static final int MAX_PAGES = 5;
	public static final int CHUNK_SIZE = 8192;

	public static String SERVER_IP;
	public static Integer SERVER_PORT;
	public static String DB_PATH;
	public static String FILE_EXPLORER_PATH;
	public static Long MAX_FILE_LENGTH;
	public static Long SMALL_FILE_LIMIT;
	public static String SEND_FOLDER;
	public static String RECEIVE_FOLDER;
	public static Boolean AUTO_OPEN_FILE;

	public static final List<ReportTemplate> REPORT_TEMPLATES = new ArrayList<ReportTemplate>();

	private static ResourceBundle langFile;

	public static void writeReport(Path path, String header, List<String> paragraphs) {

		try (PDDocument document = new PDDocument()) {

			PDFont font = PDType0Font.load(document, Commons.class.getResourceAsStream("/resources/font/arial.ttf"));

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

	public static String convertDoubleToCoordinates(double latitude, double longitude) {

		String north = translate("NORTH");
		String south = translate("SOUTH");
		String east = translate("EAST");
		String west = translate("WEST");
		char northInitial = north.isEmpty() ? 'N' : north.charAt(0);
		char southInitial = south.isEmpty() ? 'S' : south.charAt(0);
		char eastInitial = east.isEmpty() ? 'E' : east.charAt(0);
		char westInitial = west.isEmpty() ? 'W' : west.charAt(0);

		return String.format("%s%c, %s%c", convertDoubleToCoordinates(Math.abs(latitude)),
				latitude < 0 ? southInitial : northInitial, convertDoubleToCoordinates(Math.abs(longitude)),
				longitude < 0 ? westInitial : eastInitial);

	}

	private static ResourceBundle getLangFile() throws IOException {

		if (langFile == null) {

			langFile = ResourceBundle.getBundle("resources/lang/dms");

		}

		return langFile;

	}

	public static void addReportTemplate(String reportTemplatePath) {

		Path path = Paths.get(reportTemplatePath);

		if (Files.isDirectory(path) || !path.toString().toLowerCase(Locale.getDefault()).endsWith(".txt")) {
			return;
		}

		try (BufferedReader reader = Files.newBufferedReader(path)) {

			String firstLine = reader.readLine().replace("\uFEFF", "");

			if (!firstLine.startsWith("#")) {
				return;
			}

			Integer reportId = Integer.parseInt(firstLine.substring(1));

			StringBuilder stringBuilder = new StringBuilder();

			int c;

			while ((c = reader.read()) != -1)
				stringBuilder.append((char) c);

			String fileName = path.getFileName().toString();

			REPORT_TEMPLATES.add(new ReportTemplate(reportId, fileName.substring(0, fileName.length() - 4),
					stringBuilder.toString()));

		} catch (Exception e) {

			e.printStackTrace();

		}

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

				if (word.isEmpty() && lineBuffer.length() == 0 && lines.size() > 0) {
					continue;
				}

				lineBuffer.append(word);
				lineBuffer.append(space);

			} catch (IOException e) {

			}

		}

		if (lineBuffer.length() > 0) {
			lines.add(lineBuffer.toString());
		}

		return lines;

	}

	private static float getStringWidth(String str, PDFont font, float fontSize) throws IOException {

		return font.getStringWidth(str) * fontSize / 1000;

	}

	private static String convertDoubleToCoordinates(double value) {

		BigDecimal bdeg = BigDecimal.valueOf(value);
		BigDecimal bmin = bdeg.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(60));
		BigDecimal bsec = bmin.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(60));

		return String.format("%02d\u00B0%02d'%02d\"", bdeg.intValue(), bmin.intValue(), bsec.intValue());

	}

}
