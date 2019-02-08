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

/** @author simplity.org */
public enum AggregationType {
  /** sum of numeric column */
  SUM {
    @Override
    public IAggregationWorker getAggregator(
        String inputFieldName, String outputFieldName, boolean outputAsDecimal) {
      return new Sum(inputFieldName, outputFieldName, outputAsDecimal);
    }
  },
  /** average */
  AVERAGE {
    @Override
    public IAggregationWorker getAggregator(
        String inputFieldName, String outputFieldName, boolean outputAsDecimal) {
      return new Average(inputFieldName, outputFieldName, outputAsDecimal);
    }
  },
  /** count */
  COUNT {
    @Override
    public IAggregationWorker getAggregator(
        String inputFieldName, String outputFieldName, boolean outputAsDecimal) {
      return new Count(inputFieldName, outputFieldName);
    }
  },
  /** max */
  MAX {
    @Override
    public IAggregationWorker getAggregator(
        String inputFieldName, String outputFieldName, boolean outputAsDecimal) {
      return new Max(inputFieldName, outputFieldName, outputAsDecimal);
    }
  },
  /** min */
  MIN {
    @Override
    public IAggregationWorker getAggregator(
        String inputFieldName, String outputFieldName, boolean outputAsDecimal) {
      return new Min(inputFieldName, outputFieldName, outputAsDecimal);
    }
  },
  /** first */
  FIRST {
    @Override
    public IAggregationWorker getAggregator(
        String inputFieldName, String outputFieldName, boolean outputAsDecimal) {
      return new First(inputFieldName, outputFieldName);
    }
  },
  /** last */
  LAST {
    @Override
    public IAggregationWorker getAggregator(
        String inputFieldName, String outputFieldName, boolean outputAsDecimal) {
      return new Last(inputFieldName, outputFieldName);
    }
  };

  /**
   * @param inputFieldName input field for aggregation
   * @param outputFieldName output field for aggregation
   * @param outputAsDecimal true or false
   * @return an aggregation worker
   */
  public abstract IAggregationWorker getAggregator(
      String inputFieldName, String outputFieldName, boolean outputAsDecimal);
}
