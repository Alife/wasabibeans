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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import javax.jcr.Node;
import javax.jcr.Session;

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

/**
 * This Sink stores the content in a file. The file is named after the document and stored in the given path.
 */
@MetaInfServices(Filter.class)
public class FileSystemSink extends AnnotationBasedFilter implements ContentStore, Sink {

	@FilterInput
	public final Input INPUT = new Input(this, "INPUT");

	private String path;

	public void setPath(String path) {
		this.path = path;
	}

	@FilterField(name = "path", required = true)
	public String getPath() {
		return path;
	}

	@Override
	public void filter(Wire fromWire, DocumentInfo document, byte[] buffer, Session s, JmsConnector jms,
			SharedFilterBean sharedFilterBean) {
		try {
			File file = new File(document.getName());
			for (Node locationNode = ObjectServiceImpl.getEnvironment(ObjectServiceImpl.get(document
					.getDocumentNodeId(), s)); locationNode != null; locationNode = ObjectServiceImpl
					.getEnvironment(locationNode)) {
				file = new File(ObjectServiceImpl.getName(locationNode), file.getPath());
			}
			file = new File(path, file.getPath());

			file.getParentFile().mkdirs();
			// Create a writable file channel
			FileChannel wChannel = new FileOutputStream(file, false).getChannel();

			// Write the ByteBuffer contents; the bytes between the ByteBuffer's
			// position and the limit is written to the file
			wChannel.write(ByteBuffer.wrap(buffer));

			// Close the file
			wChannel.close();
			DocumentServiceImpl.addContentRef(ObjectServiceImpl.get(document.getDocumentNodeId(), s), this, file
					.getPath(), document.getContentType().toString(), (long) buffer.length, true);
			s.save();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public InputStream getContent(Node ref) {
		try {
			File file = new File(DocumentServiceImpl.getRef(ref));
			// Create a readable file channel
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
