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

package org.simplity.kernel.trans;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.app.Application;
import org.simplity.kernel.comp.FieldMetaData;
import org.simplity.kernel.idb.IDbHandle;
import org.simplity.kernel.service.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * spawns its actions into asynch threads and wait for all of them to complete
 * to proceed beyond this block. That is, this block action, as seen by its
 * parent, is synchronous, but it allows its child-actions to work in parallel
 *
 * @author simplity.org
 */
public class Synchronize extends AbstractAction {
	private static final Logger Logger = LoggerFactory.getLogger(Synchronize.class);

	/**
	 * is there something to be done before spawning thread for child-actions?
	 */
	AbstractAction initialAction;

	/** is there something we want to do after all child-actions return? */
	AbstractAction finalAction;

	/** actions of this block */
	@FieldMetaData(isRequired = true)
	AbstractAction[] actions;

	@Override
	protected String executeBlock(ServiceContext ctx, IDbHandle dbHandle, boolean transactionIsDelegated) {
		if (this.initialAction != null) {
			String whatNext = this.initialAction.executeAction(ctx, dbHandle, transactionIsDelegated);
			if (whatNext != null) {
				if (whatNext.equals(TransConventions.JumpTo.STOP)) {
					return whatNext;
				}
				if (whatNext.equals(TransConventions.JumpTo.BREAK_LOOP) == false) {
					Logger.warn(
							"initialaction has returned an invalid value of {}. It should wither return null, {} or {}. Value ignored and treated as {} and the synchronizer is not executed",
							whatNext, TransConventions.JumpTo.STOP, TransConventions.JumpTo.BREAK_LOOP,
							TransConventions.JumpTo.BREAK_LOOP);
					return null;
				}
			}
		}

		Logger.info("Synchronizer is goiing to create child-processes for each child-action.");
		Thread[] threads = new Thread[this.actions.length];
		AsynchWorker[] workers = new AsynchWorker[this.actions.length];
		int i = 0;
		for (AbstractAction action : this.actions) {
			AsynchWorker worker = new AsynchWorker(ctx, action, dbHandle, transactionIsDelegated);
			workers[i] = worker;
			Thread thread = Application.getActiveInstance().createThread(worker);
			threads[i] = thread;
			thread.start();
			i++;
		}

		Logger.info("{} Parallel actions created. Waiting for all of them to finish their job.", i);

		for (Thread thread : threads) {
			try {
				if (thread.isAlive()) {
					thread.join();
				}
			} catch (InterruptedException e) {
				Logger.info("One of the threads got interrupted for Synchronizer {}", this.actionName);
			}
		}

		Logger.info("All child-actions returned");
		if (this.finalAction != null) {
			return this.finalAction.executeAction(ctx, dbHandle, transactionIsDelegated);
		}
		return null;
	}

	@Override
	public void getReady(int idx, TransactionProcessor task) {
		super.getReady(idx, task);
		if (this.initialAction != null) {
			this.initialAction.getReady(0, task);
		}
		if (this.finalAction != null) {
			this.finalAction.getReady(0, task);
		}
		int i = 0;
		for (AbstractAction action : this.actions) {
			action.getReady(i++, task);
		}
	}

	class AsynchWorker implements Runnable {
		private final ServiceContext ctx;
		private final AbstractAction action;
		private final IDbHandle dbHandle;
		private final boolean transDelegated;

		AsynchWorker(ServiceContext ctx, AbstractAction action, IDbHandle dbHandle, boolean transDelegated) {
			this.ctx = ctx;
			this.action = action;
			this.dbHandle = dbHandle;
			this.transDelegated = transDelegated;
		}

		@Override
		public void run() {
			this.action.executeAction(this.ctx, this.dbHandle, this.transDelegated);
		}
	}

	@Override
	boolean isBlockOfActions() {
		return true;
	}

	@Override
	boolean doAct(ServiceContext ctx, IDbHandle dbHandle, boolean transactionIsDelegated) {
		throw new ApplicationError("Design Error: Synchronizer.doAct() should not be invoked");
	}
}
