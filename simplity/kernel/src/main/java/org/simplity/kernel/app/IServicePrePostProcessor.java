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

package org.simplity.kernel.app;

import org.simplity.kernel.service.ServiceContext;

/**
 * @author simplity.org
 *
 *         user-hook to carry out any common task before nd after invoking a
 *         service
 */
public interface IServicePrePostProcessor {

	/**
	 * method invoked by service agent after identifying the service, but before
	 * extracting input data.
	 *
	 * @param request
	 * @param response
	 * @param ctx
	 * @return true if all ok and the process should continue. false if we
	 *         shoudl abandon the service and get back with the response
	 */
	public boolean beforeInput(IServiceRequest request, IServiceResponse response, ServiceContext ctx);

	/**
	 * method invoked by service agent after extracting all input data as per
	 * service input specification, but before actually calling the service.
	 *
	 * @param request
	 * @param response
	 * @param ctx
	 * @return true if all ok and the process should continue. false if the
	 *         service should not be called. suitable message data is to be set
	 *         in context/response
	 */
	public boolean beforeService(IServiceRequest request, IServiceResponse response, ServiceContext ctx);

	/**
	 * method invoked by service agent after the service returns but before
	 * output is extracted from context.
	 *
	 * @param response
	 * @param ctx
	 * @return true if all ok and the process should continue. false if the
	 *         output should not be extracted
	 */
	public boolean afterService(IServiceResponse response, ServiceContext ctx);

	/**
	 * method invoked by service agent after extracting all output data as per
	 * service output specification, but before actually closing the writer.
	 *
	 * @param response
	 * @param ctx
	 */
	public void afterOutput(IServiceResponse response, ServiceContext ctx);
}
