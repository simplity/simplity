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

package org.simplity.core.aggr;

import org.simplity.core.data.IFieldsCollection;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.value.Value;

/**
 * number of rows
 *
 * @author simplity.org
 */
public class Last extends First {
	/**
	 * create an an instance with the required parameters
	 *
	 * @param inputName
	 *            input name
	 * @param outputName
	 *            field/column name that is to be written out as sum. non-empty,
	 *            non-null;
	 */
	public Last(String inputName, String outputName) {
		super(inputName, outputName);
	}

	@Override
	public void accumulate(IFieldsCollection currentRow, ServiceContext ctx) {
		if (this.inProgress == false) {
			this.throwError();
		}
		Value val = ctx.getValue(this.inputName);
		if (Value.isNull(val) == false) {
			this.value = val;
		}
	}
}
