package com.aroundroid.aroundgps;

/***
 * Translating GPSProps to an XML representation used on server responses.
 * @author Tomer
 *
 */
public final class GPSPropXML {

	/***
	 * returning xml representation based on user's availability, checked by timeCheck and isYou.
	 * @param now the current time
	 * @param isYou shuold be true if and only if it is a debug echo of the previous user's coordinates
	 * @param gpsP gps information of the data
	 * @return a complete information of the gps status if timeCheck() or isTrue. otherwise, an empty gps status information and offline validality is returned.
	 */
	public static StringBuffer gpsPropToFriend(Long now,boolean isYou,GPSProps gpsP){
		if ((timeCheck(now,gpsP.getTimeStamp())) || isYou){
			return gpsPropToFriend( isYou, gpsP, "Yes");
		}
		else{
			return gpsPropToFriend(isYou,new GPSProps(gpsP.getUser(), gpsP.getMail(), 0.0, 0.0,0),"No");
		}
	}
	
	/***
	 * checks if the user's gps status is still valid
	 * @param now
	 * @param timeStamp
	 * @return  now - timeStamp< AroundGPSConstants.gpsValidTime
	 */
	public static boolean timeCheck(long now,long timeStamp){
		return now - timeStamp< AroundGPSConstants.gpsValidTime;
	}

	/***
	 * convert gps latitude, longitude and timestamp to xml format. also adding mail address and status validity information.
	 * @param isYou isYou shuold be true if and only if it is a debug echo of the previous user's coordinates
	 * @param gpsP hold gps latitude, longitude and timestamp, in addition to mail address
	 * @param valid validity information
	 * @return the xml representation for server response, with <You> for isYou or <Friend> for !isYou.
	 */
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

	/***
	 * returning xml representation based on user's unavailability
	 * @param isYou shuold be true if and only if it is a debug echo of the previous user's coordinates
	 * @param mail user's mail address
	 * @return unregistered gps status infromation
	 */
	public static StringBuffer mailToFriend(boolean isYou,String mail){
		return gpsPropToFriend(isYou,new GPSProps(null,mail, 0.0, 0.0,0),"Unregistered");
	}
}
