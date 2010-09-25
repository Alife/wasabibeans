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

package de.wasabibeans.framework.server.core.pipes.filter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.ejb3.annotation.Service;

//use @Singleton in EJB 3.1
@Service(name = "SharedFilterBean")
public class SharedFilterBeanImpl implements SharedFilterBean {

	private Map<Long, Task> pendingTaskMap = new ConcurrentHashMap<Long, Task>();

	private Set<Task> runningTasks = new HashSet<Task>();

	private AtomicLong nextTaskId = new AtomicLong(0L);

	@Override
	public long createTask(Wire wire, Filter.DocumentInfo info, byte[] data) {
		Task task = new Task(wire, info, data);
		long taskId = nextTaskId.incrementAndGet();
		pendingTaskMap.put(taskId, task);
		return taskId;
	}

	@Override
	public List<Task> getPendingTasks() {
		return new ArrayList<Task>(pendingTaskMap.values());
	}

	@Override
	public List<Task> getRunningTasks() {
		return new ArrayList<Task>(runningTasks);
	}

	@Override
	public Task getTaskForExecution(Long taskId) {
		Task task = pendingTaskMap.remove(taskId);
		runningTasks.add(task);
		return task;
	}

	@Override
	public void finishTask(Task task) {
		runningTasks.remove(task);
	}
}
