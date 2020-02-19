package com.aselsan.rehis.reform.mcsy.ortak;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class OrtakMetotlar {

	private static Document confDoc;

	static String getSunucuIp() {

		String sunucuIp = "localhost";

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/MCSY/SUNUCU_IP").evaluate(getConfDoc(),
					XPathConstants.NODE);

			sunucuIp = node.getTextContent();

		} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException e) {

		}

		return sunucuIp;

	}

	static int getSunucuPort() {

		int sunucuPort = 0;

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/MCSY/SUNUCU_PORT").evaluate(getConfDoc(),
					XPathConstants.NODE);

			sunucuPort = Integer.parseInt(node.getTextContent());

		} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException e) {

			e.printStackTrace();

		}

		return sunucuPort;

	}

	private static Document getConfDoc() throws SAXException, IOException, ParserConfigurationException {

		if (confDoc == null) {

			try (InputStream is = Files.newInputStream(Paths.get("./plugins/mcsy/conf/mcsy.xml"))) {

				confDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(is));

			}

		}

		return confDoc;

	}

}
