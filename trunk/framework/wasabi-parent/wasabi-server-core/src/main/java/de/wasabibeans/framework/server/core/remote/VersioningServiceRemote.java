package de.wasabibeans.framework.server.core.remote;

import java.util.Vector;

import javax.ejb.Remote;

import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiVersionDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.NoPermissionException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

@Remote
public interface VersioningServiceRemote {

	public void createVersion(WasabiObjectDTO object, String comment) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ConcurrentModificationException, NoPermissionException;

	public Vector<WasabiVersionDTO> getVersions(WasabiObjectDTO object) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, NoPermissionException;

	public void restoreVersion(WasabiObjectDTO object, String versionLabel) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ConcurrentModificationException, NoPermissionException;
}
