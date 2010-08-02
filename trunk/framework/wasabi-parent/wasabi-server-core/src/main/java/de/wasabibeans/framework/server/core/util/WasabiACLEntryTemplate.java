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

package de.wasabibeans.framework.server.core.util;

public class WasabiACLEntryTemplate {

	private long id;
	private int view, read, insert, write, comment, execute, grant;
	private long start_time, end_time;
	private String location_id = "";
	private String wasabi_type;

	public WasabiACLEntryTemplate() {
	}

	public long getId() {
		return this.id;
	}

	public String getLocation_ID() {
		return this.location_id;
	}

	public int getView() {
		return this.view;
	}

	public int getRead() {
		return this.read;
	}

	public int getWrite() {
		return this.write;
	}

	public int getInsert() {
		return this.insert;
	}

	public int getComment() {
		return this.comment;
	}

	public int getExecute() {
		return this.execute;
	}

	public int getGrant() {
		return this.grant;
	}

	public long getStart_Time() {
		return this.start_time;
	}

	public long getEnd_Time() {
		return this.end_time;
	}

	public String getWasabi_Type() {
		return this.wasabi_type;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setLocation_ID(String object_id) {
		this.location_id = object_id;
	}

	public void setWasabi_Type(String wasabi_type) {
		this.wasabi_type = wasabi_type;
	}

	public void setView(int view) {
		this.view = view;
	}

	public void setRead(int read) {
		this.read = read;
	}

	public void setInsert(int insert) {
		this.insert = insert;
	}

	public void setWrite(int write) {
		this.write = write;
	}

	public void setComment(int comment) {
		this.comment = comment;
	}

	public void setExecute(int execute) {
		this.execute = execute;
	}

	public void setGrant(int grant) {
		this.grant = grant;
	}

	public void setStart_Time(long start_time) {
		this.start_time = start_time;
	}

	public void setEnd_Time(long end_time) {
		this.end_time = end_time;
	}
}
