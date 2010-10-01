package de.wasabibeans.framework.server.core.local;

import java.util.Vector;

import javax.ejb.Local;

import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.dto.WasabiVersionDTO;
import de.wasabibeans.framework.server.core.exception.ConcurrentModificationException;
import de.wasabibeans.framework.server.core.exception.ObjectDoesNotExistException;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;

@Local
public interface VersioningServiceLocal {

	public Vector<WasabiVersionDTO> getVersions(WasabiObjectDTO dto) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException;

	public void createVersion(WasabiObjectDTO dto, String comment) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ConcurrentModificationException;

	public void restoreVersion(WasabiObjectDTO dto, String versionLabel) throws UnexpectedInternalProblemException,
			ObjectDoesNotExistException, ConcurrentModificationException;
}
