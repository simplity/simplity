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

import org.simplity.json.JSONArray;
import org.simplity.kernel.comp.FieldMetaData;
import org.simplity.kernel.comp.IValidationContext;
import org.simplity.kernel.comp.ValidationUtil;
import org.simplity.kernel.util.JsonUtil;

/**
 * represents a field to be provided as input to a service
 *
 * @author simplity.org
 */
public class OutputField {
	/** field name. Qualified name is relative to its parent */
	@FieldMetaData(isRequired = true)

	String fieldSelector;
	/** field value. $variableName to get value from test context */
	String fieldValue;

	/** do you want test for non existence of this field? */
	@FieldMetaData(alternateField = "fieldValue")
	boolean shouldBeAbsent;

	/**
	 * @param vtx
	 */
	void validate(IValidationContext vtx) {
		ValidationUtil.validateMeta(vtx, this);
	}

	/**
	 * does this json data match our test expectations?
	 *
	 * @param json
	 * @return error message if test fails, null if all OK!!
	 */
	String match(Object json, TestContext ctx) {
		Object val = JsonUtil.getValue(this.fieldSelector, json);
		if (this.fieldValue != null && this.fieldValue.isEmpty() == false) {
			/*
			 * we expect some specific value for this field
			 */
			if (val == null) {
				return "No value for " + this.fieldSelector;
			}

			String thisValue = this.fieldValue;
			if (this.fieldValue.charAt(0) == '$') {
				thisValue = ctx.getValue(this.fieldValue.substring(1)).toString();
			}

			if (thisValue.equals(val.toString()) == false) {
				return "Expected a value of "
						+ thisValue
						+ " for field "
						+ this.fieldSelector
						+ " but we got "
						+ val;
			}
		}
		boolean valMissing = false;
		if (val != null) {
			if (val instanceof JSONArray) {
				if (((JSONArray) val).length() == 0) {
					valMissing = true;
				}
			} else if (val.equals(null)) {
				valMissing = true;
			}
		}
		if (this.shouldBeAbsent) {
			if (val != null && valMissing == false) {
				return "Found a value of "
						+ val.toString()
						+ " for field "
						+ this.fieldSelector
						+ " but we are not looking for a value at all.";
			}
		}
		if (this.fieldValue == null && this.shouldBeAbsent == false && (val == null || valMissing)) {

			return "Response did not contain an expected field named " + this.fieldSelector;
		}

		return null;
	}
}
