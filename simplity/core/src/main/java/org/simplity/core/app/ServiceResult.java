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

/**
 * @author simplity.org
 *
 */
public enum ServiceResult {
	/**
	 * bingo
	 */
	ALL_OK(200),
	/**
	 * security related issues
	 */
	INSUFFICIENT_PRIVILEGE(405),
	/**
	 * security related issues
	 */
	AUTH_REQUIRED(401),
	/**
	 * input data has errors. input data has violated input specification
	 * constraints
	 */
	INVALID_DATA(406),
	/**
	 * requested operation is not valid
	 */
	INVALID_OPERATION(405),
	/**
	 * service is not available at this time
	 */
	SCHEDULED_OUTAGE(503),
	/**
	 * server is taking too long, and we have decided not to wait
	 */
	TIME_OUT(408),
	/**
	 * programming/set-up errors or infrastructure failure
	 */
	INTERNAL_ERROR(500),
	/**
	 * we do not serve this item
	 */
	NO_SUCH_SERVICE(405);

	private final int httpStatus;

	private ServiceResult(int httpStatus) {
		this.httpStatus = httpStatus;
	}

	/**
	 *
	 * @return eqvt http status
	 */
	public int getHttpStatus() {
		return this.httpStatus;
	}
}
