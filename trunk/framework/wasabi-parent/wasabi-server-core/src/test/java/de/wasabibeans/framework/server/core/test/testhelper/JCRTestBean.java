package de.wasabibeans.framework.server.core.test.testhelper;

import javax.ejb.Stateless;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.naming.InitialContext;

import de.wasabibeans.framework.server.core.common.WasabiConstants;

@Stateless
public class JCRTestBean implements JCRTestBeanLocal, JCRTestBeanRemote {

	public void sessionLoginLogout(String username) throws Exception {
		String jcrUsername = username;
		System.out.println("Thread used for sessionLoginLogout: " + Thread.currentThread().getId());

		InitialContext jndiContext = new InitialContext();
		Repository rep = (Repository) jndiContext.lookup(WasabiConstants.JNDI_JCR_DATASOURCE + "/local");
		Session s = rep.login(new SimpleCredentials(jcrUsername, jcrUsername.toCharArray()));
		System.out.println(s.toString());
		s.getRootNode().addNode("huhu");
		s.save();

		throw new RuntimeException("Look behind you, a three-headed monkey!"); // make the container managed transaction
																				// fail and rollback
	}

	public void printSubNodesOfRoot(String username) throws Exception {
		String jcrUsername = username;
		System.out.println("Thread used for printSubNodesOfRoot: " + Thread.currentThread().getId());
		
		InitialContext jndiContext = new InitialContext();
		Repository rep = (Repository) jndiContext.lookup(WasabiConstants.JNDI_JCR_DATASOURCE + "/local");
		Session s = rep.login(new SimpleCredentials(jcrUsername, jcrUsername.toCharArray()));
		NodeIterator ni = s.getRootNode().getNodes();
		while (ni.hasNext()) {
			System.out.println(ni.nextNode().getName());
		}
	}
}
