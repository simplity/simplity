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

/**
 * In this iterator, we do not use a row explicitly, but implicitly by changing the default row of
 * the sheet.
 *
 * @author simplity.org
 */
public interface IDataSheetIterator {
  /**
   * peek operation. Does not change the state of the iterator
   *
   * @return true if a call to moveToNextRow() is going to return true or false.
   */
  public boolean hasNext();

  /**
   * make the next row as default in the data sheet. getField() will now return value from next row.
   *
   * @return true if the next row is made default. false if we have reached the end.
   */
  public boolean moveToNextRow();

  /**
   * should be called if the caller does not complete the iteration. failing to do so will keep the
   * sheet in an iterative state, there-by
   */
  public void cancelIteration();
}
