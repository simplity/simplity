/*
 * Copyright (c) 2018 simplity.org
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

package org.simplity.core.trans;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.transaction.UserTransaction;

import org.simplity.core.ApplicationError;
import org.simplity.core.app.Application;
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationMessage;
import org.simplity.core.comp.ValidationUtil;
import org.simplity.core.idb.IDbDriver;
import org.simplity.core.idb.IDbHandle;
import org.simplity.core.jms.JmsSetup.JmsConnector;
import org.simplity.core.jms.JmsUsage;
import org.simplity.core.rdb.DbUsage;
import org.simplity.core.service.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specifies all the infrastructure requirements and the actions to be carried
 * for a typical transaction processing service
 *
 * @author simplity.org
 *
 */
public class TransactionProcessor implements IProcessor {
	private static final Logger logger = LoggerFactory.getLogger(TransactionProcessor.class);
	/**
	 * database access type
	 */
	DbUsage dbUsage = DbUsage.NONE;

	/**
	 * schema name, different from the default schema, to be used specifically
	 * for this service
	 */
	String schemaName;

	/**
	 * does this service use jms? if so with what kind of transaction management
	 */
	JmsUsage jmsUsage;

	/**
	 * actions that make up this service
	 */
	@FieldMetaData(isRequired = true)
	AbstractAction[] actions;

	/**
	 * action names indexed to respond to navigation requests
	 */
	private final HashMap<String, Integer> indexedActions = new HashMap<String, Integer>();

	@Override
	public void execute(ServiceContext ctx) {
		/*
		 * resources that need to be released without fail..
		 */
		JmsConnector jmsConnector = null;
		UserTransaction userTransaciton = null;

		ApplicationError exception = null;
		/*
		 * we are unable to use a simple try-with. Hence this block that
		 * effectively does that
		 */
		try {
			/*
			 * acquire resources that are needed for this service
			 */
			if (this.jmsUsage != null) {
				jmsConnector = Application.getActiveInstance().getJmsSetup().borrowConnector(this.jmsUsage);
				ctx.setJmsSession(jmsConnector.getSession());
			}

			DbUsage access = this.dbUsage;
			/*
			 * is this a JTA transaction?
			 */
			BlockWorker worker = new BlockWorker(this.actions, this.indexedActions, ctx, null,
					this.dbUsage == DbUsage.SUB_SERVICE);
			if (access == DbUsage.NONE) {
				worker.execute(null);
			} else {
				if (access == DbUsage.EXTERNAL) {
					userTransaciton = Application.getActiveInstance().getUserTransaction();
					userTransaciton.begin();
				}
				IDbDriver driver = Application.getActiveInstance().getRdbSetup().getDefaultDriver();
				driver.accessDb(worker, this.dbUsage.getDbAccessType(), this.schemaName);
			}
		} catch (ApplicationError e) {
			exception = e;
		} catch (Exception e) {
			exception = new ApplicationError(e, "Exception during execution of service. ");
		}
		/*
		 * close/return resources
		 */
		if (jmsConnector != null) {
			jmsConnector.returnedWithThanks(exception == null && ctx.isInError() == false);
		}
		if (userTransaciton != null) {
			try {
				if (exception == null && ctx.isInError() == false) {
					userTransaciton.commit();
				} else {
					logger.info("Service is in error. User transaction rolled-back");
					userTransaciton.rollback();
				}
			} catch (Exception e) {
				exception = new ApplicationError(e, "Error while commit/rollback of user transaction");
			}
		}
		if (exception != null) {
			throw exception;
		}
	}

	@Override
	public boolean executeAsAction(ServiceContext ctx) {
		boolean transIsDelegated = ctx.isTransactionDelegeated();
		boolean isJms = this.jmsUsage == JmsUsage.EXTERNALLY_MANAGED || this.jmsUsage == JmsUsage.SERVICE_MANAGED;
		if (transIsDelegated || isJms) {
			/*
			 * we are on our own. behave as if we are the main processor
			 */
			this.execute(ctx);
			return !ctx.isInError();
		}

		IDbHandle dbHandle = ctx.getDbHandle();
		if (this.canWorkWithDriver(dbHandle)) {
			BlockWorker worker = new BlockWorker(this.actions,
					this.indexedActions, ctx, null, transIsDelegated);
			return worker.accessDb(dbHandle);
		}
		throw new ApplicationError("Called sub-processor uses db as " + this.dbUsage
				+ " but the main service hs created a dbHandle of type " + dbHandle.getClass().getName());

	}

	private boolean canWorkWithDriver(IDbHandle dbHandle) {
		/*
		 * use of JMS may trigger this irrespective of db access
		 */
		if (this.jmsUsage == JmsUsage.SERVICE_MANAGED || this.jmsUsage == JmsUsage.EXTERNALLY_MANAGED) {
			return false;
		}
		/*
		 * if we do not need it all, anything will do..
		 */
		if (this.dbUsage == null || this.dbUsage == DbUsage.NONE) {
			return true;
		}
		/*
		 * can not work with null.
		 */
		if (dbHandle == null) {
			return false;
		}

		/*
		 * may be we can get away for reads if schema is ok
		 */
		if (this.dbUsage == DbUsage.READ_ONLY) {
			if (this.schemaName == null) {
				return dbHandle.getSchema() == null;
			}
			return this.schemaName.equalsIgnoreCase(dbHandle.getSchema());

		}

		/*
		 * we tried our best to re-use... but failed
		 */
		return false;
	}

	@Override
	public void getReady(Service service) {
		int i = 0;
		for (AbstractAction action : this.actions) {
			action.getReady(i, this);
			if (this.indexedActions.containsKey(action.actionName)) {
				throw new ApplicationError(
						"Service " + service.getServiceName() + " has actions with duplicate action name "
								+ action.actionName
								+ " as its action nbr " + (i + 1));
			}
			this.indexedActions.put(action.actionName, new Integer(i));
			i++;
		}
	}

	@Override
	public void validate(IValidationContext vtx, Service service) {
		ValidationUtil.validateMeta(vtx, this);
		if (this.actions != null) {
			this.validateChildren(vtx);
		}

		if (this.schemaName != null) {
			IDbDriver driver = Application.getActiveInstance().getRdbSetup().getDefaultDriver();
			if (driver != null && driver.isSchemaDefined(this.schemaName) == false) {

				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
						"schemaName is set to " + this.schemaName
								+ " but it is not defined as one of additional schema names in application.xml",
						"schemaName"));
			}
		}
	}

	/**
	 * validate child actions
	 *
	 * @param vtx
	 */
	private void validateChildren(IValidationContext vtx) {
		Set<String> addedSoFar = new HashSet<String>();
		int actionNbr = 0;
		for (AbstractAction action : this.actions) {
			actionNbr++;
			if (action.actionName != null) {
				if (addedSoFar.add(action.actionName) == false) {
					vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
							"Duplicate action name " + action.actionName + " at " + actionNbr, null));
				}
			}
			action.validate(vtx, this);
		}
	}

	@Override
	public DbUsage getDbUsage() {
		return this.dbUsage;
	}

	/**
	 * @return jmsUsage of this processor
	 */
	public Object getJmsUsage() {
		return this.jmsUsage;
	}

	/**
	 *
	 * @return schema name used by this processor. null if it uses the default
	 */
	public String getSchemaName() {
		return this.schemaName;
	}
}
