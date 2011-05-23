package com.aroundroid.aroundgps;

public final class GPSPropXML {
	public static StringBuffer gpsPropToFriend(boolean isYou,GPSProps gpsP){
		StringBuffer out = new StringBuffer();
		out.append((isYou?"<You>":"<Friend>")).append("\n");
		out.append("<Mail>"+ gpsP.getMail() +"</Mail>").append("\n");
		out.append("<Latitude>"+ gpsP.getLat() +"</Latitude>").append("\n");
		out.append("<Longtitude>"+ gpsP.getLong() +"</Longtitude>").append("\n");
		out.append("<Timestamp>"+ gpsP.getTimeStamp() +"</Timestamp>").append("\n");
	    out.append((isYou?"</You>":"</Friend>")).append("\n");
	    return out;
	}
}
