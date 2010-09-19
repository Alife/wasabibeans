package de.wasabibeans.framework.server.core.aop;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import de.wasabibeans.framework.server.core.bean.ObjectService;
import de.wasabibeans.framework.server.core.util.JcrConnector;

public class TransactionInterceptor {

	@AroundInvoke
	public Object handleTransactionAndSession(InvocationContext invocationContext) throws Exception {
		WasabiService serviceEJB = (ObjectService) invocationContext.getTarget();
		JcrConnector jcr = serviceEJB.getJcrConnector();
		try {
			Object result = invocationContext.proceed();
			return result;
		} finally {
			jcr.logout();
		}
	}
}
