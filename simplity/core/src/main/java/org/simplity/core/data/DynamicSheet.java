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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.simplity.core.ApplicationError;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * represents a fields collection internally, but implements all methods of a
 * data sheet. This is to be used when you are likely to add/remove fields, and
 * if used as a row of data, the order of the columns is not important.
 *
 * @author simplity.org
 */
public class DynamicSheet implements IDataSheet {
	private static final Logger logger = LoggerFactory.getLogger(DynamicSheet.class);

	private final Map<String, Value> fieldValues = new HashMap<String, Value>();

	@Override
	public String[][] getRawData() {
		int n = this.fieldValues.size();
		String[] header = new String[n];
		String[] values = new String[n];
		n = 0;
		for (Entry<String, Value> entry : this.fieldValues.entrySet()) {
			header[n] = entry.getKey();
			values[n] = entry.getValue().toString();
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
		return this.fieldValues.size();
	}

	@Override
	public String[] getColumnNames() {
		return this.fieldValues.keySet().toArray(new String[0]);
	}

	@Override
	public ValueType[] getValueTypes() {
		String[] names = this.getColumnNames();
		ValueType[] types = new ValueType[names.length];
		int i = 0;
		for (String colName : names) {
			types[i] = this.fieldValues.get(colName).getValueType();
			i++;
		}
		return types;
	}

	@Override
	public Value[] getRow(int zeroBasedRowNumber) {
		if (zeroBasedRowNumber != 0) {
			return null;
		}
		String[] names = this.getColumnNames();
		Value[] values = new Value[names.length];
		int i = 0;
		for (String colName : names) {
			values[i] = this.fieldValues.get(colName);
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
	public void setColumnValue(String columnName, int zeroBasedRowNumber, Value value) {
		if (zeroBasedRowNumber != 0) {
			return;
		}
		if (value == null) {
			this.fieldValues.remove(columnName);
		} else {
			this.fieldValues.put(columnName, value);
		}
	}

	@Override
	public Iterator<IFieldsCollection> iterator() {
		return new DataRows(this);
	}

	@Override
	public Value getValue(String fieldName) {
		return this.fieldValues.get(fieldName);
	}

	@Override
	public void setValue(String fieldName, Value value) {
		if (value == null) {
			this.fieldValues.remove(fieldName);
		} else {
			this.fieldValues.put(fieldName, value);
		}
	}

	@Override
	public boolean hasValue(String fieldName) {
		return this.fieldValues.containsKey(fieldName);
	}

	@Override
	public Value removeValue(String fieldName) {
		return this.fieldValues.remove(fieldName);
	}

	@Override
	public void addRow(Value[] row) {
		throw new ApplicationError("addRow() should not be called for SingleRowSheet");
	}

	@Override
	public Value[] getColumnValues(String columnName) {
		Value value = this.getValue(columnName);
		Value[] values = { value };
		return values;
	}

	@Override
	public void addColumn(String columnName, ValueType valueType, Value[] columnValues) {
		if (columnValues != null) {
			this.fieldValues.put(columnName, columnValues[0]);
		}
	}

	@Override
	public Set<Entry<String, Value>> getAllFields() {
		return this.fieldValues.entrySet();
	}

	@Override
	public Set<Entry<String, Value>> getAllFields(int rowIdx) {
		return this.fieldValues.entrySet();
	}

	@Override
	public void trace() {

		logger.info("(Dynamic Sheet)");

		for (Map.Entry<String, Value> field : this.fieldValues.entrySet()) {

			logger.info(field.getKey() + '=' + field.getValue());
		}
	}

	@Override
	public int appendRows(IDataSheet sheet) {
		throw new ApplicationError(
				"Dynamic sheet can not have more than one rows, and hence appendRows operation is invalid");
	}

	@Override
	public void addColumn(String columnName, Value value) {
		this.setValue(columnName, value);
	}

	@Override
	public int getColIdx(String columnName) {
		int i = 0;
		for (String colName : this.getColumnNames()) {
			if (colName.equals(columnName)) {
				return i;
			}
			i++;
		}

		logger.info("We did not find column " + columnName + " in this dynamic sheet");

		return -1;
	}

	@Override
	public String toSerializedText(DataSerializationType serializationType) {
		throw new ApplicationError("Sorry, serialization is not yet implemented for Dynamic sheet");
		// TODO to be built
	}

	@Override
	public void fromSerializedText(String text, DataSerializationType serializationType, boolean replaceExistingRows) {
		throw new ApplicationError("Sorry, de-serialization is not yet implemented for Dynamic sheet");
		// TODO to be built
	}

	@Override
	public int[] getColumnIndexes(String[] names) {
		int[] result = new int[names.length];
		for (int i = 0; i < names.length; i++) {
			result[i] = this.getColIdx(names[i]);
		}
		return result;
	}

	@Override
	public IFieldsCollection getRowAsFields(int zeroBasedRow) {
		return null;
	}

	@Override
	public int appendEmptyRows(int n) {
		throw new ApplicationError("Rows can not be added to a Dynamic sheet");
	}
}
