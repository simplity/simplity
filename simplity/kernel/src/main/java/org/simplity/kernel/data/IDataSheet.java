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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;

/**
 * A data sheet is a run-time data storage for tabular data - rows of data with
 * each row having values for each of the columns. we expect that the callers of
 * API are responsible to check rows and columns before asking for data, and
 * hence we do not throw exceptions. If caller asks for a non-existing data, it
 * may end-up in a null-pointer exception at run time.
 *
 * @author simplity.org
 */
public interface IDataSheet extends IFieldsCollection, Iterable<IFieldsCollection> {

	/**
	 * get a simple rows-and-columns data structure with all values as text.
	 * boolean is translated to 1/0. date is translated as milliseconds
	 *
	 * @return array of rows, with the header row containing column names
	 */
	public String[][] getRawData();

	/** @return number of rows of data. 0 if there is no data. */
	public int length();

	/** @return number of columns this sheet has */
	public int width();

	/**
	 * @return an array containing the column names, in the order in which raw
	 *         data (or row data) would be returned.
	 */
	public String[] getColumnNames();

	/**
	 * @return value types of columns, in the order in which getColumnNames()
	 *         would return the column names
	 */
	public ValueType[] getValueTypes();

	/**
	 * @param zeroBasedRowNumber
	 *            0 for first row etc..
	 * @return one row of data as Value objects.
	 */
	public Value[] getRow(int zeroBasedRowNumber);

	/**
	 * add a row of values
	 *
	 * @param row
	 *            values in the right order of columns
	 */
	public void addRow(Value[] row);

	/**
	 * @param columnName
	 *            columnName
	 * @param zeroBasedRowNumber
	 *            zeroBasedRowNumber
	 * @return get value of a column from a given row
	 */
	public Value getColumnValue(String columnName, int zeroBasedRowNumber);

	/**
	 * set value for a column
	 *
	 * @param columnName
	 *            columnName
	 * @param zeroBasedRowNumber
	 *            zeroBasedRowNumber
	 * @param value
	 *            value
	 */
	public void setColumnValue(String columnName, int zeroBasedRowNumber, Value value);

	/**
	 * @param columnName
	 *            columnName
	 * @return array of column values
	 */
	public Value[] getColumnValues(String columnName);

	/**
	 * add a column of values to this sheet. If column is already present, we
	 * update its values. This is an expensive operation, as we may have to
	 * extend arrays. Use with caution.
	 *
	 * @param columnName
	 *            columnName
	 * @param columnType
	 *            columnType
	 * @param columnValues
	 *            optional. if non-null, its length MUST match the number of
	 *            rows in the data sheet
	 */
	public void addColumn(String columnName, ValueType columnType, Value[] columnValues);

	/**
	 * @param rowIdx
	 *            zero based row in this sheet
	 * @return an iterator for accessing all fields as an entry of name-value
	 *         for current row
	 */
	public Set<Map.Entry<String, Value>> getAllFields(int rowIdx);

	/** trace data in this sheet */
	public void trace();

	/**
	 * copy rows, if the columns are compatible. Throws application error if
	 * value types in the order of columns do not match
	 *
	 * @param sheet
	 *            sheet
	 * @return number of rows copied
	 */
	public int appendRows(IDataSheet sheet);

	/**
	 * add a column, but the column value is same for all rows.
	 *
	 * @param columnName
	 *            columnName
	 * @param value
	 *            value
	 */
	public void addColumn(String columnName, Value value);

	/**
	 * @param columnName
	 *            columnName
	 * @return 0 based column index of this column, -1 if no such column
	 */
	public int getColIdx(String columnName);

	/** @return list of all rows in this sheet */
	List<Value[]> getAllRows();

	/**
	 * Data is likely to be transported across domains/layers, specially in the
	 * micro-services architecture and multi-domain designs. serialize this data
	 * sheet into text as per the format
	 *
	 * @param serializationType
	 * @return text that is convenient to transport across domains
	 */
	public String toSerializedText(DataSerializationType serializationType);

	/**
	 * de-serialize and extract data into this sheet
	 *
	 * @param text
	 * @param serializationType
	 * @param replaceExistingRows
	 */
	public void fromSerializedText(String text, DataSerializationType serializationType, boolean replaceExistingRows);

	/**
	 *
	 * @param names
	 *            for which column indexes are to be set
	 * @return array of column indexes. -1 implies that it is not a column
	 */
	public int[] getColumnIndexes(String[] names);

	/**
	 * wrap a row as fields collection, so that setValue, getVAlue etc.. work
	 * for this row. NOTE: this operation is not thread-safe.
	 *
	 * @param zeroBasedRow
	 * @return a fieldsInterface that can be used to manipulate this row using
	 *         field-like methods
	 */
	public IFieldsCollection getRowAsFields(int zeroBasedRow);

	/**
	 * add empty rows at the end of this sheet
	 * 
	 * @param nbrRowsToAdd
	 * @return total number of rows after adding these rows
	 */
	public int appendEmptyRows(int nbrRowsToAdd);
}
