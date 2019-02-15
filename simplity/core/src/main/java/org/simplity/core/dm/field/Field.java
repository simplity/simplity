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

import java.util.Set;

import org.simplity.core.ApplicationError;
import org.simplity.core.app.Application;
import org.simplity.core.app.IResponseWriter;
import org.simplity.core.comp.ComponentType;
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationUtil;
import org.simplity.core.data.IFieldsCollection;
import org.simplity.core.dm.Record;
import org.simplity.core.dm.RecordUsageType;
import org.simplity.core.dt.DataType;
import org.simplity.core.msg.FormattedMessage;
import org.simplity.core.msg.Messages;
import org.simplity.core.service.DataStructureType;
import org.simplity.core.service.OutputRecord;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.sql.InputOutputType;
import org.simplity.core.value.IntegerValue;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author simplity.org
 *
 */
public class Field {
	protected static Logger logger = LoggerFactory.getLogger(Field.class);

	/**
	 * get a default field
	 *
	 * @param fieldName
	 * @param valueType
	 * @return a default for the supplied parameters
	 */
	public static Field getDefaultField(String fieldName, ValueType valueType) {
		Field field = new Field();
		field.externalName = field.name = fieldName;
		return field;
	}

	/**
	 * identifier
	 */
	@FieldMetaData(isRequired = true)
	String name = null;

	/**
	 * Type of column, if this record is associated with a table
	 */
	FieldType fieldType = FieldType.DATA;

	/**
	 * If this is a column in the database, and we use a different naming
	 * convention for db, this is the way to map field names to column names.
	 * Mapped to input/output names in case this is used for input/output.
	 * Defaults to name
	 */
	String externalName;

	/**
	 * description is used as help text or validation text
	 */
	String description;

	/**
	 * if fieldType is PARENT_KEY or FOREIGN_KEY, then the table that this
	 * column points to. If the table is a view, then this is the table from
	 * which this column is picked up from
	 */
	@FieldMetaData(isReferenceToComp = true, referredCompType = ComponentType.REC)
	String referredRecord;

	/**
	 * Valid only of the table is a view. Use this if the referred column name
	 * is different from this name. Defaults to this name.
	 */
	String referredField;

	/**
	 * what type of value is this field for
	 */
	ValueType valueType = ValueType.TEXT;

	/**
	 * relevant only if this is used as stored procedure parameter
	 */
	InputOutputType inputOutputType = InputOutputType.BOTH;
	/**
	 * validations that an input for this field is to be subjected to. USe this
	 * if this field is used as part of input data specification. Good practice
	 * to use this for db fields.
	 */
	FieldValidation validation;

	/**
	 * to be used if this is used in UX
	 */
	String label;

	/**
	 * let us have a public identity
	 *
	 * @return name of this field. Unique within a record, but can be duplicate
	 *         across records
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return value-type of this field
	 */
	public ValueType getValueType() {
		return this.valueType;
	}

	/**
	 * @return field type
	 */
	public FieldType getFieldType() {
		return this.fieldType;
	}

	/**
	 * @return true if a value for this field is required, false if it is
	 *         optional
	 */
	public boolean isRequired() {
		return this.validation != null && this.validation.isRequired;
	}

	/**
	 * @return true if some inter-field validations are defined for this field
	 */
	public boolean hasInterFieldValidations() {
		return this.validation != null && this.validation.hasInterFieldValidations();
	}

	/**
	 * @return true if an input is expected for this field, false otherwise
	 */
	public boolean toBeInput() {
		/*
		 * db record based read operations use this concept. will be over-ridden
		 * by dbField
		 */
		return true;
	}

	/**
	 * @return column name of this field
	 */
	public String getExternalName() {
		return this.externalName;
	}

	/**
	 *
	 * @return true if this field is a primitive type. false if it is a
	 *         record/array
	 */
	public boolean isPrimitive() {
		return true;
	}

	/**
	 * write this field from context to response writer
	 *
	 * @param writer
	 * @param values
	 */
	public void write(IResponseWriter writer, IFieldsCollection values) {
		writer.setField(this.externalName, values.getValue(this.name));
	}

	/**
	 * write this value to the writer
	 *
	 * @param writer
	 * @param value
	 */
	public void write(IResponseWriter writer, Value value) {
		writer.setField(this.externalName, value);
	}

	/**
	 * is this field encrypted externally (in the db, or by the client)
	 *
	 * @return true if this field is encrypted
	 */
	public boolean isEncrypted() {
		return this.validation != null && this.validation.isEncrypted;
	}

	/**
	 * parse an input object into a valid value for this field.
	 *
	 * @param inputValue
	 *            could be a primitive value or string, could be null if the
	 *            input source had no value for this field
	 * @param allFieldsAreOptional
	 *            true if the input scenario is such that all fields are
	 *            optional. Any parameter for the field in this regard is to be
	 *            ignored
	 * @param ctx
	 * @return parse value, or null if the value could not be parsed. Error
	 *         message if any, is added to the context
	 */
	public Value parseObject(Object inputValue, boolean allFieldsAreOptional, ServiceContext ctx) {
		if (this.toBeInput() == false) {
			return null;
		}
		Value value = null;
		/*
		 * is this of the right value type?
		 */
		if (inputValue != null) {
			value = this.valueType.parseObject(inputValue);
			if (value == null) {
				ctx.addMessage(new FormattedMessage(Messages.INVALID_DATA, this.externalName, inputValue.toString()));
				return null;
			}
		}

		if (this.validation != null) {
			return this.validation.validateValue(value, allFieldsAreOptional, ctx, this.externalName);
		}
		return value;
	}

	/**
	 * @param values
	 *            any fields collection. can be service context itself
	 * @param ctx
	 *            service context, or null if this is not inside a service
	 *            context. This is used to get the default from a possible
	 *            run-time parameter
	 * @return get value for the field from the collection. In case it is not
	 *         found, get default value
	 */
	public Value getValue(IFieldsCollection values, ServiceContext ctx) {
		Value value = values.getValue(this.name);

		if (Value.isNull(value)) {
			if (this.validation != null) {
				return this.validation.getDefaultValue(ctx);
			}
			return null;
		}

		/**
		 * we treat integer as milliseconds if the value is to be a date
		 */
		if (this.valueType == ValueType.DATE) {
			if (value.getValueType().equals(ValueType.INTEGER)) {
				value = Value.newDateValue((((IntegerValue) value).getLong()));
			}
		}
		return value;
	}

	/**
	 * parse n array of objects as an array of input for this field.
	 *
	 * @param values
	 * @param recordName
	 * @param ctx
	 * @return parsed and validated value. Null if there is no value. An element
	 *         would be null if there is no input or the input value is not
	 *         valid validation errors if any are added to the ctx
	 */
	public Value[] parseArray(Object[] values, String recordName, ServiceContext ctx) {
		if (this.toBeInput() == false) {
			return null;
		}
		if (values == null || values.length == 0) {
			if (this.validation != null && this.validation.isRequired && ctx != null) {
				ctx.addMessage(new FormattedMessage(Messages.VALUE_REQUIRED, recordName, this.externalName, null, 0));
			}
			return null;
		}
		Value[] result = new Value[values.length];
		for (int i = 0; i < values.length; i++) {
			Object val = values[i];
			if (val == null) {
				continue;
			}
			Value value = this.parseObject(val, false, ctx);
			if (value != null) {
				result[i] = value;
			}
		}
		return result;
	}

	/**
	 * carry out inter-field validations for this field
	 *
	 * @param fields
	 * @param recordName
	 * @param ctx
	 */
	public void validateInterfield(IFieldsCollection fields, String recordName, ServiceContext ctx) {
		if (this.validation != null) {
			this.validation.validateInterField(fields, fields.getValue(this.name), recordName, this.externalName, ctx);
		}
	}

	/**
	 * record is ready and this field is to get ready now
	 *
	 * @param parentRecord
	 *            non-null
	 * @param defaultReferredRecord
	 *            this is the record set as default referred record at the
	 *            parent record level. null if no such record is set
	 */
	public void getReady(Record parentRecord, Record defaultReferredRecord) {
		RecordUsageType ut = parentRecord.getRecordUsageType();
		if (ut == RecordUsageType.DATA_STRUCTURE) {
			if (this.fieldType != FieldType.DATA && this.isPrimitive()) {
				throw new ApplicationError("Record " + parentRecord.getQualifiedName()
						+ " is a data strucrure, and hence it cannot conatain fields meant for database or non primitives. But it has a field named "
						+ this.name + " that is a special field of type " + this.getFieldType());
			}
		} else if (ut == RecordUsageType.TABLE || ut == RecordUsageType.VIEW) {
			if (!this.isPrimitive()) {
				throw new ApplicationError("Record " + parentRecord.getQualifiedName()
						+ " is for data base, and hence it should contain only primitive fields. But it has a field named "
						+ this.name + " that is a special field of type " + this.getFieldType());
			}
		} else {
			if (this.fieldType != FieldType.DATA && this.isPrimitive() == false) {
				throw new ApplicationError("Record " + parentRecord.getQualifiedName()
						+ " is a complex data strucrure, and hence it cannot conatain fields meant for database. But it has a field named "
						+ this.name + " that is a special field of type " + this.getFieldType());
			}
		}
		this.resolverReference(parentRecord, defaultReferredRecord);
		if (this.externalName == null) {
			this.externalName = this.name;
		}
		if (this.validation != null) {
			this.validation.getReady(this.valueType);
		}
	}

	/**
	 *
	 * @param vtx
	 * @param record
	 * @param referredFields
	 */
	public void validate(IValidationContext vtx, Record record, Set<String> referredFields) {
		ValidationUtil.validateMeta(vtx, this);
		if (this.validation != null) {
			this.validation.validate(vtx, record, referredFields);
		}
	}

	/*
	 * childRecord and RecordArray fields over-ride this
	 */
	protected void resolverReference(Record parentRecord, Record defaultRefferedRecord) {
		Record refRecord = null;
		if (this.referredRecord != null) {
			refRecord = parentRecord.getRefRecord(this.referredRecord);
		} else if (this.referredField != null || parentRecord.getRecordUsageType() == RecordUsageType.VIEW) {
			refRecord = defaultRefferedRecord;
		} else {
			/*
			 * no refField, no refRecord and this is not a view.. Get the hell
			 * out of here
			 */
			return;
		}
		String refName = this.referredField == null ? this.name : this.referredField;
		Field refField = refRecord.getField(refName);

		if (refField == null) {
			throw new ApplicationError("Field " + this.name + " in record " + parentRecord.getQualifiedName()
					+ " refers to field " + refName + " of record " + refRecord.getQualifiedName()
					+ ". Referred field is not found in the referred record.");
		}
		this.copyFromRefField(refField);
	}

	/**
	 * @param refField
	 */
	private void copyFromRefField(Field refField) {
		if (this.valueType == null) {
			this.valueType = refField.valueType;
		}
		if (this.label == null) {
			this.label = refField.label;
		}
		if (this.validation == null) {
			if (refField.validation != null) {
				this.validation = refField.validation;
			}
		} else {
			if (refField.validation != null) {
				this.validation.copyFrom(refField.validation);
			}
		}
	}

	/**
	 * @return referred field
	 */
	public String getReferredField() {
		return this.referredField;
	}

	/**
	 * @return data type used by this field. If this field has no data type,
	 *         then the default data type associated with the value type is
	 *         returned
	 */
	public DataType getDataType() {
		DataType dt = null;
		if (this.validation != null) {
			dt = this.validation.getDataTypeObject();
		}
		if (dt == null) {
			dt = Application.getActiveInstance().getDataType(this.valueType.getDefaultDataType());
		}
		return dt;
	}

	/**
	 * @return record that this field refers to
	 */
	public String getReferredRecord() {
		return this.referredRecord;
	}

	/**
	 * create an output record for this non-primitive field, based on its
	 * underlying record
	 *
	 * @param outputAs
	 * @return an outputRecord for the specified purpose
	 */
	public OutputRecord getOutputRecord(DataStructureType outputAs) {
		if (this.isPrimitive()) {
			return null;
		}
		OutputRecord rec = new OutputRecord(this.name, this.externalName, this.referredRecord, true, outputAs);
		rec.getReady();
		return rec;
	}
}
