/*
 * Copyright (c) 2015 EXILANT Technologies Private Limited (www.exilant.com)
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

import org.simplity.core.app.Application;
import org.simplity.core.comp.ComponentType;
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.dm.DbTable;
import org.simplity.core.idb.IDbHandle;
import org.simplity.core.idb.IReadOnlyHandle;
import org.simplity.core.rdb.DbUsage;
import org.simplity.core.service.ServiceContext;

/**
 * Check if a row exists in this record for the primary key
 *
 * @author simplity.org
 */
public class RowExists extends AbstractDbAction {

	/** qualified record name */
	@FieldMetaData(isRequired = true, isReferenceToComp = true, referredCompType = ComponentType.REC)
	String recordName;

	/**
	 * specify the field that has the value for the key. Defaults to key
	 * specified in record. In case the primary is a composite key, then this
	 * feature is not applicable. Field names are always taken from record.
	 */
	String fieldName;

	@Override
	protected boolean actWithDb(ServiceContext ctx, IDbHandle dbHandle) {
		DbTable record = (DbTable) Application.getActiveInstance().getRecord(this.recordName);
		boolean result = record.rowExistsForKey(ctx, this.fieldName, (IReadOnlyHandle) dbHandle, ctx.getUserId());
		if (result) {
			return true;
		}
		return false;
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
