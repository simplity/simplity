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

package org.simplity.kernel.app;

import org.simplity.kernel.service.ServiceContext;
import org.simplity.kernel.value.Value;

/**
 * utility class that validates a code value against possible values for that
 * code type.
 *
 * @author simplity.org
 *
 */
public interface ICommonCodeValidator {
	/**
	 * is this code valid for this type?
	 *
	 * @param codeType
	 *            non-null. for example "country" to validate a state, if the
	 *            state names are grouped under common code :country"
	 * @param codeValue
	 *            non-null.
	 * @param outputFieldName
	 *            optional. name of the field. if a description is available for
	 *            this code, then it is set to this field in the service context
	 * @param ctx
	 *            service context in which this validation is required
	 * @return true if it is a valid value. false otherwise
	 */
	public boolean isValid(String codeType, String codeValue, String outputFieldName, ServiceContext ctx);

	/**
	 * is this code valid for this type?
	 *
	 * @param codeType
	 *            non-null. for example "country" to validate a state, if the
	 *            state names are grouped under common code :country"
	 * @param codeValue
	 *            non-null
	 * @param outputFieldName
	 *            optional. name of the field. if a description is available for
	 *            this code, then it is set to this field in the service context
	 * @param ctx
	 *            service context in which this validation is required
	 * @return true if it is a valid value. false otherwise
	 */
	public boolean isValid(String codeType, Value codeValue, String outputFieldName, ServiceContext ctx);
}
