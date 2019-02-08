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

package org.simplity.core.aggr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.simplity.core.ApplicationError;
import org.simplity.core.data.IFieldsCollection;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.value.DecimalValue;
import org.simplity.core.value.InvalidValueException;
import org.simplity.core.value.Value;

/**
 * sum of a column
 *
 * @author simplity.org
 */
public abstract class MathAggregator implements IAggregationWorker {
  private static final Logger logger = LoggerFactory.getLogger(MathAggregator.class);

  /** field name from which to accumulate sum */
  private final String inputName;

  /** field name to which sum is to be written out */
  private final String outputName;

  /** is the output decimal or int? */
  private final boolean outputAsDecimal;

  /** state variable. Current sum */
  protected double accumulatedValue;

  /** not all of them need count, but it is not a big deal!! */
  protected int count;
  /** keep track of accumulation and throw an exception in case of concurrency issues */
  private boolean inProgress;

  /**
   * create an an instance with the required parameters
   *
   * @param inputName field/column name that is being accumulated. non-empty, non-null;
   * @param outputName field/column name that is to be written out as sum. non-empty, non-null;
   * @param outputIsDecimal true if the output is a decimal value, else it is an integral value
   */
  public MathAggregator(String inputName, String outputName, boolean outputIsDecimal) {
    this.inputName = inputName;
    this.outputName = outputName;
    this.outputAsDecimal = outputIsDecimal;
  }

  /*
   * (non-Javadoc)
   *
   * @see aggr.Aggregator#init(org.simplity.service.ServiceContext)
   */
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

  /*
   * (non-Javadoc)
   *
   * @see aggr.Aggregator#accumulate(org.simplity.kernel.data.FieldsInterface,
   * org.simplity.service.ServiceContext)
   */
  @Override
  public void accumulate(IFieldsCollection currentRow, ServiceContext ctx) {
    if (this.inProgress == false) {
      this.throwError();
    }
    Value val = currentRow.getValue(this.inputName);
    if (Value.isNull(val)) {

      logger.info(this.inputName + " has no value to accumulate");

      return;
    }
    this.count++;
    try {
      if (val instanceof DecimalValue) {
        this.accumulateDecimal(val.toDecimal());
      } else {
        this.accumulateInteger(val.toInteger());
      }
    } catch (InvalidValueException e) {
      throw new ApplicationError(
          e, "Aggregator has a data type that is not suitable for the designed operation.");
    }
  }

  /** @param value value to be accumulated */
  protected abstract void accumulateInteger(long value);

  /** @param value value to be accumulated */
  protected abstract void accumulateDecimal(double value);

  /** @return result of the accumulation */
  protected abstract double getDecimalResult();

  /** @return result of the accumulation */
  protected abstract long getIntegerResult();

  /*
   * (non-Javadoc)
   *
   * @see aggr.Aggregator#writeOut(org.simplity.kernel.data.FieldsInterface,
   * org.simplity.service.ServiceContext)
   */
  @Override
  public void writeOut(IFieldsCollection outputRow, ServiceContext ctx) {
    if (this.inProgress == false) {
      this.throwError();
    }
    Value value = null;
    if (this.outputAsDecimal) {
      double result = 0;
      if (this.count != 0) {
        result = this.getDecimalResult();
      }
      value = Value.newDecimalValue(result);
    } else {
      long result = 0;
      if (this.count != 0) {
        result = this.getIntegerResult();
      }
      value = Value.newIntegerValue(result);
    }
    outputRow.setValue(this.outputName, value);
    this.discard(ctx);
  }

  /*
   * (non-Javadoc)
   *
   * @see aggr.Aggregator#reset(org.simplity.service.ServiceContext)
   */
  @Override
  public void reset(ServiceContext ctx) {
    this.accumulatedValue = 0;
    this.count = 0;
    this.inProgress = false;
  }

  /*
   * (non-Javadoc)
   *
   * @see aggr.Aggregator#discard(org.simplity.service.ServiceContext)
   */
  @Override
  public void discard(ServiceContext ctx) {
    this.accumulatedValue = 0;
    this.count = 0;
  }
}
