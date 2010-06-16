package de.wasabibeans.framework.server.init;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;

import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.commons.cnd.ParseException;

import de.wasabibeans.framework.server.core.manager.WasabiManager;

public class Init extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final String WASABI_NODETYPES_RESOURCE_PATH = "wasabi_nodetypes.cnd";

	private InitialContext ctx = null;

	public Init() throws NamingException {
		ctx = new InitialContext();

		new Thread() {

			public void run() {
				System.out.println("Wasabi initialization started...");
				int sleepTime = 2000;
				while (true) {
					try {
						WasabiManager wasabiManager = (WasabiManager) ctx.lookup("wasabibeans/WasabiManager/local");
						
						Thread.sleep(sleepTime);
						initialize();
						wasabiManager.init();
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

	public void initialize() throws NamingException, LoginException,
			RepositoryException, ParseException, IOException {
		Repository rep = (Repository) ctx.lookup("java:jcr/local");
		Credentials cred = new SimpleCredentials("user", new char[] { 'p', 'w',
				'd' });
		Session s = rep.login(cred);

		// register wasabi nodetypes (also registers the wasabi jcr namespace)
		InputStream in = getClass().getClassLoader().getResourceAsStream(
				WASABI_NODETYPES_RESOURCE_PATH);
		Reader r = new InputStreamReader(in, "utf-8");
		CndImporter.registerNodeTypes(r, s);
	}
}
