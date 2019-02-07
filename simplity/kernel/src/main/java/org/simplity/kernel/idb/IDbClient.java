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

/**
 * interface that specifies the functionality for accessing data from a data
 * base. It may look like a round-about way to do the operation. This interface
 * is designed this way to ensure that the handle's life-cycle is controlled by
 * the driver, and not by end-users of this API
 *
 * @author simplity.org
 *
 */
public interface IDbClient {
	/**
	 * Go ahead and access the data base using the handle to the db.
	 *
	 * @param dbHandle
	 *            non-null handle. its type depends on the type of access that
	 *            is asked for. this handle SHOULD NOT be used after this method
	 *            returns. dbHandle will be closed by the API once the control
	 *            returns from this method
	 * @return true if all ok. false in case of issues. this value is
	 *         significant when accessType is TRANSACTION. The transaction is
	 *         committed if returned value is true, else it is rolled back.
	 *         returned value is not used in case of other handles.
	 */
	public boolean accessDb(IDbHandle dbHandle);
}
