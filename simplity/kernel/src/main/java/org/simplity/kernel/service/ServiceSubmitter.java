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

package org.simplity.kernel.service;

import org.simplity.kernel.app.Application;
import org.simplity.kernel.app.internal.ServiceRequest;
import org.simplity.kernel.app.internal.ServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * convenient class that can be used to run a service in the background
 *
 * @author simplity.org
 */
public class ServiceSubmitter implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(ServiceSubmitter.class);

	private final ServiceRequest request;
	private final ServiceResponse response;

	/**
	 * * instantiate with required attributes
	 *
	 * @param req
	 * @param resp
	 */
	public ServiceSubmitter(ServiceRequest req, ServiceResponse resp) {
		this.request = req;
		this.response = resp;
	}

	@Override
	public void run() {
		Application app = Application.getActiveInstance();
		logger.info("Service {} is now running in its own thread", this.request.getServiceName());
		app.serve(this.request, this.response);
	}
}
