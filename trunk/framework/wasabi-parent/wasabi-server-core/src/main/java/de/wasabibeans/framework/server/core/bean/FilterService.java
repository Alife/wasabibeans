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

package de.wasabibeans.framework.server.core.bean;

import java.util.Vector;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.jboss.ejb3.annotation.SecurityDomain;

import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiPipelineDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.ObjectAlreadyExistsException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.internal.FilterServiceImpl;
import de.wasabibeans.framework.server.core.local.FilterServiceLocal;
import de.wasabibeans.framework.server.core.pipes.filter.Filter;

@SecurityDomain("wasabi")
@Stateless(name = "FilterService")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class FilterService extends ObjectService implements FilterServiceLocal {

	@Override
	public WasabiPipelineDTO create(String name, Filter filter) throws UnexpectedInternalProblemException,
			ObjectAlreadyExistsException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}
		if (filter == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"filter"));
		}

		Session s = jcr.getJCRSessionTx();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			Node pipelineNode = FilterServiceImpl.create(name, filter, s, callerPrincipal);
			s.save();
			return TransferManager.convertNode2DTO(pipelineNode);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@Override
	public void updateOrCreate(String name, Filter filter) throws UnexpectedInternalProblemException,
			ObjectAlreadyExistsException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}
		if (filter == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"filter"));
		}

		Session s = jcr.getJCRSessionTx();
		try {
			String callerPrincipal = ctx.getCallerPrincipal().getName();
			FilterServiceImpl.updateOrCreate(name, filter, s, callerPrincipal);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	@Override
	public WasabiPipelineDTO getPipeline(String name) throws UnexpectedInternalProblemException {
		if (name == null) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(WasabiExceptionMessages.INTERNAL_PARAM_NULL,
					"name"));
		}

		Session s = jcr.getJCRSessionTx();
		return TransferManager.convertNode2DTO(FilterServiceImpl.getPipeline(name, s));
	}

	@Override
	public Vector<WasabiPipelineDTO> getPipelines() throws UnexpectedInternalProblemException {
		Session s = jcr.getJCRSessionTx();
		Vector<WasabiPipelineDTO> pipelines = new Vector<WasabiPipelineDTO>();
		for (NodeIterator ni = FilterServiceImpl.getPipelines(s); ni.hasNext();) {
			pipelines.add((WasabiPipelineDTO) TransferManager.convertNode2DTO(ni.nextNode()));
		}
		return pipelines;
	}

	@Override
	public void remove(WasabiPipelineDTO pipeline) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ConcurrentModificationException {
		Session s = jcr.getJCRSessionTx();
		try {
			Node pipelineNode = TransferManager.convertDTO2Node(pipeline, s);
			FilterServiceImpl.remove(pipelineNode);
			s.save();
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}
}