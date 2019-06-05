/*
 * Copyright (c) 2019 simplity.org
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

package org.simplity.fm.http;

/**
 * Data Structure that has attributes that are required for services-request to be completed
 * @author simplity.org
 *
 */
public class LoggedInUser {
	
	/**
	 * get user instance for  pan
	 * @param id unique key for the user
	 * @param token authentication/security token issued by the login service. this is 
	 * @return user for a pan. null if this is not a valid pan
	 */
	public static LoggedInUser newUser(String id, String token) {
		return new LoggedInUser(id, token);
	}
	private final  String userId;
	private final String userToken;
	//TODO: add other attributes that are  needed for service execution

	private LoggedInUser(String id, String token) {
		this.userId = id;
		this.userToken = token;
	}
	/**
	 * @return unique key for this user
	 */
	public String getUserId() {
		return this.userId;
	}
	
	/**
	 * @return security/authentication token associated with this user
	 */
	public String getUserToken() {
		return this.userToken;

	}
}
