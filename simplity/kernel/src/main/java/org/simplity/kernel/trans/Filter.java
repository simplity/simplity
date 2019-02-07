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
import org.simplity.kernel.dm.DbTable;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.idb.IDbHandle;
import org.simplity.kernel.idb.IReadOnlyHandle;
import org.simplity.kernel.rdb.DbUsage;
import org.simplity.kernel.service.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read a row from a record, and possibly read relevant rows from related
 * records
 *
 * @author simplity.org
 */
public class Filter extends AbstractDbAction {
	private static final Logger actionLogger = LoggerFactory.getLogger(Filter.class);

	/** record that is used for inputting and creating filter criteria */
	@FieldMetaData(isReferenceToComp = true, referredCompType = ComponentType.REC)
	String filterRecordName;
	/** optional. defaults to filterRecordName */
	@FieldMetaData(isReferenceToComp = true, referredCompType = ComponentType.REC)
	String outputRecordName;
	/**
	 * name of the sheet in which data is received. if null, we take data from
	 * fields. Sheet can not contain more than one rows
	 */
	String inputSheetName;
	/**
	 * name of the sheet in which output is sent. Defaults to simple name of
	 * outputRecordName
	 */
	String outputSheetName;
	/**
	 * child records from which to read rows, for the row read in this record
	 */
	RelatedRecord[] childRecords;

	/**
	 * should child records for this filter/record be filtered automatically?
	 */
	boolean cascadeFilterForChildren;

	/** default constructor used by the component manager */
	public Filter() {
	}

	/**
	 * get a default filterAction for a record, possibly with child rows
	 *
	 * @param record
	 */
	public Filter(Record record) {
		String recordName = record.getQualifiedName();
		this.filterRecordName = recordName;
		this.outputRecordName = recordName;
		this.outputSheetName = record.getDefaultSheetName();
		this.cascadeFilterForChildren = true;
	}

	@Override
	protected boolean actWithDb(ServiceContext ctx, IDbHandle handle) {
		IReadOnlyHandle dbHandle = (IReadOnlyHandle) handle;
		Application app = Application.getActiveInstance();
		DbTable record = (DbTable) app.getRecord(this.filterRecordName);
		DbTable outRecord = record;
		if (this.outputRecordName != null) {
			outRecord = (DbTable) app.getRecord(this.outputRecordName);
		}

		IDataSheet outSheet = null;

		if (this.inputSheetName == null) {
			outSheet = outRecord.filter(record, ctx, dbHandle, ctx.getUserId());
		} else {
			IDataSheet inSheet = ctx.getDataSheet(this.inputSheetName);
			if (inSheet == null) {
				actionLogger.info("Filter Action " + this.getName() + " did not execute because input sheet "
						+ this.inputSheetName + " is not found.");

				return false;
			}

			outSheet = outRecord.filter(record, inSheet, dbHandle, ctx.getUserId());
		}
		int result = outSheet.length();
		if (this.outputSheetName == null) {
			if (result == 0) {
				return false;
			}
			ctx.copyFrom(outSheet);
			result = 1;
		} else {
			ctx.putDataSheet(this.outputSheetName, outSheet);
		}
		if (result == 0) {
			return false;
		}
		if (this.childRecords != null) {
			for (RelatedRecord rr : this.childRecords) {
				DbTable cr = (DbTable) app.getRecord(rr.recordName);
				cr.filterForParents(outSheet, dbHandle, rr.sheetName, this.cascadeFilterForChildren, ctx);
			}
			return result > 0;
		}
		if (this.cascadeFilterForChildren) {
			record.filterChildRecords(outSheet, dbHandle, ctx);
		}
		return result > 0;
	}

	@Override
	protected void validateDbAction(IValidationContext vtx, TransactionProcessor task) {
		if (this.childRecords != null) {
			for (RelatedRecord rec : this.childRecords) {
				rec.validate(vtx);
			}
		}
	}

	@Override
	public DbUsage getDbUsage() {
		return DbUsage.READ_ONLY;
	}
}
