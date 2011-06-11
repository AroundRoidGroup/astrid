package com.aroundroid.aroundgps;

import java.io.IOException;
import java.io.PrintWriter;
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

import java.util.Date;

@SuppressWarnings("serial")
public class AroundgpsServlet extends HttpServlet {

	private Date requestDate;

	private final long gpsValidTime = 1000 * 60 * 60 * 24;

	private final String GPSLat = "GPSLAT";
	private final String GPSLon = "GPSLON";
	private final String USERS = "USERS";
	private final String DEL = "::";
	private final String TIMESTAMP = "TIMESTAMP";

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws IOException {
		resp.sendRedirect("welcome.jsp");
	}

	private String buildGetQuery(String[] usersArr){
		StringBuffer query = new StringBuffer();
		query.append("select from "+ GPSProps.class.getName()+" where (");
		for (String user : usersArr){
			query.append(" mail =='" + user.toLowerCase() + "' ||");
		}
		query.append(" mail =='" + usersArr[0].toLowerCase() + "')");
		//query.append(" && timeStamp > "+(requestDate.getTime() - gpsValidTime));
		return query.toString();
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException {
		requestDate = new Date();
		//TODO deal with error, make timestamp optional


		UserService userService = UserServiceFactory.getUserService();
		User user = userService.getCurrentUser();
		if (user==null){
			resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
		}

		String lat = req.getParameter(GPSLat);
		String lon = req.getParameter(GPSLon);

		Double dLat = Double.parseDouble(lat),dLon = Double.parseDouble(lon);

		String users = req.getParameter(USERS);
		String[] usersArr = users.split(DEL);
		for(int i =0; i < usersArr.length ; i++){
			usersArr[i] = usersArr[i].toLowerCase();
		}

		String timeStamp = req.getParameter(TIMESTAMP);
		Long lTimeStamp = Math.min(Long.parseLong(timeStamp),requestDate.getTime());

		GPSProps gspP = new GPSProps(user,user.getEmail().toLowerCase(), dLon, dLat,lTimeStamp);

		PersistenceManager pm = PMF.get().getPersistenceManager();

		resp.setContentType("text/xml");
		PrintWriter out = resp.getWriter();

		out.println("<?xml version=\"1.0\"?>");
		out.println("<Users>");


		String query = buildGetQuery(usersArr);
		@SuppressWarnings("unchecked")
		List<GPSProps> gpses  = (List<GPSProps>) pm.newQuery(query).execute();


		for(GPSProps gpsP : gpses){
			if (gpsP.getTimeStamp() > (requestDate.getTime() - gpsValidTime))
				out.append(GPSPropXML.gpsPropToFriend(false,gpsP));
		}

		String query2 = buildGetQuery(new String[]{user.getEmail()});
		@SuppressWarnings("unchecked")
		List<GPSProps> gpses2  = (List<GPSProps>) pm.newQuery(query2).execute();

		for (GPSProps gpsP : gpses2){
			if (gpsP.getTimeStamp() > (requestDate.getTime() - gpsValidTime))
				out.append(GPSPropXML.gpsPropToFriend(true, gpsP));
		}

		out.println("</Users>");

		try {
			pm.deletePersistentAll(gpses2);
			pm.makePersistent(gspP);
		} finally {
			pm.close();
		}

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

	}

}
