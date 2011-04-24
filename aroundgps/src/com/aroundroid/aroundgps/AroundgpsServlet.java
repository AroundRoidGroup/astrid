package com.aroundroid.aroundgps;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

@SuppressWarnings("serial")
public class AroundgpsServlet extends HttpServlet {

	private final String GPSLat = "GPSLAT";
	private final String GPSLon = "GPSLON";
	private final String USERS = "USERS";
	private final String DEL = "XXX";

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws IOException {
		resp.sendRedirect("welcome.jsp");
	}

	private String buildGetQuery(String[] usersArr){
		StringBuffer query = new StringBuffer();
		query.append("select from "+ GPSProps.class.getName()+" where");
		for (String user : usersArr){
			query.append(" mail =='" + user + "' ||");
		}
		query.append(" mail =='" + usersArr[0] + "'");
		return query.toString();
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException {


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


		GPSProps gspP = new GPSProps(user,user.getEmail(), dLon, dLat);

		PersistenceManager pm = PMF.get().getPersistenceManager();

	    resp.setContentType("text/xml");
	    PrintWriter out = resp.getWriter();
	    
	    out.println("<?xml version=\"1.0\"?>");
	    out.println("<Users>");
	    
		
		String query = buildGetQuery(usersArr);
		@SuppressWarnings("unchecked")
		List<GPSProps> gpses  = (List<GPSProps>) pm.newQuery(query).execute();

		/*
		resp.setContentType("text/plain");
		resp.getWriter().println("Number Of Users Found = " + gpses.size());
		*/
		
		for(GPSProps gpsP : gpses){
			out.println("<Friend>");
			out.println("<Mail>"+ gpsP.getMail() +"</Mail>");
			out.println("<Latitude>"+ gpsP.getLat() +"</Latitude>");
			out.println("<Longtitude>"+ gpsP.getLong() +"</Longtitude>");
		    out.println("</Friend>");
		}

		String query2 = buildGetQuery(new String[]{user.getEmail()});
		@SuppressWarnings("unchecked")
		List<GPSProps> gpses2  = (List<GPSProps>) pm.newQuery(query2).execute();

		for (GPSProps gpsP : gpses2){
			out.println("<You>");
			out.println("<Mail>"+ gpsP.getMail() +"</Mail>");
			out.println("<Latitude>"+ gpsP.getLat() +"</Latitude>");
			out.println("<Longtitude>"+ gpsP.getLong() +"</Longtitude>");
			out.println("</You>");
		}
		
	    out.println("</Users>");

		try {
			pm.deletePersistentAll(gpses2);
			pm.makePersistent(gspP);
		} finally {
			pm.close();
		}

	}

}
