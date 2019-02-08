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

package org.simplity.core.util;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.simplity.core.ApplicationError;
import org.simplity.core.app.IResponseWriter;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.data.IFieldsCollection;
import org.simplity.core.data.MultiRowsSheet;
import org.simplity.core.dm.field.Field;
import org.simplity.core.idb.IResultSetReader;
import org.simplity.core.idb.IRowMetaData;
import org.simplity.core.idb.IRowWithNameConsumer;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * utility related to JDBC related operations
 *
 * @author simplity.org
 *
 */
public class RdbUtil {
	private static final Logger logger = LoggerFactory.getLogger(RdbUtil.class);

	/*
	 * we store sql types with corresponding value types
	 */
	private static final int[] LONG_TYPES = { Types.BIGINT, Types.INTEGER, Types.SMALLINT };
	private static final int[] DOUBLE_TYPES = { Types.DECIMAL, Types.DOUBLE, Types.FLOAT, Types.REAL };
	private static final int[] BOOLEAN_TYPES = { Types.BIT, Types.BOOLEAN };
	private static final Map<Integer, ValueType> SQL_TYPES = new HashMap<Integer, ValueType>();
	static {
		for (int i : LONG_TYPES) {
			SQL_TYPES.put(new Integer(i), ValueType.INTEGER);
		}
		SQL_TYPES.put(Types.DATE, ValueType.DATE);
		SQL_TYPES.put(Types.TIME, ValueType.DATE);
		SQL_TYPES.put(Types.TIMESTAMP, ValueType.TIMESTAMP);
		for (int i : DOUBLE_TYPES) {
			SQL_TYPES.put(new Integer(i), ValueType.DECIMAL);
		}
		for (int i : BOOLEAN_TYPES) {
			SQL_TYPES.put(new Integer(i), ValueType.BOOLEAN);
		}
	}

	/**
	 * log/trace a sql
	 *
	 * @param sql
	 * @param values
	 */
	public static void traceSql(String sql, Value[] values) {
		if (values == null || values.length == 0) {
			logger.info(sql);
			return;
		}
		StringBuilder sbf = new StringBuilder(sql);
		sbf.append("\n Parameters");
		int i = 0;
		for (Value value : values) {
			if (value == null) {
				break;
			}
			i++;
			sbf.append('\n').append(i).append(" : ").append(value.toString());
			if (i > 12) {
				sbf.append("..like wise up to ").append(values.length).append(" : ").append(values[values.length - 1]);
				break;
			}
		}
		logger.info(sbf.toString());
	}

	/**
	 * log/trace a sql
	 *
	 * @param sql
	 * @param values
	 */
	public static void traceBatchSql(String sql, Value[][] values) {
		logger.info("SQL :{}", sql);
		int i = 0;
		for (Value[] row : values) {
			if (row == null) {
				break;
			}
			i++;
			logger.info("SET {}", i);
			int j = 0;
			for (Value value : row) {
				if (value == null) {
					break;
				}
				j++;
				logger.info("{} :{} ", j, value);
			}
		}
	}

	private static class MultiReader implements IResultSetReader {
		private final IDataSheet sheet;

		protected MultiReader(IDataSheet sheet) {
			this.sheet = sheet;
		}

		@Override
		public int read(ResultSet rs) {
			int nbr = 0;
			try {
				ValueType[] types = this.sheet.getValueTypes();
				while (rs.next()) {
					this.sheet.addRow(resultToValueRow(rs, types));
					nbr++;
				}
			} catch (SQLException e) {
				throw new ApplicationError(e, "");
			}
			return nbr;
		}
	}

	private static class ReaderForResponseWriter implements IResultSetReader {
		private final IResponseWriter writer;
		private final boolean useCompact;
		private final String[] names;
		private final ValueType[] types;

		protected ReaderForResponseWriter(IResponseWriter writer, boolean useCompactFormat, String[] names,
				ValueType[] types) {
			this.writer = writer;
			this.useCompact = useCompactFormat;
			this.names = names;
			this.types = types;
		}

		@Override
		public int read(ResultSet rs) {
			int nbr = 0;
			try {
				if (this.useCompact) {
					this.writer.beginArrayAsArrayElement();
					for (String nam : this.names) {
						this.writer.addToArray(nam);
					}
					this.writer.endArray();
				}
				while (rs.next()) {
					if (this.useCompact) {
						this.writer.beginArrayAsArrayElement();
					} else {
						this.writer.beginObjectAsArrayElement();
					}
					for (int i = 0; i < this.types.length; i++) {
						Value value = this.types[i].extractFromRs(rs, i + 1);
						if (this.useCompact) {
							this.writer.addToArray(value);
						} else {
							this.writer.setField(this.names[i], value);
						}
					}
					nbr++;
				}
			} catch (SQLException e) {
				throw new ApplicationError(e, "");
			}
			return nbr;
		}
	}

	private static class SingleReader implements IResultSetReader {
		private final IDataSheet sheet;

		protected SingleReader(IDataSheet sheet) {
			this.sheet = sheet;
		}

		@Override
		public int read(ResultSet rs) {
			try {
				if (rs.next()) {
					ValueType[] types = this.sheet.getValueTypes();
					this.sheet.addRow(resultToValueRow(rs, types));
					return 1;
				}
			} catch (SQLException e) {
				throw new ApplicationError(e, "");
			}
			return 0;
		}
	}

	private static class FieldsBasedExtractor implements IResultSetReader {
		private final Field[] fields;
		private final IFieldsCollection data;

		protected FieldsBasedExtractor(IFieldsCollection data, Field[] fields) {
			this.fields = fields;
			this.data = data;
		}

		@Override
		public int read(ResultSet rs) {
			try {
				if (rs.next()) {
					int i = 1;
					for (Field field : this.fields) {
						this.data.setValue(field.getName(), field.getValueType().extractFromRs(rs, i));
						i++;
					}
					return 1;
				}
			} catch (SQLException e) {
				throw new ApplicationError(e, "");
			}
			return 0;
		}
	}

	private static class NamesBasedExtractor implements IResultSetReader {
		private final String[] names;
		private final ValueType[] types;
		private final IFieldsCollection data;

		protected NamesBasedExtractor(IFieldsCollection data, String[] names, ValueType[] types) {
			this.names = names;
			this.data = data;
			this.types = types;
		}

		@Override
		public int read(ResultSet rs) {
			try {
				if (rs.next()) {
					for (int i = 0; i < this.types.length; i++) {
						this.data.setValue(this.names[i], this.types[i].extractFromRs(rs, i + 1));
					}
					return 1;
				}
			} catch (SQLException e) {
				throw new ApplicationError(e, "");
			}
			return 0;
		}
	}

	private static class ReaderForConsumer implements IResultSetReader {
		private final IRowWithNameConsumer consumer;

		protected ReaderForConsumer(IRowWithNameConsumer consumer) {
			this.consumer = consumer;
		}

		@Override
		public int read(ResultSet rs) {
			int nbr = 0;
			try {
				IRowMetaData meta = getColumnMetaData(rs);
				ValueType[] types = meta.getColumnValueTypes();
				String[] names = meta.getColumnNames();
				while (rs.next()) {
					this.consumer.consume(names, resultToValueRow(rs, types));
					nbr++;
				}
			} catch (SQLException e) {
				throw new ApplicationError(e, "");
			}
			return nbr;
		}
	}

	/**
	 *
	 * @param sheet
	 *            to which data rows are to be added
	 * @return non-null. an instance that will append rows from rsult set into
	 *         the data sheet
	 */
	public static IResultSetReader newMultiRowsReader(IDataSheet sheet) {
		return new MultiReader(sheet);
	}

	/**
	 *
	 * @param consumer
	 *            call-back object to be used fro each row
	 * @return non-null. an instance that will append rows from rsult set into
	 *         the data sheet
	 */
	public static IResultSetReader newReaderForConsumer(IRowWithNameConsumer consumer) {
		return new ReaderForConsumer(consumer);
	}

	/**
	 *
	 * @param sheet
	 *            to which one row of data to be added
	 * @return non-null. an instance that will append rows from rsult set into
	 *         the data sheet
	 */
	public static IResultSetReader newSingleRowReader(IDataSheet sheet) {
		return new SingleReader(sheet);
	}

	/**
	 *
	 * @param data
	 *            to which field data are to be extracted
	 * @param fieldsToExtract
	 *            result set is assumed to have theses fields in that order
	 * @return non-null. an instance that will append rows from result set into
	 *         the data sheet
	 */
	public static IResultSetReader newFieldsBasedExtractor(IFieldsCollection data, Field[] fieldsToExtract) {
		return new FieldsBasedExtractor(data, fieldsToExtract);
	}

	/**
	 *
	 * @param data
	 *            to which field data are to be extracted
	 * @param fieldNames
	 *            result set is assumed to have these columns
	 * @param fieldValueTypes
	 *            value types of columns in the result set
	 * @return non-null. an instance that will append rows from result set into
	 *         the data sheet
	 */
	public static IResultSetReader newNamesBasedExtractor(IFieldsCollection data, String[] fieldNames,
			ValueType[] fieldValueTypes) {
		return new NamesBasedExtractor(data, fieldNames, fieldValueTypes);
	}

	/**
	 *
	 * @param writer
	 * @param useCompactFormat
	 * @param fieldNames
	 * @param fieldValueTypes
	 * @return non-null. an instance that will append rows from result set into
	 *         the data sheet
	 */
	public static IResultSetReader newReaderForResponseWriter(IResponseWriter writer, boolean useCompactFormat,
			String[] fieldNames, ValueType[] fieldValueTypes) {
		return new ReaderForResponseWriter(writer, useCompactFormat, fieldNames, fieldValueTypes);
	}

	/**
	 *
	 * @param rs
	 *            non-null result set. must have data ready to be read/ been
	 *            true)
	 * @param types
	 *            non-null array of value types that correspond to the columns
	 *            in the result set
	 * @return non-null array of column values for the current row of thr result
	 *         set
	 * @throws SQLException
	 */
	public static Value[] resultToValueRow(ResultSet rs, ValueType[] types) throws SQLException {
		Value[] values = new Value[types.length];
		for (int i = 0; i < types.length; i++) {
			values[i] = types[i].extractFromRs(rs, i + 1);
		}
		return values;
	}

	/**
	 *
	 * @param rs
	 *            non-null result set. must have data ready to be read
	 * @param types
	 *            non-null array of value types that correspond to the columns
	 *            in the result set
	 * @param positions
	 *            non-null array. contains the 1-based position in the result
	 *            set
	 * @return row that has values, as per types
	 * @throws SQLException
	 */
	public static Value[] resultToValueRow(ResultSet rs, ValueType[] types, int[] positions) throws SQLException {
		Value[] values = new Value[types.length];
		for (int i = 0; i < types.length; i++) {
			values[i] = types[i].extractFromRs(rs, positions[i]);
		}
		return values;
	}

	/**
	 * get column names and types of a result set
	 *
	 * @param rs
	 * @return meta data
	 */
	public static IRowMetaData getColumnMetaData(ResultSet rs) {
		return new ColumnMetaData(rs);
	}

	/**
	 * get column names and types of a result set
	 *
	 * @param rs
	 * @return empty data sheet that has the right columns to receive data from
	 *         the result set
	 */
	public static IDataSheet getDataSheetForSqlResult(ResultSet rs) {
		ColumnMetaData md = new ColumnMetaData(rs);
		return new MultiRowsSheet(md.getColumnNames(), md.getColumnValueTypes());
	}

	/**
	 * class that has the meta data about columns of a sql output
	 *
	 * @author simplity.org
	 *
	 */
	private static class ColumnMetaData implements IRowMetaData {

		private String[] names;
		private ValueType[] types;

		protected ColumnMetaData(ResultSet rs) {
			try {
				ResultSetMetaData md = rs.getMetaData();
				int n = md.getColumnCount();
				this.names = new String[n];
				this.types = new ValueType[n];
				for (int i = 0; i < n; i++) {
					this.names[i] = md.getColumnName(i + 1);
					this.types[i] = sqlTypeToValueType(md.getColumnType(i));
				}
			} catch (SQLException e) {
				throw new ApplicationError(e, "");
			}
		}

		@Override
		public String[] getColumnNames() {
			return this.names;
		}

		@Override
		public ValueType[] getColumnValueTypes() {
			return this.types;
		}
	}

	/**
	 * get valueType for sqltype
	 *
	 * @param sqlType
	 * @return value type
	 */
	public static ValueType sqlTypeToValueType(int sqlType) {
		ValueType type = SQL_TYPES.get(new Integer(sqlType));
		if (type == null) {
			logger.error("Error: We do not support sqlType {}. text type is assumed", sqlType);
			return ValueType.TEXT;
		}
		return type;
	}
}
