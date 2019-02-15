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

package org.simplity.core.service;

import org.simplity.core.data.Fields;

/**
 * @author simplity.org
 *
 *         class, typically a data structure or Value Object, to be capable of
 *         taking input data from ServiceContext and to put output data back
 *         into the context
 */
public interface ICtxPluggable {
	/**
	 * read data from service context.
	 *
	 * @param ctx
	 *            service context
	 * @param fields
	 *            optional. If non-null, suggested set of fields that are to be
	 *            read from service context.
	 */
	public void inputFromCtx(ServiceContext ctx, Fields[] fields);

	/**
	 * write data to service context.
	 *
	 * @param ctx
	 *            service context
	 * @param fields
	 *            optional. If non-null, suggested set of fields to be written
	 *            to service context.
	 */
	public void outputToCtx(ServiceContext ctx, Fields[] fields);
}
