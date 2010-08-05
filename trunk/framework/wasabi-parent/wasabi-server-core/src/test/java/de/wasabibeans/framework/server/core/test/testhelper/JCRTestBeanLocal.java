package de.wasabibeans.framework.server.core.test.testhelper;

import javax.ejb.Local;

@Local
public interface JCRTestBeanLocal {
	
	public void sessionLoginLogout(String username) throws Exception;
}
