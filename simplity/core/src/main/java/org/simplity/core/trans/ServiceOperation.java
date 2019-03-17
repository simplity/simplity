/*
 * Copyright (c) 2019 simplity.org
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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.core.trans;

import org.simplity.core.app.AppConventions;
import org.simplity.core.data.DataPurpose;
import org.simplity.core.dm.DbTable;
import org.simplity.core.dt.BooleanDataType;
import org.simplity.core.dt.TextDataType;
import org.simplity.core.msg.Messages;
import org.simplity.core.rdb.DbUsage;
import org.simplity.core.service.DataStructureType;
import org.simplity.core.service.InputData;
import org.simplity.core.service.InputField;
import org.simplity.core.service.InputRecord;
import org.simplity.core.service.OutputData;
import org.simplity.core.service.OutputRecord;

/**
 * designed as enumeration, but idea is to delegate some static functionality
 * out of Service class
 *
 * @author simplity.org
 *
 */
public enum ServiceOperation {
	/**
	 * read/get a row based on primary key value
	 */
	GET {
		@Override
		public Service generateService(DbTable record) {
			String recordName = record.getQualifiedName();

			/*
			 * what is to be input
			 */
			InputRecord[] inRecs = {
					InputRecord.getInputRecord(recordName, record.getDefaultSheetName(), DataPurpose.READ, false) };
			InputData inData = new InputData();
			inData.setRecords(inRecs);

			/*
			 * We have just one action : read action
			 */
			AbstractAction action = new Read(record, record.getChildRecordsAsRelatedRecords(true));
			action.failureMessageName = Messages.NO_ROWS;

			AbstractAction[] actions = { action };
			TransactionProcessor task = new TransactionProcessor();
			task.actions = actions;
			task.dbUsage = DbUsage.READ_ONLY;
			task.schemaName = record.getSchemaName();

			/*
			 * output fields from record.
			 */
			OutputData outData = new OutputData();
			outData.setOutputRecords(record.getOutputRecords(false));

			Service service = new Service();
			service.inputData = inData;
			service.processor = task;
			service.outputData = outData;
			return service;
		}
	},
	/**
	 * insert/update/delete
	 */
	SAVE {
		@Override
		public Service generateService(DbTable record) {
			String recordName = record.getQualifiedName();

			/*
			 * data for this record is expected in fields, while rows for
			 * child-records in data sheets
			 */
			InputData inData = new InputData();
			inData.setRecords(record.getInputRecords());

			/*
			 * what should we output? We are not sure.
			 *
			 * May be we should read back the row that is updated?
			 *
			 * As of now, we just send back data that is received
			 */
			OutputRecord outRec = new OutputRecord();
			outRec.setRecordName(recordName);
			OutputData outData = new OutputData();
			OutputRecord[] outRecs = { outRec };
			outData.setOutputRecords(outRecs);

			/*
			 * processing
			 */
			RelatedRecord[] rrs = record.getChildRecordsAsRelatedRecords(false);
			Save action = new Save(record, rrs, false);
			action.failureMessageName = Messages.NO_UPDATE;
			AbstractAction[] actions = { action };
			TransactionProcessor task = new TransactionProcessor();
			task.actions = actions;
			task.dbUsage = DbUsage.READ_WRITE;
			task.schemaName = record.getSchemaName();

			Service service = new Service();
			service.inputData = inData;
			service.processor = task;
			service.outputData = outData;
			return service;
		}
	},
	/**
	 * search
	 */
	FILTER {
		@Override
		public Service generateService(DbTable record) {
			String recordName = record.getQualifiedName();

			/*
			 * input as fields for filter
			 */
			InputRecord[] inRecs = { InputRecord.getInputRecord(recordName, record.getDefaultSheetName(),
					DataPurpose.FILTER, false) };
			InputData inData = new InputData();
			inData.setRecords(inRecs);

			/*
			 * output to named array
			 */
			OutputData outData = new OutputData();
			outData.setOutputRecords(record.getOutputRecords(true));

			AbstractAction action = new Filter(record);
			action.failureMessageName = Messages.NO_ROWS;
			AbstractAction[] actions = { action };
			TransactionProcessor task = new TransactionProcessor();
			task.dbUsage = DbUsage.READ_ONLY;
			task.schemaName = record.getSchemaName();
			task.actions = actions;

			Service service = new Service();
			service.inputData = inData;
			service.processor = task;
			service.outputData = outData;
			return service;
		}
	},
	/**
	 * like drop-down list
	 */
	LIST {
		@Override
		public Service generateService(DbTable record) {
			Service service = new Service();

			/*
			 * do we need any input? we are flexible
			 */

			InputField f1 = InputField.createInputField(AppConventions.Name.LIST_SERVICE_KEY,
					TextDataType.getDefaultInstance(), false,
					null,
					AppConventions.Name.LIST_SERVICE_KEY, null);
			InputField[] inFields = { f1 };
			InputData inData = new InputData();
			inData.setInputFields(inFields);
			service.inputData = inData;

			/*
			 * output as sheet
			 */
			String sheetName = record.getDefaultSheetName();
			OutputRecord outRec = OutputRecord.getOutputRecordForList(sheetName,
					record.listServiceUsesTwoColumns());
			OutputRecord[] outRecs = { outRec };
			OutputData outData = new OutputData();
			outData.setOutputRecords(outRecs);
			service.outputData = outData;
			/*
			 * processing logic
			 */
			TransactionProcessor task = new TransactionProcessor();
			task.dbUsage = DbUsage.READ_ONLY;
			task.schemaName = record.getSchemaName();
			if (record.okToCache()) {
				String keyName = record.getValueListKeyName();
				if (keyName == null) {
					keyName = "";
				}
				String[] list = { keyName };
				service.cacheKeyNames = list;
			}

			/*
			 * use a List action to do the job
			 */
			AbstractAction action = new KeyValueList(record);
			AbstractAction[] actions = { action };
			task.actions = actions;
			service.processor = task;

			/*
			 * getReady() is called by component manager any ways..
			 */
			return service;
		}
	},
	/**
	 * for a suggestion drop-dowm for the client
	 */
	SUGGEST {
		@Override
		public Service generateService(DbTable record) {
			Service service = new Service();
			TransactionProcessor task = new TransactionProcessor();
			service.processor = task;
			task.dbUsage = DbUsage.READ_ONLY;
			task.schemaName = record.getSchemaName();

			/*
			 * input for suggest
			 */
			InputField f1 = InputField.createInputField(AppConventions.Name.LIST_SERVICE_KEY,
					TextDataType.getDefaultInstance(), true, null,
					AppConventions.Name.LIST_SERVICE_KEY, null);
			InputField f2 = InputField.createInputField(AppConventions.Name.SUGGEST_STARTING,
					BooleanDataType.getDefaultInstance(), false,
					null,
					AppConventions.Name.SUGGEST_STARTING, null);

			InputField[] inFields = { f1, f2 };
			InputData inData = new InputData();
			inData.setInputFields(inFields);
			service.inputData = inData;

			/*
			 * use a suggest action to do the job
			 */
			AbstractAction action = new Suggest(record);
			action.failureMessageName = Messages.NO_ROWS;
			AbstractAction[] actions = { action };
			task.actions = actions;

			/*
			 * output as sheet
			 */
			String sheetName = record.getDefaultSheetName();
			OutputRecord outRec = new OutputRecord(sheetName, sheetName, record.getQualifiedName(),
					DataStructureType.SHEET,
					DataStructureType.ARRAY);
			OutputRecord[] outRecs = { outRec };
			OutputData outData = new OutputData();
			outData.setOutputRecords(outRecs);
			service.outputData = outData;

			/*
			 * getReady() is called by component manager any ways..
			 */
			return service;

		}
	};

	/**
	 * generate service for this dbTable
	 *
	 * @param record
	 * @return service or null in case teh design does not allow such a
	 *         generation, or there is some error in generation
	 */
	public abstract Service generateService(DbTable record);
}
