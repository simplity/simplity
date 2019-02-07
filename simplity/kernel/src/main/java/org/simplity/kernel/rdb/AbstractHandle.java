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

package org.simplity.kernel.rdb;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Struct;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.idb.DbAccessType;
import org.simplity.kernel.idb.IDbDriver;
import org.simplity.kernel.idb.IDbHandle;
import org.simplity.kernel.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author simplity.org
 *
 */
public abstract class AbstractHandle implements IDbHandle {
	protected static final Logger logger = LoggerFactory.getLogger(AbstractHandle.class);
	/**
	 * connection object. null if this is closed.
	 */
	protected Connection connection;
	protected RdbDriver dbDriver;
	protected String schemaName;

	/**
	 * to be used by RdbDriver only.
	 *
	 * @param con
	 * @param traceSql
	 */
	AbstractHandle(Connection con, RdbDriver driver, String schema) {
		this.connection = con;
		this.dbDriver = driver;
		this.schemaName = schema;
	}

	@Override
	public String getSchema() {
		return this.schemaName;
	}

	@Override
	public abstract DbAccessType getHandleType();

	void close() {
		/*
		 * release all objects : just as a safety in case the handle instance is
		 * not released by the consumer
		 */
		this.connection = null;
		this.dbDriver = null;
		this.schemaName = null;
	}

	@Override
	public Struct createStruct(Value[] values, String dbObjectType) {
		Object[] data = new Object[values.length];
		for (int i = 0; i < data.length; i++) {
			data[i] = values[i].toObject();
		}
		try {
			return this.createStruct(data, dbObjectType);
		} catch (SQLException e) {
			throw new ApplicationError(e, "error while creating struct from values");
		}
	}

	@Override
	public Struct createStruct(Object[] data, String dbObjectType) throws SQLException {
		this.checkActive();
		return this.dbDriver.getStructCreator().createStruct(this.connection, data, dbObjectType);
	}

	@Override
	public Array createArray(Value[] values, String dbArrayType) throws SQLException {
		this.checkActive();
		Object[] data = new Object[values.length];
		for (int i = 0; i < values.length; i++) {
			Value val = values[i];
			if (val != null) {
				data[i] = val.toObject();
			}
		}

		return this.connection.createArrayOf(dbArrayType, data);
	}

	@Override
	public Array createStructArray(Struct[] structs, String dbArrayType) throws SQLException {
		this.checkActive();
		return this.dbDriver.getArrayCreator().createArray(this.connection, structs, dbArrayType);
	}

	protected void checkActive() {
		if (this.connection == null) {
			throw new ApplicationError("Db operation attempted after the handle is closed");
		}
	}

	@Override
	public String escapeForLike(String text) {
		return this.dbDriver.escapeForLike(text);
	}

	/**
	 * set parameters to a prepared statement
	 *
	 * @param stmt
	 * @param values
	 * @throws SQLException
	 */
	protected static void setPreparedStatementParams(PreparedStatement stmt, Value[] values) throws SQLException {
		if (values == null) {
			return;
		}
		int i = 1;
		for (Value value : values) {
			if (value == null) {
				throw new ApplicationError(
						"Prepared statements should always get non-null values. use Value.UnknownxxxValue if needed");
			}
			value.setToStatement(stmt, i);
			i++;
		}
	}

	@Override
	public IDbDriver getDriver() {
		return this.dbDriver;
	}

}
