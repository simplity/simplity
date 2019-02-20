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

import org.simplity.core.ApplicationError;
import org.simplity.core.app.Application;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationMessage;
import org.simplity.core.idb.DbAccessType;
import org.simplity.core.idb.IDbClient;
import org.simplity.core.idb.IDbHandle;
import org.simplity.core.rdb.DbUsage;
import org.simplity.core.service.ServiceContext;

/**
 * action that uses db
 *
 * @author simplity.org
 *
 */
public abstract class AbstractDbAction extends AbstractAction {

	/**
	 * relevant when the parent service has delegated the transaction to
	 * actions. null to use default schema. This is very rarely used, and MUST
	 * BE used with caution, because the schema name gets embedded into this
	 * lower level component.
	 */
	String schemaName;

	@Override
	boolean isBlockOfActions() {
		return false;
	}

	@Override
	protected final boolean doAct(ServiceContext ctx, IDbHandle dbHandle, boolean transactionIsDelegated) {
		if (this.canUse(dbHandle)) {
			return this.actWithDb(ctx, dbHandle);
		}
		/*
		 * service has delegated transactions to its actions... We have to
		 * directly deal with the driver for this
		 */
		Worker worker = new Worker(ctx, this);
		Application.getActiveInstance().getRdbSetup().getDefaultDriver().accessDb(worker,
				this.getDbUsage().getDbAccessType(), this.schemaName);
		return worker.isSuccess();
	}

	@Override
	final String executeBlock(ServiceContext ctx, IDbHandle dbHandle, boolean transactionIsDelegated) {
		throw new ApplicationError("Design Error: AbstractDbAction.executeBlock() should not be invoked.");
	}

	private boolean canUse(IDbHandle dbHandle) {
		if (dbHandle == null) {
			return false;
		}
		DbUsage usage = this.getDbUsage();
		if (usage == null || usage.updatesDb() == false) {
			return true;
		}

		if (dbHandle.getHandleType() == DbAccessType.READ_ONLY) {
			return true;
		}
		return false;
	}

	/**
	 * a action that requires db access
	 *
	 * @param ctx
	 * @param dbHandle
	 *            appropriate for the declared dbUsage
	 * @return
	 */
	protected abstract boolean actWithDb(ServiceContext ctx, IDbHandle dbHandle);

	/**
	 * worker class to work with the driver
	 *
	 * @author simplity.org
	 */
	protected class Worker implements IDbClient {
		private final ServiceContext ctx;
		private boolean success;
		private final AbstractDbAction action;

		Worker(ServiceContext ctx, AbstractDbAction action) {
			this.ctx = ctx;
			this.action = action;
		}

		@Override
		public boolean accessDb(IDbHandle dbHandle) {
			this.success = this.action.actWithDb(this.ctx, dbHandle);
			if (this.ctx.isInError()) {
				return false;
			}
			return true;
		}

		boolean isSuccess() {
			return this.success;
		}
	}

	@Override
	public final void validateSpecific(IValidationContext vtx, TransactionProcessor task) {
		if (task.dbUsage == null || task.dbUsage == DbUsage.NONE) {
			vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
					"action requires db access but the task is declared but the task has dbUsage = " + task.dbUsage,
					null));
		} else if (task.dbUsage.canWorkWithChildType(this.getDbUsage()) == false) {
			vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
					"dbAccessType of service is not compatible for the dbAccessType of its actions. Please review your db access design thoroughly based on your actions design.",
					null));
		}
		if (this.schemaName != null) {
			if (task.dbUsage != DbUsage.SUB_SERVICE) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
						"schemaName can not be used for this action because the parent task is not delegating transactions",
						null));
			}
		}
		this.validateDbAction(vtx, task);
	}

	/**
	 * what type of data usage ?
	 *
	 * @return non-null and non-DbUsage.NONE
	 */
	public abstract DbUsage getDbUsage();

	/**
	 * @param vtx
	 * @param task
	 */
	protected abstract void validateDbAction(IValidationContext vtx, TransactionProcessor task);

}
