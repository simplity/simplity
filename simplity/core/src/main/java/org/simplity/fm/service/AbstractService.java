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

import org.simplity.fm.ApplicationError;
import org.simplity.fm.IForm;
import org.simplity.fm.Message;
import org.simplity.fm.MessageType;
import org.simplity.fm.http.LoggedInUser;

/**
 * a service that has no additional work (other than the validation of input
 * that is already done by <code>IForm</code>
 * 
 * @author simplity.org
 *
 */
public abstract class AbstractService implements IService {
	protected static final String MSG_NOT_AUTHORIZED = null;
	protected static final String MSG_INTERNAL_ERROR = null;

	/**
	 * null if no input is expected
	 */
	protected Class<IForm> inputFormClass;

	/**
	 * null if no output is expected
	 */
	protected Class<IForm> outputFormClass;

	@Override
	public IForm getInputForm() {
		if (this.inputFormClass == null) {
			return null;
		}
		try {
			return this.inputFormClass.newInstance();
		} catch (Exception e) {
			throw new ApplicationError("Unable to create an instance of class " + this.inputFormClass.getName()
					+ " Error:" + e.getMessage());
		}
	}

	@Override
	public IForm getOutputForm() {
		if (this.outputFormClass == null) {
			return null;
		}
		try {
			return this.outputFormClass.newInstance();
		} catch (Exception e) {
			throw new ApplicationError("Unable to create an instance of class " + this.outputFormClass.getName()
					+ " Error:" + e.getMessage());
		}
	}

	@Override
	public ServiceResult execute(LoggedInUser user, IForm inputForm, OutputStream outs) {
		String key = inputForm.getDocumentId();
		Message message = null;
		if (this.hasAccess(user, key)) {
			try {
				return this.processForm(user, inputForm, outs);
			} catch (Exception e) {
				message = Message.getGenericMessage(MessageType.Error, MSG_INTERNAL_ERROR, null, null, 0);
			}
			message = Message.getGenericMessage(MessageType.Error, MSG_NOT_AUTHORIZED, null, null, 0);
		}
		Message[] msgs = { message };
		return new ServiceResult(msgs, false);
	}

	/**
	 * let the concrete service process the form and return its result
	 * @param user non-null logged in user
	 * @param inputForm null if this service is not expecting any input
	 * @throws Exception general catch-all
	 * @return service result
	 */
	protected abstract ServiceResult processForm(LoggedInUser user, IForm inputForm,  OutputStream outs) throws Exception;

	/**
	 * let the concrete service check if the user has access to this form
	 * 
	 * @param user
	 * @param key
	 * @return true if ok, false if user has no access to this document
	 */
	protected abstract boolean hasAccess(LoggedInUser user, String key);
}
