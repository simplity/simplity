/*
 * Copyright (c) 2017 simplity.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.core.jms;

import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.simplity.core.ApplicationError;
import org.simplity.core.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * class that acts as a wrapper on JMSSession to JMS client classes. This is
 * similar to DbDriver in its functionality
 *
 * @author simplity.org
 */
public class JmsConnector {
	private static final Logger logger = LoggerFactory.getLogger(JmsConnector.class);

	/**
	 * for non-jta connection
	 */
	private static ConnectionFactory factory;

	/**
	 * for jta-managed connection
	 */
	private static ConnectionFactory xaFactory;

	/**
	 * initial setup. Called by Application on startup
	 *
	 * @param connectionFactory
	 * @param xaConnectionFactory
	 * @param properties
	 *            additional properties like user name etc.. that are required
	 *            to be set to the context for getting the connection
	 * @return error message in case of error. null if all OK
	 * @throws ApplicationError
	 *             : in case of any issue with the set-up
	 */
	public static String setup(String connectionFactory, String xaConnectionFactory, Property[] properties) {
		Context ctx = null;

		try {
			if (properties == null || properties.length == 0) {
				ctx = new InitialContext();
			} else {
				Properties env = new Properties();
				for (Property property : properties) {
					env.put(property.getName(), property.getValue());
				}
				ctx = new InitialContext(env);
			}

			if (connectionFactory != null) {
				factory = (QueueConnectionFactory) ctx.lookup(connectionFactory);
				logger.info("queueConnectionFactory successfully set to " + factory.getClass().getName());
			}

			if (xaConnectionFactory != null) {
				xaFactory = (QueueConnectionFactory) ctx.lookup(xaConnectionFactory);
				logger.info("xaQueueConnectionFactory successfully set to " + xaFactory.getClass().getName());
			}
		} catch (Exception e) {
			return e.getMessage();
		}
		return null;
	}

	/**
	 * get a JMS connection for repeated use across multiple transactions.
	 * caller can issue start(), commit() rollBack() etc..
	 *
	 * @param jmsUsage
	 * @return connection
	 */
	public static JmsConnector borrowMultiTransConnector(JmsUsage jmsUsage) {
		return borrow(jmsUsage, true);
	}

	/**
	 * get a JMS connection. And, please, please do not close() it or abandon
	 * it. Do return it once you are done. I am dependent on your discipline at
	 * this time to avoid memory leakage
	 *
	 * @param jmsUsage
	 * @return connection
	 */
	public static JmsConnector borrowConnector(JmsUsage jmsUsage) {
		return borrow(jmsUsage, false);
	}

	@SuppressWarnings("resource")
	private static JmsConnector borrow(JmsUsage jmsUsage, boolean multi) {

		try {
			Connection con = null;
			boolean transacted = false;
			Session session = null;
			if (jmsUsage == JmsUsage.EXTERNALLY_MANAGED) {
				if (xaFactory == null) {
					throw new ApplicationError("Application is not set up for JMS with JTA/JCA/XA");
				}
				con = xaFactory.createConnection();
			} else {
				if (factory == null) {
					throw new ApplicationError("Application is not set up for JMS local session managed operations");
				}
				con = factory.createConnection();
				if (jmsUsage == JmsUsage.SERVICE_MANAGED) {
					transacted = true;
				}
			}
			session = con.createSession(transacted, Session.AUTO_ACKNOWLEDGE);
			/*
			 * not very well advertised.. but this method is a MUST for
			 * consuming queues, though production works without that
			 */
			con.start();
			return new JmsConnector(con, session, jmsUsage, multi);
		} catch (Exception e) {
			throw new ApplicationError(e, "Error while creating jms session");
		}
	}

	/**
	 * @param connector
	 * @param allOk
	 */
	public static void returnConnector(JmsConnector connector, boolean allOk) {
		connector.close(allOk);
	}

	/** jndi name of queueConnection factory non-JTA connection */
	private final Connection connection;

	/** jndi name of queueConnection factory non-JTA connection */
	private final Session session;

	/** usage for which this instance is created */
	private JmsUsage jmsUsage;

	private boolean forMultiTrans;

	/**
	 * @param con
	 * @param session
	 * @param jmsUsage
	 */
	private JmsConnector(Connection con, Session session, JmsUsage jmsUsage, boolean multi) {
		this.connection = con;
		this.session = session;
		this.jmsUsage = jmsUsage;
		this.forMultiTrans = multi;
	}

	private void close(boolean allOk) {
		if (this.forMultiTrans == false) {
			try {
				if (this.jmsUsage == JmsUsage.SERVICE_MANAGED) {
					if (allOk) {

						logger.info("Jms session committed.");

						this.session.commit();
					} else {

						logger.info("Jms session rolled-back.");

						this.session.rollback();
					}
				} else {

					logger.info("non-transactional JMS session closed.");
				}
			} catch (Exception e) {
				throw new ApplicationError(e, "error while closing jms conenction");
			}
		}
		try {
			this.session.close();
		} catch (Exception ignore) {
			//
		}
		try {
			this.connection.close();
		} catch (Exception ignore) {
			//
		}
	}

	private void checkMulti() {
		if (this.forMultiTrans == false) {
			throw new ApplicationError(
					"Jms connection is borrowed for a single transaciton, but is used to manage transactions.");
		}
	}

	/**
	 * commit current transaction. Valid only if the connection is borrowed for
	 * multi-trnsactions
	 *
	 * @throws JMSException
	 */
	public void commit() throws JMSException {
		this.checkMulti();
		this.session.commit();
	}

	/**
	 * roll-back current transaction. Valid only if the connection is borrowed
	 * for multi-trnsactions
	 *
	 * @throws JMSException
	 */
	public void rollback() throws JMSException {
		this.checkMulti();
		this.session.rollback();
	}

	/**
	 * @return session associated with this connector
	 */
	public Session getSession() {
		return this.session;
	}
}
