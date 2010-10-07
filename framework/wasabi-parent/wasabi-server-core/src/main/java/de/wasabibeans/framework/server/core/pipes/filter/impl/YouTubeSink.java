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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.jcr.Node;
import javax.jcr.Session;

import org.kohsuke.MetaInfServices;

import com.google.gdata.client.authn.oauth.GoogleOAuthParameters;
import com.google.gdata.client.authn.oauth.OAuthException;
import com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer;
import com.google.gdata.client.authn.oauth.OAuthParameters;
import com.google.gdata.client.youtube.YouTubeService;
import com.google.gdata.data.media.MediaStreamSource;
import com.google.gdata.data.media.mediarss.MediaCategory;
import com.google.gdata.data.media.mediarss.MediaDescription;
import com.google.gdata.data.media.mediarss.MediaKeywords;
import com.google.gdata.data.media.mediarss.MediaTitle;
import com.google.gdata.data.youtube.VideoEntry;
import com.google.gdata.data.youtube.YouTubeMediaGroup;
import com.google.gdata.util.ServiceException;
import com.google.gson.Gson;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.DocumentServiceImpl;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.pipes.auth.YouTubeAuthTokenProvider;
import de.wasabibeans.framework.server.core.pipes.filter.AnnotationBasedFilter;
import de.wasabibeans.framework.server.core.pipes.filter.ContentStore;
import de.wasabibeans.framework.server.core.pipes.filter.Filter;
import de.wasabibeans.framework.server.core.pipes.filter.SharedFilterBean;
import de.wasabibeans.framework.server.core.pipes.filter.Sink;
import de.wasabibeans.framework.server.core.pipes.filter.Wire;
import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterField;
import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterInput;
import de.wasabibeans.framework.server.core.util.JmsConnector;

/**
 * This sink stores the content in the org.wasabibeans.server.core.transfer.model.seam.legacy.Document itself.
 */
@MetaInfServices(Filter.class)
public class YouTubeSink extends AnnotationBasedFilter implements ContentStore, Sink {

	@FilterInput
	public final Input INPUT = new Input(this, "INPUT");

	static String CONSUMER_KEY = "wasabibeans.de";
	static String CONSUMER_SECRET = "ZYRpm2Uou2ZkIwBb2KdimV82";

	static String developerKey = "AI39si6eruNGcGa6NmYfFZTWYWU6O-N0heMkjlk4KPPCaLJPGYc7v8JPObiFboQTVB0xg3bAh_6Hk-LeoU4-gifFhUCmWnj4xg";

	public enum User {
		ROOM_CREATOR, DOCUMENT_UPLOADER
	}

	private User user;

	private String accessToken;
	private String accessTokenSecret;

	@FilterField(name = "user", required = true)
	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	@Override
	public void filter(Wire fromWire, DocumentInfo document, byte[] buffer, Session s, JmsConnector jms,
			SharedFilterBean sharedFilterBean) throws ConcurrentModificationException,
			UnexpectedInternalProblemException, ObjectDoesNotExistException {

		try {

			Node userNode = null;
			switch (user) {
			case ROOM_CREATOR:
				userNode = ObjectServiceImpl.getCreatedBy(DocumentServiceImpl.getNearestRoom(ObjectServiceImpl.get(
						document.getDocumentNodeId(), s)));
				break;
			case DOCUMENT_UPLOADER:
				userNode = ObjectServiceImpl.getCreatedBy(ObjectServiceImpl.get(document.getDocumentNodeId(), s));
				break;
			}

			String[] token = new Gson().fromJson(new FilterAuthTokenDelegate<YouTubeAuthTokenProvider.Parameter>(
					new YouTubeAuthTokenProvider(), userNode).getToken(), String[].class);

			accessToken = token[0];
			accessTokenSecret = token[1];

			YouTubeService myService = new YouTubeService("WasabiBeans", developerKey);
			OAuthParameters oauthParameters = new GoogleOAuthParameters();
			oauthParameters.setOAuthConsumerKey(CONSUMER_KEY);
			oauthParameters.setOAuthConsumerSecret(CONSUMER_SECRET);
			oauthParameters.setOAuthToken(accessToken);
			oauthParameters.setOAuthTokenSecret(accessTokenSecret);

			OAuthHmacSha1Signer signer = new OAuthHmacSha1Signer();
			myService.setOAuthCredentials(oauthParameters, signer);

			VideoEntry newEntry = new VideoEntry();

			YouTubeMediaGroup mg = newEntry.getOrCreateMediaGroup();
			mg.setTitle(new MediaTitle());
			mg.getTitle().setPlainTextContent(document.getName());

			mg.setKeywords(new MediaKeywords());
			mg.getKeywords().addKeyword("WasabiBeans");
			mg.setDescription(new MediaDescription());
			mg.getDescription().setPlainTextContent("Uploaded by WasabiBeans");

			mg.addCategory(new MediaCategory("http://gdata.youtube.com/schemas/2007/categories.cat", "Entertainment"));

			MediaStreamSource ms = new MediaStreamSource(new ByteArrayInputStream(buffer), document.getContentType()
					.toString());

			newEntry.setMediaSource(ms);

			String uploadUrl = "http://uploads.gdata.youtube.com/feeds/api/users/default/uploads";

			myService.getRequestFactory().setHeader("Slug", document.getName());

			VideoEntry createdEntry = myService.insert(new URL(uploadUrl), newEntry);

			boolean doJcrSave = isAsynchronous() ? true : WasabiConstants.JCR_SAVE_PER_METHOD;
			DocumentServiceImpl.addContentRef(ObjectServiceImpl.get(document.getDocumentNodeId(), s), this,
					createdEntry.getHtmlLink().getHref(), null, null, false, s, doJcrSave, document
							.getCallerPrincipal());
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ServiceException e) {
			throw new RuntimeException(e);
		} catch (OAuthException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public InputStream getContent(Node ref) {
		throw new UnsupportedOperationException();
	}

}