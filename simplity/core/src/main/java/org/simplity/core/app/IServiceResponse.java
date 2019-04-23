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

package org.simplity.core.app;

import java.util.Map;

import org.simplity.core.msg.FormattedMessage;

/**
 * @author simplity.org
 *
 */
public interface IServiceResponse {
	/**
	 * @param result
	 *            the result of service execution
	 * @param millis
	 *            number of milliseconds taken by the actual service to process
	 *            this request. This time may not include the time used by the
	 *            infrastructure to bring-up the service etc..
	 */
	public void setResult(ServiceResult result, int millis);

	/**
	 * set messages. If the result is not ALL_OK, then at least one error
	 * message explaining the failure must be included.
	 *
	 * @param messages
	 */
	public void setMessages(FormattedMessage[] messages);

	/**
	 * @param fieldName
	 *            non-null name of session field field is replaced if already is
	 *            set
	 * @param fieldValue
	 *            non-null value of session field
	 */
	public void setSessionField(String fieldName, String fieldValue);

	/**
	 * @return true if response is linked to a writer. false if
	 */
	public boolean isStreaming();

	/**
	 *
	 * @return we use only json and xml. hence it is a boolean
	 */
	public boolean isXml();

	/**
	 * get a response writer
	 *
	 * @param responseIsAnArray
	 *            true if an array is to be written out, not an object
	 * @return non-null response writer for this response
	 */
	public IResponseWriter getPayloadWriter(boolean responseIsAnArray);

	/**
	 * @return result of this service execution
	 */
	public ServiceResult getServiceResult();

	/**
	 * @return execution time in number of milliseconds
	 */
	public int getExecutionTime();

	/**
	 * session fields are to be used by the client agent to set the Conversation
	 * context. These fields are to be added to every subsequent request. If the
	 * value of a field is null, then it means that the field is to be removed
	 * from the session context
	 *
	 * @return fields to be used as session context fields. null or empty, if
	 *         session context is not affected by this service
	 */
	public Map<String, Object> getSessionFields();

	/**
	 * @return the messages
	 */
	public FormattedMessage[] getMessages();
}
