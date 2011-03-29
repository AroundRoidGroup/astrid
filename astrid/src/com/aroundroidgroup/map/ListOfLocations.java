package com.aroundroidgroup.map;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;


public class ListOfLocations {
	public static Map<String, String> send(String qwery, double radios, DPoint dp){
		Map<String, String> places = new HashMap<String, String>();
		try{
			URL url = new URL("http://maps.google.com/maps?q="+qwery+"&sll=" + dp.getX() + "," + dp.getY() +"&radius="+10 + "&hl=en");
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


}
