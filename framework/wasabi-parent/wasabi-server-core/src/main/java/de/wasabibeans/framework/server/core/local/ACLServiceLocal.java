package de.wasabibeans.framework.server.core.local;

import java.util.Collection;

import javax.ejb.Local;

import de.wasabibeans.framework.server.core.dto.WasabiACLEntryDTO;
import de.wasabibeans.framework.server.core.dto.WasabiIdentityDTO;
import de.wasabibeans.framework.server.core.dto.WasabiLocationDTO;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;

@Local
public interface ACLServiceLocal {

	public void activateInheritance(WasabiObjectDTO wasabiObject);

	public void create(WasabiObjectDTO wasabiObject,
			WasabiIdentityDTO wasabiIdentity, int permission, boolean allowance);

	public void create(WasabiObjectDTO wasabiObject,
			WasabiIdentityDTO wasabiIdentity, int permission,
			boolean allowance, long startTime, long endTime);

	public void remove(WasabiObjectDTO wasabiObject,
			WasabiIdentityDTO wasabiIdentity, int permission);

	public void remove(WasabiObjectDTO wasabiObject,
			WasabiIdentityDTO wasabiIdentity, int permission, long startTime,
			long endTime);

	public void remove(WasabiObjectDTO wasabiObject,
			WasabiIdentityDTO wasabiIdentity, int[] permission);

	public void remove(WasabiObjectDTO wasabiObject,
			WasabiIdentityDTO wasabiIdentity, int[] permission,
			long[] startTime, long[] endTime);

	public void deactivateInheritance(WasabiObjectDTO wasabiObject);

	public void reset(WasabiObjectDTO wasabiObject);

	public void showAllACLEntries(WasabiObjectDTO wasabiObject);

	public int getPermission(WasabiACLEntryDTO wasabiACLEntry);

	public WasabiIdentityDTO getIdentity(WasabiACLEntryDTO wasabiACLEntry);

	public boolean isAllowance(WasabiACLEntryDTO wasabiACLEntry);

	public boolean isExplicitRight(WasabiACLEntryDTO wasabiACLEntry);

	public void createDefault(WasabiLocationDTO wasabiLocation,
			WasabiIdentityDTO wasabiIdentity, int[] permission,
			boolean[] allowance);

	public void removeDefault(WasabiLocationDTO wasabiLocation,
			WasabiIdentityDTO wasabiIdentity, int[] permission);

	public void create(WasabiObjectDTO wasabiObject,
			WasabiIdentityDTO wasabiIdentity, int[] permission,
			boolean[] allowance);

	public void create(WasabiObjectDTO wasabiObject,
			WasabiIdentityDTO wasabiIdentity, int[] permission,
			boolean[] allowance, long[] startTime, long[] endTime);

	public boolean isInheritanceAllowed(WasabiObjectDTO object);

	public Collection<WasabiACLEntryDTO> getACLEntries(
			WasabiObjectDTO wasabiObject);
	
	public Collection<WasabiACLEntryDTO> getACLEntriesByIdentity(
			WasabiObjectDTO wasabiObject, WasabiIdentityDTO wasabiIdentity);
}