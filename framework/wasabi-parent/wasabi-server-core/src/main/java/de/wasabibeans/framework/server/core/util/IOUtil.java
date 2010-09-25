package de.wasabibeans.framework.server.core.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class IOUtil {

	public static byte[] convert2Byte(Serializable data) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(data);
		oos.flush();
		oos.close();
		bos.close();

		return bos.toByteArray();
	}

	public static Serializable convert2Serializable(byte[] data) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInputStream ois = new ObjectInputStream(bis);
		Serializable result = (Serializable) ois.readObject();
		ois.close();
		bis.close();

		return result;
	}
}
