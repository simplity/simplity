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

package org.simplity.kernel.util;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;
import org.simplity.json.JSONWriter;
import org.simplity.json.Jsonable;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FilterCondition;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.Messages;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.FieldsInterface;
import org.simplity.kernel.data.HierarchicalSheet;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.dm.Field;
import org.simplity.kernel.dt.DataType;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.simplity.service.ServiceContext;
import org.simplity.service.ServiceProtocol;

/**
 * utilities that help Simplity deal with JSON
 *
 * @author simplity.org
 *
 */
public class JsonUtil {

	/**
	 * create a data sheet out of a well-formed json array of simple jsonObject.
	 *
	 * @param arr
	 *            that has the json array
	 * @param inputFields
	 *            Fields to be input. null if we are to take whatever is offered
	 * @param errors
	 *            to which any validation errors are added
	 * @param allFieldsAreOptional
	 *            true of we are to consider all fields as optional, even if the
	 *            field specifies it as mandatory
	 * @param parentFieldName
	 *            if this is a child sheet, specify the column name in this
	 *            sheet that should be populated with the parent key value
	 * @param parentValue
	 *            if this is a child sheet, and you have specified
	 *            parentFieldName, value to be populated in each row for that
	 *            column
	 * @return data sheet. Null if no data found or the json is not well
	 *         formated. was null. case the array is not well-formed
	 */
	public static DataSheet getSheet(JSONArray arr, Field[] inputFields, List<FormattedMessage> errors,
			boolean allFieldsAreOptional, String parentFieldName, Value parentValue) {
		if (arr == null || arr.length() == 0) {
			return null;
		}
		Field[] fields = inputFields;
		int parentIdx = -1;
		if (fields == null) {
			/*
			 * we guess the fields based on the attributes of first element in
			 * the array
			 */
			JSONObject exampleObject = arr.optJSONObject(0);
			if (exampleObject == null) {
				Tracer.trace("Json array has its first object as null, and hence we abandoned parsing it.");
				return null;
			}
			fields = getFields(exampleObject, null, null);
			if (parentFieldName != null) {
				Field[] newFields = new Field[fields.length + 1];
				newFields[0] = Field.getDefaultField(parentFieldName, parentValue.getValueType());
				int j = 1;
				for (Field field : fields) {
					newFields[j] = field;
					j++;
				}
				parentIdx = 0;
			}
		} else if (parentFieldName != null) {
			int j = 0;
			for (Field field : fields) {
				if (field.getName().equals(parentFieldName)) {
					parentIdx = j;
					break;
				}
				j++;
			}
			if (parentIdx == -1) {
				Tracer.trace("Parent field name " + parentFieldName
						+ " not found in the fields list for child. Filed will not be populated from parent sheet.");
			}
		}
		DataSheet ds = new MultiRowsSheet(fields);
		int nbrRows = arr.length();
		/*
		 * let us now extract each row into data sheet
		 */
		for (int i = 0; i < nbrRows; i++) {
			JSONObject obj = arr.optJSONObject(i);
			if (obj == null) {
				Tracer.trace("Row " + (i + 1) + " is null. Not extracted");
				continue;
			}
			int j = 0;
			Value[] row = new Value[fields.length];
			for (Field field : fields) {
				Object val = obj.opt(field.getName());
				if (j == parentIdx) {
					row[j] = parentValue;
				} else {
					row[j] = field.parseObject(val, errors, allFieldsAreOptional, null);
				}
				j++;
			}
			ds.addRow(row);
		}
		return ds;
	}

	/**
	 * supplied jsonArray has the parent rows. Extract child rows from these
	 * array elements
	 *
	 * @param arr
	 * @param attName
	 *            attribute name that holds the child JSONArray
	 * @param fields
	 *            expected fields. Input data is validated as per these field
	 *            specifications.
	 * @param errors
	 * @param allFieldsAreOptional
	 *
	 * @return data sheet. Null if no data found. Throws ApplicationError on
	 *         case the array is not well-formed
	 */
	public static DataSheet getChildSheet(JSONArray arr, String attName, Field[] fields, List<FormattedMessage> errors,
			boolean allFieldsAreOptional) {
		/*
		 * arr corresponds to following json. We are to accumulate child rows
		 * across all main rows
		 *
		 * [...,"attName"=[{},{}....],..],[....,"attName"=[{},{}.... ],..]....
		 */
		Field[] inputFields = fields;
		DataSheet ds = null;
		if (inputFields != null) {
			ds = new MultiRowsSheet(inputFields);
		}
		/*
		 * we are not sure of getting a valid child row in first element. So,
		 * let us have a flexible strategy
		 */
		int nbrParentRows = arr.length();
		/*
		 * for each parent row
		 */
		for (int i = 0; i < nbrParentRows; i++) {
			JSONObject pr = arr.optJSONObject(i);
			if (pr == null) {
				continue;
			}
			JSONArray rows = pr.optJSONArray(attName);
			if (rows == null) {
				continue;
			}
			int n = rows.length();
			/*
			 * extract this child row into ds
			 */
			for (int idx = 0; idx < n; idx++) {
				JSONObject obj = rows.optJSONObject(idx);
				if (obj == null) {
					continue;
				}
				if (ds == null || inputFields == null) {
					inputFields = getFields(obj, null, null);
					ds = new MultiRowsSheet(inputFields);
				}
				int j = 0;
				Value[] row = new Value[fields.length];
				for (Field field : inputFields) {
					Object val = obj.opt(field.getName());
					row[j] = field.parseObject(val, errors, allFieldsAreOptional, attName);
					j++;
				}
				ds.addRow(row);
			}
		}
		return ds;
	}

	/**
	 * write the data sheet to json
	 *
	 * @param writer
	 * @param ds
	 * @param childSheets
	 * @param outputAsObject
	 *            if the data sheet is meant for an object/data structure and
	 *            not an array of them. Only first row is used
	 */
	public static void sheetToJson(JSONWriter writer, DataSheet ds, HierarchicalSheet[] childSheets,
			boolean outputAsObject) {
		int nbrRows = 0;
		int nbrCols = 0;
		if (ds != null) {
			nbrRows = ds.length();
			nbrCols = ds.width();
		}
		if (ds == null || nbrRows == 0 || nbrCols == 0) {
			writer.value(null);
			Tracer.trace("Sheet  has no data. json is not added");
			return;
		}
		if (outputAsObject) {
			nbrRows = 1;
		} else {
			writer.array();
		}
		String[] names = ds.getColumnNames();
		for (int i = 0; i < nbrRows; i++) {
			writer.object();
			/*
			 * note that getRow() returns values in the same order as in
			 * getColumnNames()
			 */
			Value[] row = ds.getRow(i);
			int j = 0;
			for (String colName : names) {
				Value value = row[j];
				/*
				 * no need to write null attributes
				 */
				if (value != null) {
					writer.key(colName).value(value.toObject());
				}
				j++;
			}

			if (childSheets != null) {
				for (HierarchicalSheet childSheet : childSheets) {
					if (childSheet != null) {
						childSheet.toJson(writer, row);
					}
				}
			}
			writer.endObject();
		}
		if (outputAsObject == false) {
			writer.endArray();
		}
	}

	/**
	 * write the first column of data sheet as an array
	 *
	 * @param writer
	 * @param ds
	 */
	public static void sheetToArray(JSONWriter writer, DataSheet ds) {
		writer.array();
		if (ds != null && ds.length() > 0) {
			/*
			 * if rows exist, then first column is guaranteed
			 */
			for (Value[] row : ds.getAllRows()) {
				Value value = row[0];
				if (value != null) {
					writer.value(value.toObject());
				}
			}
		}
		writer.endArray();
	}

	/**
	 * create a data sheet for attributes in this object
	 *
	 * @param obj
	 * @param additionalAtt
	 * @param additionalVal
	 * @return array of fields in this object. additional att/val if supplied
	 *         are added as the first one.
	 */
	public static Field[] getFields(JSONObject obj, String additionalAtt, Object additionalVal) {
		String[] names = JSONObject.getNames(obj);
		int nbrCols = names.length;
		int fieldIdx = 0;
		Field[] fields = new Field[nbrCols];
		if (additionalAtt != null) {
			/*
			 * rare case, and hence not-optimized for creation of fields
			 */
			nbrCols++;
			fields = new Field[nbrCols];
			Value val = Value.parseObject(additionalVal);
			fields[fieldIdx] = Field.getDefaultField(additionalAtt, val.getValueType());
			fieldIdx = 1;
		}
		int nonAtts = 0;
		for (String colName : names) {
			Object val = obj.opt(colName);
			if (val instanceof JSONArray || val instanceof JSONObject) {
				/*
				 * this is not a att-value.
				 */
				nonAtts++;
			} else {
				ValueType vt = Value.parseObject(val).getValueType();
				fields[fieldIdx] = Field.getDefaultField(colName, vt);
				fieldIdx++;
			}
		}
		if (nonAtts == 0) {
			return fields;
		}

		/*
		 * this is rare case, and hence we have not optimized the algorithm for
		 * this case. non-primitive attributes would have their valueType set to
		 * null. Copy primitive-ones to a new array.
		 */
		nbrCols = nbrCols - nonAtts;
		Field[] newFields = new Field[nbrCols];
		for (int i = 0; i < newFields.length; i++) {
			newFields[i] = fields[i];
		}
		return newFields;
	}

	/**
	 * @param json
	 * @param fields
	 * @param ctx
	 * @param errors
	 * @param allFieldsAreOptional
	 * @return number of fields extracted
	 */
	public static int extractFields(JSONObject json, Field[] fields, FieldsInterface ctx, List<FormattedMessage> errors,
			boolean allFieldsAreOptional) {
		int result = 0;
		for (Field field : fields) {
			Object val = json.opt(field.getName());
			Value value = null;
			if (val == null) {
				/*
				 * possible that this field is already extracted
				 */
				value = ctx.getValue(field.getName());
			} else {
				value = field.getValueType().parseObject(val);
				if (value == null) {
					errors.add(new FormattedMessage(Messages.INVALID_VALUE, null, field.getName(), null, 0,
							'\'' + val.toString() + "' is not a valid " + field.getValueType()));
					continue;
				}
			}
			/*
			 * this is validation, and not exactly parse.
			 */
			value = field.parse(value, errors, allFieldsAreOptional, null);
			if (value != null) {
				ctx.setValue(field.getName(), value);
				result++;
			}
		}
		return result;
	}

	/**
	 * @param json
	 * @param names
	 * @param ctx
	 * @return number of fields added
	 */
	public static int extractFields(JSONObject json, String[] names, FieldsInterface ctx) {
		int result = 0;
		for (String name : names) {
			Object val = json.opt(name);
			if (val != null) {
				ctx.setValue(name, Value.parseObject(val));
				result++;
			}
		}
		return result;
	}

	/**
	 * @param json
	 * @param fields
	 * @param ctx
	 * @param errors
	 * @return number of fields extracted
	 */
	public static int extractFilterFields(JSONObject json, Field[] fields, FieldsInterface ctx,
			List<FormattedMessage> errors) {
		int result = 0;
		for (Field field : fields) {
			result += parseFilter(json, ctx, errors, field.getName(), field.getValueType());
		}
		/*
		 * some additional fields for filter, like sort
		 */
		/*
		 * what about sort ?
		 */
		String fieldName = ServiceProtocol.SORT_COLUMN_NAME;
		String textValue = json.optString(fieldName, null);
		if (textValue != null) {
			Value value = ComponentManager.getDataType(DataType.ENTITY_LIST).parseValue(textValue);
			if (value == null) {
				errors.add(new FormattedMessage(Messages.INVALID_ENTITY_LIST, null, fieldName, null, 0));
			} else {
				ctx.setValue(fieldName, value);
			}
		}

		fieldName = ServiceProtocol.SORT_ORDER;
		textValue = json.optString(fieldName, null);
		if (textValue != null) {
			textValue = textValue.toLowerCase();
			if (textValue.equals(ServiceProtocol.SORT_ORDER_ASC) || textValue.equals(ServiceProtocol.SORT_ORDER_DESC)) {
				ctx.setValue(fieldName, Value.newTextValue(textValue));
			} else {
				errors.add(new FormattedMessage(Messages.INVALID_SORT_ORDER, null, fieldName, null, 0));
			}
		}
		return result;
	}

	/**
	 * parse input object as a filter field
	 *
	 * @param json
	 * @param extratedFields
	 *            to which extracted fields are to be added
	 * @param validationErrors
	 * @param recordName
	 * @return number of fields extracted
	 */
	private static int parseFilter(JSONObject json, FieldsInterface extratedFields,
			List<FormattedMessage> validationErrors, String fieldName, ValueType valueType) {

		Object obj = json.opt(fieldName);
		if (obj == null) {
			return 0;
		}
		/*
		 * what is the comparator
		 */
		String otherName = fieldName + ServiceProtocol.COMPARATOR_SUFFIX;
		String otherValue = json.optString(otherName, null);
		FilterCondition f = FilterCondition.parse(otherValue);
		/*
		 * filter field need not conform to data-type but it should be of the
		 * same value type, except that IN_LIST is always text
		 */
		Value value = FilterCondition.In == f ? ValueType.TEXT.parseObject(obj) : valueType.parseObject(obj);
		if (value == null) {
			if (validationErrors != null) {
				validationErrors.add(new FormattedMessage(Messages.INVALID_VALUE, null, fieldName, null, 0));
			}
		} else {
			extratedFields.setValue(fieldName, value);
		}
		if (f == null) {
			extratedFields.setValue(otherName, Value.newTextValue(ServiceProtocol.EQUAL));
			return 1;
		}
		extratedFields.setValue(otherName, Value.newTextValue(otherValue));
		if (f != FilterCondition.Between) {
			return 1;
		}
		otherName = fieldName + ServiceProtocol.TO_FIELD_SUFFIX;
		Object val = json.opt(otherName);
		value = null;
		if (val != null) {
			value = valueType.parseObject(val);
		}
		if (value == null) {
			if (validationErrors != null) {
				validationErrors.add(
						new org.simplity.kernel.FormattedMessage(Messages.INVALID_VALUE, null, otherName, null, 0));
			}
		} else {
			extratedFields.setValue(otherName, value);
		}
		return 1;
	}

	/**
	 * extract a simple json object (with fields and tables) into service
	 * context
	 *
	 * @param jsonText
	 * @param ctx
	 */
	public static void extractAll(String jsonText, ServiceContext ctx) {
		JSONObject json = null;
		try {
			json = new JSONObject(jsonText);
		} catch (Exception e) {
			ctx.addMessage(Messages.INVALID_DATA, "Input json is invalid");
			return;
		}
		extractAll(json, ctx);
	}

	/**
	 * Create json string from all data from context
	 * context
	 *
	 * @param ctx
	 * @return jsonText
	 */
	public static String outputAll(ServiceContext ctx) {
		JSONWriter writer = new JSONWriter();
		writer.object();
		for (Map.Entry<String, Value> entry : ctx.getAllFields()) {
			writer.key(entry.getKey()).value(entry.getValue());
		}

		for (Map.Entry<String, DataSheet> entry : ctx.getAllSheets()) {
			writer.key(entry.getKey());
			DataSheet sheet = entry.getValue();
			JsonUtil.sheetToJson(writer, sheet, null, false);
		}
		List<FormattedMessage> msgs = ctx.getMessages();
		if (msgs.size() > 0) {
			writer.key(ServiceProtocol.MESSAGES);
			JsonUtil.addObject(writer, msgs);
		}
		writer.key(ServiceProtocol.REQUEST_STATUS);
		if (ctx.isInError()) {
			writer.value(ServiceProtocol.STATUS_ERROR);
		} else {
			writer.value(ServiceProtocol.STATUS_OK);
		}
		writer.endObject();
		return writer.toString();
	}

	/**
	 * extract a simple json object (with fields and tables) into service
	 * context
	 *
	 * @param json
	 * @param ctx
	 */
	public static void extractAll(JSONObject json, ServiceContext ctx) {
		for (String key : json.keySet()) {
			JSONArray arr = json.optJSONArray(key);
			if (arr != null) {
				DataSheet sheet = JsonUtil.getSheet(arr, null, null, true, null, null);
				if (sheet == null) {
					Tracer.trace("Table " + key + " could not be extracted");
				} else {
					ctx.putDataSheet(key, sheet);
					Tracer.trace("Table " + key + " extracted with " + sheet.length() + " rows");
				}
				continue;
			}
			JSONObject obj = json.optJSONObject(key);
			if (obj != null) {
				/*
				 * we do not have a standard for converting data structure. As
				 * of now, we just copy this json
				 */
				ctx.setObject(key, obj);
				Tracer.trace(key + " retained as a JSON into ctx");
				continue;
			}
			Object val = json.opt(key);
			Value value = Value.parseObject(val);
			if (value != null) {
				ctx.setValue(key, value);
				Tracer.trace(key + " = " + value + " extracted");
			} else {
				Tracer.trace(key + " = " + val + " is not extracted");
			}
		}
	}

	/**
	 *
	 * @param json
	 * @param ctx
	 * @param sheetName
	 * @param parentSheetName
	 */
	public static void extractWithNoValidation(JSONObject json, ServiceContext ctx, String sheetName,
			String parentSheetName) {
		DataSheet ds = null;
		String arrName = null;
		JSONArray arr = null;
		if (parentSheetName != null) {
			arr = json.optJSONArray(parentSheetName);
			if (arr == null) {
				arrName = parentSheetName;
			} else {
				ds = getChildSheet(arr, sheetName, null, null, true);
			}
		} else {
			arr = json.optJSONArray(sheetName);
			if (arr == null) {
				arrName = sheetName;
			} else {
				ds = JsonUtil.getSheet(arr, null, null, true, null, null);
			}
		}
		if (arr == null) {
			Tracer.trace("No data found for sheet " + arrName);
		} else if (ds == null) {
			Tracer.trace("Sheet " + arrName + " has only null data. Data not extracted");
		} else {
			ctx.putDataSheet(sheetName, ds);
		}
	}

	/**
	 * @param writer
	 * @param fieldNames
	 * @param ctx
	 */
	public static void addAttributes(JSONWriter writer, String[] fieldNames, ServiceContext ctx) {
		for (String fieldName : fieldNames) {
			fieldName = TextUtil.getFieldValue(ctx, fieldName).toText();
			Value value = ctx.getValue(fieldName);
			if (value != null) {
				if (value.isUnknown() == false) {
					writer.key(fieldName).value(value.toObject());
				}
				continue;
			}
			Object obj = ctx.getObject(fieldName);
			if (obj != null) {
				writer.key(fieldName);
				addObject(writer, obj);
			}
		}
	}

	/**
	 * write an arbitrary object to json
	 *
	 * @param writer
	 * @param obj
	 */
	public static void addObject(JSONWriter writer, Object obj) {
		if (obj == null) {
			writer.value(null);
			return;
		}
		if (obj instanceof Jsonable) {
			((Jsonable) obj).writeJsonValue(writer);
			return;
		}
		if (obj instanceof String || obj instanceof Number || obj instanceof Boolean || obj instanceof Date
				|| obj instanceof Enum) {
			writer.value(obj);
			return;
		}
		if (obj.getClass().isArray()) {
			writer.array();
			int n = Array.getLength(obj);
			for (int i = 0; i < n; i++) {
				addObject(writer, Array.get(obj, i));
			}
			writer.endArray();
			return;
		}
		if (obj instanceof Map) {
			writer.object();
			@SuppressWarnings("unchecked")
			Map<String, Object> childMap = (Map<String, Object>) obj;
			for (Map.Entry<String, Object> childEntry : childMap.entrySet()) {
				writer.key(childEntry.getKey());
				addObject(writer, childEntry.getValue());
			}
			writer.endObject();
			return;
		}
		if (obj instanceof Collection) {
			writer.array();
			@SuppressWarnings("unchecked")
			Collection<Object> children = (Collection<Object>) obj;
			for (Object child : children) {
				addObject(writer, child);
			}
			writer.endArray();
			return;
		}
		/*
		 * it is another object
		 */
		writer.object();
		for (Map.Entry<String, java.lang.reflect.Field> entry : ReflectUtil.getAllFields(obj).entrySet()) {
			writer.key(entry.getKey());
			try {
				addObject(writer, entry.getValue().get(obj));
			} catch (Exception e) {
				Tracer.trace("Unable to get value for object attribute " + entry.getKey() + ". null assumed");
				writer.value(null);
			}
		}
		writer.endObject();
	}

	/**
	 * @param object
	 *            to be convert to json
	 * @return json string for the object
	 */
	public static String toJson(Object object) {
		Writer w = new StringWriter();
		JSONWriter writer = new JSONWriter(w);
		addObject(writer, object);
		return w.toString();
	}

	/**
	 * append the text to string builder duly quoted and escaped as per JSON
	 * standard.
	 *
	 * @param value
	 *            to be appended
	 * @param json
	 *            to be appended to
	 */
	public static void appendQoutedText(String value, StringBuilder json) {
		if (value == null || value.length() == 0) {
			json.append("\"\"");
			return;
		}

		char lastChar = 0;
		String hhhh;

		json.append('"');
		for (char c : value.toCharArray()) {
			switch (c) {
			case '\\':
			case '"':
				json.append('\\');
				json.append(c);
				break;
			case '/':
				if (lastChar == '<') {
					json.append('\\');
				}
				json.append(c);
				break;
			case '\b':
				json.append("\\b");
				break;
			case '\t':
				json.append("\\t");
				break;
			case '\n':
				json.append("\\n");
				break;
			case '\f':
				json.append("\\f");
				break;
			case '\r':
				json.append("\\r");
				break;
			default:
				if (c < ' ' || (c >= '\u0080' && c < '\u00a0') || (c >= '\u2000' && c < '\u2100')) {
					json.append("\\u");
					hhhh = Integer.toHexString(c);
					json.append("0000", 0, 4 - hhhh.length());
					json.append(hhhh);
				} else {
					json.append(c);
				}
			}
			lastChar = c;
		}
		json.append('"');
	}

	/**
	 * convert a JSON array to array of primitive objects.
	 *
	 * @param array
	 *            json Array
	 * @return array of primitives, or null in case any of the array element is
	 *         not primitive
	 */
	public static Object[] toObjectArray(JSONArray array) {
		Object[] result = new Object[array.length()];
		for (int i = 0; i < result.length; i++) {
			Object obj = array.get(i);
			if (obj == null) {
				continue;
			}
			if (obj instanceof JSONObject || obj instanceof JSONArray) {
				Tracer.trace("Element no (zero based) " + i
						+ " is not a primitive, and hence unable to convert the JSONArray into an array of primitives");
				return null;
			}
			result[i] = obj;
		}
		return result;
	}

	/**
	 * get value of a qualified field name down the json object structure.
	 *
	 * @param fieldSelector
	 *            can be of the form a.b.c.. where each part can be int (for
	 *            array index) or name (for attribute).
	 * @param json
	 *            Should be either JSONObject or JSONArray
	 * @return attribute value as per the tree. null if not found.
	 * @throws ApplicationError
	 *             in case the fieldName pattern and the JSONObject structure
	 *             are not in synch.
	 *
	 */
	public static Object getValue(String fieldSelector, Object json) {
		return getValueWorker(fieldSelector, json, 0);
	}

	/**
	 * common worker method to go down the object as per selector
	 *
	 * @param fieldSelector
	 * @param json
	 * @param option
	 *
	 *            <pre>
	 * 0 means do not create/add anything. return null if anything is not found
	 * 1 means create, add and return a JSON object at the end if it is missing
	 * 2 means create, add and return a JSON array at the end if it is missing
	 *            </pre>
	 *
	 * @return
	 */
	private static Object getValueWorker(String fieldSelector, Object json, int option) {
		/*
		 * be considerate for careless-callers..
		 */
		if (fieldSelector == null || fieldSelector.isEmpty()) {
			Tracer.trace("Null/empty selector for get/setValue");
			if (option == 0) {
				return null;
			}
			if (option == 1) {
				return new JSONObject();
			}
			return new JSONArray();
		}
		/*
		 * special case that indicates root object itself
		 */
		if (fieldSelector.charAt(0) == '.') {
			return json;
		}

		String[] parts = fieldSelector.split("\\.");
		Object result = json;
		int lastPartIdx = parts.length - 1;
		try {
			for (int i = 0; i < parts.length; i++) {
				String part = parts[i];
				part = part.trim();
				if (part.isEmpty()) {
					throw new ApplicationError(fieldSelector + " is malformed for a qualified json field name.");
				}
				int idx = parseIdx(part);
				Object child = null;
				JSONObject resultObj = null;
				JSONArray resultArr = null;
				if (result instanceof JSONObject) {
					resultObj = (JSONObject) result;
					child = resultObj.opt(part);
				} else if (result instanceof JSONArray) {
					if (idx == -1) {
						throw new ApplicationError(fieldSelector
								+ " is not an appropriate selector. We encountered a object when we were expecting an array for index "
								+ idx);
					}
					resultArr = (JSONArray) result;
					child = resultArr.opt(idx);
				} else {
					throw new ApplicationError(fieldSelector
							+ " is not an appropriate selector as we encountered a non-object on the path.");
				}
				if (child != null) {
					result = child;
					continue;
				}
				if (option == 0) {
					/*
					 * no provisioning. get out of here.
					 */
					return null;
				}
				/*
				 * we create an array or an object and add it to the object.
				 */
				boolean goForObject = option == 1;
				if (i < lastPartIdx) {
					/*
					 * If next part is attribute, then we create an object, else
					 * an array
					 */
					goForObject = parseIdx(parts[i + 1]) == -1;
				}
				if (goForObject) {
					child = new JSONObject();
				} else {
					child = new JSONArray();
				}
				if (resultObj != null) {
					resultObj.put(part, child);
				} else if (resultArr != null) {
					// we have put else-if to calm down the lint!!
					resultArr.put(idx, child);
				}
				result = child;
			}
			return result;
		} catch (NumberFormatException e) {
			throw new ApplicationError(fieldSelector + " is malformed for a qualified json field name.");
		} catch (ClassCastException e) {
			throw new ApplicationError(fieldSelector
					+ " is used as an attribute-selector for a test case, but the json does not have the right structure for this pattern.");
		} catch (ApplicationError e) {
			throw e;
		} catch (Exception e) {
			throw new ApplicationError(e, "Error while getting value for field " + fieldSelector);
		}
	}

	/**
	 * set value to json as per selector, creating object/array on the path if
	 * required. This is like creating a file with full path.
	 *
	 * @param fieldSelector
	 * @param json
	 * @param value
	 */
	public static void setValueWorker(String fieldSelector, Object json, Object value) {
		/*
		 * special case of root object itself
		 */
		if (fieldSelector.equals(".")) {
			if (value instanceof JSONObject == false || json instanceof JSONObject == false) {
				Tracer.trace("We expected a JSONObjects for source and destination, but got "
						+ json.getClass().getName() + " as object, and  "
						+ (value == null ? "null" : value.getClass().getName()) + " as value");
				return;
			}
			JSONObject objFrom = (JSONObject) value;
			JSONObject objTo = (JSONObject) json;
			for (String attName : objFrom.keySet()) {
				objTo.put(attName, objFrom.opt(attName));
			}
			return;
		}

		String attName = fieldSelector;
		Object leafObject = json;
		/*
		 * assume that the value is to be added as an attribute, not an element
		 * of array.
		 */
		int objIdx = -1;

		int idx = fieldSelector.lastIndexOf('.');
		if (idx != -1) {
			attName = fieldSelector.substring(idx + 1);
			String selector = fieldSelector.substring(0, idx);
			objIdx = parseIdx(attName);
			int option = objIdx == -1 ? 1 : 2;
			leafObject = getValueWorker(selector, json, option);
		}
		if (objIdx == -1) {
			((JSONObject) leafObject).put(attName, value);
		} else {
			((JSONArray) leafObject).put(objIdx, value);
		}
		return;
	}

	/**
	 * parse string into int, or return -1;
	 *
	 * @param str
	 * @return
	 */
	private static int parseIdx(String str) {
		char c = str.charAt(0);
		if (c >= '0' && c <= '9') {
			return Integer.parseInt(str);
		}
		return -1;
	}

	/**
	 * @param itemSelector
	 * @param json
	 * @return object as per selector. A new JSON Object is added and returned
	 *         if the json does not have a value as per selector, adding as many
	 *         object/array on the path if required
	 */
	public static Object getObjectValue(String itemSelector, JSONObject json) {
		return getValueWorker(itemSelector, json, 1);
	}

	/**
	 * @param itemSelector
	 * @param json
	 * @return object as per selector. A new JSON array is added and returned if
	 *         the json does not have a value as per selector, adding as many
	 *         object/array on the path if required
	 */
	public static Object getArrayValue(String itemSelector, JSONObject json) {
		return getValueWorker(itemSelector, json, 2);
	}

	/**
	 * copy all attributes from one josn to another. In case of attribute name
	 * clash, existing value is replaced
	 *
	 * @param toJson
	 * @param fromJson
	 * @return toJson for convenience
	 */
	public static JSONObject copyAll(JSONObject toJson, JSONObject fromJson) {
		for (String key : fromJson.keySet()) {
			toJson.put(key, fromJson.get(key));
		}
		return toJson;
	}

	/**
	 * look for internal $ref attributes and de-reference them inside the json
	 *
	 * @param json
	 *            a json object that may contain schema references within the
	 *            doc.
	 */
	public static void dereference(JSONObject json) {
		if (json == null) {
			return;
		}

		JSONObject defs = json.optJSONObject("definitions");
		if (defs == null) {
			Tracer.trace("No definitions in this json");
			return;
		}
		/*
		 * defs may contain refs. substitute them first
		 */
		replaceRefs(defs, defs, true);
		/*
		 * replace refs in rest of the api
		 */
		replaceRefs(json, defs, false);
	}

	private static final String REF = "$ref";
	private static final String REF_PREFIX = "#/definitions/";
	private static final int REF_PREFIX_LEN = REF_PREFIX.length();

	/**
	 * find internal references in json and de-reference them from definitions
	 *
	 * @param parentJson
	 *            that may have "$ref" keys
	 * @param definitions
	 *            object that has all the definitions for de-referencing
	 *
	 */
	private static void replaceRefs(JSONObject parentJson, JSONObject definitions, boolean forRefs) {
		if (parentJson.length() == 0) {
			return;
		}

		for (String key : parentJson.keySet()) {
			Object obj = parentJson.get(key);
			if (obj == null) {
				continue;
			}
			if (obj instanceof JSONObject) {
				Object sub = getSubstitution((JSONObject) obj, definitions, forRefs);
				if (sub != null) {
					parentJson.put(key, sub);
				}
				continue;
			}
			if (obj instanceof JSONArray) {
				replaceRefs((JSONArray) obj, definitions, forRefs);
			}
		}
	}

	/**
	 * find internal references and replace them with actual objects
	 *
	 * @param array
	 * @param definitions
	 * @param forRefs
	 *            if we are substituting within refs, we have to go recursively
	 *            inside defs
	 *
	 */
	private static void replaceRefs(JSONArray array, JSONObject definitions, boolean forRefs) {
		int nbr = array.length();
		for (int i = 0; i < nbr; i++) {
			Object obj = array.get(i);
			if (obj == null) {
				continue;
			}

			if (obj instanceof JSONObject) {
				Object sub = getSubstitution((JSONObject) obj, definitions, forRefs);
				if (sub != null) {
					array.put(i, sub);
				}
				continue;
			}

			if (obj instanceof JSONArray) {
				replaceRefs((JSONArray) obj, definitions, forRefs);
			}
		}
	}

	private static Object getSubstitution(JSONObject childJson, JSONObject definitions, boolean forRefs) {
		String ref = getRef(childJson);
		if (ref == null) {
			/*
			 * normal JSON. Recurse to inspect it further
			 */
			replaceRefs(childJson, definitions, forRefs);
			return null;
		}
		/*
		 * get the object to substitute
		 */
		Object subObj = definitions.opt(ref);
		if (subObj == null) {
			Tracer.trace("defintion for " + ref + " not found. reference replaced with an empty object");
			subObj = new JSONObject();
		} else if (forRefs) {
			/*
			 * we may have $refs within this as well!!
			 */
			if (subObj instanceof JSONObject) {
				replaceRefs((JSONObject) subObj, definitions, forRefs);
			} else if (subObj instanceof JSONObject) {
				replaceRefs((JSONArray) subObj, definitions, forRefs);
			}
		}
		Tracer.trace("Replacing " + ref);
		return subObj;
	}

	/**
	 * @param jsonObj
	 * @return attribute name to be referred to, if this is a ref-object. FOr
	 *         example if this object is {"$ref": "#/definitions/pets"} this
	 *         method returns "pets"
	 */
	private static String getRef(JSONObject jsonObj) {
		String ref = jsonObj.optString(REF, null);
		if (ref == null) {
			return null;
		}
		if (ref.indexOf(REF_PREFIX) != 0) {
			Tracer.trace("$ref is to be set to a value starting with " + REF_PREFIX);
			return null;
		}
		Tracer.trace("Found a ref entry for " + ref);
		return ref.substring(REF_PREFIX_LEN);
	}

}
