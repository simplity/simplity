/*
 * Copyright (c) 2019 simplity.org
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

package org.simplity.fm.service;

import java.io.OutputStream;

import org.simplity.fm.IForm;
import org.simplity.fm.http.LoggedInUser;

/**
 * Interface for service. Also, the instance is expected to be re-usable, and
 * thread-safe. (immutable). Singleton pattern is suitable or this.
 * 
 * @author simplity.org
 *
 */
public interface IService {
	/**
	 * 
	 * @return data structure for input. null if it is designed not to take any
	 *         input.
	 */
	public IForm getInputForm();

	/**
	 * 
	 * @return data structure for output. null if it is designed not to return
	 *         any data
	 */
	public IForm getOutputForm();

	/**
	 * 
	 * @param user
	 *            logged-in user who has requested this service. This can be
	 *            used to check whether the user is authorized to deal with the
	 *            document/form being requested
	 * @param inputForm
	 *            instance that was returned from a call to
	 *            <code>Iservie.getInputForm()</code>
	 * @param outs stream to which the output can be written to
	 * @return non-null service result.
	 */
	public ServiceResult execute(LoggedInUser user, IForm inputForm, OutputStream outs);
	
}
