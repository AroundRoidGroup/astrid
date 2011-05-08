package com.aroundroid.aroundgps;

import java.util.Date;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
 
@SuppressWarnings("serial")
public class CronUserManagerServlet extends HttpServlet {
	
	private final Date requestDate = new Date();
	private final long deadUser = 1000 * 60 * 60 * 24 * 30;
	private final long almostDeadUser = deadUser + 1000 * 60 * 60 * 24 * 14;
	
	private static String getAllTimedUsers(long minTime,long maxTime){
		return ("select from "+ GPSProps.class.getName()+" where timeStamp > "+minTime+" && timeStamp < "+maxTime);
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp){
		PersistenceManager pm = PMF.get().getPersistenceManager();
		
		String mailQuery = getAllTimedUsers(requestDate.getTime()-almostDeadUser,requestDate.getTime()-deadUser);
		List<GPSProps> gpses  = (List<GPSProps>) pm.newQuery(mailQuery).execute();
		//mail them reminder
		
		String deleteQuery  = getAllTimedUsers(requestDate.getTime()-deadUser,requestDate.getTime());
		List<GPSProps> gpses2  = (List<GPSProps>) pm.newQuery(deleteQuery).execute();
		//mail them death
		//delete them
	}


}
