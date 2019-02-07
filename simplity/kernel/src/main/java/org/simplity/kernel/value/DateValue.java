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
package org.simplity.kernel.value;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;

import org.simplity.json.JSONWriter;
import org.simplity.kernel.util.DateUtil;

/**
 * represents a date value. java.lang.Date is mutable, and hence is not suitable to represent a
 * value. We keep date.getTime() as a long. We return a new Date() each time some one asks for value
 *
 * @author simplity.org
 */
public class DateValue extends Value {
  /** */
  private static final long serialVersionUID = 1L;

  private long value;

  protected DateValue(long value) {
    this.value = value;
  }

  protected DateValue() {
    this.valueIsNull = true;
  }

  @Override
  public ValueType getValueType() {
    return ValueType.DATE;
  }

  @Override
  protected void format() {
    this.textValue = DateUtil.formatDateTime(this.value);
  }

  @Override
  public Date toDate() throws InvalidValueException {
    /*
     * we return new date instead of caching because Date, unfortunately, is
     * mutable
     */
    return new Date(this.value);
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
  public long getDate() {
    return this.value;
  }

  @Override
  public void setToStatement(PreparedStatement statement, int idx) throws SQLException {
    if (this.isUnknown()) {
      statement.setNull(idx, Types.DATE);
    } else {
      if (DateUtil.hasTime(this.value)) {
        Timestamp dateValue = new Timestamp(this.value);
        statement.setTimestamp(idx, dateValue);
      } else {
        java.sql.Date dateValue = new java.sql.Date(this.value);
        statement.setDate(idx, dateValue);
      }
    }
  }

  @Override
  public Object getObject() {
    /*
     * should it be java.lang.Date? anyways java.sql.Date extends it.
     */
    return new java.sql.Date(this.value);
  }

  @SuppressWarnings("unchecked")
@Override
  public Object[] toArray(Value[] values) {
    int n = values.length;
    java.sql.Date[] arr = new java.sql.Date[n];
    for (int i = 0; i < n; i++) {
      DateValue val = (DateValue) values[i];
      arr[i] = new java.sql.Date(val.value);
    }
    return arr;
  }

  /* (non-Javadoc)
	 * @see org.simplity.json.Jsonable#writeJsonValue(org.simplity.json.JSONWriter)
	 */
	@Override
	public void writeJsonValue(JSONWriter writer) {
		writer.value(new Date(this.value));
	}
}
