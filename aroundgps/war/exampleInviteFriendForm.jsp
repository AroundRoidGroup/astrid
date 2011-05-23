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
    <form action="/inviteFriend" method="post">
      <div><input name="FRIEND" type="text" value="tomer.keshet@gmail.com" /></div>
      <div><input type="submit" value="OK" /></div>
    </form>
    </p>
    
    </body>
 
 	</html>
 
 <% } else { 
 		response.sendRedirect("welcome.jsp");
 		} 
 %>
 
 