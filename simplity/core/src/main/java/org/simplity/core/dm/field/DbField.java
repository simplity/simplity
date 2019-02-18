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

/**
 * @author simplity.org
 *
 */
public class DbField extends Field {
	/**
	 * can this field be null in the db
	 */
	boolean isNullable;

	/**
	 * name in which this field is saved in the table. Defaults to name
	 */
	String columnName;

	/*
	 * these attributes are set by concrete classes on init
	 */
	protected boolean updatable;
	protected boolean insertable;
	protected boolean toBeInput;

	/**
	 *
	 */
	public DbField() {
		this.fieldType = FieldType.DB_COLUMN;
		this.updatable = true;
		this.toBeInput = true;
		this.insertable = true;
	}

	/**
	 * used by utility that generates fields from the database
	 *
	 * @param name
	 * @param columnName
	 * @param description
	 * @param dataType
	 * @param isNullable
	 */
	public DbField(String name, String columnName, String description, String dataType, boolean isNullable) {
		this();
		this.name = name;
		this.columnName = columnName;
		this.description = description;
		this.isNullable = isNullable;
		this.dataType = dataType;
	}

	/**
	 * @return false if this is one of the standard fields that are not to be
	 *         touched. retained once inserted
	 */
	public boolean canUpdate() {
		return this.updatable;
	}

	/**
	 * @return false if this is one of the standard fields that are not to be
	 *         touched retained once inserted
	 */
	public boolean canInsert() {
		return this.insertable;
	}

	/**
	 * @return false if this is one of the standard fields that are not to be
	 *         touched retained once inserted
	 */
	@Override
	public boolean toBeInput() {
		return this.toBeInput;
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
}
