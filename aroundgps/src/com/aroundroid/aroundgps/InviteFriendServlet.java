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


/***
 * A servlet for inviting friend to use aroundroid if they are not using it.
 */
@SuppressWarnings("serial")
public class InviteFriendServlet extends HttpServlet {

	private final String myFriend = "FRIEND";

	/***
	 * does not need to support Get so redirects to welcome page
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws IOException {
		resp.sendRedirect("welcome.jsp");
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException {

		//if user is not logged in redirect to login page
		UserService userService = UserServiceFactory.getUserService();
		User user = userService.getCurrentUser();
		if (user==null){
			resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
		}

		boolean mailSent = false;
		
		String friendMail = req.getParameter(myFriend);

		//if mail parmeter is valid tries to send mail
		if (friendMail!=null && friendMail.compareTo("")!=0 && Mailer.validMail(friendMail)){

			PersistenceManager pm = PMF.get().getPersistenceManager();

			@SuppressWarnings("unchecked")
			List<GPSProps> gpses = (List<GPSProps>) pm.newQuery("select from "+ GPSProps.class.getName()+" where mail=='"+ friendMail.toLowerCase() +"'").execute();
			if (gpses.size()==0){
				//if cannot find friend registered to the service, sends mail
				Mailer ml = new Mailer(user.getEmail(), user.getNickname());
				try {
					mailSent = ml.sendOneHtmlMail(friendMail, "Invitation for Aroundroid, People Location Service",AroundroidFTLMails.getInviteMail(user.getNickname(), friendMail.substring(0, friendMail.indexOf('@'))));					
				} catch (MessagingException e) {
					//if error occurd mailSent will stay false
				}
			}

		}

		//send for mail sent, no send for error or user registered
		resp.getWriter().write((mailSent?"send":"no send"));


	}

}
