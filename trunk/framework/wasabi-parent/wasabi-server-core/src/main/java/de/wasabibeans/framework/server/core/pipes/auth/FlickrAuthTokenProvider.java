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

package de.wasabibeans.framework.server.core.pipes.auth;

import java.io.IOException;
import java.net.URL;

import org.kohsuke.MetaInfServices;
import org.xml.sax.SAXException;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.auth.Auth;
import com.aetrion.flickr.auth.Permission;

@MetaInfServices
public class FlickrAuthTokenProvider extends AuthTokenProvider<FlickrAuthTokenProvider.FlickrParameter> {
	private static final String apiKey = "7267f4c2c8d699aa862fe069d0d691e9";
	private static final String sharedSecret = "7857527bc75f59a2";

	static class FlickrParameter extends AuthTokenProvider.Parameter {
		private final Flickr flickr;
		private final String frob;

		FlickrParameter(Flickr flickr, String frob, URL url) {
			super(url);
			this.flickr = flickr;
			this.frob = frob;
		}
	}

	public FlickrAuthTokenProvider() {
		super("flickr");

	}

	@Override
	public void startAuth(AuthTokenDelegate<FlickrAuthTokenProvider.FlickrParameter> delegate) {

		Flickr flickr = new Flickr(apiKey);
		flickr.setSharedSecret(sharedSecret);
		try {
			String frob = flickr.getAuthInterface().getFrob();
			URL url = flickr.getAuthInterface().buildAuthenticationUrl(Permission.WRITE, frob);

			delegate.parameter = new FlickrParameter(flickr, frob, url);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		} catch (FlickrException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void confirmAuth(AuthTokenDelegate<FlickrAuthTokenProvider.FlickrParameter> delegate) {
		try {
			Auth token = delegate.parameter.flickr.getAuthInterface().getToken(delegate.parameter.frob);
			delegate.setToken(token.getToken());
			delegate.parameter = null;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		} catch (FlickrException e) {
			throw new RuntimeException(e);
		}
	}

}
