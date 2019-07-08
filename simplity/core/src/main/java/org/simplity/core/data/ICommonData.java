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

import java.time.LocalDate;

import org.simplity.core.value.Value;

/**
 * Default generic data structure that can be used as memory organization for
 * implementing any logic. for example implementing a service. created for
 * implementing a service.
 *
 * <p>
 * A common data contains fields and sheets. A column of a sheet can be used as
 * a field by using a naming convention tableName:columnName as field name.
 * Value of column from the only row, or the current row of a sheet is used.
 *
 * <p>
 * we do not provide any syntax to specify row number. For example
 * tableName:columnName:idx. Instead, we provide a feature to iterate on a
 * sheet, in which case we change the current row internally. As of now, we
 * believe setting currentRow is not a good API. However, we will get involved
 * in actual usage of these API's and re-factor/redesign as when needed
 *
 * @author simplity.org
 */
public interface ICommonData extends IFieldsCollection {

	/**
	 * get sheet, or null if no such field
	 *
	 * @param sheetName
	 *            name of the sheet
	 * @return sheet or null if sheet does not exist
	 */
	public IDataSheet getDataSheet(String sheetName);

	/**
	 * add/replace a data sheet.
	 *
	 * @param sheetName
	 *            name of the sheet. not null
	 * @param sheet
	 *            a sheet object. not null
	 */
	public void putDataSheet(String sheetName, IDataSheet sheet);

	/**
	 * do we have this data sheet?
	 *
	 * @param sheetName
	 *            name of the sheet. not null
	 * @return true if sheet exists. False otherwise.
	 */
	public boolean hasDataSheet(String sheetName);

	/**
	 * remove a data sheet
	 *
	 * @param sheetName
	 *            name of the sheet. not null
	 * @return existing sheet, or null if no such sheet existed before this
	 *         operation
	 */
	public IDataSheet removeDataSheet(String sheetName);

	/**
	 * get an iterator on this sheet. Only one iterator could be active at any
	 * point. Caller has to carefully design iteration to avoid nested iteration
	 * on the same sheet. Also, caller must ensure that the iteration is
	 * cancelled in case the iteration is not completed. Run time error is
	 * thrown on any attempt start iteration on a sheet that is already in the
	 * middle of an iteration.
	 *
	 * @param sheetName
	 *            name of the sheet. not null
	 * @return iterator instance for looping on each row of this sheet
	 * @throws AlreadyIteratingException
	 *             if this sheet is in the middle of an iteration (earlier one
	 *             is neither completed, not cancelled)
	 */
	public IDataSheetIterator startIteration(String sheetName) throws AlreadyIteratingException;

	/*
	 * we add convenient type-specific methods over getValue() and setValue()
	 */

	/**
	 * get value of this key-value pair as text value
	 *
	 * @param fieldName
	 *            name of the field
	 * @return text value, or null if there is no value
	 */
	public String getTextValue(String fieldName);

	/**
	 * value associated with this field/key
	 *
	 * @param fieldName
	 *            name of the field
	 * @return long value if the value is indeed numeric. 0 if no value, or if
	 *         it is non-numeric
	 */
	public long getLongValue(String fieldName);

	/**
	 * value associated with this field/key
	 *
	 * @param fieldName
	 *            name of the field
	 * @return date value if found. null if no such field, or if the value is
	 *         not a date
	 */
	public LocalDate getDateValue(String fieldName);

	/**
	 * value associated with this field/key
	 *
	 * @param fieldName
	 *            name of the field
	 * @return boolean value of the field if found. false if no such field, or
	 *         if the field value is not boolean
	 */
	public boolean getBooleanValue(String fieldName);

	/**
	 * value associated with this field/key
	 *
	 * @param fieldName
	 *            name of the field
	 * @return double value if the field has a numeric value. 0 if no such
	 *         field, or value is non-numeric
	 */
	public double getDoubleValue(String fieldName);

	/**
	 * @param fieldName
	 *            name of the field
	 * @param value
	 */
	public void setTextValue(String fieldName, String value);

	/**
	 * @param fieldName
	 *            name of the field
	 * @param value
	 */
	public void setLongValue(String fieldName, long value);

	/**
	 * @param fieldName
	 *            name of the field
	 * @param value
	 */
	public void setDoubleValue(String fieldName, double value);

	/**
	 * @param fieldName
	 *            name of the field
	 * @param value
	 */
	public void setDateValue(String fieldName, LocalDate value);

	/**
	 * @param fieldName
	 *            name of the field
	 * @param value
	 */
	public void setBooleanValue(String fieldName, boolean value);

	/**
	 * get an array of values for the names
	 *
	 * @param names
	 *            non-null names
	 * @return array of values. an element is null if no value is found for the
	 *         field
	 */
	public Value[] getValues(String[] names);
}
