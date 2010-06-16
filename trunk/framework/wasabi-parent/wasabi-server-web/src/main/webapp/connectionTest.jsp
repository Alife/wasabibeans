<%@page contentType="text/html" pageEncoding="UTF-8"
	import="de.wasabibeans.framework.server.core.common.*"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>JSP Page</title>
</head>
<body>
<h1>Hello World!</h1>
<%
	String s = "-- ";
	try {
		WasabiConnection wasabiConnection = new WasabiConnection();
		wasabiConnection.connect("localhost", "1099");
		wasabiConnection.login("root", "meerrettich");

	} catch (Exception e) {
		s = e.toString();
	}
%>
<%=s%>
</body>
</html>
