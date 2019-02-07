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

package org.simplity.kernel.app.internal;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.app.AppUser;
import org.simplity.kernel.value.Value;

/**
 * An example of App specific extension of AppUser. We are adding just one more
 * field as demo
 *
 * @author simplity.org
 *
 */
public class ExampleUser extends AppUser{
	/**
	 * user type that is used to decide privileges
	 */
	public final Value userType;

	/**
	 * DO NOT USE THIS. This constructor is defined for syntactic reason.
	 * @param userId
	 */
	public ExampleUser(Value userId){
		super(userId);
		throw new ApplicationError("ExampleUser should be constructed with all fields");
	}
	/**
	 * user with id and type
	 * @param userId
	 * @param userType
	 */
	public ExampleUser(Value userId, Value userType) {
		super(userId);
		this.userType = userType;
	}
}
