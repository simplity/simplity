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
package org.simplity.fm.data;

import org.simplity.fm.data.types.DataType;
import org.simplity.fm.data.types.InvalidValueException;
import org.simplity.fm.data.types.ValueType;

/**
 * @author simplity.org
 *
 */
public class Field {
	/**
	 * field name is unique within a form/template. However, it is strongly
	 * advised that the same name is used in different forms if they actually
	 * refer to the same data element
	 */
	private final String fieldName;
	/**
	 * data type describes the type of value and restrictions (validations) on
	 * the value
	 */
	private final DataType dataType;
	/**
	 * required/mandatory. If set to true, text value of empty string and 0 for
	 * integral are assumed to be not valid
	 */
	private final boolean isRequired;
	/**
	 * refers to the message id/code that is used for i18n of messages
	 */
	private final String messageId;
	/**
	 * default value is used only if this optional and the value is missing. not
	 * used if the field is mandatory
	 */
	private final String defaultValue;
	/**
	 * if true, this field value is calculated based on other fields. Typically
	 * not received from client, but some designs may receive and keep it for
	 * logging/legal purposes
	 */
	private final boolean isDerivedField;
	/**
	 * for optimized storage and retrieval, we may model the form as a sequence
	 * of fields rather than a collection of fields. this index is zero based
	 */
	private final int sequenceIdx;
	/**
	 * is this part of the conceptual key (document-id) of the form?
	 */
	private boolean isKeyField;

	/**
	 * this is generally invoked by the generated code for a Data Structure
	 * 
	 * @param fieldName
	 *            unique within its data structure
	 * @param dataType
	 *            pre-defined data type. used for validating data coming from a
	 *            client
	 * @param isRequired
	 *            is this field mandatory. used for validating data coming from
	 *            a client
	 * @param defaultValue
	 *            value to be used in case the client has not sent a value for
	 *            this. This e is used ONLY if isRequired is false. That is,
	 *            this is used if the field is optional, and the client skips
	 *            it. This value is NOT used if isRequired is set to true
	 * @param messageId
	 *            can be null in which case the id from dataType is used
	 * @param isDerivedField
	 *            true if this field value is derived/calculated based on other
	 *            fields. Like sum of other fields, or calculated based on sume
	 *            rule
	 * @param sequenceIdx
	 *            0 based sequence number of this field in the form
	 * @param isKeyField is this a key (document id) field?
	 */
	public Field(String fieldName, DataType dataType, boolean isRequired, String defaultValue, String messageId,
			boolean isDerivedField, int sequenceIdx, boolean isKeyField) {
		this.fieldName = fieldName;
		this.isRequired = isRequired;
		this.messageId = messageId;
		this.defaultValue = defaultValue;
		this.isDerivedField = isDerivedField;
		this.dataType = dataType;
		this.sequenceIdx = sequenceIdx;
		this.isKeyField = isKeyField;
		

	}

	/**
	 * @return the fieldName
	 */
	public String getFieldName() {
		return this.fieldName;
	}

	/**
	 * @return the defaultValue
	 */
	public String getDefaultValue() {
		return this.defaultValue;
	}

	/**
	 * @return the isRequired
	 */
	public boolean isRequired() {
		return this.isRequired;
	}

	/**
	 * @return the messageId
	 */
	public String getMessageId() {
		if (this.messageId != null) {
			return this.messageId;
		}
		return this.dataType.getMessageId();
	}

	/**
	 * @return 0 based sequence number of this field within the form. May be
	 *         used to store data in an array
	 * 
	 */
	public int getSequenceIdx() {
		return this.sequenceIdx;
	}

	/**
	 * @param value
	 *            text value to be validated
	 * @return true if this value is valid. false other wise.
	 */
	public boolean isValid(String value) {
		if (value == null || value.isEmpty()) {
			if (this.isRequired) {
				return false;
			}
			return true;
		}
		return this.dataType.isValid(value);
	}

	/**
	 * @return value type
	 */
	public ValueType getValueType() {
		return this.dataType.getValueType();
	}

	/**
	 * parse text into a long as per validations prescribed by this data element
	 * 
	 * @param value
	 *            input text.
	 * @return 0 if value is null or empty. parsed value if it is valid.
	 * @throws InvalidValueException
	 *             if the value is invalid
	 */
	public long parseLong(String value) throws InvalidValueException {
		try {
			return this.dataType.parseLong(value);
		} catch (Exception e) {
			//
		}
		throw new InvalidValueException(this.fieldName, this.getMessageId());
	}

	/**
	 * parse text as per validations set for this data element
	 * 
	 * @param inputValue
	 *            input text.
	 * @return empty string if value is null. inputValue if it is valid.
	 * @throws InvalidValueException
	 *             if the value is invalid
	 */
	public String parseText(String inputValue) throws InvalidValueException {
		try {
			return this.dataType.parseText(inputValue);
		} catch (Exception e) {
			//
		}
		throw new InvalidValueException(this.fieldName, this.getMessageId());
	}

	/**
	 * @return true if this field is derived based on other fields. false
	 *         otherwise
	 */
	public boolean isDerivedField() {
		return this.isDerivedField;
	}
	
	/**
	 * is this a key field?
	 * @return true if this is the key field, or one of the key fields
	 */
	public boolean isKeyField() {
		return this.isKeyField;
	}
}
