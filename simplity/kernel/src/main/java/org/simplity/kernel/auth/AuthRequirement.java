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

import java.util.ArrayList;
import java.util.List;

import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;

/**
 * immutable data structure that has all parameters for a specific auth scheme
 * element.
 *
 * @author simplity.org
 *
 */
public class AuthRequirement {
	private final String authName;
	private final String[] scopeNames;

	/**
	 * instantiate this immutable data structure
	 *
	 * @param authName
	 * @param scopeNames
	 */
	public AuthRequirement(String authName, String[] scopeNames) {
		this.authName = authName;
		this.scopeNames = scopeNames;
	}

	/**
	 * @return the authName
	 */
	public String getAuthName() {
		return this.authName;
	}

	/**
	 * @return the scopeNames
	 */
	public String[] getScopeNames() {
		return this.scopeNames;
	}

	/**
	 * parse a authRequirement object from a swagger document into an array of
	 * AuthRequirement objects
	 *
	 * @param auths
	 *            authRequirement object from a swagger document
	 * @return null if auths is null. array of parsed AuthRequirement objects
	 */
	public static AuthRequirement[] parse(JSONArray auths) {
		if (auths == null) {
			return null;
		}
		List<AuthRequirement> result = new ArrayList<AuthRequirement>();
		for (Object obj : auths) {
			JSONObject json = (JSONObject) obj;
			String name = json.names().getString(0);
			JSONArray authJson = (JSONArray) json.get(name);
			String[] scopeNames = authJson.join(",").split(",");
			AuthRequirement authResult = new AuthRequirement(name, scopeNames);
			result.add(authResult);
		}
		
		return result.toArray(new AuthRequirement[0]);
	}
}
