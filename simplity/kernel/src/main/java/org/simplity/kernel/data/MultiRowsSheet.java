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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.dm.field.Field;
import org.simplity.kernel.util.ArrayUtil;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a sheet with possibly more than one rows.
 *
 * @author simplity.org
 */
public class MultiRowsSheet implements IDataSheet {
	private static final Logger logger = LoggerFactory.getLogger(MultiRowsSheet.class);

	/**
	 * data is organized as a list (rows) of column values(array). This is
	 * because we expect rows to be added dynamically, and not columns. Of
	 * course, user can add columns as well as rows, but adding rows is expected
	 * to be quite frequent, and adding column is more of an exception
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

			logger.info("Request to get value for column" + columnName + " and index " + zeroBasedRowNumber
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

	/** set value types based on values in first row */
	private ValueType[] extractValueTypes(Value[] row) {
		ValueType[] valueTypes = new ValueType[row.length];
		int i = 0;
		for (Value value : row) {
			valueTypes[i] = value.getValueType();
			i++;
		}
		return valueTypes;
	}

	/** add column names and create columnIndexes */
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
	public Iterator<IFieldsCollection> iterator() {
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

	/** we do not remove column values */
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

			logger.info("getColumn values is returning empty array as there are no data");

			return values;
		}
		Integer n = this.columnIndexes.get(columnName);
		if (n == null) {

			logger.info(columnName
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

		logger.info(this.toString("\t", "\n"));
	}

	/**
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
	public int appendRows(IDataSheet sheet) {
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

		logger.info("We did not find column " + columnName + " in this multi-row sheet");

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
		throw new ApplicationError("Sorry, serialization is not yet implemented for MultiRowsShwwt");
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
		throw new ApplicationError("Sorry, de-serialization is not yet implemented for MultiRowsShwwt");
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

	@Override
	public int appendEmptyRows(int nbrRowsToAdd) {
		int nbr = this.width();
		for (int i = 0; i < nbrRowsToAdd; i++) {
			this.data.add(new Value[nbr]);
		}
		return this.data.size();
	}

	/**
	 * remove a row and shift any rows after that, if any. sheet.length() will
	 * reduce by if the row was indeed removed
	 *
	 * @param zeroBasedIdx
	 * @return existing row. null if no row
	 */
	public Value[] deleteRow(int zeroBasedIdx) {
		return this.data.remove(zeroBasedIdx);
	}

	class RowAsFields implements IFieldsCollection {
		private final int idx;

		RowAsFields(int idx) {
			this.idx = idx;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.simplity.kernel.data.FieldsInterface#getValue(java.lang.String)
		 */
		@Override
		public Value getValue(String fieldName) {
			return MultiRowsSheet.this.getColumnValue(fieldName, this.idx);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.simplity.kernel.data.FieldsInterface#setValue(java.lang.String,
		 * org.simplity.kernel.value.Value)
		 */
		@Override
		public void setValue(String fieldName, Value value) {
			MultiRowsSheet.this.setColumnValue(fieldName, this.idx, value);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.simplity.kernel.data.FieldsInterface#hasValue(java.lang.String)
		 */
		@Override
		public boolean hasValue(String fieldName) {
			return MultiRowsSheet.this.getColumnValue(fieldName, this.idx) != null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.simplity.kernel.data.FieldsInterface#removeValue(java.lang.
		 * String)
		 */
		@Override
		public Value removeValue(String fieldName) {
			Value val = MultiRowsSheet.this.getColumnValue(fieldName, this.idx);
			MultiRowsSheet.this.setColumnValue(fieldName, this.idx, null);
			return val;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.simplity.kernel.data.FieldsInterface#getAllFields()
		 */
		@Override
		public Set<Entry<String, Value>> getAllFields() {
			throw new ApplicationError("Sorry. MultiRowsSheet is not designed to get all fields of a row into a set");
		}

	}

	/**
	 * get an object that behaves like a fieldsIntefrace on the desired row of
	 * data
	 *
	 * @param rowIdx
	 * @return simulator, or null if the idx is out of range.
	 */
	@Override
	public IFieldsCollection getRowAsFields(int rowIdx) {
		if (rowIdx >= this.length()) {
			return null;
		}
		return new RowAsFields(rowIdx);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.kernel.data.DataSheet#getColumnIndexes(java.lang.String[])
	 */
	@Override
	public int[] getColumnIndexes(String[] names) {
		int[] result = new int[names.length];
		for (int i = 0; i < names.length; i++) {
			result[i] = this.getColIdx(names[i]);
		}
		return result;
	}
}
