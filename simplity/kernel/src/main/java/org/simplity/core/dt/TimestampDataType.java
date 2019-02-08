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
package org.simplity.core.dt;

import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;

/**
 * timestamp is a pseudo data type, because it is actually a numeric field, but it corresponds to an
 * RDBMS field defined as timestamp. JDBC, in our opinion, created a mess by defining timestamp as
 * an extension of Date field, but not exactly. Confusion is that if you cast Timestamp to Date then
 * you loose the entire second, not just the nano-part of it. Hence we treat it as long internally,
 * but allow users to treat it as date as well. Nano second is relevant only if the caller treats it
 * as long.
 *
 * <p>Also, time-stamp is used for checking whether the record is stale/dirty before updating.
 * Hence, it is important that we do not loose the nano-part while transporting it between server
 * and client. Hence simplity uses this as if it is a number.
 *
 * @author simplity.org
 */
public class TimestampDataType extends DataType {
  /** max number of characters expected. Let us keep it safe for possible key length */
  private static final int MAX_LENGTH = 11;

  private static final String DESC =
      "This is an internally managed field. Users are not encouraged to enter this value. If this is for testing, ensure that you enter the number of nano seconds from epoch";

  /*
   * (non-Javadoc)Should we check whether the key indeed points to a file?
   * Actually a good idea, except for performance. Let us go ahead with it at
   * this time
   *
   * @see
   * org.simplity.kernel.dt.DataType#validateValue(org.simplity.kernel.value
   * .Value)
   */
  @Override
  public Value validateValue(Value value) {
    return value;
  }

  @Override
  public ValueType getValueType() {
    return ValueType.TIMESTAMP;
  }

  @Override
  public int getMaxLength() {
    return MAX_LENGTH;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.simplity.kernel.dt.DataType#synthesiseDscription()
   */
  @Override
  protected String synthesiseDscription() {
    return DESC;
  }
}
