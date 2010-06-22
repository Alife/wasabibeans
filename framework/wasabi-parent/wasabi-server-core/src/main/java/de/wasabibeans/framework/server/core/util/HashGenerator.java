/* 
 * Copyright (C) 2010 
 * Dominik Klaholt, Jannis Sauer
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU LESSER GENERAL PUBLIC LICENSE as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU LESSER GENERAL PUBLIC LICENSE (LGPL) for more details.
 *
 *  You should have received a copy of the GNU LESSER GENERAL PUBLIC LICENSE
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package de.wasabibeans.framework.server.core.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import de.wasabibeans.framework.server.core.common.WasabiConstants.hashAlgorithms;

public class HashGenerator {
	
	public static String generateHash(String value, hashAlgorithms algorithm) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance(algorithm.name());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		md.update(value.getBytes());
		byte[] digest = md.digest();
		String hex = "";
		for (byte b : digest) {
			String bhex = Integer.toHexString(b & 0xff);
			hex += (bhex.length() == 1 ? "0" : "") + bhex;
		}
		return hex;
	}
}