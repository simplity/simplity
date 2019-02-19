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

package org.simplity.core.util;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.simplity.core.ApplicationError;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.data.MultiRowsSheet;
import org.simplity.core.dm.field.Field;
import org.simplity.core.msg.FormattedMessage;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;
import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;
import org.simplity.json.JSONWriter;
import org.simplity.json.JsonWritable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * utilities that help Simplity deal with JSON
 *
 * @author simplity.org
 */
public class JsonUtil {
	private static final Logger logger = LoggerFactory.getLogger(JsonUtil.class);
	/**
	 * swagger tag for object type
	 */
	public static final String OBJECT_TYPE_ATTR = "x-object-type";

	/**
	 * create a data sheet based on json array. Value types are guessed based on
	 * values in the first row
	 *
	 * @param arr
	 * @param ctx
	 * @return data sheet with columns based on the first row of data, and value
	 *         type decided based on best guess
	 */
	public static IDataSheet getSheet(JSONArray arr, ServiceContext ctx) {
		if (arr == null || arr.length() == 0) {
			logger.info("JSONArray is null or has no data. Data Sheet not created.");
			return null;
		}
		Object obj = arr.opt(0);
		if (obj instanceof JSONObject) {
			return getSheet(arr, null, null, null, null, ctx);
		}
		if (obj instanceof JSONArray) {
			return getSheetFromTable(arr);
		}
		logger.info(
				"JSONArray has its first element as {}. Data sheet can be created only if the array contains either array of header/data or array of objects",
				obj.getClass().getName());
		return null;
	}

	/**
	 * @param arr
	 * @return data sheet by guessing value type based on data in the first data
	 *         row.null if there is no data row
	 */
	private static IDataSheet getSheetFromTable(JSONArray arr) {
		int nbrRows = arr.length();
		if (nbrRows <= 1) {
			logger.info("JSONArray with header/data row format has only header, and no data. Data sheet not created");
			return null;
		}
		Field[] fields = getFields(arr.getJSONArray(0), arr.getJSONArray(1));
		int nbrCols = fields.length;
		IDataSheet sheet = new MultiRowsSheet(fields);
		for (int i = 1; i < nbrRows; i++) {
			JSONArray row = arr.getJSONArray(i);
			Value[] values = new Value[nbrCols];
			for (int j = 0; j < nbrCols; j++) {
				values[j] = Value.parseValue(row.getString(j), fields[j].getValueType());
			}
			sheet.addRow(values);
		}
		return sheet;
	}

	/**
	 * create a data sheet out of a well-formed json array of simple jsonObject.
	 *
	 * @param arr
	 *            that has the json array
	 * @param inputFields
	 *            Fields to be input. null if we are to take whatever is offered
	 * @param errors
	 *            to which any validation errors are added
	 * @param parentFieldName
	 *            if this is a child sheet, specify the column name in this
	 *            sheet that should be populated with the parent key value
	 * @param parentValue
	 *            if this is a child sheet, and you have specified
	 *            parentFieldName, value to be populated in each row for that
	 *            column
	 * @param ctx
	 * @return data sheet. Null if no data found or the json is not well
	 *         formated. was null. case the array is not well-formed
	 */
	public static IDataSheet getSheet(JSONArray arr, Field[] inputFields, List<FormattedMessage> errors,
			String parentFieldName, Value parentValue, ServiceContext ctx) {
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

				logger.info("Json array has its first object as null, and hence we abandoned parsing it.");

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

				logger.info("Parent field name " + parentFieldName
						+ " not found in the fields list for child. Filed will not be populated from parent sheet.");
			}
		}
		IDataSheet ds = new MultiRowsSheet(fields);
		int nbrRows = arr.length();
		/*
		 * let us now extract each row into data sheet
		 */
		for (int i = 0; i < nbrRows; i++) {
			JSONObject obj = arr.optJSONObject(i);
			if (obj == null) {

				logger.info("Row " + (i + 1) + " is null. Not extracted");

				continue;
			}
			int j = 0;
			Value[] row = new Value[fields.length];
			for (Field field : fields) {
				Object val = obj.opt(field.getName());
				if (j == parentIdx) {
					row[j] = parentValue;
				} else {
					row[j] = field.parseObject(val, null, ctx);
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
	 * @param ctx
	 * @return data sheet. Null if no data found. Throws ApplicationError on
	 *         case the array is not well-formed
	 */
	public static IDataSheet getChildSheet(JSONArray arr, String attName, Field[] fields, ServiceContext ctx) {
		/*
		 * arr corresponds to following json. We are to accumulate child rows
		 * across all main rows
		 *
		 * [...,"attName"=[{},{}....],..],[....,"attName"=[{},{}.... ],..]....
		 */
		Field[] inputFields = fields;
		IDataSheet ds = null;
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
					row[j] = field.parseObject(val, null, ctx);
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
	 * @param outputAsObject
	 *            if the data sheet is meant for an object/data structure and
	 *            not an array of them. Only first row is used
	 */
	public static void sheetToJson(JSONWriter writer, IDataSheet ds, boolean outputAsObject) {
		int nbrRows = 0;
		int nbrCols = 0;
		if (ds != null) {
			nbrRows = ds.length();
			nbrCols = ds.width();
		}
		if (ds == null || nbrRows == 0 || nbrCols == 0) {
			writer.value(null);

			logger.info("Sheet  has no data. json is not added");

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
			writer.endObject();
		}
		if (outputAsObject == false) {
			writer.endArray();
		}
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
	 * create a data sheet for attributes in this object
	 *
	 * @param header
	 *            row that has names of columns
	 * @param data
	 *            row of data
	 * @return array of fields in this table, based on best-guess on values
	 *         found in data row
	 */
	public static Field[] getFields(JSONArray header, JSONArray data) {
		int nbrCols = header.length();
		Field[] fields = new Field[nbrCols];
		for (int i = 0; i < nbrCols; i++) {
			ValueType vt = Value.parseObject(data.opt(i)).getValueType();
			fields[i] = Field.getDefaultField(header.getString(i), vt);
		}
		return fields;
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
			Object val = json.opt(key);
			if (val == null) {
				logger.info("{} is null. Skipped.", key);
				continue;
			}
			if (val instanceof JSONArray) {
				JSONArray arr = (JSONArray) val;
				if (arr.length() == 0) {
					logger.info("Table {} has no data. Skipped", key);
					continue;
				}
				Object obj = arr.opt(0);
				if (obj instanceof JSONArray) {
					IDataSheet sheet = JsonUtil.getSheet(arr, ctx);
					if (sheet == null) {
						logger.info("Table {} could not be extracted", key);
						continue;
					}
					if (key.equals("_messages")) {
						addMessages(sheet, ctx);
						continue;
					}
					ctx.putDataSheet(key, sheet);
					logger.info("Table {} extracted with {} rows", key, sheet.length());
					continue;
				}
				ctx.setObject(key, val);
				logger.info("{} saved as json array", key);
				continue;
			}
			if (val instanceof JSONObject) {
				/*
				 * we do not have a standard for converting data structure. As
				 * of now, we just copy this json
				 */
				ctx.setObject(key, val);
				logger.info("{} retained as a JSON into ctx", key);
				continue;
			}
			Value value = Value.parseObject(val);
			if (value == null) {
				ctx.setValue(key, value);
				logger.info("{} ={} extracted ", key, value);
			} else {
				logger.info("{} ={} NOT extracted ", key, value);
			}
		}
	}

	/**
	 * @param sheet
	 * @param ctx
	 */
	private static void addMessages(IDataSheet sheet, ServiceContext ctx) {
		if (sheet.width() != 2) {
			logger.error("_messages sheet has to have two columns. Data ignored.");
			return;
		}
		for (Value[] row : sheet.getAllRows()) {
			Value val = row[1];
			String[] params = null;
			if (Value.isNull(val) == false) {
				params = val.toText().split(",");
			}
			ctx.addMessage(row[0].toString(), params);
			logger.info("Message {} added to context", row[0]);
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
		if (obj instanceof JsonWritable) {
			((JsonWritable) obj).writeJsonValue(writer);
			return;
		}
		if (obj instanceof Value) {
			Value value = (Value) obj;
			value.writeJsonValue(writer);
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

				logger.info("Unable to get value for object attribute " + entry.getKey() + ". null assumed");

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

				logger.info("Element no (zero based) " + i
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

			logger.info("Null/empty selector for get/setValue");
			return json;
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
	public static void setValue(String fieldSelector, Object json, Object value) {
		/*
		 * special case of root object itself
		 */
		if (fieldSelector.equals(".")) {
			if (value instanceof JSONObject == false || json instanceof JSONObject == false) {

				logger.info("We expected a JSONObjects for source and destination, but got " + json.getClass().getName()
						+ " as object, and  " + (value == null ? "null" : value.getClass().getName()) + " as value");

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

	/*
	 * new set of APIs for data adapters. They all have *Child* in their name
	 */

	/**
	 * set a value to a node, possibly down the hierarchy. json objects may be
	 * created in-between to create the desired path
	 *
	 * @param root
	 *            non-null
	 * @param path
	 *            non-null
	 * @param fieldValue
	 * @return always true. kept it for nay possible complications in the future
	 */
	public static boolean setChildValue(JSONObject root, String path, Object fieldValue) {
		LeafObject lo = getLeaf(root, path, true);
		lo.parent.put(lo.fieldName, fieldValue);
		return true;
	}

	/**
	 * get a value of a node, possibly down the hierarchy
	 *
	 * @param root
	 *            non-null
	 * @param path
	 *            non-null
	 * @return value, or null in case the value is null, or the path cannot be
	 *         traversed
	 */
	public static Object getChildValue(JSONObject root, String path) {
		LeafObject lo = getLeaf(root, path, false);
		if (lo == null) {
			return null;
		}
		return lo.parent.opt(lo.fieldName);
	}

	/**
	 * get a leaf based on path of type a.b.c. Any list on the path is assumed
	 * as its first/sole member. any missing member is created. if a member if
	 * non-json, it is ruthlessly replaced!!!
	 *
	 * @param root
	 *            non-null
	 * @param path
	 *            possibly of the form a.b.c
	 * @param createIfRequired
	 *            if this is true, we will create a JSON object child if
	 *            required to continue down the path. if false, we return null
	 *            in case of non-josn member
	 * @return null if createIfRequired is false, and we encounter non-josn
	 *         member in between
	 */
	public static LeafObject getLeaf(JSONObject root, String path, boolean createIfRequired) {
		int idx = path.lastIndexOf('.');
		if (idx == -1) {
			return new LeafObject(root, path);
		}
		String leafName = path.substring(idx + 1);
		String p = path.substring(0, idx);
		JSONObject parent = root;
		for (String fn : p.split("\\.")) {
			Object value = parent.opt(fn);
			JSONObject child = null;
			if (value instanceof JSONObject) {
				child = (JSONObject) value;
			} else if (value instanceof JSONArray) {
				JSONArray arr = (JSONArray) value;
				value = arr.opt(0);
				if (value instanceof JSONObject) {
					child = (JSONObject) value;
				} else if (createIfRequired) {
					child = new JSONObject();
					arr.put(0, child);
				} else {
					return null;
				}
			} else if (createIfRequired) {
				child = new JSONObject();
				parent.put(fn, child);
			} else {
				return null;
			}
			parent = child;
		}
		return new LeafObject(parent, leafName);
	}

	/**
	 * object details for a leaf-value
	 *
	 * @author simplity.org
	 *
	 */
	public static class LeafObject {
		/**
		 * direct parent of the leaf member
		 */
		public final JSONObject parent;
		/**
		 * simple field name of the leaf relative to its direct parent
		 */
		public final String fieldName;

		/**
		 * constructor with all data members
		 *
		 * @param parent
		 * @param fieldName
		 */
		public LeafObject(JSONObject parent, String fieldName) {
			this.parent = parent;
			this.fieldName = fieldName;
		}
	}
}