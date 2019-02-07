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

package org.simplity.kernel.idb;

import org.simplity.kernel.value.Value;

/**
 * functions to be implemented by a call-back object to get row of data when the
 * meta-data of the result set is not known at design time by the caller
 *
 * @author simplity.org
 *
 */
public interface IRowWithNameConsumer {
	/**
	 * to be used when the name of the fields/columns in the result set is not
	 * known at design time by the caller. name is extracted at run time using
	 * meta-data and hence has a slight performance overhead
	 *
	 * @param names
	 *            column/field names
	 * @param values
	 *            values data row returned by a result set.
	 * @return false to stop reading the result set. true to continue to read
	 */
	public boolean consume(String[] names, Value[] values);
}
