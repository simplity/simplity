/*
 * Copyright (c) 2015 EXILANT Technologies Private Limited (www.exilant.com)
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
package org.simplity.kernel.sql;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.app.Application;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.FieldMetaData;
import org.simplity.kernel.comp.IValidationContext;
import org.simplity.kernel.comp.ValidationUtil;
import org.simplity.kernel.data.IFieldsCollection;
import org.simplity.kernel.dt.DataType;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;

/**
 * parameter used in a prepared statement or stored procedure
 *
 * @author simplity.org
 */
public class SqlParameter {
	/** name in which field value is found in input */
	@FieldMetaData(isRequired = true)
	String name;
	/** data type */
	@FieldMetaData(isRequired = true, isReferenceToComp = true, referredCompType = ComponentType.DT)
	String dataType;
	/**
	 * Value to be used in case field value is omitted. null implies that the
	 * value is mandatory.
	 */
	String defaultValue;

	/** is this a mandatory field? */
	boolean isRequired;
	/** cached for performance */
	private DataType dataTypeObject;
	/** cached for performance */
	private Value defaultValueObject;

	/**
	 * to be called by parent component after loading attribute but before this
	 * instance is used
	 */
	void getReady() {
		if (this.dataType == null) {
			throw new ApplicationError(
					"Sql parameter "
							+ this.name
							+ " is not associated with any data type. Please specify dataType attribute, or associate this parameter with the right record.");
		}
		this.dataTypeObject = Application.getActiveInstance().getDataType(this.dataType);
		if (this.defaultValue != null) {
			this.defaultValueObject = this.dataTypeObject.parseValue(this.defaultValue);
			if (this.defaultValueObject == null) {
				throw new ApplicationError("sql parameter " + this.name + " has an invalid default value.");
			}
		}
	}

	/**
	 * Field values are always mandatory. If a value is not found in the input,
	 * we look for default value, failing which we throws application error
	 *
	 * @param inValues
	 *            field values
	 * @return value
	 */
	Value getValue(IFieldsCollection inValues) {
		Value value = inValues.getValue(this.name);
		if (value != null) {
			return value;
		}
		if (this.defaultValueObject != null) {
			return this.defaultValueObject;
		}
		if (this.isRequired) {
			throw new ApplicationError("Value for sql field " + this.name + " is required");
		}
		return null;
	}

	/** @return */
	ValueType getValueType() {
		return this.dataTypeObject.getValueType();
	}

	/**
	 * validate the loaded attributes of this sub-component
	 *
	 * @param vtx
	 */
	void validate(IValidationContext vtx) {
		ValidationUtil.validateMeta(vtx, this);
	}
}
