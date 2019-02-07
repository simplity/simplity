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

package org.simplity.kernel.trans;

import org.simplity.kernel.app.Application;
import org.simplity.kernel.comp.FieldMetaData;
import org.simplity.kernel.comp.IValidationContext;
import org.simplity.kernel.rdb.DbUsage;
import org.simplity.kernel.service.ServiceContext;

/**
 * @author simplity.org
 *
 */
public class JavaProcessor implements IProcessor {

	/**
	 * fully qualified class name that implements ITask interface
	 */
	@FieldMetaData(superClass = IProcessor.class, isRequired = true)
	String className;

	@FieldMetaData(isRequired = true)
	DbUsage dbUsge;
	private IProcessor processor;

	@Override
	public void execute(ServiceContext ctx) {
		this.processor.execute(ctx);

	}

	@Override
	public boolean executeAsAction(ServiceContext ctx) {
		return this.processor.executeAsAction(ctx);
	}

	@Override
	public void validate(IValidationContext vtx, Service service) {
		//
	}

	@Override
	public void getReady(Service service) {
		this.processor = Application.getActiveInstance().getBean(this.className, IProcessor.class);
		this.processor.getReady(service);
	}

	@Override
	public DbUsage getDbUsage() {
		return this.dbUsge;
	}

}
