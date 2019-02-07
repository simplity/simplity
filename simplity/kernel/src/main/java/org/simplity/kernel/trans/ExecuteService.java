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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.kernel.trans;

import org.simplity.kernel.app.Application;
import org.simplity.kernel.app.IService;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.FieldMetaData;
import org.simplity.kernel.comp.IValidationContext;
import org.simplity.kernel.idb.IDbHandle;
import org.simplity.kernel.rdb.DbUsage;
import org.simplity.kernel.service.ServiceContext;

/**
 * execute a local service within this context. A local service is part of teh
 * same "module" if a module concept is used. Otherwise it is any service that
 * is defined as part of this Application
 *
 * @author simplity.org
 */
public class ExecuteService extends AbstractDbAction {

	/**
	 * fully qualified service name
	 */
	@FieldMetaData(isRequired = true, isReferenceToComp = true, referredCompType = ComponentType.SERVICE)
	String serviceName;

	@Override
	protected boolean actWithDb(ServiceContext ctx, IDbHandle dbHandle) {
		return Application.getActiveInstance().getService(this.serviceName).executeAsSubProcess(ctx);
	}

	@Override
	protected void validateDbAction(IValidationContext vtx, TransactionProcessor task) {
		// nothing to validate
	}

	@Override
	public DbUsage getDbUsage() {
		IService service = Application.getActiveInstance().getService(this.serviceName);
		return service.getDbUsage();
	}
}
