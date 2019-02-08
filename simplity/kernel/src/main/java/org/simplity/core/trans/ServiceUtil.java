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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.core.trans;

import org.simplity.core.app.AppConventions;
import org.simplity.core.app.Application;
import org.simplity.core.data.DataPurpose;
import org.simplity.core.dm.DbTable;
import org.simplity.core.dt.DataType;
import org.simplity.core.msg.Messages;
import org.simplity.core.rdb.DbUsage;
import org.simplity.core.service.DataStructureType;
import org.simplity.core.service.InputData;
import org.simplity.core.service.InputField;
import org.simplity.core.service.InputRecord;
import org.simplity.core.service.OutputData;
import org.simplity.core.service.OutputRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author simplity.org
 *
 */
public class ServiceUtil {
	private static final Logger logger = LoggerFactory.getLogger(ServiceUtil.class);
	/*
	 * constants used by on-the-fly services
	 */
	private static final char PREFIX_DELIMITER = '.';

	/**
	 * generate a service on the fly, if possible
	 *
	 * @param serviceName
	 *            that follows on-the-fly-service-name pattern
	 * @return service, or null if name is not a valid on-the-fly service name
	 */
	public static Service generateService(String serviceName) {
		int idx = serviceName.lastIndexOf(PREFIX_DELIMITER);
		if (idx == -1) {
			logger.info("service {} is not meant to be generated on-the-fly");
			return null;
		}

		String recordName = serviceName.substring(0, idx);
		DbTable record = null;
		try {
			record = (DbTable) Application.getActiveInstance().getRecord(recordName);
		} catch (Exception e) {
			logger.info("{} is not defined as a record, and hence we did not attempt generating service for {}.",
					recordName, serviceName);
			return null;
		}

		String operation = serviceName.substring(idx + 1);
		ServiceOperation oper = null;
		try {
			oper = ServiceOperation.valueOf(operation.toUpperCase());
		} catch (Exception e) {
			// we know that the only reason is that it is not a valid operation
		}
		if (oper == null) {
			logger.info("{} is not a valid operation, and hence we did not attempt generating a service for  {}",
					operation, serviceName);
			return null;
		}
		return oper.generateService(serviceName, record);
	}

	/**
	 * designed as enumeration, but idea is to delegate some static
	 * functionality out of Service class
	 *
	 * @author simplity.org
	 *
	 */
	enum ServiceOperation {
		/**
		 * read/get a row based on primary key value
		 */
		GET {
			@Override
			public Service generateService(String serviceName, DbTable record) {
				String recordName = record.getQualifiedName();
				Service service = new Service();
				service.setName(serviceName);
				TransactionProcessor task = new TransactionProcessor();
				service.processor = task;
				task.dbUsage = DbUsage.READ_ONLY;
				task.schemaName = record.getSchemaName();

				/*
				 * what is to be input
				 */
				InputRecord inRec = new InputRecord();
				inRec.setRecordName(recordName);
				inRec.setPurpose(DataPurpose.READ);
				InputRecord[] inRecs = { inRec };
				InputData inData = new InputData();
				inData.setRecords(inRecs);
				service.inputData = inData;

				/*
				 * We have just one action : read action
				 */
				AbstractAction action = new Read(record);
				action.failureMessageName = Messages.NO_ROWS;

				AbstractAction[] actions = { action };
				task.actions = actions;

				/*
				 * output fields from record.
				 */
				OutputData outData = new OutputData();
				outData.setOutputRecords(record.getOutputRecords(false));
				service.outputData = outData;

				return service;
			}
		},
		/**
		 * insert/update/delete
		 */
		SAVE {
			@Override
			public Service generateService(String serviceName, DbTable record) {
				String recordName = record.getQualifiedName();
				Service service = new Service();
				TransactionProcessor task = new TransactionProcessor();
				service.processor = task;
				task.dbUsage = DbUsage.READ_WRITE;
				service.setName(serviceName);
				task.schemaName = record.getSchemaName();

				/*
				 * data for this record is expected in fields, while rows for
				 * child-records in data sheets
				 */
				InputData inData = new InputData();
				inData.setRecords(getInputRecords(record));
				service.inputData = inData;

				/*
				 * save action
				 */
				RelatedRecord[] rrs = record.getChildRecordsAsRelatedRecords(false);
				Save action = new Save(record, rrs);
				action.failureMessageName = Messages.NO_UPDATE;
				AbstractAction[] actions = { action };
				task.actions = actions;
				/*
				 * we think we have to read back the row, but not sure.. Here
				 * the action commented for that
				 */
				// Read action1 = new Read();
				// action1.executeOnCondition = "saveResult != 0";
				// action1.name = "read";
				// action1.recordName = recordName;
				// action1.childRecords = getChildRecords(record, true);
				// Abstractaction[] actions = { action, action1 };

				/*
				 * what should we output? We are not sure. As of now let us send
				 * back fields
				 */
				OutputRecord outRec = new OutputRecord();
				outRec.setRecordName(recordName);
				OutputData outData = new OutputData();
				OutputRecord[] outRecs = { outRec };
				outData.setOutputRecords(outRecs);
				service.outputData = outData;

				return service;
			}
		},
		/**
		 * search
		 */
		FILTER {
			@Override
			public Service generateService(String serviceName, DbTable record) {
				String recordName = record.getQualifiedName();
				Service service = new Service();
				TransactionProcessor task = new TransactionProcessor();
				service.processor = task;
				task.dbUsage = DbUsage.READ_ONLY;
				service.setName(serviceName);
				task.schemaName = record.getSchemaName();

				/*
				 * input for filter
				 */
				InputRecord inRec = new InputRecord();
				inRec.setRecordName(recordName);
				inRec.setPurpose(DataPurpose.FILTER);
				InputRecord[] inRecs = { inRec };
				InputData inData = new InputData();
				inData.setRecords(inRecs);
				service.inputData = inData;

				AbstractAction action;
				OutputData outData = new OutputData();
				service.outputData = outData;
				/*
				 * if we have to read children, we use filter action, else we
				 * use filterToJson
				 */
				// if (record.getChildrenToOutput() == null) {
				// action = new FilterToJson(record);
				// outData.enableOutputFromWriter();
				// } else {
				// action = new Filter(record);
				// outData.setOutputRecords(record.getOutputRecords(true));
				// }

				action = new Filter(record);
				outData.setOutputRecords(record.getOutputRecords(true));

				action.failureMessageName = Messages.NO_ROWS;
				AbstractAction[] actions = { action };
				task.actions = actions;

				/*
				 * getReady() is called by component manager any ways..
				 */
				return service;

			}
		},
		/**
		 * like drop-down list
		 */
		LIST {
			@Override
			public Service generateService(String serviceName, DbTable record) {
				Service service = new Service();
				TransactionProcessor task = new TransactionProcessor();
				service.processor = task;
				task.dbUsage = DbUsage.READ_ONLY;
				service.setName(serviceName);
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
				 * do we need any input? we are flexible
				 */

				InputField f1 = new InputField(AppConventions.Name.LIST_SERVICE_KEY, DataType.DEFAULT_TEXT, false,
						null,
						AppConventions.Name.LIST_SERVICE_KEY, null);
				InputField[] inFields = { f1 };
				InputData inData = new InputData();
				inData.setInputFields(inFields);
				service.inputData = inData;
				/*
				 * use a List action to do the job
				 */
				AbstractAction action = new KeyValueList(record);
				AbstractAction[] actions = { action };
				task.actions = actions;

				/*
				 * output as sheet
				 */
				String sheetName = record.getDefaultSheetName();
				OutputRecord outRec = new OutputRecord(sheetName, sheetName, record.getQualifiedName(), false,
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
		},
		/**
		 * for a suggestion drop-dowm for the client
		 */
		SUGGEST {
			@Override
			public Service generateService(String serviceName, DbTable record) {
				Service service = new Service();
				TransactionProcessor task = new TransactionProcessor();
				service.processor = task;
				task.dbUsage = DbUsage.READ_ONLY;
				service.setName(serviceName);
				task.schemaName = record.getSchemaName();

				/*
				 * input for suggest
				 */
				InputField f1 = new InputField(AppConventions.Name.LIST_SERVICE_KEY, DataType.DEFAULT_TEXT, true, null,
						AppConventions.Name.LIST_SERVICE_KEY, null);
				InputField f2 = new InputField(AppConventions.Name.SUGGEST_STARTING, DataType.DEFAULT_BOOLEAN, false,
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
				OutputRecord outRec = new OutputRecord(sheetName, sheetName, record.getQualifiedName(), false,
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
		abstract Service generateService(String ServiceName, DbTable record);

		/**
		 * @param record
		 * @return
		 */
		protected static InputRecord[] getInputRecords(DbTable record) {
			String recordName = record.getQualifiedName();
			String[] children = record.getChildrenToInput();
			int nrecs = 1;
			if (children != null) {
				nrecs = children.length + 1;
			}

			InputRecord inRec = new InputRecord();
			inRec.setRecordName(recordName);
			inRec.setPurpose(DataPurpose.SAVE);
			inRec.enableSaveAction();

			InputRecord[] recs = new InputRecord[nrecs];
			recs[0] = inRec;
			Application app = Application.getActiveInstance();

			if (children != null) {
				String sheetName = record.getDefaultSheetName();
				int i = 1;
				for (String child : children) {
					recs[i++] = app.getRecord(child).getInputRecord(sheetName);
				}
			}
			return recs;
		}
	}

}
