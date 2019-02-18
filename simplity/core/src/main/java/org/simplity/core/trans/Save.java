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

import org.simplity.core.ApplicationError;
import org.simplity.core.app.Application;
import org.simplity.core.comp.ComponentType;
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.dm.DbTable;
import org.simplity.core.dm.Record;
import org.simplity.core.dm.SaveActionType;
import org.simplity.core.idb.IDbHandle;
import org.simplity.core.idb.ITransactionHandle;
import org.simplity.core.rdb.DbUsage;
import org.simplity.core.rdb.TransactionHandle;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Save (add/modify/delete) a row from a record, and possibly save relevant rows
 * from related records
 *
 * @author simplity.org
 */
public class Save extends AbstractDbAction {
	private static final Logger logger = LoggerFactory.getLogger(Save.class);

	/** qualified record name */
	@FieldMetaData(isRequired = true, isReferenceToComp = true, referredCompType = ComponentType.REC)
	String recordName;

	/**
	 * if this is for more than one rows, and the data is to be received in a
	 * sheet
	 */
	String inputSheetName;

	/**
	 * add/update/delete/auto/replace . Auto means update if primary key is
	 * specified, else add
	 */
	SaveActionType saveAction = SaveActionType.SAVE;

	/** do we save child records */
	RelatedRecord[] childRecords;

	/**
	 * at times, you may design an insert operation that will try to insert,
	 * failing which you may want to update. In such cases, you may get a sql
	 * error on key-violation. By default we would raise an exception. You may
	 * alter this behavior with this keyword.
	 */
	boolean treatSqlErrorAsNoResult;

	/** default constructor */
	public Save() {
		//
	}

	/**
	 * get a save action for this record
	 *
	 * @param record
	 * @param children
	 * @param inputIsSheet
	 *            true if input data to be saved is in a sheet, false if input
	 *            as fields from ctx
	 */
	public Save(Record record, RelatedRecord[] children, boolean inputIsSheet) {
		if (inputIsSheet) {
			this.inputSheetName = record.getDefaultSheetName();
		}
		this.recordName = record.getQualifiedName();
		this.childRecords = children;
	}

	@Override
	protected boolean actWithDb(ServiceContext ctx, IDbHandle handle) {
		ITransactionHandle dbHandle = (ITransactionHandle) handle;
		if (this.inputSheetName == null) {
			return this.saveFromFields(ctx, dbHandle) > 0;
		}
		IDataSheet inSheet = ctx.getDataSheet(this.inputSheetName);
		if (inSheet == null) {
			return this.saveFromFields(ctx, dbHandle) > 0;
		}
		int nbrRows = inSheet.length();

		logger.info("Starting save for a sheet with " + nbrRows);

		if (nbrRows == 0) {
			return false;
		}

		DbTable record = (DbTable) Application.getActiveInstance().getRecord(this.recordName);
		int nbrRowsAffected = 0;
		SaveActionType[] actions = null;
		Value userId = ctx.getUserId();
		if (this.saveAction == SaveActionType.MODIFY) {
			nbrRowsAffected = record.update(inSheet, dbHandle, userId, false);
		} else if (this.saveAction == SaveActionType.ADD) {
			nbrRowsAffected = record.insert(inSheet, dbHandle, userId, false);
		} else if (this.saveAction == SaveActionType.DELETE) {
			nbrRowsAffected = record.delete(inSheet, dbHandle, false);
		} else {
			actions = record.saveMany(inSheet, dbHandle, userId, false);
			nbrRowsAffected = actions.length;
		}
		if (this.childRecords != null) {

			logger.info(
					"Child records are valid only when parent is for a single row. Data if any, ignored.");
		}
		return nbrRowsAffected > 0;
	}

	/**
	 * TODO: this is a clone of the above. we have to think and re-factor this
	 * to avoid this duplication
	 *
	 * @param ctx
	 * @param dbHandle
	 * @return
	 */
	private int saveFromFields(ServiceContext ctx, IDbHandle handle) {
		TransactionHandle dbHandle = (TransactionHandle) handle;
		DbTable record = (DbTable) Application.getActiveInstance().getRecord(this.recordName);
		int nbrRowsAffected = 0;
		Value userId = ctx.getUserId();
		/*
		 * if action is 'save' it will be set to either add or modify later
		 */
		SaveActionType action = this.saveAction;
		if (action == SaveActionType.DELETE) {
			if (this.childRecords == null) {
				return record.delete(ctx, dbHandle, this.treatSqlErrorAsNoResult);
			}
			/*
			 * it is quite likely that the rdbms designer would have put
			 * constraints on child rows. So let us check with child rows before
			 * deleting this row
			 */
			int nbrChildrenSaved = this.saveChildRows(ctx, dbHandle, userId, action);
			nbrRowsAffected = record.delete(ctx, dbHandle, this.treatSqlErrorAsNoResult);
			if (nbrRowsAffected == 0 && nbrChildrenSaved > 0) {
				/*
				 * ooops. we deleted child rows, but parent is not deleted, may
				 * be because of time-stamp..
				 */
				throw new ApplicationError(
						"Data was changed by some one else while you were editing it. Please cancel this operation and redo it with latest data.");
			}
			return nbrRowsAffected + nbrChildrenSaved;
		}
		/*
		 * it is either update or insert, implicitly or explicitly
		 */
		if (action == SaveActionType.MODIFY) {
			nbrRowsAffected = record.update(ctx, dbHandle, userId, this.treatSqlErrorAsNoResult);
		} else if (action == SaveActionType.ADD) {
			nbrRowsAffected = record.insert(ctx, dbHandle, userId, this.treatSqlErrorAsNoResult);
		} else {
			action = record.saveOne(ctx, dbHandle, userId, this.treatSqlErrorAsNoResult);
			nbrRowsAffected = action == null ? 0 : 1;
		}
		if (nbrRowsAffected > 0 && this.childRecords != null) {
			nbrRowsAffected += this.saveChildRows(ctx, dbHandle, userId, action);
		}
		return nbrRowsAffected;
	}

	/**
	 * process child rows when a parent is affected
	 *
	 * @param ctx
	 * @param dbHandle
	 * @param userId
	 * @param action
	 * @return
	 */
	private int saveChildRows(
			ServiceContext ctx, ITransactionHandle dbHandle, Value userId, SaveActionType action) {
		int nbr = 0;
		for (RelatedRecord rr : this.childRecords) {
			DbTable record = (DbTable) Application.getActiveInstance().getRecord(rr.recordName);
			IDataSheet relatedSheet = ctx.getDataSheet(rr.sheetName);
			if (rr.replaceRows) {
				nbr += record.deleteWithParent(ctx, dbHandle, userId);
				if (relatedSheet == null || relatedSheet.length() == 0) {

					logger.info(
							"Rows in record "
									+ rr.recordName
									+ " deleted, as there were no rows to replace them.");

					continue;
				}
				nbr += record.insertWithParent(relatedSheet, ctx, dbHandle, userId);

				logger.info(
						"Rows in record "
								+ rr.recordName
								+ " replaced based on "
								+ relatedSheet.length()
								+ " rows of data in sheet "
								+ rr.sheetName);

				continue;
			}

			if (relatedSheet == null || relatedSheet.length() == 0) {

				logger.info(
						"Related record "
								+ rr.recordName
								+ " not saved as there is no data in sheet "
								+ rr.sheetName);

				continue;
			}

			logger.info(
					"Saving children is a noble cause!! Going to save child record "
							+ rr.recordName
							+ " for action = "
							+ action);

			if (action == SaveActionType.ADD) {
				nbr += record.insertWithParent(relatedSheet, ctx, dbHandle, userId);
			} else if (action == SaveActionType.DELETE) {
				nbr += record.deleteWithParent(relatedSheet, dbHandle, userId);
			} else if (rr.replaceRows) {
				nbr += record.deleteWithParent(ctx, dbHandle, userId);
				nbr += record.insertWithParent(relatedSheet, ctx, dbHandle, userId);
			} else {
				/*
				 * when the parent is modified, child rows could be update as
				 * well as insert/delete!!
				 */
				SaveActionType[] actions = { action };
				nbr += record.saveWithParent(relatedSheet, ctx, actions, dbHandle, userId);
			}
		}
		return nbr;
	}

	@Override
	public DbUsage getDbUsage() {
		return DbUsage.READ_WRITE;
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
	protected void validateDbAction(IValidationContext vtx, TransactionProcessor task) {
		if (this.childRecords != null) {
			for (RelatedRecord rec : this.childRecords) {
				rec.validate(vtx);
			}
		}
	}
}
