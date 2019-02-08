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
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.simplity.core.ApplicationError;
import org.simplity.core.app.AppConventions;
import org.simplity.core.util.DateUtil;
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
	public static final String NULL_TEXT_VALUE = "";
	/** text value of a true boolean value :-) */
	public static final String TRUE_TEXT_VALUE = "1";

	/** text value of a boolean value of false */
	public static final String FALSE_TEXT_VALUE = "0";

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
	public static final BooleanValue UNNOWN_BOOLEAN_VALUE = new BooleanValue();
	/** unknown date */
	public static final DateValue UNKNOWN_DATE_VALUE = new DateValue();
	/** unknown decimal */
	public static final DecimalValue UNKNOWN_DECIMAL_VALUE = new DecimalValue();
	/** unknown integer */
	public static final IntegerValue UNKNOWN_INTERGRAL_VALUE = new IntegerValue();
	/** unknown text */
	public static final TextValue UNKNOWN_TEXT_VALUE = new TextValue();

	/** unknown text */
	public static final BlobValue UNKNOWN_BLOB_VALUE = new BlobValue();
	/** unknown text */
	public static final ClobValue UNKNOWN_CLOB_VALUE = new ClobValue();

	/** unknown timestamp */
	public static final TimestampValue UNKNOWN_TIMESTAMP_VALUE = new TimestampValue();
	/** true Boolean */
	public static final Boolean TRUE_OBJECT = new Boolean(true);

	/** */
	public static final Boolean FALSE_OBJECT = new Boolean(false);

	/** */
	public static final String JSON_NULL = "null";
	/** */
	public static final String JSON_TRUE = "true";
	/** */
	public static final String JSON_FALSE = "false";

	private static final int DATE_LENGTH = 12; // "/yyyy-mm-dd-/".length();
	private static final int LAST_POSITION = DATE_LENGTH - 1;
	private static final char DATE_DILIMITER = '/';
	private static final String TRUE = "true";
	private static final String FALSE = "false";
	private static final char ZERO = '0';
	private static final char NINE = '9';
	private static final char MINUS = '-';
	private static final char DOT = '.';
	/** text value. cached for non-text values. */
	protected String textValue;

	/**
	 * We may have to deal with RDBMS that allows nullable fields. We treat such
	 * a value as unknown.
	 */
	protected boolean valueIsNull = false;

	/**
	 * @param textValue
	 * @return an instance of Value for textValue.
	 */
	public static TextValue newTextValue(String textValue) {
		return new TextValue(textValue);
	}

	/**
	 * @param key
	 * @return an instance of Value for textValue.
	 */
	public static BlobValue newBlobValue(String key) {
		return new BlobValue(key);
	}

	/**
	 * @param key
	 * @return an instance of Value for textValue.
	 */
	public static ClobValue newClobValue(String key) {
		return new ClobValue(key);
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
	 * @param milliseconds
	 * @return returns an instance of Value for dateValue
	 */
	public static DateValue newDateValue(long milliseconds) {
		return new DateValue(milliseconds);
	}

	/**
	 * @param date
	 * @return returns an instance of Value for date
	 */
	public static DateValue newDateValue(Date date) {
		return new DateValue(date.getTime());
	}

	/**
	 * create a time-stamp value
	 *
	 * @param nanos
	 *            nano seconds from epoch
	 * @return new instance of time-stamp value
	 */
	public static TimestampValue newTimestampValue(long nanos) {
		return new TimestampValue(nanos);
	}

	/**
	 * create a time-stamp value
	 *
	 * @param stamp
	 *            instance of a time-stamp
	 * @return new instance of time-stamp value
	 */
	public static TimestampValue newTimestampValue(Timestamp stamp) {
		return new TimestampValue(stamp);
	}

	/**
	 * create a value of desired type by parsing the text input
	 *
	 * @param textValue
	 *            text value to be parsed
	 * @param valueType
	 *            value type of the desired Value object instance
	 * @return Value object of desired value type or null if the text value is
	 *         not compatible for the desired value type
	 */
	public static Value parseValue(String textValue, ValueType valueType) {
		if (textValue == null) {
			return Value.newUnknownValue(valueType);
		}

		String text = textValue.trim();
		try {
			switch (valueType) {
			case BOOLEAN:
				if (Value.TRUE_TEXT_VALUE.equals(text)) {
					return Value.VALUE_TRUE;
				}
				if (Value.FALSE_TEXT_VALUE.equals(text)) {
					return Value.VALUE_FALSE;
				}
				return null;
			case DATE:
				Date date = DateUtil.parseDateWithOptionalTime(text);
				if (date == null) {
					return null;
				}
				return new DateValue(date.getTime());
			case DECIMAL:
				return new DecimalValue(Double.parseDouble(text));
			case INTEGER:
				return new IntegerValue(Math.round(Double.parseDouble(text)));
			case TEXT:
				return new TextValue(text);
			case BLOB:
				return new BlobValue(text);
			case CLOB:
				return new ClobValue(text);
			case TIMESTAMP:
				return new TimestampValue(Long.parseLong(text));
			default:
				return new TextValue(text);
			}
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * @param valueType
	 *            desired value type
	 * @return Value object with null as its value
	 */
	public static Value newUnknownValue(ValueType valueType) {
		switch (valueType) {
		case TEXT:
			return Value.UNKNOWN_TEXT_VALUE;
		case BOOLEAN:
			return Value.UNNOWN_BOOLEAN_VALUE;
		case DATE:
			return Value.UNKNOWN_DATE_VALUE;
		case DECIMAL:
			return Value.UNKNOWN_DECIMAL_VALUE;
		case INTEGER:
			return Value.UNKNOWN_INTERGRAL_VALUE;
		case BLOB:
			return Value.UNKNOWN_BLOB_VALUE;
		case CLOB:
			return Value.UNKNOWN_CLOB_VALUE;
		case TIMESTAMP:
			return Value.UNKNOWN_TIMESTAMP_VALUE;
		default:
			throw new ApplicationError("Value class does not take care of value type " + valueType);
		}
	}

	/**
	 * is this value initialized with null? (unknown value)
	 *
	 * @return true if this is initialized with null (unknown) value
	 */
	public final boolean isUnknown() {
		return this.valueIsNull;
	}

	/**
	 * returns text value as per our convention. Intended use is for a
	 * text-based representation, but not for human readability. decimal value
	 * will have exactly four decimal places always. Date is represented as
	 * milli-seconds-from-epoch. Boolean becomes "1"/"0"
	 *
	 * @return text value.
	 */
	public final String toText() {
		if (this.textValue == null) {
			if (this.valueIsNull) {
				this.textValue = Value.NULL_TEXT_VALUE;
			} else {
				this.format();
			}
		}
		return this.textValue;
	}

	@Override
	public final String toString() {
		return this.toText();
	}

	/**
	 * true if obj is an instance of a compatible value, and both have non-null
	 * values and the values are equal
	 */
	@Override
	public final boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (this.valueIsNull || obj == null || obj instanceof Value == false) {
			return false;
		}
		Value otherValue = (Value) obj;
		if (otherValue.valueIsNull) {
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
	public Date toDate() throws InvalidValueException {
		throw new InvalidValueException(this.getValueType(), ValueType.DATE);
	}

	/** @return value type */
	public abstract ValueType getValueType();

	/** format value to textValue attribute */
	protected abstract void format();

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
	 * parse a value list and return a map of text value and the parsed value.
	 *
	 * @param textList
	 *            of the form a,b,c or a:alpha,b:beta...
	 * @param valueType
	 * @return set of values in this list
	 */
	public static Set<Value> parseValueList(String textList, ValueType valueType) {
		Set<Value> result = new HashSet<Value>();
		String[] vals = textList.split(AppConventions.LIST_SEPARATOR + "");
		for (String val : vals) {
			val = val.trim();
			int idx = val.indexOf(AppConventions.LIST_VALUE_SEPARATOR);
			if (idx != -1) {
				val = val.substring(idx + 1).trim();
			}
			if (val.length() == 0) {
				/*
				 * this is the blank/default option for client's sake. We treat
				 * is not specified
				 */
				continue;
			}
			Value value = Value.parseValue(val, valueType);
			if (value == null) {
				throw new ApplicationError("Value list " + textList + " has an invalid value of " + val);
			}
			if (result.add(value) == false) {
				throw new ApplicationError("Value list " + textList + " has duplicate value of " + val);
			}
		}
		return result;
	}

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
			Value value = Value.parseValue(val, valueType);
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
	public static Value parseValue(String text) {
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
			Date date = DateUtil.parseDate(dateText);
			if (date != null) {
				return Value.newDateValue(date);
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
		Date date = DateUtil.parseDateWithOptionalTime(text);
		if (date != null) {
			return Value.newDateValue(date);
		}

		return Value.newTextValue(text);
	}

	/** @return an object that is suitable for db operation */
	public abstract Object getObject();

	/**
	 * @param values
	 * @return gets an array of primitives/object for the Value[] array
	 */
	public abstract <T> T[] toArray(Value[] values);

	/**
	 * @return java Object that represents the underlying value. String, Long,
	 *         Double, Date or Boolean instance.
	 */
	public Object toObject() {
		if (this.valueIsNull) {
			return null;
		}
		return this.getObject();
	}

	/**
	 * parse an object, say one that is returned from rdbms, or from JSON, into
	 * a Value
	 *
	 * @param object
	 * @return Value of this object based on the object type that can be best
	 *         guessed
	 */
	public static Value parseObject(Object object) {
		if (object == null) {

			logger.info("Parse Object received null. Returning empty text value.");

			return VALUE_EMPTY;
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

		if (object instanceof Date) {
			if (object instanceof Timestamp) {
				return newTimestampValue((Timestamp) object);
			}
			return newDateValue((Date) object);
		}
		/*
		 * we wouldn't consider well-formed date strings as coincidence
		 */
		String val = object.toString();
		Date date = DateUtil.parseDateWithOptionalTime(val);
		if (date != null) {
			return newDateValue(date);
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
	 * time-stamp value is provided more flexibility to allow implementation
	 * specific variations
	 *
	 * @param value
	 * @return time stamp value
	 */
	public static Value newTimestampValue(Value value) {
		if (value == null) {
			return null;
		}
		switch (value.getValueType()) {
		case TEXT:
			String text = value.toText();
			try {
				return new TimestampValue(Long.parseLong(text));
			} catch (Exception e) {
				Date date = DateUtil.parseDateWithOptionalTime(text);
				if (date != null) {
					return new TimestampValue(date.getTime());
				}
			}
			break;
		case INTEGER:
			return new TimestampValue(((IntegerValue) value).getLong());
		case DATE:
			return new TimestampValue(((DateValue) value).getDate());
		default:
			break;
		}
		throw new ApplicationError(
				value.toString() + " is not a valid time stamp value. It has to be eithe rnumeric, or a valid date.");
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
			values[i] = valueTypes[i].fromObject(objects[i]);
		}
		return values;
	}
}
