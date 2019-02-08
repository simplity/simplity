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

package org.simplity.core.job;

import java.util.Map;

import org.simplity.core.ApplicationError;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;

/**
 * represents a field to be provided as input to a service
 *
 * @author simplity.org
 */
public class InputField {

	/** field name. */
	String name;
	/** field value. */
	String value;

	/** value type */
	ValueType valueType;
	/*
	 * value parsed into object
	 */
	private Value valueObject;

	/** */
	public void getReady() {
		if (this.name == null) {
			throw new ApplicationError("name is a required attribute for an input field");
		}
		if (this.value == null) {
			throw new ApplicationError("value is required for an input field");
		}
		if (this.valueType == null) {
			throw new ApplicationError("valueType is required for an input field");
		}
		this.valueObject = Value.parseValue(this.value, this.valueType);
		if (Value.isNull(this.valueObject)) {
			throw new ApplicationError("Field " + this.name + " is of type " + this.valueType
					+ " and has an invalid value of " + this.value);
		}
	}

	/**
	 * @param fields
	 */
	public void addToMap(Map<String, Object> fields) {
		fields.put(this.name, this.valueObject);

	}
}
