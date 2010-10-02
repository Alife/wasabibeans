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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.activation.MimetypesFileTypeMap;
import javax.jcr.Session;

import org.kohsuke.MetaInfServices;

import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.DocumentContentException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.pipes.filter.AnnotationBasedFilter;
import de.wasabibeans.framework.server.core.pipes.filter.Filter;
import de.wasabibeans.framework.server.core.pipes.filter.SharedFilterBean;
import de.wasabibeans.framework.server.core.pipes.filter.Sink;
import de.wasabibeans.framework.server.core.pipes.filter.Source;
import de.wasabibeans.framework.server.core.pipes.filter.Wire;
import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterField;
import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterInput;
import de.wasabibeans.framework.server.core.pipes.filter.annotation.FilterOutput;
import de.wasabibeans.framework.server.core.util.JmsConnector;

@MetaInfServices(Filter.class)
public class FfmpegFilter extends AnnotationBasedFilter implements Sink, Source {
	@FilterOutput
	public final Output OUTPUT = new Output(this, "OUTPUT");

	@FilterInput
	public final Input INPUT = new Input(this, "INPUT");

	private String command = "/usr/local/bin/ffmpeg";

	private String format = "flv";

	private String parameter;

	@FilterField(name = "format")
	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	@FilterField(name = "command")
	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	@FilterField(name = "parameter")
	public String getParameter() {
		return parameter;
	}

	public void setParameter(String parameter) {
		this.parameter = parameter;
	}

	@Override
	public boolean isAsynchronous() {
		return true;
	}

	@Override
	public void filter(Wire fromWire, DocumentInfo document, final byte[] buffer, Session s, JmsConnector jms,
			SharedFilterBean sharedFilterBean) throws UnexpectedInternalProblemException, ObjectDoesNotExistException,
			DocumentContentException, ConcurrentModificationException {
		try {
			final Process process = Runtime.getRuntime().exec(
					command + " -i - -f " + format + " " + (parameter == null ? "" : parameter) + " -");
			// BufferedReader reader=new BufferedReader(new InputStreamReader(process.getErrorStream()));
			// String s;
			// while(( s=reader.readLine())!=null){
			// System.out.println(s);
			// }

			new Thread() {
				@Override
				public void run() {
					try {
						process.getOutputStream().write(buffer);
						process.getOutputStream().flush();
						process.getOutputStream().close();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}.start();

			InputStream in = process.getInputStream();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			String str;
			while ((str = reader.readLine()) != null) {
				System.out.println(str);
				if (str.contains("ERROR"))
					throw new RuntimeException(str);
			}

			document.setName(document.getName() + "." + format);
			document.setContentType(new MimeType(new MimetypesFileTypeMap().getContentType(document.getName())));
			forward(OUTPUT, document, out.toByteArray(), s, jms, sharedFilterBean);

		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (MimeTypeParseException e) {
			throw new RuntimeException(e);
		}
	}

}