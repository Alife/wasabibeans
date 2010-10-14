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

import java.sql.SQLException;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.jboss.ejb3.annotation.Service;

import de.wasabibeans.framework.server.core.authorization.WasabiCertificate;
import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.common.WasabiPermission;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

@Service
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ACLTimeEntryCleaner implements ACLTimeEntryCleanerLocal {

	private static WasabiLogger logger = WasabiLogger.getLogger(ACLTimeEntryCleaner.class);

	@Resource
	private SessionContext ctx;

	@Timeout
	public void check(Timer timer) throws UnexpectedInternalProblemException {
		SqlConnector sqlConnector = new SqlConnector();
		QueryRunner run = new QueryRunner(sqlConnector.getDataSource());

		try {
			long time = java.lang.System.currentTimeMillis();
			String cleanupACLTimeEntries = "DELETE FROM `wasabi_rights` " + "WHERE `end_time`<? AND `end_time`!=0";

			certificateInvalidation(run, time);

			run.update(cleanupACLTimeEntries, time);
			logger.info("ACL time entry cleanup at " + time);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			sqlConnector.close();
		}
	}

	public void startACLTimeEntryCleaner() {
		ctx.getTimerService().createTimer(WasabiConstants.ACL_TIME_ENTRY_CLEANUP * 60 * 1000,
				WasabiConstants.ACL_TIME_ENTRY_CLEANUP * 60 * 1000, null);
	}

	private void certificateInvalidation(QueryRunner run, long time) throws SQLException,
			UnexpectedInternalProblemException {
		String getACLTimeEntries = "SELECT * FROM `wasabi_rights` WHERE " + "`end_time`<? AND `end_time`!=0";

		ResultSetHandler<List<WasabiACLEntry>> h = new BeanListHandler<WasabiACLEntry>(WasabiACLEntry.class);
		List<WasabiACLEntry> result = run.query(getACLTimeEntries, h, time);

		for (WasabiACLEntry wasabiACLEntry : result) {
			String objectUUID = wasabiACLEntry.getObject_Id();
			String userUUID = wasabiACLEntry.getUser_Id();
			String groupUUID = wasabiACLEntry.getGroup_Id();

			for (int i = 0; i < 7; i++) {
				int permission = getRight(wasabiACLEntry, i);
				if (permission == 1) {
					if (userUUID.length() > 0)
						WasabiCertificate.invalidateCertificateForUser(objectUUID, userUUID, permission);
					else
						WasabiCertificate.invalidateCertificateForGroup(objectUUID, groupUUID, permission);
				}
			}
		}
	}

	/**
	 * Returns right of a given ACL entry by given permission.
	 * 
	 * @param wasabiACLEntry
	 * @param permission
	 * @return right of a given ACL entry by given permission
	 */
	private static int getRight(WasabiACLEntry wasabiACLEntry, int permission) {
		switch (permission) {
		case WasabiPermission.VIEW:
			return wasabiACLEntry.getView();
		case WasabiPermission.READ:
			return wasabiACLEntry.getRead();
		case WasabiPermission.EXECUTE:
			return wasabiACLEntry.getExecute();
		case WasabiPermission.COMMENT:
			return wasabiACLEntry.getComment();
		case WasabiPermission.INSERT:
			return wasabiACLEntry.getInsert();
		case WasabiPermission.WRITE:
			return wasabiACLEntry.getWrite();
		case WasabiPermission.GRANT:
			return wasabiACLEntry.getGrant();
		}
		return 0;
	}
}
