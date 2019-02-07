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

/**
 * sum of a column
 *
 * @author simplity.org
 */
public class Average extends MathAggregator {

  /**
   * @param inputName inputname of the field
   * @param outputName outputname of the field
   * @param outputIsDecimal true or false
   */
  public Average(String inputName, String outputName, boolean outputIsDecimal) {
    super(inputName, outputName, outputIsDecimal);
  }

  @Override
  protected void accumulateInteger(long value) {
    this.accumulatedValue += value;
  }

  @Override
  protected void accumulateDecimal(double value) {
    this.accumulatedValue += value;
  }

  @Override
  protected double getDecimalResult() {
    return this.accumulatedValue;
  }

  @Override
  protected long getIntegerResult() {
    return Math.round(this.accumulatedValue);
  }
}
