package com.aselsan.rehis.reform.mcsy.mcsunucu.ortak;

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

	static int getIntercomPort() {

		int intercomPort = -1;

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/MC_SUNUCU/INTERCOM_PORT")
					.evaluate(getConfDoc(), XPathConstants.NODE);

			intercomPort = Integer.parseInt(node.getTextContent());

		} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException e) {

		}

		return intercomPort;

	}

	static String getMulticastIp() {

		String multicastIp = "";

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/MC_SUNUCU/MULTICAST_IP")
					.evaluate(getConfDoc(), XPathConstants.NODE);

			multicastIp = node.getTextContent();

		} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException e) {

		}

		return multicastIp;

	}

	static int getMulticastPort() {

		int multicastPort = -1;

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/MC_SUNUCU/MULTICAST_PORT")
					.evaluate(getConfDoc(), XPathConstants.NODE);

			multicastPort = Integer.parseInt(node.getTextContent());

		} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException e) {

		}

		return multicastPort;

	}

	static int getComPort() {

		int comPort = -1;

		try {

			Node node = (Node) XPathFactory.newInstance().newXPath().compile("/MC_SUNUCU/COM_PORT")
					.evaluate(getConfDoc(), XPathConstants.NODE);

			comPort = Integer.parseInt(node.getTextContent());

		} catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException e) {

		}

		return comPort;

	}

	private static Document getConfDoc() throws SAXException, IOException, ParserConfigurationException {

		if (confDoc == null) {

			try (InputStream is = Files.newInputStream(Paths.get("./conf/mcsunucu.xml"))) {

				confDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(is));

			}

		}

		return confDoc;

	}

}
