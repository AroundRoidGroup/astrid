package com.aroundroid.aroundgps;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class CronUserManagerServlet extends HttpServlet {

	private Date requestDate;

	private final long almostDeadUser = 1000 * 60 * 60 * 24 * 15;
	private final long deadUser = almostDeadUser + 1000 * 60 * 60 * 24 * 15;

	//private final long almostDeadUser = 1000 * 60;
	//private final long deadUser = almostDeadUser + 1000 * 181;
	//for debug with these, set cron timer to 1 minute!


	private static String getAllTimedUsers(long minTime,long maxTime){
		return ("select from "+ GPSProps.class.getName()+" where ((timeStamp > "+maxTime+") && (timeStamp < "+minTime+"))");
	}
	
	private static String getAllTimedUsersUnnotified(long minTime,long maxTime){
		return ("select from "+ GPSProps.class.getName()+" where ((timeStamp > "+maxTime+") && (timeStamp < "+minTime+") && (reminded==false))");
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp){
		requestDate = new Date();
		//consider scheduale task for later, instade of sending mails immidietly
		PersistenceManager pm = PMF.get().getPersistenceManager();

		String deleteQuery  = getAllTimedUsers(requestDate.getTime()-deadUser,0);
		List<GPSProps> gpses2  = (List<GPSProps>) pm.newQuery(deleteQuery).execute();

		try{
			//TODO use a better mailing system, plus sending a html mail for bold and stuff
			Mailer ml = new Mailer(AroundGPSConstants.mailName, AroundGPSConstants.mailUser);
			String mailQuery = getAllTimedUsersUnnotified(requestDate.getTime()-almostDeadUser,requestDate.getTime()-deadUser);

			List<GPSProps> gpses  = (List<GPSProps>) pm.newQuery(mailQuery).execute();

			//mail them reminder

			for (GPSProps gpsP : gpses){
				try {
					//ml.sendOneMail(gpsP.getMail(), "AroundRoid People Location Service - Long time, no see!", "Hi "+gpsP.getUser().getNickname()+"!\n\nYou haven't used Aroundroid People location Service for a long time.\n\nA week from now, you will no longer be available for other users.\n\nTo start using Aroundroid again, open Astrid from you Android and choose People Location Service.");
					ml.sendOneHtmlMail(gpsP.getMail(), "AroundRoid People Location Service - Long time, no see!", AroundroidFTLMails.getReminderMail(gpsP.getUser().getNickname()));
					gpsP.setReminded(true);
				} catch (MessagingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			//update 'reminded'

			pm.makePersistentAll(gpses);

			//mail them death
			for (GPSProps gpsP : gpses2){
				try {
					//ml.sendOneMail(gpsP.getMail(), "AroundRoid People Location Service - Is it the end?", "Hi "+gpsP.getUser().getNickname()+"!\n\nYou haven't used Aroundroid People location Service for a long time.\n\nAs a result, you will no longer be available for other users.\n\nTo start using Aroundroid again, open Astrid from you Android and choose People Location Service.");
					ml.sendOneHtmlMail(gpsP.getMail(),"AroundRoid People Location Service - Is it the end?" , AroundroidFTLMails.getDeletedMail(gpsP.getUser().getNickname()));
				} catch (MessagingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//delete them
		pm.deletePersistentAll(gpses2);
		
		pm.close();


	}


}
