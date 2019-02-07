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

package org.simplity.kernel.dm.field;

import org.simplity.kernel.value.ValueType;

/**
 * @author simplity.org
 *
 */
public class DbField extends Field {
	/**
	 * can this field be null in the db
	 */
	boolean isNullable;

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
		this.fieldType = FieldType.DATA;
	}

	/**
	 * used by utility that generates fields from the database
	 *
	 * @param name
	 * @param externalName
	 * @param description
	 * @param vt
	 * @param isNullable
	 */
	public DbField(String name, String externalName, String description, ValueType vt, boolean isNullable) {
		this.name = name;
		this.externalName = externalName;
		this.valueType = vt;
		this.description = description;
		this.isNullable = isNullable;
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

}
