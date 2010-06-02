/* 
 * Copyright (C) 2007-2009 
 * Thorsten Hampel, Jonas Schulte, Daniel Buese,
 * Andreas Oberhoff, Thomas Bopp, and Robert Hinn
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

package org.wasabibeans.server.core.transfer.model.dto;

import javax.ejb.Stateless;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.naming.InitialContext;

@Stateless
public class TransferManager implements TransferManagerInterface {
	
	private final String suffix = "DTO";

	@SuppressWarnings("unchecked")
	protected <T extends WasabiObjectDTO> T convertNode2DTO(Node wasabiObject) {
		if (wasabiObject == null) {
			return null;
		}
		Class<T> clazz = null;
		try {
			clazz = (Class<T>) Class.forName(WasabiObjectDTO.class.getPackage()
					.getName()
					+ "." + "WasabiDocument" + suffix);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		WasabiObjectDTO object = null;
		try {
			object = clazz.newInstance();
			object.setId(wasabiObject.getIdentifier());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return (T) object;
	}

	protected Node convertDTO2Node(WasabiObjectDTO wasabiObjectDTO) {
		if (wasabiObjectDTO != null) {
			try {
				InitialContext ctx = new InitialContext();
				Repository rep = (Repository) ctx.lookup("java:jcr/local");
				Credentials cred = new SimpleCredentials("user", new char[] { 'p',
						'w', 'd' });
				Session s = rep.login(cred);
				Node node = s.getNodeByIdentifier(wasabiObjectDTO.getId());
				return node;
			} catch (Exception e) {
				return null;
			}
		} else {
			return null;
		}
	}
}