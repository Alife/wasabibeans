package de.wasabibeans.framework.server.core.test.testhelper;

import javax.ejb.Remote;

@Remote
public interface JCRTestBeanRemote {
	
	public void sessionLoginLogout(String username) throws Exception;
	public void printSubNodesOfRoot(String username) throws Exception;
}
