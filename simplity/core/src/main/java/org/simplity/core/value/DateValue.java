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
package org.simplity.core.value;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;

import org.simplity.json.JSONWriter;

/**
 * represents a date value. internally represented by LocalDate
 *
 * @author simplity.org
 */
public class DateValue extends Value {
	/** */
	private static final long serialVersionUID = 1L;

	private final LocalDate value;

	protected DateValue(LocalDate value) {
		this.value = value;
	}

	protected DateValue() {
		this.value = null;
	}

	@Override
	public ValueType getValueType() {
		return ValueType.DATE;
	}

	@Override
	public LocalDate toDate() throws InvalidValueException {
		return this.value;
	}

	@Override
	protected boolean equalValue(Value otherValue) {
		if (otherValue instanceof DateValue) {
			return ((DateValue) otherValue).value == this.value;
		}
		return false;
	}

	/**
	 * method to be used on a concrete class to avoid exception handling
	 *
	 * @return date
	 */
	public LocalDate getDate() {
		return this.value;
	}

	@Override
	public void setToStatement(PreparedStatement statement, int idx) throws SQLException {
		if (this.isUnknown()) {
			statement.setNull(idx, Types.DATE);
			return;
		}
		statement.setDate(idx, Date.valueOf(this.value));
	}

	@Override
	public LocalDate getObject() {
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
