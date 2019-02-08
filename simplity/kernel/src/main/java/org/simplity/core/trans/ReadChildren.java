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

import org.simplity.core.app.Application;
import org.simplity.core.comp.ComponentType;
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.dm.DbTable;
import org.simplity.core.idb.IDbHandle;
import org.simplity.core.idb.IReadOnlyHandle;
import org.simplity.core.rdb.DbUsage;
import org.simplity.core.service.ServiceContext;

/**
 * Read rows from a record for a given value of its parent key
 *
 * @author simplity.org
 */
public class ReadChildren extends AbstractDbAction {

	/** qualified record name */
	@FieldMetaData(isRequired = true, isReferenceToComp = true, referredCompType = ComponentType.REC)
	String recordName;
	/**
	 * sheet name in which output is read into. defaults to simple name of
	 * record
	 */
	String outputSheetName;

	@Override
	protected boolean actWithDb(ServiceContext ctx, IDbHandle dbHandle) {
		DbTable record = (DbTable) Application.getActiveInstance().getRecord(this.recordName);
		IDataSheet outSheet = record.filterForAParent(ctx, (IReadOnlyHandle) dbHandle);
		ctx.putDataSheet(this.outputSheetName, outSheet);
		return outSheet.length() > 0;
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
