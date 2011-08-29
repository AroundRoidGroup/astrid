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
public class AroundgpsServlet extends HttpServlet {

	private Date requestDate;



	private final static String GPSLat = "GPSLAT";
	private final static String GPSLon = "GPSLON";
	private final static String USERS = "USERS";
	private final static String DEL = "::";
	private final static String TIMESTAMP = "TIMESTAMP";

	private final static String selectStringStart = "select from "+ GPSProps.class.getName()+" where mail =='";

	private String buildGetQuery(String friend){
		return (selectStringStart + friend.toLowerCase() + "'");
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws IOException {
		resp.sendRedirect("welcome.jsp");
	}

	private boolean arrayHasEmpty(String[] usersArr) {
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
		//TODO deal with error (missing parameter, etc.)

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

		String[] usersArr = req.getParameterValues(USERS);
		if (usersArr==null || arrayHasEmpty(usersArr)){
			usersArr = new String[0];
		}

		Arrays.sort(usersArr);

		PersistenceManager pm = PMF.get().getPersistenceManager();

		resp.setContentType("text/xml");
		PrintWriter out = resp.getWriter();

		out.println("<?xml version=\"1.0\"?>");
		out.println("<Users>");


		Cache cache = null;

		Map props = new HashMap();
		props.put(MemcacheService.SetPolicy.ADD_ONLY_IF_NOT_PRESENT, true);
		props.put(GCacheFactory.EXPIRATION_DELTA, 3600);

		try {
			cache = CacheManager.getInstance().getCacheFactory().createCache(Collections.emptyMap());
		} catch (CacheException e) {
			// ...
		}

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

		/* this loop if for debugging purposes only*/
		for (GPSProps gpsP : gpses2){
			out.append(GPSPropXML.gpsPropToFriend(requestDate.getTime(),true, gpsP));
		}

		out.println("</Users>");



		try {
			if (lTimeStamp>0){
				if (gpses2.size()==0){
					//TODO use a better mailing system, plus sending a html mail for bold and stuff
					Mailer ml = new Mailer(AroundGPSConstants.mailName, AroundGPSConstants.mailUser);
					try {
						ml.sendOneMail(user.getEmail(), "Welcome to Aroundroid, People Location Reminders!", "Hi "+user.getNickname()+"!\n\nWe are happy that you have chosen using Astrid, Aroundroid, and Aroundroid People Location.");
					} catch (MessagingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
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
