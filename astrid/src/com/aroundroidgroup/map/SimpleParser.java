package com.aroundroidgroup.map;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class SimpleParser {

	public static DPoint getCoords(String location) throws IOException {
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		DPoint points = null;
		try {
			String s = NetworkManager.send(location);
			InputStream is = new ByteArrayInputStream(s.getBytes("UTF-8"));
			InputSource inputXml = new InputSource(is);
			NodeList nodesLat = (NodeList) xpath.evaluate("/GeocodeResponse/result/geometry/location/lat", inputXml, XPathConstants.NODESET);
			is = new ByteArrayInputStream(s.getBytes("UTF-8"));
			inputXml = new InputSource(is);
			NodeList nodesLng = (NodeList) xpath.evaluate("/GeocodeResponse/result/geometry/location/lng", inputXml, XPathConstants.NODESET);
			points = new DPoint(Double.parseDouble(nodesLat.item(0).getTextContent()),
									  Double.parseDouble(nodesLng.item(0).getTextContent()));
		}
		 catch (XPathExpressionException ex) {
			System.out.print("XML Parsing Error");
		}
		return points;
	}
}