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
import org.simplity.core.idb.IDbHandle;
import org.simplity.core.service.ServiceContext;

/**
 * an action insode a transaction processor that does not use data base
 *
 * @author simplity.org
 *
 */
public abstract class AbstractNonDbAction extends AbstractAction {
	@Override
	protected final boolean doAct(ServiceContext ctx, IDbHandle dbHandle, boolean transactionIsDelegated) {
		return this.act(ctx);
	}

	@Override
	boolean isBlockOfActions() {
		return false;
	}

	@Override
	final String executeBlock(ServiceContext ctx, IDbHandle dbHandle, boolean transactionIsDelegated) {
		throw new ApplicationError("Design Error: AbstractDbStep.aggregate() shoudl not be invoked.");
	}

	/**
	 * execute this action
	 *
	 * @param ctx
	 * @return true if the action succeeded. false otherwise. Note that false is
	 *         not "error". Error, if any is to be managed with service context.
	 *         This returned value is used to trigger other directives like
	 *         onSuccessxxx and onFailureXXX
	 */
	protected abstract boolean act(ServiceContext ctx);
}
