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

package org.simplity.kernel.adapter;

import org.simplity.kernel.comp.IValidationContext;
import org.simplity.kernel.service.ServiceContext;

/**
 * a field in a data adapter
 *
 * @author simplity.org
 *
 */
public abstract class AbstractField {
	/**
	 * name of field to copy from. required except if the default value is to be
	 * copied to the target. Use a.b.c convention to directly reach a field in
	 * the hierarchy
	 */
	String fromName;

	/**
	 * name of the field to copy to. for group fields, this is optional
	 */
	String toName;

	/**
	 * open shop and get ready for repeated use
	 */
	public void getReady() {
		//
	}

	/**
	 *
	 * @param source
	 *            non-null source to get data from
	 * @param target
	 *            non-null target to set data to
	 * @param ctx
	 *            non-null service context
	 */
	public abstract void copy(IDataSource source, IDataTarget target, ServiceContext ctx);

	/**
	 * @param vtx
	 */
	protected void validate(IValidationContext vtx) {
		//
	}
}
