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
package org.simplity.core.dt;

import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationMessage;
import org.simplity.core.value.DecimalValue;
import org.simplity.core.value.IntegerValue;
import org.simplity.core.value.InvalidValueException;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * numeric with or without decimals
 *
 * @author simplity.org
 */
public class NumericDataType extends DataType {
	private static final Logger logger = LoggerFactory.getLogger(NumericDataType.class);

	/** min digits before decimal places required for this value */
	long minValue = Long.MIN_VALUE;

	/** maximum number of whole digits (excluding decimal digits) */
	long maxValue = Long.MAX_VALUE;

	/**
	 * maximum number of fractional digits. Anything more than this is rounded
	 * off
	 */
	int nbrFractionDigits = 0;

	@Override
	public Value validateValue(Value value) {
		if (this.nbrFractionDigits == 0) {
			return this.validateInt(value);
		}
		return this.validateDecimal(value);
	}

	private Value validateInt(Value value) {
		ValueType valueType = value.getValueType();
		long longValue = 0L;

		/*
		 * check for numeric type
		 */
		if (valueType == ValueType.INTEGER) {
			longValue = ((IntegerValue) value).getLong();
		} else if (valueType == ValueType.DECIMAL) {
			longValue = ((DecimalValue) value).getLong();
		} else {
			return null;
		}
		/*
		 * min-max check
		 */
		if (longValue > this.maxValue || longValue < this.minValue) {
			return null;
		}
		/*
		 * create new value if required
		 */
		if (valueType == ValueType.INTEGER) {
			return value;
		}
		return Value.newIntegerValue(longValue);
	}

	private Value validateDecimal(Value value) {
		ValueType valueType = value.getValueType();
		double dbl = 0;

		/*
		 * check for numeric type
		 */
		if (valueType == ValueType.INTEGER) {
			dbl = ((IntegerValue) value).getDouble();
		} else if (valueType == ValueType.DECIMAL) {
			dbl = ((DecimalValue) value).getDouble();
		} else {
			return null;
		}
		/*
		 * min-max check
		 */
		if (dbl > this.maxValue || dbl < this.minValue) {
			return null;
		}
		/*
		 * create new value if required
		 */
		if (valueType == ValueType.DECIMAL) {
			return value;
		}
		return Value.newDecimalValue(dbl);
	}

	@Override
	public ValueType getValueType() {
		if (this.nbrFractionDigits == 0) {
			return ValueType.INTEGER;
		}
		return ValueType.DECIMAL;
	}

	@Override
	public int getMaxLength() {
		return Long.toString(this.maxValue).length() + this.nbrFractionDigits + 1;
	}

	@Override
	public int getScale() {
		return this.nbrFractionDigits;
	}

	@Override
	protected void validateSpecific(IValidationContext vtx) {
		if (this.minValue > this.maxValue) {
			String msg = "Invalid number range. Min vaue of "
					+ this.minValue
					+ " is greater that max value of "
					+ this.maxValue;
			vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR, msg, "minValue"));
			vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR, msg, "maxValue"));
		}
		if (this.nbrFractionDigits < 0) {
			vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
					"nbrFractionDigits is set to a negative value of " + this.nbrFractionDigits, "nbrFractionDigits"));
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.dt.DataType#synthesiseDscription()
	 */
	@Override
	protected String synthesiseDscription() {
		StringBuilder sbf = new StringBuilder("Expecting ");
		if (this.nbrFractionDigits == 0) {
			sbf.append("an integer ");
		} else {
			sbf.append("a decimal number ");
		}
		sbf.append("between ").append(this.minValue).append(" and ").append(this.maxValue);
		return sbf.toString();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.dt.DataType#formatVal(org.simplity.kernel.value.
	 * Value)
	 */
	@Override
	public String formatVal(Value value) {
		try {
			if (this.nbrFractionDigits == 0) {
				return "" + value.toInteger();
			}
			return String.format("%." + this.nbrFractionDigits + "f", new Double(value.toDecimal()));
		} catch (InvalidValueException e) {

			logger.info("Numeric data type is asked to format " + value.getValueType());
		}
		return Value.FALSE_TEXT_VALUE;
	}
}
