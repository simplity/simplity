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
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.idb.IDbHandle;
import org.simplity.core.rdb.DbUsage;
import org.simplity.core.service.ServiceContext;

/**
 * complex logic that is implemented in a java code that also accesses db. We
 * believe that a logic that requires db operations in between is leads to
 * maintenance issues, and hence must be developed and reviewed by experienced
 * folks. For enabling such a process, we have separated simple and complex
 * logic interfaces.
 *
 * @author simplity.org
 */
public class LogicWithDbAccess extends AbstractDbAction {
	/**
	 * fully qualified class name of logic
	 */
	@FieldMetaData(isRequired = true, superClass = ILogicWithDbAccess.class)
	String className;

	@FieldMetaData(isRequired = true)
	DbUsage dbUsage;
	private ILogicWithDbAccess logic;

	@Override
	protected boolean actWithDb(ServiceContext ctx, IDbHandle dbHandle) {
		return this.logic.execute(ctx, dbHandle);
	}

	@Override
	protected void validateDbAction(IValidationContext vtx, TransactionProcessor task) {
		// nothing to validate
	}

	@Override
	public DbUsage getDbUsage() {
		return this.dbUsage;
	}

	@Override
	public void getReady(int idx, TransactionProcessor processor) {
		super.getReady(idx, processor);
		this.logic = Application.getActiveInstance().getBean(this.className, ILogicWithDbAccess.class);
		if (this.logic == null) {
			throw new ApplicationError(this.className + " is not a valid java class.");
		}
	}
}
