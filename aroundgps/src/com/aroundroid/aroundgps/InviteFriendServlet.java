package com.aroundroid.aroundgps;

import java.io.IOException;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

@SuppressWarnings("serial")
public class InviteFriendServlet extends HttpServlet {

	private final String myFriend = "FRIEND";

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws IOException {
		resp.sendRedirect("welcome.jsp");
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException {

		//TODO need to limit the number of times for inviting a friend

		UserService userService = UserServiceFactory.getUserService();
		User user = userService.getCurrentUser();
		if (user==null){
			resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
		}

		boolean mailSent = false;

		//TODO assuming correct mail format for now
		String friendMail = req.getParameter(myFriend);

		if (friendMail!=null && friendMail.compareTo("")!=0 && Mailer.validMail(friendMail)){

			PersistenceManager pm = PMF.get().getPersistenceManager();

			@SuppressWarnings("unchecked")
			List<GPSProps> gpses = (List<GPSProps>) pm.newQuery("select from "+ GPSProps.class.getName()+" where mail=='"+ friendMail.toLowerCase() +"'").execute();
			if (gpses.size()==0){
				//if cannot find friend registered to the service, sends mail
				Mailer ml = new Mailer(user.getEmail(), user.getNickname());
				try {
					mailSent = ml.sendOneHtmlMail(friendMail, "Invitation for Aroundroid, People Location Service",AroundroidFTLMails.getInviteMail(user.getNickname(), friendMail.substring(0, friendMail.indexOf('@'))));
					//ml.sendOneMail(friendMail, "Invitation for Aroundroid, People Location Service", "Hi!\n\n I am inviting you to download astrid and Aroundroid.\n\n It will allow me to set a reminder that will notify me when I am around you area!\n\nNow THAT'S cool!\n\nYours,\n\n"+user.getNickname());					
				} catch (MessagingException e) {
				}
			}

		}

		//TODO set the response to be "MAIL SENT" or "MAIL NO SENT"
		resp.getWriter().write((mailSent?"send":"no send"));


	}

}
