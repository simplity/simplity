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
package org.simplity.kernel.service;

import org.simplity.json.JSONArray;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.app.Application;
import org.simplity.kernel.app.IRequestReader;
import org.simplity.kernel.app.IRequestReader.InputValueType;
import org.simplity.kernel.app.internal.CommonCodeValidator;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.FieldMetaData;
import org.simplity.kernel.comp.IValidationContext;
import org.simplity.kernel.comp.ValidationUtil;
import org.simplity.kernel.data.IDataSheet;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.dt.DataType;
import org.simplity.kernel.msg.Messages;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * data structure that holds name and data type of an input/output field.
 */
public class InputField {
	private static final Logger logger = LoggerFactory.getLogger(InputField.class);
	/**
	 * name
	 */
	@FieldMetaData(isRequired = true)
	String name;

	/**
	 * name of the input field, if different from name
	 */
	String inputName;

	/**
	 * data type. Used for validating in case this is used as input. Used for
	 * valueType otherwise
	 */
	@FieldMetaData(isReferenceToComp = true, referredCompType = ComponentType.DT)
	String dataType;

	/**
	 * most application designs have the concept of using code-and-description.
	 * Tagging a field helps in automating its validation, based on actual app
	 * design. For example customer-type field in customer table may be tagged
	 * as common code "custType", and at run time, value of this field is
	 * validated against valid values for "custType"
	 */
	String commonCodeType;

	/**
	 * used for validation during input
	 */
	boolean isRequired;
	/**
	 * used in case value is not available at run time, and isRequired is true
	 */
	String defaultValue;

	/**
	 * is it an array
	 */
	boolean isArray;
	/*
	 * cached for performance
	 */
	private DataType dataTypeObject;
	/*
	 * cached for performance
	 */
	private Value defaultObject;

	/**
	 * default constructor
	 */
	public InputField() {
		//
	}

	/**
	 * convenient constructor to create a simple field
	 *
	 * @param name
	 * @param dataType
	 * @param isRequired
	 * @param defaultValue
	 * @param inputName
	 * @param commonCodeType
	 */
	public InputField(String name, String dataType, boolean isRequired,
			String defaultValue, String inputName, String commonCodeType) {
		this.name = name;
		this.dataType = dataType;
		this.isRequired = isRequired;
		this.defaultValue = defaultValue;
		this.inputName = inputName;
		this.commonCodeType = commonCodeType;
	}

	/**
	 *
	 * @return data type
	 */
	public DataType getDataType() {
		return this.dataTypeObject;
	}

	/**
	 * open shop..
	 */
	public void getReady() {
		if (this.inputName == null) {
			this.inputName = this.name;
		}

		this.dataTypeObject = Application.getActiveInstance().getDataType(this.dataType);
		/*
		 * above statement would have thrown an exception if data type is not
		 * valid
		 */
		if (this.defaultValue != null) {
			this.defaultObject = this.dataTypeObject
					.parseValue(this.defaultValue);

			if (this.defaultObject == null) {
				throw new ApplicationError("Input/Output field " + this.name
						+ " has an invalid default value of "
						+ this.defaultValue);
			}
		}
	}

	/**
	 * if field value is not valid, a validation error is added to context
	 *
	 * @param objectValue
	 *            input
	 * @param ctx
	 *            to which we extract this field
	 * @return true if data is extracted, false otherwise.
	 */
	public boolean extractInput(Object objectValue, ServiceContext ctx) {
		if (objectValue == null) {
			/*
			 * is this already in the context?. possible if this is called from
			 * another java code rather than from a client agent
			 */
			Value value = ctx.getValue(this.name);
			if (value != null) {
				Value newValue = this.dataTypeObject.validateValue(value);
				if (newValue == null) {
					this.validationError(value.toText(), 0, ctx);
					return false;
				}
				ctx.setValue(this.name, newValue);
				return true;
			}
			value = this.defaultObject;
			if (value == null && this.isRequired) {
				logger.info(this.inputName + " failed mandatory criterion");
				ctx.addMessage(Messages.VALUE_REQUIRED, this.inputName);
				return false;
			}
			/*
			 * default value is validated at load time. No need to validate it
			 * again
			 */
			if (this.isArray) {
				Value[] vals = { this.defaultObject };
				ctx.putDataSheet(this.name, this.createDataSheet(vals));
			} else {
				ctx.setValue(this.name, value);
			}
			return true;
		}
		/*
		 * normal field
		 */
		if (this.isArray == false) {
			Value value = this.parseValue(objectValue, ctx, 0);
			if (value == null) {
				return false;
			}
			ctx.setValue(this.name, value);
			return true;
		}
		/*
		 * array
		 */
		Value[] values = null;
		if (objectValue instanceof JSONArray) {
			values = this.jsonToValues((JSONArray) objectValue, ctx);
		} else if (objectValue.getClass().isArray()) {
			values = this.arrayToValues((Object[]) objectValue, ctx);
		} else {
			values = Value.parse(objectValue.toString().split(","),
					this.dataTypeObject.getValueType());
		}
		if (values != null) {
			ctx.putDataSheet(this.name, this.createDataSheet(values));
			return true;
		}

		return false;
	}

	private Value parseValue(Object objectValue, ServiceContext ctx, int idx) {
		Value value = this.dataTypeObject.getValueType()
				.fromObject(objectValue);
		if (value != null) {
			value = this.dataTypeObject.validateValue(value);
		}
		if (this.commonCodeType != null && ctx != null) {
			boolean allOk = CommonCodeValidator.isValid(this.commonCodeType, value, null, ctx);
			if (!allOk) {
				value = null;
			}
		}
		if (value == null) {
			this.validationError(objectValue.toString(), idx, ctx);
		}
		return value;
	}

	/**
	 * create a data sheet with one column named same as this field name
	 *
	 * @return multi-rows data sheet to which one can keep adding rows
	 */
	private IDataSheet createDataSheet(Value[] values) {
		String[] fields = { this.name };
		ValueType[] types = { this.dataTypeObject.getValueType() };
		IDataSheet sheet = new MultiRowsSheet(fields, types);
		if (values != null) {
			for (Value value : values) {
				if (value != null) {
					Value[] row = { value };
					sheet.addRow(row);
				}
			}
		}
		return sheet;
	}

	/**
	 * extract values from an array into a data sheet
	 *
	 * @param arr
	 * @param sheet
	 * @param ctx
	 * @return
	 */
	private Value[] arrayToValues(Object[] arr, ServiceContext ctx) {
		ValueType vt = this.dataTypeObject.getValueType();
		int idx = 0;
		Value[] values = new Value[arr.length];
		for (Object obj : arr) {
			Value value = null;
			if (obj != null) {
				value = vt.fromObject(obj);
			}
			if (value == null) {
				this.validationError("" + obj, idx, ctx);
				return null;
			}
			idx++;
		}
		return values;
	}

	/**
	 * extract values from a json array into a sheet
	 *
	 * @param arr
	 * @param sheet
	 * @param ctx
	 * @return
	 */
	private Value[] jsonToValues(JSONArray arr, ServiceContext ctx) {
		ValueType vt = this.dataTypeObject.getValueType();
		int nbr = arr.length();
		Value[] values = new Value[nbr];
		for (int i = 0; i < nbr; i++) {
			Object obj = arr.opt(i);
			Value value = null;
			if (obj != null) {
				value = vt.fromObject(obj);
			}
			if (value == null) {
				this.validationError("" + obj, i, ctx);
				return null;
			}
			values[i] = value;
		}
		return values;
	}

	/**
	 * add validation error for this field to the context
	 *
	 * @param value
	 * @param idx
	 * @param ctx
	 * @return false for convenience
	 */
	private boolean validationError(String value, int idx, ServiceContext ctx) {
		logger.info(this.name + " failed validation against data type "
				+ this.dataType);
		String msg = this.dataTypeObject.getMessageName();
		if (msg != null) {
			ctx.addValidationMessage(msg, this.inputName, null, null, idx, value);
			return false;
		}

		msg = this.dataTypeObject.getDescription();
		ctx.addValidationMessage(Messages.INVALID_DATA, this.inputName, null, null,
				idx, value, msg);

		return false;
	}

	/**
	 * @param vtx
	 */
	void validate(IValidationContext vtx) {
		ValidationUtil.validateMeta(vtx, this);
	}

	/**
	 * if field value is not valid, a validation error is added to context
	 *
	 * @param reader
	 *            source of input
	 * @param ctx
	 *            to which we extract this field
	 * @return 0 if field is not extracted, 1 if field is extracted.
	 */
	public int read(IRequestReader reader, ServiceContext ctx) {
		InputValueType objectType = reader.getValueType(this.inputName);
		if (objectType == InputValueType.NULL) {
			return this.missingInput(ctx);
		}
		if (this.isArray) {
			if (objectType == InputValueType.ARRAY) {
				Value[] values = this.readArray(reader, ctx);
				if (values == null) {
					return 0;
				}
				ctx.putDataSheet(this.name, this.createDataSheet(values));
				return 1;
			}
			logger.info("Non-array input received for  " + this.inputName + ". Input ignored.");
			return 0;
		}

		Value value = this.parseValue(reader.getValue(this.inputName), ctx, 0);
		if (value == null) {
			return 0;
		}

		ctx.setValue(this.name, value);
		return 1;

	}

	private Value[] readArray(IRequestReader reader, ServiceContext ctx) {
		reader.openArray(this.inputName);
		ValueType vt = this.dataTypeObject.getValueType();
		int nbr = reader.getNbrElements();
		Value[] values = new Value[nbr];
		for (int i = 0; i < nbr; i++) {
			Object obj = reader.getValue(i);
			Value value = null;
			if (obj != null) {
				value = vt.fromObject(obj);
			}
			if (value == null) {
				this.validationError("" + obj, i, ctx);
				values = null;
				break;
			}
		}
		reader.closeArray();
		return values;

	}

	/**
	 * what do we do if input value is not received for this field
	 *
	 * @param ctx
	 * @return
	 */
	private int missingInput(ServiceContext ctx) {
		/*
		 * is this already in the context?. possible if this is called from
		 * another java code rather than from a client agent
		 */
		Value value = ctx.getValue(this.name);
		if (value != null) {
			Value newValue = this.dataTypeObject.validateValue(value);
			if (newValue == null) {
				this.validationError(value.toText(), 0, ctx);
				return 0;
			}
			ctx.setValue(this.name, newValue);
			return 1;
		}
		if (this.isRequired) {
			logger.info(this.inputName + " failed mandatory criterion");
			ctx.addMessage(Messages.VALUE_REQUIRED, this.inputName);
			return 0;
		}

		if (this.defaultObject == null) {
			return 0;
		}

		if (this.isArray) {
			Value[] vals = { this.defaultObject };
			ctx.putDataSheet(this.name, this.createDataSheet(vals));
		} else {
			ctx.setValue(this.name, this.defaultObject);
		}

		return 1;
	}

	/**
	 *
	 * @return name of this field
	 */
	public String getName() {
		return this.name;
	}

	/**
	 *
	 * @return name of this field
	 */
	public String getInputName() {
		return this.inputName;
	}

	/**
	 *
	 * @param name
	 *            name of this field
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 *
	 * @param name
	 *            name with which input is received for this field, if it is
	 *            different from name
	 */
	public void setInputName(String name) {
		this.inputName = name;
	}

	/**
	 *
	 * @return is a value required for this field from input?
	 */
	public boolean isRequired() {
		return this.isRequired;
	}

	/**
	 *
	 * @param isRequired
	 *            true if a value is to be set by caller for this field, false
	 *            otherwise
	 */
	public void setRequired(boolean isRequired) {
		this.isRequired = isRequired;
	}

	/**
	 * set data type for this field
	 *
	 * @param dataType
	 */
	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

}
