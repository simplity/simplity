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

package org.simplity.kernel.fn;

import org.simplity.kernel.data.IFieldsCollection;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;

/** @author simplity.org */
public class Concat extends AbstractFunction {
  /** null as the last entry means var args.. */
  private static final ValueType[] MY_ARG_TYPES = {ValueType.TEXT, null};

  /*
   * (non-Javadoc)
   *
   * @see
   * org.simplity.kernel.fn.Function#execute(org.simplity.kernel.value.Value
   * [], org.simplity.kernel.data.FieldsInterface)
   */
  @Override
  public Value execute(Value[] arguments, IFieldsCollection data) {
    if (arguments == null || arguments.length == 0) {
      return Value.VALUE_EMPTY;
    }
    StringBuilder sbf = new StringBuilder();
    for (Value val : arguments) {
      sbf.append(val);
    }
    return Value.newTextValue(sbf.toString());
  }

  /*
   * (non-Javadoc)
   *
   * @see org.simplity.kernel.fn.Function#getReturnType()
   */
  @Override
  public ValueType getReturnType() {
    return ValueType.TEXT;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.simplity.kernel.fn.Function#getArgDataTypes()
   */
  @Override
  public ValueType[] getArgDataTypes() {
    return MY_ARG_TYPES;
  }
}
