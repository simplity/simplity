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

package org.simplity.rest.param;

/**
 * represents a parameter type as in a swagger document
 * @author simplity.org
 *
 */
public enum ParameterFormat {
	/**
	 * sequence of octets, not implemented
	 */
	BINARY,
	/**
	 * base64 encoded characters, not implemented
	 */
	BYTE,
	/**
	 * date only
	 */
	DATE,
	/**
	 * date with time
	 */
	DATETIME,
	/**
	 * number greater that float
	 */
	DOUBLE,
	/**
	 * number
	 */
	FLOAT,
	/**
	 * integer
	 */
	INT32,
	/**
	 * long integer
	 */
	INT64,

	/**
	 * password
	 */
	PASSWORD;
}
