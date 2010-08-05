package de.wasabibeans.framework.server.core.test.testhelper;

import javax.ejb.Local;

@Local
public interface JCRTestBeanLocal {
	
	public void sessionLoginLogout(String username) throws Exception;
	
	public String createNode(String username) throws Exception;
	
	public void alterProperty(String id, String newValue, String username) throws Exception;
	
	public String getProperty(String id, String username) throws Exception;
}
