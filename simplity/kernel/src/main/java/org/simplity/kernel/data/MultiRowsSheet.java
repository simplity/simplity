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
package org.simplity.kernel.data;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.dm.Field;
import org.simplity.kernel.util.ArrayUtil;
import org.simplity.kernel.util.ReflectUtil;
import org.simplity.kernel.value.InvalidValueException;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;

/**
 * a sheet with possibly more than one rows.
 *
 * @author simplity.org
 *
 */
public class MultiRowsSheet implements DataSheet {

	/**
	 * data is organized as a list (rows) of column values(array). This is
	 * because we expect rows to be added dynamically, and not columns. Of
	 * course, user can add columns as well as rows, but adding rows is expected
	 * to be quite frequent, and adding column is more of an exception
	 *
	 */
	private List<Value[]> data = new ArrayList<Value[]>();

	private String[] columnNames;
	private ValueType[] columnValueTypes;
	/**
	 * we expect calls to getColoumnValue etc.. Should we lazy-populate this?
	 */
	private Map<String, Integer> columnIndexes = new HashMap<String, Integer>();
	private int[] columnWidths;

	/**
	 * create a data sheet from raw data with columns parsed as per types
	 *
	 * @param rawData
	 *            rows of text data, with a header row for column names
	 * @param valueTypes
	 *            value type of columns in that order
	 *
	 */
	public MultiRowsSheet(String[][] rawData, ValueType[] valueTypes) {
		this.columnValueTypes = valueTypes;
		this.setColumnNames(rawData[0]);
		for (int i = 1; i < rawData.length; i++) {
			this.data.add(this.textToRow(rawData[i]));
		}
	}

	/**
	 * create a data sheet with all text data using the raw data
	 *
	 * @param rawData
	 *            rows of text data, with a header row for column names. All
	 *            columns are treated as text.
	 *
	 */
	public MultiRowsSheet(String[][] rawData) {
		String[] names = rawData[0];
		ValueType[] types = new ValueType[names.length];
		for (int i = 0; i < types.length; i++) {
			types[i] = ValueType.TEXT;
		}
		this.columnValueTypes = types;
		this.setColumnNames(names);
		for (int i = 1; i < rawData.length; i++) {
			this.data.add(this.textToRow(rawData[i]));
		}
	}

	/**
	 * create a data sheet with all text data using header row and data rows
	 *
	 * @param names
	 *            columnNames
	 * @param dataRows
	 *            rows of text data, with a header row for column names. All
	 *            columns are treated as text.
	 *
	 */
	public MultiRowsSheet(String[] names, String[][] dataRows) {
		ValueType[] types = new ValueType[names.length];
		for (int i = 0; i < types.length; i++) {
			types[i] = ValueType.TEXT;
		}
		this.columnValueTypes = types;
		this.setColumnNames(names);
		for (String[] row : dataRows) {
			this.data.add(this.textToRow(row));
		}
	}

	/**
	 * create a table with structure but no data.
	 *
	 * @param columnNames
	 * @param columnValueTypes
	 */
	public MultiRowsSheet(String[] columnNames, ValueType[] columnValueTypes) {
		this.setColumnNames(columnNames);
		this.columnValueTypes = columnValueTypes;
	}

	/**
	 * create table with data
	 *
	 * @param columnNames
	 * @param data
	 */
	public MultiRowsSheet(String[] columnNames, List<Value[]> data) {
		this.setColumnNames(columnNames);
		this.data = data;
		this.columnValueTypes = this.extractValueTypes(data.get(0));
	}

	/**
	 * Most of the time, dynamic sheet is created internally and are guaranteed
	 * to be ok. We avoid the over-head, especially when this may have several
	 * rows. Any caller who suspects that the rows may not be in order should
	 * call this method after creating the sheet. check whether rows have same
	 * number of columns, and cells across a column are of same type
	 */
	public void validate() {
		if (this.columnIndexes.size() != this.columnNames.length) {
			Set<String> set = new HashSet<String>();
			for (String nam : this.columnNames) {
				if (set.add(nam) == false) {
					throw new ApplicationError(nam + " is a duplicate column in data sheet.");
				}
			}
		}
		int n = this.columnNames.length;
		for (Value[] row : this.data) {
			if (row.length != n) {
				throw new ApplicationError("Data sheet is to have same number of columns across all rows. found "
						+ row.length + " columns in a row when header has " + n + " columns.");
			}
			int i = 0;
			for (ValueType vt : this.columnValueTypes) {
				if (row[i].getValueType() != vt) {
					throw new ApplicationError(
							"Each column in data sheet is to have the same value type across all rows. Issue with column(0 based) "
									+ i);
				}
				i++;
			}
		}
	}

	/**
	 * @param columnNames
	 * @param columnValues
	 *            is Value[nbrCols][nbrRows] This order of values is very
	 *            important
	 */
	public MultiRowsSheet(String[] columnNames, Value[][] columnValues) {
		int nbrColumns = columnNames.length;
		if (columnValues.length != nbrColumns) {
			throw new ApplicationError("Data sheet can not be created because " + columnValues.length
					+ " column values are supplied for " + nbrColumns + " columns.");
		}
		this.columnNames = columnNames;
		this.columnValueTypes = new ValueType[nbrColumns];
		int nbrRows = columnValues[0].length;

		/*
		 * add data. Remember that ColumnValues is ordered first by column, and
		 * then by row
		 */
		for (int row = 0; row < nbrRows; row++) {
			Value[] colVals = new Value[nbrColumns];
			for (int col = 0; col < nbrColumns; col++) {
				colVals[col] = columnValues[col][row];
			}
			this.data.add(colVals);
		}

		/*
		 * create columns and related data structure
		 */
		for (int i = 0; i < nbrColumns; i++) {
			Value[] vals = columnValues[i];
			if (vals.length != nbrRows) {
				throw new ApplicationError("Data sheet can not be created because column " + columnNames[i] + " has "
						+ vals.length + " while the first column had " + nbrRows + " rows.");
			}
			this.columnIndexes.put(columnNames[i], new Integer(i));
			this.columnValueTypes[i] = vals[0].getValueType();
		}
	}

	/**
	 * @param fields
	 *            to be used as columns for the data sheet
	 */
	public MultiRowsSheet(Field[] fields) {
		int n = fields.length;
		this.columnNames = new String[n];
		this.columnValueTypes = new ValueType[n];
		n = 0;
		for (Field field : fields) {
			String fieldName = field.getName();
			this.columnNames[n] = fieldName;
			this.columnValueTypes[n] = field.getValueType();
			this.columnIndexes.put(fieldName, new Integer(n));
			n++;
		}
		/*
		 * do we have field widths?. we did not put this inside the loop with an
		 * if becuase this is a very very very rare case.
		 */
		if (fields[0].getFieldWidth() != 0) {
			this.columnWidths = new int[fields.length];
			n = 0;
			for (Field field : fields) {
				this.columnWidths[n] = field.getFieldWidth();
				n++;
			}
		}
	}

	@Override
	public String[][] getRawData() {
		String[][] rawData = new String[this.data.size() + 1][];
		rawData[0] = this.columnNames;
		int i = 1;
		for (Value[] row : this.data) {
			rawData[i] = this.rowToText(row);
			i++;
		}
		return rawData;
	}

	@Override
	public int length() {
		return this.data.size();
	}

	@Override
	public int width() {
		return this.columnNames.length;
	}

	@Override
	public String[] getColumnNames() {
		return this.columnNames;
	}

	@Override
	public ValueType[] getValueTypes() {
		return this.columnValueTypes;
	}

	@Override
	public Value[] getRow(int zeroBasedRowNumber) {
		return this.data.get(zeroBasedRowNumber);
	}

	@Override
	public List<Value[]> getAllRows() {
		return this.data;
	}

	@Override
	public Value getColumnValue(String columnName, int zeroBasedRowNumber) {
		try {
			int idx = this.columnIndexes.get(columnName).intValue();
			return this.data.get(zeroBasedRowNumber)[idx];
		} catch (Exception e) {
			Tracer.trace("Request to get value for column" + columnName + " and index " + zeroBasedRowNumber
					+ " is not valid. going to return null");
			return null;
		}
	}

	@Override
	public void setColumnValue(String columnName, int zeroBasedRowNumber, Value value) {
		try {
			int idx = this.columnIndexes.get(columnName).intValue();
			this.data.get(zeroBasedRowNumber)[idx] = value;
		} catch (Exception e) {
			throw new ApplicationError(e, "Request to set value  for column" + columnName + " and index "
					+ zeroBasedRowNumber + " is not valid.");
		}
	}

	/**
	 * convert a row of values to row of text
	 *
	 * @param row
	 * @return
	 */
	private String[] rowToText(Value[] row) {
		String[] texts = new String[row.length];
		int i = 0;
		for (Value value : row) {
			texts[i] = value == null ? "" : value.toString();
			i++;
		}
		return texts;
	}

	/**
	 * parse text row into a row of values
	 *
	 * @param texts
	 * @return values
	 */
	private Value[] textToRow(String[] texts) {
		Value[] values = new Value[texts.length];
		int i = 0;
		for (String text : texts) {
			values[i] = Value.parseValue(text, this.columnValueTypes[i]);
			i++;
		}
		return values;
	}

	/**
	 * set value types based on values in first row
	 */
	private ValueType[] extractValueTypes(Value[] row) {
		ValueType[] valueTypes = new ValueType[row.length];
		int i = 0;
		for (Value value : row) {
			valueTypes[i] = value.getValueType();
			i++;
		}
		return valueTypes;
	}

	/**
	 * add column names and create columnIndexes
	 */
	private void setColumnNames(String[] columnNames) {
		this.columnNames = columnNames;
		int i = 0;
		for (String columnName : this.columnNames) {
			if (this.columnIndexes.containsKey(columnName)) {
				throw new ApplicationError("Column names can not be duplicate in a data sheet.");
			}
			this.columnIndexes.put(columnName, new Integer(i));
			i++;
		}
	}

	@Override
	public Iterator<FieldsInterface> iterator() {
		return new DataRows(this);
	}

	@Override
	public Value getValue(String fieldName) {
		return this.getColumnValue(fieldName, 0);
	}

	@Override
	public void setValue(String fieldName, Value value) {
		this.setColumnValue(fieldName, 0, value);
	}

	@Override
	public boolean hasValue(String fieldName) {
		return this.getColumnValue(fieldName, 0) != null;
	}

	/**
	 * we do not remove column values
	 */
	@Override
	public Value removeValue(String fieldName) {
		return null;
	}

	@Override
	public void addRow(Value[] row) {
		this.data.add(row);
	}

	@Override
	public Value[] getColumnValues(String columnName) {
		int nbr = this.data.size();
		Value[] values = new Value[nbr];
		if (nbr == 0) {
			Tracer.trace("getColumn values is returning empty array as there are no data");
			return values;
		}
		Integer n = this.columnIndexes.get(columnName);
		if (n == null) {
			Tracer.trace(columnName
					+ " is not a column in the sheet and hence null values are returned for getColumnValues()");
			return null;
		}

		int idx = n.intValue();
		nbr = 0;
		for (Value[] row : this.data) {
			values[nbr] = row[idx];
			nbr++;
		}
		return values;
	}

	@Override
	public void addColumn(String columnName, ValueType valueType, Value[] values) {
		int nbr = this.length();
		Value[] columnValues = null;
		if (values == null) {
			columnValues = new Value[nbr];
		} else {
			if (values.length != nbr) {
				throw new ApplicationError("column " + columnName + " is being added with " + values.length
						+ " values but the sheet has " + nbr + " rows.");
			}
			columnValues = values;
		}
		Integer key = this.columnIndexes.get(columnName);
		if (key == null) {
			/*
			 * new column
			 */
			this.extend(columnName, valueType, columnValues);
			return;
		}
		int idx = key.intValue();
		this.columnValueTypes[idx] = valueType;
		int i = 0;
		for (Value[] row : this.data) {
			row[idx] = columnValues[i++];
		}
	}

	/**
	 * @param columnName
	 * @param values
	 */
	private void extend(String columnName, ValueType valueType, Value[] values) {
		int idx = this.columnNames.length;
		this.columnIndexes.put(columnName, new Integer(idx));
		this.columnNames = ArrayUtil.extend(this.columnNames, columnName);
		this.columnValueTypes = ArrayUtil.extend(this.columnValueTypes, valueType);
		int nbr = this.data.size();
		for (int i = 0; i < nbr; i++) {
			this.data.set(i, ArrayUtil.extend(this.data.get(i), values[i]));
		}
	}

	@Override
	public Set<Entry<String, Value>> getAllFields() {
		return this.getAllFields(0);
	}

	@Override
	public Set<Entry<String, Value>> getAllFields(int rowIdx) {
		if (rowIdx > this.data.size()) {
			throw new ApplicationError("A request is received to fetch a non-existing row in a data sheet");
		}
		Map<String, Value> fields = new HashMap<String, Value>(this.columnNames.length);
		int i = 0;
		Value[] row = this.data.get(rowIdx);
		for (String fieldName : this.columnNames) {
			fields.put(fieldName, row[i++]);
		}
		return fields.entrySet();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.data.DataSheet#trace()
	 */
	@Override
	public void trace() {
		Tracer.trace(this.toString("\t", "\n"));
	}

	/**
	 *
	 * @param fieldSep
	 * @param rowSep
	 * @return a printable string for data in this sheet
	 */
	public String toString(String fieldSep, String rowSep) {
		StringBuilder sbf = new StringBuilder();
		for (String nam : this.columnNames) {
			sbf.append(nam).append(fieldSep);
		}
		for (Value[] row : this.data) {
			sbf.append(rowSep);
			for (Value val : row) {
				sbf.append(val).append(fieldSep);
			}
		}
		return sbf.toString();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.kernel.data.DataSheet#appendRows(org.simplity.kernel.data.
	 * DataSheet)
	 */
	@Override
	public int appendRows(DataSheet sheet) {
		if (sheet == null) {
			return 0;
		}
		int n = sheet.length();
		if (n == 0) {
			return 0;
		}
		ValueType[] fromTypes = sheet.getValueTypes();
		ValueType[] toTypes = sheet.getValueTypes();
		int nbrCols = fromTypes.length;
		if (nbrCols != toTypes.length) {
			throw new ApplicationError("AppendRows is not possible because from sheet has " + nbrCols
					+ " columns while to sheet has " + toTypes.length + " columns");
		}
		int idx = 0;
		for (ValueType toType : toTypes) {
			if (toType.equals(fromTypes[idx]) == false) {
				throw new ApplicationError("AppendRows is not possible because from and to sheets have " + nbrCols
						+ " columns each, but column " + (++idx) + " are of different type.");
			}
			idx++;
		}
		for (int i = 0; i < n; i++) {
			this.data.add(sheet.getRow(i));
		}
		return n;
	}

	@Override
	public void addColumn(String columnName, Value value) {
		int n = this.length();
		Value[] values = new Value[n];
		for (int i = 0; i < n; i++) {
			values[i] = value;
		}
		this.addColumn(columnName, value.getValueType(), values);
	}

	@Override
	public int getColIdx(String columnName) {
		int i = 0;
		for (String colName : this.columnNames) {
			if (colName.equals(columnName)) {
				return i;
			}
			i++;
		}
		Tracer.trace("We did not find column " + columnName + " in this multi-row sheet");
		return -1;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.kernel.data.DataSheet#toSerializedText(org.simplity.kernel.
	 * data.DataSerializationType)
	 */
	@Override
	public String toSerializedText(DataSerializationType serializationType) {
		throw new ApplicationError("Sorry, serialization is not yet implemented for Dynamic sheet");
		// TODO to be built
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.kernel.data.DataSheet#fromSerializedText(java.lang.String,
	 * org.simplity.kernel.data.DataSerializationType, boolean)
	 */
	@Override
	public void fromSerializedText(String text, DataSerializationType serializationType, boolean replaceExistingRows) {
		throw new ApplicationError("Sorry, de-serialization is not yet implemented for Dynamic sheet");
		// TODO to be built
	}

	/**
	 * if this is used for serialization into fixed-width test, we need this
	 *
	 * @param widths
	 */
	public void setWidths(int[] widths) {
		if (widths.length != this.width()) {
			throw new ApplicationError("Design error : data sheet has " + this.width() + " columns but " + widths.length
					+ " values are supplied for width.");
		}
		this.columnWidths = widths;
	}

	/**
	 * @return widths of columns in case this used for fixed-width formatting
	 */
	public int[] getWidths() {
		return this.columnWidths;
	}

	/**
	 * 
	 * @param <T>
	 * @param sheet
	 *            sheet
	 * @param columnName
	 *            columnName
	 * @return array of column values of primitive data type
	 */
	@SuppressWarnings("unchecked")
	public <T> T[] columnAsArray(String columnName, T[] array) {
		Value[] columnValues = this.getColumnValues(columnName);
		Class<?> genericType = array.getClass().getComponentType();
		array = (T[]) Array.newInstance(genericType, columnValues.length);
		for (int i = 0; i < columnValues.length; i++) {
			Value value = columnValues[i];
			try {
				if (genericType.equals(Integer.class)) {
					array[i] = (T) new Integer((int) value.toInteger());
					continue;
				}
				if (genericType.equals(Float.class)) {
					array[i] = (T) new Float((float) value.toDecimal());
					continue;
				}
			} catch (InvalidValueException e) {
				throw new ApplicationError(e.getMessage());
			}
			array[i] = (T) value.toObject();
		}
		return array;
	}

	/**
	 * 
	 * @param <T>
	 * @param sheet
	 *            sheet
	 * @param columnName
	 *            columnName
	 * @return list of column values
	 */
	@SuppressWarnings("unchecked")
	public <T> Collection<T> columnAsCollection(String columnName, Collection<T> c, Class<?> T) {
		Value[] columnValues = this.getColumnValues(columnName);
		for (Value value : columnValues) {
			if (value.getValueType() == ValueType.INTEGER) {
				if (T == Integer.class) {
					c.add((T) (Integer)((Long)value.toObject()).intValue());
					continue;
				}
				if (T == Float.class) {
					c.add((T) (Float) ((Double)value.toObject()).floatValue());
					continue;
				}
			}
			c.add((T) value.toObject());
		}
		return c;
	}

	/**
	 * 
	 * @param sheet
	 * @param keyColumnName
	 * @param valueColumnName
	 * @return 2 column values as a map with 1 column data as keys and other
	 *         column data as values
	 */
	@SuppressWarnings("unchecked")
	public <K, V> Map<K, V> columnsAsMap(String keyColumnName, String valueColumnName, Map<K, V> map,Class<?> keyType,Class<?> valueType) {
		Value[] keys = this.getColumnValues(keyColumnName);
		Value[] values = this.getColumnValues(valueColumnName);
		for (int i = 0; i < keys.length; i++) {
			K key = null;
			V value = null;
			try {
				if (keyType.equals(Integer.class)) {
					key = (K) new Integer((int) keys[i].toInteger());
				} else if (keyType.equals(Float.class)) {
					key = (K) new Float((float) keys[i].toDecimal());
				} else if (valueType.equals(Integer.class)) {
					value = (V) new Integer((int) values[i].toInteger());
				} else if (valueType.equals(Float.class)) {
					value = (V) new Float((float) values[i].toDecimal());
				} else {
					key = (K) keys[i].toObject();
					value = (V) values[i].toObject();
				}
			} catch (InvalidValueException e) {
				throw new ApplicationError(e.getMessage());
			}
			map.put(key, value);
		}
		return map;
	}

	/**
	 * 
	 * @param <T>
	 * @param sheet
	 * @param className
	 *            fully qualified class name
	 * @return List of entity objects
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> toList(List<T> listObject, Class<T> clazz) {
		List<Value[]> rowList = this.getAllRows();
		for (Value[] row : rowList) {
			T obj = (T) rowToObject(clazz, row);
			listObject.add(obj);
		}
		return listObject;
	}

	/**
	 * 
	 * @param sheet
	 * @param className
	 *            fully qualified class name
	 * @return Set of entity objects
	 */
	@SuppressWarnings("unchecked")
	public <T> Set<T> toSet(Set<T> setObject, Class<T> clazz) {
		List<Value[]> rowList = this.getAllRows();
		for (Value[] row : rowList) {
			T obj = (T) rowToObject(clazz, row);
			setObject.add(obj);
		}
		return setObject;
	}

	/**
	 * 
	 * @param arr
	 * @param columnName
	 * @return MultiRowsSheet
	 */
	public static <T> MultiRowsSheet toDatasheet(T[] arr) {
		if (arr != null && arr.length > 0) {
			Class<?> cls = arr[0].getClass();
			if (cls.isPrimitive() || cls.getName().startsWith("java.lang") || cls.equals(String.class)
					|| cls.equals(Date.class) || cls.equals(Timestamp.class)) {
				String[] header = { "array" };
				ValueType[] valueTypes = { getType(cls) };
				MultiRowsSheet sheet = new MultiRowsSheet(header, valueTypes);
				for (T value : arr) {
					Value[] valarray = new Value[1];
					valarray[0] = Value.parseObject(value);
					sheet.addRow(valarray);
				}
				return sheet;
			}
			java.lang.reflect.Field[] fields = cls.getDeclaredFields();
			String[] header = new String[fields.length];
			ValueType[] valueTypes = new ValueType[fields.length];
			int i = 0;
			for (java.lang.reflect.Field field : fields) {
				header[i] = field.getName();
				valueTypes[i] = getType(field.getType());
				i++;
			}
			MultiRowsSheet sheet = new MultiRowsSheet(header, valueTypes);
			for (Object obj : arr) {
				Value[] valarray = objectToValueArray(obj, fields);
				sheet.addRow(valarray);
			}
			return sheet;
		}
		return null;
	}

	/**
	 * 
	 * @param <T>
	 * @param list
	 * @param columnName
	 * @return MultiRowsSheet
	 */
	@SuppressWarnings("unchecked")
	public static <T> MultiRowsSheet toDatasheet(Collection<T> c) {
		Iterator<T> iterator = c.iterator();
		Class<?> clazz = null;
		while (iterator.hasNext()) {
			clazz = iterator.next().getClass();
			break;
		}
		T[] array = (T[]) Array.newInstance(clazz, c.size());
		array = c.toArray(array);
		return toDatasheet(array);
	}

	/**
	 * writes map data to a MultiRowsSheet
	 * 
	 * @param map
	 *            map
	 * @param transpose
	 *            If transpose = true, the output data sheet will be a
	 *            SingleRowDataSheet with keys as the column and values as the
	 *            row. If transpose = false, the output data sheet will be
	 *            MultiRowDatasheet with columns as "Key","Value" and
	 *            corresponding rows.
	 * @return DataSheet
	 */
	public static <K, V> DataSheet toDatasheet(Map<K, V> map, boolean transpose) {
		if (!map.isEmpty() && map != null) {
			if (transpose) {
				String[] columnNames = new String[map.size()];
				ValueType[] valueTypes = new ValueType[map.size()];
				Value[] row = new Value[map.size()];
				int i = 0;
				for (K key : map.keySet()) {
					columnNames[i] = key.toString();
					valueTypes[i] = getType(map.get(key).getClass());
					row[i] = Value.parseObject(map.get(key));
					i++;
				}
				SingleRowSheet singleRowSheet = new SingleRowSheet(columnNames, valueTypes);
				singleRowSheet.addRow(row);
				return singleRowSheet;
			}
			String[] columnNames = { "key", "value" };
			ValueType[] valueTypes = new ValueType[2];
			for (Object key : map.keySet()) {
				valueTypes[0] = getType(key.getClass());
				valueTypes[1] = getType(map.get(key).getClass());
				break;
			}

			MultiRowsSheet multirowsSheet = new MultiRowsSheet(columnNames, valueTypes);
			for (Object key : map.keySet()) {
				Value[] row = new Value[2];
				row[0] = Value.parseObject(key);
				row[1] = Value.parseObject(map.get(key));
				multirowsSheet.addRow(row);
			}
			return multirowsSheet;
		}
		return null;
	}

	private static ValueType getType(Class<?> type) {
		if (type.equals(String.class)) {
			return ValueType.TEXT;
		}

		if (type.isPrimitive()) {
			if (type.equals(int.class)) {
				return ValueType.INTEGER;
			}

			if (type.equals(long.class)) {
				return ValueType.INTEGER;
			}

			if (type.equals(short.class)) {
				return ValueType.INTEGER;
			}

			if (type.equals(byte.class)) {
				return ValueType.INTEGER;
			}

			if (type.equals(char.class)) {
				return ValueType.TEXT;
			}

			if (type.equals(boolean.class)) {
				return ValueType.BOOLEAN;
			}

			if (type.equals(float.class)) {
				return ValueType.DECIMAL;
			}

			if (type.equals(double.class)) {
				return ValueType.DECIMAL;
			}
		}
		if (type.equals(Date.class)) {
			return ValueType.DATE;
		}
		if (type.equals(Timestamp.class)) {
			return ValueType.TIMESTAMP;
		}
		return ValueType.TEXT;
	}

	private static Value[] objectToValueArray(Object obj, java.lang.reflect.Field[] fields) {
		Value[] valarray = new Value[fields.length];
		int j = 0;
		for (java.lang.reflect.Field field : fields) {
			try {
				field.setAccessible(true);
				valarray[j] = Value.parseObject(field.get(obj));
				j++;
			} catch (Exception e) {
				throw new ApplicationError(e.getMessage());
			}
		}
		return valarray;
	}

	/**
	 * sets all row values to object of provided class
	 * 
	 * @param <T>
	 * 
	 * @param className
	 * @param row
	 * @return provided class object
	 */
	private <T> Object rowToObject(Class<T> clazz, Value[] row) {
		T obj = null;
		try {
			obj = clazz.newInstance();
			java.lang.reflect.Field[] fields = obj.getClass().getDeclaredFields();
			String[] columnNames = this.getColumnNames();
			for (java.lang.reflect.Field field : fields) {
				for (int j = 0; j < columnNames.length; j++) {
					/*
					 * sets value to corresponding field of class instance
					 */
					if (field.getName().equalsIgnoreCase(columnNames[j])) {
						/*
						 * We expects the caller to make sure the field names of
						 * the class and data sheet column names to be same
						 */
						field.setAccessible(true);
						ReflectUtil.setAttribute(obj, field.getName(), row[j].toString(), false, true);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obj;
	}
}
