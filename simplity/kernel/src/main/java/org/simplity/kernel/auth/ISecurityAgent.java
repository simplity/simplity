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

/**
 * what a security agent should be doing.
 *
 * @author simplity.org
 *
 */
public interface ISecurityAgent {
	/**
	 * there is no standard list of parameters we can prescribe, and hence we
	 * have resorted to this unsafe signature. It is recommended that each
	 * implementation exposes a method with better signature as equivalent of
	 * this, and allow callers to use that after casting.
	 *
	 * @param params
	 * @return true if security is cleared. false means security not cleared.
	 *         when this is false, specific implementation may do other required
	 *         things like redirecting to the authentication site etc..
	 */
	public boolean securityCleared(Object... params);

}
