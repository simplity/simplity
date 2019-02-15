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

package org.simplity.core.trans;

import org.simplity.core.app.internal.CommonCodeValidator;
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.value.Value;

/**
 * we expect that the app provides its commonCodeValidator in case such a
 * concept is used in the project. Also, since this is a fairly common/frequent
 * action, we expect that the designer would consider separate caching
 * mechanism, and ultimately use a different data base connection than the one
 * opened specifically for the service. Hence this action is designed as a
 * non-db action
 *
 * @author simplity.org
 *
 */
public class ValidateCommonCode extends AbstractNonDbAction {

	/**
	 * code name/type. for example country. This is known at design time, and
	 * hence this is the actual value, and not the field name that will have
	 * value at run time.
	 *
	 */
	@FieldMetaData(isRequired = true)
	String codeName;

	/** field name to be validated as a common code field value. */
	@FieldMetaData(isRequired = true)
	String inputFieldName;

	/**
	 * optional field name in the context to which the description of this code,
	 * if valid, is to be copied to. Apps may or may not design description for
	 * codes
	 */
	String outputFieldName;

	@Override
	protected boolean act(ServiceContext ctx) {
		Value codeValue = ctx.getValue(this.inputFieldName);
		if (Value.isNull(codeValue)) {
			return false;
		}
		return CommonCodeValidator.isValid(this.codeName, codeValue, this.outputFieldName, ctx);
	}
}
