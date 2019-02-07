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
 * represents a value that has string/text value
 *
 * @author simplity.org
 */
public class TextValue extends Value {

  /** */
  private static final long serialVersionUID = 1L;

  protected final String value;

  /** @param textValue empty string is not null. */
  protected TextValue(String textValue) {
    if (textValue == null) {
      this.valueIsNull = true;
    }
    this.value = textValue;
  }

  /** create a TextValue with null as its value */
  protected TextValue() {
    this.valueIsNull = true;
    this.value = null;
  }

  @Override
  public ValueType getValueType() {
    return ValueType.TEXT;
  }

  @Override
  protected void format() {
    this.textValue = this.value;
  }

  @Override
  protected boolean equalValue(Value otherValue) {
    return this.value.equalsIgnoreCase(otherValue.toString());
  }

  @Override
  public void setToStatement(PreparedStatement statement, int idx) throws SQLException {
    if (this.isUnknown()) {
      statement.setNull(idx, Types.NVARCHAR);
    } else {
      statement.setString(idx, this.value);
    }
  }

  @Override
  public Object getObject() {
    return this.value;
  }

  @SuppressWarnings("unchecked")
@Override
  public String[] toArray(Value[] values) {
    int n = values.length;
    String[] arr = new String[n];
    for (int i = 0; i < n; i++) {
      TextValue val = (TextValue) values[i];
      arr[i] = val.value;
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
