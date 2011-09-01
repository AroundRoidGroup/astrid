package com.aroundroid.aroundgps;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/***
 * Cron servlet used for deleting users that have used the app for a long time,
 * or to remind them to use the application
 * @author Tomer
 *
 */
@SuppressWarnings("serial")
public class CronUserManagerServlet extends HttpServlet {

	private Date requestDate;

	private final long almostDeadUser = 1000 * 60 * 60 * 24 * 15;
	private final long deadUser = almostDeadUser + 1000 * 60 * 60 * 24 * 15;

	//COMMENTS FOR DEBUG NO ATTENTION REQUIRED
	//private final long almostDeadUser = 1000 * 60;
	//private final long deadUser = almostDeadUser + 1000 * 181;
	//for debug with these, set cron timer to 1 minute!

	/***
	 * returns a select query for all the users between maxTime and minTime
	 * @param minTime
	 * @param maxTime
	 * @return a string sql select query
	 */
	private static String getAllTimedUsers(long minTime,long maxTime){
		return ("select from "+ GPSProps.class.getName()+" where ((timeStamp > "+maxTime+") && (timeStamp < "+minTime+"))");
	}
	
	/***
	 * returns a select query for all the users between maxTime and minTime that were not reminded
	 * @param minTime
	 * @param maxTime
	 * @return a string sql select query
	 */
	private static String getAllTimedUsersUnnotified(long minTime,long maxTime){
		return ("select from "+ GPSProps.class.getName()+" where ((timeStamp > "+maxTime+") && (timeStamp < "+minTime+") && (reminded==false))");
	}

	/***
	 * 
	 */
	@SuppressWarnings("unchecked")
	public void doGet(HttpServletRequest req, HttpServletResponse resp){
		requestDate = new Date();
		PersistenceManager pm = PMF.get().getPersistenceManager();

		String deleteQuery  = getAllTimedUsers(requestDate.getTime()-deadUser,0);
		List<GPSProps> gpses2  = (List<GPSProps>) pm.newQuery(deleteQuery).execute();

		try{
			Mailer ml = new Mailer(AroundGPSConstants.mailName, AroundGPSConstants.mailUser);
			String mailQuery = getAllTimedUsersUnnotified(requestDate.getTime()-almostDeadUser,requestDate.getTime()-deadUser);

			List<GPSProps> gpses  = (List<GPSProps>) pm.newQuery(mailQuery).execute();

			//mail them reminder
			for (GPSProps gpsP : gpses){
				try {;
					ml.sendOneHtmlMail(gpsP.getMail(), "AroundRoid People Location Service - Long time, no see!", AroundroidFTLMails.getReminderMail(gpsP.getUser().getNickname()));
					gpsP.setReminded(true);
				} catch (MessagingException e) {
					//nothing needs to be done
				}
			}
			//update 'reminded'

			pm.makePersistentAll(gpses);

			//mail them deleted mail
			for (GPSProps gpsP : gpses2){
				try {
					ml.sendOneHtmlMail(gpsP.getMail(),"AroundRoid People Location Service - Is it the end?" , AroundroidFTLMails.getDeletedMail(gpsP.getUser().getNickname()));
				} catch (MessagingException e) {
				}
			}

		} catch (UnsupportedEncodingException e) {
			//nothing needs to be done
		}
		
		//delete them
		pm.deletePersistentAll(gpses2);
		
		pm.close();


	}


}
