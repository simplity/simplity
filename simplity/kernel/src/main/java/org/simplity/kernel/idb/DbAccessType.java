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
 *
 * @author simplity.org
 *
 */
public enum DbAccessType {
	/**
	 * read-only. no update.
	 */
	READ_ONLY,
	/**
	 * read-write but no transaction management. updates are automatically
	 * committed.
	 */
	AUTO_COMMIT,
	/**
	 * read-write within a single transaction boundary. no commit or roll-backs
	 * are possible in-between.
	 */
	SINGLE_TRANS,
	/**
	 * read-write along with control for transactions. commit and roll-backs are
	 * possible. Also, it is the responsibility of the caller to manage
	 * commit/rollback
	 */
	MULTI_TRANS,
	/**
	 * meant for accessing meta-data about the db, like tables columns etc,,
	 */
	META_DATA
}
