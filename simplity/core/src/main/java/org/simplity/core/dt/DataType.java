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
package org.simplity.core.dt;

import java.util.HashSet;
import java.util.Set;

import org.simplity.core.comp.ComponentType;
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IComponent;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationMessage;
import org.simplity.core.comp.ValidationUtil;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;

/**
 * DataType describes the constraints on the range of values that a
 * data-element/field can have
 *
 * @author simplity.org
 */
public abstract class DataType implements IComponent {
	/** max digits that java supports */
	static final int MAX_DIGITS = 17;

	/**
	 * data type is identified by its name that is unique across all modules in
	 * an application.
	 */
	@FieldMetaData(isRequired = true)
	String name = null;

	/**
	 * message to be used when a field that is associated with this data type
	 * fails validation.
	 */
	@FieldMetaData(isReferenceToComp = true, referredCompType = ComponentType.MSG)
	String messageName = null;

	/**
	 * list of valid values. e.g "1:first,2:second,3:third" for an integral
	 * type. This is like creating enumerations
	 */
	String valueList;
	/** sql data type to be used to create this as a column for a table */
	String sqlType = null;

	/** function name used by client-side to format this value */
	String formatter = null;

	/** description */
	@FieldMetaData(isRequired = true)
	String description;

	/** parse valueLinst into this map for ready validation */
	private Set<Value> validValues;

	@Override
	public String getSimpleName() {
		return this.name;
	}

	@Override
	public String getQualifiedName() {
		return this.name;
	}

	@Override
	public void getReady() {
		if (this.valueList != null) {
			this.validValues = new HashSet<>();
			for (String txt : this.valueList.split(",")) {
				Value value = this.getValueType().parse(txt.split(":")[0]);
				if (!Value.isNull(value)) {
					this.validValues.add(value);
				}
			}
		}

		if (this.description == null) {
			this.description = this.synthesiseDscription();
		}
	}

	/** @return name */
	public final String getName() {
		return this.name;
	}

	/**
	 * * returns the string that represents the data type to be used in create
	 * SQL for a column of this type
	 *
	 * @return sql type of this data type
	 */
	public String getSqlType() {
		return this.sqlType;
	}

	/**
	 * * parse supplied text and return parsed value. we decided against
	 * throwing an exception on error. we return null silently instead.
	 *
	 * <p>
	 * We recommend that you handle null in your code, and send only non-null
	 * text for parsing. That way the code is more readable/maintainable.
	 * However, for completeness,if you pass null as textValue, we return a
	 * valid value object with null as its value.
	 *
	 * @param textValue
	 *            text to be parsed. null implies that the caller wants a Value
	 *            object with null-value in it.
	 * @return parsed value, null in case of any error.
	 */
	public final Value parseValue(String textValue) {
		if (textValue == null) {
			return Value.newUnknownValue(this.getValueType());
		}

		String text = textValue.trim();

		if (text.isEmpty() && this.getValueType() != ValueType.TEXT) {
			return Value.newUnknownValue(this.getValueType());
		}

		/*
		 * parse and validate
		 */
		Value value = this.getValueType().parse(text);
		if (value == null) {
			return null;
		}
		if (this.validValues != null) {
			if (this.validValues.contains(value)) {
				return value;
			}
			return null;
		}
		return this.validateValue(value);
	}

	/**
	 * Convenient method to get the actual type of the subclass
	 *
	 * @return value type
	 */
	public abstract ValueType getValueType();

	/**
	 * validate value as per this data type definition, and return valid value.
	 * It is important to note that the returned value could be different from
	 * supplied value because of data-type specific approximations, like
	 * rounding-off and knocking-off time.
	 *
	 * @param value
	 *            to be validated
	 * @return null if value is invalid. Value (possibly modified as per
	 *         data-type rules) if it is valid.
	 */
	public abstract Value validateValue(Value value);

	/**
	 * maximum length of this value.
	 *
	 * @return maximum number of characters
	 */
	public abstract int getMaxLength();

	/**
	 * @return value list
	 */
	public String getValueList() {
		return this.valueList;
	}

	/**
	 * @return the formatter
	 */
	public String getFormatter() {
		return this.formatter;
	}

	/**
	 * @return the validValues map that is parsed from this.valueList
	 */
	public Set<Value> getValidValues() {
		return this.validValues;
	}

	/**
	 * @return 0 for non-numeric data types, and number of positions after
	 *         decimal point for number
	 */
	public int getScale() {
		return 0;
	}

	@Override
	public ComponentType getComponentType() {
		return ComponentType.DT;
	}

	@Override
	public void validate(IValidationContext vtx) {
		ValidationUtil.validateMeta(vtx, this);
		if (this.valueList != null) {
			for (String txt : this.valueList.split(",")) {
				Value value = this.getValueType().parse(txt.split(":")[0]);
				if (Value.isNull(value)) {
					vtx.message(
							new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
									this.valueList + " is an invalid list for value type " + this.getValueType(),
									"valueList"));
					break;
				}
			}
		}
		this.validateSpecific(vtx);
	}

	/**
	 * @param vtx
	 */
	protected void validateSpecific(IValidationContext vtx) {
		// if there is any instance specific validation
	}

	/** @return the description */
	public String getDescription() {
		if (this.description != null) {
			return this.description;
		}
		return this.synthesiseDscription();
	}

	protected abstract String synthesiseDscription();

	/** @return message name */
	public String getMessageName() {
		return this.messageName;
	}

	/**
	 * format value to be output. For example a date value needs to be formatted
	 *
	 * @param value
	 * @return text value. empty string if the value is indeed an empty text
	 *         value, or if it is incompatible with the data type
	 */
	public String formatValue(Value value) {
		if (Value.isNull(value)) {
			return "";
		}
		return value.toString();
	}
}
