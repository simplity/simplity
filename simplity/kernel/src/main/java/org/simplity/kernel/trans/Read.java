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
package org.simplity.kernel.trans;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.app.Application;
import org.simplity.kernel.comp.IValidationContext;
import org.simplity.kernel.data.IDataSheet;
import org.simplity.kernel.dm.DbTable;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.idb.IDbHandle;
import org.simplity.kernel.rdb.DbUsage;
import org.simplity.kernel.rdb.ReadonlyHandle;
import org.simplity.kernel.service.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read a row from a record, and possibly read relevant rows from related
 * records
 *
 * @author simplity.org
 */
public class Read extends AbstractDbAction {
	private static final Logger logger = LoggerFactory.getLogger(Read.class);

	/** qualified record name */
	String recordName;
	/** sheet in which input is expected. defaults to simple name of record */
	String inputSheetName;
	/**
	 * sheet name in which output is read into. defaults to simple name of
	 * record
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

	/** default constructor used by the ComponentManager */
	public Read() {
		// default
	}

	/**
	 * get an action to read this record possibly with child rows
	 *
	 * @param record
	 *            non-null
	 */
	public Read(DbTable record) {
		this.recordName = record.getQualifiedName();
		this.actionName = "read_" + record.getSimpleName();
		this.cascadeFilterForChildren = true;
	}

	/**
	 * create an action to read this record with specific set of child records
	 *
	 * @param record
	 * @param children
	 */
	public Read(Record record, RelatedRecord[] children) {
		this.recordName = record.getQualifiedName();
		this.actionName = "read_" + record.getSimpleName();
		this.cascadeFilterForChildren = true;
		this.childRecords = children;
	}

	@Override
	protected boolean actWithDb(ServiceContext ctx, IDbHandle handle) {
		ReadonlyHandle dbHandle = (ReadonlyHandle) handle;
		Application app = Application.getActiveInstance();
		DbTable record = (DbTable) app.getRecord(this.recordName);
		IDataSheet inSheet = null;
		int result = 0;
		if (this.inputSheetName != null && ctx.hasDataSheet(this.inputSheetName)) {
			inSheet = ctx.getDataSheet(this.inputSheetName);
		}
		int nbrInputs = inSheet == null ? 1 : inSheet.length();
		if (nbrInputs > 1 && this.outputSheetName != null) {
			throw new ApplicationError(
					"Read action is trying to read more than one rows, but has not specified outsheet.");
		}
		IDataSheet outSheet = null;
		if (inSheet == null) {
			outSheet = record.readOne(ctx, dbHandle, ctx.getUserId());
		} else {
			outSheet = record.readMany(inSheet, dbHandle, ctx.getUserId());
		}
		if (outSheet == null || outSheet.length() == 0) {
			return false;
		}
		if (this.outputSheetName != null) {
			ctx.putDataSheet(this.outputSheetName, outSheet);
		} else {
			ctx.copyFrom(outSheet);
		}
		if (result == 0) {
			return false;
		}
		if (this.childRecords != null) {
			for (RelatedRecord rr : this.childRecords) {
				DbTable cr = (DbTable) app.getRecord(rr.recordName);
				logger.info("Going to read child record ");

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
	public DbUsage getDbUsage() {
		return DbUsage.READ_ONLY;
	}

	@Override
	public void getReady(int idx, TransactionProcessor task) {
		super.getReady(idx, task);
		if (this.childRecords != null) {
			for (RelatedRecord rec : this.childRecords) {
				rec.getReady();
			}
		}
	}

	@Override
	public void validateDbAction(IValidationContext vtx, TransactionProcessor task) {
		if (this.childRecords != null) {
			for (RelatedRecord rec : this.childRecords) {
				rec.validate(vtx);
			}
		}
	}
}
