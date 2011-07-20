package com.aroundroid.aroundgps;

public final class GPSPropXML {


	public static StringBuffer gpsPropToFriend(Long now,boolean isYou,GPSProps gpsP){
		if ((now - gpsP.getTimeStamp()< AroundGPSConstants.gpsValidTime) || isYou){
			return gpsPropToFriend( isYou, gpsP, "Yes");
		}
		else{
			return gpsPropToFriend(isYou,new GPSProps(gpsP.getUser(), gpsP.getMail(), 0.0, 0.0,0),"No");
		}
	}

	private static StringBuffer gpsPropToFriend(boolean isYou,GPSProps gpsP, String valid){
		StringBuffer out = new StringBuffer();
		out.append((isYou?"<You>":"<Friend>")).append("\n");
		out.append("<Mail>"+ gpsP.getMail() +"</Mail>").append("\n");
		out.append("<Valid>"+valid+"</Valid>");
		out.append("<Latitude>"+ gpsP.getLat() +"</Latitude>").append("\n");
		out.append("<Longtitude>"+ gpsP.getLong() +"</Longtitude>").append("\n");
		out.append("<Timestamp>"+ gpsP.getTimeStamp() +"</Timestamp>").append("\n");
		out.append((isYou?"</You>":"</Friend>")).append("\n");
		return out;
	}

	public static StringBuffer mailToFriend(boolean isYou,String mail){
		return gpsPropToFriend(isYou,new GPSProps(null,mail, 0.0, 0.0,0),"Unregistered");
	}
}
