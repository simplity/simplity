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
import org.simplity.core.comp.ComponentType;
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationUtil;
import org.simplity.core.expr.Expression;
import org.simplity.core.idb.IDbHandle;
import org.simplity.core.msg.MessageType;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An action inside a processor. processor consists of one or more actions
 *
 * @author simplity.org
 */
public abstract class AbstractAction {
	private static final Logger logger = LoggerFactory.getLogger(AbstractAction.class);

	private static final String NAME_PREFIX = "_a";

	/**
	 * unique name within a processor. name assigned if not specified.
	 */
	protected String actionName = null;

	/**
	 * precondition to be met for this action to be executed.
	 */
	protected Expression executeOnCondition = null;

	/**
	 * if you want to execute this action if a sheet exists and has at least one
	 * row
	 */
	protected String executeIfRowsInSheet;

	/**
	 * execute if there is no sheet, or sheet has no rows
	 */
	protected String executeIfNoRowsInSheet;

	/**
	 * if the sql succeeds in extracting at least one row, or affecting one
	 * update, do we need to put a message?
	 */
	@FieldMetaData(isReferenceToComp = true, referredCompType = ComponentType.MSG)
	String successMessageName;
	/**
	 * comma separated list of parameters, to be used to populate success
	 * message
	 */
	@FieldMetaData(leaderField = "successMessageName")
	String[] successMessageParameters;

	/**
	 * if the sql fails to extract/update even a single row, should we flash any
	 * message?
	 */
	@FieldMetaData(isReferenceToComp = true, referredCompType = ComponentType.MSG)
	String failureMessageName;
	/**
	 * parameters to be used to format failure message
	 */
	@FieldMetaData(leaderField = "failureMessageName")
	String[] failureMessageParameters;

	/**
	 * should we stop this processor in case the message added is of type error.
	 */
	boolean stopIfMessageTypeIsError;

	/**
	 * name of action to navigate to within this block, (_stop, _continue and
	 * _break are special commands, as in jumpTo)
	 */
	String onSuccessJumpTo;
	/**
	 * name of action to navigate to within this block, (_stop, _continue and
	 * _break are special commands, as in jumpTo)
	 */
	String onFailureJumpTo;

	private int actionIdx;

	private boolean requiresPostProcessing;

	/**
	 *
	 * @return name of this action
	 */
	public String getName() {
		return this.actionName;
	}

	/**
	 * main method called by processor.
	 *
	 * @param ctx
	 *            non-null
	 * @param dbHandle
	 *            could be null if the service is not designed for db access
	 * @param transactionIsDelegated
	 * @return name of next action to jump to.
	 *         <li>null if no jumping, just move on.</li>
	 *         <li>one of jump signal as in
	 *         <code>TransConventions.JumpTo.isSignal</code></li>
	 *         <li>non-null, non-signal means the control should jump to this
	 *         named action.</li>
	 */
	public final String executeAction(ServiceContext ctx, IDbHandle dbHandle, boolean transactionIsDelegated) {
		/*
		 * is this a conditional action? i.e. to be executed only if the
		 * condition is met
		 */

		if (this.executeOnCondition != null) {
			try {
				Value val = this.executeOnCondition.evaluate(ctx);
				if (Value.intepretAsBoolean(val)) {
					logger.debug("Cleared the condition " + this.executeOnCondition + " for action to proceed.");
				} else {
					logger.debug("Condition " + this.executeOnCondition + " and hence skipping this action.");
					return null;
				}
			} catch (Exception e) {
				throw new ApplicationError("action " + this.actionName + " has an executOnCondition="
						+ this.executeOnCondition.toString() + " that is invalid. \nError : " + e.getMessage());
			}
		}
		if (this.executeIfNoRowsInSheet != null && ctx.nbrRowsInSheet(this.executeIfNoRowsInSheet) > 0) {
			return null;
		}
		if (this.executeIfRowsInSheet != null && ctx.nbrRowsInSheet(this.executeIfRowsInSheet) == 0) {
			return null;
		}

		if (this.isBlockOfActions()) {
			return this.executeBlock(ctx, dbHandle, transactionIsDelegated);
		}
		boolean ok = this.doAct(ctx, dbHandle, transactionIsDelegated);
		if (this.requiresPostProcessing == false) {
			return null;
		}
		if (ok) {
			if (this.successMessageName != null) {
				MessageType msgType = ctx.addMessage(this.successMessageName, this.successMessageParameters);
				if (msgType == MessageType.ERROR && this.stopIfMessageTypeIsError) {
					return TransConventions.JumpTo.STOP;
				}
			}
			if (this.onSuccessJumpTo != null) {
				return this.onSuccessJumpTo;
			}
			return null;
		}
		if (this.failureMessageName != null) {
			MessageType msgType = ctx.addMessage(this.failureMessageName, this.failureMessageParameters);
			if (msgType == MessageType.ERROR && this.stopIfMessageTypeIsError) {
				return TransConventions.JumpTo.STOP;
			}
		}
		if (this.onFailureJumpTo != null) {
			return this.onFailureJumpTo;
		}
		return null;
	}

	/**
	 * to be extended and sealed by next level sub-type that is meant for block
	 * of actions. like loop and block.
	 *
	 * @param ctx
	 * @param dbHandle
	 * @param transactionIsDelegated
	 * @return null to take next action. jusmpSignal or action name to jump
	 */
	abstract String executeBlock(ServiceContext ctx, IDbHandle dbHandle, boolean transactionIsDelegated);

	/**
	 *
	 * @return true if this action is an aggregator, and hence executeBlock()
	 *         method is used. if false, act() method would be invoked
	 */
	abstract boolean isBlockOfActions();

	/**
	 * take this action. do the work. to be sealed by next level, and expose
	 * more specific method
	 *
	 * @param ctx
	 * @param dbHandle
	 * @return true if all ok. false is something is not ok.
	 */
	abstract boolean doAct(ServiceContext ctx, IDbHandle dbHandle, boolean transactionIsDelegated);

	/**
	 * if there is anything this class wants to do after loading its attributes,
	 * but before being used, here is the method to do that.
	 *
	 * @param idx
	 *            0 based index of actions in this action
	 * @param processor
	 *            to which this action belongs to
	 */
	public void getReady(int idx, TransactionProcessor processor) {
		this.actionIdx = idx;
		if (this.actionName == null) {
			this.actionName = NAME_PREFIX + this.actionIdx;
		}

		this.requiresPostProcessing = this.onFailureJumpTo != null || this.onSuccessJumpTo != null
				|| this.failureMessageName != null || this.successMessageName != null;
	}

	/**
	 * validate this action
	 *
	 * @param vtx
	 * @param processor
	 *            parent processor
	 */
	public final void validate(IValidationContext vtx, TransactionProcessor processor) {
		ValidationUtil.validateMeta(vtx, this);
		this.validateSpecific(vtx, processor);
	}

	/**
	 * validate the extended action
	 *
	 * @param vtx
	 * @param processor
	 */
	public void validateSpecific(IValidationContext vtx, TransactionProcessor processor) {
		// meant to be extended if required
	}
}
