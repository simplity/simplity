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
package org.simplity.core.dt;

import org.simplity.core.value.InvalidValueException;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author simplity.org */
public class BooleanDataType extends DataType {
	private static final Logger logger = LoggerFactory.getLogger(BooleanDataType.class);

  private static final String DESC = "1 for yes/true and 0 for false/no";

  @Override
  public Value validateValue(Value value) {
    return value;
  }

  @Override
  public ValueType getValueType() {
    return ValueType.BOOLEAN;
  }

  @Override
  public int getMaxLength() {
    return 20;
  }

  @Override
  protected String synthesiseDscription() {
    return DESC;
  }

  /* (non-Javadoc)
   * @see org.simplity.kernel.dt.DataType#formtValue(org.simplity.kernel.value.Value)
   */
  @Override
  public String formatVal(Value value) {

    try {
      if (value.toBoolean()) {
        return Value.TRUE_TEXT_VALUE;
      }
    } catch (InvalidValueException e) {

      logger.info("Boolean data type is asked to format  non-boolean value. False value assumed");
    }
    return Value.FALSE_TEXT_VALUE;
  }
}
