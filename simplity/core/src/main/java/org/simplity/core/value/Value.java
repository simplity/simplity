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
package org.simplity.core.value;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;

import org.simplity.core.ApplicationError;
import org.simplity.json.JsonWritable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents value of a field or data element. Text, Integer, Decimal. Boolean
 * and Date are the five types we support.
 *
 * <p>
 * If we have to carry values like int/long, any ways we have to wrap them in
 * Integer/Long etc.. class. Value class not only wraps the data, it provides
 * the foundation on which generic utilities, like expression evaluation, can be
 * built.
 *
 * <p>
 * Text-value of boolean and date :
 *
 * <p>
 * Important consideration. we expect that Value is internal to programming, and
 * any conversion to/from text would be within programming paradigm. For example
 * serialization/de-serialization. It need not be human-readable. Hence we have
 * chosen "1"/"0" for boolean and the number-of-milli-seconds-from-epoch for
 * date.
 *
 * @author simplity.org
 */
public abstract class Value implements Serializable, JsonWritable {
	protected static final Logger logger = LoggerFactory.getLogger(Value.class);

	/** */
	private static final long serialVersionUID = 1L;
	/*
	 * we debated whether to make Value itself an enum, but settled with the
	 * arrangement that we define valueType as enum for each sub-class of Value
	 */

	/**
	 * what is the text value of null? As we are focusing more on serialization,
	 * rather than human readable, we use empty string
	 */
	public static final String NULL_TEXT_VALUE = "null";

	/** value is anyway immutable. no need to create new instance */
	public static final BooleanValue VALUE_TRUE = new BooleanValue(true);

	/** value is anyway immutable. no need to create new instance */
	public static final BooleanValue VALUE_FALSE = new BooleanValue(false);

	/** integral 0 is so frequently used. */
	public static final IntegerValue VALUE_ZERO = new IntegerValue(0);
	/** empty string. */
	public static final TextValue VALUE_EMPTY = new TextValue("");
	/*
	 * why keep producing null/unknown values? cache these immutable object
	 * instances
	 */
	/** boolean unknown value */
	public static final BooleanValue VALUE_UNKNOWN_BOOLEAN = new BooleanValue();
	/** unknown date */
	public static final DateValue VALUE_UNKNOWN_DATE = new DateValue();
	/** unknown decimal */
	public static final DecimalValue VALUE_UNKNOWN_DECIMAL = new DecimalValue();
	/** unknown integer */
	public static final IntegerValue VALUE_UNKNOWN_INTEGER = new IntegerValue();
	/** unknown text */
	public static final TextValue VALUE_UNKNOWN_TEXT = new TextValue();

	/** unknown timestamp */
	public static final TimestampValue VALUE_UNKNOWN_TIMESTAMP = new TimestampValue();
	/** true Boolean */
	public static final Boolean TRUE_OBJECT = new Boolean(true);

	/** */
	public static final Boolean FALSE_OBJECT = new Boolean(false);

	private static final int DATE_LENGTH = 12; // "/yyyy-mm-dd-/".length();
	private static final int LAST_POSITION = DATE_LENGTH - 1;
	private static final char DATE_DILIMITER = '/';
	private static final String TRUE = "true";
	private static final String FALSE = "false";
	private static final char ZERO = '0';
	private static final char NINE = '9';
	private static final char MINUS = '-';
	private static final char DOT = '.';

	/**
	 * @param textValue
	 * @return an instance of Value for textValue.
	 */
	public static TextValue newTextValue(String textValue) {
		return new TextValue(textValue);
	}

	/**
	 * @param integralValue
	 * @return returns an instance of Value for integralValue
	 */
	public static IntegerValue newIntegerValue(long integralValue) {
		if (integralValue == 0) {
			return VALUE_ZERO;
		}
		return new IntegerValue(integralValue);
	}

	/**
	 * @param decimalValue
	 * @return returns an instance of Value for decimalValue
	 */
	public static DecimalValue newDecimalValue(double decimalValue) {
		return new DecimalValue(decimalValue);
	}

	/**
	 * @param booleanValue
	 * @return returns an instance of Value for booleanValue
	 */
	public static BooleanValue newBooleanValue(boolean booleanValue) {
		if (booleanValue) {
			return Value.VALUE_TRUE;
		}
		return Value.VALUE_FALSE;
	}

	/**
	 * @param date
	 * @return returns an instance of Value for dateValue
	 */
	public static DateValue newDateValue(LocalDate date) {
		return new DateValue(date);
	}

	/**
	 * create a time-stamp value
	 *
	 * @param instant
	 *            an instant of time
	 * @return new instance of time-stamp value
	 */
	public static TimestampValue newTimestampValue(Instant instant) {
		return new TimestampValue(instant);
	}

	/**
	 * @param valueType
	 *            desired value type
	 * @return Value object with null as its value
	 */
	public static Value newUnknownValue(ValueType valueType) {
		switch (valueType) {
		case TEXT:
			return Value.VALUE_UNKNOWN_TEXT;
		case BOOLEAN:
			return Value.VALUE_UNKNOWN_BOOLEAN;
		case DATE:
			return Value.VALUE_UNKNOWN_DATE;
		case DECIMAL:
			return Value.VALUE_UNKNOWN_DECIMAL;
		case INTEGER:
			return Value.VALUE_UNKNOWN_INTEGER;
		case TIMESTAMP:
			return Value.VALUE_UNKNOWN_TIMESTAMP;
		default:
			throw new ApplicationError("Value class does not take care of value type " + valueType);
		}
	}

	/**
	 * is this value initialized with null? (unknown value)
	 *
	 * @return true if this is initialized with null (unknown) value
	 */
	public abstract boolean isUnknown();

	/**
	 * true if obj is an instance of a compatible value, and both have non-null
	 * values and the values are equal
	 */
	@Override
	public final boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (this.isUnknown() || obj == null || obj instanceof Value == false) {
			return false;
		}
		Value otherValue = (Value) obj;
		if (otherValue.isUnknown()) {
			return false;
		}

		return this.equalValue(otherValue);
	}

	/**
	 * decimal is converted to integer, but other value types result in
	 * exception
	 *
	 * @return integer value
	 * @throws InvalidValueException
	 *             in case the value type is neither integer, nor decimal.
	 */
	public long toInteger() throws InvalidValueException {
		throw new InvalidValueException(this.getValueType(), ValueType.INTEGER);
	}

	/**
	 * integer is converted to decimal, but other value types are considered
	 * incompatible
	 *
	 * @return decimal representation for this value
	 * @throws InvalidValueException
	 *             in case the value type is not numeric
	 */
	public double toDecimal() throws InvalidValueException {
		throw new InvalidValueException(this.getValueType(), ValueType.DECIMAL);
	}

	/**
	 * @return true/false
	 * @throws InvalidValueException
	 *             if value type is not boolean
	 */
	public boolean toBoolean() throws InvalidValueException {
		throw new InvalidValueException(this.getValueType(), ValueType.BOOLEAN);
	}

	/**
	 * @return date if internal value is of type date
	 * @throws InvalidValueException
	 *             if value type is not date
	 */
	public LocalDate toDate() throws InvalidValueException {
		throw new InvalidValueException(this.getValueType(), ValueType.DATE);
	}

	/**
	 * @return instant if internal value is of type timestamp
	 * @throws InvalidValueException
	 *             if value type is not timestamp
	 */
	public Instant toTimestamp() throws InvalidValueException {
		throw new InvalidValueException(this.getValueType(), ValueType.TIMESTAMP);
	}

	/** @return value type */
	public abstract ValueType getValueType();

	/**
	 * this as well as otherValue are have non-null value. Compare them
	 *
	 * @param otherValue
	 * @return true if the two values are compatible and equal, false otherwise
	 */
	protected abstract boolean equalValue(Value otherValue);

	/**
	 * add this value to a sql prepared statement
	 *
	 * @param statement
	 * @param idx
	 * @throws SQLException
	 */
	public abstract void setToStatement(PreparedStatement statement, int idx) throws SQLException;

	/**
	 * parse an array of text values into an array of given value type
	 *
	 * @param textList
	 *            of the form a,b,c
	 * @param valueType
	 * @return array of values of given type, or null in case of any error whiel
	 *         parsing
	 */
	public static Value[] parse(String[] textList, ValueType valueType) {
		Value[] result = new Value[textList.length];

		for (int i = 0; i < textList.length; i++) {
			String val = textList[i].trim();
			Value value = valueType.parse(val);
			if (value == null) {
				return null;
			}
			result[i] = value;
		}
		return result;
	}

	/**
	 * @param value
	 * @return true if either value is null, or has a null value
	 */
	public static boolean isNull(Value value) {
		if (value == null) {
			return true;
		}
		return value.isUnknown();
	}

	/**
	 * parse a constant as per our convention. true/false for boolean
	 * /yyyy-mm-dd/ for date, or any valid number. else text
	 *
	 * @param text
	 * @return parsed value
	 */
	public static Value parse(String text) {
		if (text == null) {
			return null;
		}

		int n = text.length();
		if (n == 0) {
			return VALUE_EMPTY;
		}

		char c = text.charAt(0);
		if (n == DATE_LENGTH && c == DATE_DILIMITER && text.charAt(LAST_POSITION) == DATE_DILIMITER) {
			String dateText = text.substring(1, text.length() - 1);
			try {
				return Value.newDateValue(LocalDate.parse(dateText));
			} catch (Exception ignore) {
				//
			}
		}

		if (text.equals(TRUE)) {
			return VALUE_TRUE;
		}
		if (text.equals(FALSE)) {
			return VALUE_FALSE;
		}
		if (c >= ZERO && c <= NINE || c == MINUS) {
			try {
				if (text.indexOf(DOT) == -1) {
					return Value.newIntegerValue(Long.parseLong(text));
				}

				return Value.newDecimalValue(Double.parseDouble(text));
			} catch (Exception e) {
				// we just tried
			}
		}
		/*
		 * date?
		 */
		try {
			return Value.newDateValue(LocalDate.parse(text));
		} catch (Exception ignore) {
			//
		}

		return Value.newTextValue(text);
	}

	/**
	 * @return java Object that represents the underlying value. String, Long,
	 *         Double, Date or Boolean instance.
	 */
	public Object toObject() {
		if (this.isUnknown()) {
			return null;
		}
		return this.getObject();
	}

	/** @return an object that is suitable for db operation */
	protected abstract Object getObject();

	/**
	 * parse an object, say one that is returned from rdbms, or from JSON, into
	 * a Value
	 *
	 * @param object
	 *            non-null
	 * @return Value of this object based on the object type that can be best
	 *         guessed
	 */
	public static Value parse(Object object) {
		if (object == null) {
			logger.info("Parse Object received null. Returning empty text value.");
			return null;
		}
		if (object instanceof Boolean) {
			if (((Boolean) object).booleanValue()) {
				return VALUE_TRUE;
			}
			return VALUE_FALSE;
		}
		if (object instanceof Number) {
			if (object instanceof Double) {
				return newDecimalValue(((Double) object).doubleValue());
			}
			return newIntegerValue(((Number) object).longValue());
		}

		if (object instanceof LocalDate) {
			return newDateValue((LocalDate) object);
		}
		if (object instanceof Instant) {
			return newTimestampValue((Instant) object);
		}
		/*
		 * we wouldn't consider well-formed date strings as coincidence
		 */
		String val = object.toString();
		if (val.length() == 10) {
			try {
				return newDateValue(LocalDate.parse(val));
			} catch (Exception ignore) {
				//
			}
		}

		/*
		 * when it is not anything else, it is text
		 */
		return newTextValue(val);
	}

	/**
	 * interpret the value as a boolean, irrespective of its value type
	 *
	 * @param value
	 * @return true if boolean-true, positive-number, date, or non-empty content
	 */
	public static boolean intepretAsBoolean(Value value) {
		if (Value.isNull(value)) {
			return false;
		}

		switch (value.getValueType()) {
		case BOOLEAN:
			return ((BooleanValue) value).getBoolean();
		case INTEGER:
			return ((IntegerValue) value).getLong() > 0;
		case DECIMAL:
			return ((DecimalValue) value).getDouble() > 0;
		case DATE:
			return true;
		default:
			return value.toString().length() > 0;
		}
	}

	/**
	 *
	 * @param objects
	 *            array of primitive objects to be parsed into values
	 * @param valueTypes
	 *            value types to be used for parsing
	 * @return array in which each element is parsed from the corresponding
	 *         object array element, based on the vlueType
	 */
	public static Value[] toValues(Object[] objects, ValueType[] valueTypes) {
		Value[] values = new Value[objects.length];
		for (int i = 0; i < values.length; i++) {
			values[i] = valueTypes[i].parseObject(objects[i]);
		}
		return values;
	}
}
