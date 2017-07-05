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
package org.simplity.tp;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.dm.Record;
import org.simplity.service.ServiceContext;

/**
 * Read a row from a record, and possibly read relevant rows from related
 * records
 *
 *
 * @author simplity.org
 *
 */
public class Read extends DbAction {

	/**
	 * qualified record name
	 */
	String recordName;
	/**
	 * sheet in which input is expected. defaults to simple name of record
	 */
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

	/**
	 * default constructor
	 */
	public Read() {
		// default
	}

	/**
	 * get an action to read this record possibly with child rows
	 *
	 * @param record
	 *            non-null
	 */
	public Read(Record record) {
		this.recordName = record.getQualifiedName();
		this.actionName = "read_" + record.getSimpleName();
		this.cascadeFilterForChildren = true;
	}
	
	public Read(Record record,RelatedRecord[] children) {
		this.recordName = record.getQualifiedName();
		this.actionName = "read_" + record.getSimpleName();
		this.cascadeFilterForChildren = true;
		this.childRecords = children;
	}

	@Override
	protected int doDbAct(ServiceContext ctx, DbDriver driver) {
		Record record = ComponentManager.getRecord(this.recordName);
		DataSheet inSheet = null;
		int result = 0;
		if (this.inputSheetName != null
				&& ctx.hasDataSheet(this.inputSheetName)) {
			inSheet = ctx.getDataSheet(this.inputSheetName);
		}
		int nbrInputs = inSheet == null ? 1 : inSheet.length();
		if (nbrInputs > 1 && this.outputSheetName != null) {
			throw new ApplicationError(
					"Read action is trying to read more than one rows, but has not specified outsheet.");
		}
		DataSheet outSheet = record.createSheet(nbrInputs == 1, false);
		if (inSheet == null) {
			result = record.readOne(ctx, outSheet, driver, ctx.getUserId());
		} else {
			result = record
					.readMany(inSheet, outSheet, driver, ctx.getUserId());
		}
		if (result == 0) {
			return 0;
		}
		if (this.outputSheetName != null) {
			ctx.putDataSheet(this.outputSheetName, outSheet);
		} else {
			ctx.copyFrom(outSheet);
		}
		if (result == 0) {
			return 0;
		}
		if (this.childRecords != null) {
			for (RelatedRecord rr : this.childRecords) {
				Record cr = ComponentManager.getRecord(rr.recordName);
				Tracer.trace("Going to read child record ");
				cr.filterForParents(outSheet, driver, rr.sheetName,
						this.cascadeFilterForChildren, ctx);
			}
			return result;
		}
		if (this.cascadeFilterForChildren) {
			record.filterChildRecords(outSheet, driver, ctx);
		}
		return result;
	}

	@Override
	public DbAccessType getDataAccessType() {
		return DbAccessType.READ_ONLY;
	}

	@Override
	public void getReady(int idx, Service service) {
		super.getReady(idx, service);
		if (this.childRecords != null) {
			for (RelatedRecord rec : this.childRecords) {
				rec.getReady();
			}
		}
	}

	@Override
	public int validate(ValidationContext ctx, Service service) {
		int count = super.validate(ctx, service);
		if (this.recordName == null) {
			ctx.addError("Record name is required for read action");
			count++;
		} else {
			ctx.addReference(ComponentType.REC, this.recordName);
		}
		if (this.childRecords != null) {
			for (RelatedRecord rec : this.childRecords) {
				count += rec.validate(ctx);
			}
		}
		return count;
	}
}
