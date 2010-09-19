package de.wasabibeans.framework.server.core.aop;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import de.wasabibeans.framework.server.core.util.JcrConnector;

public class TransactionInterceptor {
	
	@AroundInvoke
	public Object handleTransactionAndSession(InvocationContext invocationContext) throws Exception {
		WasabiService serviceEJB = (WasabiService) invocationContext.getTarget();
		JcrConnector jcr = serviceEJB.getJcrConnector();
		jcr.getJCRSession();
		try {
			Object result = invocationContext.proceed();
			
			jcr.logout();
			
			return result;
		} catch (Exception e) {
			jcr.destroy();
			throw e;
		} 
	}
}
