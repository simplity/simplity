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
package org.simplity.core.sql;

import java.sql.CallableStatement;
import java.sql.SQLException;

/**
 * an interface that defines all advanced, driver specific functionalities of stored procedure
 * parameter. This design helps us to separate driver specific functionalities into specific
 * classes. Class that implement these should be state-less or immutable so that a singleton can do
 * the job
 *
 * @author simplity.org
 */
public abstract class AbstractParameterHandler {
  /**
   * set the given array of primitives to an input parameter for the stored procedure
   *
   * @param statement to which this array is to be set
   * @param arrayType type name as defined in the db stored procedure
   * @param array primitive array
   * @param posn 1-based position of this parameter
   * @throws SQLException if this feature is not supported
   */
  public void setPrimitiveArray(
      CallableStatement statement, String arrayType, Object[] array, int posn) throws SQLException {
    throw new SQLException(
        "Passing array as a stored procedure is not supported with current database driver.");
  }

  /**
   * set the given array of objects (structure) to an input parameter for the stored procedure
   *
   * @param statement to which this array is to be set
   * @param arrayType type name as defined in the db stored procedure
   * @param objectType type name as defined in the db for the object/struct that this array contains
   * @param array each row has values that correspond to the object/struct definition in the db
   *     stored procedure
   * @param posn 1-based position of this parameter
   * @throws SQLException
   */
  public void setStructArray(
      CallableStatement statement, String arrayType, String objectType, Object[][] array, int posn)
      throws SQLException {
    throw new SQLException(
        "Passing array of object/struct to a stored procedure is not supported with current database driver.");
  }

  /**
   * set the given object/struct to an input parameter for the stored procedure
   *
   * @param statement to which this array is to be set
   * @param objectType type name as defined in the db for the object/struct that this array contains
   * @param array that has the values for members/attributes of this object/struct in that order
   * @param posn 1-based position of this parameter
   * @throws SQLException
   */
  public void setStruct(CallableStatement statement, String objectType, Object[] array, int posn)
      throws SQLException {
    throw new SQLException(
        "Passing object/structs to a stored procedure is not supported with current database driver.");
  }
}
