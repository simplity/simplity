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
import java.sql.Types;

import org.simplity.json.JSONWriter;

/**
 * represents whole number
 *
 * @author simplity.org
 */
public class IntegerValue extends Value {

  /** */
  private static final long serialVersionUID = 1L;

  private long value;

  protected IntegerValue(long value) {
    this.value = value;
  }

  protected IntegerValue() {
    this.valueIsNull = true;
  }

  @Override
  public ValueType getValueType() {
    return ValueType.INTEGER;
  }

  @Override
  protected void format() {
    this.textValue = "" + this.value;
  }

  @Override
  protected boolean equalValue(Value otherValue) {
    if (otherValue instanceof IntegerValue) {
      return ((IntegerValue) otherValue).value == this.value;
    }

    if (otherValue instanceof DecimalValue) {
      return ((DecimalValue) otherValue).getLong() == this.value;
    }

    return false;
  }

  @Override
  public long toInteger() throws InvalidValueException {
    return this.value;
  }

  @Override
  public double toDecimal() throws InvalidValueException {
    return this.value;
  }

  /**
   * preferred method if this concrete class is used. Avoids exception
   *
   * @return long value
   */
  public long getLong() {
    return this.value;
  }

  /**
   * preferred method if this concrete class is used. Avoids exception
   *
   * @return long value cast as decimal
   */
  public double getDouble() {
    return this.value;
  }

  @Override
  public void setToStatement(PreparedStatement statement, int idx) throws SQLException {
    if (this.isUnknown()) {
      statement.setNull(idx, Types.BIGINT);
    } else {
      statement.setLong(idx, this.value);
    }
  }

  @Override
  public Object getObject() {
    return new Long(this.value);
  }

  @SuppressWarnings("unchecked")
@Override
  public Object[] toArray(Value[] values) {
    int n = values.length;
    Long[] arr = new Long[n];
    for (int i = 0; i < n; i++) {
      IntegerValue val = (IntegerValue) values[i];
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
