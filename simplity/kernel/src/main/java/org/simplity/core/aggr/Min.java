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

import org.simplity.core.service.ServiceContext;

/**
 * sum of a column
 *
 * @author simplity.org
 */
public class Min extends MathAggregator {

  /**
   * @param inputName input name
   * @param outputName output name
   * @param outputIsDecimal true or false
   */
  public Min(String inputName, String outputName, boolean outputIsDecimal) {
    super(inputName, outputName, outputIsDecimal);
    this.accumulatedValue = Double.MAX_VALUE;
  }

  @Override
  protected void accumulateInteger(long value) {
    if (value < this.accumulatedValue) {
      this.accumulatedValue = value;
    }
  }

  @Override
  protected void accumulateDecimal(double value) {
    if (value < this.accumulatedValue) {
      this.accumulatedValue = value;
    }
  }

  @Override
  protected double getDecimalResult() {
    return this.accumulatedValue;
  }

  @Override
  protected long getIntegerResult() {
    return Math.round(this.accumulatedValue);
  }

  @Override
  public void discard(ServiceContext ctx) {
    super.discard(ctx);
    this.accumulatedValue = Double.MAX_VALUE;
  }
}
