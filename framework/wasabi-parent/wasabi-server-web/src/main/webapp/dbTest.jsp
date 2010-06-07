<%@page contentType="text/html" pageEncoding="UTF-8"
	import="javax.naming.*,de.wasabibeans.framework.server.core.dbTest.*"%>
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
		Context context = new InitialContext();
		ConnectionTestDBLocal bean = (ConnectionTestDBLocal) context
				.lookup("wasabibeans/ConnectionTestDB/local");

		bean.createDatabase();

	} catch (Exception e) {
		s = e.toString();
	}
%>
<%=s%>
</body>
</html>
