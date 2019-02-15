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
public class JmsSetup {
	protected static final Logger logger = LoggerFactory.getLogger(JmsSetup.class);

	/**
	 * if JMS is used by this application, connection factory for local/session
	 * managed operations
	 */
	String connectionFactoryJndiName;
	/**
	 * if JMS is used by this application, connection factory for JTA/JCA/XA
	 * managed operations
	 */
	String xaConnectionFactoryJndiName;

	/**
	 * properties of jms connection, like user name password and other flags
	 */
	Property[] properties;
	/**
	 * for non-jta connection
	 */
	protected ConnectionFactory factory;

	/**
	 * for jta-managed connection
	 */
	protected ConnectionFactory xaFactory;

	/**
	 * initial setup. Called by Application on startup
	 *
	 * @return null if all Ok. error message in case of any issue with
	 */
	public String configure() {
		Context ctx = null;

		try {
			if (this.properties == null || this.properties.length == 0) {
				ctx = new InitialContext();
			} else {
				Properties env = new Properties();
				for (Property property : this.properties) {
					env.put(property.getName(), property.getValue());
				}
				ctx = new InitialContext(env);
			}

			if (this.connectionFactoryJndiName == null) {
				if (this.xaConnectionFactoryJndiName == null) {
					return "jsm set up error. No jndi name specified for connection factory.";
				}
			} else {
				this.factory = (QueueConnectionFactory) ctx.lookup(this.connectionFactoryJndiName);
				logger.info("queueConnectionFactory successfully set to " + this.factory.getClass().getName());
			}

			if (this.xaConnectionFactoryJndiName != null) {
				this.xaFactory = (QueueConnectionFactory) ctx.lookup(this.xaConnectionFactoryJndiName);
				logger.info("xaQueueConnectionFactory successfully set to " + this.xaFactory.getClass().getName());
			}
		} catch (Exception e) {
			String msg = "Jms set up failed. " + e.getLocalizedMessage();
			logger.error(msg);
			return msg;
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
	public JmsConnector borrowMultiTransConnector(JmsUsage jmsUsage) {
		return new JmsConnector(jmsUsage, true);
	}

	/**
	 * get a JMS connection. And, please, please do not close() it or abandon
	 * it. Do return it once you are done. I am dependent on your discipline at
	 * this time to avoid memory leakage
	 *
	 * @param jmsUsage
	 * @return connection
	 */
	public JmsConnector borrowConnector(JmsUsage jmsUsage) {
		return new JmsConnector(jmsUsage, false);
	}

	/**
	 * class that manages jms connection
	 *
	 * @author simplity.org
	 *
	 */
	public class JmsConnector {
		private final Connection connection;
		private final Session session;
		private JmsUsage jmsUsage;
		private boolean forMultiTrans;

		protected JmsConnector(JmsUsage jmsUsage, boolean multi) {
			this.jmsUsage = jmsUsage;
			this.forMultiTrans = multi;
			try {
				boolean transacted = false;
				if (jmsUsage == JmsUsage.EXTERNALLY_MANAGED) {
					if (JmsSetup.this.xaFactory == null) {
						throw new ApplicationError("Application is not set up for JMS with JTA/JCA/XA");
					}
					this.connection = JmsSetup.this.xaFactory.createConnection();
				} else {
					if (JmsSetup.this.factory == null) {
						throw new ApplicationError(
								"Application is not set up for JMS local session managed operations");
					}
					this.connection = JmsSetup.this.factory.createConnection();
					if (jmsUsage == JmsUsage.SERVICE_MANAGED) {
						transacted = true;
					}
				}
				this.session = this.connection.createSession(transacted, Session.AUTO_ACKNOWLEDGE);
				/*
				 * not very well advertised.. but this method is a MUST for
				 * consuming queues, though production works without that
				 */
				this.connection.start();
			} catch (Exception e) {
				throw new ApplicationError(e, "Error while creating jms session");
			}
		}

		/**
		 * user is done with this connector. returning it
		 *
		 * @param allOk
		 *            if everything went well. transaction may be committed.
		 *            false if the transaction need to be rolled back
		 */
		public void returnedWithThanks(boolean allOk) {
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
		 * commit current transaction. Valid only if the connection is borrowed
		 * for multi-trnsactions
		 *
		 * @throws JMSException
		 */
		public void commit() throws JMSException {
			this.checkMulti();
			this.session.commit();
		}

		/**
		 * roll-back current transaction. Valid only if the connection is
		 * borrowed for multi-trnsactions
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
}
