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

import org.simplity.core.rdb.DbVendor;

/**
 * <p>
 * this specification is based on RDBMS as of now. We will enhance this as and
 * when we have to cater to other features
 * </p>
 *
 * <p>
 * features that a db has to support are not directly specified here. This
 * interface simply provides a signature to access the db. operations to be
 * supported are delegated to db-handles
 * </p>
 *
 * @author simplity.org
 *
 */
public interface IDbDriver {

	/**
	 * access the database
	 *
	 * @param dbClient
	 *            non-null object that is called back with the right db-handle
	 *            for db access
	 * @param accessType
	 *            non-null type of db access required by the client
	 * @param schemaName
	 *            null if default schema is to be used. If non-null, should be
	 *            one of the schemas that are configured as part of application
	 *            set-up
	 */
	public void accessDb(IDbClient dbClient, DbAccessType accessType, String schemaName);

	/**
	 * if LIKE is used in a sql, the operand (text) needs to be escaped for any
	 * occurrence of '%' in it. This is not common acrss all vendors, and hence
	 * this function.
	 *
	 * @param text
	 *            to be escaped
	 * @return go ahead and send this as value of prepared statement for LIKE
	 */
	public String escapeForLike(String text);

	/**
	 * @param schema
	 *            name of schema to check for
	 * @return true is this schema is defined as additional schema. False
	 *         otherwise
	 */
	public boolean isSchemaDefined(String schema);

	/**
	 * @return default schema used by this driver. null if the db does not use
	 *         schema. some vendors use "PUBLIC" as default schema, if no schema
	 *         is explicitly created
	 */
	public String getDefaultSchema();

	/**
	 * @return dbVendor of this driver
	 */
	public DbVendor getDbVendor();

	/**
	 * @return function, may be specific to the dbVendor, that is to be used to
	 *         get time-stamp value
	 */
	public String getTimeStampFn();

	/**
	 * struct parameters in sp are built based on data objects. Oracle does not
	 * conform to the standards, and hence this abstraction
	 *
	 * @return non-null lambda function that creates a struct parameter
	 */
	public StructCreator getStructCreator();

	/**
	 * array parameters in sp are built based on data objects. Oracle does not
	 * conform to the standards, and hence this abstraction
	 *
	 * @return non-null lambda function that creates a array of struct for a
	 *         stored procedure parameter
	 */
	public ArrayCreator getArrayCreator();

	/**
	 * if the db design uses auto-generated sequences as primary key in any of
	 * the tables, then the driver may require the name of the sequence
	 * generator
	 *
	 * @return true if the drivers needs a sequence generated to manage
	 *         auto-generated primary keys. false otherwise
	 */
	public boolean sequenceGeneratorRequired();
}
