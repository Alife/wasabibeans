package wasabibeans.client.remoteClient;
import java.io.IOException;


public class Test {
	public static void main(String[] horst) {
		try {
			byte buffer[] = new byte[80];
			int read;
			read = System.in.read(buffer, 0, 80);
			String input = new String(buffer, 0, read);
			String[] tmp = input.split("\r");
			System.out.println(tmp[0]);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
