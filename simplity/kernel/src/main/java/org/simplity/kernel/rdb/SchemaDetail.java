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

/**
 * data structure that has the details to use a schema that is different from
 * the default for the project
 * 
 * @author simplity.org
 */
public class SchemaDetail {
	String schemaName;
	String dataSourceName;
	String connectionString;

	/**
	 *
	 * @return non-null schema name
	 */
	public String getSchemaName() {
		return this.schemaName;
	}

	/**
	 *
	 * @return data source name. null if data source is not used, in which case
	 *         getConnectionString() would return non-null.
	 */
	public String getDataSourceName() {
		return this.dataSourceName;
	}

	/**
	 *
	 * @return connection string. null if connection string is not used but data
	 *         source is used
	 */
	public String getConnectionString() {
		return this.connectionString;
	}
}
