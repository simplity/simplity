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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.kernel.auth;

import org.simplity.json.JSONObject;

/**
 * types of standard authentications we understand
 *
 * @author simplity.org
 *
 */
public enum AuthType {
	/**
	 * basic
	 */
	BASIC {
		@Override
		public ISecurityAgent getAgent(JSONObject authSpec) {
			return new BasicAgent(authSpec);
		}

	},
	/**
	 * API-KEY
	 */
	APIKEY{
		@Override
		public ISecurityAgent getAgent(JSONObject authSpec) {
			return new ApiKeyAgent(authSpec);
		}

	},
	/**
	 * Oauth-2
	 */
	OAUTH2{
		@Override
		public ISecurityAgent getAgent(JSONObject authSpec) {
			return new OAuth2Agent(authSpec);
		}

	};

	/**
	 * get an agent who can manage this authentication for us
	 * @param authSpec specifications specific to this authentication scheme
	 * @return an agent to whom authentication.security can be delegated to
	 */
	public abstract ISecurityAgent getAgent(JSONObject authSpec);


}
