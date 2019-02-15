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

import org.simplity.core.service.ServiceContext;

/** @author simplity.org */
public interface IBatchOutput {

	/**
	 * gear up for read session. Resources may be acquired, s a closeShop() is
	 * guaranteed after a call to openShop()
	 *
	 * @param ctx
	 * @throws Exception
	 */
	public void openShop(ServiceContext ctx) throws Exception;

	/**
	 * guaranteed call at the end of processing
	 *
	 * @param ctx
	 */
	public void closeShop(ServiceContext ctx);

	/**
	 * output a row.
	 *
	 * @param ctx
	 * @return true if the row was actually output. false otherwise
	 * @throws Exception
	 */
	public boolean outputARow(ServiceContext ctx) throws Exception;
}
