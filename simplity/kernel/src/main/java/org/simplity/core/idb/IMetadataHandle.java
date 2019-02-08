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

import org.simplity.core.data.IDataSheet;

/**
 * db handle to get meta data about tables, columns etc..
 *
 * @author simplity.org
 *
 */
public interface IMetadataHandle extends IDbHandle {

	/**
	 * get tables/views defined in the database
	 *
	 * @param tableName
	 *            null, pattern or name
	 * @return data sheet that has attributes for tables/views. Null if no
	 *         output
	 */
	public IDataSheet getTables(String tableName);

	/**
	 * get column names of a table
	 *
	 * @param tableName
	 *            can be null to get all tables or pattern, or actual name
	 * @return sheet with one row per column. Null if no columns.
	 */
	public IDataSheet getTableColumns(String tableName);

	/**
	 * get primary keys for all tables
	 *
	 * @return data sheet that has primary key details for all tables
	 */
	public IDataSheet getPrimaryKeys();

	/**
	 * get key columns names of a table
	 *
	 * @param tableName
	 *            non-null
	 * @return key column names
	 */
	public String[] getPrimaryKeysForTable(String tableName);

	/**
	 * get stored procedures
	 *
	 * @param procedureName
	 *            null, pattern or name
	 * @return data sheet that has attributes of procedures. Null if no output
	 */
	public IDataSheet getProcedures(String procedureName);

	/**
	 * get parameters of procedure
	 *
	 * @param procedureName
	 *            null, pattern or name
	 * @return sheet with one row per column. Null if this table does not exist,
	 *         or something went wrong!!
	 */
	public IDataSheet getProcedureParams(String procedureName);

	/**
	 * get structures/user defined types
	 *
	 * @param structName
	 *            null or pattern.
	 * @return data sheet containing attributes of structures. Null of no output
	 */
	public IDataSheet getStructs(String structName);

	/**
	 * get attributes of structure (user defined data type)
	 *
	 * @param structName
	 *            null for all or pattern/name
	 * @return sheet with one row per column. Null if no output
	 */
	public IDataSheet getStructAttributes(String structName);
}
