package de.wasabibeans.framework.server.core.aop;

import javax.ejb.EJB;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.jcr.Session;
import javax.transaction.TransactionManager;

import de.wasabibeans.framework.server.core.common.WasabiConstants;
import de.wasabibeans.framework.server.core.dto.WasabiObjectDTO;
import de.wasabibeans.framework.server.core.exception.UnexpectedInternalProblemException;
import de.wasabibeans.framework.server.core.locking.Locker;
import de.wasabibeans.framework.server.core.locking.LockingHelperLocal;
import de.wasabibeans.framework.server.core.util.JndiConnector;

public class JCRSessionInterceptor {

	@EJB
	private LockingHelperLocal locker;

	private TransactionManager tm;

	private TransactionManager getTransactionManager(JndiConnector jndi) throws UnexpectedInternalProblemException {
		if (tm == null) {
			tm = (TransactionManager) jndi.lookup("java:/TransactionManager");
		}
		return tm;
	}

	@AroundInvoke
	public Object handleJCRSession(InvocationContext invocationContext) throws Exception {
		// get service instance and given parameters
		WasabiAOP service = (WasabiAOP) invocationContext.getTarget();
		Object[] params = invocationContext.getParameters();

		// is an optLockId given?
		String lockToken = null;
		if (params.length > 0 && params[params.length - 1] instanceof Long) {
			// acquire lock according to the given optLockId
			Long optLockId = (Long) params[params.length - 1];
			lockToken = Locker.acquireServiceCallLock((WasabiObjectDTO) params[0], optLockId, locker,
					getTransactionManager(service.getJndiConnector()));
		}

		// get a JCR session from the JCA connection pool
		Session s;
		if (WasabiConstants.JCR_SAVE_PER_METHOD) {
			s = service.getJcrConnector().getJCRSession();
		} else {
			s = service.getJcrConnector().txModegetJCRSession(getTransactionManager(service.getJndiConnector()));
		}

		// associate existing lock-tokens with the JCR session
		Locker.recognizeLockToken(s, lockToken);
		for (Object param : params) {
			if (param instanceof WasabiObjectDTO) {
				Locker.recognizeLockToken(s, (WasabiObjectDTO) param);
			}
		}

		if (WasabiConstants.JCR_SAVE_PER_METHOD) {
			try {
				// execute the service method
				return invocationContext.proceed();
			} finally {
				// prepare the used JCR session to be returned to the JCA connection pool
				service.getJcrConnector().cleanup(false);
			}
		} else {
			try {
				// execute the service method
				return invocationContext.proceed();
			} catch (Exception e) {
				// any exception during a transaction in the 'WasabiConstants.JCR_SAVE_PER_METHOD = false'-mode must be
				// followed by an immediate rollback
				service.getJcrConnector().txModeCleanup();
				throw e;
			} finally {
				service.getJcrConnector().txModeAfterEachMethod();
			}
		}

	}
}
