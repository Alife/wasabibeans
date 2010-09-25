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

import java.net.MalformedURLException;
import java.net.URL;

import org.kohsuke.MetaInfServices;

import com.google.gdata.client.authn.oauth.GoogleOAuthHelper;
import com.google.gdata.client.authn.oauth.GoogleOAuthParameters;
import com.google.gdata.client.authn.oauth.OAuthException;
import com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer;
import com.google.gson.Gson;

@MetaInfServices
public class YouTubeAuthTokenProvider extends AuthTokenProvider<YouTubeAuthTokenProvider.YouTubeParameter> {
	private static final String CONSUMER_KEY = "wasabibeans.de";
	private static final String CONSUMER_SECRET = "ZYRpm2Uou2ZkIwBb2KdimV82";

	static class YouTubeParameter extends AuthTokenProvider.Parameter {
		private final GoogleOAuthParameters oauthParameters;
		private final GoogleOAuthHelper oauthHelper;

		YouTubeParameter(GoogleOAuthParameters oauthParameters, GoogleOAuthHelper oauthHelper, URL url) {
			super(url);
			this.oauthParameters = oauthParameters;
			this.oauthHelper = oauthHelper;
		}
	}

	public YouTubeAuthTokenProvider() {
		super("youtube");
	}

	@Override
	public void startAuth(AuthTokenDelegate<YouTubeAuthTokenProvider.YouTubeParameter> delegate) {

		// fetches a request token from the service provider and builds
		// a url based on AUTHORIZE_WEBSITE_URL and CALLBACK_URL to
		// which your app must now send the user
		try {
			// getRequestToken

			GoogleOAuthParameters oauthParameters = new GoogleOAuthParameters();
			oauthParameters.setOAuthConsumerKey(CONSUMER_KEY);
			oauthParameters.setOAuthConsumerSecret(CONSUMER_SECRET);
			oauthParameters.setScope("http://gdata.youtube.com");

			OAuthHmacSha1Signer signer = new OAuthHmacSha1Signer();

			GoogleOAuthHelper oauthHelper = new GoogleOAuthHelper(signer);
			oauthHelper.getUnauthorizedRequestToken(oauthParameters);

			// AutorizeToken
			oauthParameters.setOAuthCallback(null);

			URL url = new URL(oauthHelper.createUserAuthorizationUrl(oauthParameters));

			delegate.parameter = new YouTubeParameter(oauthParameters, oauthHelper, url);
		} catch (OAuthException e) {
			throw new RuntimeException(e);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void confirmAuth(AuthTokenDelegate<YouTubeAuthTokenProvider.YouTubeParameter> delegate) {
		try {
			delegate.parameter.oauthParameters.setOAuthVerifier(delegate.parameter.oauthParameters.getOAuthToken());

			// GetAccessToken
			String accessToken = delegate.parameter.oauthHelper.getAccessToken(delegate.parameter.oauthParameters);

			String accessTokenSecret = delegate.parameter.oauthParameters.getOAuthTokenSecret();

			delegate.setToken(new Gson().toJson(new String[] { accessToken, accessTokenSecret }));

			delegate.parameter = null;
		} catch (OAuthException e) {
			throw new RuntimeException(e);
		}
	}

}
