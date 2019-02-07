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

package org.simplity.kernel.comp;

/**
 * data structure that holds all details of a validation message
 *
 * @author simplity.org
 *
 */
public class ValidationMessage {
	/**
	 * valid value for field severity. message is informational. could be an
	 * unusual setting.
	 */
	public static final int SEVERITY_INFO = 0;
	/**
	 * valid value for field severity. component will load, but may not work as
	 * expected
	 */
	public static final int SEVERITY_WARNING = 0;
	/**
	 * valid value for field severity. error because of which component will
	 * fail to load
	 */
	public static final int SEVERITY_ERROR = 0;

	/**
	 * one of the values specified as constants
	 */
	public final int severity;

	/**
	 * object that raised this message, optional.
	 */
	public Object refObject;
	/**
	 * name of attribute value of which is the primary reason for this message.
	 * null in case this message is not associated with any attribute
	 */
	public final String fieldName;
	/**
	 * message text in English. non-null
	 */
	public final String messageText;

	/**
	 * resource for which this message is meant for.
	 */
	public String resourceId;
	/**
	 * line number in the resource. 0 if this is not set. lne number starts with
	 * 1.
	 */
	public int lineNo;
	/*
	 * planned for future for i18n
	 *
	 */
	// public String id;
	// public String[] params;

	/**
	 * constructor with all attributes
	 *
	 * @param object
	 *
	 * @param severity
	 * @param message
	 * @param fieldName
	 */
	public ValidationMessage(Object object, int severity, String message, String fieldName) {
		this.refObject = object;
		this.severity = severity;
		this.messageText = message;
		this.fieldName = fieldName;
	}
}
