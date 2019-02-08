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

import java.util.Map;

import org.simplity.core.ApplicationError;
import org.simplity.core.service.ServiceContext;

/**
 * static class to manage gateways
 *
 * @author simplity.org
 *
 */
public class Gateways {

	/**
	 * configured gateways
	 */
	private static Map<String, ServiceGateway> allGateways;

	/**
	 * set gateways. as of now this is happening at application boot time. Refer
	 * to <code>Application</code>,
	 *
	 * @param gateways
	 */
	public static void setGateways(Map<String, ServiceGateway> gateways) {
		allGateways = gateways;
		for (ServiceGateway gateway : gateways.values()) {
			gateway.getReady();
		}
	}

	/**
	 * get the right agent who knows how to handle service request to the
	 * desired server
	 *
	 * @param applicationName
	 *            unique external application name for which we want the
	 *            gateway. non-null. A gateway must be set-up for this
	 *            application.
	 * @param serviceName
	 *            name of service to be requested from the application.
	 *            non-null. an external-service-component must be defined with
	 *            this name.
	 * @param ctx
	 *            service context in which the caller is executing. non-null;
	 * @return assistant to be used to execute this external service. non-null.
	 *         ApplicationError is thrown in case the service or gateway is not
	 *         set-up is if no gateway is set up for this application
	 */
	public static IServiceAssistant getAssistant(String applicationName, String serviceName, ServiceContext ctx) {
		ServiceGateway gateway = allGateways.get(applicationName);
		if (gateway == null) {
			throw new ApplicationError("Gateway is not set up for application " + applicationName);
		}
		return gateway.getAssistant(serviceName, ctx);
	}
}
