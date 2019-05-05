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

package org.simplity.core.app;

import org.simplity.core.value.Value;

/**
 * Data structure that holds User information. Simplity uses userId. App should
 * extend this class to include all the data about the logged in user that are
 * used across services. Typically this set of data is saved in session
 *
 * We have used an immutable data structure design.
 *
 * @author simplity.org
 *
 */
public class AppUser {

	/**
	 * logged-in user id
	 */
	protected final Value userId;

	protected final String token;

	protected final Value tenantId;

	/**
	 * @param userId
	 *            internal id of the logged-in user. (not the login name)
	 * @param tenantId
	 *            null if this is not a multi-tenant one
	 * @param authToken
	 *            with which the user has authenticated. null if this not
	 *            relevant
	 *
	 */
	public AppUser(Value userId, Value tenantId, String authToken) {
		this.userId = userId;
		this.token = authToken;
		this.tenantId = tenantId;
	}

	/**
	 * @return the userId
	 */
	public Value getUserId() {
		return this.userId;
	}

	/**
	 * @return the token with which the user authenticated. null if no
	 *         authentication token was used
	 */
	public String getAuthToken() {
		return this.token;
	}
}
