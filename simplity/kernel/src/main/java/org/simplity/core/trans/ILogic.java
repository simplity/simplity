/*
 * Copyright (c) 2016 simplity.org
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
package org.simplity.core.trans;

import org.simplity.core.service.ServiceContext;

/**
 * interface to be implemented by any class that can be called from a service as
 * a service action.
 *
 * @author simplity.org
 */
public interface ILogic {
	/**
	 * execute logic and return result. Important to note that this method is
	 * executed from a singleton instance that is cached for repeated use.This
	 * implies that the method must be implemented without using any mutable
	 * instance fields. In case you need to track state during the execution,
	 * you should have helper class instance for the same.
	 *
	 * @param ctx
	 *            This is your data area, for input as well as output.
	 * @return value, if required. if non-null value is returned it is saved
	 *         back in ctx with some specific name. refer to Action.java
	 */
	public boolean execute(ServiceContext ctx);
}
