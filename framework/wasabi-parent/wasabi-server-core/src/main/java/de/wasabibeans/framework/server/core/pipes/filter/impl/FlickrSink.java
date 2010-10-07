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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.Session;

import org.kohsuke.MetaInfServices;
import org.xml.sax.SAXException;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.RequestContext;
import com.aetrion.flickr.photos.PhotosInterface;
import com.aetrion.flickr.photos.Size;
import com.aetrion.flickr.uploader.UploadMetaData;
import com.aetrion.flickr.uploader.Uploader;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.DocumentServiceImpl;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.pipes.auth.FlickrAuthTokenProvider;
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
public class FlickrSink extends AnnotationBasedFilter implements ContentStore, Sink {

	@FilterInput
	public final Input INPUT = new Input(this, "INPUT");

	private final static String apiKey = "7267f4c2c8d699aa862fe069d0d691e9";

	private final static String sharedSecret = "7857527bc75f59a2";

	public enum User {
		ROOM_CREATOR, DOCUMENT_UPLOADER
	}

	private User user;

	private String token;

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
			Flickr flickr = new Flickr(apiKey);
			flickr.setSharedSecret(sharedSecret);

			Uploader uploader = new Uploader(apiKey, sharedSecret);

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

			if (userNode == null)
				throw new IllegalArgumentException("Could not get WasabiUser for " + user);

			token = new FilterAuthTokenDelegate<FlickrAuthTokenProvider.Parameter>(new FlickrAuthTokenProvider(),
					userNode).getToken();

			RequestContext.getRequestContext().setAuth(flickr.getAuthInterface().checkToken(token));

			UploadMetaData metaData = new UploadMetaData();

			metaData.setPublicFlag(true);
			metaData.setTitle(document.getName());

			String ref = uploader.upload(buffer, metaData);

			PhotosInterface photos = flickr.getPhotosInterface();

			Collection<Size> sizes = photos.getSizes(ref);

			boolean doJcrSave = isAsynchronous() ? true : WasabiConstants.JCR_SAVE_PER_METHOD;
			for (Size size : sizes) {
				DocumentServiceImpl.addContentRef(ObjectServiceImpl.get(document.getDocumentNodeId(), s), this, size
						.getSource(), document.getContentType().toString(), (long) new URL(size.getSource())
						.openConnection().getContentLength(), true, s, doJcrSave, document.getCallerPrincipal());
			}
		} catch (FlickrException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public InputStream getContent(Node ref) {
		try {
			return new URL(DocumentServiceImpl.getRef(ref)).openStream();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}