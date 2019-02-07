/*
 * Copyright (c) 2017 simplity.org
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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.kernel.aggr;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.data.IFieldsCollection;
import org.simplity.kernel.service.ServiceContext;
import org.simplity.kernel.value.Value;

/**
 * number of rows
 *
 * @author simplity.org
 */
public class Count implements IAggregationWorker {
  /**
   * field name to count. If specified, rows with non-null values for this field are counted. null,
   * or '*' implies count every row. sum is to be written out
   */
  private String inputFeildName;

  /** field name to which sum is to be written out */
  private final String outputFieldName;

  private int count;
  /** keep track of accumulation and throw an exception in case of concurrency issues */
  private boolean inProgress;

  /**
   * create an an instance with the required parameters
   *
   * @param inputFieldName Input field name
   * @param outputFieldName field/column name that is to be written out as sum. non-empty, non-null;
   */
  public Count(String inputFieldName, String outputFieldName) {
    if (inputFieldName != null && inputFieldName.equals("*") == false) {
      this.inputFeildName = inputFieldName;
    }
    this.outputFieldName = outputFieldName;
  }

  @Override
  public void init(ServiceContext ctx) {
    if (this.inProgress) {
      this.throwError();
    }
    this.inProgress = true;
  }

  private void throwError() {
    throw new ApplicationError(
        "Aggregator instance should be ideally not re-used across aggregations."
            + " In case it is used, it is to be ensured that the the sequence of calls is  "
            + " init(), accumulate(), writeOut()/discard(), reset()");
  }

  @Override
  public void accumulate(IFieldsCollection currentRow, ServiceContext ctx) {
    if (this.inProgress == false) {
      this.throwError();
    }
    if (this.inputFeildName != null) {
      /*
       * count only if this field is non-null
       */
      Value value = ctx.getValue(this.inputFeildName);
      if (Value.isNull(value)) {
        return;
      }
    }
    this.count++;
  }

  @Override
  public void writeOut(IFieldsCollection outputRow, ServiceContext ctx) {
    if (this.inProgress == false) {
      this.throwError();
    }
    outputRow.setValue(this.outputFieldName, Value.newIntegerValue(this.count));
    this.discard(ctx);
  }

  @Override
  public void reset(ServiceContext ctx) {
    this.count = 0;
    this.inProgress = false;
  }

  @Override
  public void discard(ServiceContext ctx) {
    this.count = 0;
  }
}
