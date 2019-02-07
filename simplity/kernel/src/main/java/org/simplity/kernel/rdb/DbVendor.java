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
package org.simplity.kernel.rdb;

import java.sql.Connection;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.idb.ArrayCreator;
import org.simplity.kernel.idb.StructCreator;

import oracle.jdbc.driver.OracleConnection;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import oracle.sql.STRUCT;
import oracle.sql.StructDescriptor;

/**
 * RDBMS vendors that we can use. This Enum helps is documenting as to how to
 * add a new vendor. as of now we use only two features that are supported by
 * all in a standard way.
 *
 * @author simplity.org
 */
public enum DbVendor {
	/** oracle */
	ORACLE(
			"select sys_context('userenv','current_schema') x from dual",
			"ALTER SESSION SET CURRENT_SCHEMA = ") {
		@SuppressWarnings("resource")
		@Override
		public StructCreator getStructCreator() {
			return (con, data, structName) -> {
				OracleConnection ocon = this.toOracleConnection(con);
				StructDescriptor sd = StructDescriptor.createDescriptor(structName, ocon);
				return new STRUCT(sd, ocon, data);
			};
		}

		@SuppressWarnings("resource")
		@Override
		public ArrayCreator getArrayCreator() {
			return (con, data, structName) -> {
				OracleConnection ocon = this.toOracleConnection(con);
				ArrayDescriptor ad = ArrayDescriptor.createDescriptor(structName, ocon);
				return new ARRAY(ad, ocon, data);
			};
		}

		private OracleConnection toOracleConnection(Connection con) {
			if (con instanceof OracleConnection) {
				return (OracleConnection) con;
			}
			try {
				return con.unwrap(OracleConnection.class);
			} catch (Exception e) {
				throw new ApplicationError(
						"Error while unwrapping to Oracle connection. This is a set-up issue with your server. It is probably using a pooled-connection with a flag not to allow access to underlying connection object "
								+ e.getMessage());
			}
		}
	}

	/** Microsoft Sql Server */
	,
	MSSQL("CURRENT_TIMESTAMP", "select schema_name()", "use ", '%', '_', '[', ']')

	/** postgres sql */
	,POSTGRESQL("select current_schema()", "SET schema ")

	/** my sql */
	, MYSQL

	/** H2 data base */
	,H2("SELECT SCHEMA()", "SET schema ")
	/** db2 */
	,DB2("select current_schema from sysibm.sysdummy1", "set schema ");

	/*
	 * fields default to standard
	 */
	private String timeStampFunctionName = "CURRENT_TIMESTAMP";
	private char[] escapeCharsForLike = { '%', '_' };
	private String getSchema = "SELECT DATABASE()";
	private String setSchema = "USE ";

	/*
	 * standard sql compliant vendor
	 */
	DbVendor() {
	}

	/** with non-standard parameters */
	private DbVendor(
			String timeStampFunctionName,
			String getSchema,
			String setSchema,
			char... escapeCharsForLike) {
		this.timeStampFunctionName = timeStampFunctionName;
		this.getSchema = getSchema;
		this.setSchema = setSchema;
		this.escapeCharsForLike = escapeCharsForLike;
	}

	/** standard except for schema... */
	private DbVendor(String getSchema, String setSchema) {
		this.getSchema = getSchema;
		this.setSchema = setSchema;
	}

	/** standard except for escape chars.. */
	private DbVendor(char... escapeCharsForLike) {
		this.escapeCharsForLike = escapeCharsForLike;
	}

	/** @return sql function name to get current time stamp */
	public String getTimeStamp() {
		return this.timeStampFunctionName;
	}

	/** @return escape characters recognized inside a like parameter. */
	public char[] getEscapesForLike() {
		return this.escapeCharsForLike;
	}

	/**
	 * @return get the sql that returns the default schema for the logged-in
	 *         user
	 */
	public String getGetSchemaSql() {
		return this.getSchema;
	}

	/**
	 * @param schema
	 *            to be set as default
	 * @return ddl sql get the sql that sets the schema
	 */
	public String getSetSchemaSql(String schema) {
		return this.setSchema + schema;
	}

	/**
	 *
	 * @return function to be used to create a struct from data
	 */
	public StructCreator getStructCreator() {
		return (con, data, structName) -> con.createStruct(structName, data);
	}

	/**
	 *
	 * @return function to be used to create an sql-array from data
	 */
	public ArrayCreator getArrayCreator() {
		return (con, data, structName) -> con.createArrayOf(structName, data);
	}
}
