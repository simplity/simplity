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
package org.simplity.kernel.value;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.file.FileManager;
import org.simplity.kernel.util.DateUtil;
import org.simplity.kernel.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * basic type of values used to represent data values in application.
 *
 * @author simplity.org
 */
public enum ValueType {
	/** textual */
	TEXT(Types.VARCHAR, "VARCHAR", "_text") {

		@Override
		public void toJson(String value, StringBuilder json) {
			JsonUtil.appendQoutedText(value, json);
		}

		@Override
		public Value extractFromRs(ResultSet resultSet, int posn) throws SQLException {
			/*
			 * written for text. others override this
			 */
			String val = resultSet.getString(posn);
			if (resultSet.wasNull()) {
				return Value.newUnknownValue(TEXT);
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
				return Value.newUnknownValue(TEXT);
			}
			return Value.newTextValue(val);
		}

		@Override
		public Value fromObject(Object dbObject) {
			return Value.newTextValue(dbObject.toString());
		}
	},
	/** whole numbers with no fraction */
	INTEGER(Types.BIGINT, "BIGINT", "_number") {
		@Override
		public Value extractFromRs(ResultSet resultSet, int idx) throws SQLException {
			long val = resultSet.getLong(idx);
			if (resultSet.wasNull()) {
				return Value.newUnknownValue(INTEGER);
			}
			return Value.newIntegerValue(val);
		}

		@Override
		public Value extractFromSp(CallableStatement stmt, int idx) throws SQLException {
			long val = stmt.getLong(idx);
			if (stmt.wasNull()) {
				return Value.newUnknownValue(INTEGER);
			}
			return Value.newIntegerValue(val);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.simplity.kernel.value.ValueType#fromObject(java.lang.Object)
		 */
		@Override
		public Value fromObject(Object dbObject) {
			long val = 0;
			if (dbObject instanceof Number) {
				val = ((Number) dbObject).longValue();
			} else {
				try {
					val = Long.parseLong(dbObject.toString());
				} catch (Exception e) {

					logger.info(dbObject.toString() + " is an invalid number.");

					return null;
				}
			}
			return Value.newIntegerValue(val);
		}

		@Override
		public Value[] toValues(Object[] arr) {
			int n = arr.length;
			Value[] result = new Value[n];
			int i = 0;
			for (Object obj : arr) {
				result[i] = this.fromObject(obj);
				i++;
			}
			return result;
		}
	},
	/** number with possible fraction */
	DECIMAL(Types.DECIMAL, "DECIMAL", "_decimal") {
		@Override
		public Value extractFromRs(ResultSet resultSet, int idx) throws SQLException {
			double val = resultSet.getDouble(idx);
			if (resultSet.wasNull()) {
				return Value.newUnknownValue(DECIMAL);
			}
			return Value.newDecimalValue(val);
		}

		@Override
		public Value extractFromSp(CallableStatement stmt, int idx) throws SQLException {
			double val = stmt.getDouble(idx);
			if (stmt.wasNull()) {
				return Value.newUnknownValue(DECIMAL);
			}
			return Value.newDecimalValue(val);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.simplity.kernel.value.ValueType#fromObject(java.lang.Object)
		 */
		@Override
		public Value fromObject(Object dbObject) {
			double val = 0;
			if (dbObject instanceof Number) {
				val = ((Number) dbObject).doubleValue();
			} else {
				try {
					val = Double.parseDouble(dbObject.toString());
				} catch (Exception e) {

					logger.info(dbObject.toString() + " is an invalid number.");

					return null;
				}
			}
			return Value.newDecimalValue(val);
		}

		@Override
		public Value[] toValues(Object[] arr) {
			Number[] vals = (Number[]) arr;
			int n = vals.length;
			Value[] result = new Value[n];
			for (int i = 0; i < n; i++) {
				result[i] = Value.newDecimalValue(vals[i].doubleValue());

				logger.info(arr[i] + " got extracted into " + result[i]);
			}
			return result;
		}
	},
	/**
	 * true-false we would have loved to call it binary, but unfortunately that
	 * has different connotation :-)
	 */
	BOOLEAN(Types.BOOLEAN, "BOOLEAN", "_boolean") {
		@Override
		public Value extractFromRs(ResultSet resultSet, int idx) throws SQLException {
			Object obj = resultSet.getObject(idx);
			if (resultSet.wasNull()) {
				return Value.newUnknownValue(BOOLEAN);
			}
			boolean val = false;
			if (obj instanceof Boolean) {
				val = ((Boolean) obj).booleanValue();
			} else {
				val = this.parse(obj);
			}
			if (val) {
				return Value.VALUE_TRUE;
			}
			return Value.VALUE_FALSE;
		}

		@Override
		public Value extractFromSp(CallableStatement stmt, int idx) throws SQLException {
			Object obj = stmt.getObject(idx);
			if (stmt.wasNull()) {
				return Value.newUnknownValue(BOOLEAN);
			}
			boolean val = false;
			if (obj instanceof Boolean) {
				val = ((Boolean) obj).booleanValue();
			} else {
				val = this.parse(obj);
			}
			if (val) {
				return Value.VALUE_TRUE;
			}
			return Value.VALUE_FALSE;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.simplity.kernel.value.ValueType#fromObject(java.lang.Object)
		 */
		@Override
		public Value fromObject(Object dbObject) {
			boolean val = true;
			if (dbObject instanceof Boolean) {
				val = ((Boolean) dbObject).booleanValue();
			} else {
				val = this.parse(dbObject);
			}
			if (val) {
				return Value.VALUE_TRUE;
			}
			return Value.VALUE_FALSE;
		}

		@Override
		public Value[] toValues(Object[] arr) {
			Boolean[] vals = (Boolean[]) arr;
			int n = vals.length;
			Value[] result = new Value[n];
			for (int i = 0; i < n; i++) {
				result[i] = Value.newBooleanValue(vals[i].booleanValue());
			}
			return result;
		}

		private boolean parse(Object obj) {
			String str = obj.toString();
			if (str.length() == 0) {
				return false;
			}
			char c = str.charAt(0);
			if (c == ZERO || c == N || c == N1) {
				return false;
			}
			return true;
		}
	},
	/** date, possibly with specific time of day */
	DATE(Types.DATE, "DATE", "_dateTime") {
		@Override
		public Value extractFromRs(ResultSet resultSet, int idx) throws SQLException {
			Timestamp val = resultSet.getTimestamp(idx);
			if (resultSet.wasNull()) {
				return Value.newUnknownValue(DATE);
			}
			return Value.newDateValue(val);
		}

		@Override
		public Value extractFromSp(CallableStatement stmt, int idx) throws SQLException {
			Timestamp val = stmt.getTimestamp(idx);
			if (stmt.wasNull()) {
				return Value.newUnknownValue(DATE);
			}
			return Value.newDateValue(val);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.simplity.kernel.value.ValueType#fromObject(java.lang.Object)
		 */
		@Override
		public Value fromObject(Object dbObject) {
			if (dbObject instanceof java.util.Date) {
				return Value.newDateValue(((java.util.Date) dbObject).getTime());
			}

			if (dbObject instanceof Number) {
				return Value.newDateValue(new Date(((Number) dbObject).longValue()));
			}

			String val = dbObject.toString();
			java.util.Date date = DateUtil.parseDateWithOptionalTime(val);
			if (date != null) {
				return Value.newDateValue(date);
			}
			return null;
		}

		@Override
		public Value[] toValues(Object[] arr) {
			java.util.Date[] vals = (java.util.Date[]) arr;
			int n = vals.length;
			Value[] result = new Value[n];
			n = 0;
			for (java.util.Date date : vals) {
				result[n] = Value.newDateValue(date);
				n++;
			}
			return result;
		}
	},
	/**
	 * clob : a wrapper on text specifically for RDBMS clob field. Behaviour
	 * mimics text except for RDBMS I/O
	 */
	CLOB(Types.CLOB, "CLOB", "_clob") {

		/*
		 * (non-Javadoc) a field of this data type actually contains a text
		 * value that is key to the CLOB value stored elsewhere. Hence it is
		 * same as text
		 *
		 * @see org.simplity.kernel.value.ValueType#toJson(java.lang.String,
		 * java.lang.StringBuilder)
		 */
		@Override
		public void toJson(String value, StringBuilder json) {
			JsonUtil.appendQoutedText(value, json);
		}

		/*
		 * (non-Javadoc) We extract the content and save it to a temp file. We
		 * return the key to this storage as value
		 *
		 * @see
		 * org.simplity.kernel.value.ValueType#extractFromRs(java.sql.ResultSet,
		 * int)
		 */
		@Override
		public Value extractFromRs(ResultSet resultSet, int posn) throws SQLException {
			Clob clob = resultSet.getClob(posn);
			if (resultSet.wasNull()) {
				clob = null;
			}
			return this.saveIt(clob);
		}

		@Override
		public Value extractFromSp(CallableStatement stmt, int posn) throws SQLException {
			Clob clob = stmt.getClob(posn);
			if (stmt.wasNull()) {
				clob = null;
			}
			return this.saveIt(clob);
		}

		@Override
		public Value fromObject(Object dbObject) {
			return Value.newClobValue(dbObject.toString());
		}

		/*
		 * save clob into file
		 */
		private Value saveIt(Clob clob) {
			if (clob == null) {
				return Value.newUnknownValue(CLOB);
			}
			try (Reader reader = clob.getCharacterStream()) {
				File file = FileManager.createTempFile(reader);
				if (file == null) {
					throw new ApplicationError("Unable to save clob value to a tmp storage.");
				}
				return Value.newClobValue(file.getName());
			} catch (Exception e) {
				throw new ApplicationError(e, "Error while saving clob value to a tmp storage.");
			}
		}
	},
	/**
	 * Blob : this is a sub-class of text that behaves differently ONLY when
	 * dealing with RDBMS i/o
	 */
	BLOB(Types.BLOB, "BLOB", "_blob") {

		@Override
		public void toJson(String value, StringBuilder json) {
			JsonUtil.appendQoutedText(value, json);
		}

		@Override
		public Value extractFromRs(ResultSet resultSet, int posn) throws SQLException {
			Blob blob = resultSet.getBlob(posn);
			if (resultSet.wasNull()) {
				return this.saveIt(null);
			}
			return this.saveIt(blob);
		}

		@Override
		public Value extractFromSp(CallableStatement stmt, int posn) throws SQLException {
			Blob blob = stmt.getBlob(posn);
			if (stmt.wasNull()) {
				return this.saveIt(null);
			}
			return this.saveIt(blob);
		}

		@Override
		public Value fromObject(Object dbObject) {
			return Value.newBlobValue(dbObject.toString());
		}

		/*
		 * save blob into file
		 */
		private Value saveIt(Blob blob) {
			if (blob == null) {
				return Value.newUnknownValue(BLOB);
			}
			try (InputStream stream = blob.getBinaryStream()) {
				File file = FileManager.createTempFile(stream);
				if (file == null) {
					throw new ApplicationError("Unable to save blob value to a tmp storage.");
				}
				return Value.newBlobValue(file.getName());
			} catch (Exception e) {
				throw new ApplicationError(e, "Error while saving blob field to a temp file");
			}
		}
	},
	/** time-stamp that is specifically used for RDBMS related operations */
	TIMESTAMP(Types.TIMESTAMP, "TIMESTAMP", "_timestamp") {
		@Override
		public Value extractFromRs(ResultSet resultSet, int idx) throws SQLException {
			Timestamp ts = resultSet.getTimestamp(idx);
			if (resultSet.wasNull()) {
				return Value.newUnknownValue(INTEGER);
			}
			return new TimestampValue(ts);
		}

		@Override
		public Value extractFromSp(CallableStatement stmt, int idx) throws SQLException {
			Timestamp ts = stmt.getTimestamp(idx);
			if (stmt.wasNull()) {
				return Value.newUnknownValue(INTEGER);
			}
			return new TimestampValue(ts);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.simplity.kernel.value.ValueType#fromObject(java.lang.Object)
		 * )
		 */
		@Override
		public Value fromObject(Object dbObject) {
			long val = 0;
			if (dbObject instanceof Timestamp) {
				return Value.newTimestampValue((Timestamp) dbObject);
			}
			try {
				val = Long.parseLong(dbObject.toString());
			} catch (Exception e) {
				return null;
			}
			return Value.newTimestampValue(val);
		}

		@Override
		public Value[] toValues(Object[] arr) {
			int n = arr.length;
			Value[] result = new Value[n];
			int i = 0;
			for (Object obj : arr) {
				result[i] = this.fromObject(obj);
				i++;
			}
			return result;
		}
	};
	protected static final Logger logger = LoggerFactory.getLogger(ValueType.class);

	protected static final String NULL = "null";
	protected static final char ZERO = '0';
	protected static final char N = 'N';
	protected static char N1 = 'n';

	protected final int sqlType;
	protected final String sqlText;
	protected final String defaultDataType;

	ValueType(int sqlType, String sqlText, String dt) {
		this.sqlType = sqlType;
		this.sqlText = sqlText;
		this.defaultDataType = dt;
	}

	/**
	 * @return sql type that can be used to register a parameter of this type
	 */
	public int getSqlType() {
		return this.sqlType;
	}

	/**
	 * extracts the value from result set at the current index
	 *
	 * @param resultSet
	 * @param posn
	 *            1-based index position of the column in the result set
	 * @return value
	 * @throws SQLException
	 */
	public abstract Value extractFromRs(ResultSet resultSet, int posn) throws SQLException;

	/**
	 * extracts the value from result set at the current index
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
	 * assuming that the supplied value is a valid value, format it for a json
	 *
	 * @param value
	 *            text input
	 * @param json
	 *            to which value is to be appended
	 */
	public void toJson(String value, StringBuilder json) {
		if (value == null || value.length() == 0) {
			json.append(NULL);
		} else {
			json.append(value);
		}
	}

	/**
	 * @param arr
	 * @return value list for the array object
	 */
	public Value[] toValues(Object[] arr) {

		logger.info("Going to convert " + arr.length + " objects into " + this.name());

		int n = arr.length;
		Value[] result = new Value[n];
		for (int i = 0; i < n; i++) {
			Object obj = arr[i];
			String val = obj == null ? "null" : obj.toString();
			result[i] = this.fromObject(val);
		}
		return result;
	}

	/**
	 * @param object
	 *            as returned by a resultSet.getObject() or json.opt()
	 * @return object converted to value of this value type
	 */
	public Value parseObject(Object object) {
		if (object == null) {

			logger.info("Parse Object received null for type " + this.name()
					+ ". Client may receive null or empty string depending on the setting.");

			return Value.newUnknownValue(this);
		}
		return this.fromObject(object);
	}

	/**
	 * parse/convert object instance to specific value
	 *
	 * @param dbObject
	 * @return value. null if dbObject is not the right object for this value
	 *         type.
	 */
	public abstract Value fromObject(Object dbObject);

	/** @return get the sql type text */
	public String getSqlTypeText() {
		return this.sqlText;
	}

	/** @return a default data type for this value type */
	public String getDefaultDataType() {
		return this.defaultDataType;
	}

	/**
	 * @param sqlType
	 * @return the ValueType name based on SQL Type
	 */
	public static ValueType getValueType(String sqlType) {
		for (ValueType value : ValueType.values()) {
			if (value.getSqlTypeText().equals(sqlType)) {
				return value;
			}
		}
		return null;
	}
}
