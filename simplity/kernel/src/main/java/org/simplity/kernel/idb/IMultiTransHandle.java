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

import java.sql.SQLException;

/**
 * handle that allows db operations across multiple transactions. SHOUDL BE USED
 * SPARINGLY, and by experienced programmers only. Allows caller to manage the
 * transaction processing, that is caller cal commit or rollback multiple times.
 * Typically used by batch-programs that are NOT linked to an end-user active
 * session.
 *
 * @author simplity.org
 *
 */
public interface IMultiTransHandle extends ITransactionHandle {
	/**
	 * commit all updates, and release all locks.
	 * 
	 * @throws SQLException
	 */
	public void commit() throws SQLException;

	/**
	 * roll-back any updates done and release all locks
	 * 
	 * @throws SQLException
	 */
	public void rollback() throws SQLException;
}
