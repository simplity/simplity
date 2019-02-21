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

import org.simplity.core.MessageBox;

/**
 * @author simplity.org
 *
 */
public interface IServiceRequest {
	/**
	 * @return the serviceName
	 */
	public String getServiceName();

	/**
	 * @return the userId who requested for this service. null if this is not
	 *         originated from an authenticated user
	 */
	public AppUser getUser();

	/**
	 * message box is a simple mechanism to exchange message between the
	 * requester (client-agent) and a running service, in case it is running on
	 * a different thread, like running as a background job. Objective is to use
	 * it for simple things like conveying status, like % completion. Any
	 * heavier use, though possible, must be done after a careful performance
	 * review
	 *
	 * @return the messageBox
	 */
	public MessageBox getMessageBox();

	/**
	 * Client context typically contains cached information about the
	 * authenticated user to simulate a conversational mode of operation, while
	 * the actual operation is state-less. It is requester's responsibility to
	 * manage the context and provide necessary input for the app that is
	 * state-less. Each request is treated as a fresh one and responded to.
	 *
	 * @return the clientContext
	 */
	public Object getClientContext();

	/**
	 * create an appropriate reader on the underlying payload
	 *
	 * @return null if there is no payload to be read. reader from which the
	 *         data from the payload can be read
	 */
	public IRequestReader getPayloadReader();

	/**
	 *
	 * @param fieldName
	 * @return value of the named field, or null if no such field
	 */
	public Object getFieldValue(String fieldName);

	/**
	 *
	 * @return set containing all the fields. empty set if there are no fields
	 */
	public Map<String, Object> getFieldValues();
}
