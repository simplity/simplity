/*
 * Copyright (c) 2017 simplity.org
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

package org.simplity.core.data;

import org.simplity.core.ApplicationError;
import org.simplity.core.dm.field.Field;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;
import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;
import org.simplity.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * how the JMS message body is used
 *
 * @author simplity.org
 */
public enum DataSerializationType {
	/** single field value as text. Actual value could be numeric etc.. */
	TEXT {
		@Override
		public String serializeFields(IFieldsCollection values, String[] names) {
			Value value = values.getValue(names[0]);
			if (value == null) {
				return null;
			}
			return value.toString();
		}

		@Override
		public String serializeFields(IFieldsCollection values, Field[] fields) {
			Value value = values.getValue(fields[0].getName());
			if (value == null) {
				return null;
			}
			return value.toString();
		}

		@Override
		public int parseFields(String text, IFieldsCollection inData, String[] names, int[] widths) {
			Value value = Value.parseValue(text);
			if (value == null) {
				return 0;
			}
			inData.setValue(names[0], value);
			return 1;
		}

		@Override
		public int parseFields(String text, IFieldsCollection inData, Field[] fields) {
			Field field = fields[0];
			Value value = field.getDataType().parseValue(text);
			if (value == null) {

				logger.info(text + " is not a valid value for field " + field.getName());

				return 0;
			}
			inData.setValue(field.getName(), value);
			return 1;
		}
	}
	/** field values in a predefined sequence separated by comma */
	,
	COMMA_SEPARATED {
		@Override
		public String serializeFields(IFieldsCollection fieldValues, String[] names) {
			Value[] values = new Value[names.length];

			for (int i = 0; i < names.length; i++) {
				values[i] = fieldValues.getValue(names[i]);
			}
			StringBuilder sbf = new StringBuilder();
			this.format(values, sbf);
			return sbf.toString();
		}

		@Override
		public String serializeFields(IFieldsCollection fieldValues, Field[] fields) {
			Value[] values = new Value[fields.length];
			for (int i = 0; i < fields.length; i++) {
				values[i] = fieldValues.getValue(fields[i].getName());
			}
			StringBuilder sbf = new StringBuilder();
			this.format(values, fields, sbf);
			return sbf.toString();
		}

		@Override
		public String serializeRows(Value[][] values, String[] names) {
			StringBuilder sbf = new StringBuilder();
			for (Value[] row : values) {
				this.format(row, sbf);
				sbf.append(NL);
			}
			return sbf.toString();
		}

		@Override
		public String serializeRows(Value[][] values, Field[] fields) {
			StringBuilder sbf = new StringBuilder();
			for (Value[] row : values) {
				this.format(row, fields, sbf);
				sbf.append(NL);
			}
			return sbf.toString();
		}

		@Override
		public int parseFields(String rowText, IFieldsCollection inData, String[] names, int[] widths) {
			if (names == null || names.length == 0) {
				this.fieldNameRequired();
				return 0;
			}
			int nbrFields = 0;
			Value[] values = this.extract(rowText, (ValueType[]) null);

			for (int i = 0; i < names.length; i++) {
				Value value = values[i];
				if (value != null) {
					inData.setValue(names[i], value);
					nbrFields++;
				}
			}
			return nbrFields;
		}

		@Override
		public int parseFields(String rowText, IFieldsCollection inData, Field[] fields) {
			if (fields == null || fields.length == 0) {
				this.fieldNameRequired();
				return 0;
			}
			int nbrFields = 0;
			Value[] values = this.extract(rowText, fields);
			for (int i = 0; i < fields.length; i++) {
				Value value = values[i];
				if (value != null) {
					inData.setValue(fields[i].getName(), value);
					nbrFields++;
				}
			}
			return nbrFields;
		}

		@Override
		public MultiRowsSheet parseRows(String text, String[] names) {
			if (names == null || names.length == 0) {
				this.fieldNameRequired();
				return null;
			}
			String[] texts = text.split(NL);
			Value[] values = this.extract(texts[0], (ValueType[]) null);
			ValueType[] types = new ValueType[values.length];
			for (int i = 0; i < types.length; i++) {
				Value value = values[i];
				types[i] = value == null ? ValueType.TEXT : value.getValueType();
			}
			MultiRowsSheet sheet = new MultiRowsSheet(names, types);
			sheet.addRow(values);
			for (int i = 1; i < texts.length; i++) {
				sheet.addRow(this.extract(texts[i], (ValueType[]) null));
			}
			return sheet;
		}

		@Override
		public MultiRowsSheet parseRows(String text, Field[] fields) {
			if (fields == null || fields.length == 0) {
				this.fieldNameRequired();
				return null;
			}
			String[] texts = text.split(NL);
			MultiRowsSheet sheet = new MultiRowsSheet(fields);
			for (int i = 0; i < texts.length; i++) {
				sheet.addRow(this.extract(texts[i], fields));
			}
			return sheet;
		}

		private void fieldNameRequired() {
			throw new ApplicationError(
					"Field name is requried for data serialization using comma separated fields");
		}

		private void format(Value[] values, StringBuilder sbf) {
			for (Value value : values) {
				sbf.append(value.toString());
				sbf.append(COMMA);
			}
			sbf.setLength(sbf.length() - 1);
		}

		private void format(Value[] values, Field[] fields, StringBuilder sbf) {
			for (int i = 0; i < values.length; i++) {
				sbf.append(fields[i].getDataType().formatValue(values[i]));
				sbf.append(COMMA);
			}
			sbf.setLength(sbf.length() - 1);
		}

		private Value[] extract(String rowText, ValueType[] types) {
			String[] texts = rowText.split(COMMA_STR);
			if (types != null && types.length != texts.length) {
				throw new ApplicationError(
						"Text row has "
								+ texts.length
								+ " comma separated values but we are expecting "
								+ types.length
								+ ".");
			}
			Value[] values = new Value[texts.length];
			for (int i = 0; i < texts.length; i++) {
				if (types == null) {
					values[i] = Value.parseValue(texts[i]);
				} else {
					values[i] = Value.parseValue(texts[i], types[i]);
				}
			}
			return values;
		}

		private Value[] extract(String rowText, Field[] fields) {
			String[] texts = rowText.split(COMMA_STR);
			if (fields.length != texts.length) {
				throw new ApplicationError(
						"Text row has "
								+ texts.length
								+ " comma separated values but we are expecting "
								+ fields.length
								+ ".");
			}
			Value[] values = new Value[texts.length];
			for (int i = 0; i < texts.length; i++) {
				values[i] = fields[i].getDataType().parseValue(texts[i]);
			}
			return values;
		}
	}
	/** JSONtext with field and value */
	,
	JSON {
		@Override
		public String serializeFields(IFieldsCollection fieldValues, String[] names) {
			JSONWriter writer = new JSONWriter();
			writer.object();
			for (String name : names) {
				writer.key(name);
				Value value = fieldValues.getValue(name);

				if (Value.isNull(value)) {
					writer.value(null);
				} else {
					writer.value(value.toObject());
				}
			}
			writer.object();
			return writer.toString();
		}

		@Override
		public String serializeFields(IFieldsCollection fieldValues, Field[] fields) {
			JSONWriter writer = new JSONWriter();
			writer.object();
			for (Field field : fields) {
				String name = field.getName();
				writer.key(name);
				Value value = fieldValues.getValue(name);

				if (Value.isNull(value)) {
					writer.value(null);
				} else {
					writer.value(value.toObject());
				}
			}
			writer.endObject();
			return writer.toString();
		}

		@Override
		public String serializeRows(Value[][] values, String[] names) {
			JSONWriter writer = new JSONWriter();
			writer.array();
			for (Value[] row : values) {
				writer.object();
				for (int i = 0; i < names.length; i++) {
					writer.key(names[i]);
					Value value = row[i];
					if (Value.isNull(value)) {
						writer.value(null);
					} else {
						writer.value(value.toObject());
					}
				}
				writer.endObject();
			}
			return writer.toString();
		}

		@Override
		public String serializeRows(Value[][] values, Field[] fields) {
			JSONWriter writer = new JSONWriter();
			writer.array();
			for (Value[] row : values) {
				writer.object();
				for (int i = 0; i < fields.length; i++) {
					writer.key(fields[i].getName());
					Value value = row[i];
					if (Value.isNull(value)) {
						writer.value(null);
					} else {
						writer.value(value.toObject());
					}
				}
				writer.endObject();
			}
			return writer.toString();
		}

		@Override
		public int parseFields(String rowText, IFieldsCollection inData, String[] names, int[] widths) {
			JSONObject json = new JSONObject(rowText);
			String[] namesToUse = names;
			if (names == null) {
				namesToUse = json.keySet().toArray(new String[0]);
			}
			int nbrFields = 0;
			for (String name : namesToUse) {
				Object obj = json.opt(name);
				if (obj != null) {
					inData.setValue(name, Value.parseObject(obj));
					nbrFields++;
				}
			}
			return nbrFields;
		}

		@Override
		public int parseFields(String rowText, IFieldsCollection inData, Field[] fields) {
			JSONObject json = new JSONObject(rowText);
			int nbrFields = 0;
			for (Field field : fields) {
				String name = field.getName();
				Object obj = json.opt(name);
				if (obj != null) {
					inData.setValue(name, Value.parseObject(obj));
					nbrFields++;
				}
			}
			return nbrFields;
		}

		@Override
		public MultiRowsSheet parseRows(String text, String[] names) {
			JSONArray json = new JSONArray(text);
			Value[] values = this.extract((JSONObject) json.opt(0), names);
			ValueType[] types = new ValueType[values.length];
			for (int i = 0; i < types.length; i++) {
				Value value = values[i];
				types[i] = value == null ? ValueType.TEXT : value.getValueType();
			}
			MultiRowsSheet sheet = new MultiRowsSheet(names, types);
			sheet.addRow(values);
			int nbrRows = json.length();
			for (int i = 1; i < nbrRows; i++) {
				sheet.addRow(this.extract((JSONObject) json.opt(i), names));
			}
			return sheet;
		}

		@Override
		public MultiRowsSheet parseRows(String text, Field[] fields) {
			JSONArray json = new JSONArray(text);
			MultiRowsSheet sheet = new MultiRowsSheet(fields);
			int nbrRows = json.length();
			for (int i = 0; i < nbrRows; i++) {
				sheet.addRow(this.extract((JSONObject) json.opt(i), fields));
			}
			return sheet;
		}

		private Value[] extract(JSONObject json, String names[]) {
			Value[] values = new Value[names.length];
			for (int i = 0; i < names.length; i++) {
				Object obj = json.opt(names[i]);
				if (obj != null) {
					values[i] = Value.parseObject(obj);
				}
			}
			return values;
		}

		private Value[] extract(JSONObject json, Field fields[]) {
			Value[] values = new Value[fields.length];
			for (int i = 0; i < fields.length; i++) {
				Object obj = json.opt(fields[i].getName());
				if (obj != null) {
					values[i] = Value.parseObject(obj);
				}
			}
			return values;
		}
	}
	/** xml with each field as an element */
	,
	XML
	/** serialized object */
	, OBJECT
	/** serialized object - special case of Map */
	,MAP;
	protected static final Logger logger = LoggerFactory.getLogger(DataSerializationType.class);

	protected static final String NL = "\\r?\\n";
	protected static final char COMMA = ',';
	protected static final String COMMA_STR = ",";

	/**
	 * serialize set of key-value pairs
	 *
	 * @param values
	 * @param names
	 *            if the formatting uses names. null if this is not required.
	 * @return text that can be de-serialized back. Suitable to transport data
	 *         across layers/domains
	 */
	public String serializeFields(IFieldsCollection values, String[] names) {
		this.notSupported("serialization with key-value pairs");
		return null;
	}

	/**
	 * serialize rows of data
	 *
	 * @param values
	 *            rows of data, each having array of column values
	 * @param names
	 *            if the formatting uses names. null if this is not required
	 * @return text that can be de-serialized back. Suitable to transport data
	 *         across layers/domains
	 */
	public String serializeRows(Value[][] values, String[] names) {
		this.notSupported("serialization with key-value pairs");
		return null;
	}

	/**
	 * serialize set of key-value pairs. Use this if you have the fields
	 *
	 * @param values
	 * @param fields
	 *            fields that make up this format
	 * @return text that can be de-serialized back. Suitable to transport data
	 *         across layers/domains
	 */
	public String serializeFields(IFieldsCollection values, Field[] fields) {
		this.notSupported("serialization with key-value pairs");
		return null;
	}

	/**
	 * serialize rows of data
	 *
	 * @param values
	 *            rows of data, each having array of column values
	 * @param fields
	 *            that make a row/record of this format
	 * @return text that can be de-serialized back. Suitable to transport data
	 *         across layers/domains
	 */
	public String serializeRows(Value[][] values, Field[] fields) {
		this.notSupported("serialization with key-value pairs");
		return null;
	}

	/**
	 * extract data from a text that should have been serialized by
	 * toSerializedText()
	 *
	 * @param text
	 *            serialized representation
	 * @param inData
	 *            into which fields are to be extracted
	 * @param names
	 *            if the format does not have the names, or if we want to
	 *            extract subset. null if this is not required.
	 * @param widths
	 *            in case the format uses fixed widths. null if this is not
	 *            relevant.
	 * @return number of fields extracted
	 */
	public int parseFields(String text, IFieldsCollection inData, String[] names, int[] widths) {
		this.notSupported("de-serialization with key-value pairs");
		return 0;
	}

	/**
	 * extract multiple rows from a serialized text
	 *
	 * @param text
	 *            serialized text from which to extract data
	 * @param names
	 *            field names. null if this is not relevant or not required
	 * @return data sheet into which dat is extracted
	 */
	public MultiRowsSheet parseRows(String text, String[] names) {
		this.notSupported("de-serialization with key-value pairs");
		return null;
	}

	/**
	 * parse a row of text into fields using field specification
	 *
	 * @param rowText
	 *            serialized text for a set of fields
	 * @param inData
	 *            to which fields are to be extracted
	 * @param fields
	 *            fields to be extracted
	 * @return number of fields extracted
	 */
	public int parseFields(String rowText, IFieldsCollection inData, Field[] fields) {
		this.notSupported("de-serialization with key-value pairs");
		return 0;
	}

	/**
	 * extract rows of data from a text that should have been serialized by
	 * toSerializedText()
	 *
	 * @param text
	 *            serialized representation
	 * @param fields
	 *            fields that make up a row of this data sheet
	 * @return data sheet that contains the data
	 */
	public MultiRowsSheet parseRows(String text, Field[] fields) {
		this.notSupported("de-serialization with key-value pairs");
		return null;
	}

	/**
	 * throw unsupported format exception
	 *
	 * @param text
	 */
	private void notSupported(String text) {
		throw new ApplicationError("Data Serialization Type " + this + " is not designed for " + text);
	}
}
