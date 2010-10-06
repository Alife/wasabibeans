/* 
 * Copyright (C) 2010 
 * Jonas Schulte, Dominik Klaholt, Jannis Sauer
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU GENERAL PUBLIC LICENSE as published by
 *  the Free Software Foundation; either version 3 of the license, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU GENERAL PUBLIC LICENSE (GPL) for more details.
 *
 *  You should have received a copy of the GNU GENERAL PUBLIC LICENSE version 3
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 *  Further information are online available at: http://www.wasabibeans.de
 */

package de.wasabibeans.framework.server.core.pipes.filter.impl;

import javax.jcr.Node;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.internal.AttributeServiceImpl;
import de.wasabibeans.framework.server.core.pipes.auth.AuthTokenDelegate;
import de.wasabibeans.framework.server.core.pipes.auth.AuthTokenProvider;

class FilterAuthTokenDelegate<T extends AuthTokenProvider.Parameter> extends AuthTokenDelegate<T> {

	private Node userNode;

	public FilterAuthTokenDelegate(AuthTokenProvider provider, Node userNode) {
		super(provider);
		this.userNode = userNode;
	}

	@Override
	public String getToken() {
		try {
			Node attributeNode = AttributeServiceImpl.getAttributeByName(userNode, getAttributeName());
			return attributeNode == null ? null : AttributeServiceImpl.getValue(String.class, attributeNode);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void deleteToken() {
		try {
			AttributeServiceImpl.remove(AttributeServiceImpl.getAttributeByName(userNode, getAttributeName()), userNode
					.getSession(), true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void setToken(String token) {
		try {
			AttributeServiceImpl.create(getAttributeName(), token, userNode, userNode.getSession(), true,
					WasabiConstants.ROOT_USER_NAME);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
