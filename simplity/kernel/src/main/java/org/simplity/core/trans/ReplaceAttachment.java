/*
 * Copyright (c) 2015 EXILANT Technologies Private Limited (www.exilant.com)
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
package org.simplity.core.trans;

import org.simplity.core.app.AppConventions;
import org.simplity.core.app.Application;
import org.simplity.core.comp.ComponentType;
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.data.SingleRowSheet;
import org.simplity.core.dm.DbTable;
import org.simplity.core.idb.IDbHandle;
import org.simplity.core.idb.IReadOnlyHandle;
import org.simplity.core.idb.ITransactionHandle;
import org.simplity.core.rdb.DbUsage;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Save (add/modify/delete) a row from a record, and possibly save relevant rows
 * from related records
 *
 * @author simplity.org
 */
public class ReplaceAttachment extends AbstractDbAction {
	private static final Logger actionLogger = LoggerFactory.getLogger(ReplaceAttachment.class);

	/** rdbms table that has this column */
	@FieldMetaData(isRequired = true, isReferenceToComp = true, referredCompType = ComponentType.REC)
	String recordName;

	/**
	 * field name of this attachment. This is the name that we refer in our
	 * service context. This is the name that is known to client
	 */
	@FieldMetaData(isRequired = true)
	String attachmentFieldName;

	private String selectSql;
	private String updateSql;
	private String keyFieldName;

	@Override
	protected boolean actWithDb(ServiceContext ctx, IDbHandle dbHandle) {
		Value tokenValue = ctx.getValue(this.attachmentFieldName);
		Value keyValue = ctx.getValue(this.keyFieldName);
		Value[] values = { keyValue };
		String[] columnNames = { this.attachmentFieldName };
		ValueType[] valueTypes = { ValueType.TEXT };
		IDataSheet outData = new SingleRowSheet(columnNames, valueTypes);
		int res = ((IReadOnlyHandle) dbHandle).read(this.selectSql, values, outData);
		if (res == 0) {
			actionLogger.info(
					"No row found while reading from record {} for key value {} and hence no update.", this.recordName,
					keyValue);
			return false;
		}
		/*
		 * save this token into fieldNameOld. Service will take care of removing
		 * this on exit
		 */
		ctx.setValue(
				this.attachmentFieldName + AppConventions.Name.OLD_ATT_TOKEN_SUFFIX, outData.getRow(0)[0]);

		/*
		 * update row with new value
		 */
		Value[] updateValues = { tokenValue, keyValue };
		return ((ITransactionHandle) dbHandle).execute(this.updateSql, updateValues, false) > 0;
	}

	@Override
	public DbUsage getDbUsage() {
		return DbUsage.READ_WRITE;
	}

	@Override
	public void getReady(int idx, TransactionProcessor task) {
		super.getReady(idx, task);
		this.createSqls();
	}

	private void createSqls() {
		DbTable record = (DbTable) Application.getActiveInstance().getRecord(this.recordName);
		this.keyFieldName = record.getPrimaryKeyFields()[0].getName();

		String tableName = record.getTableName();
		String attColName = record.getField(this.attachmentFieldName).getExternalName();
		String keyColName = record.getField(this.keyFieldName).getExternalName();

		this.selectSql = "SELECT " + attColName + " FROM " + tableName + " WHERE " + keyColName + " =?";
		this.updateSql = "UPDATE " + tableName + " SET " + attColName + " = ? " + " WHERE " + keyColName + " =?";
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.kernel.trans.AbstractDbStep#validateDbStep(org.simplity.
	 * kernel.comp.IValidationContext, org.simplity.kernel.trans.Task)
	 */
	@Override
	protected void validateDbAction(IValidationContext vtx, TransactionProcessor task) {
		// TODO Auto-generated method stub

	}
}
