/*
 * Copyright (c) 2019 simplity.org
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

package org.simplity.core.dm.field;

import org.simplity.core.dt.DataType;
import org.simplity.core.value.ValueType;

/**
 * @author simplity.org
 *
 */
public class DbField extends Field {
	/**
	 * get a default field
	 *
	 * @param fieldName
	 * @param valueType
	 * @return a default for the supplied parameters
	 */
	public static DbField getDefaultField(String fieldName, ValueType valueType) {
		DbField field = new DbField();
		field.columnName = field.name = fieldName;
		DataType dt = valueType.getDefaultDataType();
		field.dataTypeObject = dt;
		field.dataType = dt.getQualifiedName();
		field.isNullable = true;
		return field;
	}

	/**
	 * can this field be null in the db
	 */
	boolean isNullable;

	/**
	 * name in which this field is saved in the table. Defaults to name
	 */
	String columnName;

	/**
	 * used by utility that generates fields from the database
	 *
	 * @param name
	 * @param columnName
	 * @param description
	 * @param dt
	 * @param isNullable
	 * @return an DbField instance
	 */
	public static DbField createDbField(String name, String columnName, String description, DataType dt,
			boolean isNullable) {
		DbField f = new DbField();
		f.name = name;
		f.columnName = columnName;
		f.description = description;
		f.isNullable = isNullable;
		f.dataTypeObject = dt;
		f.dataType = dt.getQualifiedName();
		return f;
	}

	/**
	 * used by utility that generates fields from the database
	 *
	 * @param name
	 * @param dt
	 * @return a nullable db field
	 */
	public static DbField createDbField(String name, DataType dt) {
		DbField f = new DbField();
		f.name = name;
		f.isNullable = true;
		f.dataTypeObject = dt;
		f.dataType = dt.getQualifiedName();
		return f;
	}

	/**
	 * @return is this field nullable
	 */
	public boolean isNullable() {
		return this.isNullable;
	}

	/**
	 *
	 * @return name of the column in the db.
	 */
	public String getColumnName() {
		if (this.columnName != null) {
			return this.columnName;
		}
		return this.name;
	}

	@Override
	public boolean isDbField() {
		return true;
	}

	/**
	 * @return true if the type is primary or primary as well as parent
	 */
	public boolean isPrimaryKey() {
		return false;
	}

	/**
	 * @return true if the type is parent key, or primary as well as parent
	 */
	public boolean isParentKey() {
		return false;
	}

	/**
	 * @return false if this is one of the standard fields that are not to be
	 *         touched. retained once inserted
	 */
	public boolean canUpdate() {
		return true;
	}

	/**
	 * @return false for time stamp fields. Managed b the geerted sql
	 */
	public boolean canInsert() {
		return true;
	}

	/**
	 * @return false if this is one of the standard fields that are not to be
	 *         touched retained once inserted
	 */
	public boolean toBeInput() {
		return true;
	}

}
