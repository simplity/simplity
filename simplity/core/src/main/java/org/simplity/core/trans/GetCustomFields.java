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

import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.data.Fields;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.data.IFieldsCollection;
import org.simplity.core.dm.DbTable;
import org.simplity.core.idb.IDbHandle;
import org.simplity.core.rdb.DbUsage;
import org.simplity.core.rdb.ReadOnlyHandle;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.value.Value;

/**
 * get custom fields for a table
 *
 * @author simplity.org
 */
public class GetCustomFields extends AbstractDbAction {
	/**
	 * name of table for which custom fields are to be retrieved
	 */
	@FieldMetaData(isRequired = true)
	String tableName;
	/**
	 * sheet name to which field custom details are to be extracted. defaults to
	 * _customField
	 */
	String outputSheetName;

	@Override
	protected boolean actWithDb(ServiceContext ctx, IDbHandle handle) {
		DbTable record = DbTable.getCustomFieldsTable();
		IFieldsCollection fields = new Fields();
		fields.setValue("tableName", Value.newTextValue(this.tableName));
		fields.setValue("tenantId", ctx.getTenantId());

		IDataSheet result = record.filter(record, fields, (ReadOnlyHandle) handle, ctx.getUserId());
		ctx.putDataSheet(this.outputSheetName, result);
		return true;
	}

	@Override
	public DbUsage getDbUsage() {
		return DbUsage.READ_ONLY;
	}

	@Override
	protected void validateDbAction(IValidationContext vtx, TransactionProcessor task) {
		//
	}
}
