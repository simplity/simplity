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
import org.simplity.core.app.internal.ParameterRetriever;
import org.simplity.core.comp.ComponentType;
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationUtil;
import org.simplity.core.data.IFieldsCollection;
import org.simplity.core.dm.Record;
import org.simplity.core.dt.DataType;
import org.simplity.core.expr.BinaryOperator;
import org.simplity.core.expr.InvalidOperationException;
import org.simplity.core.msg.FormattedMessage;
import org.simplity.core.msg.Messages;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.value.BooleanValue;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;

/**
 * @author simplity.org
 *
 */
public class FieldValidation {
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
	/**
	 * default values is parsed into a Value object for performance
	 */
	private Value defaultValueObject;
	private boolean hasInterfields;
	private Set<Value> validValues;

	/**
	 * @return true if some inter-field validations are defined for this field
	 */
	boolean hasInterFieldValidations() {
		return this.hasInterfields;
	}

	void getReady(ValueType vt) {
		this.hasInterfields = this.basedOnField != null || this.fromField != null || this.toField != null
				|| this.otherField != null;
		if (this.valueList != null) {
			this.validValues = Value.parseValueList(this.valueList, vt);
		}
		if (this.dataType != null) {
			this.dataTypeObject = Application.getActiveInstance().getDataType(this.dataType);
			if (this.messageName == null) {
				this.messageName = this.dataTypeObject.getMessageName();
			}
		}
	}

	Value validateValue(Value inputValue, boolean allFieldsAreOptional, ServiceContext ctx, String externalName) {
		Value value = null;
		if (inputValue == null || inputValue.isUnknown()) {
			value = this.getDefaultValue(ctx);
			if (value == null && allFieldsAreOptional == false && this.isRequired && ctx != null) {
				ctx.addMessage(new FormattedMessage(Messages.VALUE_REQUIRED, externalName));
			}
			return value;
		}

		if (this.validValues != null) {
			if (this.validValues.contains(inputValue)) {
				return inputValue;
			}

			if (ctx != null) {
				ctx.addMessage(new FormattedMessage(this.messageName, externalName, inputValue.toString()));
			}
			return null;
		}
		value = this.dataTypeObject.validateValue(inputValue);
		if (value == null && ctx != null) {
			ctx.addMessage(new FormattedMessage(this.messageName, externalName, inputValue.toString()));
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

	void validateInterField(IFieldsCollection fields, Value value, String recordName, String fieldName,
			ServiceContext ctx) {
		if (!this.hasInterfields) {
			return;
		}
		if (value == null) {
			/*
			 * possible error case 1 : basedOnField forces this field to be
			 * mandatory
			 */
			if (this.basedOnField != null) {
				Value basedValue = fields.getValue(this.basedOnField);
				if (basedValue != null) {
					if (this.basedOnFieldValue == null) {
						ctx.addMessage(new FormattedMessage(Messages.INVALID_BASED_ON_FIELD, recordName, fieldName,
								this.basedOnField, 0));
					} else {
						/*
						 * not only that the basedOnField has to have value, it
						 * should have a specific value
						 */
						Value matchingValue = Value.parseValue(this.basedOnFieldValue, basedValue.getValueType());
						if (basedValue.equals(matchingValue)) {
							ctx.addMessage(new FormattedMessage(Messages.INVALID_BASED_ON_FIELD, recordName, fieldName,
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
					ctx.addMessage(new FormattedMessage(Messages.INVALID_OTHER_FIELD, recordName, fieldName,
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
					throw new ApplicationError("incompatible fields " + fieldName + " and " + this.fromField
							+ " are set as from-to fields");
				}
				if (result.getBoolean()) {
					ctx.addMessage(
							new FormattedMessage(Messages.INVALID_FROM_TO, recordName, this.fromField, fieldName, 0));
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
					throw new ApplicationError("incompatible fields " + fieldName + " and " + this.fromField
							+ " are set as from-to fields");
				}
				if (result.getBoolean()) {
					ctx.addMessage(
							new FormattedMessage(Messages.INVALID_FROM_TO, recordName, fieldName, this.toField, 0));
				}
			}
		}
	}

	/**
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

	void copyFrom(FieldValidation ref) {
		if (this.dataType == null) {
			this.dataType = ref.dataType;
		}
		if (this.valueList == null) {
			this.valueList = ref.valueList;
		}
		if (this.defaultValue == null) {
			this.defaultValue = ref.defaultValue;
		}
		if (this.defaultValueParameter == null) {
			this.defaultValueParameter = ref.defaultValueParameter;
		}
		if (this.messageName == null) {
			this.messageName = ref.messageName;
		}

	}

	/**
	 * @return data type object
	 */
	DataType getDataTypeObject() {
		return this.dataTypeObject;
	}

}
