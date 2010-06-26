package de.wasabibeans.framework.server.init;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;

import de.wasabibeans.framework.server.core.manager.WasabiManagerLocal;
import de.wasabibeans.framework.server.core.util.WasabiLogger;

public class Init extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private InitialContext ctx = null;
	private WasabiLogger logger = WasabiLogger.getLogger(this.getClass());

	public Init() throws NamingException {
		ctx = new InitialContext();

		new Thread() {

			public void run() {
				System.out.println("Wasabi initialization started...");
				int sleepTime = 2000;
				while (true) {
					try {
						WasabiManagerLocal wasabiManager = (WasabiManagerLocal) ctx
								.lookup("wasabibeans/WasabiManager/local");
						Thread.sleep(sleepTime);
						wasabiManager.initDatabase();
						wasabiManager.initRepository();
						wasabiManager.initWorkspace("default");
						logger.info("Wasabi initialization completed.");
						break;
					} catch (Exception e) {
						logger.error("Wasabi initialization failed:", e);
						break;
					}
				}
			}

		}.start();
	}
}
