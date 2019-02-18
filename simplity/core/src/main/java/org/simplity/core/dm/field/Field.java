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
import org.simplity.core.app.internal.ParameterRetriever;
import org.simplity.core.comp.ComponentType;
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationUtil;
import org.simplity.core.data.IFieldsCollection;
import org.simplity.core.dm.Record;
import org.simplity.core.dm.RecordUsageType;
import org.simplity.core.dt.DataType;
import org.simplity.core.expr.BinaryOperator;
import org.simplity.core.expr.InvalidOperationException;
import org.simplity.core.msg.FormattedMessage;
import org.simplity.core.msg.Messages;
import org.simplity.core.service.DataStructureType;
import org.simplity.core.service.OutputRecord;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.sql.InputOutputType;
import org.simplity.core.value.BooleanValue;
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
	private static final String DEFAULT_DATA_TYPE = ValueType.TEXT.getDefaultDataType();

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
	 * relevant only if this is used as stored procedure parameter
	 */
	InputOutputType inputOutputType = InputOutputType.BOTH;

	/**
	 * to be used if this is used in UX
	 */
	String label;

	/**
	 * data type as described in dataTypes.xml
	 */
	@FieldMetaData(isReferenceToComp = true, referredCompType = ComponentType.DT)
	String dataType;
	/**
	 * is this field required to have a value, for the purpose that this is
	 * designed for
	 */
	boolean isRequired;

	/**
	 * value to be used if it is not supplied, even if it is optional. use this
	 * if it is known at design time. if this is a deployment/runtime parmeter,
	 * then use the other attribute.
	 */
	String defaultValue = null;

	/**
	 * name of the run-time/deployment-time parameter whoe value is to be used
	 * as default value. This parameter is tried before considering defaultValue
	 */
	String defaultValueParameter = null;
	/**
	 * If this field can take set of design-time determined values, this is the
	 * place. this is of the form
	 * "internalValue1:displayValue1,internaValue2,displayValue2......."
	 */
	String valueList;
	/**
	 * * is this field mandatory but only when value for another field is
	 * supplied?
	 */
	String basedOnField = null;

	/**
	 * relevant if basedOnField is true. this field is required if the other
	 * field has a specific value (not just any value)
	 */
	String basedOnFieldValue = null;

	/**
	 * At times, we have two fields but only one of them should have value. Do
	 * you have such a pair? If so, one of them should set this. Note that it
	 * does not imply that one of them is a must. It only means that both cannot
	 * be specified. Both can be optional is implemented by isOptional for both.
	 */
	String otherField = null;
	/**
	 * * is this a to-field for another field? Specify the name of the from
	 * field. Note that you should not specify this on both from and to fields.
	 */
	String fromField = null;

	/**
	 * * is this part of a from-to field, and you want to specify that thru this
	 * field?
	 */
	String toField = null;

	/**
	 * most application designs have the concept of using code-and-description.
	 * Tagging a field helps in automating its validation, based on actual app
	 * design. For example customer-type field in customer table may be tagged
	 * as common code "custType", and at run time, value of this field is
	 * validated against valid values for "custType"
	 */
	String commonCodeType;
	/**
	 * is this field encrypted
	 */
	boolean isEncrypted;

	/**
	 * * message or help text that is to be flashed to user on the client as a
	 * help text and/or error text when the field value is in error. This
	 * defaults to recordName.fieldName so that a project can have some utility
	 * to maintain all messages for field errors
	 */
	@FieldMetaData(isReferenceToComp = true, referredCompType = ComponentType.MSG)
	String messageName = null;

	private DataType dataTypeObject;
	private ValueType valueType;
	/**
	 * default values is parsed into a Value object for performance
	 */
	private Value defaultValueObject;
	private boolean hasInterfields;
	private Set<Value> validValues;

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
		return this.isRequired;
	}

	/**
	 * @return true if some inter-field validations are defined for this field
	 */
	public boolean hasInterFieldValidations() {
		return this.hasInterfields;
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
	 * @return name with which this fiel dis known column name of this field
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
		return this.isEncrypted;
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

		return this.validateValue(value, allFieldsAreOptional, ctx);
	}

	private Value validateValue(Value inputValue, boolean allFieldsAreOptional, ServiceContext ctx) {
		Value value = null;
		if (inputValue == null || inputValue.isUnknown()) {
			value = this.getDefaultValue(ctx);
			if (value == null && allFieldsAreOptional == false && this.isRequired && ctx != null) {
				ctx.addMessage(new FormattedMessage(Messages.VALUE_REQUIRED, this.externalName));
			}
			return value;
		}

		if (this.validValues != null) {
			if (this.validValues.contains(inputValue)) {
				return inputValue;
			}

			if (ctx != null) {
				ctx.addMessage(new FormattedMessage(this.messageName, this.externalName, inputValue.toString()));
			}
			return null;
		}
		value = this.dataTypeObject.validateValue(inputValue);
		if (value == null && ctx != null) {
			ctx.addMessage(new FormattedMessage(this.messageName, this.externalName, inputValue.toString()));
		}
		return value;
	}

	Value getDefaultValue(ServiceContext ctx) {
		if (this.defaultValueObject != null) {
			return this.defaultValueObject;
		}
		if (this.defaultValueParameter != null) {
			String txt = ParameterRetriever.getValue(this.defaultValueParameter, ctx);
			if (txt != null) {
				return Value.parseValue(txt, this.defaultValueObject.getValueType());
			}
		}
		return null;
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
			return this.getDefaultValue(ctx);
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
			if (this.isRequired && ctx != null) {
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
		if (!this.hasInterfields) {
			return;
		}
		Value value = fields.getValue(this.name);
		if (value == null) {
			/*
			 * possible error case 1 : basedOnField forces this field to be
			 * mandatory
			 */
			if (this.basedOnField != null) {
				Value basedValue = fields.getValue(this.basedOnField);
				if (basedValue != null) {
					if (this.basedOnFieldValue == null) {
						ctx.addMessage(new FormattedMessage(Messages.INVALID_BASED_ON_FIELD, recordName, this.name,
								this.basedOnField, 0));
					} else {
						/*
						 * not only that the basedOnField has to have value, it
						 * should have a specific value
						 */
						Value matchingValue = Value.parseValue(this.basedOnFieldValue, basedValue.getValueType());
						if (basedValue.equals(matchingValue)) {
							ctx.addMessage(new FormattedMessage(Messages.INVALID_BASED_ON_FIELD, recordName, this.name,
									this.basedOnField, 0));
						}
					}
				}
			}
			/*
			 * case 2 : other field is not provided. hence this becomes
			 * mandatory
			 */
			if (this.otherField != null) {
				Value otherValue = fields.getValue(this.basedOnField);
				if (otherValue == null) {
					ctx.addMessage(new FormattedMessage(Messages.INVALID_OTHER_FIELD, recordName, this.name,
							this.basedOnField, 0));
				}
			}
			return;
		}

		/*
		 * problems when this field has value - case 1 - from field
		 */
		BooleanValue result;
		if (this.fromField != null) {
			Value fromValue = fields.getValue(this.fromField);
			if (fromValue != null) {
				try {
					result = (BooleanValue) BinaryOperator.Greater.operate(fromValue, value);
				} catch (InvalidOperationException e) {
					throw new ApplicationError("incompatible fields " + this.name + " and " + this.fromField
							+ " are set as from-to fields");
				}
				if (result.getBoolean()) {
					ctx.addMessage(
							new FormattedMessage(Messages.INVALID_FROM_TO, recordName, this.fromField, this.name, 0));
				}
			}
		}
		/*
		 * case 2 : to field
		 */
		if (this.toField != null) {
			Value toValue = fields.getValue(this.toField);
			if (toValue != null) {
				try {
					result = (BooleanValue) BinaryOperator.Greater.operate(value, toValue);
				} catch (InvalidOperationException e) {
					throw new ApplicationError("incompatible fields " + this.name + " and " + this.fromField
							+ " are set as from-to fields");
				}
				if (result.getBoolean()) {
					ctx.addMessage(
							new FormattedMessage(Messages.INVALID_FROM_TO, recordName, this.name, this.toField, 0));
				}
			}
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
		this.assertCompatibility(parentRecord.getRecordUsageType(), parentRecord.getQualifiedName());
		this.resolverReference(parentRecord, defaultReferredRecord);
		if (this.dataType == null) {
			this.dataType = DEFAULT_DATA_TYPE;
			logger.info("Field {} is assigned defeault data type {}", this.name, DEFAULT_DATA_TYPE);
		}
		this.dataTypeObject = Application.getActiveInstance().getDataType(this.dataType);

		if (this.messageName == null) {
			this.messageName = this.dataTypeObject.getMessageName();
		}
		this.valueType = this.dataTypeObject.getValueType();
		if (this.externalName == null) {
			this.externalName = this.name;
		}
		this.hasInterfields = this.basedOnField != null || this.fromField != null || this.toField != null
				|| this.otherField != null;
		if (this.valueList != null) {
			this.validValues = Value.parseValueList(this.valueList, this.valueType);
		}

	}

	private void assertCompatibility(RecordUsageType ut, String recordName) {
		if (this.isPrimitive() == false) {
			if (ut.canTakeNonPrimitives()) {
				return;
			}
			throw new ApplicationError("Field " + this.name + " is a non-primitive field defined inside record "
					+ recordName + ". non-primitive fields are allowed only in object-strucres");
		}

		if (this.fieldType.isDbField()) {
			if (ut.isDbRelated()) {
				return;
			}
			throw new ApplicationError("Field " + this.name + " is a database-related field defined inside record "
					+ recordName + " that is not a db related record");
		}
		/* it is a non-db field */
		if (ut.isDbRelated() == false) {
			return;
		}
		throw new ApplicationError("Field " + this.name + " is not database-related but it defined in record "
				+ recordName + " that is a db related record");
	}

	/**
	 *
	 * @param vtx
	 * @param record
	 * @param referredFields
	 */
	public void validate(IValidationContext vtx, Record record, Set<String> referredFields) {
		ValidationUtil.validateMeta(vtx, this);
		/*
		 * add referred fields
		 */
		if (this.fromField != null) {
			referredFields.add(this.fromField);
		}
		if (this.toField != null) {
			referredFields.add(this.toField);
		}
		if (this.otherField != null) {
			referredFields.add(this.otherField);
		}
		if (this.basedOnField != null) {
			referredFields.add(this.fromField);
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
			 * no reference
			 */
			return;
		}
		if (refRecord == null) {
			throw new ApplicationError("Field " + this.name + " in record " + parentRecord.getQualifiedName()
					+ " requires to refer to another record. Either use defaultReferredRecord at the record level, or referredREcord at the field level");
		}
		String refName = this.getReferredField();
		Field refField = refRecord.getField(refName);

		if (refField == null) {
			throw new ApplicationError("Field " + this.name + " in record " + parentRecord.getQualifiedName()
					+ " refers to field " + refName + " of record " + refRecord.getQualifiedName()
					+ ". Referred field is not found in the referred record.");
		}
		if (this.externalName == null) {
			this.externalName = refField.externalName;
		}
		if (this.label == null) {
			this.label = refField.label;
		}
		if (this.dataType == null) {
			this.dataType = refField.dataType;
		}
		if (this.valueList == null) {
			this.valueList = refField.valueList;
		}
		if (this.defaultValue == null) {
			this.defaultValue = refField.defaultValue;
		}
		if (this.defaultValueParameter == null) {
			this.defaultValueParameter = refField.defaultValueParameter;
		}
		if (this.messageName == null) {
			this.messageName = refField.messageName;
		}
	}

	/**
	 * @return referred field
	 */
	public String getReferredField() {
		if (this.referredField != null) {
			return this.referredField;
		}
		return this.name;
	}

	/**
	 * @return data type used by this field. If this field has no data type,
	 *         then the default data type associated with the value type is
	 *         returned
	 */
	public DataType getDataType() {
		return this.dataTypeObject;
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
		OutputRecord rec = new OutputRecord(this.name, this.externalName, this.referredRecord, DataStructureType.OBJECT,
				outputAs);
		rec.getReady();
		return rec;
	}
}
