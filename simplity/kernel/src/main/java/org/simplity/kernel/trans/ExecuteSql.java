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

import org.simplity.kernel.app.Application;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.FieldMetaData;
import org.simplity.kernel.comp.IValidationContext;
import org.simplity.kernel.data.IDataSheet;
import org.simplity.kernel.idb.IDbHandle;
import org.simplity.kernel.idb.ITransactionHandle;
import org.simplity.kernel.rdb.DbUsage;
import org.simplity.kernel.service.ServiceContext;
import org.simplity.kernel.sql.Sql;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * execute a sql
 *
 * @author simplity.org
 */
public class ExecuteSql extends AbstractDbAction {
	private static final Logger actionLogger = LoggerFactory.getLogger(ExecuteSql.class);

	/** qualified sql name */
	@FieldMetaData(isRequired = true, isReferenceToComp = true, referredCompType = ComponentType.REC)
	String sqlName;
	/** sheet name for input data */
	String inputSheetName;
	/**
	 * many a times, we put constraints in db, and it may be convenient to use
	 * that to do the validation. for example we insert a row, and db may raise
	 * an error because of a duplicate columns. In such a case, we treat this
	 * error as "failure", rather than an exception
	 */
	boolean treatSqlErrorAsNoResult;

	@Override
	protected boolean actWithDb(ServiceContext ctx, IDbHandle handle) {
		ITransactionHandle dbHandle = (ITransactionHandle) handle;
		Sql sql = Application.getActiveInstance().getSql(this.sqlName);
		if (this.inputSheetName == null) {
			int nbr = sql.execute(ctx, dbHandle, this.treatSqlErrorAsNoResult);
			return nbr > 0;
		}

		IDataSheet inSheet = ctx.getDataSheet(this.inputSheetName);
		if (inSheet != null) {
			int nbr = sql.execute(inSheet, dbHandle, this.treatSqlErrorAsNoResult);
			return nbr > 0;
		}

		actionLogger.info(
				"Sql Save Action "
						+ this.getName()
						+ " did not execute because input sheet "
						+ this.inputSheetName
						+ " is not found.");

		return false;
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
