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

package org.simplity.core.idb;

import java.sql.ResultSet;

/**
 * functions to be implemented by a call-back object to read each row of a
 * result set. <code>ResultSet</code> is a db-related object, and hence its
 * propagation in to other parts of the code is to be minimized. We recommend
 * that the concrete classes implement this interface and provide other
 * interfaces, not involving <code>ResuktSet</code> to suit application
 * developers
 *
 * @author simplity.org
 *
 */
public interface IResultSetReader {
	/**
	 *
	 * @param resultSet
	 *            result set, as returned by a sql from a JDBC driver. Should be
	 *            utilized only for reading
	 * @return number of rows read
	 */
	public int read(ResultSet resultSet);
}
