/*
 * Copyright (c) 2017 simplity.org
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
package org.simplity.kernel.trans;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.data.IDataSheet;
import org.simplity.kernel.service.ServiceContext;
import org.simplity.kernel.util.TextUtil;
import org.simplity.kernel.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * encrypt fields and columns
 *
 * @author simplity
 */
public class Encryption extends AbstractNonDbAction {
	private static final Logger actionLogger = LoggerFactory.getLogger(Encryption.class);

	private static final String ENCRYPT = "encrypt";
	private static final String DECRYPT = "decrypt";

	/** field names to encrypt/decrypt */
	String[] fieldNames;

	/** columns to encrypt/decrypt. use sheetName.columnName */
	String[] columnNames;
	/** encrypt/decrypt */
	String operation = ENCRYPT;

	/*
	 * parse columns into sheet name and column names at getReady
	 */
	private String[] sheetNames;
	private String[] cols;
	private boolean toEncrypt;

	@Override
	protected boolean act(ServiceContext ctx) {
		if (this.fieldNames != null) {
			for (String fieldName : this.fieldNames) {
				Value value = ctx.getValue(fieldName);
				if (value == null) {

					actionLogger.info(fieldName + " not found in service context. Field not encrypted.");

				} else {
					ctx.setValue(fieldName, this.crypt(value));
				}
			}
		}
		if (this.sheetNames != null) {
			for (int i = 0; i < this.sheetNames.length; i++) {
				String sheetName = this.sheetNames[i];
				IDataSheet ds = ctx.getDataSheet(sheetName);
				if (ds == null) {

					actionLogger.info(
							"Datasheet"
									+ sheetName
									+ " not found in service context. "
									+ this.columnNames[i]
									+ " not encrypted.");

					continue;
				}
				int nbrRows = ds.length();
				if (nbrRows == 0) {

					actionLogger.info(
							"Datasheet" + sheetName + " has no data. " + this.columnNames[i] + " not encrypted.");

					continue;
				}
				String colName = this.cols[i];
				int colIdx = ds.getColIdx(colName);
				if (colIdx == -1) {
					actionLogger.info(
							"Coulmn"
									+ colName
									+ " does not exist in datasheet "
									+ sheetName
									+ ".  "
									+ this.columnNames[i]
									+ " not encrypted.");

					continue;
				}
				/*
				 * replace value with crypted one
				 */
				for (int rowIdx = 0; rowIdx < nbrRows; rowIdx++) {
					Value[] row = ds.getRow(rowIdx);
					row[colIdx] = this.crypt(row[colIdx]);
				}
				actionLogger.info(nbrRows + " values transformed in data sheet " + sheetName);
			}
		}
		return true;
	}

	private Value crypt(Value value) {
		if (Value.isNull(value)) {
			return value;
		}
		String txt = value.toString();
		if (this.toEncrypt) {
			txt = TextUtil.encrypt(txt);
		} else {
			txt = TextUtil.decrypt(txt);
		}
		return Value.newTextValue(txt);
	}

	@Override
	public void getReady(int idx, TransactionProcessor task) {
		super.getReady(idx, task);
		if (this.operation == null) {
			throw new ApplicationError(
					"crypto should have a value of either " + ENCRYPT + " or " + DECRYPT);
		}
		String text = this.operation.toLowerCase();
		if (text.equals(ENCRYPT)) {
			this.toEncrypt = true;
		} else if (text.equals(DECRYPT) == false) {
			throw new ApplicationError(
					"crypto should have a value of either " + ENCRYPT + " or " + DECRYPT);
		}
		if (this.columnNames != null) {
			int nbrCols = this.columnNames.length;
			this.cols = new String[nbrCols];
			this.sheetNames = new String[nbrCols];
			for (int i = 0; i < this.cols.length; i++) {
				String[] parts = this.sheetNames[i].split("\\.");
				if (parts.length != 2) {
					throw new ApplicationError(
							"columnName shoudl folloe sheetName.columnNAme convention. columnNames "
									+ this.columnNames
									+ " has invalid format");
				}
				this.cols[i] = parts[0].trim();
				this.sheetNames[i] = parts[1].trim();
			}
		}
	}
}
