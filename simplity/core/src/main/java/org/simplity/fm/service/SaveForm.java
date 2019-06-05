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
import org.simplity.fm.io.DataStore;

/**
 * Simple service that just saves the form with no saves the form received from
 * 
 * @author simplity.org
 *
 */
public class SaveForm extends AbstractService {

	/**
	 * a simple service that just saves the form. output form is null;
	 * 
	 * @param inputForm
	 */
	public SaveForm(Class<IForm> inputForm) {
		this.inputFormClass = inputForm;
	}

	@Override
	public ServiceResult processForm(LoggedInUser user, IForm inputForm,  OutputStream respStream) throws Exception {
		try (OutputStream outs = DataStore.getStore().getOutStream(inputForm.getDocumentId())) {
			inputForm.serializeAsJson(outs);
		}
		return new ServiceResult(null, true);
	}

	@Override
	protected boolean hasAccess(LoggedInUser user, String key) {
		// TODO implement the logic to check if this user has write access to
		// this form
		return true;
	}

}
