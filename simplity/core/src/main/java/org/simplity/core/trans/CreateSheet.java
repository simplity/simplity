/*
 * Copyright (c) 2018 simplity.org
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

package org.simplity.core.trans;

import java.util.ArrayList;
import java.util.List;

import org.simplity.core.ApplicationError;
import org.simplity.core.app.Application;
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationMessage;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.data.IFieldsCollection;
import org.simplity.core.data.MultiRowsSheet;
import org.simplity.core.dm.Record;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.util.TextUtil;
import org.simplity.core.value.Value;

/**
 * Create a data sheet
 *
 * @author simplity.org
 *
 */
public class CreateSheet extends AbstractNonDbAction {

	/** name of sheet to be created */
	@FieldMetaData(isRequired = true)
	String sheetName;

	/**
	 * this is a semi-colon(;) separated set of rows. Each row is a comma(,)
	 * separated set of columns; First row is header. e.g.
	 * name,value;red,1;green,2;blue,3. Note that the syntax does not allow text
	 * that may contain comma or semi-colon
	 */
	String[][] data;
	/** record name for creating an empty sheet */
	@FieldMetaData(alternateField = "data")
	String recordName;

	@Override
	protected boolean act(ServiceContext ctx) {
		if (!(this.data == null || this.data.length == 0 || this.data[0].length == 0)) {
			/*
			 * parse rows into a collection
			 */
			List<Value[]> allValues = new ArrayList<Value[]>();
			for (int i = 1; i < this.data.length; i++) {
				allValues.add(this.parseRow(this.data[i], ctx));
			}
			/*
			 * it is possible that the sheet is improper
			 */
			try {
				MultiRowsSheet sheet = new MultiRowsSheet(this.data[0], allValues);
				sheet.validate();
				ctx.putDataSheet(this.sheetName, sheet);
			} catch (Exception e) {
				throw new ApplicationError(e, " Error while creating data sheet.");
			}
			return true;

		}
		try {
			Record rec = Application.getActiveInstance().getRecord(this.recordName);
			IDataSheet sheet = rec.createSheet(false, false);
			ctx.putDataSheet(this.sheetName, sheet);
		} catch (Exception e) {
			throw new ApplicationError(e, " Error while creating data sheet.");
		}

		return true;
	}

	/**
	 * parse a text row into values
	 *
	 * @param row
	 *            text row
	 * @param fields
	 *            name-value pairs that are part of the context. This is the
	 *            collection from which we get values for field names in our
	 *            text array.
	 * @return array of parsed values
	 * @throws ApplicationError
	 *             in case of any parse error
	 */
	private Value[] parseRow(String[] row, IFieldsCollection fields) {
		Value[] values = new Value[row.length];
		int idx = 0;
		for (String cell : row) {
			Value value = null;
			/*
			 * does this cell contain name of a field?
			 */
			String fldName = TextUtil.getFieldName(cell);
			if (fldName == null) {
				value = Value.parse(cell);
				if (value == null) {
					throw new ApplicationError(
							"data for createSheet action has an invalid cell value of " + cell);
				}
			} else {
				value = fields.getValue(fldName);
				if (value == null) {
					throw new ApplicationError(
							"Field "
									+ fldName
									+ " not found in fields collection. This is expected as a value for a cell for createSheet action.");
				}
			}
			values[idx] = value;
			idx++;
		}
		return values;
	}

	@Override
	public void validateSpecific(IValidationContext vtx, TransactionProcessor task) {
		super.validateSpecific(vtx, task);
		/*
		 * every row should have same number of rows.
		 */
		if (this.data == null || this.data.length == 0 || this.data[0].length == 0) {
			return;
		}

		int n = this.data[0].length;
		for (int i = 1; i < this.data.length; i++) {
			String[] arr = this.data[i];
			if (arr.length != n) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
						"Each row in data is to have same number of columns as the header. we have header with "
								+ n
								+ " columns, but data row "
								+ i
								+ " has "
								+ arr.length
								+ " columns.",
						"data"));
			}
			/*
			 * validate whether cell content is valid
			 */
			for (int j = 0; j < arr.length; j++) {
				String cell = arr[j];
				/*
				 * value could be a fieldName like $customerName
				 */
				String fldName = TextUtil.getFieldName(cell);
				if (fldName == null) {
					Value val = Value.parse(cell);
					if (val == null) {
						vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
								"Cell at row "
										+ i
										+ " and column (1 based) "
										+ (j + 1)
										+ " has an invalid value of "
										+ cell,
								"data"));
					}
				}
			}
		}
	}
}
