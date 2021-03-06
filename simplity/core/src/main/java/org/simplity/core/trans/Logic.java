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
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.service.ServiceContext;

/**
 * logic that does not use db. This is preferred way of implementing logic.
 *
 * @author simplity.org
 *
 */
public class Logic extends AbstractNonDbAction {
	/**
	 * fully qualified class name of logic
	 */
	@FieldMetaData(isRequired = true, superClass = ILogic.class)
	String className;

	private ILogic logic;

	@Override
	protected boolean act(ServiceContext ctx) {
		return this.logic.execute(ctx);
	}

	@Override
	public void getReady(int idx, TransactionProcessor processor) {
		super.getReady(idx, processor);
		this.logic = Application.getActiveInstance().getBean(this.className, ILogic.class);
		if (this.logic == null) {
			throw new ApplicationError(this.className + " is not a valid java class.");
		}
	}
}
