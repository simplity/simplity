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
package org.simplity.core.data;

/**
 * why is the data being input?
 *
 * @author simplity.org
 */
public enum DataPurpose {
  /** only primary key is expected as input */
  READ

  /**
   * fields are used for selection of rows. typically for filtering rows from a view. filter fields
   * may also be there
   */
  ,
  FILTER

  /** each row may ask for different action. specific field will have the desired action. */
  ,
  SAVE
  /**
   * meant for elective update or any other purpose where we accept any subset of expected fields.
   * That is, every field is optional.
   */
  ,
  SUBSET

  /** when the purpose is not directly related to rdbms operations. */
  ,
  OTHERS
}
