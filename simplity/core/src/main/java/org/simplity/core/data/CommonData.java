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
package org.simplity.core.data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.simplity.core.value.BooleanValue;
import org.simplity.core.value.DateValue;
import org.simplity.core.value.DecimalValue;
import org.simplity.core.value.IntegerValue;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;
import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;

/**
 * Default generic data structure that is created for implementing a service. We
 * would encourage sub-classing this rather than having this as an attribute.
 *
 * @author simplity.org
 */
public class CommonData implements ICommonData {
	/**
	 * field name for get/set may be for embedded/hierarchical data structure.
	 * like objectName/subObjectName/fieldName
	 */
	public static final String NAME_PART_SEPARATOR = "/";
	private static final char SEP_CHAR = '/';
	protected static final int NOT_APPLICABLE = -1;

	/**
	 * generally temp and work related fields that are created during an
	 * algorithm, and are not part of any record. These fields are addressed
	 * with just field name with no qualifier
	 */
	protected final Map<String, Value> allFields = new HashMap<String, Value>();

	/**
	 * data sheets. a given data sheet may be either a SingleRowSheet or
	 * DataSheet
	 */
	protected final Map<String, IDataSheet> allSheets = new HashMap<String, IDataSheet>();

	/**
	 * sheets that are currently being iterated. Iteration over single-row sheet
	 * is dummy, and is simulated, and hence is never put into this list
	 */
	protected final Map<String, SheetIterator> iteratedSheets = new HashMap<String, CommonData.SheetIterator>();

	/**
	 * in java-class intensive projects, objects like DAO, DTO are used for data
	 * So, let us carry them as well.
	 */
	protected final Map<String, Object> allObjects = new HashMap<String, Object>();

	@Override
	public final Value getValue(String fieldName) {
		if (fieldName == null) {
			return null;
		}
		if (fieldName.indexOf(SEP_CHAR) == -1) {
			return this.allFields.get(fieldName);
		}
		return this.getValeForPath(fieldName.split(NAME_PART_SEPARATOR));
	}

	private final Value getValeForPath(String[] parts) {
		String txt = parts[0];
		IDataSheet sheet = this.allSheets.get(txt);

		if (sheet != null) {
			int idx = 0;
			SheetIterator iter = this.iteratedSheets.get(txt);
			if (iter != null) {
				idx = iter.getIdx();
			}
			return sheet.getColumnValue(parts[1], idx);
		}

		Object obj = this.allObjects.get(txt);
		if (obj != null && obj instanceof JSONObject) {
			JSONObject json = this.getJSONFromPath((JSONObject) obj, parts);
			if (json != null) {
				obj = json.opt(parts[parts.length - 1]);
				if (obj instanceof Value) {
					return (Value) obj;
				}
				return Value.parseObject(obj);
			}
		}
		return null;
	}

	/**
	 * parts[0] is already utilized to identify this json. last part of parts[]
	 * is the field name. other parts, if any, indicate path to the leaf object
	 * that holds field values. our job is to return that leaf object.
	 *
	 * Any array on the path is treated as if it is its first element
	 *
	 * @param parentJson
	 * @param parts
	 *            ignore first and last part from this to locate the object
	 * @return leaf object, or null if it can not be located.
	 */
	private JSONObject getJSONFromPath(JSONObject parentJson, String[] parts) {
		int n = parts.length - 1;
		JSONObject json = parentJson;
		for (int i = 1; i < n; i++) {
			Object obj = json.opt(parts[i]);
			if (obj instanceof JSONArray) {
				obj = ((JSONArray) obj).opt(0);
			}
			if (obj instanceof JSONObject) {
				json = (JSONObject) obj;
			} else {
				return null;
			}
		}
		return json;
	}

	@Override
	public final void setValue(String fieldName, Value value) {
		if (fieldName == null) {
			return;
		}
		if (fieldName.indexOf(SEP_CHAR) == -1) {
			this.allFields.put(fieldName, value);
		}
		this.setValeForPath(fieldName.split(NAME_PART_SEPARATOR), value);
	}

	/**
	 * this being an internal method, we use some tricks..
	 *
	 * @param parts
	 *            parts[0] is used to locate the primary data source : sheet or
	 *            object. last part is field name and rest are actual path to
	 *            the object/sheet that is treated as leaf object.
	 * @param value
	 *            null if either the path does not lead to an object, or the
	 *            object does not contain the field
	 */
	private final void setValeForPath(String[] parts, Value value) {
		String txt = parts[0];

		IDataSheet sheet = this.allSheets.get(txt);
		if (sheet != null) {
			int idx = 0;
			SheetIterator iter = this.iteratedSheets.get(txt);
			if (iter != null) {
				idx = iter.getIdx();
			}
			sheet.setColumnValue(parts[1], idx, value);
			return;
		}

		Object obj = this.allObjects.get(txt);
		if (obj != null && obj instanceof JSONObject) {
			JSONObject json = this.getJSONFromPath((JSONObject) obj, parts);
			if (json != null) {
				json.put(parts[parts.length - 1], value);
			}
		}
	}

	@Override
	public final Value removeValue(String fieldName) {
		if (fieldName == null) {
			return null;
		}
		if (fieldName.indexOf(SEP_CHAR) == -1) {
			return this.allFields.remove(fieldName);
		}
		return this.removeValeForPath(fieldName.split(NAME_PART_SEPARATOR));
	}

	/**
	 * this being an internal method, we use some tricks..
	 *
	 * @param parts
	 *            parts[0] is used to locate the primary data source : sheet or
	 *            object. last part is field name and rest are actual path to
	 *            the object/sheet that is treated as leaf object.
	 * @param value
	 *            null if either the path does not lead to an object, or the
	 *            object does not contain the field
	 */
	private final Value removeValeForPath(String[] parts) {
		String objectName = parts[0];
		String fieldName = parts[parts.length - 1];
		IDataSheet sheet = this.allSheets.get(objectName);
		if (sheet != null) {
			int idx = 0;
			SheetIterator iter = this.iteratedSheets.get(objectName);
			if (iter != null) {
				idx = iter.getIdx();
			}

			Value val = sheet.getColumnValue(fieldName, idx);
			if (val != null) {
				sheet.setColumnValue(fieldName, idx, Value.newUnknownValue(val.getValueType()));
			}
			return val;
		}

		Object obj = this.allObjects.get(objectName);
		if (obj != null && obj instanceof JSONObject) {
			JSONObject json = this.getJSONFromPath((JSONObject) obj, parts);
			if (json != null) {
				obj = json.opt(fieldName);
				Value val = null;
				if (obj != null) {
					val = Value.parseObject(obj);
					json.remove(fieldName);
				}
				return val;
			}
		}
		return null;
	}

	@Override
	public final boolean hasValue(String fieldName) {
		if (fieldName == null) {
			return false;
		}
		return this.getValue(fieldName) != null;
	}

	@Override
	public final IDataSheet getDataSheet(String sheetName) {
		return this.allSheets.get(sheetName);
	}

	@Override
	public final void putDataSheet(String sheetName, IDataSheet sheet) {
		this.allSheets.put(sheetName, sheet);
	}

	@Override
	public final boolean hasDataSheet(String sheetName) {
		return this.allSheets.containsKey(sheetName);
	}

	@Override
	public final IDataSheet removeDataSheet(String sheetName) {
		return this.allSheets.remove(sheetName);
	}

	@Override
	/** iterator is non-null. This avoids for-loop null-pointer exceptions */
	public final IDataSheetIterator startIteration(String sheetName) throws AlreadyIteratingException {
		if (this.iteratedSheets.containsKey(sheetName)) {
			throw new AlreadyIteratingException();
		}
		IDataSheet sheet = this.getDataSheet(sheetName);
		int nbrRows = 0;
		if (sheet != null) {
			nbrRows = sheet.length();
		}

		if (nbrRows == 0 || sheet instanceof MultiRowsSheet == false) {
			return new SheetIterator(null, nbrRows);
		}
		/*
		 * we have to track this iterator
		 */
		SheetIterator iter = new SheetIterator(sheetName, nbrRows);
		this.iteratedSheets.put(sheetName, iter);
		return iter;
	}

	void endIteration(String sheetName) {
		if (sheetName != null) {
			this.iteratedSheets.remove(sheetName);
		}
	}

	protected class SheetIterator implements IDataSheetIterator {

		/** last index - zero based, that this sheet can go up to. */
		private int lastIdx;
		/**
		 * bit tricky, because we want to accommodate the state
		 * started-but-before -first-get-next. -1 is that state, but we return 0
		 * as current index
		 */
		private int currentIdx = -1;

		/**
		 * null means need not worry about. non-null means we have to inform
		 * parent when this iteration completes
		 */
		private final String sheetName;

		/**
		 * @param sheetName
		 * @param nbrRows
		 */
		SheetIterator(String sheetName, int nbrRows) {
			this.lastIdx = nbrRows - 1;
			this.sheetName = sheetName;
		}

		/**
		 * get current index. default is zero
		 *
		 * @return
		 */
		int getIdx() {
			if (this.currentIdx < 0) {
				return 0;
			}
			return this.currentIdx;
		}

		@Override
		public boolean hasNext() {
			return this.currentIdx < this.lastIdx;
		}

		@Override
		public boolean moveToNextRow() {
			if (this.currentIdx < this.lastIdx) {
				this.currentIdx++;
				return true;
			}
			if (this.currentIdx == this.lastIdx) {
				this.cancelIteration();
			}
			return false;
		}

		@Override
		public void cancelIteration() {
			this.lastIdx = -1;
			CommonData.this.endIteration(this.sheetName);
		}
	}

	SheetIterator getIterator(String sheetName) {
		return this.iteratedSheets.get(sheetName);
	}

	@Override
	public Set<Entry<String, Value>> getAllFields() {
		return this.allFields.entrySet();
	}

	@Override
	public Value[] getValues(String[] names) {
		Value[] values = new Value[names.length];
		int i = 0;
		for (String name : names) {
			values[i++] = this.getValue(name);
		}
		return values;
	}

	/**
	 * Way to pass an object to subsequent action
	 *
	 * @param dataName
	 *            name by which this is referred
	 * @param object
	 *            object being set to this name
	 */
	public void setObject(String dataName, Object object) {
		if (dataName == null) {
			return;
		}
		if (dataName.indexOf(SEP_CHAR) == -1) {
			this.allObjects.put(dataName, object);
			return;
		}
		String[] parts = dataName.split(NAME_PART_SEPARATOR);
		Object obj = this.allObjects.get(dataName);
		if (obj instanceof JSONObject == false) {
			return;
		}
		JSONObject json = this.getJSONFromPath((JSONObject) obj, parts);
		if (json == null) {
			return;
		}
		json.put(parts[parts.length - 1], object);
	}

	/**
	 * @param dataName
	 *            name by which this is referred
	 * @return get the named object, or null if the object does not exists
	 */
	public Object getObject(String dataName) {
		if (dataName == null) {
			return null;
		}
		if (dataName.indexOf(SEP_CHAR) == -1) {
			return this.allObjects.get(dataName);
		}
		String[] parts = dataName.split(NAME_PART_SEPARATOR);
		Object obj = this.allObjects.get(dataName);
		if (obj instanceof JSONObject == false) {
			return null;
		}
		JSONObject json = this.getJSONFromPath((JSONObject) obj, parts);
		if (json == null) {
			return null;
		}
		return json.opt(parts[parts.length - 1]);
	}

	/**
	 * @param dataName
	 *            name of the object to be removed
	 * @return object being removed, null if object was not found
	 */
	public Object removeObject(String dataName) {
		if (dataName == null) {
			return null;
		}
		if (dataName.indexOf(SEP_CHAR) == -1) {
			return this.allObjects.remove(dataName);
		}
		String[] parts = dataName.split(NAME_PART_SEPARATOR);
		Object obj = this.allObjects.get(dataName);
		if (obj instanceof JSONObject == false) {
			return null;
		}
		JSONObject json = this.getJSONFromPath((JSONObject) obj, parts);
		if (json == null) {
			return null;
		}
		return json.remove(parts[parts.length - 1]);
	}

	@Override
	public String getTextValue(String fieldName) {
		Value val = this.getValue(fieldName);
		if (Value.isNull(val)) {
			return null;
		}
		String str = val.toString();
		if (str.isEmpty()) {
			return null;
		}
		return str;
	}

	@Override
	public long getLongValue(String fieldName) {
		Value val = this.getValue(fieldName);
		if (Value.isNull(val)) {
			return 0;
		}
		ValueType vt = val.getValueType();
		if (vt == ValueType.INTEGER) {
			return ((IntegerValue) val).getLong();
		}
		if (vt == ValueType.DECIMAL) {
			return ((DecimalValue) val).getLong();
		}
		return 0;
	}

	@Override
	public Date getDateValue(String fieldName) {
		Value val = this.getValue(fieldName);
		if (Value.isNull(val)) {
			return null;
		}

		if (val.getValueType() == ValueType.DECIMAL) {
			return new Date(((DateValue) val).getDate());
		}
		return null;
	}

	@Override
	public boolean getBooleanValue(String fieldName) {
		Value val = this.getValue(fieldName);
		if (Value.isNull(val)) {
			return false;
		}
		if (val.getValueType() == ValueType.BOOLEAN) {
			return ((BooleanValue) val).getBoolean();
		}
		return false;
	}

	@Override
	public double getDoubleValue(String fieldName) {
		Value val = this.getValue(fieldName);
		if (Value.isNull(val)) {
			return 0;
		}
		ValueType vt = val.getValueType();
		if (vt == ValueType.DECIMAL) {
			return ((DecimalValue) val).getDouble();
		}
		if (vt == ValueType.INTEGER) {
			return ((IntegerValue) val).getDouble();
		}
		return 0;
	}

	@Override
	public void setTextValue(String fieldName, String value) {
		this.setValue(fieldName, Value.newTextValue(value));
	}

	@Override
	public void setLongValue(String fieldName, long value) {
		this.setValue(fieldName, Value.newIntegerValue(value));
	}

	@Override
	public void setDoubleValue(String fieldName, double value) {
		this.setValue(fieldName, Value.newDecimalValue(value));
	}

	@Override
	public void setDateValue(String fieldName, Date value) {
		this.setValue(fieldName, Value.newDateValue(value));
	}

	@Override
	public void setBooleanValue(String fieldName, boolean value) {
		this.setValue(fieldName, Value.newBooleanValue(value));
	}

	/** @return set of all sheets that you can iterate over */
	public Set<Map.Entry<String, IDataSheet>> getAllSheets() {
		return this.allSheets.entrySet();
	}

	/**
	 * @return set of all objects that you can iterate over
	 */
	public Set<Map.Entry<String, Object>> getAllObjects() {
		return this.allObjects.entrySet();
	}

}
