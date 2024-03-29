<%@page contentType="text/html" pageEncoding="UTF-8"
	import="javax.naming.*,de.wasabibeans.framework.server.core.dto.*,de.wasabibeans.framework.server.core.jcrTest.*,de.wasabibeans.framework.server.core.local.*,de.wasabibeans.framework.server.core.common.*"%>
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
		WasabiConnection connection = new WasabiConnection();
		connection.connect("localhost", "1099");
		connection.login("root", "horst");
		
		ConnectionTestLocal bean = (ConnectionTestLocal) context.lookup("wasabibeans/ConnectionTest/local");
		WasabiRoomDTO root = bean.login();

		DocumentServiceLocal documentService = (DocumentServiceLocal) context
				.lookup("wasabibeans/DocumentService/local");
		documentService.create("hallo", root);
		for (WasabiDocumentDTO doc : documentService.getDocuments(root)) {
			s += documentService.getName(doc) + " -- ";
		}

		s += "<br/>";
		s += bean.printNodeTypes();

	} catch (Exception e) {
		s = e.toString();
	}
%>

<%=s%>
</body>
</html>
