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

import org.simplity.core.comp.IValidationContext;
import org.simplity.core.rdb.DbUsage;
import org.simplity.core.service.ServiceContext;

/**
 * DO the actual processing part of a service execution. It is the worker part
 * of <code>Service</code>
 *
 * @author simplity.org
 *
 */
public interface IProcessor {
	/**
	 * main method. called after processing <code>InputData</code>.
	 * <code>OutputData</code will be processed after execution of this method.
	 *
	 * @param ctx
	 *            non-null service context
	 */
	public void execute(ServiceContext ctx);

	/**
	 * This is being called as a sub-process from another processor.
	 *
	 * @param ctx
	 *            non-null service context
	 * @return true if all well. false if task did not com
	 */
	public boolean executeAsAction(ServiceContext ctx);

	/**
	 * validating the specification. called from parent.
	 *
	 * @param vtx
	 * @param service
	 *            parent service that is calling this method
	 */
	public void validate(IValidationContext vtx, Service service);

	/**
	 * initial set-up. called from getReady() of parent.
	 *
	 * @param service
	 */
	public void getReady(Service service);

	/**
	 *
	 * @return non-null dbUsage requirement of this task
	 */
	public DbUsage getDbUsage();
}
