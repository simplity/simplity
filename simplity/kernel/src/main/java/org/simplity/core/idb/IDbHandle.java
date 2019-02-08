/*
 * Copyright (c) 2018 simplity.org
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

package org.simplity.core.idb;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Struct;

import org.simplity.core.value.Value;

/**
 * Designed as base interface for all handles. Used for method signature to
 * accept any handle. API consumer may check the specific instance before using
 * it for desired purpose.
 *
 * @author simplity.org
 *
 */
public interface IDbHandle {

	/**
	 * what type of handle is this?
	 *
	 * @return non-null handle type
	 */
	public DbAccessType getHandleType();

	/**
	 *
	 * @return the db driver that was used to create this handle. This should be
	 *         used carefully. driver accessed like this should not be used to
	 *         create more handles, as that may violate the transaction
	 *         boundaries
	 */
	public IDbDriver getDriver();

	/**
	 * Create a struct object for a stored procedure parameter
	 *
	 * @param values
	 *            array that has the value for each field/attribute in the
	 *            struct, in the right order
	 * @param dbObjectType
	 *            as defined in RDBMS
	 * @return object that can be assigned to a struct parameter
	 */
	public Struct createStruct(Value[] values, String dbObjectType);

	/**
	 * Create a struct object for a stored procedure parameter
	 *
	 * @param data
	 *            array that has the value for each field/attribute in the
	 *            struct, in the right order
	 * @param dbObjectType
	 *            as defined in RDBMS
	 * @return object that can be assigned to a struct parameter.
	 * @throws SQLException
	 */
	public Struct createStruct(Object[] data, String dbObjectType) throws SQLException;

	/**
	 * create an Array suitable as value for a stored procedure parameter
	 *
	 * @param values
	 *            array with values for creating Sql-Array
	 * @param dbArrayType
	 *            as defined in the RDBMS
	 * @return object that is suitable to be assigned to an array parameter
	 * @throws SQLException
	 */
	public Array createArray(Value[] values, String dbArrayType) throws SQLException;

	/**
	 * Create a struct array that can be assigned to procedure parameter. This
	 * is delegated to DBDriver because of issues with Oracle driver
	 *
	 * @param structs
	 *            array of structs from which to create the Array object for a
	 *            stored procedure
	 * @param dbArrayType
	 *            as defined in the rdbms
	 * @return object that is suitable to be assigned to stored procedure
	 *         parameter
	 * @throws SQLException
	 */
	public Array createStructArray(Struct[] structs, String dbArrayType) throws SQLException;

	/**
	 * method from IDbDriver replicated for ease-of-use
	 *
	 * @param text
	 *            to be escaped
	 * @return text suitably escaped so that it can be used to set parameter of
	 *         a prepared statement for a LIKE operator
	 */
	public String escapeForLike(String text);

	/**
	 *
	 * @return non-null if this db handle is opened with a non-default schema.
	 *         null if this db handle is opened with default schema. (if you
	 *         need the name of the default schema, you may ask
	 *         <code>IDbDriver<code> for it.
	 */
	public String getSchema();
}
