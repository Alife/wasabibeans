package de.wasabibeans.framework.server.core.util;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

public class DebugInterceptor {

	@AroundInvoke
	public Object filter(InvocationContext invocationContext) throws Exception {
		String calledMethod = invocationContext.getMethod().getName();
		String service = invocationContext.getMethod().getDeclaringClass().getSimpleName();
		System.out.println("Begin " + service + "." + calledMethod);
		Object result = invocationContext.proceed();
		System.out.println("End " + service + "." + calledMethod);
		return result;
	}

}
