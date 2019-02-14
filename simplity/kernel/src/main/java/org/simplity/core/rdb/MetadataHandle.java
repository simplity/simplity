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

package org.simplity.core.rdb;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;

import org.simplity.core.ApplicationError;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.data.MultiRowsSheet;
import org.simplity.core.idb.DbAccessType;
import org.simplity.core.idb.IMetadataHandle;
import org.simplity.core.value.IntegerValue;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;

/**
 * @author simplity.org
 *
 */
public class MetadataHandle extends AbstractHandle implements IMetadataHandle {
	private static final DbAccessType HANDLE_TYPE = DbAccessType.META_DATA;
	/*
	 * we are going to use value types s many time, it is ugly to use full name.
	 * Let us have some short and sweet names
	 */
	private static final ValueType INT = ValueType.INTEGER;
	private static final ValueType TXT = ValueType.TEXT;
	private static final ValueType BOOL = ValueType.BOOLEAN;
	/*
	 * names, types and positions as per result set for meta.getTables()
	 */
	private static final int TABLE_IDX = 0;
	private static final String[] TABLE_NAMES = { "schema", "tableName", "tableType", "remarks" };
	private static final ValueType[] TABLE_TYPES = { TXT, TXT, TXT, TXT };
	private static final int[] TABLE_POSNS = { 2, 3, 4, 5 };
	private static final String[] TABLE_TYPES_TO_EXTRACT = { "TABLE", "VIEW" };
	/*
	 * names, types and positions as per result set for meta.getTables()
	 */
	private static final int COL_IDX = 1;
	private static final String[] COL_NAMES = { "schema", "tableName", "columnName", "sqlType", "sqlTypeName", "size",
			"nbrDecimals", "remarks", "nullable" };
	private static final ValueType[] COL_TYPES = { TXT, TXT, TXT, INT, TXT, INT, INT, TXT, BOOL };
	private static final int[] COL_POSNS = { 2, 3, 4, 5, 6, 7, 9, 12, 18 };

	/*
	 * names, types and positions as per result set for meta.getTables()
	 */
	private static final int KEY_IDX = 2;
	private static final String[] KEY_NAMES = { "columnName", "sequence" };
	private static final ValueType[] KEY_TYPES = { TXT, INT };
	private static final int[] KEY_POSNS = { 4, 5 };

	/*
	 * names, types and positions as per result set for meta.getTables()
	 */
	private static final int PROC_IDX = 3;
	private static final String[] PROC_NAMES = { "schema", "procedureName", "procedureType", "remarks" };
	private static final ValueType[] PROC_TYPES = { TXT, TXT, INT, TXT };
	private static final int[] PROC_POSNS = { 2, 3, 8, 7 };

	/*
	 * names, types and positions as per result set for meta.getTables()
	 */
	private static final int PARAM_IDX = 4;
	private static final String[] PARAM_NAMES = { "schema", "procedureName", "paramName", "columnType", "sqlType",
			"sqlTypeName", "size", "precision", "scale", "remarks", "nullable", "position" };
	private static final ValueType[] PARAM_TYPES = { TXT, TXT, TXT, INT, INT, TXT, INT, INT, INT, TXT, BOOL, INT };
	private static final int[] PARAM_POSNS = { 2, 3, 4, 5, 6, 7, 9, 8, 10, 13, 19, 18 };

	/*
	 * names, types and positions as per result set for meta.getUDTs()
	 */
	private static final int STRUCT_IDX = 5;
	private static final String[] STRUCT_NAMES = { "schema", "structName", "structType", "remarks" };
	private static final ValueType[] STRUCT_TYPES = { TXT, TXT, TXT, TXT };
	private static final int[] STRUCT_POSNS = { 2, 3, 5, 6 };
	private static final int[] STRUCT_TYPES_TO_EXTRACT = { Types.STRUCT };
	/*
	 * names, types and positions as per result set for meta.getTables()
	 */
	private static final int ATTR_IDX = 6;
	private static final String[] ATTR_NAMES = { "schema", "structName", "attributeName", "sqlType", "sqlTypeName",
			"size", "nbrDecimals", "remarks", "nullable", "position" };
	private static final ValueType[] ATTR_TYPES = { TXT, TXT, TXT, INT, TXT, INT, INT, TXT, BOOL, INT };
	private static final int[] ATTR_POSNS = { 2, 3, 4, 5, 6, 7, 8, 11, 17, 16 };

	/*
	 * put them into array for modularity
	 */
	private static final String[][] META_COLUMNS = { TABLE_NAMES, COL_NAMES, KEY_NAMES, PROC_NAMES, PARAM_NAMES,
			STRUCT_NAMES, ATTR_NAMES };
	private static final ValueType[][] META_TYPES = { TABLE_TYPES, COL_TYPES, KEY_TYPES, PROC_TYPES, PARAM_TYPES,
			STRUCT_TYPES, ATTR_TYPES };
	private static final int[][] META_POSNS = { TABLE_POSNS, COL_POSNS, KEY_POSNS, PROC_POSNS, PARAM_POSNS,
			STRUCT_POSNS, ATTR_POSNS };

	MetadataHandle(Connection con, RdbSetup driver, String schema) {
		super(con, driver, schema);
	}

	@Override
	public DbAccessType getHandleType() {
		return HANDLE_TYPE;
	}

	@Override
	public IDataSheet getTables(String tableName) {
		return this.getMetaSheet(tableName, TABLE_IDX);
	}

	@Override
	public IDataSheet getTableColumns(String tableName) {
		return this.getMetaSheet(tableName, COL_IDX);
	}

	@Override
	public IDataSheet getPrimaryKeys() {
		return this.getMetaSheet(null, KEY_IDX);
	}

	/**
	 * get key columns names of a table
	 *
	 * @param tableName
	 *            non-null
	 * @return key column names
	 */
	@Override
	public String[] getPrimaryKeysForTable(String tableName) {
		if (tableName == null) {
			logger.info(
					"getPrimaryKeysForTable() is for a specific table. If you want for all tables, use the getPrimaryKeys()");
			return null;
		}
		IDataSheet sheet = this.getMetaSheet(tableName, KEY_IDX);
		if (sheet == null) {
			return null;
		}
		int n = sheet.length();
		String[] result = new String[n];
		for (int i = 0; i < n; i++) {
			Value[] row = sheet.getRow(i);
			int idx = (int) ((IntegerValue) row[1]).getLong() - 1;
			result[idx] = row[0].toString();
		}
		return result;
	}

	/**
	 * get stored procedures
	 *
	 * @param procedureName
	 *            null, pattern or name
	 * @return data sheet that has attributes of procedures. Null if no output
	 */
	@Override
	public IDataSheet getProcedures(String procedureName) {
		return this.getMetaSheet(procedureName, PROC_IDX);
	}

	/**
	 * get parameters of procedure
	 *
	 * @param procedureName
	 *            null, pattern or name
	 * @return sheet with one row per column. Null if this table does not exist,
	 *         or something went wrong!!
	 */
	@Override
	public IDataSheet getProcedureParams(String procedureName) {
		return this.getMetaSheet(procedureName, PARAM_IDX);
	}

	/**
	 * get structures/user defined types
	 *
	 * @param structName
	 *            null or pattern.
	 * @return data sheet containing attributes of structures. Null of no output
	 */
	@Override
	public IDataSheet getStructs(String structName) {
		return this.getMetaSheet(structName, STRUCT_IDX);
	}

	/**
	 * get attributes of structure (user defined data type)
	 *
	 * @param structName
	 *            null for all or pattern/name
	 * @return sheet with one row per column. Null if no output
	 */
	@Override
	public IDataSheet getStructAttributes(String structName) {
		return this.getMetaSheet(structName, ATTR_IDX);
	}

	private IDataSheet getMetaSheet(String metaName, int metaIdx) {
		ResultSet rs = null;
		String schema = this.schemaName;
		String defaultSchema = this.dbDriver.getDefaultSchema();
		if (schema == null && defaultSchema != null && defaultSchema.equals("PUBLIC") == false) {
			schema = defaultSchema;
		}
		try {
			DatabaseMetaData meta = this.connection.getMetaData();
			switch (metaIdx) {
			case TABLE_IDX:
				logger.info("trying tables with schema={}, meta={}", this.schemaName, metaIdx);
				rs = meta.getTables(null, schema, metaName, TABLE_TYPES_TO_EXTRACT);
				break;
			case COL_IDX:
				rs = meta.getColumns(null, schema, metaName, null);
				break;
			case KEY_IDX:
				rs = meta.getPrimaryKeys(null, schema, metaName);
				break;
			case PROC_IDX:
				rs = meta.getProcedures(null, schema, metaName);
				break;
			case PARAM_IDX:
				rs = meta.getProcedureColumns(null, schema, metaName, null);
				break;
			case STRUCT_IDX:
				rs = meta.getUDTs(null, schema, metaName, STRUCT_TYPES_TO_EXTRACT);
				break;
			case ATTR_IDX:
				rs = meta.getAttributes(null, schema, metaName, null);
				break;
			default:
				throw new ApplicationError("Meta data " + metaIdx + " is not defined yet.");
			}
			IDataSheet sheet = new MultiRowsSheet(META_COLUMNS[metaIdx], META_TYPES[metaIdx]);
			ValueType[] types = META_TYPES[metaIdx];
			int[] posn = META_POSNS[metaIdx];
			while (rs.next()) {
				Value[] row = new Value[types.length];
				for (int i = 0; i < types.length; i++) {
					row[i] = types[i].extractFromRs(rs, posn[i]);
				}
				sheet.addRow(row);
			}
			return sheet;
		} catch (Exception e) {
			logger.error("Unable to get meta data for " + metaName, e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
					//
				}
			}
		}
		logger.warn("Returnig null data sheet for eta data");
		return null;
	}
}
