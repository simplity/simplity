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

package org.simplity.core.test;

import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationUtil;
import org.simplity.core.util.JsonUtil;
import org.simplity.json.JSONObject;

/**
 * represents a JSON object. Has attributes and values. Allows setting
 * assertions on such an item
 */
public class InputItem {

	/**
	 * qualified name to get the row data. 0 for first row, a.2 to get third row
	 * of the array with attribute a etc..
	 */
	@FieldMetaData(isRequired = true)
	String itemSelector;
	/**
	 * fields specified for this row. This is a must. If you want to assert that
	 * this should not be there, you may as well define that as a Field
	 */
	@FieldMetaData(isRequired = true)
	InputField[] inputFields;

	/**
	 * is this component designed ok?
	 *
	 * @param vtx
	 */
	void validate(IValidationContext vtx) {
		ValidationUtil.validateMeta(vtx, this);
		if (this.inputFields != null) {
			for (InputField field : this.inputFields) {
				field.validate(vtx);
			}
		}
	}

	/**
	 * set values for this item into the root json object. parent objects are
	 * created if required as per selector
	 *
	 * @param json
	 */
	void setInputValues(JSONObject json, TestContext ctx) {
		Object obj = JsonUtil.getObjectValue(this.itemSelector, json);
		for (InputField field : this.inputFields) {
			field.setInputValue(obj, ctx);
		}
	}
}
