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
package org.simplity.core.data;

import java.util.Map.Entry;

import org.simplity.core.value.Value;

import java.util.Set;

/**
 * a row inside a data sheet.
 *
 * @author simplity.org
 */
public class DataRow implements IFieldsCollection {

  private final IDataSheet dataSheet;
  private final int myIdx;

  DataRow(IDataSheet dataSheet, int rowIdx) {
    this.dataSheet = dataSheet;
    this.myIdx = rowIdx;
  }

  @Override
  public Value getValue(String fieldName) {
    return this.dataSheet.getColumnValue(fieldName, this.myIdx);
  }

  @Override
  public void setValue(String fieldName, Value value) {
    this.dataSheet.setColumnValue(fieldName, this.myIdx, value);
  }

  @Override
  public boolean hasValue(String fieldName) {
    return this.getValue(fieldName) != null;
  }

  @Override
  public Value removeValue(String fieldName) {
    Value value = this.getValue(fieldName);
    this.setValue(fieldName, null);
    return value;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.simplity.kernel.data.FieldsInterface#getAllFields(int)
   */
  @Override
  public Set<Entry<String, Value>> getAllFields() {
    return this.dataSheet.getAllFields(this.myIdx);
  }
}
