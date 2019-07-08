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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import org.simplity.json.JSONWriter;

/**
 * Instant of time. this is time-zone sensitive if being printed/rendered.
 *
 * @author simplity.org
 */
public class TimestampValue extends Value {

	private static final long serialVersionUID = 1L;

	private final Instant value;

	protected TimestampValue(Instant value) {
		this.value = value;
	}

	protected TimestampValue(Timestamp ts) {
		if (ts == null) {
			this.value = null;
		} else {
			this.value = ts.toInstant();
		}
	}

	protected TimestampValue() {
		this.value = null;
	}

	@Override
	public ValueType getValueType() {
		return ValueType.TIMESTAMP;
	}

	@Override
	protected boolean equalValue(Value otherValue) {
		if (otherValue instanceof TimestampValue) {
			return ((TimestampValue) otherValue).value == this.value;
		}
		return false;
	}

	@Override
	public Instant toTimestamp() throws InvalidValueException {
		return this.value;
	}

	/**
	 * method to be used on a concrete class to avoid exception handling
	 *
	 * @return instant
	 */
	public Instant getInstant() {
		return this.value;
	}

	@Override
	public void setToStatement(PreparedStatement statement, int idx) throws SQLException {
		if (this.value == null) {
			statement.setTimestamp(idx, null);
		} else {
			statement.setTimestamp(idx, Timestamp.from(this.value));
		}
	}

	@Override
	public Object getObject() {
		return this.value;
	}

	@Override
	public void writeJsonValue(JSONWriter writer) {
		writer.value(this.value);
	}

	@Override
	public boolean isUnknown() {
		return this.value == null;
	}

	@Override
	public String toString() {
		if (this.value == null) {
			return Value.NULL_TEXT_VALUE;
		}
		return this.value.toString();
	}
}
