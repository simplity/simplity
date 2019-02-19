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

package org.simplity.core.dt;

/**
 * data types that are built-in and used internally by Simplity. These are
 * provided through a resoure file named simplity.xml inside org.simplity
 * package
 *
 * @author simplity.org
 *
 */
public class BuiltInDataTypes {
	private BuiltInDataTypes() {
		//
	}

	/**
	 * general text
	 */
	public static final String TEXT = "_text";

	/**
	 * calendar date. date wit no time.
	 */
	public static final String DATE = "_date";

	/**
	 * instance of time (date with time)
	 */
	public static final String DATE_TIME = "_dateTime";

	/**
	 * integer
	 */
	public static final String NUMBER = "_number";

	/**
	 * like 1234.456
	 */
	public static final String DECIMAL = "_decimal";

	/**
	 * like 1234.456
	 */
	public static final String BOOLEAN = "_boolean";

	/**
	 * qualified name of components.
	 */
	public static final String ENTITY_NAME = "_entityName";

	/**
	 * of the form name1,name2,name3....
	 */
	public static final String ENTITY_LIST = "_entityList";

	/**
	 * not used directly by simplity.mapped to string in java
	 */
	public static final String CLOB = "_clob";

	/**
	 * blob. not used by simplity directly. file-attachment-storage is a known,
	 * but not-recommended use
	 */
	public static final String BLOB = "_blob";

	/**
	 * timestamp provided by teh rdbms. typically an int of nano-seconds. can be
	 * converted into date
	 */
	public static final String TIMESTAMP = "_timestamp";
}
