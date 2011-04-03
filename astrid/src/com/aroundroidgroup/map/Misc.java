package com.aroundroidgroup.map;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class Misc {

    static Location deviceLocation = null;

	/**
	 * The function receives type of place, radius and a coordinate.
	 * It returns all the places of the given type in the given radius from the given coordinate.
	 * May throw IOException
	 * */
	public static Map<String, String> getPlaces(String qwery, double radius, Location dp, int zoomLevel){
		Map<String, String> places = new HashMap<String, String>();
		try{
			URL url = new URL("http://maps.google.com/maps?q="+qwery+"&sll=" + dp.getLatitude() + "," + dp.getLongitude() +"&radius="+ radius + "&hl=en&z=" + zoomLevel);
			URLConnection connection = url.openConnection();
			connection.setDoOutput(true);
			connection.setConnectTimeout(10000000);
			BufferedReader in = new BufferedReader(
					new InputStreamReader(
							connection.getInputStream()));

			String decodedString;
			StringBuffer sb = new StringBuffer();
			while ((decodedString = in.readLine()) != null) {
				sb.append(decodedString);
			}
			in.close();
			int i = sb.indexOf("<span class=\"pp-place-title\"><span>");
			String str = sb.toString();
			while (i!=-1){
				str = str.substring(str.indexOf("<span class=\"pp-place-title\"><span>")+35);
				String p = str.substring(0,str.indexOf("</span>"));
				str = str.substring(str.indexOf("<span dir=\"ltr\" class=\"pp-headline-item pp-headline-address\"><span>")+67);
				String q = str.substring(0,str.indexOf("</span>"));
				i = str.indexOf("<span class=\"pp-place-title\"><span>");
				places.put(p, q);
			};
		}catch (IOException e) {
			e.printStackTrace();
		}
		return places;
	}

	/**
	 * The function remove spaces from a string and put instead %20.
	 * */
	private static String adjustRequest(String str) {
		int i;
		String s = "";
		while ((i = str.indexOf(' ')) != -1) {
			s += str.substring(0, i) + "%20";
			str = str.substring(i + 1);
		}
		s += str;
		return s;
	}

	/**
	 * The function receives a string with an address.
	 * It returns a string which represents a XML document which contains googleMaps Information.
	 * May throw IOException
	 * */
	public static String getXML(String str){
		try {
			URL url = new URL("http://maps.googleapis.com/maps/api/geocode/xml?address=" + adjustRequest(str) + "&sensor=false");
			URLConnection connection = url.openConnection();
			connection.setDoOutput(true);
			connection.setConnectTimeout(10000000);
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String decodedString;
			StringBuffer sb = new StringBuffer();
			while ((decodedString = in.readLine()) != null)
				sb.append(decodedString);
			in.close();
			return sb.toString();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * The function receives location (any textual address) and returns degree coordinates.
	 * May throw IOException.
	 * */
	public static DPoint getCoords(String location) throws IOException {
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		DPoint points = null;
		String latRelativeLocationInXML = "/GeocodeResponse/result/geometry/location/lat";
		String lngRelativeLocationInXML = "/GeocodeResponse/result/geometry/location/lng";
		try {
			String XMLStr = getXML(location);
			InputStream is = new ByteArrayInputStream(XMLStr.getBytes("UTF-8"));
			InputSource inputXml = new InputSource(is);
			NodeList nodesLat = (NodeList) xpath.evaluate(latRelativeLocationInXML, inputXml, XPathConstants.NODESET);
			is = new ByteArrayInputStream(XMLStr.getBytes("UTF-8"));
			inputXml = new InputSource(is);
			NodeList nodesLng = (NodeList) xpath.evaluate(lngRelativeLocationInXML, inputXml, XPathConstants.NODESET);
			points = new DPoint(Double.parseDouble(nodesLat.item(0).getTextContent()),
					Double.parseDouble(nodesLng.item(0).getTextContent()));
		}
		catch (XPathExpressionException ex) {
			System.out.print("XML Parsing Error");
		}
		return points;
	}

	private final LocationListener locationListener = new LocationListener() {
	    public void onLocationChanged(Location location) {
	        deviceLocation = location;
	    }

	    public void onStatusChanged(String provider, int status, Bundle extras) {}

	    public void onProviderEnabled(String provider) {    }

	    public void onProviderDisabled(String provider) {}
	};

	private void gpsSetup(Context context) {
	    LocationManager locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
	    Criteria criteria = new Criteria();
	    criteria.setAccuracy(Criteria.ACCURACY_FINE);
	    criteria.setAltitudeRequired(false);
	    criteria.setBearingRequired(false);
	    criteria.setCostAllowed(true);
	    criteria.setPowerRequirement(Criteria.POWER_LOW);
	    String provider = locationManager.getBestProvider(criteria, true);
	    Location location = locationManager.getLastKnownLocation(provider);
	    locationManager.requestLocationUpdates(provider, 2000, 10, locationListener);
	}

	public static Location getDeviceLocation() {
	    return deviceLocation;
	}

	public static Map<String, String> getPlacesAroundMe(String str, double radius, int zoomLevel) {
	    if (deviceLocation != null)
	        return getPlaces(str, radius, getDeviceLocation(), zoomLevel);
	    return null;
	}
}
