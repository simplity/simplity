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
import org.simplity.kernel.idb.IDbHandle;
import org.simplity.kernel.service.ServiceContext;

/**
 * a block of actions ia modeled as an action by itself.
 *
 * @author simplity.org
 */
public abstract class AbstractBlock extends AbstractAction {

	@Override
	final boolean isBlockOfActions() {
		return true;
	}

	@Override
	final boolean doAct(ServiceContext ctx, IDbHandle dbHandle, boolean transactionIsDelegated) {
		throw new ApplicationError("Design error: Block.doAct() is not to be invoked");
	}

	@Override
	final String executeBlock(ServiceContext ctx, IDbHandle dbHandle, boolean transIsDelegated) {
		return this.execute(ctx, dbHandle, transIsDelegated);
	}

	/**
	 * method to be extended by concrete actions that aggregate other actions.
	 * like loop/block
	 *
	 * @param ctx
	 * @param dbHandle
	 * @param transIsDelegated
	 * @return null to just take next action. jumpSignal or action name to jump
	 *         to if non-null
	 */
	protected abstract String execute(ServiceContext ctx, IDbHandle dbHandle, boolean transIsDelegated);
}
