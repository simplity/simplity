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

import org.simplity.core.data.IFieldsCollection;
import org.simplity.core.service.ServiceContext;

/**
 * Functionality required to accumulate/aggregate column value/s across rows. init() is invoked once
 * for a table/sheet/rows. accumulate() is called for each row of data and writeOut() is called as
 * and when the accumulated value is to be written out. For example when the group-by fields change,
 * or when there are no more rows
 *
 * @author simplity.org
 */
public interface IAggregationWorker {
  /**
   * set-up shop and get ready to start aggregation process. Utility for a class to reset in case it
   * is re-used across aggregation cycles. Implementations should put safety against init() when
   * accumulation is in progress. Best practice would be a reset() and then init()
   *
   * @param ctx service context
   */
  public void init(ServiceContext ctx);

  /**
   * called with fields from the current row of data. Main method for keep accumulating data
   *
   * @param currentRow fields from current row being aggregated
   * @param ctx service context
   */
  public void accumulate(IFieldsCollection currentRow, ServiceContext ctx);

  /**
   * end of rows for the current accumulation. Write-out your result, and get ready for the next
   * accumulation. Note that init is NOT called after this. Also, this is called when there are no
   * more rows
   *
   * @param outputRow Output Row
   * @param ctx Service Context
   */
  public void writeOut(IFieldsCollection outputRow, ServiceContext ctx);

  /**
   * invoked in case the accumulated value is to be discarded (opposite of writeOut)
   *
   * @param ctx Service Context
   */
  public void discard(ServiceContext ctx);

  /**
   * reset to a state as if this was never called earlier..
   *
   * @param ctx Service Context
   */
  public void reset(ServiceContext ctx);
}
