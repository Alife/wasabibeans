<%@page contentType="text/html" pageEncoding="UTF-8"
	import="java.util.*,javax.naming.*,de.wasabibeans.framework.server.core.auth.*"%>
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
		Hashtable<String, String> environment = new Hashtable<String, String>();
		environment.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.security.jndi.JndiLoginInitialContextFactory");
		environment.put(Context.SECURITY_PRINCIPAL, "root");
		//environment.put(Context.SECURITY_CREDENTIALS, "meerrettich");
		
		Context context = new InitialContext(environment);
		
		//AuthTestRemote beanRemote = (AuthTestRemote) context.lookup("wasabibeans/AuthTest/remote");
		AuthTestLocal beanLocal = (AuthTestLocal) context.lookup("wasabibeans/AuthTest/local");
		s += beanLocal.HelloWorld();
		s += " -- ";
		//s += beanRemote.HelloWorld();
		
	} catch (Exception e) {
		s = e.toString();
	}
%>
<%=s%>
</body>
</html>
