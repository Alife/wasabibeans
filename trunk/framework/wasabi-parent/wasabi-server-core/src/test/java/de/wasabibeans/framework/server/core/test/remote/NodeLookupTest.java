package de.wasabibeans.framework.server.core.test.remote;

import java.util.Vector;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Run(RunModeType.AS_CLIENT)
public class NodeLookupTest extends WasabiRemoteTest {
	
	@BeforeMethod
	public void setUpBeforeEachMethod() throws Exception {
		// initialize jcr repository
		testhelper.initWorkspace("default");
	}
	
	@Test
	public void idLookup() throws Exception {
		Vector<String> nodeIds = testhelper.createManyNodes(10000);
		Vector<String> result = testhelper.getManyNodesByIdLookup(nodeIds);
		System.out.println(result.size());
		System.out.println(result.lastElement());
	
	}
	
	@Test
	public void idFilter() throws Exception {
		Vector<String> nodeIds = testhelper.createManyNodes(10000);
		Vector<String> result = testhelper.getManyNodesByIdFilter(nodeIds);
		System.out.println(result.size());
		System.out.println(result.lastElement());
	}

}
