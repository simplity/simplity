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

package org.simplity.service;

/**
 * @author simplity.org
 *
 */
public interface ServiceCacheManager {
	/**
	 * get a cached response
	 *
	 * @param inputData
	 * @return response text to be sent to client. null if this service is not
	 *         available in cache. Empty string if the cacher would like to
	 *         cache the response on its way back from server.
	 */
	public ServiceData respond(ServiceData inputData);

	/**
	 * cache a response from server. This is called if a previous call to
	 * respond() would have returned an empty string indicating desire to cache
	 * this response
	 *
	 * @param inData
	 * @param outData
	 */
	public void cache(ServiceData inData, ServiceData outData);

	/**
	 * remove/invalidate any cache for this service
	 * 
	 * @param serviceName
	 */
	public void invalidate(String serviceName,ServiceData inData);

}
