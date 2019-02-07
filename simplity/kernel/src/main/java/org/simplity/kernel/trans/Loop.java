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
import org.simplity.kernel.comp.IValidationContext;
import org.simplity.kernel.comp.ValidationMessage;
import org.simplity.kernel.data.AlreadyIteratingException;
import org.simplity.kernel.data.IDataSheet;
import org.simplity.kernel.data.IDataSheetIterator;
import org.simplity.kernel.idb.IDbHandle;
import org.simplity.kernel.service.ServiceContext;
import org.simplity.kernel.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loop through a set of actions for each row in a data sheet
 *
 * @author simplity.org
 */
public class Loop extends Block {
	private static final Logger logger = LoggerFactory.getLogger(Loop.class);

	/** data sheet on which to loop */
	String dataSheetName;

	/**
	 * for the loop, do you want to treat some columns as if they are fields in
	 * the collection? This feature helps in re-using services that assume
	 * fields as sub-service inside loops. * for all columns
	 */
	String[] columnsToCopyAsFields;

	/**
	 * in case the code inside the loop is updating some of the fields that are
	 * to be copied back to data sheet
	 */
	String[] fieldsToCopyBackAsColumns;

	/**
	 * in case this service is used as a batch processing kind of logic, then
	 * you may want to use this loop-block as a unit of work, and you are
	 * amenable to stop-work at this point, and not in-between
	 */
	boolean breakOnInterrupt;
	/** special case where we are to copy all columns as fields */
	private boolean copyAllColumnsToFields;

	/** special case where we are to copy back all fields into columns */
	private boolean copyBackAllColumns;

	@Override
	protected String execute(ServiceContext ctx, IDbHandle dbHandle, boolean transIsDelegated) {
		BlockWorker worker = new BlockWorker(this.actions, this.indexedActions, ctx, null, transIsDelegated);
		if (this.dataSheetName != null) {
			return this.loopOnSheet(worker, dbHandle, ctx);
		}
		if (this.executeOnCondition == null) {
			logger.info(
					"Loop action "
							+ this.getName()
							+ " has niether data sheet, nor condition. This is a run-for-ever loop,but could be interrupted");
		}
		return this.loopOnCondition(worker, dbHandle, ctx);
	}

	/**
	 * loop over this block for supplied expression/condition
	 *
	 * @param expr
	 * @param dbHandle
	 * @return true if normal completion. False if we encountered a STOP signal
	 */
	private String loopOnCondition(BlockWorker worker, IDbHandle dbHandle, ServiceContext ctx) {
		/*
		 * loop with a condition
		 */
		try {
			Value toContinue = Value.VALUE_TRUE;
			if (this.executeOnCondition != null) {
				toContinue = this.executeOnCondition.evaluate(ctx);
			}
			boolean interrupted = false;
			while (toContinue.toBoolean()) {
				if (this.breakOnInterrupt && Thread.interrupted()) {
					interrupted = true;
					break;
				}
				/*
				 * run the block
				 */
				String whatNext = worker.execute(dbHandle);
				if (whatNext != null) {
					if (TransConventions.JumpTo.STOP.equals(whatNext)) {
						/*
						 * signal to the caller that the execution should stop
						 */
						return whatNext;
					}
					if (TransConventions.JumpTo.BREAK_LOOP.equals(whatNext)) {
						/*
						 * signal for us to stop looping. Tell the caller to
						 * simply move to next action (outside the loop-block)
						 */
						return null;
					}
					if (TransConventions.JumpTo.NEXT_LOOP.equals(whatNext) == false) {
						/*
						 * asking us to jump out of the loop directly to some
						 * other action. Not allowed. for us to stop looping.
						 * Tell the caller to simply move to next action
						 * (outside the loop-block)
						 */
						throw new ApplicationError(
								"Service action inside a loop is signalling a jump to a task outside the loop.");
					}
				}
				/*
				 * should we continue?
				 */
				if (this.executeOnCondition != null) {
					toContinue = this.executeOnCondition.evaluate(ctx);
				}
			}
			if (interrupted) {
				logger.info("Coming out of loop because the thread is interrupted");
				Thread.currentThread().interrupt();
			}
			return null;
		} catch (Exception e) {
			throw new ApplicationError(
					e, "Error while evaluating " + this.executeOnCondition + " into a boolean value.");
		}
	}

	/**
	 * loop over this block for supplied data sheet
	 *
	 * @param expr
	 * @param dbHandle
	 * @return true if normal completion. False if we encountered a STOP signal
	 */
	private String loopOnSheet(BlockWorker worker, IDbHandle dbHandle, ServiceContext ctx) {
		IDataSheet ds = ctx.getDataSheet(this.dataSheetName);
		if (ds == null) {
			logger.info(
					"Data Sheet "
							+ this.dataSheetName
							+ " not found in the context. Loop action has no work.");
			return null;
		}
		if (ds.length() == 0) {
			logger.info("Data Sheet " + this.dataSheetName + " has no data. Loop action has no work.");
			return null;
		}
		IDataSheetIterator iterator = null;
		try {
			iterator = ctx.startIteration(this.dataSheetName);
		} catch (AlreadyIteratingException e) {
			throw new ApplicationError(
					"Loop action is designed to iterate on data sheet "
							+ this.dataSheetName
							+ " but that data sheet is already iterating as part of an enclosing loop action.");
		}
		/*
		 * are we to copy columns as fields?
		 */
		Value[] savedValues = null;
		if (this.columnsToCopyAsFields != null) {
			savedValues = this.saveFields(ctx, ds);
		}
		String result = null;
		int idx = 0;
		boolean interrupted = false;
		while (iterator.moveToNextRow()) {
			if (this.breakOnInterrupt && Thread.interrupted()) {
				interrupted = true;
				break;
			}
			if (this.columnsToCopyAsFields != null) {
				this.copyToFields(ctx, ds, idx);
			}

			String whatNext = worker.execute(dbHandle);
			if (this.fieldsToCopyBackAsColumns != null) {
				this.copyToColumns(ctx, ds, idx);
			}
			if (whatNext != null) {
				if (TransConventions.JumpTo.STOP.equals(whatNext)) {
					/*
					 * signal to the caller that the execution should stop
					 */
					iterator.cancelIteration();
					result = whatNext;
					break;
				}
				if (TransConventions.JumpTo.BREAK_LOOP.equals(whatNext)) {
					/*
					 * signal for us to stop looping. Tell the caller to simply
					 * move to next action (outside the loop-block)
					 */
					iterator.cancelIteration();
					break;
				}
				if (TransConventions.JumpTo.NEXT_LOOP.equals(whatNext) == false) {
					/*
					 * asking us to jump out of the loop directly to some other
					 * action. Not allowed. for us to stop looping. Tell the
					 * caller to simply move to next action (outside the
					 * loop-block)
					 */
					throw new ApplicationError(
							"Service action inside a loop is signalling a jump to a task outside the loop.");
				}
			}
			idx++;
		}
		if (savedValues != null) {
			this.restoreFields(ctx, ds, savedValues);
		}
		if (interrupted) {
			logger.info("Coming out of loop because the thread is interrupted");
			Thread.currentThread().interrupt();
		}
		return result;
	}

	/**
	 * @param ctx
	 */
	private void copyToColumns(ServiceContext ctx, IDataSheet ds, int idx) {
		if (this.copyBackAllColumns) {
			/*
			 * slightly optimized over getting individual columns..
			 */
			Value[] values = ds.getRow(idx);
			int i = 0;
			for (String fieldName : ds.getColumnNames()) {
				values[i++] = ctx.getValue(fieldName);
			}
			return;
		}
		for (String fieldName : this.fieldsToCopyBackAsColumns) {
			ds.setColumnValue(fieldName, idx, ctx.getValue(fieldName));
		}
	}

	/**
	 * @param ctx
	 */
	private void restoreFields(ServiceContext ctx, IDataSheet ds, Value[] values) {
		int i = 0;
		if (this.copyAllColumnsToFields) {
			for (String fieldName : ds.getColumnNames()) {
				Value value = values[i++];
				if (value != null) {
					ctx.setValue(fieldName, value);
				}
			}
		} else {
			for (String fieldName : this.columnsToCopyAsFields) {
				Value value = values[i++];
				if (value != null) {
					ctx.setValue(fieldName, value);
				}
			}
		}
	}

	/**
	 * @param ctx
	 */
	private void copyToFields(ServiceContext ctx, IDataSheet ds, int idx) {
		if (this.copyAllColumnsToFields) {
			/*
			 * slightly optimized over getting individual columns..
			 */
			Value[] values = ds.getRow(idx);
			int i = 0;
			for (String fieldName : ds.getColumnNames()) {
				ctx.setValue(fieldName, values[i++]);
			}
			return;
		}
		for (String fieldName : this.columnsToCopyAsFields) {
			ctx.setValue(fieldName, ds.getColumnValue(fieldName, idx));
		}
	}

	/**
	 * @param ctx
	 * @return
	 */
	private Value[] saveFields(ServiceContext ctx, IDataSheet ds) {
		if (this.copyAllColumnsToFields) {
			Value[] values = new Value[ds.width()];
			int i = 0;
			for (String fieldName : ds.getColumnNames()) {
				values[i++] = ctx.getValue(fieldName);
			}
			return values;
		}
		Value[] values = new Value[this.columnsToCopyAsFields.length];
		for (int i = 0; i < values.length; i++) {
			values[i] = ctx.getValue(this.columnsToCopyAsFields[i]);
		}
		return values;
	}

	@Override
	public void getReady(int idx, TransactionProcessor task) {
		super.getReady(idx, task);

		if (this.columnsToCopyAsFields != null) {
			if (this.columnsToCopyAsFields.length == 1 && this.columnsToCopyAsFields[0].equals("*")) {
				this.copyAllColumnsToFields = true;
			}
		}

		if (this.fieldsToCopyBackAsColumns != null) {
			if (this.fieldsToCopyBackAsColumns.length == 1
					&& this.fieldsToCopyBackAsColumns[0].equals("*")) {
				this.copyBackAllColumns = true;
			}
		}
	}

	@Override
	public void validateSpecific(IValidationContext vtx, TransactionProcessor task) {
		super.validateSpecific(vtx, task);
		if (this.dataSheetName == null) {
			if (this.executeOnCondition == null) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_WARNING,
						"An infinite loop is used in the service. Ensure that you jump out with an onSuccessGoTo or onFailureGoTo. In case this service is used as a background job that runs for ever, then breakOnInterrupt should be used.",
						"breakOnInterrupt"));
			}
		} else if (this.executeOnCondition != null) {
			vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_INFO,
					"Loop action is for each row of a sheet, but it also specifies executeOnCondition. Note that the executeOnCondition is checked only once in the beginning to decide whether to start the loop at all. It is not checked for further itertions per row. Change your design if this is not the intended behaviour",
					"executeOnCondition"));
		}
		if (this.executeIfNoRowsInSheet != null) {
			vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
					"executeIfNoRowsInSheet is invalid for loopaction.", "executeIfNoRowsInSheet"));
		}
		if (this.executeIfRowsInSheet != null) {
			vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
					"executeIfRowsInSheet is invalid for loopaction.", "executeIfRowsInSheet"));
		}
	}
}
