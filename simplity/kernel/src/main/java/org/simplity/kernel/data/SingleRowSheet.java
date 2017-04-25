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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.dm.Field;
import org.simplity.kernel.util.ArrayUtil;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;

/**
 * represents a sheet with exactly one row of data. Use this if list of columns
 * is generally known at the time of creating the sheet, and the order is
 * important. If you expect frequent add/remove columns, look at DynamicSheet
 * instead
 *
 * @author simplity.org
 *
 */
public class SingleRowSheet implements DataSheet {
	private static final char TAB = '\t';
	private String[] columnNames;
	private ValueType[] valueTypes;
	private final Map<String, Value> fieldValues = new HashMap<String, Value>();
	private int[] columnWidths;
	/**
	 * create a sheet that does not have fixed fields. fields are added as and
	 * when values are added.
	 *
	 * @param columnNames
	 * @param valueTypes
	 */
	public SingleRowSheet(String[] columnNames, ValueType[] valueTypes) {
		this.columnNames = columnNames;
		this.valueTypes = valueTypes;
		this.setUnknownValues();
	}

	/**
	 * @param fields
	 *            to be used as columns for the data sheet
	 */
	public SingleRowSheet(Field[] fields) {
		int n = fields.length;
		this.columnNames = new String[n];
		this.valueTypes = new ValueType[n];
		n = 0;
		/*
		 * do we have field widths?. Small optimization because a very small minority of records will have this.
		 */
		for (Field field : fields) {
			this.columnNames[n] = field.getName();
			this.valueTypes[n] = field.getValueType();
			n++;
		}
		this.setUnknownValues();
		/*
		 * do we have field widths?.
		 * we did not put this inside the loop with an if becuase this is a very very very rare case.
		 */
		if(fields[0].getFieldWidth() != 0){
			this.columnWidths = new int[fields.length];
			n = 0;
			for (Field field : fields) {
				this.columnWidths[n] = field.getFieldWidth();
				n++;
			}
		}
	}

	/**
	 * creates a sheet from raw data and known data types
	 *
	 * @param rawData
	 * @param valueTypes
	 */
	public SingleRowSheet(String[][] rawData, ValueType[] valueTypes) {
		if (rawData.length != 2) {
			throw new ApplicationError(
					"Data sheet that is designed for one row is asked to take "
							+ rawData.length + " rows.");
		}
		this.columnNames = rawData[0];
		this.valueTypes = valueTypes;
		String[] values = rawData[1];
		for (int i = 0; i < this.columnNames.length; i++) {
			Value value = Value.parseValue(values[i], valueTypes[i]);
			if (value != null) {
				this.fieldValues.put(this.columnNames[i], value);
			}
		}
	}

	@Override
	public String[][] getRawData() {
		int n = this.columnNames.length;
		String[] values = new String[n];
		String[] header = new String[n];
		n = 0;
		for (String colName : this.columnNames) {
			header[n] = colName;
			Value val = this.fieldValues.get(colName);
			if (val == null || val.isUnknown()) {
				values[n] = Value.NULL_TEXT_VALUE;
			} else {
				values[n] = val.toString();
			}
			n++;
		}
		String[][] rawData = { header, values };
		return rawData;
	}

	@Override
	public int length() {
		return 1;
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
		return this.valueTypes;
	}

	@Override
	public Value[] getRow(int zeroBasedRowNumber) {
		if (zeroBasedRowNumber != 0) {
			return null;
		}
		Value[] values = new Value[this.columnNames.length];
		int i = 0;
		for (String colName : this.columnNames) {
			Value value = this.fieldValues.get(colName);
			values[i] = value == null ? Value
					.newUnknownValue(this.valueTypes[i]) : value;
			i++;
		}
		return values;
	}

	@Override
	public List<Value[]> getAllRows() {
		List<Value[]> rows = new ArrayList<Value[]>(1);
		rows.add(this.getRow(0));
		return rows;
	}

	@Override
	public Value getColumnValue(String columnName, int zeroBasedRowNumber) {
		if (zeroBasedRowNumber != 0) {
			return null;
		}
		return this.fieldValues.get(columnName);
	}

	@Override
	public void setColumnValue(String columnName, int zeroBasedRowNumber,
			Value value) {
		if (zeroBasedRowNumber != 0) {
			return;
		}
		this.fieldValues.put(columnName, value);
	}

	@Override
	public Iterator<FieldsInterface> iterator() {
		return new DataRows(this);
	}

	@Override
	public Value getValue(String fieldName) {
		return this.fieldValues.get(fieldName);
	}

	@Override
	public void setValue(String fieldName, Value value) {
		this.fieldValues.put(fieldName, value);
	}

	@Override
	public boolean hasValue(String fieldName) {
		Value val = this.fieldValues.get(fieldName);
		return val != null && val.isUnknown() == false;
	}

	@Override
	public void addRow(Value[] row) {
		for (int i = 0; i < this.columnNames.length; i++) {
			this.setValue(this.columnNames[i], row[i]);
		}
	}

	private void setUnknownValues() {
		for (int i = 0; i < this.columnNames.length; i++) {
			this.fieldValues.put(this.columnNames[i],
					Value.newUnknownValue(this.valueTypes[i]));
		}
	}

	@Override
	public Value[] getColumnValues(String columnName) {
		Value value = this.getValue(columnName);
		Value[] values = { value };
		return values;
	}

	@Override
	public void addColumn(String columnName, ValueType valueType, Value[] values) {
		int colIdx = -1;
		/*
		 * do we have this column?
		 */
		if (this.fieldValues.containsKey(columnName)) {
			for (int i = 0; i < this.columnNames.length; i++) {
				if (this.columnNames.equals(columnName)) {
					colIdx = i;
					break;
				}
			}
		}
		/*
		 * let us add this column by extending arrays
		 */
		if (colIdx == -1) {
			this.columnNames = ArrayUtil.extend(this.columnNames, columnName);
			this.valueTypes = ArrayUtil.extend(this.valueTypes, valueType);
		} else if (valueType != this.valueTypes[colIdx]) {
			/*
			 * is this value compatible with existing value?
			 */
			throw new ApplicationError(columnName + "in this sheet is of type "
					+ this.valueTypes[colIdx]
					+ " but an addColumn() is requested for a value type of "
					+ valueType);
		}
		this.fieldValues.put(columnName,
				values == null || values.length == 0 ? null : values[0]);
	}

	@Override
	public Value removeValue(String fieldName) {
		return this.fieldValues.remove(fieldName);
	}

	@Override
	public Set<Entry<String, Value>> getAllFields() {
		return this.fieldValues.entrySet();
	}

	@Override
	public Set<Entry<String, Value>> getAllFields(int rowIdx) {
		return this.fieldValues.entrySet();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.data.DataSheet#trace()
	 */
	@Override
	public void trace() {
		Tracer.trace("(Single row sheet)\n");
		StringBuilder hdr = new StringBuilder("Header:");
		StringBuilder row = new StringBuilder("Data  :");
		for (String col : this.columnNames) {
			hdr.append(TAB).append(col);
			row.append(TAB).append(this.fieldValues.get(col));
		}
		Tracer.trace(hdr.toString());
		Tracer.trace(row.toString());
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
		throw new ApplicationError(
				"SingleRowSheet sheet can not have more than one rows, and hence appendRows operation is invalid");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.data.DataSheet#addColumn(java.lang.String,
	 * org.simplity.kernel.value.Value)
	 */
	@Override
	public void addColumn(String columnName, Value value) {
		this.setValue(columnName, value);

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.data.DataSheet#getColIdx(java.lang.String)
	 */
	@Override
	public int getColIdx(String columnName) {
		int i = 0;
		for (String colName : this.columnNames) {
			if (colName.equals(columnName)) {
				return i;
			}
			i++;
		}
		Tracer.trace("We did not find column " + columnName
				+ " in this single-row sheet");
		return -1;
	}
	/* (non-Javadoc)
	 * @see org.simplity.kernel.data.DataSheet#toSerializedText(org.simplity.kernel.data.DataSerializationType)
	 */
	@Override
	public String toSerializedText(DataSerializationType serializationType) {
		throw new ApplicationError("Sorry, serialization is not yet implemented for Single row sheet");
		//TODO to be built
	}

	/* (non-Javadoc)
	 * @see org.simplity.kernel.data.DataSheet#fromSerializedText(java.lang.String, org.simplity.kernel.data.DataSerializationType, boolean)
	 */
	@Override
	public void fromSerializedText(String text,
			DataSerializationType serializationType,
			boolean replaceExistingRows) {
		throw new ApplicationError("Sorry, de-serialization is not yet implemented for Single row sheet");
		//TODO to be built
	}
	/**
	 * if this is used for serialization into fixed-width test, we need this
	 * @param widths
	 */
	public void setWidths(int[] widths){
		if(widths.length != this.width()){
			throw new ApplicationError("Design error : data sheet has " + this.width() + " columns but " + widths.length +" values are supplied for width.");
		}
		this.columnWidths=widths;
	}

	/**
	 * @return widths of columns in case this used for fixed-width formatting
	 */
	public int[] getWidths(){
		return this.columnWidths;
	}
}
