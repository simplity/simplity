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
package org.simplity.kernel.value;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;

import org.simplity.json.JSONWriter;

/**
 * Very special value, and should not be used except for specific timestap field
 * defined in an
 * rdbms. Simplity uses time-stamp field to take care of concurrency issues.
 * This class is designed
 * to facilitate that
 *
 * @author simplity.org
 */
public class TimestampValue extends Value {
	private static final long NANO = 1000000000;

	/** */
	private static final long serialVersionUID = 1L;
	/**
	 * this is the number of nano-seconds from epoch.
	 */
	private long value;

	protected TimestampValue(long value) {
		this.value = value;
	}

	protected TimestampValue(Timestamp ts) {
		/*
		 * remove fractional secs from date and convert that to nanos
		 */
		long nanosInDate = (ts.getTime() / 1000) * 1000000000;
		this.value = nanosInDate + ts.getNanos();
	}

	protected TimestampValue() {
		this.valueIsNull = true;
	}

	@Override
	public ValueType getValueType() {
		return ValueType.TIMESTAMP;
	}

	@Override
	protected void format() {
		this.textValue = Long.toString(this.value);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.value.Value#toInteger()
	 */
	@Override
	public long toInteger() throws InvalidValueException {
		return this.value;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.value.Value#toDecimal()
	 */
	@Override
	public double toDecimal() throws InvalidValueException {
		return this.value;
	}

	@Override
	public Date toDate() throws InvalidValueException {
		/*
		 * we return new date instead of caching because Date, unfortunately, is
		 * mutable
		 */
		return new Date(this.value / 1000000);
	}

	@Override
	protected boolean equalValue(Value otherValue) {
		if (otherValue instanceof TimestampValue) {
			return ((TimestampValue) otherValue).value == this.value;
		}
		return false;
	}

	/**
	 * method to be used on a concrete class to avoid exception handling
	 *
	 * @return date
	 */
	public long getDate() {
		return this.value / 1000000;
	}

	/**
	 * method to be used on a concrete class to avoid exception handling
	 *
	 * @return date
	 */
	public long getInteger() {
		return this.value;
	}

	@Override
	public void setToStatement(PreparedStatement statement, int idx) throws SQLException {
		if (this.valueIsNull) {
			statement.setNull(idx, Types.TIMESTAMP);
		} else {
			/*
			 * create date with truncated seconds
			 */
			Timestamp dateValue = new Timestamp((this.value / NANO) * 1000);
			dateValue.setNanos((int) (this.value % NANO));
			statement.setTimestamp(idx, dateValue);
		}
	}

	@Override
	public Object getObject() {
		return this.value;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object[] toArray(Value[] values) {
		int n = values.length;
		Long[] arr = new Long[n];
		for (int i = 0; i < n; i++) {
			TimestampValue val = (TimestampValue) values[i];
			arr[i] = new Long(val.value);
		}
		return arr;
	}
	/* (non-Javadoc)
	 * @see org.simplity.json.Jsonable#writeJsonValue(org.simplity.json.JSONWriter)
	 */
	@Override
	public void writeJsonValue(JSONWriter writer) {
		writer.value(this.value);
	}

}
