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

package org.simplity.core.trans;

import java.util.HashMap;
import java.util.Map;

import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.idb.IDbHandle;
import org.simplity.core.service.ServiceContext;

/**
 * abstract action that manages a block of actions
 *
 * @author simplity.org
 */
public abstract class Block extends AbstractBlock {

	/** actions of this block */
	@FieldMetaData(isRequired = true)
	AbstractAction[] actions;

	protected Map<String, Integer> indexedActions = new HashMap<String, Integer>();

	@Override
	protected String execute(ServiceContext ctx, IDbHandle dbHandle, boolean transIsDelegated) {
		return new BlockWorker(this.actions, this.indexedActions, ctx, dbHandle, transIsDelegated)
				.execute(dbHandle);
	}

	@Override
	public void getReady(int idx, TransactionProcessor task) {
		super.getReady(idx, task);

		int i = 0;
		for (AbstractAction action : this.actions) {
			action.getReady(i, task);
			this.indexedActions.put(action.getName(), new Integer(i));
			i++;
		}
	}

	@Override
	public void validateSpecific(IValidationContext ctx, TransactionProcessor task) {
		super.validateSpecific(ctx, task);
		if (this.actions != null) {
			for (AbstractAction action : this.actions) {
				action.validate(ctx, task);
			}
		}
	}
}
