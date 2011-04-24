<%@ page import="com.google.appengine.api.users.User" %>
<%@ page import="com.google.appengine.api.users.UserService" %>
<%@ page import="com.google.appengine.api.users.UserServiceFactory" %>



<%  UserService userService = UserServiceFactory.getUserService();
    User user = userService.getCurrentUser();
    if (user != null)  {%>

	<html>
	<head>
	</head>
	
	<body>
	
	<p>Hello, <b><%= user.getNickname() %></b>!</p>
	
	<p>Click <a href="<%= userService.createLogoutURL(request.getRequestURI()) %>">here</a> to sign out</p>
	
	<p> <u> Exmaple Form: </u> </p>
	<p>
    <form action="/aroundgps" method="post">
      <div><input name="GPSLAT" type="text" value="34.0" /></div>
      <div><input name="GPSLON" type="text" value="32.0" /></div>
      <div><input name="USERS" type="text" value="moti@gmail.comXXXshiran@gmail.comXXXalondener@gmail.com" /></div>
      <div><input type="submit" value="OK" /></div>
    </form>
    </p>
    
    </body>
 
 	</html>
 
 <% } else { 
 		response.sendRedirect("welcome.jsp");
 		} 
 %>
 
 