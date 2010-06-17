import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.jcrTest.ConnectionTestRemote;
import de.wasabibeans.framework.server.core.remote.DocumentServiceRemote;

public class RemoteClient {
	public static void main(String[] horst) throws NamingException, LoginException {
		// get the jndi context
		Properties properties = new Properties();
		properties.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory"); // "org.jboss.security.jndi.JndiLoginInitialContextFactory");
		properties.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
		properties.put(Context.PROVIDER_URL, "localhost:1099");
		// properties.put(Context.SECURITY_PRINCIPAL, "root");
		// properties.put(Context.SECURITY_CREDENTIALS, "horst");
		Context jndiContext = new InitialContext(properties);

		// set a jaas configuration
		Configuration.setConfiguration(new Configuration() {
			public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
				HashMap<String, String> options = new HashMap<String, String>();

				return new AppConfigurationEntry[] { new AppConfigurationEntry("org.jboss.security.ClientLoginModule",
						LoginModuleControlFlag.REQUIRED, options) };

			}

			public void refresh() {

			}
		});

		// authenticate via jaas (that is, use ClientLoginModule of jboss which does not really authenticate ->
		// authentication must happen on the server)
		// name parameter doesn't matter due to use of anonymous class above
		LoginContext loginContext = new LoginContext("doesntMatter", new ConsoleCallbackHandler());
		loginContext.login();

		// use the ejbs remotely
		ConnectionTestRemote bean = (ConnectionTestRemote) jndiContext.lookup("wasabibeans/ConnectionTest/remote");
		DocumentServiceRemote documentService = (DocumentServiceRemote) jndiContext
				.lookup("wasabibeans/DocumentService/remote");
		try {
			String input = "";
			WasabiRoomDTO root = null;
			WasabiDocumentDTO document = null;

			while (!input.equals("exit")) {
				System.out.println("Warte auf Eingabe...");
				input = readIn();
				if (input.equals("login")) {
					root = bean.login();
				} else if (input.equals("create")) {
					System.out.println("Name eingeben...");
					input = readIn();
					document = documentService.create(input, root);
				} else if (input.equals("content")) {
					System.out.println("Inhalt eingeben...");
					input = readIn();
					documentService.setContent(document, input);
				} else if (input.equals("remove")) {
					System.out.println("Welches...");
					input = readIn();
					document = documentService.getDocumentByName(root, input);
					if (document != null) {
						documentService.remove(document);
					}
				} else if (input.equals("show")) {
					System.out.println("=====Alle Dokumente=====");
					for (WasabiDocumentDTO doc : documentService.getDocuments(root)) {
						System.out.print(documentService.getName(doc) + ": ");
						try {
							System.out.println(documentService.getContent(doc));
						} catch (Exception e) {
							System.out.println();
						}
					}
					System.out.println("========================");
				}
			}
			System.out.println("ENDE");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static String readIn() {
		try {
			byte buffer[] = new byte[80];
			int read;
			read = System.in.read(buffer, 0, 80);
			String input = new String(buffer, 0, read);
			String[] tmp = input.split("\r");
			return tmp[0];
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}

	private static void print(String string) {
		System.out.println(string);
		System.out.println("-----------");
	}
}
