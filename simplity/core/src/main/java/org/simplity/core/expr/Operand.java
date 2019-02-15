/*
 * Copyright (c) 2015 EXILANT Technologies Private Limited (www.exilant.com)
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
package org.simplity.core.expr;

import org.simplity.core.ApplicationError;
import org.simplity.core.app.Application;
import org.simplity.core.data.IFieldsCollection;
import org.simplity.core.fn.IFunction;
import org.simplity.core.value.InvalidValueException;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;

/**
 * models an operand of this expression
 *
 * @author simplity.org
 */
class Operand {
	/*
	 * operand can be of four types
	 */
	static final int NONE = 0;
	static final int CONSTANT = 1;
	static final int FIELD = 2;
	static final int FUNCTION = 3;
	static final int EXPRESSION = 4;
	int operandType = Operand.NONE;

	/** unary operator is stored only if it is not a constant */
	UnaryOperator uop;
	/**
	 * value is the value of the constant. otherwise name of the field/function.
	 */
	Value value;
	/** in case this is an expression */
	Expression expression;

	/**
	 * algorithm assumes that the operand is valid. Works because the operand is
	 * validated as part of parsing.
	 *
	 * @param data
	 * @return
	 * @throws InvalidOperationException
	 */
	Value getValue(IFieldsCollection data) throws InvalidOperationException {
		if (this.operandType == Operand.CONSTANT) {
			return this.value;
		}

		Value valueToReturn = null;
		try {
			if (this.operandType == Operand.EXPRESSION) {
				valueToReturn = this.expression.evaluate(data);
			} else {
				String fname = this.value.toString();
				if (this.operandType == Operand.FIELD) {
					valueToReturn = data.getValue(fname);
					if (this.uop == UnaryOperator.IsKnown) {
						return Value.newBooleanValue(valueToReturn != null);
					}
					if (this.uop == UnaryOperator.IsUnknown) {
						return Value.newBooleanValue(valueToReturn == null);
					}
					if (valueToReturn == null) {
						valueToReturn = Value.newUnknownValue(ValueType.TEXT);
					}
				} else {
					Value[] args = null;
					if (this.expression != null) {
						args = this.expression.getValueList(data);
					}
					IFunction fn = Application.getActiveInstance().getFunction(fname);
					valueToReturn = fn.execute(args, data);
				}
			}
			if (this.uop == null || valueToReturn.isUnknown()) {
				return valueToReturn;
			}
			ValueType type = valueToReturn.getValueType();
			if (this.uop == UnaryOperator.Not) {
				if (type == ValueType.BOOLEAN) {
					return Value.newBooleanValue(!valueToReturn.toBoolean());
				}
			} else {
				if (type == ValueType.INTEGER) {
					return Value.newIntegerValue(-valueToReturn.toInteger());
				}
				if (type == ValueType.DECIMAL) {
					return Value.newDecimalValue(-valueToReturn.toDecimal());
				}
			}
		} catch (InvalidValueException e) {
			// we have ensure that this exception is not thrown. And, in case of
			// some design change, this is still valid as we will end up
			// throwing another exception
		}
		throw new InvalidOperationException(UnaryOperator.Not, valueToReturn.getValueType());
	}

	/**
	 * invalid if ~ ? are used on a non-field
	 *
	 * @return
	 */
	boolean isValid() {
		if (this.uop == null) {
			return true;
		}

		if (this.operandType == Operand.FIELD) {
			return true;
		}

		if (this.uop == UnaryOperator.IsKnown || this.uop == UnaryOperator.IsUnknown) {
			return false;
		}

		if (this.operandType != Operand.CONSTANT) {
			return true;
		}
		ValueType type = this.value.getValueType();
		try {
			if (this.uop == UnaryOperator.Minus) {
				if (type == ValueType.INTEGER) {
					this.value = Value.newIntegerValue(-this.value.toInteger());
					this.uop = null;
					return true;
				}
				if (type == ValueType.DECIMAL) {
					this.value = Value.newDecimalValue(-this.value.toDecimal());
					this.uop = null;
					return true;
				}
			}
		} catch (InvalidValueException e) {
			/*
			 * by design, we are not to reach here. But we have to play it safe
			 */
			throw new ApplicationError(e, "Error while using an operand.");
		}
		return false;
	}

	@Override
	public String toString() {
		return "unaryOperator: " + this.uop + " value:" + this.value;
	}
}
