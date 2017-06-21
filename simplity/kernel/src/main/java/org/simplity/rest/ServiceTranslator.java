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

package org.simplity.rest;

import org.simplity.json.JSONObject;

/**
 * interface to be used to develop an installation specific mapper/translator
 * between Swagger defined operations, and service implementations
 *
 * @author simplity.org
 *
 */
public interface ServiceTranslator {

	/**
	 * translate client request into name/format that this server understands
	 *
	 * @param serviceName
	 * @param params
	 *            input parameters. We may alter the contents to suit the server
	 * @return service name that the server understands
	 */
	public String translateInput(String serviceName, JSONObject params);

	/**
	 * translate output parameters from service implementation output to the desired output as per OPen Api
	 * @param serviceName
	 * @param params
	 */
	public void translateOutput(String serviceName, JSONObject params);
}
