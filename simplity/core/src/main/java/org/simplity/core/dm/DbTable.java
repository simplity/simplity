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

package org.simplity.core.dm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.simplity.core.ApplicationError;
import org.simplity.core.FilterCondition;
import org.simplity.core.app.AppConventions;
import org.simplity.core.app.Application;
import org.simplity.core.app.IAppDataCacher;
import org.simplity.core.app.IResponseWriter;
import org.simplity.core.comp.ComponentType;
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationMessage;
import org.simplity.core.data.DataPurpose;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.data.IFieldsCollection;
import org.simplity.core.data.MultiRowsSheet;
import org.simplity.core.dm.field.CreatedByUser;
import org.simplity.core.dm.field.CreatedTimestamp;
import org.simplity.core.dm.field.DbField;
import org.simplity.core.dm.field.Field;
import org.simplity.core.dm.field.ModifiedByUser;
import org.simplity.core.dm.field.ModifiedTimestamp;
import org.simplity.core.dt.DataType;
import org.simplity.core.idb.IDbDriver;
import org.simplity.core.idb.IMetadataHandle;
import org.simplity.core.idb.IReadOnlyHandle;
import org.simplity.core.idb.IResultSetReader;
import org.simplity.core.idb.ITransactionHandle;
import org.simplity.core.rdb.DbVendor;
import org.simplity.core.service.DataStructureType;
import org.simplity.core.service.InputRecord;
import org.simplity.core.service.OutputRecord;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.trans.RelatedRecord;
import org.simplity.core.util.RdbUtil;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author simplity.org
 *
 */
public class DbTable extends Record {

	/**
	 *
	 * @return a DbYable instance that is meant for customFields
	 */
	public static DbTable getCustomFieldsTable() {
		return customTableInstance;
	}

	private static final DbTable customTableInstance = initCustomTable();

	private static DbTable initCustomTable() {

		Application app = Application.getActiveInstance();
		DbField[] arr = new DbField[8];
		DataType text = app.getDataType("_text");
		DataType numbr = app.getDataType("_number");

		arr[0] = DbField.createDbField("tenantId", numbr);
		arr[1] = DbField.createDbField("tableName", text);
		arr[2] = DbField.createDbField("seqNo", numbr);
		arr[3] = DbField.createDbField("label", text);
		arr[4] = DbField.createDbField("valueType", app.getDataType("_valueType"));
		arr[5] = DbField.createDbField("maxLength", numbr);
		arr[6] = DbField.createDbField("isRequired", app.getDataType("_boolean"));
		arr[7] = DbField.createDbField("validValues", app.getDataType("_entityList"));
		DbTable ct = new DbTable();
		ct.name = "_customField";
		ct.fields = arr;
		return ct;
	}

	private static final Logger logger = LoggerFactory.getLogger(DbTable.class);
	/** header row of returned sheet when there is only one column */
	private static String[] SINGLE_HEADER = { "value" };
	/** header row of returned sheet when there are two columns */
	private static String[] DOUBLE_HEADER = { "id", "value" };

	private static final char COMMA = ',';
	private static final char PARAM = '?';
	private static final char EQUAL = '=';
	private static final String EQUAL_PARAM = "=?";
	private static final char PERCENT = '%';
	private static final char KEY_JOINER = 0;

	private static final String KEY_PREFIX = "rec.";
	/**
	 * name of the rdbms table, if this is either a storage table, or a view
	 * that is to be defined in the rdbms
	 */
	String tableName;

	/**
	 * has this table got an internal key, and do you want it to be managed
	 * automatically?
	 */
	@FieldMetaData(leaderField = "tableName")
	boolean keyIsGenerated;
	/**
	 * if this table is expected to have large number of rows, we would like to
	 * protect against a select with no where conditions. Of course one can
	 * always argue that this is no protection, as some one can easily put a
	 * condition like 1 = 1
	 */
	boolean okToSelectAll;

	/**
	 * child records that are to be read whenever a row from this record is
	 * read.
	 */
	@FieldMetaData(isReferenceToComp = true, referredCompType = ComponentType.REC)
	String[] childrenToBeRead = null;
	/**
	 * child records to be saved along with this record. operations for this
	 * record
	 */
	@FieldMetaData(isReferenceToComp = true, referredCompType = ComponentType.REC)
	String[] childrenToBeSaved = null;
	/**
	 * In case this is a table that supplies key-value list for drop-downs, then
	 * we use primary key as internal key. Specify the field to be used as
	 * display value. key of this table is the internal value
	 */
	String listFieldName = null;

	/**
	 * relevant only if valueListFieldName is used. If the list of values need
	 * be further filtered with a key, like country code for list of state,
	 * specify the that field name.
	 */
	@FieldMetaData(leaderField = "listFieldName")
	String listGroupKeyName = null;
	/**
	 * if this application uses multiple schemas, and the underlying table of
	 * this record belongs to a schema other than the default, then specify it
	 * here, so that the on-the-fly services based on this record can use the
	 * right schema.
	 */
	String schemaName;

	/**
	 * should we insist that the client returns the last time stamp during an
	 * update that we match with the current row before updating it? This
	 * technique allows us to detect whether the row was updated after it was
	 * sent to client.
	 */
	boolean useTimestampForConcurrency = false;

	/**
	 * filed to b eused as key for suggesting matches
	 */
	String suggestionKeyName;
	/**
	 * field names to be output as suggestions
	 */
	String[] suggestionOutputNames;
	/**
	 * if this table is (almost) static, and the vauleList that is delivered on
	 * a list request can be cached by the agent. Valid only if valueListField
	 * is set (list_ auto service is enabled) if valueListKey is specified, the
	 * result will be cached by that field. For example, by country-code.
	 */
	boolean okToCache;
	/**
	 * if this record allows update of underlying table, and the table is
	 * "cacheable", then there may be one or more records that would have set
	 * okToCache="true". We may have to notify them in case data in this table
	 * changes.
	 */
	String[] recordsToBeNotifiedOnChange;
	/**
	 * is this record only for reading?
	 */
	boolean readOnly;
	/*
	 * design note:
	 *
	 * data base will have varchar fields named tableName_0, tbleName_1.... all
	 * nullable. All dbTable based operations treat them as text fields. A
	 * tenant-level metadata describes these fields (name data-type)
	 * input/output is managed as if the fields are pre-defined like that
	 */
	/**
	 * in multi-tenancy design, we may need different sets of fields for an
	 * entity. Our approach is to design them as text fields in the db and mp
	 * them to the desired name and dataType at run time, based on a
	 * configuration meta data for that tenant
	 */
	int nbrGenericFields;
	/*
	 * standard fields are cached
	 */
	private DbField modifiedStampField;
	private DbField modifiedUserField;
	private DbField createdUserField;
	private DbField createdStampField;

	/** sql for reading a row for given primary key value */
	private String readSql;

	/** select f1,f2,..... WHERE used in filtering */
	private String filterSql;

	/** sql ready to insert a row into the table */
	private String insertSql;

	/** sql to update every field. (Not selective update) */
	private String updateSql;

	/**
	 * we skip few standard fields while updating s row. Keeping this count
	 * simplifies code
	 */
	private String deleteSql;

	/** sql to be used for a list action */
	private String listSql;
	/** value types of fields selected for list action */
	private ValueType[] valueListTypes;
	/** value type of key used in list action */
	private ValueType valueListKeyType;

	/** sql to be used for a suggestion action */
	private String suggestSql;
	/**
	 * in case the primary is a composite key : with more than one fields, then
	 * we keep all of them in an array. This is null if we have a single key. We
	 * have designed it that way to keep the single key case as simple as
	 * possible
	 */
	private DbField[] allPrimaryKeys;

	/** parent key, in case parent has composite primary key */
	private DbField[] allParentKeys;

	/** " WHERE key1=?,key2=? */
	private String primaryWhereClause;

	private int nbrUpdateFields = 0;
	private int nbrInsertFields = 0;
	private Field[] fieldsToInput;

	/**
	 *
	 */
	public DbTable() {
		this.recordUsageType = RecordUsageType.TABLE;
	}

	/**
	 * @return array of primary key fields.
	 */
	public Field[] getPrimaryKeyFields() {
		return this.allPrimaryKeys;
	}

	/**
	 * @return dependent child records that are read whenever this rec is read
	 */
	public String[] getChildrenToBeRead() {
		return this.childrenToBeRead;
	}

	/**
	 *
	 * @return child recs that have to be saved whenever this rec is saved
	 */
	public String[] getChildrenToBeSaved() {
		return this.childrenToBeRead;
	}

	/**
	 * read, in our vocabulary, is ALWAYS primary key based read. Hence we
	 * expect (at most) one row of output per read. If values has more than one
	 * rows, we read for primary key in each row.
	 *
	 * @param inSheet
	 *            one or more rows that has value for the primary key.
	 * @param handle
	 * @param userId
	 *            we may have a common security strategy to restrict output rows
	 *            based on user. Not used as of now
	 * @return data sheet with data, or null if there is no data output.
	 *
	 */
	public IDataSheet readMany(IDataSheet inSheet, IReadOnlyHandle handle, Value userId) {
		if (this.allPrimaryKeys == null) {
			throw new ApplicationError("Record " + this.name
					+ " is not defined with a primary key, and hence we can not do a read operation on this.");
		}
		int nbrRows = inSheet.length();
		if (nbrRows == 0) {
			return null;
		}

		IDataSheet outSheet = this.createSheet(false, false);
		boolean singleRow = nbrRows == 1;
		if (singleRow) {
			Value[] values = this.getPrimaryKeyValues(inSheet, 0);
			if (values == null) {
				logger.info("Primary key value not available and hence no read operation.");
				return null;
			}
			int n = handle.read(this.readSql, values, outSheet);
			if (n == 0) {
				return null;
			}
			return outSheet;
		}

		Value[][] values = new Value[nbrRows][];
		for (int i = 0; i < nbrRows; i++) {
			Value[] vals = this.getPrimaryKeyValues(inSheet, i);
			if (vals == null) {
				logger.info("Primary key value not available and hence no read operation.");
				return null;
			}
			values[i] = vals;
		}
		handle.readBatch(this.readSql, values, outSheet);
		return outSheet;
	}

	private Value[] getPrimaryKeyValues(IDataSheet inSheet, int idx) {
		Value[] values = new Value[this.allPrimaryKeys.length];
		for (int i = 0; i < this.allPrimaryKeys.length; i++) {
			Value value = inSheet.getColumnValue(this.allPrimaryKeys[i].getName(), idx);
			if (Value.isNull(value)) {
				return null;
			}
			values[i] = value;
		}
		return values;
	}

	private Value[] getWhereValues(IFieldsCollection inFields, boolean includeTimeStamp) {
		Value[] values;
		int nbr = this.allPrimaryKeys.length;
		if (includeTimeStamp) {
			values = new Value[nbr + 1];
			Value stamp = inFields.getValue(this.modifiedStampField.getName());
			if (Value.isNull(stamp)) {
				throw new ApplicationError("Field " + this.modifiedStampField.getName()
						+ " is timestamp, and value is required for an update operation to check for concurrency.");
			}
			values[nbr] = Value.newTimestampValue(stamp);
		} else {
			values = new Value[nbr];
		}
		for (int i = 0; i < this.allPrimaryKeys.length; i++) {
			Value value = inFields.getValue(this.allPrimaryKeys[i].getName());
			if (Value.isNull(value)) {
				return null;
			}
			values[i] = value;
		}
		return values;
	}

	/**
	 * read, in our vocabulary, is ALWAYS primary key based read. Hence we
	 * expect (at most) one row of output per read.
	 *
	 * @param inData
	 *            one or more rows that has value for the primary key.
	 * @param handle
	 * @param userId
	 *            we may have a common security strategy to restrict output rows
	 *            based on user. Not used as of now
	 * @return Single row data sheet, or null if there is no data
	 */
	public IDataSheet readOne(IFieldsCollection inData, IReadOnlyHandle handle, Value userId) {
		if (this.allPrimaryKeys == null) {
			throw new ApplicationError(
					"Record " + this.name + " is not defined with a primary key but a request is made for read.");
		}
		Value[] values = this.getWhereValues(inData, false);
		if (values == null) {

			logger.info("Value for primary key not present, and hence no read operation.");

			return null;
		}
		IDataSheet outData = null;
		if (this.okToCache) {
			outData = this.getRowFromCache(inData);
			if (outData != null) {
				return outData;
			}
		}
		outData = this.createSheet(true, false);
		handle.read(this.readSql, values, outData);
		if (this.okToCache) {
			this.cacheRow(inData, outData);
		}
		return outData;
	}

	/**
	 * checks if there is a row for this key. Row is not read.
	 *
	 * @param inData
	 * @param keyFieldName
	 * @param handle
	 * @param userId
	 * @return true if there is a row for this key, false otherwise. Row is not
	 *         read.
	 */
	public boolean rowExistsForKey(IFieldsCollection inData, String keyFieldName, IReadOnlyHandle handle,
			Value userId) {
		if (this.allPrimaryKeys == null) {

			logger.info("Record " + this.name
					+ " is not defined with a primary key, and hence we can not do a read operation on this.");

			this.noPrimaryKey();
			return false;
		}
		Value[] values;
		if (keyFieldName != null) {
			if (this.allPrimaryKeys.length > 1) {

				logger.info("There are more than one primary keys, and hence supplied name keyFieldName of "
						+ keyFieldName + " is ognored");

				values = this.getWhereValues(inData, false);
			} else {
				Value value = inData.getValue(keyFieldName);
				if (Value.isNull(value)) {

					logger.info("Primary key field " + keyFieldName + " has no value, and hence no read operation.");

					return false;
				}
				values = new Value[1];
				values[0] = value;
			}
		} else {
			values = this.getWhereValues(inData, false);
		}
		return handle.hasData(this.readSql, values);
	}

	/**
	 * filter rows from underlying view/table as per filtering criterion
	 *
	 * @param inputRecord
	 *            record that has fields for filter criterion
	 * @param userId
	 *            we may have a common security strategy to restrict output rows
	 *            based on user. Not used as of now
	 * @param inData
	 *            as per filtering conventions
	 * @param handle
	 * @return data sheet, possible with retrieved rows
	 */
	public IDataSheet filter(DbTable inputRecord, IFieldsCollection inData, IReadOnlyHandle handle, Value userId) {
		SqlAndValues temp = this.getSqlAndValues(handle, inData, inputRecord);
		IDataSheet result = this.createSheet(false, false);
		handle.read(temp.sql, temp.values, result);
		return result;
	}

	/**
	 * add, modify and delete are the three operations we can do for a record.
	 * "save" is a special convenient command. If key is specified, it is
	 * assumed to be modify, else add. Save
	 *
	 * @param row
	 *            data to be saved.
	 * @param handle
	 * @param userId
	 * @param treatSqlErrorAsNoResult
	 *            if true, we assume that some constraints are set at db level,
	 *            and sql error is treated as if affected rows is zero
	 * @return number of rows saved. -1 in case of batch, and the handle is
	 *         unable to count the saved rows
	 */
	public SaveActionType saveOne(IFieldsCollection row, ITransactionHandle handle, Value userId,
			boolean treatSqlErrorAsNoResult) {
		if (this.readOnly) {
			this.notWritable();
		}

		if (this.allPrimaryKeys == null) {
			this.noPrimaryKey();
		}
		DbField pkey = this.allPrimaryKeys[0];
		Value[] values = new Value[this.fields.length];
		/*
		 * modified user field, even if sent by client, must be over-ridden
		 */
		if (this.modifiedUserField != null) {
			row.setValue(this.modifiedUserField.getName(), userId);
		}
		/*
		 * is the action explicitly specified
		 */
		SaveActionType saveAction = SaveActionType.SAVE;
		Value action = row.getValue(AppConventions.Name.TABLE_ACTION);
		if (action != null) {
			/*
			 * since this field is extracted by us earlier, we DO KNOW that it
			 * is valid
			 */
			saveAction = SaveActionType.parse(action.toString());
			logger.info("Service has requested a specific save action={} for record {}", saveAction,
					this.getQualifiedName());
		}
		if (saveAction == SaveActionType.SAVE) {
			/*
			 * is the key supplied?
			 */
			Value keyValue = row.getValue(pkey.getName());
			if (this.keyIsGenerated) {
				if (Value.isNull(keyValue)) {
					saveAction = SaveActionType.ADD;
				} else {
					saveAction = SaveActionType.MODIFY;
				}
			} else {
				if (this.rowExistsForKey(row, null, handle, userId)) {
					saveAction = SaveActionType.MODIFY;
				} else {
					saveAction = SaveActionType.ADD;
				}
			}
			logger.info("Save request translated into {} action for record {}", saveAction, this.getQualifiedName());
		}
		if (saveAction == SaveActionType.ADD) {
			if (this.createdUserField != null) {
				row.setValue(this.createdUserField.getName(), userId);
			}
			values = this.getInsertValues(row, userId);
			if (this.keyIsGenerated) {
				long[] generatedKeys = new long[1];
				String[] generatedColumns = { pkey.getColumnName() };
				handle.insertAndGetKeys(this.insertSql, values, generatedKeys, generatedColumns,
						treatSqlErrorAsNoResult);
				row.setValue(pkey.getName(), Value.newIntegerValue(generatedKeys[0]));
			} else {
				handle.execute(this.insertSql, values, treatSqlErrorAsNoResult);
			}
		} else if (saveAction == SaveActionType.DELETE) {
			values = this.getWhereValues(row, this.useTimestampForConcurrency);
			handle.execute(this.deleteSql, values, treatSqlErrorAsNoResult);
		} else {
			values = this.getUpdateValues(row, userId);
			if (handle.execute(this.updateSql, values, treatSqlErrorAsNoResult) == 0) {
				throw new ApplicationError(
						"Data was changed by some one else while you were editing it. Please cancel this operation and redo it with latest data.");
			}
		}
		return saveAction;
	}

	/**
	 * add, modify and delete are the three operations we can do for a record.
	 * "save" is a special convenient command. If key is specified, it is
	 * assumed to be modify, else add. Save
	 *
	 * @param inSheet
	 *            data to be saved.
	 * @param handle
	 * @param userId
	 * @param treatSqlErrorAsNoResult
	 * @return number of rows saved. -1 in case of batch, and the handle is
	 *         unable to count the saved rows
	 */
	public SaveActionType[] saveMany(IDataSheet inSheet, ITransactionHandle handle, Value userId,
			boolean treatSqlErrorAsNoResult) {
		if (this.readOnly) {
			this.notWritable();
		}
		SaveActionType[] result = new SaveActionType[inSheet.length()];
		int rowIdx = 0;
		for (IFieldsCollection row : inSheet) {
			result[rowIdx] = this.saveOne(row, handle, userId, treatSqlErrorAsNoResult);
			rowIdx++;
		}
		return result;
	}

	/**
	 * parent record got saved. we are to save rows for this record
	 *
	 * @param inSheet
	 *            data for this record
	 * @param parentRow
	 *            data for parent record that is already saved
	 * @param actions
	 *            that are already done using parent sheet
	 * @param handle
	 * @param userId
	 * @return number of rows affected
	 */
	public int saveWithParent(IDataSheet inSheet, IFieldsCollection parentRow, SaveActionType[] actions,
			ITransactionHandle handle, Value userId) {
		if (this.readOnly) {
			this.notWritable();
		}
		if (this.allParentKeys == null) {
			this.noParent();
		}
		/*
		 * for security/safety, we copy parent key into data
		 */
		this.copyParentKeys(parentRow, inSheet);
		for (IFieldsCollection row : inSheet) {
			this.saveOne(row, handle, userId, false);
		}
		return inSheet.length();
	}

	/**
	 * @param inSheet
	 * @param userId
	 * @param rowIdx
	 * @return
	 */
	private Value[] getInsertValues(IFieldsCollection row, Value userId) {
		Value[] values = new Value[this.nbrInsertFields];
		int valueIdx = 0;
		for (Field f : this.fields) {
			DbField field = (DbField) f;
			if (field.canInsert() == false) {
				continue;
			}
			if (this.keyIsGenerated && field.isPrimaryKey()) {
				continue;
			}
			if (field instanceof CreatedByUser
					|| field instanceof ModifiedByUser) {
				values[valueIdx] = userId;
			} else {
				Value value = field.getValue(row, null);
				if (Value.isNull(value)) {
					if (field.isNullable()) {
						value = Value.newUnknownValue(field.getValueType());
					} else {
						throw new ApplicationError("Column " + field.getColumnName() + " in table " + this.tableName
								+ " is designed to be non-null, but a row is being inserted with a null value in it.");
					}
				}
				values[valueIdx] = value;
			}
			valueIdx++;
		}
		return values;
	}

	/**
	 * insert row/s
	 *
	 * @param inSheet
	 * @param handle
	 * @param userId
	 * @param treatSqlErrorAsNoResult
	 * @return number of rows saved. -1 in case of batch, and the handle is
	 *         unable to count the saved rows
	 */
	public int insert(IDataSheet inSheet, ITransactionHandle handle, Value userId, boolean treatSqlErrorAsNoResult) {
		if (this.readOnly) {
			this.notWritable();
		}
		int nbrRows = inSheet.length();

		/*
		 * simple case first...
		 */
		if (nbrRows == 1) {
			return this.insert((IFieldsCollection) inSheet, handle, userId, treatSqlErrorAsNoResult);
		}
		Value[][] allValues = new Value[nbrRows][];
		/*
		 * we mostly expect one row, but we do not want to write separate
		 * code...
		 */
		int rowIdx = 0;
		for (IFieldsCollection row : inSheet) {
			allValues[rowIdx] = this.getInsertValues(row, userId);
			rowIdx++;
		}
		if (this.keyIsGenerated == false) {
			return this.executeWorker(handle, this.insertSql, allValues, treatSqlErrorAsNoResult);
		}
		long[] generatedKeys = new long[nbrRows];
		int result = this.insertWorker(handle, this.insertSql, allValues, generatedKeys, treatSqlErrorAsNoResult);
		if (result > 0 && generatedKeys[0] != 0) {
			this.addKeyColumn(inSheet, generatedKeys);
		}

		return result;
	}

	/**
	 * insert row/s
	 *
	 * @param inData
	 * @param handle
	 * @param userId
	 * @param treatSqlErrorAsNoResult
	 * @return number of rows saved. -1 in case of batch, and the handle is
	 *         unable to count the saved rows
	 */
	public int insert(IFieldsCollection inData, ITransactionHandle handle, Value userId,
			boolean treatSqlErrorAsNoResult) {
		if (this.readOnly) {
			this.notWritable();
		}
		Value[][] allValues = new Value[1][];
		allValues[0] = this.getInsertValues(inData, userId);

		if (this.keyIsGenerated == false) {
			return this.executeWorker(handle, this.insertSql, allValues, treatSqlErrorAsNoResult);
		}
		/*
		 * try to get generated keys and set it/them
		 */
		long[] generatedKeys = new long[1];
		int result = this.insertWorker(handle, this.insertSql, allValues, generatedKeys, treatSqlErrorAsNoResult);
		if (result > 0) {
			/*
			 * generated key feature may not be available with some rdb vendor
			 */
			long key = generatedKeys[0];
			if (key > 0) {
				inData.setValue(this.allPrimaryKeys[0].getName(), Value.newIntegerValue(key));
			}
		}

		return result;
	}

	/**
	 * insert row/s
	 *
	 * @param inSheet
	 *            data for this record to be inserted inserted after its parent
	 *            got inserted
	 * @param parentRow
	 *            fields/row that has the parent key
	 * @param handle
	 * @param userId
	 * @return number of rows saved. -1 in case of batch, and the handle is
	 *         unable to count the saved rows
	 */
	public int insertWithParent(IDataSheet inSheet, IFieldsCollection parentRow, ITransactionHandle handle,
			Value userId) {
		if (this.readOnly) {
			this.notWritable();
		}
		if (this.allParentKeys == null) {
			this.noParent();
		}

		/*
		 * for security/safety, we copy parent key into data
		 */
		this.copyParentKeys(parentRow, inSheet);

		int nbrRows = inSheet.length();
		Value[][] allValues = new Value[nbrRows][];
		int rowIdx = 0;

		for (IFieldsCollection row : inSheet) {
			allValues[rowIdx] = this.getInsertValues(row, userId);
			rowIdx++;
		}
		if (this.keyIsGenerated == false) {
			return this.executeWorker(handle, this.insertSql, allValues, false);
		}
		/*
		 * generated key is t be retrieved
		 */
		long[] keys = new long[nbrRows];
		int result = this.insertWorker(handle, this.insertSql, allValues, keys, false);
		if (keys[0] != 0) {
			this.addKeyColumn(inSheet, keys);
		}
		return result;
	}

	/**
	 * add a column to the data sheet and copy primary key values into that
	 *
	 * @param inSheet
	 * @param keys
	 */
	private void addKeyColumn(IDataSheet inSheet, long[] keys) {
		int nbrKeys = keys.length;
		Value[] values = new Value[nbrKeys];
		int i = 0;
		for (long key : keys) {
			values[i++] = Value.newIntegerValue(key);
		}
		inSheet.addColumn(this.allPrimaryKeys[0].getName(), ValueType.INTEGER, values);
	}

	/**
	 * copy parent key to child sheet
	 *
	 * @param inData
	 *            row/fields that has the parent key
	 * @param sheet
	 *            to which we have to copy the key values
	 */
	private void copyParentKeys(IFieldsCollection inData, IDataSheet sheet) {
		for (Field field : this.allParentKeys) {
			String fieldName = field.getName();
			String parentKeyName = field.getReferredField();
			Value parentKey = inData.getValue(parentKeyName);
			if (Value.isNull(parentKey)) {

				logger.info("No value found for parent key field " + parentKeyName
						+ " and hence no column is going to be added to child table");

				return;
			}
			sheet.addColumn(fieldName, parentKey);
		}
	}

	/**
	 * get parent key values
	 *
	 * @param inData
	 *            row/fields that has the parent key
	 * @param sheet
	 *            to which we have to copy the key values
	 */
	private Value[] getParentValues(IFieldsCollection inData) {
		Value[] values = new Value[this.allParentKeys.length];
		for (int i = 0; i < this.allParentKeys.length; i++) {
			Field field = this.allParentKeys[i];
			Value value = inData.getValue(field.getReferredField());
			if (Value.isNull(value)) {

				logger.info("No value found for parent key field " + field.getReferredField()
						+ " and hence no column is going to be added to child table");

				return null;
			}
			values[i] = value;
		}
		return values;
	}

	/**
	 * update all fields, except of course the ones that are not to be.
	 *
	 * @param inSheet
	 *            data to be saved.
	 * @param handle
	 * @param userId
	 * @param treatSqlErrorAsNoResult
	 * @return number of rows saved. -1 in case of batch, and the handle is
	 *         unable to count the saved rows
	 */
	public int update(IDataSheet inSheet, ITransactionHandle handle, Value userId, boolean treatSqlErrorAsNoResult) {

		if (this.readOnly) {
			this.notWritable();
		}
		if (this.allPrimaryKeys == null) {
			this.noPrimaryKey();
		}
		int nbrRows = inSheet.length();
		Value[][] allValues = new Value[nbrRows][];
		if (nbrRows == 1) {
			allValues[0] = this.getUpdateValues(inSheet, userId);
		} else {
			int i = 0;
			for (IFieldsCollection row : inSheet) {
				allValues[i++] = this.getUpdateValues(row, userId);
			}
		}
		int result = this.executeWorker(handle, this.updateSql, allValues, treatSqlErrorAsNoResult);

		if (result > 0 && this.recordsToBeNotifiedOnChange != null) {
			for (IFieldsCollection row : inSheet) {
				this.invalidateCache(row);
			}
		}
		return result;
	}

	/**
	 * update all fields, except of course the ones that are not to be.
	 *
	 * @param inputData
	 *            data to be saved.
	 * @param handle
	 * @param userId
	 * @param treatSqlErrorAsNoResult
	 * @return number of rows saved. -1 in case of batch, and the handle is
	 *         unable to count the saved rows
	 */
	public int update(IFieldsCollection inputData, ITransactionHandle handle, Value userId,
			boolean treatSqlErrorAsNoResult) {

		if (this.readOnly) {
			this.notWritable();
		}
		if (this.allPrimaryKeys == null) {
			this.noPrimaryKey();
		}
		Value[][] allValues = new Value[1][];

		allValues[0] = this.getUpdateValues(inputData, userId);
		int result = this.executeWorker(handle, this.updateSql, allValues, treatSqlErrorAsNoResult);
		if (result > 0 && this.recordsToBeNotifiedOnChange != null) {
			this.invalidateCache(inputData);
		}
		return result;
	}

	/**
	 * update all fields, except of course the ones that are not to be.
	 *
	 * @param inSheet
	 *            data to be saved.
	 * @param handle
	 * @param treatSqlErrorAsNoResult
	 * @return number of rows saved. -1 in case of batch, and the handle is
	 *         unable to count the saved rows
	 */
	public int delete(IDataSheet inSheet, ITransactionHandle handle, boolean treatSqlErrorAsNoResult) {
		if (this.readOnly) {
			this.notWritable();
		}
		if (this.allPrimaryKeys == null) {
			this.noPrimaryKey();
		}
		int nbrRows = inSheet.length();
		/*
		 * we mostly expect one row, but we do not want to write separate
		 * code...
		 */
		Value[][] allValues = new Value[nbrRows][];
		nbrRows = 0;
		for (IFieldsCollection row : inSheet) {
			allValues[nbrRows++] = this.getWhereValues(row, this.useTimestampForConcurrency);
		}
		int result = this.executeWorker(handle, this.deleteSql, allValues, treatSqlErrorAsNoResult);

		if (result > 0 && this.recordsToBeNotifiedOnChange != null) {
			for (IFieldsCollection row : inSheet) {
				this.invalidateCache(row);
			}
		}
		return result;
	}

	/**
	 * update all fields, except of course the ones that are not to be.
	 *
	 * @param inData
	 *            data to be saved.
	 * @param handle
	 * @param treatSqlErrorAsNoResult
	 * @return number of rows saved. -1 in case of batch, and the handle is
	 *         unable to count the saved rows
	 */
	public int delete(IFieldsCollection inData, ITransactionHandle handle, boolean treatSqlErrorAsNoResult) {
		if (this.readOnly) {
			this.notWritable();
		}
		if (this.allPrimaryKeys == null) {
			this.noPrimaryKey();
		}
		Value[][] allValues = new Value[1][];
		allValues[0] = this.getWhereValues(inData, this.useTimestampForConcurrency);
		int result = this.executeWorker(handle, this.deleteSql, allValues, treatSqlErrorAsNoResult);

		if (result > 0 && this.recordsToBeNotifiedOnChange != null) {
			this.invalidateCache(inData);
		}
		return result;
	}

	private void notWritable() {
		throw new ApplicationError("Record " + this.name
				+ " is not designed to be writable. Add/Update/Delete operations are not possible.");
	}

	private void noParent() {
		throw new ApplicationError(
				"Record " + this.name + " does not have a parent key field. Operation with parent is not possible.");
	}

	private void noPrimaryKey() {
		throw new ApplicationError("Update/Delete operations are not possible for Record " + this.name
				+ " as it does not define a primary key.");
	}

	/**
	 * delete child rows for this record when its parent is deleted.
	 *
	 * @param parentRow
	 *            from where we pick up parent key.
	 * @param handle
	 * @param userId
	 * @return number of rows saved. -1 in case of batch, and the handle is
	 *         unable to count the saved rows
	 */
	public int deleteWithParent(IFieldsCollection parentRow, ITransactionHandle handle, Value userId) {
		if (this.readOnly) {
			this.notWritable();
		}
		if (this.allParentKeys == null) {
			this.noParent();
		}
		Value[] values = this.getParentValues(parentRow);
		if (values == null) {

			logger.info("Delete with parent has nothing to delete as parent key is null");

			return 0;
		}
		StringBuilder sql = new StringBuilder("DELETE FROM ");
		sql.append(this.tableName).append(" WHERE ").append(this.getParentWhereClause());
		if (this.allParentKeys.length > 1) {
			for (int i = 1; i < this.allParentKeys.length; i++) {
				sql.append(" AND " + this.allParentKeys[i].getColumnName() + EQUAL_PARAM);
			}
		}
		return handle.execute(sql.toString(), values, false);
	}

	private int executeWorker(ITransactionHandle handle, String sql, Value[][] values,
			boolean treatSqlErrorAsNoResult) {
		if (values.length == 1) {
			return handle.execute(sql, values[0], treatSqlErrorAsNoResult);
		}
		int[] counts = handle.executeBatch(sql, values, treatSqlErrorAsNoResult);
		int result = 0;
		for (int n : counts) {
			if (n < 0) {
				return -1;
			}
			result += n;
		}
		return result;
	}

	private int insertWorker(ITransactionHandle handle, String sql, Value[][] values, long[] generatedKeys,
			boolean treatSqlErrorAsNoResult) {
		String[] keyNames = { this.allPrimaryKeys[0].getColumnName() };
		if (values.length == 1) {
			return handle.insertAndGetKeys(sql, values[0], generatedKeys, keyNames, treatSqlErrorAsNoResult);
		}
		if (generatedKeys != null) {

			logger.info(
					"Generated key retrieval is NOT supported for batch. Keys for child table are to be retrieved automatically");
		}
		int[] counts = handle.executeBatch(sql, values, treatSqlErrorAsNoResult);
		int result = 0;
		for (int n : counts) {
			if (n < 0) {
				return -1;
			}
			result += n;
		}
		return result;
	}

	/**
	 * read rows from this record for a given parent record
	 *
	 * @param parentData
	 *            rows for parent
	 * @param handle
	 * @param sheetName
	 * @param cascadeFilter
	 * @param ctx
	 * @return number of rows filtered from this child for the parent.
	 */
	public int filterForParents(IDataSheet parentData, IReadOnlyHandle handle, String sheetName, boolean cascadeFilter,
			ServiceContext ctx) {
		IDataSheet result = this.createSheet(false, false);
		if (parentData.length() == 0) {
			return 0;
		}
		if (this.allParentKeys.length > 1) {
			this.filterForMultiParentKeys(parentData, handle, result);
		} else {
			this.filterForSingleParentKey(parentData, handle, result);
		}
		String sn = sheetName;
		if (sn == null) {
			sn = this.getDefaultSheetName();
		}
		ctx.putDataSheet(sn, result);
		logger.info("Added child sheet {} to context with {} rows", sn, result.length());
		if (result.length() > 0 && cascadeFilter) {
			this.filterChildRecords(result, handle, ctx);
		}
		return result.length();
	}

	/**
	 * @param parentData
	 * @param handle
	 * @param result
	 */
	private void filterForSingleParentKey(IDataSheet parentData, IReadOnlyHandle handle, IDataSheet result) {
		String keyName = this.allParentKeys[0].getReferredField();
		int n = parentData.length();
		Value[] values = parentData.getColumnValues(keyName);
		StringBuilder sbf = new StringBuilder(this.filterSql);
		sbf.append(this.allParentKeys[0].getColumnName());
		/*
		 * for single key we use where key = ?
		 *
		 * for multiple, we use where key in (?,?,....)
		 */
		if (n == 1) {
			sbf.append(EQUAL_PARAM);
		} else {
			sbf.append(" IN (?");
			for (int i = 1; i < n; i++) {
				sbf.append(",?");
			}
			sbf.append(')');
		}
		handle.read(sbf.toString(), values, result);
	}

	/**
	 * read rows from this record for a given parent record
	 *
	 * @param parentData
	 *            rows for parent
	 * @param handle
	 * @param name
	 * @param cascadeFilter
	 * @param ctx
	 */
	private void filterForMultiParentKeys(IDataSheet parentData, IReadOnlyHandle handle, IDataSheet outSheet) {
		String sql = this.filterSql + this.getParentWhereClause();
		int nbrRows = parentData.length();
		Value[][] allValues = new Value[nbrRows][];
		int idx = 0;
		for (IFieldsCollection prentRow : parentData) {
			allValues[idx++] = this.getParentValues(prentRow);
		}
		handle.readBatch(sql, allValues, outSheet);
	}

	/**
	 * if this record has child records, filter them based on this parent sheet
	 *
	 * @param parentSheet
	 *            sheet that has rows for this record
	 * @param handle
	 * @param ctx
	 */
	public void filterChildRecords(IDataSheet parentSheet, IReadOnlyHandle handle, ServiceContext ctx) {
		if (this.childrenToBeRead == null) {
			return;
		}
		for (String childName : this.childrenToBeRead) {
			DbTable cr = (DbTable) Application.getActiveInstance().getRecord(childName);
			cr.filterForParents(parentSheet, handle, cr.getDefaultSheetName(), true, ctx);
		}
	}

	/**
	 * read rows from this record for a given parent record
	 *
	 * @param parentData
	 * @param handle
	 * @return sheet that contains rows from this record for the parent rows
	 */
	public IDataSheet filterForAParent(IFieldsCollection parentData, IReadOnlyHandle handle) {
		IDataSheet result = this.createSheet(false, false);
		Value[] values = this.getParentValues(parentData);
		String sql = this.filterSql + this.getParentWhereClause();
		handle.read(sql, values, result);
		return result;
	}

	private String getParentWhereClause() {
		StringBuilder sbf = new StringBuilder();
		sbf.append(this.allParentKeys[0].getColumnName()).append(EQUAL_PARAM);
		for (int i = 1; i < this.allParentKeys.length; i++) {
			sbf.append(" AND ").append(this.allParentKeys[i].getColumnName()).append(EQUAL_PARAM);
		}
		return sbf.toString();
	}

	/**
	 * @param nbrPrimaries
	 */
	private void cacheSpecialFields(int nbrPrimaries, int nbrParents) {
		if (nbrPrimaries > 0) {
			this.allPrimaryKeys = new DbField[nbrPrimaries];
		}
		if (nbrParents > 0) {
			this.allParentKeys = new DbField[nbrParents];
		}
		int primaryIdx = 0;
		int parentIdx = 0;
		for (Field f : this.fields) {
			DbField field = (DbField) f;
			if (field.isPrimaryKey()) {
				this.allPrimaryKeys[primaryIdx] = field;
				primaryIdx++;
			}
			if (field.isParentKey()) {
				this.allParentKeys[parentIdx] = field;
				parentIdx++;
			}
		}
	}

	/**
	 * @param thisIsSheet
	 *            is this record being output as sheet?
	 * @return list of output which output records are to be added
	 */
	public OutputRecord[] getOutputRecords(boolean thisIsSheet) {
		List<OutputRecord> list = new ArrayList<>();
		this.addOutputRecordsCascaded(list, null, thisIsSheet);
		return list.toArray(new OutputRecord[0]);
	}

	/**
	 * @param recs
	 *            list to which output records are to be added
	 * @param parentSheetName
	 * @param parentKey
	 */
	private void addOutputRecordsCascaded(List<OutputRecord> recs, DbTable parentRec, boolean thisIsSheet) {
		String sheetName = this.getDefaultSheetName();
		DataStructureType readAs;
		DataStructureType writeAs;
		if (thisIsSheet) {
			readAs = DataStructureType.SHEET;
			writeAs = DataStructureType.ARRAY;
		} else {
			readAs = DataStructureType.FIELDS;
			writeAs = DataStructureType.FIELDS;
		}
		OutputRecord outRec = new OutputRecord(sheetName, sheetName, this.getQualifiedName(), readAs, writeAs);
		if (parentRec != null) {
			String parentSheetName = parentRec.getDefaultSheetName();
			Field[] parentKeys = parentRec.allPrimaryKeys;
			Field[] refKeys = this.allParentKeys;
			if (parentKeys == null || refKeys == null || parentKeys.length != refKeys.length) {
				throw new ApplicationError("Parent record " + parentRec.getQualifiedName()
						+ " defines number of children to be read. This would work only if this record defines key field(s) and the child records define corresponding links using parentKeyFields");
			}
			int nbr = parentKeys.length;

			String[] pk = new String[nbr];
			String[] rk = new String[nbr];
			for (int i = 0; i < pk.length; i++) {
				pk[i] = parentKeys[i].getName();
				rk[i] = refKeys[i].getName();
			}
			outRec.linkToParent(parentSheetName, rk, pk);

		}
		recs.add(outRec);
		if (this.childrenToBeRead == null) {
			return;
		}
		/*
		 * child sheets are hierarchical only if this is output as sheet
		 */
		DbTable par = thisIsSheet ? this : null;
		for (String child : this.childrenToBeRead) {
			DbTable cr = (DbTable) Application.getActiveInstance().getRecord(child);
			cr.addOutputRecordsCascaded(recs, par, true);
		}
	}

	/**
	 * @return list of output which output records are to be added
	 */
	public InputRecord[] getInputRecords() {
		int nrecs = 1;
		if (this.childrenToBeSaved != null) {
			nrecs = this.childrenToBeSaved.length + 1;
		}
		InputRecord[] recs = new InputRecord[nrecs];
		recs[0] = InputRecord.getInputRecord(this.getQualifiedName(), this.getDefaultSheetName(), DataPurpose.SAVE,
				false);
		Application app = Application.getActiveInstance();
		if (this.childrenToBeSaved != null) {
			int i = 1;
			for (String child : this.childrenToBeSaved) {
				Record childRec = app.getRecord(child);
				String sheetName = childRec.getDefaultSheetName();
				recs[i] = InputRecord.getInputRecord(child, sheetName, DataPurpose.SAVE, true);
				i++;
			}
		}

		return recs;
	}

	@Override
	public Field[] getFieldsToBeInput(String[] names, DataPurpose purpose, boolean extractSaveAction) {
		/*
		 * is the caller choosing a subset of fields?
		 */
		if (names != null || purpose == null || purpose == DataPurpose.OTHERS) {
			return super.getFieldsToBeInput(names, purpose, extractSaveAction);
		}

		if (purpose == DataPurpose.READ) {
			/*
			 * we read only keys field
			 */
			return this.allPrimaryKeys;
		}
		return this.fieldsToInput;
	}

	/**
	 * @param recs
	 *            list to which output records are to be added
	 * @param parentSheetName
	 * @param parentKey
	 */

	/**
	 * @param parentSheetName
	 *            if this sheet is to be output as a child. null if a normal
	 *            sheet
	 * @return output record that will copy data sheet to output
	 */
	@Override
	public InputRecord getInputRecord(String parentSheetName) {
		if (parentSheetName == null) {
			return new InputRecord(this.getQualifiedName(), this.defaultSheetName);
		}
		int nbr = this.allParentKeys.length;
		String[] thisKeys = new String[nbr];
		String[] linkKeys = new String[nbr];
		for (int i = 0; i < this.allParentKeys.length; i++) {
			Field key = this.allParentKeys[i];
			thisKeys[i] = key.getName();
			linkKeys[i] = key.getReferredField();
		}
		return new InputRecord(this.getQualifiedName(), this.defaultSheetName, parentSheetName, thisKeys, linkKeys);
	}

	/**
	 * set sql strings. We are setting four fields at the end. For clarity, you
	 * should trace one string at a time and understand what we are trying to
	 * do. Otherwise it looks confusing
	 *
	 * @param row
	 * @param userId
	 * @return
	 */
	private Value[] getUpdateValues(IFieldsCollection row, Value userId) {
		Value[] values = new Value[this.nbrUpdateFields];
		int i = 0;
		for (Field f : this.fields) {
			DbField field = (DbField) f;
			/*
			 * some fields are not updatable
			 */
			if (!field.canUpdate()) {
				continue;
			}
			if (field instanceof ModifiedByUser) {
				values[i] = userId;
				i++;
				continue;
			}
			Value value = field.getValue(row, null);
			if (Value.isNull(value)) {
				if (field.isNullable()) {
					value = Value.newUnknownValue(field.getValueType());
				} else {
					throw new ApplicationError("Column " + field.getColumnName() + " in table " + this.tableName
							+ " is designed to be non-null, but a row is being updated with a null value in it.");
				}
			}
			values[i] = value;
			i++;
		}
		/*
		 * where clause of delete and update are same, but they are valid only
		 * if we have a primary key
		 */

		for (Field field : this.allPrimaryKeys) {
			values[i] = row.getValue(field.getName());
			i++;
		}

		if (this.useTimestampForConcurrency) {
			if (!row.hasValue(this.modifiedStampField.getName())) {
				throw new ApplicationError("Timestamp field for concurrency is required "
						+ this.modifiedStampField.getName() + " is not available ");
			}
			values[i] = row.getValue(this.modifiedStampField.getName());
			i++;
		}
		return values;
	}

	private void setPrimaryWhere() {
		StringBuilder where = new StringBuilder(" WHERE ");

		boolean firstTime = true;
		for (DbField field : this.allPrimaryKeys) {
			if (firstTime) {
				firstTime = false;
			} else {
				where.append(" AND ");
			}
			where.append(field.getColumnName()).append(DbTable.EQUAL).append(DbTable.PARAM);
		}
		this.primaryWhereClause = where.toString();
	}

	private void setListSql() {
		DbField field = (DbField) this.getField(this.listFieldName);
		if (field == null) {
			this.invalidFieldName(this.listFieldName);
			return;
		}
		StringBuilder sbf = new StringBuilder();
		sbf.append("SELECT ");
		/*
		 * if this record has no primary key at all, or the listFieldName itself
		 * is the key, then we are to select just the lustField.
		 */
		if (this.allPrimaryKeys == null || this.listFieldName.equals(this.allPrimaryKeys[0].getName())) {
			this.valueListTypes = new ValueType[1];
			this.valueListTypes[0] = field.getValueType();
		} else {
			/*
			 * we have to select the primary key and the list field
			 */
			DbField keyField = this.allPrimaryKeys[0];
			sbf.append(keyField.getColumnName()).append(" id,");
			this.valueListTypes = new ValueType[2];
			this.valueListTypes[0] = keyField.getValueType();
			this.valueListTypes[1] = field.getValueType();
		}
		sbf.append(field.getColumnName()).append(" value from ").append(this.tableName);
		if (this.listGroupKeyName != null) {
			field = (DbField) this.getField(this.listGroupKeyName);
			if (field == null) {
				this.invalidFieldName(this.listGroupKeyName);
				return;
			}
			sbf.append(" WHERE ").append(field.getColumnName()).append(EQUAL_PARAM);
			this.valueListKeyType = field.getValueType();
		}
		this.listSql = sbf.toString();
	}

	/**
	 * does the list service from this record need two columns?
	 *
	 * @return true if this record specifies an intenal key and value as list
	 */
	public boolean listServiceUsesTwoColumns() {
		return this.valueListTypes.length > 1;
	}

	private void setSuggestSql() {
		DbField field = (DbField) this.getField(this.suggestionKeyName);
		if (field == null) {
			this.invalidFieldName(this.suggestionKeyName);
			return;
		}
		if (this.suggestionOutputNames == null || this.suggestionOutputNames.length == 0) {
			throw new ApplicationError(
					"Record " + this.getQualifiedName() + " specifies suggestion key but no suggestion output fields");
		}
		StringBuilder sbf = new StringBuilder();
		sbf.append("SELECT ");
		for (String fieldName : this.suggestionOutputNames) {
			DbField f = (DbField) this.getField(fieldName);
			if (f == null) {
				this.invalidFieldName(this.suggestionKeyName);
				return;
			}
			sbf.append(f.getColumnName()).append(' ').append(f.getName()).append(COMMA);
		}
		sbf.setLength(sbf.length() - 1);
		sbf.append(" from ").append(this.tableName).append(" WHERE ").append(field.getColumnName()).append(" LIKE ?");
		this.suggestSql = sbf.toString();
	}

	/**
	 * get list of values, typically for drop-down control
	 *
	 * @param keyValue
	 * @param handle
	 * @param userId
	 * @return sheet that has the data
	 */
	public IDataSheet list(String keyValue, IReadOnlyHandle handle, Value userId) {
		Value[] values = null;
		if (this.listGroupKeyName != null) {
			if (keyValue == null || keyValue.length() == 0) {
				return null;
			}
			values = new Value[1];
			values[0] = Value.parseValue(keyValue, this.valueListKeyType);
		}
		IDataSheet sheet = null;
		if (this.valueListTypes.length == 1) {
			sheet = new MultiRowsSheet(SINGLE_HEADER, this.valueListTypes);
		} else {
			sheet = new MultiRowsSheet(DOUBLE_HEADER, this.valueListTypes);
		}
		handle.read(this.listSql, values, sheet);
		return sheet;
	}

	@Override
	public void getReadyExtension(Record refRecord) {
		if (this.tableName == null) {
			this.tableName = this.name;
		}
		int nbrPrimaries = 0;
		int nbrParents = 0;
		/*
		 * fields that are to be input from client for this dbTable.
		 *
		 * One extra for SAVE action
		 */
		Field[] inpFields = new Field[this.fields.length + 1];
		int inpIdx = 0;
		for (int i = 0; i < this.fields.length; i++) {
			Field f = this.fields[i];
			if (f instanceof DbField == false) {
				throw new ApplicationError(
						"Non-db field " + f.getName() + " defined inside a db table " + this.getQualifiedName());
			}
			DbField field = (DbField) f;
			if (field.toBeInput()) {
				inpFields[inpIdx] = f;
				inpIdx++;
			}
			if (field.isPrimaryKey()) {
				nbrPrimaries++;
			}
			if (field.isParentKey()) {
				nbrParents++;
			}
			if (field instanceof CreatedByUser) {
				this.checkDuplicateError(this.createdUserField, CreatedByUser.class);
				this.createdUserField = field;
			} else if (field instanceof CreatedTimestamp) {
				this.checkDuplicateError(this.createdStampField, CreatedTimestamp.class);
				this.createdStampField = field;
			} else if (field instanceof ModifiedByUser) {
				this.checkDuplicateError(this.modifiedUserField, ModifiedByUser.class);
				this.modifiedUserField = field;
			} else if (field instanceof ModifiedTimestamp) {
				this.checkDuplicateError(this.modifiedStampField, ModifiedTimestamp.class);
				this.modifiedStampField = field;
			}
		}
		inpFields[inpIdx] = TABLE_ACTION_FIELD;

		if (inpIdx == this.fields.length) {
			this.fieldsToInput = inpFields;
		} else {
			try {
				this.fieldsToInput = Arrays.copyOf(inpFields, inpIdx + 1);
			} catch (Exception e) {
				throw new ApplicationError(e, "Error while copying DbTable fields");
			}
		}
		if (this.nbrGenericFields != 0) {
			this.addGenericFields();
		}
		/*
		 * because of possible composite keys, we save keys in arrays
		 */
		if (nbrPrimaries > 0 || nbrParents > 0) {
			this.cacheSpecialFields(nbrPrimaries, nbrParents);
		}
		/*
		 * are we ok for concurrency check?
		 */
		if (this.useTimestampForConcurrency) {
			if (this.modifiedStampField == null) {
				throw new ApplicationError("Record " + this.name
						+ " has set useTimestampForConcurrency=true, but not has marked any field as modifiedAt.");
			}
			if (this.modifiedStampField.getValueType() != ValueType.TIMESTAMP) {
				throw new ApplicationError("Record " + this.name + " uses " + this.modifiedStampField.getName()
						+ " as modiedAt field, but has defined it as a " + this.modifiedStampField.getValueType()
						+ ". It should be defined as a TIMESTAMP for it to be used for concurrency check.");
			}
		}

		if (this.allPrimaryKeys != null) {
			this.setPrimaryWhere();
		}
		/*
		 * get ready with sqls for reading
		 */
		this.createReadSqls();

		/*
		 * is this record writable?
		 */
		if (this.readOnly == false && this.allPrimaryKeys != null) {
			this.createWriteSqls();
		}
	}

	/**
	 * append generic fields to this.fields array
	 */
	private void addGenericFields() {
		int idx = this.fields.length;
		try {
			this.fields = Arrays.copyOf(this.fields, idx + this.nbrGenericFields);
		} catch (Exception e) {
			throw new ApplicationError(e, "Error while copying DbTable fields to accomodate generic fields");
		}
		String fieldPrefix = this.name + '_';
		for (int i = 0; i < this.nbrGenericFields; i++) {
			this.fields[idx++] = DbField.getDefaultField(fieldPrefix + i, ValueType.TEXT);
		}
	}

	/**
	 * field name specified at record level is not defined as a field
	 *
	 * @param fieldName
	 */
	private void invalidFieldName(String fieldName) {
		throw new ApplicationError(
				fieldName + " is specified as a field in record " + this.name + " but that field is not defined.");
	}

	/** Create read and filter sqls */
	private void createReadSqls() {

		StringBuilder select = new StringBuilder("SELECT ");

		boolean isFirstField = true;
		for (Field f : this.fields) {
			DbField field = (DbField) f;
			if (isFirstField) {
				isFirstField = false;
			} else {
				select.append(DbTable.COMMA);
			}
			select.append(field.getColumnName()).append(" \"").append(field.getName()).append('"');
		}

		select.append(" FROM ").append(this.tableName);

		/*
		 * filter sql stops at where. Actual where clauses will be added at run
		 * time
		 */
		String selectText = select.toString();
		this.filterSql = selectText + " WHERE ";

		/*
		 * read is applicable if there is primary key
		 */
		if (this.allPrimaryKeys != null) {
			/*
			 * where clause is common across different sqls..
			 */
			this.readSql = selectText + this.primaryWhereClause;
		}

		if (this.listFieldName != null) {
			this.setListSql();
		}
		if (this.suggestionKeyName != null) {
			this.setSuggestSql();
		}
	}

	private String getTimeStamp() {
		IDbDriver driver = Application.getActiveInstance().getRdbSetup().getDefaultDriver();
		if (driver == null) {
			return DbVendor.MYSQL.getTimeStamp();
		}
		return driver.getTimeStampFn();
	}

	/**
	 * set sql strings. We are setting four fields at the end. For clarity, you
	 * should trace one string at a time and understand what we are trying to
	 * do. Otherwise it looks confusing
	 */
	private void createWriteSqls() {
		String timeStamp = this.getTimeStamp();
		/*
		 * we have two buffers for insert as fields are to be inserted at two
		 * parts
		 */
		StringBuilder insert = new StringBuilder("INSERT INTO ");
		insert.append(this.tableName).append('(');
		StringBuilder vals = new StringBuilder(") Values(");

		StringBuilder update = new StringBuilder("UPDATE ");
		update.append(this.tableName).append(" SET ");

		boolean firstInsertField = true;
		boolean firstUpdatableField = true;
		for (Field f : this.fields) {
			DbField field = (DbField) f;
			/*
			 * some fields are not updatable
			 */
			boolean isModStamp = field instanceof ModifiedTimestamp;
			if (field.canUpdate() || isModStamp) {
				if (firstUpdatableField) {
					firstUpdatableField = false;
				} else {
					update.append(COMMA);
				}
				update.append(field.getColumnName()).append(DbTable.EQUAL);
				if (field instanceof ModifiedTimestamp) {
					update.append(timeStamp);
				} else {
					update.append(DbTable.PARAM);
					this.nbrUpdateFields++;
				}
			}
			if (field.canInsert() == false) {
				continue;
			}
			if (this.keyIsGenerated && field.isPrimaryKey()) {
				continue;
			}

			if (firstInsertField) {
				firstInsertField = false;
			} else {
				insert.append(DbTable.COMMA);
				vals.append(DbTable.COMMA);
			}
			insert.append(field.getColumnName());
			/*
			 * value is hard coded for time stamps
			 */
			if (isModStamp || field instanceof CreatedTimestamp) {
				vals.append(timeStamp);
			} else {
				vals.append(DbTable.PARAM);
				this.nbrInsertFields++;
			}
		}
		/*
		 * set insert sql
		 */
		insert.append(vals.append(')'));
		this.insertSql = insert.toString();

		this.nbrUpdateFields += this.allPrimaryKeys.length;
		this.deleteSql = "DELETE FROM " + this.tableName + this.primaryWhereClause;
		if (this.useTimestampForConcurrency) {
			String clause = " AND " + this.modifiedStampField.getColumnName() + "=?";
			this.updateSql = update.append(this.primaryWhereClause).append(clause).toString();
			this.deleteSql += clause;
			this.nbrUpdateFields++;

		} else {
			this.updateSql = update.append(this.primaryWhereClause).toString();
		}
	}

	private void checkDuplicateError(Field savedField, Class<?> cls) {
		if (savedField == null) {
			return;
		}

		throw new ApplicationError("Record " + this.getQualifiedName() + " defines more than one field with field type "
				+ cls.getSimpleName() + ". This feature is not supported");
	}

	/**
	 *
	 * @param forRead
	 * @return array of child records as related records that are suitable
	 *         record based actions
	 */
	public RelatedRecord[] getChildRecordsAsRelatedRecords(boolean forRead) {
		String[] children;
		if (forRead) {
			children = this.getChildrenToBeRead();
		} else {
			children = this.getChildrenToBeSaved();
		}
		if (children == null) {
			return null;
		}
		RelatedRecord[] recs = new RelatedRecord[children.length];
		int i = 0;
		for (String child : children) {
			Record childRecord = Application.getActiveInstance().getRecord(child);
			RelatedRecord rr = new RelatedRecord(child, childRecord.getDefaultSheetName());
			rr.getReady();
			recs[i++] = rr;
		}
		return recs;
	}

	protected void validateTable(IMetadataHandle handle, Map<String, Field> columnMap, IValidationContext vtx) {
		String nam = this.tableName;
		if (nam == null) {
			nam = this.name;
		}
		IDataSheet columns = handle.getTableColumns(nam);
		if (columns == null) {
			vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
					this.tableName + " is not a valid table/view defined in the data base", "tableName"));
			return;
		}
		int nbrCols = columns.length();
		/*
		 * as of now, we check only if names match. we will do more. refer to
		 * DbDrive.COL_NAMES for sequence of columns in each row of columns data
		 * sheet
		 */
		for (int i = 0; i < nbrCols; i++) {
			Value[] row = columns.getRow(i);
			String colName = row[2].toText();
			/*
			 * we should cross-check value type and size. As of now let us check
			 * for length issues with text fields
			 */
			Field field = columnMap.remove(colName);
			if (field == null) {
				/*
				 * column not in this record. No problems.
				 */
				continue;
			}
		}
		if (columnMap.size() > 0) {
			for (String key : columnMap.keySet()) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
						key + " is not a valid column name in the data base table/view", null));
			}
		}
	}

	/**
	 * extract rows matching/starting with supplied chars. Typically for a
	 * suggestion list
	 *
	 * @param keyValue
	 * @param matchStarting
	 * @param handle
	 * @param userId
	 * @return sheet that has the data
	 */
	public IDataSheet suggest(String keyValue, boolean matchStarting, IReadOnlyHandle handle, Value userId) {
		String text = keyValue + '%';
		if (!matchStarting) {
			text = '%' + text;
		}
		Value[] values = new Value[1];
		values[0] = Value.newTextValue(text);
		IDataSheet sheet = this.createSheet(this.suggestionOutputNames, false, false);
		handle.read(this.suggestSql, values, sheet);
		return sheet;
	}

	/**
	 * @return the suggestionKeyName
	 */
	public String getSuggestionKeyName() {
		return this.suggestionKeyName;
	}

	/**
	 * @return the suggestionOutputNames
	 */
	public String[] getSuggestionOutputNames() {
		return this.suggestionOutputNames;
	}

	/**
	 * @return the valueListKeyName
	 */
	public String getValueListKeyName() {
		return this.listGroupKeyName;
	}

	/**
	 * @return table name for this record
	 */
	public String getTableName() {
		return this.tableName;
	}

	/**
	 * @param inRecord
	 *            record to be used to input filter fields
	 * @param inData
	 *            that has the values for filter fields
	 * @param handle
	 * @param useCompactFormat
	 *            json compact format is an array of arrays of data, with first
	 *            row as header. Otherwise, each row is an object
	 * @param writer
	 *            Response writer to which we will output 0 or more objects or
	 *            arrays. (Caller should have started an array. and should end
	 *            array after this call
	 */
	public void filterToJson(Record inRecord, IFieldsCollection inData, IReadOnlyHandle handle,
			boolean useCompactFormat,
			IResponseWriter writer) {
		/*
		 * we have to create where clause with ? and corresponding values[]
		 */
		SqlAndValues temp = this.getSqlAndValues(handle, inData, inRecord);
		String[] names = this.getFieldNames();
		IResultSetReader reader = RdbUtil.newReaderForResponseWriter(writer, useCompactFormat, names,
				this.getValueTypes());
		handle.read(temp.sql, temp.values, reader);
	}

	/**
	 * worker method to create a prepared statement and corresponding values for
	 * filter method
	 *
	 * @param inData
	 * @param inRecord
	 * @return struct that has both sql and values
	 */
	private SqlAndValues getSqlAndValues(IReadOnlyHandle handle, IFieldsCollection inData, Record inRecord) {
		StringBuilder sql = new StringBuilder(this.filterSql);
		List<Value> filterValues = new ArrayList<Value>();
		boolean firstTime = true;
		for (Field f : inRecord.fields) {
			DbField field = (DbField) f;
			String fieldName = field.getName();
			Value value = inData.getValue(fieldName);
			if (Value.isNull(value) || value.toString().isEmpty()) {
				continue;
			}
			if (firstTime) {
				firstTime = false;
			} else {
				sql.append(" AND ");
			}

			FilterCondition condition = FilterCondition.Equal;
			Value otherValue = inData.getValue(fieldName + AppConventions.Name.COMPARATOR_SUFFIX);
			if (Value.isNull(otherValue) == false) {
				String text = otherValue.toString();
				/*
				 * it could be raw text like "~" or parsed value like
				 * "GreaterThan"
				 */
				condition = FilterCondition.valueOf(text);
				if (condition == null) {
					condition = FilterCondition.parse(text);
				}
				if (condition == null) {
					throw new ApplicationError(
							"Context has an invalid filter condition of " + text + " for field " + fieldName);
				}
			}

			/** handle the special case of in-list */
			if (condition == FilterCondition.In) {
				Value[] values = Value.parse(value.toString().split(","), field.getValueType());
				/*
				 * we are supposed to have validated this at the input gate...
				 * but playing it safe
				 */
				if (values == null) {
					throw new ApplicationError(
							value + " is not a valid comma separated list for field " + field.getName());
				}
				sql.append(field.getColumnName()).append(" in (?");
				filterValues.add(values[0]);
				for (int i = 1; i < values.length; i++) {
					sql.append(",?");
					filterValues.add(values[i]);
				}
				sql.append(") ");
				continue;
			}

			if (condition == FilterCondition.Like) {
				value = Value
						.newTextValue(DbTable.PERCENT + handle.escapeForLike(value.toString()) + DbTable.PERCENT);
			} else if (condition == FilterCondition.StartsWith) {
				value = Value.newTextValue(handle.escapeForLike(value.toString()) + DbTable.PERCENT);
			}

			sql.append(field.getColumnName()).append(condition.getSql()).append("?");
			filterValues.add(value);

			if (condition == FilterCondition.Between) {
				otherValue = inData.getValue(fieldName + AppConventions.Name.TO_FIELD_SUFFIX);
				if (otherValue == null || otherValue.isUnknown()) {
					throw new ApplicationError("To value not supplied for field " + this.name + " for filtering");
				}
				sql.append(" AND ?");
				filterValues.add(otherValue);
			}
		}
		Value[] values;
		if (firstTime) {
			/*
			 * no conditions..
			 */
			if (this.okToSelectAll == false) {
				throw new ApplicationError("Record " + this.name
						+ " is likely to contain large number of records, and hence we do not allow select-all operation");
			}
			sql.append(" 1 = 1 ");
			values = new Value[0];
		} else {
			values = filterValues.toArray(new Value[0]);
		}
		/*
		 * is there sort order?
		 */
		Value sorts = inData.getValue(AppConventions.Name.SORT_COLUMN);
		if (sorts != null) {
			sql.append(" ORDER BY ").append(sorts.toString());
		}
		return new SqlAndValues(sql.toString(), values);
	}

	/**
	 * @param inData
	 * @return
	 */
	private IDataSheet getRowFromCache(IFieldsCollection values) {
		IAppDataCacher cacher = Application.getActiveInstance().getAppDataCacher();
		if (cacher == null) {
			return null;
		}
		String key1 = this.getCachingKey(values);
		String key2 = this.getSecondaryKey(values);
		Object obj = cacher.get(key1, key2);
		if (obj == null) {
			return null;
		}
		logger.info("Row located in cache for primary key {} and secondary key {}", key1, key2);
		return (IDataSheet) obj;
	}

	/**
	 * cache a row for this input
	 *
	 * @param values
	 *            input values
	 * @param row
	 *            output to be cached
	 */
	private void cacheRow(IFieldsCollection values, IDataSheet row) {
		IAppDataCacher cacher = Application.getActiveInstance().getAppDataCacher();
		if (cacher == null) {
			return;
		}
		cacher.put(this.getCachingKey(values), this.getSecondaryKey(values), row);
	}

	/**
	 * remove all cache for an update using the input values. we invalidate list
	 * as well as all individual cach
	 *
	 * @param values
	 */
	private void invalidateCache(IFieldsCollection values) {
		IAppDataCacher cacher = Application.getActiveInstance().getAppDataCacher();
		if (cacher == null) {
			return;
		}
		String groupKey = null;
		if (this.listGroupKeyName != null) {
			groupKey = values.getValue(this.listGroupKeyName).toString();
		}
		for (String recName : this.recordsToBeNotifiedOnChange) {
			DbTable rec = (DbTable) Application.getActiveInstance().getRecord(recName);
			String cacheKey = rec.getCachingKey(groupKey);
			cacher.invalidate(cacheKey);
			cacher.invalidate(cacheKey, null);
		}
	}

	/**
	 * get caching key for a given group key value
	 *
	 * @param groupKeyValue
	 *            null if this record is designed with no group key
	 * @return string to be used as primary/sole key for caching
	 */
	private String getCachingKey(String groupKeyValue) {
		if (this.listGroupKeyName == null) {
			return KEY_PREFIX + this.getQualifiedName();
		}
		return KEY_PREFIX + this.getQualifiedName() + KEY_JOINER + groupKeyValue;
	}

	/**
	 * get caching key by picking up the group key, if required, from the input
	 * values
	 *
	 * @param values
	 * @return string to be used as primary/sole key for caching
	 */
	private String getCachingKey(IFieldsCollection inputValues) {
		if (this.listGroupKeyName == null) {
			return KEY_PREFIX + this.getQualifiedName();
		}
		return KEY_PREFIX + this.getQualifiedName() + KEY_JOINER
				+ inputValues.getValue(this.listGroupKeyName).toString();
	}

	/**
	 * get cache key for the key field value(s)
	 *
	 * @param values
	 *            from which key values are extracted
	 * @return string to be used as secondary key for caching.
	 */
	private String getSecondaryKey(IFieldsCollection values) {
		StringBuilder result = new StringBuilder();
		for (Field field : this.allPrimaryKeys) {
			/*
			 * we have an extra null at the end. that is OKm so long as we are
			 * consistent
			 */
			result.append(values.getValue(field.getName()).toString()).append(KEY_JOINER);
		}
		return result.toString();
	}

	/**
	 *
	 * @return schema name with which this record is associated with. Null if it
	 *         uses default schema
	 */
	public String getSchemaName() {
		return this.schemaName;
	}

	/**
	 *
	 * @return true if the data from this table is quite static and the client
	 *         may cache it. Note that the design allows invalidating the cache
	 *         as and when the data changes
	 */
	public boolean okToCache() {
		return this.okToCache;
	}

	@Override
	public boolean isKeyGenerated() {
		return this.keyIsGenerated;
	}

}

class SqlAndValues {
	final String sql;
	final Value[] values;

	SqlAndValues(String sql, Value[] values) {
		this.sql = sql;
		this.values = values;
	}
}
