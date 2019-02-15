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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.core.fn;

import org.simplity.core.ApplicationError;
import org.simplity.core.comp.ComponentType;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.data.IFieldsCollection;
import org.simplity.core.value.InvalidValueException;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author simplity.org */
public class SampleFunction implements IFunction {
	private static final Logger logger = LoggerFactory.getLogger(SampleFunction.class);

	private static final String MY_FULL_NAME = "james.bond";
	private static final String MY_NAME = "bond";
	private static final ValueType[] ARG_TYPES = { ValueType.TEXT, ValueType.INTEGER };

	@Override
	public String getSimpleName() {
		return MY_NAME;
	}

	@Override
	public String getQualifiedName() {
		return MY_FULL_NAME;
	}

	@Override
	public void getReady() {
		// ever ready
	}

	@Override
	public void validate(IValidationContext ctx) {
		// always valid
	}

	@Override
	public ComponentType getComponentType() {
		return ComponentType.FUNCTION;
	}

	@Override
	public ValueType getReturnType() {
		return ValueType.BOOLEAN;
	}

	@Override
	public ValueType[] getArgDataTypes() {
		return ARG_TYPES;
	}

	@Override
	public Value execute(Value[] arguments, IFieldsCollection data) {

		logger.info(
				MY_FULL_NAME
						+ " called with "
						+ (arguments == null ? -1 : arguments.length)
						+ " arguments with datat="
						+ data);

		if (data == null) {
			this.oops();
			return null;
		}
		if (arguments == null || arguments.length != 2) {
			this.oops();
			return null;
		}
		Value val1 = arguments[0];
		if (Value.isNull(val1) || val1.getValueType() != ValueType.TEXT) {

			logger.info("Trouble with 1 " + val1);

			this.oops();
			return null;
		}

		Value val2 = arguments[1];
		if (Value.isNull(val2) || val2.getValueType() != ValueType.INTEGER) {

			logger.info("Trouble with 2 " + val2.getValueType() + "");

			this.oops();
			return null;
		}
		String fieldName = val1.toString();
		val1 = data.getValue(fieldName);
		if (Value.isNull(val1)) {
			return Value.VALUE_FALSE;
		}
		long nbr1 = 0;
		long nbr2 = 0;
		try {
			nbr1 = val1.toInteger();
			nbr2 = val2.toInteger();
		} catch (InvalidValueException e) {
			return Value.VALUE_FALSE;
		}

		return Value.newBooleanValue(nbr1 > nbr2);
	}

	private void oops() {
		throw new ApplicationError(
				"Function "
						+ MY_FULL_NAME
						+ " is to be called with a non-null fields collection and two argumants. 1:fieldName to use, 2:integer value to match the fiel dvalue with.");
	}
}
