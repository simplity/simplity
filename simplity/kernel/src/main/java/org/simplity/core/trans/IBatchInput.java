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

package org.simplity.core.trans;

import java.io.IOException;

import javax.jms.JMSException;

import org.simplity.core.service.ServiceContext;

/** @author simplity.org */
public interface IBatchInput {

	/**
	 * gear up for write session. Resources may be acquired, s a closeShop() is
	 * guaranteed after a call to openShop()
	 *
	 * @param ctx
	 * @throws IOException
	 * @throws JMSException
	 */
	public void openShop(ServiceContext ctx) throws IOException, JMSException;

	/**
	 * guaranteed call at the end of processing
	 *
	 * @param ctx
	 */
	public void closeShop(ServiceContext ctx);

	/**
	 * is this class designed to read possibly multiple rows for a given parent
	 * row
	 *
	 * @return false if at most one row is read for a parent row. And there will
	 *         be calls to read() and not to readForParent(); If true, user of
	 *         this class initially calls getKeyValue() for a parent, and will
	 *         invoke readForPrent();
	 */
	public boolean possiblyMultipleRowsPerParent();

	/**
	 * read an input row.
	 *
	 * @param ctx
	 * @return false means the row was not read. Either there is no row for the
	 *         current parent, or EOF.
	 * @throws InvalidRowException
	 * @throws Exception
	 */
	public boolean inputARow(ServiceContext ctx)
			throws Exception, InvalidRowException;

	/**
	 * read an input row for the specified parent key. This key is fetched with
	 * an earlier call to getParentKeyValue()
	 *
	 * @param parentKey
	 * @param ctx
	 * @return false means the row was not read. Either there is no row for the
	 *         current parent, or EOF.
	 * @throws InvalidRowException
	 * @throws Exception
	 */
	public boolean inputARow(String parentKey, ServiceContext ctx)
			throws Exception, InvalidRowException;

	/**
	 * get parent key value as designed by this object.
	 *
	 * @param ctx
	 * @return String, possibly concatenated as values of individual key parent
	 *         key fields.
	 */
	public String getParentKeyValue(ServiceContext ctx);

	/**
	 * @return actual name of input file, if relevant. null if there is no such
	 *         file
	 */
	public String getFileName();
}
