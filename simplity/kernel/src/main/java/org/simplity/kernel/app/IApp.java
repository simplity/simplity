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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.kernel.app;

import java.util.List;
import java.util.Map;

/**
 * Represents an app that has the functionality to respond to request for a set
 * of services.
 *
 *
 * @author simplity.org
 */
public interface IApp {
	/**
	 * each app has a unique id
	 *
	 * @return unique id of this app
	 */
	public String getAppId();

	/**
	 * configure the app, and get ready for a long day of serving requests. a
	 * shutShop() call is expected for this to release any resources being held
	 * open
	 *
	 * @param params
	 *            initial parameters
	 * @param messages
	 *            list to which error messages are added in case the app could
	 *            not be configured properly.
	 * @return true if all ok. false in case of any difficulty in setting up the
	 *         app, and the messages are added to the list
	 */
	public boolean openShop(Map<String, String> params, List<String> messages);

	/**
	 * main RPC into the server
	 *
	 * @param request
	 * @param response
	 */
	public void serve(IServiceRequest request, IServiceResponse response);

	/**
	 * shutdown is to be called by the app manager for a graceful shutdown. app
	 * may keep some resources open etc..
	 */
	public void closeShop();
}
