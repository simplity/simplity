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

package org.simplity.core.gateway;

import org.simplity.core.service.ServiceContext;

/**
 * common interface for executing a service using the gateway.
 *
 * The way to execute a service on a gateway largely depends on the protocol and
 * other conventions used by the gateway. There is nothing much to abstract in
 * this. However, we define one interface that we can use to call services
 * either internally, or from other apps that use our design patterns.
 *
 * Callers would generally know the concrete type that is relevant for the
 * gateway and use methods that are specific to that concrete class.
 *
 * @author simplity.org
 *
 */
public interface IServiceAssistant {

	/**
	 * execute this service in this service context. FOr gateways where such a
	 * method is not applicable should throw ApplicationError
	 *
	 * @param ctx
	 *            service context where the caller service is executing
	 * @return true if all OK. false means something went wrong, and the service
	 *         did not execute. Message may or may not added to context, based
	 *         on the actual design of the service.
	 *
	 */
	public boolean execute(ServiceContext ctx);
}
