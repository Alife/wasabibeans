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

package de.wasabibeans.framework.server.core.internal;

import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import de.wasabibeans.framework.server.core.authorization.WasabiCertificate;
import de.wasabibeans.framework.server.core.common.WasabiACLPriority;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiExceptionMessages;
import de.wasabibeans.framework.server.core.common.WasabiNodeProperty;
import de.wasabibeans.framework.server.core.common.WasabiNodeType;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.common.WasabiType;
import de.wasabibeans.framework.server.core.dto.TransferManager;
import de.wasabibeans.framework.server.core.dto.WasabiIdentityDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.util.SqlConnector;
import de.wasabibeans.framework.server.core.util.WasabiACLEntry;
import de.wasabibeans.framework.server.core.util.WasabiACLEntryDeprecated;
import de.wasabibeans.framework.server.core.util.WasabiACLEntryTemplate;

public class ACLServiceImpl {

	private static int[] allowanceConverter(boolean[] allowance) {
		int[] allow = new int[allowance.length];

		for (int i = 0; i < allow.length; i++) {
			if (allowance[i])
				allow[i] = 1;
			else if (!allowance[i])
				allow[i] = -1;
			else
				allow[i] = 0;
		}

		return allow;
	}

	private static String convertNodeType(String nodeType) {
		if (nodeType.equals(WasabiNodeType.USER))
			return WasabiType.USER.toString();
		if (nodeType.equals(WasabiNodeType.GROUP))
			return WasabiType.GROUP.toString();
		if (nodeType.equals(WasabiNodeType.ROOM))
			return WasabiType.ROOM.toString();
		if (nodeType.equals(WasabiNodeType.CONTAINER))
			return WasabiType.CONTAINER.toString();
		if (nodeType.equals(WasabiNodeType.LINK))
			return WasabiType.LINK.toString();
		if (nodeType.equals(WasabiNodeType.ATTRIBUTE))
			return WasabiType.ATTRIBUTE.toString();
		if (nodeType.equals(WasabiNodeType.DOCUMENT))
			return WasabiType.DOCUMENT.toString();
		return null;
	}

	public static void create(Node objectNode, Node identityNode, int[] permission, boolean[] allowance,
			long startTime, long endTime) throws UnexpectedInternalProblemException {
		if (permission.length != allowance.length) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_UNEQUAL_LENGTH, "permission", "allowance"));
		}

		try {
			int[] allow = allowanceConverter(allowance);

			String identityType = identityNode.getPrimaryNodeType().getName();
			if (identityType.equals(WasabiNodeType.USER))
				updateExplicitUserRights(objectNode, identityNode, permission, allow, startTime, endTime);
			else
				updateExplicitGroupRights(objectNode, identityNode, permission, allow, startTime, endTime);

			/* WasabiCertificate - Begin */
			if (WasabiConstants.ACL_CERTIFICATE_ENABLE)
				WasabiCertificate.invalidateCertificate(objectNode, identityNode, permission, allow);
			/* WasabiCertificate - End */
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void create(Node objectNode, Node identityNode, int[] permission, int[] allowance, long startTime,
			long endTime) throws UnexpectedInternalProblemException {
		if (permission.length != allowance.length) {
			throw new IllegalArgumentException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.INTERNAL_UNEQUAL_LENGTH, "permission", "allowance"));
		}

		try {
			String identityType = identityNode.getPrimaryNodeType().getName();
			if (identityType.equals(WasabiNodeType.USER))
				updateExplicitUserRights(objectNode, identityNode, permission, allowance, startTime, endTime);
			else
				updateExplicitGroupRights(objectNode, identityNode, permission, allowance, startTime, endTime);

			/* WasabiCertificate - Begin */
			if (WasabiConstants.ACL_CERTIFICATE_ENABLE)
				WasabiCertificate.invalidateCertificate(objectNode, identityNode, permission, allowance);
			/* WasabiCertificate - End */
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void createDefault(Node wasabiLocationNode, WasabiType wasabiType, int[] permission,
			boolean[] allowance, long startTime, long endTime) throws UnexpectedInternalProblemException {
		try {
			if (!wasabiLocationNode.getPrimaryNodeType().getName().equals(WasabiNodeType.ROOM)
					&& !wasabiLocationNode.getPrimaryNodeType().getName().equals(WasabiNodeType.CONTAINER)) {
				throw new IllegalArgumentException(WasabiExceptionMessages
						.get(WasabiExceptionMessages.INTERNAL_TYPE_CONFLICT));
			}

			updateDefaultRights(wasabiLocationNode, wasabiType, permission, allowanceConverter(allowance), startTime,
					endTime);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	// private static void createInheritanceEntries(Node identityNode, int[] permission, int[] allowance, String
	// parentId,
	// Session s, WasabiType wasabiType, QueryRunner run, String identityUUID, String parentUUID, int view,
	// int read, int comment, int execute, int insert, int write, int grant, long startTime, long endTime,
	// String inheritance_id) throws UnexpectedInternalProblemException, RepositoryException {
	//
	// Vector<Node> NodesWithInheritace = getChildrenWithInheritace(parentId, s);
	// try {
	// if (!NodesWithInheritace.isEmpty()) {
	// for (Node node : NodesWithInheritace) {
	//
	// // delete certificates of children
	// if (WasabiConstants.ACL_CERTIFICATE_ENABLE)
	// WasabiCertificate.invalidateCertificate(node, identityNode, permission, allowance);
	//
	// String objectUUID = node.getIdentifier();
	// if (wasabiType == WasabiType.USER) {
	// // SQL insert query
	// if (view == 0 && read == 0 && insert == 0 && write == 0 && execute == 0 && comment == 0
	// && grant == 0) {
	// String deleteACLEntryQuery = "DELETE FROM wasabi_rights "
	// + "WHERE `start_time`=? AND `end_time`=? AND `user_id`=? AND `inheritance_id`=?";
	// try {
	// run.update(deleteACLEntryQuery, startTime, endTime, identityUUID, inheritance_id);
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// } else {
	// String insertACLEntryQuery = "INSERT INTO wasabi_rights "
	// +
	// "(`object_id`, `user_id`, `parent_id`, `group_id` , `view`, `read`, `insert`, `write`, `execute`, `comment`, `grant`, `start_time`, `end_time`, `inheritance_id`, `priority`, `wasabi_type`)"
	// + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	// try {
	//
	// int prio;
	// if (startTime != 0 || endTime != 0)
	// prio = WasabiACLPriority.INHERITED_USER_TIME_RIGHT;
	// else
	// prio = WasabiACLPriority.INHERITED_USER_RIGHT;
	//
	// run.update(insertACLEntryQuery, objectUUID, identityUUID, parentUUID, "", view, read,
	// insert, write, execute, comment, grant, startTime, endTime, inheritance_id,
	// prio, convertNodeType(node.getPrimaryNodeType().getName()));
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// }
	// // next children
	// createInheritanceEntries(identityNode, permission, allowance, objectUUID, s, wasabiType, run,
	// identityUUID, parentId, view, read, comment, execute, insert, write, grant, startTime,
	// endTime, inheritance_id);
	// } else if (wasabiType == WasabiType.GROUP) {
	// // SQL insert query
	// if (view == 0 && read == 0 && insert == 0 && write == 0 && execute == 0 && comment == 0
	// && grant == 0) {
	// String deleteACLEntryQuery = "DELETE FROM wasabi_rights "
	// + "WHERE `start_time`=? AND `end_time`=? AND `group_id`=? AND `inheritance_id`=?";
	// try {
	// run.update(deleteACLEntryQuery, startTime, endTime, identityUUID, inheritance_id);
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// } else {
	// String insertACLEntryQuery = "INSERT INTO wasabi_rights "
	// +
	// "(`object_id`, `user_id`, `parent_id`, `group_id` , `view`, `read`, `insert`, `write`, `execute`, `comment`, `grant`, `start_time`, `end_time`, `inheritance_id`, `priority`, `wasabi_type`)"
	// + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	// try {
	//
	// int prio;
	// if (startTime != 0 || endTime != 0)
	// prio = WasabiACLPriority.INHERITED_GROUP_TIME_RIGHT;
	// else
	// prio = WasabiACLPriority.INHERITED_GROUP_RIGHT;
	//
	// run.update(insertACLEntryQuery, objectUUID, "", parentUUID, identityUUID, view, read,
	// insert, write, execute, comment, grant, startTime, endTime, inheritance_id,
	// prio, convertNodeType(node.getPrimaryNodeType().getName()));
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// }
	//
	// // next children
	// createInheritanceEntries(identityNode, permission, allowance, objectUUID, s, wasabiType, run,
	// identityUUID, parentId, view, read, comment, execute, insert, write, grant, startTime,
	// endTime, inheritance_id);
	// }
	// }
	// }
	// } catch (RepositoryException re) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
	// }
	// }

	private static void deleteInheritedGroupRights(String parentUUID, String groupUUID, long startTime, long endTime,
			int[] permission, int[] allowance, String wasabiType, QueryRunner run)
			throws UnexpectedInternalProblemException {
		try {
			String deleteACLEntryQuery = "DELETE FROM wasabi_rights "
					+ "WHERE `start_time`=? AND `end_time`=? AND `group_id`=? AND `inheritance_id`=?";
			run.update(deleteACLEntryQuery, startTime, endTime, groupUUID, parentUUID);
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}

	private static void deleteInheritedUserRights(String parentUUID, String userUUID, long startTime, long endTime,
			int[] permission, int[] allowance, String wasabiType, QueryRunner run)
			throws UnexpectedInternalProblemException {
		try {
			String deleteACLEntryQuery = "DELETE FROM wasabi_rights "
					+ "WHERE `start_time`=? AND `end_time`=? AND `user_id`=? AND `inheritance_id`=?";
			run.update(deleteACLEntryQuery, startTime, endTime, userUUID, parentUUID);
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}

	private static void dispropagateInheritance(Node objectNode) throws UnexpectedInternalProblemException {
		try {
			try {
				QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

				String objectUUID = objectNode.getIdentifier();

				String getInheritanceEntries = "SELECT `inheritance_id` FROM `wasabi_rights` "
						+ "WHERE `object_id`=? AND `inheritance_id`!=''";

				ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
				List<WasabiACLEntry> results = run.query(getInheritanceEntries, h, objectUUID);

				String[] result = new String[results.size()];
				int i = 0;

				for (WasabiACLEntry wasabiACLEntry : results) {
					result[i] = wasabiACLEntry.getInheritance_Id();
					i++;
				}

				/* WasabiCertificate - Begin */
				if (WasabiConstants.ACL_CERTIFICATE_ENABLE)
					WasabiCertificate.invalidateCertificateByObject(objectNode, new int[] { WasabiPermission.VIEW,
							WasabiPermission.READ, WasabiPermission.EXECUTE, WasabiPermission.COMMENT,
							WasabiPermission.INSERT, WasabiPermission.WRITE, WasabiPermission.GRANT }, new int[] { 0,
							0, 0, 0, 0, 0, 0 });
				/* WasabiCertificate - End */

				resetInheritance(objectNode, result);
			} catch (SQLException e) {
				throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
			}
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static List<WasabiACLEntry> getAclEntries(Node wasabiObjectNode) throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String objectUUID = ObjectServiceImpl.getUUID(wasabiObjectNode);

		String getACLEntriesQuery = "SELECT * FROM wasabi_rights WHERE `object_id`=?";

		ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
		try {
			List<WasabiACLEntry> result = run.query(getACLEntriesQuery, h, objectUUID);
			return result;
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}

	public static List<WasabiACLEntry> getAclEntriesByIdentity(Node wasabiObjectNode, Node wasabiIdentityNode)
			throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String objectUUID = ObjectServiceImpl.getUUID(wasabiObjectNode);
		String identityUUID = ObjectServiceImpl.getUUID(wasabiIdentityNode);

		String getACLEntriesByIdentityQuery = "SELECT * FROM wasabi_rights "
				+ "WHERE object_id=? AND (group_id=? OR user_id=?)";

		ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
		try {
			List<WasabiACLEntry> result = run.query(getACLEntriesByIdentityQuery, h, objectUUID, identityUUID,
					identityUUID);
			return result;
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}

	@Deprecated
	public static Vector<WasabiACLEntryDeprecated> getACLEntriesDeprecated(Node wasabiObjectNode,
			Node wasabiIdentityNode, Session s) throws UnexpectedInternalProblemException {

		int view = 0, read = 0, insert = 0, write = 0, execute = 0, comment = 0, grant = 0;
		long id;
		String wasabiGroupUUID = "", wasabiUserUUID = "";
		Node wasabiIdentity;
		List<WasabiACLEntry> wasabiACLEntry;

		Vector<WasabiACLEntryDeprecated> aclEntries = new Vector<WasabiACLEntryDeprecated>();
		if (wasabiIdentityNode == null) {
			wasabiACLEntry = getAclEntries(wasabiObjectNode);
		} else {
			wasabiACLEntry = getAclEntriesByIdentity(wasabiObjectNode, wasabiIdentityNode);
		}

		for (int i = 0; i < wasabiACLEntry.size(); i++) {

			id = wasabiACLEntry.get(i).getId();
			view = wasabiACLEntry.get(i).getView();
			read = wasabiACLEntry.get(i).getRead();
			insert = wasabiACLEntry.get(i).getInsert();
			write = wasabiACLEntry.get(i).getWrite();
			execute = wasabiACLEntry.get(i).getExecute();
			comment = wasabiACLEntry.get(i).getComment();
			grant = wasabiACLEntry.get(i).getGrant();
			wasabiGroupUUID = wasabiACLEntry.get(i).getGroup_Id();
			wasabiUserUUID = wasabiACLEntry.get(i).getUser_Id();

			if (wasabiGroupUUID.length() > 0) {
				try {
					wasabiIdentity = s.getNodeByIdentifier(wasabiGroupUUID);
				} catch (RepositoryException e) {
					throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, e);
				}
			} else {
				try {
					wasabiIdentity = s.getNodeByIdentifier(wasabiUserUUID);
				} catch (RepositoryException e) {
					throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, e);
				}
			}

			WasabiIdentityDTO wasabiIdentityDTO = TransferManager.convertNode2DTO(wasabiIdentity);

			if (view == -1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(false);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.VIEW);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				if (wasabiACLEntry.get(i).getInheritance_Id().isEmpty())
					wasabiACLEntryDeprecated.setInheritance(false);
				else
					wasabiACLEntryDeprecated.setInheritance(true);
				aclEntries.add(wasabiACLEntryDeprecated);
			} else if (view == 1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(true);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.VIEW);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				if (wasabiACLEntry.get(i).getInheritance_Id().isEmpty())
					wasabiACLEntryDeprecated.setInheritance(false);
				else
					wasabiACLEntryDeprecated.setInheritance(true);
				aclEntries.add(wasabiACLEntryDeprecated);
			}

			if (read == -1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(false);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.READ);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				if (wasabiACLEntry.get(i).getInheritance_Id().isEmpty())
					wasabiACLEntryDeprecated.setInheritance(false);
				else
					wasabiACLEntryDeprecated.setInheritance(true);
				aclEntries.add(wasabiACLEntryDeprecated);
			} else if (read == 1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(true);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.READ);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				if (wasabiACLEntry.get(i).getInheritance_Id().isEmpty())
					wasabiACLEntryDeprecated.setInheritance(false);
				else
					wasabiACLEntryDeprecated.setInheritance(true);
				aclEntries.add(wasabiACLEntryDeprecated);
			}
			if (execute == -1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(false);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.EXECUTE);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				if (wasabiACLEntry.get(i).getInheritance_Id().isEmpty())
					wasabiACLEntryDeprecated.setInheritance(false);
				else
					wasabiACLEntryDeprecated.setInheritance(true);
				aclEntries.add(wasabiACLEntryDeprecated);
			} else if (execute == 1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(true);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.EXECUTE);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				if (wasabiACLEntry.get(i).getInheritance_Id().isEmpty())
					wasabiACLEntryDeprecated.setInheritance(false);
				else
					wasabiACLEntryDeprecated.setInheritance(true);
				aclEntries.add(wasabiACLEntryDeprecated);
			}

			if (write == -1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(false);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.WRITE);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				if (wasabiACLEntry.get(i).getInheritance_Id().isEmpty())
					wasabiACLEntryDeprecated.setInheritance(false);
				else
					wasabiACLEntryDeprecated.setInheritance(true);
				aclEntries.add(wasabiACLEntryDeprecated);
			} else if (write == 1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(true);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.WRITE);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				if (wasabiACLEntry.get(i).getInheritance_Id().isEmpty())
					wasabiACLEntryDeprecated.setInheritance(false);
				else
					wasabiACLEntryDeprecated.setInheritance(true);
				aclEntries.add(wasabiACLEntryDeprecated);
			}

			if (insert == -1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(false);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.INSERT);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				aclEntries.add(wasabiACLEntryDeprecated);
			} else if (insert == 1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(true);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.INSERT);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				if (wasabiACLEntry.get(i).getInheritance_Id().isEmpty())
					wasabiACLEntryDeprecated.setInheritance(false);
				else
					wasabiACLEntryDeprecated.setInheritance(true);
				aclEntries.add(wasabiACLEntryDeprecated);
			}

			if (comment == -1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(false);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.COMMENT);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				if (wasabiACLEntry.get(i).getInheritance_Id().isEmpty())
					wasabiACLEntryDeprecated.setInheritance(false);
				else
					wasabiACLEntryDeprecated.setInheritance(true);
				aclEntries.add(wasabiACLEntryDeprecated);
			} else if (comment == 1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(true);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.COMMENT);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				if (wasabiACLEntry.get(i).getInheritance_Id().isEmpty())
					wasabiACLEntryDeprecated.setInheritance(false);
				else
					wasabiACLEntryDeprecated.setInheritance(true);
				aclEntries.add(wasabiACLEntryDeprecated);
			}

			if (grant == -1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(false);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.GRANT);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				if (wasabiACLEntry.get(i).getInheritance_Id().isEmpty())
					wasabiACLEntryDeprecated.setInheritance(false);
				else
					wasabiACLEntryDeprecated.setInheritance(true);
				aclEntries.add(wasabiACLEntryDeprecated);
			} else if (grant == 1) {
				WasabiACLEntryDeprecated wasabiACLEntryDeprecated = new WasabiACLEntryDeprecated();
				wasabiACLEntryDeprecated.setAllowance(true);
				wasabiACLEntryDeprecated.setId(id);
				wasabiACLEntryDeprecated.setPermission(WasabiPermission.GRANT);
				wasabiACLEntryDeprecated.setWasabiIdentity(wasabiIdentityDTO);
				if (wasabiACLEntry.get(i).getInheritance_Id().isEmpty())
					wasabiACLEntryDeprecated.setInheritance(false);
				else
					wasabiACLEntryDeprecated.setInheritance(true);
				aclEntries.add(wasabiACLEntryDeprecated);
			}
		}
		return aclEntries;

	}

	public static Vector<Node> getChildren(Node objectNode) {
		Vector<Node> result = new Vector<Node>();

		Node parentNode = objectNode;

		NodeIterator iteratorRooms = getChildrenNodes(parentNode, WasabiNodeProperty.ROOMS);
		NodeIterator iteratorContainers = getChildrenNodes(parentNode, WasabiNodeProperty.CONTAINERS);
		NodeIterator iteratorLinks = getChildrenNodes(parentNode, WasabiNodeProperty.LINKS);
		NodeIterator iteratorDocuments = getChildrenNodes(parentNode, WasabiNodeProperty.DOCUMENTS);
		NodeIterator iteratorAttributes = getChildrenNodes(parentNode, WasabiNodeProperty.ATTRIBUTES);
		NodeIterator iteratorSubgroups = getChildrenNodes(parentNode, WasabiNodeProperty.SUBGROUPS);

		while (iteratorRooms != null && iteratorRooms.hasNext()) {
			Node aNode = iteratorRooms.nextNode();
			result.add(aNode);
		}

		while (iteratorContainers != null && iteratorContainers.hasNext()) {
			Node aNode = iteratorContainers.nextNode();
			result.add(aNode);
		}

		while (iteratorLinks != null && iteratorLinks.hasNext()) {
			Node aNode = iteratorLinks.nextNode();
			result.add(aNode);
		}

		while (iteratorDocuments != null && iteratorDocuments.hasNext()) {
			Node aNode = iteratorDocuments.nextNode();
			result.add(aNode);
		}

		while (iteratorAttributes != null && iteratorAttributes.hasNext()) {
			Node aNode = iteratorAttributes.nextNode();
			result.add(aNode);
		}

		while (iteratorSubgroups != null && iteratorSubgroups.hasNext()) {
			Node aNode = iteratorSubgroups.nextNode();
			result.add(aNode);
		}

		return result;
	}

	private static NodeIterator getChildrenNodes(Node parentNode, String nodeType) {
		try {
			NodeIterator iteratorRooms = parentNode.getNode(nodeType).getNodes();
			return iteratorRooms;
		} catch (RepositoryException re) {
			return null;
		}
	}

	// private static Vector<Node> getChildrenWithInheritace(String parentId, Session s)
	// throws UnexpectedInternalProblemException {
	// try {
	//
	// Vector<Node> result = new Vector<Node>();
	//
	// Node parentNode = s.getNodeByIdentifier(parentId);
	//
	// NodeIterator iteratorRooms = getChildrenNodes(parentNode, WasabiNodeProperty.ROOMS);
	// NodeIterator iteratorContainers = getChildrenNodes(parentNode, WasabiNodeProperty.CONTAINERS);
	// NodeIterator iteratorLinks = getChildrenNodes(parentNode, WasabiNodeProperty.LINKS);
	// NodeIterator iteratorDocuments = getChildrenNodes(parentNode, WasabiNodeProperty.DOCUMENTS);
	// NodeIterator iteratorAttributes = getChildrenNodes(parentNode, WasabiNodeProperty.ATTRIBUTES);
	//
	// while (iteratorRooms != null && iteratorRooms.hasNext()) {
	// Node aNode = iteratorRooms.nextNode();
	// if (aNode.getProperty(WasabiNodeProperty.INHERITANCE).getBoolean())
	// result.add(aNode);
	// }
	//
	// while (iteratorContainers != null && iteratorContainers.hasNext()) {
	// Node aNode = iteratorContainers.nextNode();
	// if (aNode.getProperty(WasabiNodeProperty.INHERITANCE).getBoolean())
	// result.add(aNode);
	// }
	//
	// while (iteratorLinks != null && iteratorLinks.hasNext()) {
	// Node aNode = iteratorLinks.nextNode();
	// if (aNode.getProperty(WasabiNodeProperty.INHERITANCE).getBoolean())
	// result.add(aNode);
	// }
	//
	// while (iteratorDocuments != null && iteratorDocuments.hasNext()) {
	// Node aNode = iteratorDocuments.nextNode();
	// if (aNode.getProperty(WasabiNodeProperty.INHERITANCE).getBoolean())
	// result.add(aNode);
	// }
	//
	// while (iteratorAttributes != null && iteratorAttributes.hasNext()) {
	// Node aNode = iteratorAttributes.nextNode();
	// if (aNode.getProperty(WasabiNodeProperty.INHERITANCE).getBoolean())
	// result.add(aNode);
	// }
	//
	// return result;
	// } catch (RepositoryException re) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
	// }
	// }

	private static Vector<Node> getChildrenWithInheritace(Node parentNode) throws UnexpectedInternalProblemException {
		try {

			Vector<Node> result = new Vector<Node>();

			NodeIterator iteratorRooms = getChildrenNodes(parentNode, WasabiNodeProperty.ROOMS);
			NodeIterator iteratorContainers = getChildrenNodes(parentNode, WasabiNodeProperty.CONTAINERS);
			NodeIterator iteratorLinks = getChildrenNodes(parentNode, WasabiNodeProperty.LINKS);
			NodeIterator iteratorDocuments = getChildrenNodes(parentNode, WasabiNodeProperty.DOCUMENTS);
			NodeIterator iteratorAttributes = getChildrenNodes(parentNode, WasabiNodeProperty.ATTRIBUTES);
			NodeIterator iteratorGroups = getChildrenNodes(parentNode, WasabiNodeProperty.SUBGROUPS);

			while (iteratorRooms != null && iteratorRooms.hasNext()) {
				Node aNode = iteratorRooms.nextNode();
				if (aNode.getProperty(WasabiNodeProperty.INHERITANCE).getBoolean())
					result.add(aNode);
			}

			while (iteratorContainers != null && iteratorContainers.hasNext()) {
				Node aNode = iteratorContainers.nextNode();
				if (aNode.getProperty(WasabiNodeProperty.INHERITANCE).getBoolean())
					result.add(aNode);
			}

			while (iteratorLinks != null && iteratorLinks.hasNext()) {
				Node aNode = iteratorLinks.nextNode();
				if (aNode.getProperty(WasabiNodeProperty.INHERITANCE).getBoolean())
					result.add(aNode);
			}

			while (iteratorDocuments != null && iteratorDocuments.hasNext()) {
				Node aNode = iteratorDocuments.nextNode();
				if (aNode.getProperty(WasabiNodeProperty.INHERITANCE).getBoolean())
					result.add(aNode);
			}

			while (iteratorAttributes != null && iteratorAttributes.hasNext()) {
				Node aNode = iteratorAttributes.nextNode();
				if (aNode.getProperty(WasabiNodeProperty.INHERITANCE).getBoolean())
					result.add(aNode);
			}

			while (iteratorGroups != null && iteratorGroups.hasNext()) {
				Node aNode = iteratorGroups.nextNode();
				if (aNode.getProperty(WasabiNodeProperty.INHERITANCE).getBoolean())
					result.add(aNode);
			}

			return result;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static List<WasabiACLEntryTemplate> getDefaultACLEntries(Node wasabiLocationNode, Session s)
			throws UnexpectedInternalProblemException {
		try {
			if (!wasabiLocationNode.getPrimaryNodeType().getName().equals(WasabiNodeType.ROOM)
					&& !wasabiLocationNode.getPrimaryNodeType().getName().equals(WasabiNodeType.CONTAINER)) {
				throw new IllegalArgumentException(WasabiExceptionMessages
						.get(WasabiExceptionMessages.INTERNAL_TYPE_CONFLICT));
			}

			QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

			String locationUUID = ObjectServiceImpl.getUUID(wasabiLocationNode);

			String getDefaultACLEntriesQuery = "SELECT * FROM wasabi_template_rights WHERE `location_id`=?";

			ResultSetHandler<List<WasabiACLEntryTemplate>> h = new BeanListHandler<WasabiACLEntryTemplate>(
					WasabiACLEntryTemplate.class);
			try {
				List<WasabiACLEntryTemplate> result = run.query(getDefaultACLEntriesQuery, h, locationUUID);
				return result;
			} catch (SQLException e) {
				throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
			}

		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static List<WasabiACLEntryTemplate> getDefaultACLEntriesByType(Node wasabiLocationNode,
			WasabiType wasabiType, Session s) throws UnexpectedInternalProblemException {
		try {
			if (!wasabiLocationNode.getPrimaryNodeType().getName().equals(WasabiNodeType.ROOM)
					&& !wasabiLocationNode.getPrimaryNodeType().getName().equals(WasabiNodeType.CONTAINER)) {
				throw new IllegalArgumentException(WasabiExceptionMessages
						.get(WasabiExceptionMessages.INTERNAL_TYPE_CONFLICT));
			}

			QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

			String locationUUID = ObjectServiceImpl.getUUID(wasabiLocationNode);

			String getDefaultACLEntriesQuery = "SELECT * FROM wasabi_template_rights "
					+ "WHERE `location_id`=? AND `wasabi_type`=?";

			ResultSetHandler<List<WasabiACLEntryTemplate>> h = new BeanListHandler<WasabiACLEntryTemplate>(
					WasabiACLEntryTemplate.class);
			try {
				List<WasabiACLEntryTemplate> result = run.query(getDefaultACLEntriesQuery, h, locationUUID, wasabiType
						.toString());
				return result;
			} catch (SQLException e) {
				throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
			}

		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static boolean getInheritance(Node objectNode) throws UnexpectedInternalProblemException {
		if (objectNode == null)
			return false;

		try {
			if (objectNode.getProperty(WasabiNodeProperty.INHERITANCE).getBoolean())
				return true;
			else
				return false;
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	private static void propagateGroupInheritance(String objectUUID, String parentUUID, String groupUUID, int view,
			int read, int comment, int execute, int insert, int write, int grant, String inheritanceID, long startTime,
			long endTime, String wasabiType, QueryRunner run) throws UnexpectedInternalProblemException {
		try {
			String insertUserACLEntryQuery = "INSERT INTO wasabi_rights "
					+ "(`object_id`, `user_id`, `parent_id`, `group_id`, "
					+ "`view`, `read`, `insert`, `write`, `execute`, `comment`, `grant`, "
					+ "`start_time`, `end_time`, `priority`, `inheritance_id`, `wasabi_type`)"
					+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			int prio;
			if ((startTime != 0 || endTime != 0))
				prio = WasabiACLPriority.INHERITED_USER_TIME_RIGHT;
			else
				prio = WasabiACLPriority.INHERITED_USER_RIGHT;

			run.update(insertUserACLEntryQuery, objectUUID, "", parentUUID, groupUUID, view, read, insert, write,
					execute, comment, grant, startTime, endTime, prio, inheritanceID, wasabiType);
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}

	private static void propagateInheritance(Node objectNode, List<WasabiACLEntry> result)
			throws UnexpectedInternalProblemException {
		try {
			QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

			String objectUUID = ObjectServiceImpl.getUUID(objectNode);
			Node parentNode = objectNode.getParent().getParent();
			String parentUUID = ObjectServiceImpl.getUUID(parentNode);

			try {
				if (result.isEmpty()) {
					String getParentACLEntries = "SELECT * FROM `wasabi_rights` " + "WHERE `object_id`=?";
					ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
					result = run.query(getParentACLEntries, h, parentUUID);
				}

				for (WasabiACLEntry wasabiACLEntry : result) {
					String userUUID = wasabiACLEntry.getUser_Id();
					String groupUUID = wasabiACLEntry.getGroup_Id();

					int view = wasabiACLEntry.getView();
					int read = wasabiACLEntry.getRead();
					int comment = wasabiACLEntry.getComment();
					int execute = wasabiACLEntry.getExecute();
					int insert = wasabiACLEntry.getInsert();
					int write = wasabiACLEntry.getWrite();
					int grant = wasabiACLEntry.getGrant();
					long startTime = wasabiACLEntry.getStart_Time();
					long endTime = wasabiACLEntry.getEnd_Time();
					String wasabiType = wasabiACLEntry.getWasabi_Type();
					String inheritance = wasabiACLEntry.getInheritance_Id();
					String objectID = wasabiACLEntry.getObject_Id();

					if (!(inheritance.length() > 0))
						inheritance = objectID;

					if (userUUID.length() > 0) {
						propagateUserInheritance(objectUUID, parentUUID, userUUID, view, read, comment, execute,
								insert, write, grant, inheritance, startTime, endTime, wasabiType, run);

						/* WasabiCertificate - Begin */
						if (WasabiConstants.ACL_CERTIFICATE_ENABLE)
							WasabiCertificate.invalidateCertificate(objectNode, objectNode.getSession()
									.getNodeByIdentifier(userUUID), new int[] { WasabiPermission.VIEW,
									WasabiPermission.READ, WasabiPermission.EXECUTE, WasabiPermission.COMMENT,
									WasabiPermission.INSERT, WasabiPermission.WRITE, WasabiPermission.GRANT },
									new int[] { 0, 0, 0, 0, 0, 0, 0 });
						/* WasabiCertificate - End */
					} else {
						propagateGroupInheritance(objectUUID, parentUUID, groupUUID, view, read, comment, execute,
								insert, write, grant, inheritance, startTime, endTime, wasabiType, run);

						/* WasabiCertificate - Begin */
						if (WasabiConstants.ACL_CERTIFICATE_ENABLE)
							WasabiCertificate.invalidateCertificate(objectNode, objectNode.getSession()
									.getNodeByIdentifier(groupUUID), new int[] { WasabiPermission.VIEW,
									WasabiPermission.READ, WasabiPermission.EXECUTE, WasabiPermission.COMMENT,
									WasabiPermission.INSERT, WasabiPermission.WRITE, WasabiPermission.GRANT },
									new int[] { 0, 0, 0, 0, 0, 0, 0 });
						/* WasabiCertificate - End */
					}
				}
				// propagate to children
				Vector<Node> children = getChildrenWithInheritace(objectNode);
				for (Node node : children) {
					propagateInheritance(node, result);
				}

			} catch (SQLException e) {
				throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
			}
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	private static void propagateInheritedGroupRights(Node parentNode, String groupUUID, int view, int read,
			int comment, int execute, int insert, int write, int grant, long startTime, long endTime,
			String inheritanceID, String wasabiType, QueryRunner run) throws UnexpectedInternalProblemException {
		Vector<Node> NodesWithInheritace = getChildrenWithInheritace(parentNode);
		String parentUUID = ObjectServiceImpl.getUUID(parentNode);

		try {
			for (Node node : NodesWithInheritace) {
				String objectUUID = ObjectServiceImpl.getUUID(node);

				String insertUserACLEntryQuery = "INSERT INTO wasabi_rights "
						+ "(`object_id`, `user_id`, `parent_id`, `group_id`, "
						+ "`view`, `read`, `insert`, `write`, `execute`, `comment`, `grant`, "
						+ "`start_time`, `end_time`, `priority`, `inheritance_id`, `wasabi_type`)"
						+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

				int prio;
				if ((startTime != 0 || endTime != 0))
					prio = WasabiACLPriority.INHERITED_GROUP_TIME_RIGHT;
				else
					prio = WasabiACLPriority.INHERITED_GROUP_RIGHT;

				run.update(insertUserACLEntryQuery, objectUUID, "", parentUUID, groupUUID, view, read, insert, write,
						execute, comment, grant, startTime, endTime, prio, inheritanceID, convertNodeType(wasabiType));

				// propagate to children
				propagateInheritedGroupRights(node, groupUUID, view, read, comment, execute, insert, write, grant,
						startTime, endTime, inheritanceID, wasabiType, run);
			}
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}

	private static void propagateInheritedUserRights(Node parentNode, String userUUID, int view, int read, int comment,
			int execute, int insert, int write, int grant, long startTime, long endTime, String inheritanceID,
			String wasabiType, QueryRunner run) throws UnexpectedInternalProblemException {
		Vector<Node> NodesWithInheritace = getChildrenWithInheritace(parentNode);
		String parentUUID = ObjectServiceImpl.getUUID(parentNode);

		try {
			for (Node node : NodesWithInheritace) {
				String objectUUID = ObjectServiceImpl.getUUID(node);

				String insertUserACLEntryQuery = "INSERT INTO wasabi_rights "
						+ "(`object_id`, `user_id`, `parent_id`, `group_id`, "
						+ "`view`, `read`, `insert`, `write`, `execute`, `comment`, `grant`, "
						+ "`start_time`, `end_time`, `priority`, `inheritance_id`, `wasabi_type`)"
						+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

				int prio;
				if ((startTime != 0 || endTime != 0))
					prio = WasabiACLPriority.INHERITED_USER_TIME_RIGHT;
				else
					prio = WasabiACLPriority.INHERITED_USER_RIGHT;

				run.update(insertUserACLEntryQuery, objectUUID, userUUID, parentUUID, "", view, read, insert, write,
						execute, comment, grant, startTime, endTime, prio, inheritanceID, convertNodeType(wasabiType));

				// propagate to children
				propagateInheritedUserRights(node, userUUID, view, read, comment, execute, insert, write, grant,
						startTime, endTime, inheritanceID, wasabiType, run);
			}
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}

	private static void propagateUserInheritance(String objectUUID, String parentUUID, String userUUID, int view,
			int read, int comment, int execute, int insert, int write, int grant, String inheritanceID, long startTime,
			long endTime, String wasabiType, QueryRunner run) throws UnexpectedInternalProblemException {
		try {
			String insertUserACLEntryQuery = "INSERT INTO wasabi_rights "
					+ "(`object_id`, `user_id`, `parent_id`, `group_id`, "
					+ "`view`, `read`, `insert`, `write`, `execute`, `comment`, `grant`, "
					+ "`start_time`, `end_time`, `priority`, `inheritance_id`, `wasabi_type`)"
					+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			int prio;
			if ((startTime != 0 || endTime != 0))
				prio = WasabiACLPriority.INHERITED_USER_TIME_RIGHT;
			else
				prio = WasabiACLPriority.INHERITED_USER_RIGHT;

			run.update(insertUserACLEntryQuery, objectUUID, userUUID, parentUUID, "", view, read, insert, write,
					execute, comment, grant, startTime, endTime, prio, inheritanceID, wasabiType);
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}

	public static void remove(Node objectNode, Node identityNode, int[] permission, long startTime, long endTime)
			throws UnexpectedInternalProblemException {
		int[] allowance = new int[permission.length];

		for (int i = 0; i < allowance.length; i++)
			allowance[i] = 0;

		try {

			String identityType = identityNode.getPrimaryNodeType().getName();
			if (identityType.equals(WasabiNodeType.USER))
				updateExplicitUserRights(objectNode, identityNode, permission, allowance, startTime, endTime);
			else
				updateExplicitGroupRights(objectNode, identityNode, permission, allowance, startTime, endTime);

			/* WasabiCertificate - Begin */
			if (WasabiConstants.ACL_CERTIFICATE_ENABLE)
				WasabiCertificate.invalidateCertificate(objectNode, identityNode, permission, allowance);
			/* WasabiCertificate - End */
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void removeDefault(Node wasabiLocationNode, WasabiType wasabiType, int[] permission, long startTime,
			long endTime) throws UnexpectedInternalProblemException {
		int[] allowance = new int[permission.length];

		for (int i = 0; i < allowance.length; i++)
			allowance[i] = 0;

		try {
			updateDefaultRights(wasabiLocationNode, wasabiType, permission, allowance, startTime, endTime);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void reset(Node objectNode) throws UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String objectUUID = ObjectServiceImpl.getUUID(objectNode);
		String deleteRights = "DELETE FROM `wasabi_rights` WHERE `object_id`=? AND `inheritance_id`!=''";
		try {
			run.update(deleteRights, objectUUID);
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}

		/* WasabiCertificate - Begin */
		if (WasabiConstants.ACL_CERTIFICATE_ENABLE)
			WasabiCertificate.invalidateCertificateByObject(objectNode, new int[] { WasabiPermission.VIEW,
					WasabiPermission.READ, WasabiPermission.EXECUTE, WasabiPermission.COMMENT, WasabiPermission.INSERT,
					WasabiPermission.WRITE, WasabiPermission.GRANT }, new int[] { 0, 0, 0, 0, 0, 0, 0 });
		/* WasabiCertificate - End */

		resetChildren(objectNode, run);
	}

	private static void resetChildren(Node objectNode, QueryRunner run) throws UnexpectedInternalProblemException {
		Vector<Node> Nodes = getChildren(objectNode);

		if (!Nodes.isEmpty()) {
			for (Node node : Nodes) {
				// clean ACLEntries
				String objectUUID = ObjectServiceImpl.getUUID(node);
				String deleteRights = "DELETE FROM `wasabi_rights` WHERE `object_id`=?";
				try {
					run.update(deleteRights, objectUUID);
				} catch (SQLException e) {
					throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
				}

				/* WasabiCertificate - Begin */
				if (WasabiConstants.ACL_CERTIFICATE_ENABLE)
					WasabiCertificate.invalidateCertificateByObject(objectNode, new int[] { WasabiPermission.VIEW,
							WasabiPermission.READ, WasabiPermission.EXECUTE, WasabiPermission.COMMENT,
							WasabiPermission.INSERT, WasabiPermission.WRITE, WasabiPermission.GRANT }, new int[] { 0,
							0, 0, 0, 0, 0, 0 });
				/* WasabiCertificate - End */

				// next children
				resetChildren(node, run);

				// set entries from parent
				setInheritance(objectNode, true);
			}
		}
	}

	public static void resetInheritance(Node objectNode, String[] inheritance_ids)
			throws UnexpectedInternalProblemException {
		/* WasabiCertificate - Begin */
		if (WasabiConstants.ACL_CERTIFICATE_ENABLE)
			WasabiCertificate.invalidateCertificateByObject(objectNode, new int[] { WasabiPermission.VIEW,
					WasabiPermission.READ, WasabiPermission.EXECUTE, WasabiPermission.COMMENT, WasabiPermission.INSERT,
					WasabiPermission.WRITE, WasabiPermission.GRANT }, new int[] { 0, 0, 0, 0, 0, 0, 0 });
		/* WasabiCertificate - End */

		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String parentId = ObjectServiceImpl.getUUID(objectNode);
		Vector<Node> Nodes = getChildren(objectNode);

		String inheritanceQuery = " (";
		for (int i = 0; i < inheritance_ids.length; i++) {
			inheritanceQuery = inheritanceQuery + " `inheritance_id`='" + inheritance_ids[i] + "' OR";
		}
		inheritanceQuery = inheritanceQuery + "`inheritance_id`='placeHolderValue')";

		String deleteRight = "DELETE FROM `wasabi_rights` WHERE `object_id`=? AND" + inheritanceQuery;
		try {
			run.update(deleteRight, parentId);
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}

		if (!Nodes.isEmpty()) {
			for (Node node : Nodes) {
				if (ACLServiceImpl.getInheritance(node))
					resetInheritance(node, inheritance_ids);
			}
		}
	}

	public static void resetInheritanceForGroups(Node groupNode, String[] inheritance_ids)
			throws UnexpectedInternalProblemException {
		try {
			QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

			String parentId = ObjectServiceImpl.getUUID(groupNode);
			Vector<Node> Nodes = new Vector<Node>();
			NodeIterator ni = GroupServiceImpl.getSubGroups(groupNode);
			while (ni.hasNext())
				Nodes.add(ni.nextNode());

			String inheritanceQuery = " (";
			for (int i = 0; i < inheritance_ids.length; i++) {
				inheritanceQuery = inheritanceQuery + " `inheritance_id`='" + inheritance_ids[i] + "' OR";
			}
			inheritanceQuery = inheritanceQuery + "`inheritance_id`='placeHolderValue')";

			String deleteRight = "DELETE FROM `wasabi_rights` WHERE `object_id`=? AND" + inheritanceQuery;
			try {
				run.update(deleteRight, parentId);
			} catch (SQLException e) {
				throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
			}

			if (!Nodes.isEmpty()) {
				for (Node node : Nodes) {
					if (node.getProperty(WasabiNodeProperty.INHERITANCE).getBoolean())
						resetInheritanceForGroups(node, inheritance_ids);
				}
			}
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	public static void setInheritance(Node objectNode, boolean inheritance) throws UnexpectedInternalProblemException {
		if (inheritance)
			propagateInheritance(objectNode, new Vector<WasabiACLEntry>());
		else {
			dispropagateInheritance(objectNode);
		}
	}

	// public static void setInheritance(Node objectNode, boolean inheritance, Session s, boolean doJcrSave)
	// throws UnexpectedInternalProblemException, ConcurrentModificationException {
	// try {
	// QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());
	// objectNode.setProperty(WasabiNodeProperty.INHERITANCE, inheritance);
	// Node parentNode = objectNode.getParent().getParent();
	//
	// if (inheritance == true) {
	//
	// try {
	// String getParentACLEntries = "SELECT * FROM wasabi_rights " + "WHERE `object_id`=?";
	//
	// ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result = run.query(getParentACLEntries, h, parentNode.getIdentifier());
	//
	// if (!result.isEmpty()) {
	// for (WasabiACLEntry wasabiACLEntry : result) {
	//
	// String wasabiUserID = wasabiACLEntry.getUser_Id();
	// String wasabiGroupID = wasabiACLEntry.getGroup_Id();
	// Node identityNode;
	// int[] allowance = new int[7];
	//
	// // Get data from SQL result
	// if (wasabiUserID.isEmpty()) {
	// identityNode = s.getNodeByIdentifier(wasabiGroupID);
	// } else {
	// identityNode = s.getNodeByIdentifier(wasabiUserID);
	// }
	//
	// allowance[WasabiPermission.VIEW] = wasabiACLEntry.getView();
	// allowance[WasabiPermission.READ] = wasabiACLEntry.getRead();
	// allowance[WasabiPermission.EXECUTE] = wasabiACLEntry.getExecute();
	// allowance[WasabiPermission.COMMENT] = wasabiACLEntry.getComment();
	// allowance[WasabiPermission.INSERT] = wasabiACLEntry.getInsert();
	// allowance[WasabiPermission.WRITE] = wasabiACLEntry.getWrite();
	// allowance[WasabiPermission.GRANT] = wasabiACLEntry.getGrant();
	//
	// long startTime = wasabiACLEntry.getStart_Time();
	// long endTime = wasabiACLEntry.getEnd_Time();
	//
	// String inheritance_id = wasabiACLEntry.getInheritance_Id();
	//
	// // Case: explicit right. Inheritance id is equal to parent id.
	// if (inheritance_id.isEmpty()) {
	// updateRights(objectNode, identityNode, new int[] { WasabiPermission.VIEW,
	// WasabiPermission.READ, WasabiPermission.EXECUTE, WasabiPermission.COMMENT,
	// WasabiPermission.INSERT, WasabiPermission.WRITE, WasabiPermission.GRANT },
	// allowance, startTime, endTime, parentNode.getIdentifier());
	// }
	// // Case: inherited right. Inheritance id is equal to the given inheritance id (root node of
	// // this inherited right).
	// else {
	// updateRights(objectNode, identityNode, new int[] { WasabiPermission.VIEW,
	// WasabiPermission.READ, WasabiPermission.EXECUTE, WasabiPermission.COMMENT,
	// WasabiPermission.INSERT, WasabiPermission.WRITE, WasabiPermission.GRANT },
	// allowance, startTime, endTime, inheritance_id);
	// }
	// }
	//
	// // Look child nodes and set entries if inheritance is true
	// Vector<Node> childrenNodes = new Vector<Node>();
	// childrenNodes = getChildren(objectNode);
	//
	// for (Node node : childrenNodes) {
	// if (node.getProperty(WasabiNodeProperty.INHERITANCE).getBoolean())
	// setInheritance(node, true, s, false);
	// }
	// }
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// } else {
	// try {
	// String objectUUID = objectNode.getIdentifier();
	//
	// String getInheritanceEntries = "SELECT `inheritance_id` FROM `wasabi_rights` "
	// + "WHERE `object_id`=? AND `inheritance_id`!=''";
	//
	// ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> results = run.query(getInheritanceEntries, h, objectUUID);
	//
	// String[] result = new String[results.size()];
	// int i = 0;
	//
	// for (WasabiACLEntry wasabiACLEntry : results) {
	// result[i] = wasabiACLEntry.getInheritance_Id();
	// i++;
	// }
	//
	// resetInheritance(objectNode, result);
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// }
	// if (doJcrSave) {
	// s.save();
	// }
	// } catch (InvalidItemStateException iise) {
	// throw new ConcurrentModificationException(WasabiExceptionMessages.CONCURRENT_MOD_INVALIDSTATE, iise);
	// } catch (LockException le) {
	// throw new ConcurrentModificationException(WasabiExceptionMessages.get(
	// WasabiExceptionMessages.CONCURRENT_MOD_LOCKED, le.getFailureNodePath()), le);
	// } catch (RepositoryException re) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
	// }
	// }

	/**
	 * Sets the inheritance flag as a node property. The inheritance flag is true if inheritance is active, otherwise
	 * false.
	 * 
	 * @param objectNode
	 *            the node representing a wasabi-object
	 * @param inheritance
	 * @param s
	 * @param doJcrSave
	 * @throws UnexpectedInternalProblemException
	 * @throws ConcurrentModificationException
	 */
	public static void setInheritanceNodeProperty(Node objectNode, boolean inheritance, Session s, boolean doJcrSave)
			throws UnexpectedInternalProblemException, ConcurrentModificationException {
		try {
			objectNode.setProperty(WasabiNodeProperty.INHERITANCE, inheritance);
			if (doJcrSave) {
				s.save();
			}
		} catch (InvalidItemStateException iise) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.CONCURRENT_MOD_INVALIDSTATE, iise);
		} catch (LockException le) {
			throw new ConcurrentModificationException(WasabiExceptionMessages.get(
					WasabiExceptionMessages.CONCURRENT_MOD_LOCKED, le.getFailureNodePath()), le);
		} catch (RepositoryException re) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.JCR_REPOSITORY_FAILURE, re);
		}
	}

	private static void updateDefaultRights(Node wasabiLocationNode, WasabiType wasabiType, int[] permission,
			int[] allowance, long startTime, long endTime) throws RepositoryException,
			UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String locationUUID = ObjectServiceImpl.getUUID(wasabiLocationNode);

		int view = 0, read = 0, insert = 0, write = 0, execute = 0, comment = 0, grant = 0;

		try {
			String getDefaultACLEntryQuery = "SELECT * FROM wasabi_template_rights "
					+ "WHERE `location_id`=? AND `start_time`=? AND `end_time`=? AND `wasabi_type`=?";

			ResultSetHandler<List<WasabiACLEntryTemplate>> h = new BeanListHandler<WasabiACLEntryTemplate>(
					WasabiACLEntryTemplate.class);
			List<WasabiACLEntryTemplate> result = run.query(getDefaultACLEntryQuery, h, locationUUID, startTime,
					endTime, wasabiType.toString());

			if (!result.isEmpty()) {
				view = result.get(0).getView();
				read = result.get(0).getRead();
				insert = result.get(0).getInsert();
				write = result.get(0).getWrite();
				execute = result.get(0).getExecute();
				comment = result.get(0).getComment();
				grant = result.get(0).getGrant();

				for (int i = 0; i < permission.length; i++) {
					switch (permission[i]) {
					case WasabiPermission.VIEW:
						view = allowance[i];
						break;
					case WasabiPermission.READ:
						read = allowance[i];
						break;
					case WasabiPermission.INSERT:
						insert = allowance[i];
						break;
					case WasabiPermission.WRITE:
						write = allowance[i];
						break;
					case WasabiPermission.EXECUTE:
						execute = allowance[i];
						break;
					case WasabiPermission.COMMENT:
						comment = allowance[i];
						break;
					case WasabiPermission.GRANT:
						grant = allowance[i];
						break;
					}
				}

				if (view == 0 && read == 0 && insert == 0 && write == 0 && execute == 0 && comment == 0 && grant == 0) {
					String deleteDefaultACLEntryQuery = "DELETE FROM wasabi_template_rights "
							+ "WHERE `location_id`=? AND `start_time`=? AND `end_time`=? AND `wasabi_type`=?";
					run.update(deleteDefaultACLEntryQuery, locationUUID, startTime, endTime, wasabiType.toString());
				} else {
					String updateDefaultACLEntryQuery = "UPDATE wasabi_template_rights SET "
							+ "`view`=?, `read`=?, `insert`=?, `write`=?, `execute`=?, `comment`=?, `grant`=?"
							+ " WHERE `location_id`=? AND `start_time`=? AND `end_time`=? AND `wasabi_type`=?";
					run.update(updateDefaultACLEntryQuery, view, read, insert, write, execute, comment, grant,
							locationUUID, startTime, endTime, wasabiType.toString());
				}
			} else {

				for (int i = 0; i < permission.length; i++) {
					switch (permission[i]) {
					case WasabiPermission.VIEW:
						view = allowance[i];
						break;
					case WasabiPermission.READ:
						read = allowance[i];
						break;
					case WasabiPermission.INSERT:
						insert = allowance[i];
						break;
					case WasabiPermission.WRITE:
						write = allowance[i];
						break;
					case WasabiPermission.EXECUTE:
						execute = allowance[i];
						break;
					case WasabiPermission.COMMENT:
						comment = allowance[i];
						break;
					case WasabiPermission.GRANT:
						grant = allowance[i];
						break;
					}
				}

				if (view == 0 && read == 0 && insert == 0 && write == 0 && execute == 0 && comment == 0 && grant == 0) {
					String deleteDefaultACLEntryQuery = "DELETE FROM wasabi_template_rights "
							+ "WHERE `location_id`=? AND `start_time`=? AND `end_time`=? AND `wasabi_type`=?";
					run.update(deleteDefaultACLEntryQuery, locationUUID, startTime, endTime, wasabiType.toString());
				} else {
					String insertUserACLEntryQuery = "INSERT INTO wasabi_template_rights "
							+ "(`location_id`, `wasabi_type`, `view`, `read`, `insert`, `write`, `execute`, `comment`, `grant`, `start_time`, `end_time`)"
							+ " VALUES (?,?,?,?,?,?,?,?,?,?,?)";
					run.update(insertUserACLEntryQuery, locationUUID, wasabiType.toString(), view, read, insert, write,
							execute, comment, grant, startTime, endTime);
				}
			}
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}

	private static void updateExplicitGroupRights(Node objectNode, Node groupNode, int[] permission, int[] allowance,
			long startTime, long endTime) throws RepositoryException, UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String objectUUID = ObjectServiceImpl.getUUID(objectNode);
		String groupUUID = ObjectServiceImpl.getUUID(groupNode);
		String parentUUID = ObjectServiceImpl.getUUID(ObjectServiceImpl.getEnvironment(objectNode));
		String wasabiType = objectNode.getPrimaryNodeType().getName();

		int view = 0, read = 0, insert = 0, write = 0, execute = 0, comment = 0, grant = 0;

		try {
			// check if an entry exists in the database
			String getUserACLEntryQuery = "SELECT `view`, `read`, `execute`, `comment`, `insert`, `write`, `grant` "
					+ "FROM `wasabi_rights` " + "WHERE `object_id`=? " + "AND `start_time`=? " + "AND `end_time`=? "
					+ "AND `group_id`=? " + "AND `inheritance_id`='' ";

			ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
			List<WasabiACLEntry> result = run.query(getUserACLEntryQuery, h, objectUUID, startTime, endTime, groupUUID);

			// case entry exists: get values and update entry with new values
			if (!result.isEmpty()) {
				view = result.get(0).getView();
				read = result.get(0).getRead();
				insert = result.get(0).getInsert();
				write = result.get(0).getWrite();
				execute = result.get(0).getExecute();
				comment = result.get(0).getComment();
				grant = result.get(0).getGrant();

				for (int i = 0; i < permission.length; i++) {
					switch (permission[i]) {
					case WasabiPermission.VIEW:
						view = allowance[i];
						break;
					case WasabiPermission.READ:
						read = allowance[i];
						break;
					case WasabiPermission.INSERT:
						insert = allowance[i];
						break;
					case WasabiPermission.WRITE:
						write = allowance[i];
						break;
					case WasabiPermission.EXECUTE:
						execute = allowance[i];
						break;
					case WasabiPermission.COMMENT:
						comment = allowance[i];
						break;
					case WasabiPermission.GRANT:
						grant = allowance[i];
						break;
					}
				}

				// case all values are 0, then delete entry
				if (view == 0 && read == 0 && insert == 0 && write == 0 && execute == 0 && comment == 0 && grant == 0) {
					String deleteACLEntryQuery = "DELETE FROM wasabi_rights "
							+ "WHERE `object_id`=? AND `start_time`=? AND `end_time`=? AND `group_id`=? AND `inheritance_id`=''";
					run.update(deleteACLEntryQuery, objectUUID, startTime, endTime, groupUUID);

					// propagate rights to children: if an entry exists -> children have an entry -> simple delete where
					// inheritance_id=object_id
					deleteInheritedGroupRights(objectUUID, groupUUID, startTime, endTime, permission, allowance,
							wasabiType, run);
				}
				// otherwise update entry
				else {
					String updateACLEntryQuery = "UPDATE wasabi_rights SET "
							+ "`view`=?, `read`=?, `comment`=?, `insert`=?, `execute`=?, `write`=?, `grant`=?"
							+ " WHERE `object_id`=? AND `group_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance_id`=''";
					run.update(updateACLEntryQuery, view, read, comment, insert, execute, write, grant, objectUUID,
							groupUUID, startTime, endTime);

					// propagate rights to children: if an entry exists -> children have an entry -> simple update where
					// inheritance_id=object_id
					updateInheritedGroupRights(objectUUID, groupUUID, view, read, comment, execute, insert, write,
							grant, startTime, endTime, wasabiType, run);
				}
			}
			// case entry does not exist: insert a new entry if one of the values is not 0
			else {
				for (int i = 0; i < permission.length; i++) {
					switch (permission[i]) {
					case WasabiPermission.VIEW:
						view = allowance[i];
						break;
					case WasabiPermission.READ:
						read = allowance[i];
						break;
					case WasabiPermission.INSERT:
						insert = allowance[i];
						break;
					case WasabiPermission.WRITE:
						write = allowance[i];
						break;
					case WasabiPermission.EXECUTE:
						execute = allowance[i];
						break;
					case WasabiPermission.COMMENT:
						comment = allowance[i];
						break;
					case WasabiPermission.GRANT:
						grant = allowance[i];
						break;
					}
				}

				if (view != 0 || read != 0 || insert != 0 || write != 0 || execute != 0 || comment != 0 || grant != 0) {
					String insertUserACLEntryQuery = "INSERT INTO wasabi_rights "
							+ "(`object_id`, `user_id`, `parent_id`, `group_id`, "
							+ "`view`, `read`, `insert`, `write`, `execute`, `comment`, `grant`, "
							+ "`start_time`, `end_time`, `priority`, `inheritance_id`, `wasabi_type`)"
							+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

					int prio;
					if ((startTime != 0 || endTime != 0))
						prio = WasabiACLPriority.EXPLICIT_GROUP_TIME_RIGHT;
					else
						prio = WasabiACLPriority.EXPLICIT_GROUP_RIGHT;

					run.update(insertUserACLEntryQuery, objectUUID, "", parentUUID, groupUUID, view, read, insert,
							write, execute, comment, grant, startTime, endTime, prio, "", convertNodeType(wasabiType));

					// propagate rights to children: if no entry exists -> children do not have an entry too -> insert
					// an entry for children
					propagateInheritedGroupRights(objectNode, groupUUID, view, read, comment, execute, insert, write,
							grant, startTime, endTime, objectUUID, wasabiType, run);
				}
			}

		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}

	private static void updateExplicitUserRights(Node objectNode, Node userNode, int[] permission, int[] allowance,
			long startTime, long endTime) throws RepositoryException, UnexpectedInternalProblemException {
		QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());

		String objectUUID = ObjectServiceImpl.getUUID(objectNode);
		String userUUID = ObjectServiceImpl.getUUID(userNode);
		String parentUUID = ObjectServiceImpl.getUUID(ObjectServiceImpl.getEnvironment(objectNode));
		String wasabiType = objectNode.getPrimaryNodeType().getName();

		int view = 0, read = 0, insert = 0, write = 0, execute = 0, comment = 0, grant = 0;

		try {
			// check if an entry exists in the database
			String getUserACLEntryQuery = "SELECT `view`, `read`, `execute`, `comment`, `insert`, `write`, `grant` "
					+ "FROM `wasabi_rights` " + "WHERE `object_id`=? " + "AND `start_time`=? " + "AND `end_time`=? "
					+ "AND `user_id`=? " + "AND `inheritance_id`='' ";

			ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
			List<WasabiACLEntry> result = run.query(getUserACLEntryQuery, h, objectUUID, startTime, endTime, userUUID);

			// case entry exists: get values and update entry with new values
			if (!result.isEmpty()) {
				view = result.get(0).getView();
				read = result.get(0).getRead();
				insert = result.get(0).getInsert();
				write = result.get(0).getWrite();
				execute = result.get(0).getExecute();
				comment = result.get(0).getComment();
				grant = result.get(0).getGrant();

				for (int i = 0; i < permission.length; i++) {
					switch (permission[i]) {
					case WasabiPermission.VIEW:
						view = allowance[i];
						break;
					case WasabiPermission.READ:
						read = allowance[i];
						break;
					case WasabiPermission.INSERT:
						insert = allowance[i];
						break;
					case WasabiPermission.WRITE:
						write = allowance[i];
						break;
					case WasabiPermission.EXECUTE:
						execute = allowance[i];
						break;
					case WasabiPermission.COMMENT:
						comment = allowance[i];
						break;
					case WasabiPermission.GRANT:
						grant = allowance[i];
						break;
					}
				}

				// case all values are 0, then delete entry
				if (view == 0 && read == 0 && insert == 0 && write == 0 && execute == 0 && comment == 0 && grant == 0) {
					String deleteACLEntryQuery = "DELETE FROM wasabi_rights "
							+ "WHERE `object_id`=? AND `start_time`=? AND `end_time`=? AND `user_id`=? AND `inheritance_id`=''";
					run.update(deleteACLEntryQuery, objectUUID, startTime, endTime, userUUID);

					// propagate rights to children: if an entry exists -> children have an entry -> simple delete where
					// inheritance_id=object_id
					deleteInheritedUserRights(objectUUID, userUUID, startTime, endTime, permission, allowance,
							wasabiType, run);
				}
				// otherwise update entry
				else {
					String updateACLEntryQuery = "UPDATE wasabi_rights SET "
							+ "`view`=?, `read`=?, `comment`=?, `insert`=?, `execute`=?, `write`=?, `grant`=?"
							+ " WHERE `object_id`=? AND `user_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance_id`=''";
					run.update(updateACLEntryQuery, view, read, comment, insert, execute, write, grant, objectUUID,
							userUUID, startTime, endTime);

					// propagate rights to children: if an entry exists -> children have an entry -> simple update where
					// inheritance_id=object_id
					updateInheritedUserRights(objectUUID, userUUID, view, read, comment, execute, insert, write, grant,
							startTime, endTime, permission, allowance, wasabiType, run);
				}
			}
			// case entry does not exist: insert a new entry if one of the values is not 0
			else {
				for (int i = 0; i < permission.length; i++) {
					switch (permission[i]) {
					case WasabiPermission.VIEW:
						view = allowance[i];
						break;
					case WasabiPermission.READ:
						read = allowance[i];
						break;
					case WasabiPermission.INSERT:
						insert = allowance[i];
						break;
					case WasabiPermission.WRITE:
						write = allowance[i];
						break;
					case WasabiPermission.EXECUTE:
						execute = allowance[i];
						break;
					case WasabiPermission.COMMENT:
						comment = allowance[i];
						break;
					case WasabiPermission.GRANT:
						grant = allowance[i];
						break;
					}
				}

				if (view != 0 || read != 0 || insert != 0 || write != 0 || execute != 0 || comment != 0 || grant != 0) {
					String insertUserACLEntryQuery = "INSERT INTO wasabi_rights "
							+ "(`object_id`, `user_id`, `parent_id`, `group_id`, "
							+ "`view`, `read`, `insert`, `write`, `execute`, `comment`, `grant`, "
							+ "`start_time`, `end_time`, `priority`, `inheritance_id`, `wasabi_type`)"
							+ " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

					int prio;
					if ((startTime != 0 || endTime != 0))
						prio = WasabiACLPriority.EXPLICIT_USER_TIME_RIGHT;
					else
						prio = WasabiACLPriority.EXPLICIT_USER_RIGHT;

					run.update(insertUserACLEntryQuery, objectUUID, userUUID, parentUUID, "", view, read, insert,
							write, execute, comment, grant, startTime, endTime, prio, "", convertNodeType(wasabiType));

					// propagate rights to children: if no entry exists -> children do not have an entry too -> insert
					// an entry for children
					propagateInheritedUserRights(objectNode, userUUID, view, read, comment, execute, insert, write,
							grant, startTime, endTime, objectUUID, wasabiType, run);
				}
			}

		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}

	private static void updateInheritedGroupRights(String parentUUID, String groupUUID, int view, int read,
			int comment, int execute, int insert, int write, int grant, long startTime, long endTime,
			String wasabiType, QueryRunner run) throws UnexpectedInternalProblemException {
		try {
			String updateUserACLEntryQueryView = "UPDATE wasabi_rights SET "
					+ "`view`=?, `read`=?, `comment`=?, `insert`=?, `execute`=?, `write`=?, `grant`=?"
					+ " WHERE `group_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance_id`=?";
			run.update(updateUserACLEntryQueryView, view, read, comment, insert, execute, write, grant, groupUUID,
					startTime, endTime, parentUUID);
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}

	// private static void updateInheritedRights(Node objectNode, Node identityNode, int[] permission, int[] allowance,
	// long startTime, long endTime, Session s) throws UnexpectedInternalProblemException, RepositoryException {
	//
	// // reset certificate for actual node
	// if (WasabiConstants.ACL_CERTIFICATE_ENABLE)
	// WasabiCertificate.invalidateCertificate(objectNode, identityNode, permission, allowance);
	//
	// QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());
	//
	// String objectUUID = ObjectServiceImpl.getUUID(objectNode);
	// String wasabiIdentityType = identityNode.getPrimaryNodeType().getName();
	// String identityUUID = ObjectServiceImpl.getUUID(identityNode);
	// String parentUUID = ObjectServiceImpl.getUUID(ObjectServiceImpl.getEnvironment(objectNode));
	//
	// int view = 0, read = 0, insert = 0, write = 0, execute = 0, comment = 0, grant = 0;
	//
	// if (wasabiIdentityType.equals(WasabiNodeType.USER)) {
	// try {
	// String getUserACLEntryQuery = "SELECT * FROM wasabi_rights "
	// + "WHERE `object_id`=? AND `start_time`=? AND `end_time`=? AND `user_id`=? AND `inheritance_id`=?";
	// ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result = run.query(getUserACLEntryQuery, h, objectUUID, startTime, endTime,
	// identityUUID, "");
	//
	// if (!result.isEmpty()) {
	// view = result.get(0).getView();
	// read = result.get(0).getRead();
	// insert = result.get(0).getInsert();
	// write = result.get(0).getWrite();
	// execute = result.get(0).getExecute();
	// comment = result.get(0).getComment();
	// grant = result.get(0).getGrant();
	//
	// for (int i = 0; i < permission.length; i++) {
	// switch (permission[i]) {
	// case WasabiPermission.VIEW:
	// view = allowance[i];
	// break;
	// case WasabiPermission.READ:
	// read = allowance[i];
	// break;
	// case WasabiPermission.INSERT:
	// insert = allowance[i];
	// break;
	// case WasabiPermission.WRITE:
	// write = allowance[i];
	// break;
	// case WasabiPermission.EXECUTE:
	// execute = allowance[i];
	// break;
	// case WasabiPermission.COMMENT:
	// comment = allowance[i];
	// break;
	// case WasabiPermission.GRANT:
	// grant = allowance[i];
	// break;
	// }
	// }
	// if (view == 0 && read == 0 && insert == 0 && write == 0 && execute == 0 && comment == 0
	// && grant == 0) {
	// String deleteACLEntryQuery = "DELETE FROM wasabi_rights "
	// + "WHERE `start_time`=? AND `end_time`=? AND `user_id`=? AND `inheritance_id`=?";
	// run.update(deleteACLEntryQuery, startTime, endTime, identityUUID, objectUUID);
	// } else {
	// String updateUserACLEntryQuery = "UPDATE wasabi_rights SET "
	// + "`view`=?, `read`=?, `insert`=?, `write`=?, `execute`=?, `comment`=?, `grant`=?"
	// + " WHERE `user_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance_id`=?";
	// run.update(updateUserACLEntryQuery, view, read, insert, write, execute, comment, grant,
	// identityUUID, startTime, endTime, objectUUID);
	// }
	//
	// } else {
	// for (int i = 0; i < permission.length; i++) {
	// switch (permission[i]) {
	// case WasabiPermission.VIEW:
	// view = allowance[i];
	// break;
	// case WasabiPermission.READ:
	// read = allowance[i];
	// break;
	// case WasabiPermission.INSERT:
	// insert = allowance[i];
	// break;
	// case WasabiPermission.WRITE:
	// write = allowance[i];
	// break;
	// case WasabiPermission.EXECUTE:
	// execute = allowance[i];
	// break;
	// case WasabiPermission.COMMENT:
	// comment = allowance[i];
	// break;
	// case WasabiPermission.GRANT:
	// grant = allowance[i];
	// break;
	// }
	// }
	// createInheritanceEntries(identityNode, permission, allowance, objectUUID, s, WasabiType.USER, run,
	// identityUUID, objectUUID, view, read, comment, execute, insert, write, grant, startTime,
	// endTime, objectUUID);
	// }
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// } else if (wasabiIdentityType.equals(WasabiNodeType.GROUP)) {
	// try {
	// String getUserACLEntryQuery = "SELECT * FROM wasabi_rights "
	// + "WHERE `object_id`=? AND `start_time`=? AND `end_time`=? AND `group_id`=? AND `inheritance_id`=?";
	// ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result = run.query(getUserACLEntryQuery, h, objectUUID, startTime, endTime,
	// identityUUID, "");
	//
	// if (!result.isEmpty()) {
	// view = result.get(0).getView();
	// read = result.get(0).getRead();
	// insert = result.get(0).getInsert();
	// write = result.get(0).getWrite();
	// execute = result.get(0).getExecute();
	// comment = result.get(0).getComment();
	// grant = result.get(0).getGrant();
	//
	// for (int i = 0; i < permission.length; i++) {
	// switch (permission[i]) {
	// case WasabiPermission.VIEW:
	// view = allowance[i];
	// break;
	// case WasabiPermission.READ:
	// read = allowance[i];
	// break;
	// case WasabiPermission.INSERT:
	// insert = allowance[i];
	// break;
	// case WasabiPermission.WRITE:
	// write = allowance[i];
	// break;
	// case WasabiPermission.EXECUTE:
	// execute = allowance[i];
	// break;
	// case WasabiPermission.COMMENT:
	// comment = allowance[i];
	// break;
	// case WasabiPermission.GRANT:
	// grant = allowance[i];
	// break;
	// }
	// }
	// if (view == 0 && read == 0 && insert == 0 && write == 0 && execute == 0 && comment == 0
	// && grant == 0) {
	// String deleteACLEntryQuery = "DELETE FROM wasabi_rights "
	// + "WHERE `start_time`=? AND `end_time`=? AND `group_id`=? AND `inheritance_id`=?";
	// run.update(deleteACLEntryQuery, startTime, endTime, identityUUID, objectUUID);
	// } else {
	// String updateUserACLEntryQuery = "UPDATE wasabi_rights SET "
	// + "`parent_id`=?, `view`=?, `read`=?, `insert`=?, `write`=?, `execute`=?, `comment`=?, `grant`=?"
	// + " WHERE `group_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance_id`=?";
	// run.update(updateUserACLEntryQuery, parentUUID, view, read, insert, write, execute, comment,
	// grant, identityUUID, startTime, endTime, objectUUID);
	// }
	//
	// } else {
	// for (int i = 0; i < permission.length; i++) {
	// switch (permission[i]) {
	// case WasabiPermission.VIEW:
	// view = allowance[i];
	// break;
	// case WasabiPermission.READ:
	// read = allowance[i];
	// break;
	// case WasabiPermission.INSERT:
	// insert = allowance[i];
	// break;
	// case WasabiPermission.WRITE:
	// write = allowance[i];
	// break;
	// case WasabiPermission.EXECUTE:
	// execute = allowance[i];
	// break;
	// case WasabiPermission.COMMENT:
	// comment = allowance[i];
	// break;
	// case WasabiPermission.GRANT:
	// grant = allowance[i];
	// break;
	// }
	// }
	// createInheritanceEntries(identityNode, permission, allowance, objectUUID, s, WasabiType.GROUP, run,
	// identityUUID, objectUUID, view, read, comment, execute, insert, write, grant, startTime,
	// endTime, objectUUID);
	// }
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// }
	// }

	private static void updateInheritedUserRights(String parentUUID, String userUUID, int view, int read, int comment,
			int execute, int insert, int write, int grant, long startTime, long endTime, int[] permission,
			int[] allowance, String wasabiType, QueryRunner run) throws UnexpectedInternalProblemException {
		try {
			String updateUserACLEntryQueryView = "UPDATE wasabi_rights SET "
					+ "`view`=?, `read`=?, `comment`=?, `insert`=?, `execute`=?, `write`=?, `grant`=?"
					+ " WHERE `user_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance_id`=?";
			run.update(updateUserACLEntryQueryView, view, read, comment, insert, execute, write, grant, userUUID,
					startTime, endTime, parentUUID);
		} catch (SQLException e) {
			throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
		}
	}

	// private static void updateRights(Node objectNode, Node identityNode, int[] permission, int[] allowance,
	// long startTime, long endTime, String inheritance_id) throws RepositoryException,
	// UnexpectedInternalProblemException {
	//
	// // reset certificate for actual node
	// if (WasabiConstants.ACL_CERTIFICATE_ENABLE)
	// WasabiCertificate.invalidateCertificate(objectNode, identityNode, permission, allowance);
	//
	// QueryRunner run = new QueryRunner(new SqlConnector().getDataSource());
	//
	// String objectUUID = ObjectServiceImpl.getUUID(objectNode);
	// String wasabiIdentityType = identityNode.getPrimaryNodeType().getName();
	// String identityUUID = ObjectServiceImpl.getUUID(identityNode);
	// String parentUUID = ObjectServiceImpl.getUUID(ObjectServiceImpl.getEnvironment(objectNode));
	// String wasabiType = objectNode.getPrimaryNodeType().getName();
	//
	// int view, read, insert, write, execute, comment, grant;
	//
	// if (wasabiIdentityType.equals(WasabiNodeType.USER)) {
	// try {
	// String getUserACLEntryQuery = "SELECT `view`, `read`, `execute`, `comment`, `insert`, `write`, `grant` "
	// + "FROM wasabi_rights "
	// + "WHERE `object_id`=? "
	// + "AND `start_time`=? "
	// + "AND `end_time`=? "
	// + "AND `user_id`=? " + "AND `inheritance_id`=?";
	//
	// ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result = run.query(getUserACLEntryQuery, h, objectUUID, startTime, endTime,
	// identityUUID, inheritance_id);
	//
	// if (result.isEmpty()) {
	// view = 0;
	// read = 0;
	// insert = 0;
	// write = 0;
	// execute = 0;
	// comment = 0;
	// grant = 0;
	//
	// for (int i = 0; i < permission.length; i++) {
	// switch (permission[i]) {
	// case WasabiPermission.VIEW:
	// view = allowance[i];
	// break;
	// case WasabiPermission.READ:
	// read = allowance[i];
	// break;
	// case WasabiPermission.INSERT:
	// insert = allowance[i];
	// break;
	// case WasabiPermission.WRITE:
	// write = allowance[i];
	// break;
	// case WasabiPermission.EXECUTE:
	// execute = allowance[i];
	// break;
	// case WasabiPermission.COMMENT:
	// comment = allowance[i];
	// break;
	// case WasabiPermission.GRANT:
	// grant = allowance[i];
	// break;
	// }
	// }
	//
	// if (view != 0 || read != 0 || insert != 0 || write != 0 || execute != 0 || comment != 0
	// || grant != 0) {
	// String insertUserACLEntryQuery = "INSERT INTO wasabi_rights "
	// +
	// "(`object_id`, `user_id`, `parent_id`, `group_id` , `view`, `read`, `insert`, `write`, `execute`, `comment`, `grant`, `start_time`, `end_time`, `inheritance_id`, `priority`, `wasabi_type`)"
	// + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	//
	// int prio;
	// if ((startTime != 0 || endTime != 0) && inheritance_id.isEmpty())
	// prio = WasabiACLPriority.EXPLICIT_USER_TIME_RIGHT;
	// else if ((startTime != 0 || endTime != 0) && !inheritance_id.isEmpty())
	// prio = WasabiACLPriority.INHERITED_USER_TIME_RIGHT;
	// else if (startTime == 0 && endTime == 0 && inheritance_id.isEmpty())
	// prio = WasabiACLPriority.EXPLICIT_USER_RIGHT;
	// else
	// prio = WasabiACLPriority.INHERITED_USER_RIGHT;
	//
	// run.update(insertUserACLEntryQuery, objectUUID, identityUUID, parentUUID, "", view, read,
	// insert, write, execute, comment, grant, startTime, endTime, inheritance_id, prio,
	// convertNodeType(wasabiType));
	// }
	// } else {
	// view = result.get(0).getView();
	// read = result.get(0).getRead();
	// insert = result.get(0).getInsert();
	// write = result.get(0).getWrite();
	// execute = result.get(0).getExecute();
	// comment = result.get(0).getComment();
	// grant = result.get(0).getGrant();
	//
	// for (int i = 0; i < permission.length; i++) {
	// switch (permission[i]) {
	// case WasabiPermission.VIEW:
	// view = allowance[i];
	// break;
	// case WasabiPermission.READ:
	// read = allowance[i];
	// break;
	// case WasabiPermission.INSERT:
	// insert = allowance[i];
	// break;
	// case WasabiPermission.WRITE:
	// write = allowance[i];
	// break;
	// case WasabiPermission.EXECUTE:
	// execute = allowance[i];
	// break;
	// case WasabiPermission.COMMENT:
	// comment = allowance[i];
	// break;
	// case WasabiPermission.GRANT:
	// grant = allowance[i];
	// break;
	// }
	//
	// if (view == 0 && read == 0 && insert == 0 && write == 0 && execute == 0 && comment == 0
	// && grant == 0) {
	// String deleteACLEntryQuery = "DELETE FROM wasabi_rights "
	// + "WHERE `object_id`=? AND `start_time`=? AND `end_time`=? AND `user_id`=? AND `inheritance_id`=?";
	// run.update(deleteACLEntryQuery, objectUUID, startTime, endTime, identityUUID,
	// inheritance_id);
	// } else {
	// for (int j = 0; j < permission.length; j++) {
	// switch (permission[j]) {
	// case WasabiPermission.VIEW:
	// String updateUserACLEntryQueryView = "UPDATE wasabi_rights SET "
	// + "`parent_id`=?, `view`=?"
	// + " WHERE `object_id`=? AND `user_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance_id`=?";
	// run.update(updateUserACLEntryQueryView, parentUUID, view, objectUUID, identityUUID,
	// startTime, endTime, inheritance_id);
	// break;
	// case WasabiPermission.READ:
	// String updateUserACLEntryQueryRead = "UPDATE wasabi_rights SET "
	// + "`parent_id`=?, `read`=?"
	// + " WHERE `object_id`=? AND `user_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance_id`=?";
	// run.update(updateUserACLEntryQueryRead, parentUUID, read, objectUUID, identityUUID,
	// startTime, endTime, inheritance_id);
	// break;
	// case WasabiPermission.INSERT:
	// String updateUserACLEntryQueryInsert = "UPDATE wasabi_rights SET "
	// + "`parent_id`=?, `insert`=?"
	// + " WHERE `object_id`=? AND `user_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance_id`=?";
	// run.update(updateUserACLEntryQueryInsert, parentUUID, insert, objectUUID,
	// identityUUID, startTime, endTime, inheritance_id);
	// break;
	// case WasabiPermission.WRITE:
	// String updateUserACLEntryQueryWrite = "UPDATE wasabi_rights SET "
	// + "`parent_id`=?, `write`=?"
	// + " WHERE `object_id`=? AND `user_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance_id`=?";
	// run.update(updateUserACLEntryQueryWrite, parentUUID, write, objectUUID,
	// identityUUID, startTime, endTime, inheritance_id);
	// break;
	// case WasabiPermission.EXECUTE:
	// String updateUserACLEntryQueryExecute = "UPDATE wasabi_rights SET "
	// + "`parent_id`=?, `execute`=?"
	// + " WHERE `object_id`=? AND `user_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance_id`=?";
	// run.update(updateUserACLEntryQueryExecute, parentUUID, execute, objectUUID,
	// identityUUID, startTime, endTime, inheritance_id);
	// break;
	// case WasabiPermission.COMMENT:
	// String updateUserACLEntryQueryComment = "UPDATE wasabi_rights SET "
	// + "`parent_id`=?, `comment`=?"
	// + " WHERE `object_id`=? AND `user_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance_id`=?";
	// run.update(updateUserACLEntryQueryComment, parentUUID, comment, objectUUID,
	// identityUUID, startTime, endTime, inheritance_id);
	// break;
	// case WasabiPermission.GRANT:
	// String updateUserACLEntryQueryGrant = "UPDATE wasabi_rights SET "
	// + "`parent_id`=?, `grant`=?"
	// + " WHERE `object_id`=? AND `user_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance_id`=?";
	// run.update(updateUserACLEntryQueryGrant, parentUUID, grant, objectUUID,
	// identityUUID, startTime, endTime, inheritance_id);
	// break;
	// }
	// }
	// }
	// }
	// }
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// } else if (wasabiIdentityType.equals(WasabiNodeType.GROUP)) {
	// try {
	// String getUserACLEntryQuery = "SELECT `view`, `read`, `execute`, `comment`, `insert`, `write`, `grant` "
	// + "FROM wasabi_rights "
	// + "WHERE `object_id`=? "
	// + "AND `start_time`=? "
	// + "AND `end_time`=? "
	// + "AND `group_id`=? " + "AND `inheritance_id`=?";
	//
	// ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
	// List<WasabiACLEntry> result = run.query(getUserACLEntryQuery, h, objectUUID, startTime, endTime,
	// identityUUID, inheritance_id);
	//
	// if (result.isEmpty()) {
	// view = 0;
	// read = 0;
	// insert = 0;
	// write = 0;
	// execute = 0;
	// comment = 0;
	// grant = 0;
	//
	// for (int i = 0; i < permission.length; i++) {
	// switch (permission[i]) {
	// case WasabiPermission.VIEW:
	// view = allowance[i];
	// break;
	// case WasabiPermission.READ:
	// read = allowance[i];
	// break;
	// case WasabiPermission.INSERT:
	// insert = allowance[i];
	// break;
	// case WasabiPermission.WRITE:
	// write = allowance[i];
	// break;
	// case WasabiPermission.EXECUTE:
	// execute = allowance[i];
	// break;
	// case WasabiPermission.COMMENT:
	// comment = allowance[i];
	// break;
	// case WasabiPermission.GRANT:
	// grant = allowance[i];
	// break;
	// }
	// }
	//
	// if (view != 0 || read != 0 || insert != 0 || write != 0 || execute != 0 || comment != 0
	// || grant != 0) {
	// String insertUserACLEntryQuery = "INSERT INTO wasabi_rights "
	// +
	// "(`object_id`, `user_id`, `parent_id`, `group_id` , `view`, `read`, `insert`, `write`, `execute`, `comment`, `grant`, `start_time`, `end_time`, `inheritance_id`, `priority`, `wasabi_type`)"
	// + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	//
	// int prio;
	// if ((startTime != 0 || endTime != 0) && inheritance_id.isEmpty())
	// prio = WasabiACLPriority.EXPLICIT_GROUP_TIME_RIGHT;
	// else if ((startTime != 0 || endTime != 0) && !inheritance_id.isEmpty())
	// prio = WasabiACLPriority.INHERITED_GROUP_TIME_RIGHT;
	// else if (startTime == 0 && endTime == 0 && inheritance_id.isEmpty())
	// prio = WasabiACLPriority.EXPLICIT_GROUP_RIGHT;
	// else
	// prio = WasabiACLPriority.INHERITED_GROUP_RIGHT;
	//
	// run.update(insertUserACLEntryQuery, objectUUID, "", parentUUID, identityUUID, view, read,
	// insert, write, execute, comment, grant, startTime, endTime, inheritance_id, prio,
	// convertNodeType(wasabiType));
	// }
	// } else {
	// view = result.get(0).getView();
	// read = result.get(0).getRead();
	// insert = result.get(0).getInsert();
	// write = result.get(0).getWrite();
	// execute = result.get(0).getExecute();
	// comment = result.get(0).getComment();
	// grant = result.get(0).getGrant();
	//
	// for (int i = 0; i < permission.length; i++) {
	// switch (permission[i]) {
	// case WasabiPermission.VIEW:
	// view = allowance[i];
	// break;
	// case WasabiPermission.READ:
	// read = allowance[i];
	// break;
	// case WasabiPermission.INSERT:
	// insert = allowance[i];
	// break;
	// case WasabiPermission.WRITE:
	// write = allowance[i];
	// break;
	// case WasabiPermission.EXECUTE:
	// execute = allowance[i];
	// break;
	// case WasabiPermission.COMMENT:
	// comment = allowance[i];
	// break;
	// case WasabiPermission.GRANT:
	// grant = allowance[i];
	// break;
	// }
	//
	// if (view == 0 && read == 0 && insert == 0 && write == 0 && execute == 0 && comment == 0
	// && grant == 0) {
	// String deleteACLEntryQuery = "DELETE FROM wasabi_rights "
	// + "WHERE `object_id`=? AND `start_time`=? AND `end_time`=? AND `group_id`=? AND `inheritance_id`=?";
	// run.update(deleteACLEntryQuery, objectUUID, startTime, endTime, identityUUID,
	// inheritance_id);
	// } else {
	// for (int j = 0; j < permission.length; j++) {
	// switch (permission[j]) {
	// case WasabiPermission.VIEW:
	// String updateUserACLEntryQueryView = "UPDATE wasabi_rights SET "
	// + "`parent_id`=?, `view`=?"
	// + " WHERE `object_id`=? AND `group_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance_id`=?";
	// run.update(updateUserACLEntryQueryView, parentUUID, view, objectUUID, identityUUID,
	// startTime, endTime, inheritance_id);
	// break;
	// case WasabiPermission.READ:
	// String updateUserACLEntryQueryRead = "UPDATE wasabi_rights SET "
	// + "`parent_id`=?, `read`=?"
	// + " WHERE `object_id`=? AND `group_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance_id`=?";
	// run.update(updateUserACLEntryQueryRead, parentUUID, read, objectUUID, identityUUID,
	// startTime, endTime, inheritance_id);
	// break;
	// case WasabiPermission.INSERT:
	// String updateUserACLEntryQueryInsert = "UPDATE wasabi_rights SET "
	// + "`parent_id`=?, `insert`=?"
	// + " WHERE `object_id`=? AND `group_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance_id`=?";
	// run.update(updateUserACLEntryQueryInsert, parentUUID, insert, objectUUID,
	// identityUUID, startTime, endTime, inheritance_id);
	// break;
	// case WasabiPermission.WRITE:
	// String updateUserACLEntryQueryWrite = "UPDATE wasabi_rights SET "
	// + "`parent_id`=?, `write`=?"
	// + " WHERE `object_id`=? AND `group_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance_id`=?";
	// run.update(updateUserACLEntryQueryWrite, parentUUID, write, objectUUID,
	// identityUUID, startTime, endTime, inheritance_id);
	// break;
	// case WasabiPermission.EXECUTE:
	// String updateUserACLEntryQueryExecute = "UPDATE wasabi_rights SET "
	// + "`parent_id`=?, `execute`=?"
	// + " WHERE `object_id`=? AND `group_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance_id`=?";
	// run.update(updateUserACLEntryQueryExecute, parentUUID, execute, objectUUID,
	// identityUUID, startTime, endTime, inheritance_id);
	// break;
	// case WasabiPermission.COMMENT:
	// String updateUserACLEntryQueryComment = "UPDATE wasabi_rights SET "
	// + "`parent_id`=?, `comment`=?"
	// + " WHERE `object_id`=? AND `group_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance_id`=?";
	// run.update(updateUserACLEntryQueryComment, parentUUID, comment, objectUUID,
	// identityUUID, startTime, endTime, inheritance_id);
	// break;
	// case WasabiPermission.GRANT:
	// String updateUserACLEntryQueryGrant = "UPDATE wasabi_rights SET "
	// + "`parent_id`=?, `grant`=?"
	// + " WHERE `object_id`=? AND `group_id`=? AND `start_time`=? AND `end_time`=? AND `inheritance_id`=?";
	// run.update(updateUserACLEntryQueryGrant, parentUUID, grant, objectUUID,
	// identityUUID, startTime, endTime, inheritance_id);
	// break;
	// }
	// }
	// }
	// }
	// }
	// } catch (SQLException e) {
	// throw new UnexpectedInternalProblemException(WasabiExceptionMessages.DB_FAILURE, e);
	// }
	// }
	// }
}
