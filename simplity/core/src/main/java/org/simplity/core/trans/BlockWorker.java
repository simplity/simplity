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

import java.util.Map;

import org.simplity.core.ApplicationError;
import org.simplity.core.idb.IDbClient;
import org.simplity.core.idb.IDbHandle;
import org.simplity.core.jms.IJmsClient;
import org.simplity.core.service.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * block of actions. This class is not a "component". This is used to create a
 * run-time instance to execute actions (actions are components)
 *
 * @author simplity.org
 */
public class BlockWorker implements IDbClient, IJmsClient {
	private static final Logger logger = LoggerFactory.getLogger(BlockWorker.class);

	private final AbstractAction[] actions;

	private final Map<String, Integer> indexedActions;

	private final ServiceContext ctx;

	private final IDbHandle initialHandle;

	private final boolean transactionIsDelegated;

	private boolean keepGoing = true;
	/*
	 * if the worker is indirectly called via dbDriver, then we need to keep the
	 * whatNext for the caller to query for it later
	 */
	private String executionResult;

	/**
	 * run-time object instance to execute a block of actions
	 *
	 * @param actions
	 * @param indexedActions
	 * @param ctx
	 * @param dbHandle
	 * @param transactionIsDelegated
	 */
	public BlockWorker(AbstractAction[] actions, Map<String, Integer> indexedActions, ServiceContext ctx,
			IDbHandle dbHandle,
			boolean transactionIsDelegated) {
		this.actions = actions;
		this.indexedActions = indexedActions;
		this.ctx = ctx;
		this.initialHandle = dbHandle;
		this.transactionIsDelegated = transactionIsDelegated;
	}

	/*
	 * call-back from dbDriver.
	 */
	@Override
	public boolean accessDb(IDbHandle dbHandle) {
		/*
		 * this method is invoked as call-back from dbDriver
		 */
		this.executionResult = this.execute(dbHandle);
		if (this.ctx.isInError()) {
			return false;
		}
		return true;
	}

	/**
	 * execute this block once
	 *
	 * @param dbHandle
	 * @return null if it is a normal exit, or a specific signal
	 */
	public String execute(IDbHandle dbHandle) {
		int nbrActions = this.actions.length;
		int currentIdx = 0;
		String whatNext = null;
		while (currentIdx < nbrActions) {
			AbstractAction action = this.actions[currentIdx];
			long startedAt = System.currentTimeMillis();
			whatNext = action.executeAction(this.ctx, dbHandle, this.transactionIsDelegated);
			currentIdx++;

			logger.info("Action {} finished in {} ms", action.actionName,
					(System.currentTimeMillis() - startedAt) + " ms");

			if (whatNext == null) {
				continue;
			}
			/*
			 * did the caller signal a stop ?
			 */
			if (TransConventions.JumpTo.isSignal(whatNext)) {
				return whatNext;
			}
			/*
			 * is the target action within this block?
			 */
			Integer idx = this.indexedActions.get(whatNext);
			if (idx != null) {
				/*
				 * we are to go to this action.
				 */
				currentIdx = idx.intValue();
			} else {
				throw new ApplicationError("An action within a block requested to jump to a action " + whatNext
						+ " but that is not a valid name of any action within that block.");
			}
		}
		return null;
	}

	/*
	 * this call is coming from JMS. This is convoluted. we have to re-factor
	 * this to improve the call-graph
	 */
	@Override
	public boolean process(ServiceContext sameCtxComingBack) {
		this.executionResult = this.execute(this.initialHandle);
		/*
		 * we have to return true to JMS, but we have to remember whether to
		 * stop or continue, after JMS returns the corresponding action
		 */
		this.keepGoing = this.executionResult == null
				|| TransConventions.JumpTo.STOP.equals(this.executionResult) == false;
		return true;
	}

	@Override
	public boolean toContinue() {
		return this.keepGoing;
	}

	/**
	 * @return execution result
	 */
	public String getWhatNext() {
		return this.executionResult;
	}
}
