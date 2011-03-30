package com.aroundroidgroup.map;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;


public class NetworkManager {

	public static String adjustRequest(String str) {
		int i;
		String s = "";
		while ((i = str.indexOf(' ')) != -1) {
			s += str.substring(0, i) + "%20";
			str = str.substring(i + 1);
		}
		s += str;
		return s;
	}

	public static String send(String str){
		try{
			URL url = new URL("http://maps.googleapis.com/maps/api/geocode/xml?address=" + adjustRequest(str) + "&sensor=false");
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
			return sb.toString();
		}catch (IOException e) {
			e.printStackTrace();
			return null;
		}

	}


}
