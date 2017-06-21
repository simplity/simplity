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
 * holds installation specific parameters for this Rest
 *
 * @author simplity.org
 *
 */
public class RestContext {
	private static RestContext instance = new RestContext();
	/**
	 * @return the instance
	 */
	public static RestContext getContext() {
		return instance;
	}

	/**
	 * should the body parameter be retained as an object, or should the content
	 * be extracted as fields/objects at the top of body. This default can be
	 * over-ridden at the swagger operation object.
	 */
	boolean retainBodyAsObject = false;

	/**
	 * should we ignore input parameters specifications, if any at operation? If
	 * this is set to true, then we assume that the service implementation
	 * handles that task
	 */
	boolean acceptAllData = false;

	/**
	 * should we ignore parameter specification at for response in swagger
	 * document and send all data coming from service?
	 */
	boolean sendAllData = false;

	/**
	 * http response code to be set when a service returns with success, and the
	 * operation spec can not determine response code.
	 */
	String defaultSuccessResponseCode = null;

	/**
	 * http response code to be set when a service returns with failure, and the
	 * operation spec can not determine response code.
	 */
	String defaultFailureResponseCode = null;

	/**
	 * mapping services and input/output between rest definitions and service
	 * implementations
	 */
	ServiceTranslator serviceTranslator = null;

	/**
	 * meant for auto-loading. Should not be used by outsiders
	 */
	public RestContext() {
		//
	}
	/**
	 * instantiate and set parameters from json
	 * @param json that has values of all paramaters
	 */
	public RestContext(JSONObject json) {
		//
	}

	/**
	 * @return the defaultFailureResponseCode
	 */
	public String getDefaultFailureResponseCode() {
		return this.defaultFailureResponseCode;
	}

	/**
	 * @return the defaultSuccessResponseCode
	 */
	public String getDefaultSuccessResponseCode() {
		return this.defaultSuccessResponseCode;
	}

	/**
	 * @return the serviceTranslator
	 */
	public ServiceTranslator getServiceTranslator() {
		return this.serviceTranslator;
	}

	/**
	 * @return the acceptAllData
	 */
	public boolean acceptAllData() {
		return this.acceptAllData;
	}

	/**
	 * @return the retainBodyAsObject
	 */
	public boolean retainBodyAsObject() {
		return this.retainBodyAsObject;
	}

	/**
	 * @return the sendAllData
	 */
	public boolean sendAllData() {
		return this.sendAllData;
	}
}
