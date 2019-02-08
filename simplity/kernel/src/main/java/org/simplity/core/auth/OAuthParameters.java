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
package org.simplity.core.auth;

/**
 * dummy data structure to hold parameters for OAuth
 *
 * @author simplity.org
 *
 */
public class OAuthParameters {
	/*
	 * all attributes are made package private for them to be loaded by the
	 * oader
	 */
	/**
	 * client id
	 */
	String clientId;
	/**
	 * typically password
	 */
	String clientSecret;
	/**
	 * check token URL
	 */
	String checkTokenURL;

	/**
	 *
	 * @return check token URL
	 */
	public String getCheckTokenURL() {
		return this.checkTokenURL;
	}

	/**
	 *
	 * @return clientId
	 */
	public String getClientId() {
		return this.clientId;
	}

	/**
	 *
	 * @return client password
	 */
	public String getClientSecret() {
		return this.clientSecret;
	}
}
