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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.kernel.trans;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.app.Application;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.FieldMetaData;
import org.simplity.kernel.comp.IValidationContext;
import org.simplity.kernel.data.IDataSheet;
import org.simplity.kernel.data.IFieldsCollection;
import org.simplity.kernel.idb.IDbHandle;
import org.simplity.kernel.idb.ITransactionHandle;
import org.simplity.kernel.rdb.DbUsage;
import org.simplity.kernel.service.ServiceContext;
import org.simplity.kernel.sql.StoredProcedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * execute a stored procedure
 *
 * @author simplity.org
 */
public class ExecuteSp extends AbstractDbAction {
	private static final Logger logger = LoggerFactory.getLogger(ExecuteSp.class);

	/** qualified name */
	@FieldMetaData(isRequired = true, isReferenceToComp = true, referredCompType = ComponentType.SP)
	String procedureName;
	/**
	 * sheet name for input data. Null implies that input, if any, would be from
	 * fields collection of ctx
	 */
	String sheetNameForInputParameters;

	/**
	 * output parameters from this SP may be extracted out to a sheet. Else, if
	 * present, they will be extracted to fields collection of ctx
	 */
	String sheetNameForOutputParameters;

	/**
	 * if this procedure has defined outputRecordNames, then you may specify the
	 * sheet names. Default is to use definition from record.
	 */
	String[] outputSheetNames;

	@Override
	protected boolean actWithDb(ServiceContext ctx, IDbHandle dbHandle) {
		IFieldsCollection inSheet = ctx;
		IFieldsCollection outSheet = ctx;

		if (this.sheetNameForInputParameters != null) {
			inSheet = ctx.getDataSheet(this.sheetNameForInputParameters);
			if (inSheet == null) {
				throw new ApplicationError(
						"Store Procedure Step "
								+ this.getName()
								+ " requires data sheet "
								+ this.sheetNameForInputParameters
								+ " for its input parameters.");
			}
		}

		if (this.sheetNameForOutputParameters != null) {
			outSheet = ctx.getDataSheet(this.sheetNameForOutputParameters);
			if (outSheet == null) {
				throw new ApplicationError(
						"Store Procedure Step "
								+ this.getName()
								+ " requires data sheet "
								+ this.sheetNameForOutputParameters
								+ " for its output parameters.");
			}
		}

		StoredProcedure sp = Application.getActiveInstance().getStoredProcedure(this.procedureName);
		IDataSheet[] outSheets = sp.execute(inSheet, outSheet, (ITransactionHandle) dbHandle, ctx);
		if (outSheets == null) {
			logger.info("Stored procedure " + this.getName() + " execution completed with no sheets.");
			return true;
		}

		int nbrOutSheets = outSheets.length;

		logger.info(
				"Stored procedure Step "
						+ this.getName()
						+ " returned "
						+ nbrOutSheets
						+ " sheets of data");

		String[] names = null;
		if (this.outputSheetNames != null) {
			if (this.outputSheetNames.length != nbrOutSheets) {
				throw new ApplicationError(
						"Store Procedure Step "
								+ this.getName()
								+ " uses stored procedure "
								+ this.procedureName
								+ " with "
								+ this.outputSheetNames.length
								+ " output sheets, but the stored procedure requires "
								+ nbrOutSheets);
			}
			names = this.outputSheetNames;
		} else {
			names = sp.getDefaultSheetNames();
		}
		for (int i = 0; i < nbrOutSheets; i++) {
			ctx.putDataSheet(names[i], outSheets[i]);
		}
		return true;
	}

	@Override
	public DbUsage getDbUsage() {
		return DbUsage.READ_WRITE;
	}

	@Override
	protected void validateDbAction(IValidationContext vtx, TransactionProcessor task) {
		// nothing
	}
}
