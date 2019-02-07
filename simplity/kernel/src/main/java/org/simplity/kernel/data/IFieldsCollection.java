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
package org.simplity.kernel.data;

import java.util.Map;
import java.util.Set;

import org.simplity.kernel.value.Value;

/**
 * A simple name-value pair Map.
 *
 * @author simplity.org
 */
public interface IFieldsCollection {

  /**
   * get field value, or null if no such field
   *
   * @param fieldName
   * @return value or null if field does not exist
   */
  public Value getValue(String fieldName);

  /**
   * set/reset value to a field.
   *
   * @param fieldName not null
   * @param value not null, but vaule.isNull() could be true
   */
  public void setValue(String fieldName, Value value);

  /**
   * do we have a field with this name?
   *
   * @param fieldName
   * @return true if field exists. False otherwise.
   */
  public boolean hasValue(String fieldName);

  /**
   * remove value
   *
   * @param fieldName
   * @return existing value or null
   */
  public Value removeValue(String fieldName);

  /** @return an iterator for accessing all fields as an entry of name-value for current row */
  public Set<Map.Entry<String, Value>> getAllFields();
}
