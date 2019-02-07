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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.simplity.kernel.value.Value;

/**
 * represents a fields collection internally, but implements all methods of a data sheet. This is to
 * be used when you are likely to add/remove fields, and if used as a row of data, the order of the
 * columns is not important.
 *
 * @author simplity.org
 */
public class Fields implements IFieldsCollection {
  private final Map<String, Value> fieldValues = new HashMap<String, Value>();

  @Override
  public Value getValue(String fieldName) {
    return this.fieldValues.get(fieldName);
  }

  @Override
  public void setValue(String fieldName, Value value) {
    if (value == null) {
      this.fieldValues.remove(fieldName);
    } else {
      this.fieldValues.put(fieldName, value);
    }
  }

  @Override
  public boolean hasValue(String fieldName) {
    return this.fieldValues.containsKey(fieldName);
  }

  @Override
  public Value removeValue(String fieldName) {
    return this.fieldValues.remove(fieldName);
  }

  @Override
  public Set<Entry<String, Value>> getAllFields() {
    return this.fieldValues.entrySet();
  }
}
