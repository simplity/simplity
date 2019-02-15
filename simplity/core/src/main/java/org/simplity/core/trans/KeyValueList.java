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
package org.simplity.core.trans;

import org.simplity.core.app.AppConventions;
import org.simplity.core.app.Application;
import org.simplity.core.comp.ComponentType;
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.dm.DbTable;
import org.simplity.core.dm.Record;
import org.simplity.core.idb.IDbHandle;
import org.simplity.core.idb.IReadOnlyHandle;
import org.simplity.core.rdb.DbUsage;
import org.simplity.core.service.ServiceContext;

/**
 * typical list of values for a drop-down. action is designed in case you need
 * this in addition to other things. In case you have a service with only one
 * keyValueList action, consider using a record based on-the-fly service
 *
 * @author simplity.org
 */
public class KeyValueList extends AbstractDbAction {

	/** record that is to be used */
	@FieldMetaData(isRequired = true, isReferenceToComp = true, referredCompType = ComponentType.REC)
	String recordName;
	/** defaults to setting in the record */
	String outputSheetName;

	/**
	 * default used by component manager
	 */
	public KeyValueList() {
		// default constructor is required for component creation process
	}

	/**
	 * list action for the record
	 *
	 * @param record
	 */
	public KeyValueList(Record record) {
		this.recordName = record.getQualifiedName();
		this.outputSheetName = record.getDefaultSheetName();
	}

	@Override
	protected boolean actWithDb(ServiceContext ctx, IDbHandle dbHandle) {
		DbTable record = (DbTable) Application.getActiveInstance().getRecord(this.recordName);
		String value = null;
		String keyName = record.getValueListKeyName();
		if (keyName != null) {
			value = ctx.getTextValue(keyName);
			if (value == null) {
				value = ctx.getTextValue(AppConventions.Name.LIST_SERVICE_KEY);
			}
		}
		IDataSheet sheet = record.list(value, (IReadOnlyHandle) dbHandle, ctx.getUserId());
		if (sheet == null) {
			return false;
		}
		String sheetName = this.outputSheetName == null ? record.getDefaultSheetName() : this.outputSheetName;
		ctx.putDataSheet(sheetName, sheet);
		return sheet.length() > 0;
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
