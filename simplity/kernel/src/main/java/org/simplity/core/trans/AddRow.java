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

import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * add a row of data to a data sheet based on values in the service context.
 * Value of a column is picked up from the service context with using columnName
 * as fieldValue. If a column value is not found a Value-null is added. We
 * always avoid java nulls
 *
 * @author simplity.org
 *
 */
public class AddRow extends AbstractNonDbAction {
	private static final Logger logger = LoggerFactory.getLogger(AddRow.class);

	/** sheet to which we want to add a column */
	@FieldMetaData(isRequired = true)
	String sheetName;

	@Override
	protected boolean act(ServiceContext ctx) {
		IDataSheet sheet = ctx.getDataSheet(this.sheetName);
		if (sheet == null) {
			logger.info("Sheet {} not found in context. Row not added", this.sheetName);
			return false;
		}

		String[] names = sheet.getColumnNames();
		Value[] row = new Value[names.length];
		ValueType[] types = sheet.getValueTypes();
		int i = 0;
		for (String name : names) {
			Value value = ctx.getValue(name);
			ValueType vt = types[i];
			if (value == null) {
				/*
				 * We prefer to avoid java null for obvious reasons
				 */
				value = Value.newUnknownValue(vt);
			} else if (value.getValueType() != vt) {
				/*
				 * should we reject this value? possible that it is text but has
				 * valid number in it!!
				 */
				String txt = value.toString();

				logger.info(
						"Found a value of type {} for column {} while we were expecting {}. Will be converted.",
						value.getValueType(), name, vt);

				value = Value.parseValue(txt, vt);
				if (value == null) {
					logger.info(
							"Unable to convert {} to type {} . setting column to  NullValue", txt, vt);
				}
			}
			row[i] = value;
			i++;
		}
		sheet.addRow(row);
		return true;
	}
}
