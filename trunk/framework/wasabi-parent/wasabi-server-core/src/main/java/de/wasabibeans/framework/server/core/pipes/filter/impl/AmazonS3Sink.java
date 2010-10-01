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
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Session;

import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;
import org.kohsuke.MetaInfServices;

import de.wasabibeans.framework.server.core.internal.DocumentServiceImpl;
import de.wasabibeans.framework.server.core.internal.ObjectServiceImpl;
import de.wasabibeans.framework.server.core.pipes.filter.AnnotationBasedFilter;
import de.wasabibeans.framework.server.core.pipes.filter.ContentStore;
import de.wasabibeans.framework.server.core.pipes.filter.Filter;
import de.wasabibeans.framework.server.core.pipes.filter.SharedFilterBean;
import de.wasabibeans.framework.server.core.pipes.filter.Sink;
import de.wasabibeans.framework.server.core.pipes.filter.Wire;
import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterField;
import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterInput;
import de.wasabibeans.framework.server.core.util.JmsConnector;

@MetaInfServices(Filter.class)
public class AmazonS3Sink extends AnnotationBasedFilter implements Sink, ContentStore {

	@FilterInput
	public final Input INPUT = new Input(this, "INPUT");

	private String awsAccessKey;

	private String awsSecretKey;

	private String bucket;

	@FilterField(name = "awsAccessKey", required = true)
	public String getAwsAccessKey() {
		return awsAccessKey;
	}

	public void setAwsAccessKey(String awsAccessKey) {
		this.awsAccessKey = awsAccessKey;
	}

	@FilterField(name = "awsSecretKey", required = true)
	public String getAwsSecretKey() {
		return awsSecretKey;
	}

	public void setAwsSecretKey(String awsSecretKey) {
		this.awsSecretKey = awsSecretKey;
	}

	@FilterField(name = "bucket", required = true)
	public String getBucket() {
		return bucket;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	@Override
	public void filter(Wire fromWire, DocumentInfo document, byte[] buffer, Session s, JmsConnector jms,
			SharedFilterBean sharedFilterBean) {

		AWSCredentials awsCredentials = new AWSCredentials(awsAccessKey, awsSecretKey);

		try {
			S3Service s3Service = new RestS3Service(awsCredentials);
			S3Bucket s3Bucket = s3Service.getOrCreateBucket(bucket);

			S3Object s3Object = new S3Object(document.getName());

			s3Object.setContentType(document.getContentType().toString());
			s3Object.setContentLength(buffer.length);
			s3Object.setDataInputStream(new ByteArrayInputStream(buffer));

			s3Service.putObject(s3Bucket, s3Object);

			DocumentServiceImpl.addContentRef(ObjectServiceImpl.get(document.getDocumentNodeId(), s), this, document
					.getName(), document.getContentType().toString(), (long) buffer.length, true, document
					.getCallerPrincipal());
			s.save();
		} catch (S3ServiceException e) {
			throw new RuntimeException(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public InputStream getContent(Node ref) {
		AWSCredentials awsCredentials = new AWSCredentials(awsAccessKey, awsSecretKey);

		try {
			S3Service s3Service = new RestS3Service(awsCredentials);
			S3Bucket s3Bucket = s3Service.getOrCreateBucket(bucket);

			S3Object s3Object = s3Service.getObject(s3Bucket, ObjectServiceImpl.getName(DocumentServiceImpl
					.getDocument(ref)));

			return s3Object.getDataInputStream();

		} catch (S3ServiceException e) {
			throw new RuntimeException(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
