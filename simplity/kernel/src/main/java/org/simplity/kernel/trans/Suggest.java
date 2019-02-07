/*
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
package org.simplity.kernel.trans;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.app.AppConventions;
import org.simplity.kernel.app.Application;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.FieldMetaData;
import org.simplity.kernel.comp.IValidationContext;
import org.simplity.kernel.data.IDataSheet;
import org.simplity.kernel.dm.DbTable;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.idb.IDbHandle;
import org.simplity.kernel.idb.IReadOnlyHandle;
import org.simplity.kernel.rdb.DbUsage;
import org.simplity.kernel.service.ServiceContext;
import org.simplity.kernel.value.InvalidValueException;
import org.simplity.kernel.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Get suggestions from the table associated with a record based on the inputs.
 * This action is "auto-generated" for on-the-fly service, but action is useful
 * for a service that has to do other things as well. This is a thin wrapper on
 * record.suggest()
 *
 * @author simplity.org
 */
public class Suggest extends AbstractDbAction {
	private static final Logger logger = LoggerFactory.getLogger(Suggest.class);

	/** record that is to be used */
	@FieldMetaData(isRequired = true, isReferenceToComp = true, referredCompType = ComponentType.REC)
	String recordName;
	/** field to filter on. Defaults to specification in record */
	String fieldToMatch;
	/** name of the output data sheet */
	String outputSheetName;

	/** default */
	public Suggest() {
	}

	/**
	 * suggestion action for the record
	 *
	 * @param record
	 */
	public Suggest(Record record) {
		this.actionName = "suggest" + record.getSimpleName();
		this.recordName = record.getQualifiedName();
	}

	@Override
	protected boolean actWithDb(ServiceContext ctx, IDbHandle dbHandle) {
		DbTable record = (DbTable) Application.getActiveInstance().getRecord(this.recordName);
		Value value = ctx.getValue(this.fieldToMatch);
		if (value == null) {

			logger.info(
					"No value is available in field "
							+ this.fieldToMatch
							+ " for us to suggest. No suggestions sent to client");

			return false;
		}
		boolean matchStarting = false;
		Value v = ctx.getValue(AppConventions.Name.SUGGEST_STARTING);
		try {
			if (v != null && v.toBoolean()) {
				matchStarting = true;
			}
		} catch (InvalidValueException e) {

			logger.info(
					"we expected boolean value in "
							+ AppConventions.Name.SUGGEST_STARTING
							+ " but encountered "
							+ v
							+ ". Assumed false value.");
		}
		IDataSheet sheet = record.suggest(value.toString(), matchStarting, (IReadOnlyHandle) dbHandle, ctx.getUserId());
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
	public void getReady(int idx, TransactionProcessor task) {
		super.getReady(idx, task);
		if (this.recordName == null) {
			throw new ApplicationError("Suggest action requires recordName");
		}
		if (this.fieldToMatch == null) {
			this.fieldToMatch = AppConventions.Name.LIST_SERVICE_KEY;
		}
	}

	@Override
	protected void validateDbAction(IValidationContext vtx, TransactionProcessor task) {
		//
	}
}
