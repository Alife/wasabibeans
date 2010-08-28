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

public class WasabiACLEntry {

	private String group_id = "";
	private long id;
	private String inheritance_id = "";
	private String object_id = "";
	private String parent_id = "";
	private long start_time, end_time;
	private String user_id = "";
	private int view, read, insert, write, comment, execute, grant, priority;

	public WasabiACLEntry() {
	}

	public int getComment() {
		return this.comment;
	}

	public long getEnd_Time() {
		return this.end_time;
	}

	public int getExecute() {
		return this.execute;
	}

	public int getGrant() {
		return this.grant;
	}

	public String getGroup_Id() {
		return this.group_id;
	}

	public long getId() {
		return this.id;
	}

	public String getInheritance_Id() {
		return this.inheritance_id;
	}

	public int getInsert() {
		return this.insert;
	}

	public String getObject_Id() {
		return this.object_id;
	}

	public String getParent_Id() {
		return this.parent_id;
	}

	public int getPriority() {
		return this.priority;
	}

	public int getRead() {
		return this.read;
	}

	public long getStart_Time() {
		return this.start_time;
	}

	public String getUser_Id() {
		return this.user_id;
	}

	public int getView() {
		return this.view;
	}

	public int getWrite() {
		return this.write;
	}

	public void setComment(int comment) {
		this.comment = comment;
	}

	public void setEnd_Time(long end_time) {
		this.end_time = end_time;
	}

	public void setExecute(int execute) {
		this.execute = execute;
	}

	public void setGrant(int grant) {
		this.grant = grant;
	}

	public void setGroup_Id(String group_id) {
		this.group_id = group_id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setInheritance_Id(String inheritance_id) {
		this.inheritance_id = inheritance_id;
	}

	public void setInsert(int insert) {
		this.insert = insert;
	}

	public void setObject_Id(String object_id) {
		this.object_id = object_id;
	}

	public void setParent_Id(String parent_id) {
		this.parent_id = parent_id;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public void setRead(int read) {
		this.read = read;
	}

	public void setStart_Time(long start_time) {
		this.start_time = start_time;
	}

	public void setUser_Id(String user_id) {
		this.user_id = user_id;
	}

	public void setView(int view) {
		this.view = view;
	}

	public void setWrite(int write) {
		this.write = write;
	}
}
