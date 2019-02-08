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

package org.simplity.core.gateway;

import org.simplity.core.service.ServiceContext;

/**
 * Gateway to all services from an external application. One instance of gateway
 * would handle services for one specific application.
 *
 *
 * This is an abstract class rather than an interface because we have designed
 * this as a sub-part of application component. That is, the attributes of a
 * specific gateway are loaded at run time from application.xml
 *
 *
 * Most agents may require a method to prepare the connection before the serve()
 * method and a method to wind-up after the serve method. We have not formally
 * defined them at this stage. We may have to revisit this design and re-factor
 * for this purpose based on actual usage scenarios.
 *
 * @author simplity.org
 *
 */
public abstract class ServiceGateway {
	/**
	 * unique name of external application, for which this gateway is set up
	 */
	String applicationName;

	/**
	 * @param serviceName
	 *            name of service to be requested/executed. Could be null for
	 *            agents that do not require this parameter
	 * @param ctx
	 *            service context in which the caller service is executing. in
	 *            case the agent needs something from the context
	 * @return an assistant who knows how to invoke an external service through
	 *         this gateway. ApplcationError is thrown if this service can not
	 *         be requested through this gateway
	 */
	public abstract IServiceAssistant getAssistant(String serviceName, ServiceContext ctx);

	/**
	 * called once after loading its attributes. This method is called once
	 * before repeatedly calling serve()
	 */
	public abstract void getReady();

	/**
	 * called before graceful shutdown of app. Opportunity for this instance to
	 * release any resource
	 */
	public abstract void shutdown();

	/**
	 *
	 * @return unique name associated with this gateway
	 */
	public String getApplicationName() {
		return this.applicationName;
	}
}
