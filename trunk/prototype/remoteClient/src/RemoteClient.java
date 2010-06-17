import java.io.IOException;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import de.wasabibeans.framework.server.core.dto.WasabiDocumentDTO;
import de.wasabibeans.framework.server.core.dto.WasabiRoomDTO;
import de.wasabibeans.framework.server.core.jcrTest.ConnectionTestRemote;
import de.wasabibeans.framework.server.core.remote.DocumentServiceRemote;

public class RemoteClient {
	public static void main(String[] horst) throws NamingException {
		Properties properties = new Properties();
		properties.put(Context.INITIAL_CONTEXT_FACTORY,
				"org.jnp.interfaces.NamingContextFactory");
		properties.put(Context.URL_PKG_PREFIXES,
				"org.jboss.naming:org.jnp.interfaces");
		properties.put(Context.PROVIDER_URL, "localhost:1099");
		Context context = new InitialContext(properties);
		
		/*Test1Remote test1 = (Test1Remote) context.lookup("Test1/remote");
		Test2Remote test2 = (Test2Remote) context.lookup("Test2/remote");
		
		print(test1.get());
		test1.set("ufftata");
		print(test1.get());
		
		
		print(test2.get());*/

		ConnectionTestRemote bean = (ConnectionTestRemote) context
				.lookup("wasabibeans/ConnectionTest/remote");
		DocumentServiceRemote documentService = (DocumentServiceRemote) context
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
					for (WasabiDocumentDTO doc : documentService
							.getDocuments(root)) {
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
