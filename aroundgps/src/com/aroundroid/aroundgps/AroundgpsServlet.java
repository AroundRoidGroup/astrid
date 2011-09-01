package com.aroundroid.aroundgps;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jdo.PersistenceManager;
import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheManager;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.jsr107cache.GCacheFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

@SuppressWarnings("serial")
/***
 * A servlet for handling a request for gps coordinates.
 * updates user's coordinates, and responses with a list of requested coordinates
 */
public class AroundgpsServlet extends HttpServlet {

	private Date requestDate;

	private final static String GPSLat = "GPSLAT";
	private final static String GPSLon = "GPSLON";
	private final static String USERS = "USERS";
	private final static String TIMESTAMP = "TIMESTAMP";

	private final static String selectStringStart = "select from "+ GPSProps.class.getName()+" where mail =='";

	/***
	 * build select query for selecting the user with the mail 'friend'
	 * @param friend
	 * @return
	 */
	private String buildGetQuery(String friend){
		return (selectStringStart + friend.toLowerCase() + "'");
	}

	/***
	 * get is not supposed to be supported by this servlet, so it just redirects to welcome page (for debbuging purposes)
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws IOException {
		resp.sendRedirect("welcome.jsp");
	}

	/***
	 * checks if the array 'userArr' contains an empty string. 
	 * @param usersArr
	 * @return true <=> condition mention is met
	 */
	private static boolean arrayHasEmpty(String[] usersArr) {
		for (String s  : usersArr){
			if (s.compareTo("")==0){
				return true;
			}
		}
		return false;
	}

	//"select from "+ GPSProps.class.getName()+" where mail == ' '(");

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException {
		requestDate = new Date();
		
		//if the user is not logged in it must not use the application.
		//redirect to login page for debbuging purposes
		UserService userService = UserServiceFactory.getUserService();
		User user = userService.getCurrentUser();
		if (user==null){
			resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
		}

		String lat = req.getParameter(GPSLat);
		String lon = req.getParameter(GPSLon);
		String timeStamp = req.getParameter(TIMESTAMP);
		long lTimeStamp = 0;
		double dLat = 0;
		double dLon = 0;
		//if a gps parameter is missing not formmated correctly, lTimeStamp is set to 0 so the database will not be updated
		if (timeStamp!=null && lat !=null && lon != null){
			try {
				lTimeStamp = Math.min(Long.parseLong(timeStamp),requestDate.getTime());
				dLat = Double.parseDouble(lat);
				dLon = Double.parseDouble(lon);
			} catch (NumberFormatException e){
				lTimeStamp = 0;
				dLat = 0;
				dLon = 0;
			}
		}

		//checking that the array of users parameter is not missing and in correct format
		//if not is it set to an empty array.
		String[] usersArr = req.getParameterValues(USERS);
		if (usersArr==null || arrayHasEmpty(usersArr)){
			usersArr = new String[0];
		}
		Arrays.sort(usersArr);

		//initiating PersistenceManager
		PersistenceManager pm = PMF.get().getPersistenceManager();

		//start preapering xml response
		resp.setContentType("text/xml");
		PrintWriter out = resp.getWriter();
		out.println("<?xml version=\"1.0\"?>");
		out.println("<Users>");

		//initiating the memcache
		Cache cache = null;
		Map props = new HashMap();
		props.put(MemcacheService.SetPolicy.ADD_ONLY_IF_NOT_PRESENT, true);
		props.put(GCacheFactory.EXPIRATION_DELTA, 3600);

		try {
			cache = CacheManager.getInstance().getCacheFactory().createCache(Collections.emptyMap());
		} catch (CacheException e) {
			//if CacheException occurs its ok, because 'cache' is null checked upon every use
		}

		//preapering output for each user' mail requested
		for (String friendMail : usersArr){
			GPSProps hisGPS;
			if (cache!=null && cache.containsKey(friendMail)){
				hisGPS = ((GPSProps) cache.get(friendMail));
			}
			else{
				@SuppressWarnings("unchecked")
				List<GPSProps> friendGPS  = (List<GPSProps>) pm.newQuery(buildGetQuery(friendMail)).execute();
				if (friendGPS.size()>0){
					hisGPS  = friendGPS.get(0);
				}
				else{
					hisGPS = GPSProps.getNoPROPSGps();
				}
				if (cache!=null){
					cache.put(friendMail,hisGPS );
				}
			}
			if (GPSProps.isNoProps(hisGPS)){
				out.append(GPSPropXML.mailToFriend(false, friendMail));
			} else {
				out.append(GPSPropXML.gpsPropToFriend(requestDate.getTime(),false,hisGPS));
			}
		}

		String query2 = buildGetQuery(user.getEmail());
		@SuppressWarnings("unchecked")
		List<GPSProps> gpses2  = (List<GPSProps>) pm.newQuery(query2).execute();

		//this loop if for debugging purposes only
		//echos the previous database records of the current user
		for (GPSProps gpsP : gpses2){
			out.append(GPSPropXML.gpsPropToFriend(requestDate.getTime(),true, gpsP));
		}
		out.println("</Users>");

		//if the gps parameters are valid, updates user gps status in both database and cache
		try {
			if (lTimeStamp>0){
				//if there are no previous record it is the first time this user is using Aroundroid with valid gps coordinates.
				if (gpses2.size()==0){
					//sends a welcoming email to first time user :)
					Mailer ml = new Mailer(AroundGPSConstants.mailName, AroundGPSConstants.mailUser);
					try {
						ml.sendOneHtmlMail(user.getEmail(), "Welcome to Aroundroid, People Location Reminders!", AroundroidFTLMails.getWelcome(user.getNickname()));
					} catch (MessagingException e) {
						//on exception no mail is sent
					}
				}
				GPSProps gpsP = new GPSProps(user,user.getEmail().toLowerCase(), dLon, dLat,lTimeStamp);
				pm.deletePersistentAll(gpses2);
				pm.makePersistent(gpsP);
				if (cache!=null){
					cache.put(user.getEmail().toLowerCase(), gpsP);
				}				
			}
		} finally {
			pm.close();
		}

	}

}
