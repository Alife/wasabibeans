package de.wasabibeans.framework.server.init;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;

public class Init extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	private InitialContext ctx = null;
	
	public Init() throws NamingException {
		ctx = new InitialContext();
		
		new Thread() {
			
			public void run() {
				System.out.println("Wasabi initialization started...");
				int sleepTime = 2000;
				while (true) {
					try {
						Thread.sleep(sleepTime);
						initialize();
						System.out.println("Wasabi initialization completed.");
						break;
					} catch (Exception e) {
						System.out.println("Wasabi initialization failed:");
						e.printStackTrace();
						break;
					}
				}
			}
			
		}.start();
	}
	
	public void initialize() throws NamingException, LoginException, RepositoryException {
		Repository rep = (Repository) ctx.lookup("java:jcr/local");
		Credentials cred = new SimpleCredentials("user", new char[] { 'p',
				'w', 'd' });
		Session s = rep.login(cred);

		s.getRootNode();
	}
}
