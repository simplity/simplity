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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.kernel.test;

/**
 * Data structure to hold all attributes of a test result
 *
 * @author simplity.org
 */
public class TestResult {
	/**
	 * header fields for the row of data toRow() would return
	 */
	public static final String[] HEADR = {
			"serviceName", "testCaseName", "millis", "cleared", "errorMessage"
	};
	/** service that we tested */
	private final String serviceName;
	/** test case that we ran */
	private final String testCaseName;

	/** number of milliseconds that the service took to respond back */
	private final int millis;

	/** error message. null implies that the service succeeded */
	private final String errorMessage;

	/**
	 * construct with all its attributes
	 *
	 * @param serviceName
	 * @param testCaseName
	 * @param millis
	 * @param errorMessage
	 */
	public TestResult(String serviceName, String testCaseName, int millis, String errorMessage) {
		this.serviceName = serviceName;
		this.testCaseName = testCaseName;
		this.millis = millis;
		this.errorMessage = errorMessage;
	}

	/**
	 * did this test case cleared it?
	 *
	 * @return true if the service cleared the test, false if it failed
	 */
	public boolean cleared() {
		return this.errorMessage == null;
	}

	/**
	 * @return an array of field values. They are in the same order as
	 *         TestResult.HEADER
	 */
	public String[] toRow() {
		String[] row = {
				this.serviceName,
				this.testCaseName,
				this.millis + "",
				(this.errorMessage == null) + "",
				this.errorMessage
		};
		return row;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.testCaseName
				+ " for service "
				+ this.serviceName
				+ " has msg = "
				+ this.errorMessage;
	}
}
