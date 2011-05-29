<%@ page import="com.google.appengine.api.users.User" %>
<%@ page import="com.google.appengine.api.users.UserService" %>
<%@ page import="com.google.appengine.api.users.UserServiceFactory" %>

<html>
  <head>
   
  </head>
  <body>
 <%
    UserService userService = UserServiceFactory.getUserService();
    User user = userService.getCurrentUser();
    if (user != null) {
%>
<p>Hello, <b><%= user.getNickname() %></b>! . Welcome To AroundRoid People Based Location Service.</p>

<p> Click <a href = "examplePostForm.jsp">here</a> to go to an example gps post form.</p>
<p> Click <a href = "exampleInviteFriendForm.jsp">here</a> to go to an example invite friend form.</p>

<p>Click <a href="<%= userService.createLogoutURL(request.getRequestURI()) %>">here</a> to sign out</p> 
<%
    } else {
%>
<p>Hello, Welcome To AroundRoid People Based Location Service.</p>
<p> You are not logged in to the service. 
Please <a href="<%= userService.createLoginURL(request.getRequestURI()) %>">Sign in</a>.
</p>
<%
    }
%>
  
  </body>
  
</html>