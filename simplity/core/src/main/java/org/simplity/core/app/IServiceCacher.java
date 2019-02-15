/*
 * Copyright (c) 2016 simplity.org
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

package org.simplity.core.app;

import org.simplity.core.app.internal.ServiceRequest;
import org.simplity.core.app.internal.ServiceResponse;

/**
 * component that manages caching of data output from service
 *
 * @author simplity.org
 *
 */
public interface IServiceCacher {
	/**
	 * get a cached response
	 *
	 * @param request
	 * @param response
	 *
	 * @return if responded from cache. false if this request can is not served
	 *         from cache.
	 */
	public boolean respond(ServiceRequest request, ServiceResponse response);

	/**
	 * cache a response from server.
	 *
	 * /** get a cached response
	 *
	 * @param request
	 * @param response
	 */
	public void cache(ServiceRequest request, ServiceResponse response);

	/**
	 * remove/invalidate any cache for this service
	 *
	 * @param serviceName
	 */
	public void invalidate(String serviceName);

	/**
	 * clear all cache.
	 */
	public void clearAll();
}
