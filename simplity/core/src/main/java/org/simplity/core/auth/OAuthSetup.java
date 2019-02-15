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
 * Open Authentication Set up. Work in progress.
 *
 * @author simplity.org
 *
 */
public class OAuthSetup {
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
	String checkTokenUrl;

	/**
	 *
	 * @return check token URL
	 */
	public String getCheckTokenUrl() {
		return this.checkTokenUrl;
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

	/**
	 * called by <code>Application</code> before using it
	 *
	 * @return error message in case any error in configuring. null if all OK.
	 */
	public String configure() {
		return "Oauth is not yet implemented";
	}
}
