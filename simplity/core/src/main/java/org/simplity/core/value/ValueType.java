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
package org.simplity.core.value;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;

import org.simplity.core.dt.BooleanDataType;
import org.simplity.core.dt.DataType;
import org.simplity.core.dt.DateDataType;
import org.simplity.core.dt.NumericDataType;
import org.simplity.core.dt.TextDataType;
import org.simplity.core.dt.TimestampDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * basic type of values used to represent data values in application.
 *
 * @author simplity.org
 */
public enum ValueType {
	/** textual */
	TEXT(Types.VARCHAR, "VARCHAR") {

		@Override
		protected DataType getDataType() {
			return TextDataType.getDefaultInstance();
		}

		@Override
		public Value extractFromRs(ResultSet resultSet, int posn) throws SQLException {
			/*
			 * written for text. others override this
			 */
			String val = resultSet.getString(posn);
			if (resultSet.wasNull()) {
				return Value.VALUE_UNKNOWN_TEXT;
			}
			return Value.newTextValue(val);
		}

		@Override
		public Value extractFromSp(CallableStatement stmt, int posn) throws SQLException {
			/*
			 * written for text. others override this
			 */
			String val = stmt.getString(posn);
			if (stmt.wasNull()) {
				return Value.VALUE_UNKNOWN_TEXT;
			}
			return Value.newTextValue(val);
		}

		@Override
		public Value parse(Object dbObject) {
			return Value.newTextValue(dbObject.toString());
		}

		@Override
		public Value parse(String value) {
			return Value.newTextValue(value);
		}
	},
	/** whole numbers with no fraction */
	INTEGER(Types.BIGINT, "BIGINT") {
		@Override
		protected DataType getDataType() {
			return NumericDataType.getDefaultInstance();
		}

		@Override
		public Value extractFromRs(ResultSet resultSet, int idx) throws SQLException {
			long val = resultSet.getLong(idx);
			if (resultSet.wasNull()) {
				return Value.VALUE_UNKNOWN_INTEGER;
			}
			return Value.newIntegerValue(val);
		}

		@Override
		public Value extractFromSp(CallableStatement stmt, int idx) throws SQLException {
			long val = stmt.getLong(idx);
			if (stmt.wasNull()) {
				return Value.VALUE_UNKNOWN_INTEGER;
			}
			return Value.newIntegerValue(val);
		}

		@Override
		public Value parse(Object object) {
			if (object instanceof Number) {
				return new IntegerValue(((Number) object).longValue());
			}
			return this.parse(object.toString());
		}

		@Override
		public Value parse(String value) {
			try {
				return new IntegerValue(Long.parseLong(value));
			} catch (Exception e) {
				return Value.VALUE_UNKNOWN_INTEGER;
			}
		}
	},
	/** number with possible fraction */
	DECIMAL(Types.DECIMAL, "DECIMAL") {
		@Override
		protected DataType getDataType() {
			return NumericDataType.getDefaultDecimalInstance();
		}

		@Override
		public Value extractFromRs(ResultSet resultSet, int idx) throws SQLException {
			double val = resultSet.getDouble(idx);
			if (resultSet.wasNull()) {
				return Value.VALUE_UNKNOWN_DECIMAL;
			}
			return Value.newDecimalValue(val);
		}

		@Override
		public Value extractFromSp(CallableStatement stmt, int idx) throws SQLException {
			double val = stmt.getDouble(idx);
			if (stmt.wasNull()) {
				return Value.VALUE_UNKNOWN_DECIMAL;
			}
			return Value.newDecimalValue(val);
		}

		@Override
		public Value parse(Object object) {
			if (object instanceof Number) {
				return new DecimalValue(((Number) object).doubleValue());
			}
			return this.parse(object.toString());
		}

		@Override
		public Value parse(String value) {
			try {
				return new DecimalValue(Double.parseDouble(value));
			} catch (Exception e) {
				return Value.VALUE_UNKNOWN_DECIMAL;
			}
		}
	},
	/**
	 * true-false we would have loved to call it binary, but unfortunately that
	 * has different connotation :-)
	 */
	BOOLEAN(Types.BOOLEAN, "BOOLEAN") {
		@Override
		protected DataType getDataType() {
			return BooleanDataType.getDefaultInstance();
		}

		@Override
		public Value extractFromRs(ResultSet resultSet, int idx) throws SQLException {
			/*
			 * oracle does not have boolean. DB Designers routinely use char as
			 * boolean. We insist on 0/1as char in that case for boolean
			 */
			Object obj = resultSet.getObject(idx);
			if (resultSet.wasNull()) {
				return Value.VALUE_UNKNOWN_BOOLEAN;
			}
			return this.parse(obj);
		}

		@Override
		public Value extractFromSp(CallableStatement stmt, int idx) throws SQLException {
			Object obj = stmt.getObject(idx);
			if (stmt.wasNull()) {
				return Value.VALUE_UNKNOWN_BOOLEAN;
			}
			return this.parseObject(obj);
		}

		@Override
		public Value parse(Object object) {
			if (object instanceof Boolean) {
				if ((Boolean) object) {
					return Value.VALUE_TRUE;
				}
				return Value.VALUE_FALSE;
			}
			return this.parse(object.toString());
		}

		@Override
		public Value parse(String value) {
			String v = value.toLowerCase();
			if (v.isEmpty() || "0".equals(v) || "n".equals(v) || "false".equals(v)) {
				return Value.VALUE_FALSE;
			}
			if ("1".equals(value) || "y".equals(v) || "true".equals(v)) {
				return Value.VALUE_TRUE;
			}
			return Value.VALUE_UNKNOWN_BOOLEAN;
		}

	},
	/**
	 * date as in a calendar. time-zone insensitive, possibly with specific time
	 * of day
	 */
	DATE(Types.DATE, "DATE") {
		@Override
		protected DataType getDataType() {
			return DateDataType.getDefaultInstance();
		}

		@Override
		public Value extractFromRs(ResultSet resultSet, int idx) throws SQLException {
			Date val = resultSet.getDate(idx);
			if (val == null) {
				return Value.VALUE_UNKNOWN_DATE;
			}
			return Value.newDateValue(val.toLocalDate());
		}

		@Override
		public Value extractFromSp(CallableStatement stmt, int idx) throws SQLException {
			Date val = stmt.getDate(idx);
			if (val == null) {
				return Value.VALUE_UNKNOWN_DATE;
			}
			return Value.newDateValue(val.toLocalDate());
		}

		@Override
		public Value parse(Object dbObject) {
			if (dbObject instanceof Date) {
				return new DateValue(((Date) dbObject).toLocalDate());
			}
			if (dbObject instanceof LocalDate) {
				return new DateValue((LocalDate) dbObject);
			}
			try {
				return new DateValue(LocalDate.parse(dbObject.toString()));
			} catch (Exception ignore) {
				return Value.VALUE_UNKNOWN_DATE;
			}
		}

		@Override
		public Value parse(String value) {
			try {
				return new DateValue(LocalDate.parse(value));
			} catch (Exception ignore) {
				return Value.VALUE_UNKNOWN_DATE;
			}
		}
	},
	/**
	 * an instance of time. This is ALWAYS the number of nano seconds from
	 * UTC-Epoch. This may have to be converted to local time for printing
	 */
	TIMESTAMP(Types.TIMESTAMP, "TIMESTAMP") {
		@Override
		protected DataType getDataType() {
			return TimestampDataType.getDefaultInstance();
		}

		@Override
		public Value extractFromRs(ResultSet resultSet, int idx) throws SQLException {
			Timestamp ts = resultSet.getTimestamp(idx);
			if (resultSet.wasNull()) {
				return Value.VALUE_UNKNOWN_TIMESTAMP;
			}
			return new TimestampValue(ts.toInstant());
		}

		@Override
		public Value extractFromSp(CallableStatement stmt, int idx) throws SQLException {
			Timestamp ts = stmt.getTimestamp(idx);
			if (stmt.wasNull()) {
				return Value.VALUE_UNKNOWN_TIMESTAMP;
			}
			return new TimestampValue(ts.toInstant());
		}

		@Override
		public Value parse(Object object) {
			if (object instanceof Timestamp) {
				return new TimestampValue(((Timestamp) object).toInstant());
			}

			if (object instanceof Instant) {
				return new TimestampValue((Instant) object);
			}

			try {
				return new TimestampValue(Instant.parse(object.toString()));
			} catch (Exception ignore) {
				return Value.VALUE_UNKNOWN_TIMESTAMP;
			}
		}

		@Override
		public Value parse(String value) {

			try {
				return new TimestampValue(Instant.parse(value));
			} catch (Exception ignore) {
				return Value.VALUE_UNKNOWN_TIMESTAMP;
			}

		}
	};
	protected static final Logger logger = LoggerFactory.getLogger(ValueType.class);

	protected static final String NULL = "null";
	protected static final char ZERO = '0';
	protected static final char N = 'N';
	protected static char N1 = 'n';

	protected final int sqlType;
	protected final String sqlText;
	protected final DataType defaultDataType;

	ValueType(int sqlType, String sqlText) {
		this.sqlType = sqlType;
		this.sqlText = sqlText;
		this.defaultDataType = this.getDataType();
	}

	protected abstract DataType getDataType();

	/**
	 * @return sql type that can be used to register a parameter of this type
	 */
	public int getSqlType() {
		return this.sqlType;
	}

	/** @return get the sql type text */
	public String getSqlTypeText() {
		return this.sqlText;
	}

	/** @return a default data type for this value type */
	public DataType getDefaultDataType() {
		return this.defaultDataType;
	}

	/**
	 * extracts the value from result set
	 *
	 * @param resultSet
	 * @param posn
	 *            1-based index position of the column in the result set
	 * @return value
	 * @throws SQLException
	 */
	public abstract Value extractFromRs(ResultSet resultSet, int posn) throws SQLException;

	/**
	 * extracts the value from result of a stored procedure (callable
	 * statement)x
	 *
	 * @param stmt
	 * @param posn
	 * @return value
	 * @throws SQLException
	 */
	public abstract Value extractFromSp(CallableStatement stmt, int posn) throws SQLException;

	/**
	 * registers return type of a stored procedure
	 *
	 * @param statement
	 * @param posn
	 * @throws SQLException
	 */
	public void registerForSp(CallableStatement statement, int posn) throws SQLException {
		statement.registerOutParameter(posn, this.sqlType);
	}

	/**
	 * @param object
	 *            as returned by a resultSet.getObject() or json.opt()
	 * @return object converted to value of this value type
	 */
	public Value parseObject(Object object) {
		if (object == null) {
			return Value.newUnknownValue(this);
		}
		return this.parse(object);
	}

	/**
	 * parse/convert object instance to specific value
	 *
	 * @param text
	 *            non-null
	 * @return non-null, but the value.isUNknown() is true if the value is nu
	 *         could be if text could not be parsed into this type of value
	 */
	public abstract Value parse(String text);

	protected abstract Value parse(Object dbObject);
}
