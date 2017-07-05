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
package org.simplity.kernel.dm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.simplity.json.JSONArray;
import org.simplity.json.JSONException;
import org.simplity.json.JSONObject;
import org.simplity.json.JSONWriter;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FilterCondition;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.Messages;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.Component;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.data.DataPurpose;
import org.simplity.kernel.data.DataSerializationType;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.FieldsInterface;
import org.simplity.kernel.data.FlatFileRowType;
import org.simplity.kernel.data.InputRecord;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.data.OutputRecord;
import org.simplity.kernel.data.SingleRowSheet;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.dt.DataType;
import org.simplity.kernel.dt.DataTypeSuggester;
import org.simplity.kernel.file.FileManager;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.kernel.util.TextUtil;
import org.simplity.kernel.util.XmlUtil;
import org.simplity.kernel.value.BooleanValue;
import org.simplity.kernel.value.IntegerValue;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.simplity.service.ResponseWriter;
import org.simplity.service.ServiceContext;
import org.simplity.service.ServiceProtocol;

/**
 *
 * This is the main part of our data model. Every piece of data that the
 * application has to keep as part of "system of records" must be modeled into
 * this. Data structures that are used as input to, or output from a service are
 * modeled as records as well.
 *
 * It is common industry practice to have "physical data model" and
 * "logical data model" for proper understanding. We encourage such designs
 * before recording them using the Record concept.
 *
 * Record is a good candidate to represent any data structure that is used
 * across components, even if it is not persisted.
 *
 */
public class Record implements Component {
	/**
	 * header row of returned sheet when there is only one column
	 */
	private static String[] SINGLE_HEADER = { "value" };
	/**
	 * header row of returned sheet when there are two columns
	 */
	private static String[] DOUBLE_HEADER = { "key", "value" };
	private static final char COMMA = ',';
	private static final char PARAM = '?';
	private static final char EQUAL = '=';
	private static final String EQUAL_PARAM = "=?";
	private static final char PERCENT = '%';
	private static final String TABLE_ACTION_FIELD_NAME = ServiceProtocol.TABLE_ACTION_FIELD_NAME;

	/*
	 * initialization deferred because it needs bootstrapping..
	 */
	private static Field TABLE_ACTION_FIELD = null;

	private static final ComponentType MY_TYPE = ComponentType.REC;

	/*
	 * oracle sequence name is generally tableName_SEQ.
	 */
	private static final String DEFAULT_SEQ_SUFFIX = "_SEQ.NEXTVAL";

	/***
	 * Name of this record/entity, as used in application
	 */
	String name;

	/**
	 * module name + name would be unique for a component type within an
	 * application. we also insist on a java-like convention that the the
	 * resource is stored in a folder structure that mimics module name
	 */
	String moduleName;
	/**
	 * type of this record
	 */
	RecordUsageType recordType = RecordUsageType.STORAGE;
	/**
	 * name of the rdbms table, if this is either a storage table, or a view
	 * that is to be defined in the rdbms
	 */
	String tableName;

	/**
	 * has this table got an internal key, and do you want it to be managed
	 * automatically?
	 */
	boolean keyToBeGenerated;
	/**
	 * oracle does not support auto-increment. Standard practice is to have a
	 * sequence, typically named as tableName_SEQ, and use sequence.NEXTVAL as
	 * value of the key field. If you follow different standard that that,
	 * please specify the expression. We have made this an expression to provide
	 * flexibility to have any expression, including functions that you may have
	 * written.
	 */
	String sequenceName;
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
	String[] childrenToBeRead = null;
	/**
	 * child records to be saved along with this record. operations for this
	 * record
	 */
	String[] childrenToBeSaved = null;
	/**
	 * fields that this record is made-up of
	 */
	Field[] fields = new Field[0];

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
	String listGroupKeyName = null;

	/**
	 * what is the sheet name to be used as input/output sheet. (specifically
	 * used in creating services on the fly)
	 */
	String defaultSheetName = null;

	/**
	 * if this record is used for a suggestion service, field that is used for
	 * search
	 */
	String suggestionKeyName;
	/**
	 * what fields do we respond back with for a suggestion service
	 */
	String[] suggestionOutputNames;
	/**
	 * is this record only for reading?
	 */
	boolean readOnly;

	/**
	 * in case this is a view, then the record from which fields are referred by
	 * default
	 */
	String defaultRefRecord;

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
	 * if this table is (almost) static, and the vauleList that is delivered on
	 * a list request can be cached by the agent. Valid only if valueListField
	 * is set (list_ auto service is enabled) if valueListKey is specified, the
	 * result will be cached by that field. For example, by country-code.
	 */
	boolean okToCacheList;

	/**
	 * If this record represents a data structure corresponding to an object
	 * defined in the RDBMS, what is the Object name in the sql. This is used
	 * while handling stored procedure parameters that pass objects and array of
	 * objects
	 */
	String sqlStructName;

	/**
	 * do you intend to use this to read from /write to flat file with fixed
	 * width rows? If this is set to true, then each field must set fieldWidth;
	 */
	boolean forFixedWidthRow;

	/*
	 * following fields are assigned for caching/performance
	 */
	/**
	 * Is there at least one field that has validations linked to another field.
	 * Initialized during init operation
	 */
	private boolean hasInterFieldValidations;

	/*
	 * standard fields are cached
	 */
	private Field modifiedStampField;
	private Field modifiedUserField;
	private Field createdUserField;

	/**
	 * we need the set to validate field names at time
	 */
	private final Map<String, Field> indexedFields = new HashMap<String, Field>();

	/**
	 * and field names of course. cached after loading
	 */
	private String[] fieldNames;
	/**
	 * sql for reading a row for given primary key value
	 */
	private String readSql;

	/**
	 * select f1,f2,..... WHERE used in filtering
	 */
	private String filterSql;

	/**
	 * sql ready to insert a row into the table
	 */
	private String insertSql;

	/**
	 * sql to update every field. (Not selective update)
	 */
	private String updateSql;

	/**
	 * we skip few standard fields while updating s row. Keeping this count
	 * simplifies code
	 */
	private String deleteSql;

	/**
	 * sql to be used for a list action
	 */
	private String listSql;
	/**
	 * value types of fields selected for list action
	 */
	private ValueType[] valueListTypes;
	/**
	 * value type of key used in list action
	 */
	private ValueType valueListKeyType;

	/**
	 * sql to be used for a suggestion action
	 */
	private String suggestSql;

	/**
	 * sequence of oracle if required
	 */
	private String sequence;

	/**
	 * This record is a dataObject if any of its field is non-primitive (array
	 * or child-record
	 */
	private boolean isComplexStruct;

	/**
	 * min length of record in case forFixedWidthRow = true
	 */
	private int minRecordLength;

	/**
	 * in case the primary is a composite key : with more than one fields, then
	 * we keep all of them in an array. This is null if we have a single key. We
	 * have designed it that way to keep the single key case as simple as
	 * possible
	 */
	private Field[] allPrimaryKeys;

	/**
	 * parent key, in case parent has composite primary key
	 */
	private Field[] allParentKeys;

	/**
	 * " WHERE key1=?,key2=?
	 */
	private String primaryWhereClause;

	private int nbrUpdateFields;

	private int nbrInsertFields;

	private Field[] encryptedFields;
	/*
	 * methods for ComponentInterface
	 */

	@Override
	public String getSimpleName() {
		return this.name;
	}

	/**
	 * @param fieldName
	 * @return field or null
	 */
	public Field getField(String fieldName) {
		return this.indexedFields.get(fieldName);
	}

	/**
	 * @param fieldName
	 * @return field or null
	 */
	public int getFieldIndex(String fieldName) {
		for (int i = 0; i < this.fieldNames.length; i++) {
			if (this.fieldNames[i].equals(fieldName)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * to avoid confusion, we do not have a method called getName.
	 *
	 * @return qualified name of this record
	 */
	@Override
	public String getQualifiedName() {
		if (this.moduleName == null) {
			return this.name;
		}
		return this.moduleName + '.' + this.name;
	}

	/**
	 * @return array of primary key fields.
	 */
	public Field[] getPrimaryKeyFields() {
		return this.allPrimaryKeys;
	}

	/**
	 *
	 * @return dependent children to be input for saving
	 */
	public String[] getChildrenToInput() {
		return this.childrenToBeSaved;
	}

	/**
	 *
	 * @return dependent child records that are read along
	 */
	public String[] getChildrenToOutput() {
		return this.childrenToBeRead;
	}

	/**
	 * get the default name of data sheet to be used for input/output
	 *
	 * @return sheet name
	 */
	public String getDefaultSheetName() {
		return this.defaultSheetName;
	}

	/**
	 * get value types of fields in the same order that getFieldName() return
	 * field names
	 *
	 * @return value types of fields
	 */
	public ValueType[] getValueTypes() {
		ValueType[] types = new ValueType[this.fieldNames.length];
		int i = 0;
		for (Field field : this.fields) {
			types[i] = field.getValueType();
			i++;
		}
		return types;
	}

	/**
	 * get value types of fields in the same order that getFieldName() and an
	 * additional field for save action
	 *
	 * @return value types of fields and the last field a save action, which is
	 *         TEXT
	 */
	private Field[] getFieldsWithSave() {
		int n = this.fields.length + 1;
		Field[] allFields = new Field[n];
		n = 0;
		for (Field field : this.fields) {
			allFields[n++] = field;
		}
		allFields[n] = TABLE_ACTION_FIELD;
		return allFields;
	}

	/**
	 * to avoid confusion, we do not have a method called setName. name is
	 * split, if required into module name and name
	 *
	 * @param nam
	 *            qualified name
	 *
	 */
	public void setQualifiedName(String nam) {
		int idx = nam.lastIndexOf('.');
		if (idx == -1) {
			this.name = nam;
			return;
		}
		this.moduleName = nam.substring(0, idx);
		this.name = nam.substring(idx + 1);
	}

	/**
	 *
	 * @param moduleName
	 */
	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	/**
	 *
	 * @param tableName
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	/**
	 * create an empty data sheet based on this record
	 *
	 * @param forSingleRow
	 *            true if you intend to store only one row. False otherwise
	 * @param addActionColumn
	 *            should we add a column to have action to be performed on each
	 *            row
	 *
	 * @return an empty sheet ready to receive data
	 */
	public DataSheet createSheet(boolean forSingleRow, boolean addActionColumn) {
		Field[] sheetFeilds = this.fields;
		if (addActionColumn) {
			sheetFeilds = this.getFieldsWithSave();
		}
		if (forSingleRow) {
			return new SingleRowSheet(sheetFeilds);
		}
		return new MultiRowsSheet(sheetFeilds);
	}

	/**
	 * create an empty data sheet based on this record for a subset of fields
	 *
	 * @param colNames
	 *            subset of column names to be included
	 * @param forSingleRow
	 *            true if you intend to use this to have just one row
	 * @param addActionColumn
	 *            should we add a column to have action to be performed on each
	 *            row
	 * @return an empty sheet ready to receive rows of data
	 */
	public DataSheet createSheet(String[] colNames, boolean forSingleRow, boolean addActionColumn) {
		Field[] subset = new Field[colNames.length];
		int i = 0;
		for (String colName : colNames) {
			Field field = this.indexedFields.get(colName);
			if (field == null) {
				throw new ApplicationError("Record " + this.getQualifiedName() + " has no field named " + colName);
			}
			subset[i] = field;
			i++;
		}

		if (forSingleRow) {
			return new SingleRowSheet(subset);
		}
		return new MultiRowsSheet(subset);
	}

	/**
	 * read, in our vocabulary, is ALWAYS primary key based read. Hence we
	 * expect (at most) one row of output per read. If values has more than one
	 * rows, we read for primary key in each row.
	 *
	 * @param inSheet
	 *            one or more rows that has value for the primary key.
	 * @param outSheet
	 *            sheet to which rows are to be extracted
	 *
	 * @param driver
	 * @param userId
	 *            we may have a common security strategy to restrict output rows
	 *            based on user. Not used as of now
	 * @return data sheet for sure. With data that is returned by the driver
	 */
	public int readMany(DataSheet inSheet, DataSheet outSheet, DbDriver driver, Value userId) {
		if (this.allPrimaryKeys == null) {
			throw new ApplicationError("Record " + this.name
					+ " is not defined with a primary key, and hence we can not do a read operation on this.");
		}
		int nbrRows = inSheet.length();
		if (nbrRows == 0) {
			return 0;
		}

		boolean singleRow = nbrRows == 1;
		if (singleRow) {
			Value[] values = this.getPrimaryValues(inSheet, 0);
			if (values == null) {
				Tracer.trace("Primary key value not available and hence no read operation.");
				return 0;
			}
			return driver.extractFromSql(this.readSql, values, outSheet, singleRow);
		}

		Value[][] values = new Value[nbrRows][];
		for (int i = 0; i < nbrRows; i++) {
			Value[] vals = this.getPrimaryValues(inSheet, i);
			if (vals == null) {
				Tracer.trace("Primary key value not available and hence no read operation.");
				return 0;
			}
			values[i] = vals;
		}
		int nbr = driver.extractFromSql(this.readSql, values, outSheet);
		if (this.encryptedFields != null) {
			this.crypt(outSheet, true);
		}
		return nbr;
	}

	private Value[] getPrimaryValues(DataSheet inSheet, int idx) {
		Value[] values = new Value[this.allPrimaryKeys.length];
		for (int i = 0; i < this.allPrimaryKeys.length; i++) {
			Value value = inSheet.getColumnValue(this.allPrimaryKeys[i].name, idx);
			if (Value.isNull(value)) {
				return null;
			}
			values[i] = value;
		}
		return values;
	}

	private Value[] getPrimaryValues(FieldsInterface inFields, boolean includeTimeStamp) {
		Value[] values;
		int nbr = this.allPrimaryKeys.length;
		if (includeTimeStamp && this.useTimestampForConcurrency) {
			values = new Value[nbr + 1];
			values[nbr] = inFields.getValue(this.modifiedStampField.name);
		} else {
			values = new Value[nbr];
		}
		for (int i = 0; i < this.allPrimaryKeys.length; i++) {
			Value value = inFields.getValue(this.allPrimaryKeys[i].name);
			if (Value.isNull(value)) {
				return null;
			}
			values[i] = value;
		}
		return values;
	}

	/**
	 * read, in our vocabulary, is ALWAYS primary key based read. Hence we
	 * expect (at most) one row of output per read. If values has more than one
	 * rows, we read for primary key in each row.
	 *
	 * @param inData
	 *            one or more rows that has value for the primary key.
	 * @param outData
	 *            sheet to which data is to be extracted into
	 * @param driver
	 * @param userId
	 *            we may have a common security strategy to restrict output rows
	 *            based on user. Not used as of now
	 * @return number of rows extracted
	 */
	public int readOne(FieldsInterface inData, DataSheet outData, DbDriver driver, Value userId) {
		if (this.allPrimaryKeys == null) {
			Tracer.trace("Record " + this.name
					+ " is not defined with a primary key, and hence we can not do a read operation on this.");
			return 0;
		}
		Value[] values = this.getPrimaryValues(inData, false);
		if (values == null) {
			Tracer.trace("Value for primary key not present, and hence no read operation.");
			return 0;
		}
		int nbr = driver.extractFromSql(this.readSql, values, outData, true);
		if (this.encryptedFields != null && nbr > 0) {
			this.crypt(outData, true);
		}
		return nbr;
	}

	/**
	 * checks if there is a row for this key. Row is not read.
	 *
	 * @param inData
	 * @param keyFieldName
	 * @param driver
	 * @param userId
	 * @return true if there is a row for this key, false otherwise. Row is not
	 *         read.
	 */
	public boolean rowExistsForKey(FieldsInterface inData, String keyFieldName, DbDriver driver, Value userId) {
		if (this.allPrimaryKeys == null) {
			Tracer.trace("Record " + this.name
					+ " is not defined with a primary key, and hence we can not do a read operation on this.");
			this.noPrimaryKey();
			return false;
		}
		Value[] values;
		if (keyFieldName != null) {
			if (this.allPrimaryKeys.length > 1) {
				Tracer.trace("There are more than one primary keys, and hence supplied name keyFieldName of "
						+ keyFieldName + " is ognored");
				values = this.getPrimaryValues(inData, false);
			} else {
				Value value = inData.getValue(keyFieldName);
				if (Value.isNull(value)) {
					Tracer.trace("Primary key field " + keyFieldName + " has no value, and hence no read operation.");
					return false;
				}
				values = new Value[1];
				values[0] = value;
			}
		} else {
			values = this.getPrimaryValues(inData, false);
		}
		return driver.hasResult(this.readSql, values);
	}

	/**
	 * filter rows from underlying view/table as per filtering criterion
	 *
	 * @param inputRecord
	 *            record that has fields for filter criterion
	 * @param userId
	 *            we may have a common security strategy to restrict output rows
	 *            based on user. Not used as of now
	 *
	 * @param inData
	 *            as per filtering conventions
	 * @param driver
	 * @return data sheet, possible with retrieved rows
	 */
	public DataSheet filter(Record inputRecord, FieldsInterface inData, DbDriver driver, Value userId) {
		/*
		 * we have to create where clause with ? and corresponding values[]
		 */
		StringBuilder sql = new StringBuilder(this.filterSql);
		List<Value> filterValues = new ArrayList<Value>();
		boolean firstTime = true;
		for (Field field : inputRecord.fields) {
			String fieldName = field.name;
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
			Value otherValue = inData.getValue(fieldName + ServiceProtocol.COMPARATOR_SUFFIX);
			if (otherValue != null && otherValue.isUnknown() == false) {
				condition = FilterCondition.parse(otherValue.toText());
			}

			/**
			 * handle the special case of in-list
			 */
			if (condition == FilterCondition.In) {
				Value[] values = Value.parse(value.toString().split(","), field.getValueType());
				/*
				 * we are supposed to have validated this at the input gate...
				 * but playing it safe
				 */
				if (values == null) {
					throw new ApplicationError(value + " is not a valid comma separated list for field " + field.name);
				}
				sql.append(field.columnName).append(" in (?");
				filterValues.add(values[0]);
				for (int i = 1; i < values.length; i++) {
					sql.append(",?");
					filterValues.add(values[i]);
				}
				sql.append(") ");
				continue;
			}

			if (condition == FilterCondition.Like) {
				value = Value.newTextValue(Record.PERCENT + DbDriver.escapeForLike(value.toString()) + Record.PERCENT);
			} else if (condition == FilterCondition.StartsWith) {
				value = Value.newTextValue(DbDriver.escapeForLike(value.toString()) + Record.PERCENT);
			}

			sql.append(field.columnName).append(condition.getSql()).append("?");
			filterValues.add(value);

			if (condition == FilterCondition.Between) {
				otherValue = inData.getValue(fieldName + ServiceProtocol.TO_FIELD_SUFFIX);
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
		Value sorts = inData.getValue(ServiceProtocol.SORT_COLUMN_NAME);
		if (sorts != null) {
			sql.append(" ORDER BY ").append(sorts.toString());
		}

		DataSheet result = this.createSheet(false, false);
		driver.extractFromSql(sql.toString(), values, result, false);
		if (this.encryptedFields != null) {
			this.crypt(result, true);
		}
		return result;
	}

	/**
	 * add, modify and delete are the three operations we can do for a record.
	 * "save" is a special convenient command. If key is specified, it is
	 * assumed to be modify, else add. Save
	 *
	 * @param row
	 *            data to be saved.
	 * @param driver
	 * @param userId
	 * @param treatSqlErrorAsNoResult
	 *            if true, we assume that some constraints are set at db level,
	 *            and sql error is treated as if affected rows is zero
	 * @return number of rows saved. -1 in case of batch, and the driver is
	 *         unable to count the saved rows
	 */
	public SaveActionType saveOne(FieldsInterface row, DbDriver driver, Value userId, boolean treatSqlErrorAsNoResult) {
		if (this.readOnly) {
			this.notWritable();
		}

		if (this.allPrimaryKeys == null) {
			this.noPrimaryKey();
		}
		Field pkey = this.allPrimaryKeys[0];
		Value[] values = new Value[this.fields.length];
		/*
		 * modified user field, even if sent by client, must be over-ridden
		 */
		if (this.modifiedUserField != null) {
			row.setValue(this.modifiedUserField.name, userId);
		}
		/*
		 * is the action explicitly specified
		 */
		SaveActionType saveAction = SaveActionType.SAVE;
		Value action = row.getValue(ServiceProtocol.TABLE_ACTION_FIELD_NAME);
		if (action != null) {
			/*
			 * since this field is extracted by us earlier, we DO KNOW that it
			 * is valid
			 */
			saveAction = SaveActionType.parse(action.toString());
		}
		if (saveAction == SaveActionType.SAVE) {
			/*
			 * is the key supplied?
			 */
			Value keyValue = row.getValue(pkey.name);
			if (this.keyToBeGenerated) {
				if (Value.isNull(keyValue)) {
					saveAction = SaveActionType.ADD;
				} else {
					saveAction = SaveActionType.MODIFY;
				}
			} else {
				if (this.rowExistsForKey(row, null, driver, userId)) {
					saveAction = SaveActionType.MODIFY;
				} else {
					saveAction = SaveActionType.ADD;
				}
			}
		}
		if (saveAction == SaveActionType.ADD) {
			if (this.createdUserField != null) {
				row.setValue(this.createdUserField.name, userId);
			}
			values = this.getInsertValues(row, userId);
			if (this.keyToBeGenerated) {
				long[] generatedKeys = new long[1];
				String[] generatedColumns = { pkey.columnName };
				driver.insertAndGetKeys(this.insertSql, values, generatedKeys, generatedColumns,
						treatSqlErrorAsNoResult);
				row.setValue(pkey.name, Value.newIntegerValue(generatedKeys[0]));
			} else {
				driver.executeSql(this.insertSql, values, treatSqlErrorAsNoResult);
			}
		} else if (saveAction == SaveActionType.DELETE) {
			values = this.getPrimaryValues(row, true);
			driver.executeSql(this.deleteSql, values, treatSqlErrorAsNoResult);
		} else {
			values = this.getUpdateValues(row, userId);
			if (driver.executeSql(this.updateSql, values, treatSqlErrorAsNoResult) == 0) {
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
	 * @param driver
	 * @param userId
	 * @param treatSqlErrorAsNoResult
	 * @return number of rows saved. -1 in case of batch, and the driver is
	 *         unable to count the saved rows
	 */
	public SaveActionType[] saveMany(DataSheet inSheet, DbDriver driver, Value userId,
			boolean treatSqlErrorAsNoResult) {
		if (this.readOnly) {
			this.notWritable();
		}
		SaveActionType[] result = new SaveActionType[inSheet.length()];
		int rowIdx = 0;
		for (FieldsInterface row : inSheet) {
			result[rowIdx] = this.saveOne(row, driver, userId, treatSqlErrorAsNoResult);
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
	 * @param driver
	 * @param userId
	 * @return number of rows affected
	 */
	public int saveWithParent(DataSheet inSheet, FieldsInterface parentRow, SaveActionType[] actions, DbDriver driver,
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
		for (FieldsInterface row : inSheet) {
			this.saveOne(row, driver, userId, false);
		}
		return inSheet.length();
	}

	/**
	 * @param inSheet
	 * @param userId
	 * @param rowIdx
	 * @return
	 */
	private Value[] getInsertValues(FieldsInterface row, Value userId) {
		/*
		 * we may not fill all fields, but let us handle that exception later
		 */
		Value[] values = new Value[this.nbrInsertFields];
		int valueIdx = 0;
		for (Field field : this.fields) {
			if (field.canInsert() == false) {
				continue;
			}
			if (field.fieldType == FieldType.CREATED_BY_USER || field.fieldType == FieldType.MODIFIED_BY_USER) {
				values[valueIdx] = userId;
			} else {
				Value value = field.getValue(row);
				if (Value.isNull(value)) {
					if (field.isNullable) {
						value = Value.newUnknownValue(field.getValueType());
					} else {
						throw new ApplicationError("Column " + field.columnName + " in table " + this.tableName
								+ " is designed to be non-null, but a row is being inserted with a null value in it.");
					}
				}
				if (field.isEncrypted) {
					value = this.crypt(value, false);
				}
				values[valueIdx] = value;
			}
			valueIdx++;
		}
		return values;
	}

	/**
	 * @param row
	 * @param userId
	 * @param rowIdx
	 * @return
	 */
	private Value[] getUpdateValues(FieldsInterface row, Value userId) {
		int nbrValues = this.nbrUpdateFields + this.allPrimaryKeys.length;
		/*
		 * we need an extra field for concurrency check
		 */
		if (this.useTimestampForConcurrency) {
			nbrValues++;
		}

		Value[] values = new Value[nbrValues];
		int valueIdx = 0;
		for (Field field : this.fields) {
			if (field.canUpdate() == false) {
				continue;
			}
			if (field.fieldType == FieldType.MODIFIED_BY_USER) {
				values[valueIdx] = userId;
			} else {
				Value value = field.getValue(row);
				if (Value.isNull(value)) {
					if (field.isNullable) {
						value = Value.newUnknownValue(field.getValueType());
					} else {
						throw new ApplicationError("Column " + field.columnName + " in table " + this.tableName
								+ " is designed to be non-null, but a row is being updated with a null value in it.");
					}
				}
				if (field.isEncrypted) {
					value = this.crypt(value, false);
				}
				values[valueIdx] = value;
			}
			valueIdx++;
		}
		for (Value value : this.getPrimaryValues(row, true)) {
			values[valueIdx++] = value;
		}
		return values;
	}

	/**
	 * insert row/s
	 *
	 * @param inSheet
	 *
	 * @param driver
	 * @param userId
	 * @param treatSqlErrorAsNoResult
	 * @return number of rows saved. -1 in case of batch, and the driver is
	 *         unable to count the saved rows
	 */
	public int insert(DataSheet inSheet, DbDriver driver, Value userId, boolean treatSqlErrorAsNoResult) {
		if (this.readOnly) {
			this.notWritable();
		}
		int nbrRows = inSheet.length();

		/*
		 * simple case first...
		 */
		if (nbrRows == 1) {
			return this.insert((FieldsInterface) inSheet, driver, userId, treatSqlErrorAsNoResult);
		}
		Value[][] allValues = new Value[nbrRows][];
		/*
		 * we mostly expect one row, but we do not want to write separate
		 * code...
		 */
		int rowIdx = 0;
		for (FieldsInterface row : inSheet) {
			allValues[rowIdx] = this.getInsertValues(row, userId);
			rowIdx++;
		}
		if (this.keyToBeGenerated == false) {
			return this.executeWorker(driver, this.insertSql, allValues, treatSqlErrorAsNoResult);
		}
		long[] generatedKeys = new long[nbrRows];
		int result = this.insertWorker(driver, this.insertSql, allValues, generatedKeys, treatSqlErrorAsNoResult);
		if (result > 0 && generatedKeys[0] != 0) {
			this.addKeyColumn(inSheet, generatedKeys);
		}

		return result;
	}

	/**
	 * insert row/s
	 *
	 * @param inData
	 *
	 * @param driver
	 * @param userId
	 * @param treatSqlErrorAsNoResult
	 * @return number of rows saved. -1 in case of batch, and the driver is
	 *         unable to count the saved rows
	 */
	public int insert(FieldsInterface inData, DbDriver driver, Value userId, boolean treatSqlErrorAsNoResult) {
		if (this.readOnly) {
			this.notWritable();
		}
		Value[][] allValues = new Value[1][];
		allValues[0] = this.getInsertValues(inData, userId);

		if (this.keyToBeGenerated == false) {
			return this.executeWorker(driver, this.insertSql, allValues, treatSqlErrorAsNoResult);
		}
		/*
		 * try to get generated keys and set it/them
		 */
		long[] generatedKeys = new long[1];
		int result = this.insertWorker(driver, this.insertSql, allValues, generatedKeys, treatSqlErrorAsNoResult);
		if (result > 0) {
			/*
			 * generated key feature may not be available with some rdb
			 * vendor
			 */
			long key = generatedKeys[0];
			if (key > 0) {
				inData.setValue(this.allPrimaryKeys[0].name, Value.newIntegerValue(key));
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
	 *
	 * @param driver
	 * @param userId
	 * @return number of rows saved. -1 in case of batch, and the driver is
	 *         unable to count the saved rows
	 */
	public int insertWithParent(DataSheet inSheet, FieldsInterface parentRow, DbDriver driver, Value userId) {
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

		for (FieldsInterface row : inSheet) {
			allValues[rowIdx] = this.getInsertValues(row, userId);
			rowIdx++;
		}
		if (this.keyToBeGenerated == false) {
			return this.executeWorker(driver, this.insertSql, allValues, false);
		}
		/*
		 * generated key is t be retrieved
		 */
		long[] keys = new long[nbrRows];
		int result = this.insertWorker(driver, this.insertSql, allValues, keys, false);
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
	private void addKeyColumn(DataSheet inSheet, long[] keys) {
		int nbrKeys = keys.length;
		Value[] values = new Value[nbrKeys];
		int i = 0;
		for (long key : keys) {
			values[i++] = Value.newIntegerValue(key);
		}
		inSheet.addColumn(this.allPrimaryKeys[0].name, ValueType.INTEGER, values);
	}

	/**
	 * copy parent key to child sheet
	 *
	 * @param inData
	 *            row/fields that has the parent key
	 * @param sheet
	 *            to which we have to copy the key values
	 */
	private void copyParentKeys(FieldsInterface inData, DataSheet sheet) {
		for (Field field : this.allParentKeys) {
			String fieldName = field.name;
			String parentKeyName = field.referredField;
			Value parentKey = inData.getValue(parentKeyName);
			if (Value.isNull(parentKey)) {
				Tracer.trace("No value found for parent key field " + parentKeyName
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
	private Value[] getParentValues(FieldsInterface inData) {
		Value[] values = new Value[this.allParentKeys.length];
		for (int i = 0; i < this.allParentKeys.length; i++) {
			Field field = this.allParentKeys[i];
			Value value = inData.getValue(field.referredField);
			if (Value.isNull(value)) {
				Tracer.trace("No value found for parent key field " + field.referredField
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
	 * @param driver
	 * @param userId
	 * @param treatSqlErrorAsNoResult
	 * @return number of rows saved. -1 in case of batch, and the driver is
	 *         unable to count the saved rows
	 */
	public int update(DataSheet inSheet, DbDriver driver, Value userId, boolean treatSqlErrorAsNoResult) {
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
			for (FieldsInterface row : inSheet) {
				allValues[i++] = this.getUpdateValues(row, userId);
			}
		}
		int result = this.executeWorker(driver, this.updateSql, allValues, treatSqlErrorAsNoResult);
		return result;
	}

	/**
	 * update all fields, except of course the ones that are not to be.
	 *
	 * @param inputData
	 *            data to be saved.
	 * @param driver
	 * @param userId
	 * @param treatSqlErrorAsNoResult
	 * @return number of rows saved. -1 in case of batch, and the driver is
	 *         unable to count the saved rows
	 */
	public int update(FieldsInterface inputData, DbDriver driver, Value userId, boolean treatSqlErrorAsNoResult) {
		if (this.readOnly) {
			this.notWritable();
		}
		if (this.allPrimaryKeys == null) {
			this.noPrimaryKey();
		}
		Value[][] allValues = new Value[1][];

		allValues[0] = this.getUpdateValues(inputData, userId);
		int result = this.executeWorker(driver, this.updateSql, allValues, treatSqlErrorAsNoResult);
		return result;
	}

	/**
	 * update all fields, except of course the ones that are not to be.
	 *
	 * @param inSheet
	 *            data to be saved.
	 * @param driver
	 * @param treatSqlErrorAsNoResult
	 * @return number of rows saved. -1 in case of batch, and the driver is
	 *         unable to count the saved rows
	 */
	public int delete(DataSheet inSheet, DbDriver driver, boolean treatSqlErrorAsNoResult) {
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
		for (FieldsInterface row : inSheet) {
			allValues[nbrRows++] = this.getPrimaryValues(row, true);
		}
		return this.executeWorker(driver, this.deleteSql, allValues, treatSqlErrorAsNoResult);
	}

	/**
	 * update all fields, except of course the ones that are not to be.
	 *
	 * @param inData
	 *            data to be saved.
	 * @param driver
	 * @param treatSqlErrorAsNoResult
	 * @return number of rows saved. -1 in case of batch, and the driver is
	 *         unable to count the saved rows
	 */
	public int delete(FieldsInterface inData, DbDriver driver, boolean treatSqlErrorAsNoResult) {
		if (this.readOnly) {
			this.notWritable();
		}
		if (this.allPrimaryKeys == null) {
			this.noPrimaryKey();
		}
		Value[][] allValues = new Value[1][];
		allValues[0] = this.getPrimaryValues(inData, true);
		return this.executeWorker(driver, this.deleteSql, allValues, treatSqlErrorAsNoResult);
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
	 * @param driver
	 * @param userId
	 * @return number of rows saved. -1 in case of batch, and the driver is
	 *         unable to count the saved rows
	 */
	public int deleteWithParent(FieldsInterface parentRow, DbDriver driver, Value userId) {
		if (this.readOnly) {
			this.notWritable();
		}
		if (this.allParentKeys == null) {
			this.noParent();
		}
		Value[] values = this.getParentValues(parentRow);
		if (values == null) {
			Tracer.trace("Delete with parent has nothing to delete as parent key is null");
			return 0;
		}
		StringBuilder sql = new StringBuilder("DELETE FROM ");
		sql.append(this.tableName).append(" WHERE ").append(this.getParentWhereClause());
		if (this.allParentKeys.length > 1) {
			for (int i = 1; i < this.allParentKeys.length; i++) {
				sql.append(" AND " + this.allParentKeys[i].columnName + EQUAL_PARAM);
			}
		}
		return driver.executeSql(sql.toString(), values, false);
	}

	private int executeWorker(DbDriver driver, String sql, Value[][] values, boolean treatSqlErrorAsNoResult) {
		if (values.length == 1) {
			return driver.executeSql(sql, values[0], treatSqlErrorAsNoResult);
		}
		int[] counts = driver.executeBatch(sql, values, treatSqlErrorAsNoResult);
		int result = 0;
		for (int n : counts) {
			if (n < 0) {
				return -1;
			}
			result += n;
		}
		return result;
	}

	private int insertWorker(DbDriver driver, String sql, Value[][] values, long[] generatedKeys,
			boolean treatSqlErrorAsNoResult) {
		String[] keyNames = { this.allPrimaryKeys[0].columnName };
		if (values.length == 1) {
			return driver.insertAndGetKeys(sql, values[0], generatedKeys, keyNames, treatSqlErrorAsNoResult);
		}
		if (generatedKeys != null) {
			Tracer.trace(
					"Generated key retrieval is NOT supported for batch. Keys for child table are to be retrieved automatically");
		}
		int[] counts = driver.executeBatch(sql, values, treatSqlErrorAsNoResult);
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
	 * update subset of fields.
	 *
	 * @param inData
	 *            data to be saved.
	 * @param driver
	 * @param userId
	 * @param treatSqlErrorAsNoResult
	 * @return number of rows saved. -1 in case of batch, and the driver is
	 *         unable to count the saved rows
	 */
	public int selectiveUpdate(FieldsInterface inData, DbDriver driver, Value userId, boolean treatSqlErrorAsNoResult) {
		if (this.readOnly) {
			this.notWritable();
		}
		if (this.allPrimaryKeys == null) {
			this.noPrimaryKey();
		}
		/*
		 * we have primary key in where clause.
		 */
		int nbrFields = this.fields.length;
		Value[] values = new Value[nbrFields];
		int valueIdx = 0;
		StringBuilder update = new StringBuilder("UPDATE ");
		update.append(this.tableName).append(" SET ");
		for (Field field : this.fields) {
			Value value = null;
			if (field.canUpdate() == false) {
				continue;
			}
			if (field.fieldType == FieldType.MODIFIED_BY_USER) {
				value = userId;
			} else {
				value = inData.getValue(field.name);
			}
			if (value != null) {
				if (field.isEncrypted) {
					value = this.crypt(value, false);
				}
				if (valueIdx != 0) {
					update.append(Record.COMMA);
				}
				update.append(field.columnName).append(Record.EQUAL_PARAM);
				values[valueIdx++] = value;
			}
		}
		update.append(this.primaryWhereClause);
		if (this.modifiedStampField != null) {
			update.append(" AND ").append(this.modifiedStampField.columnName).append(Record.EQUAL_PARAM);
		}
		/*
		 * append values for primary keys and timestamp
		 */
		Value[] whereValues = this.getPrimaryValues(inData, true);
		if (whereValues == null) {
			Tracer.trace("Primary keys not available and hence update operaiton aborted.");
			return 0;
		}
		for (Value value : whereValues) {
			values[valueIdx++] = value;
		}

		/*
		 * did we skip some values?
		 */
		if (nbrFields != valueIdx) {
			values = this.chopValues(values, valueIdx);
		}
		return driver.executeSql(this.updateSql, values, treatSqlErrorAsNoResult);
	}

	private Value[] chopValues(Value[] values, int nbrsToRetain) {
		Value[] newValues = new Value[nbrsToRetain];
		for (int i = 0; i < nbrsToRetain; i++) {
			newValues[i] = values[i];
		}
		return newValues;
	}

	/**
	 * update subset of fields.
	 *
	 * @param inSheet
	 *            data to be saved.
	 * @param driver
	 * @param userId
	 * @param treatSqlErrorAsNoResult
	 * @return number of rows saved. -1 in case of batch, and the driver is
	 *         unable to count the saved rows
	 */
	public int selectiveUpdate(DataSheet inSheet, DbDriver driver, Value userId, boolean treatSqlErrorAsNoResult) {
		if (this.readOnly) {
			this.notWritable();
		}
		int nbrRows = inSheet.length();
		if (nbrRows == 1) {
			return this.selectiveUpdate(inSheet, driver, userId, treatSqlErrorAsNoResult);
		}
		nbrRows = 0;
		for (FieldsInterface row : inSheet) {
			nbrRows += this.selectiveUpdate(row, driver, userId, treatSqlErrorAsNoResult);
		}
		return nbrRows;
	}

	/**
	 * read rows from this record for a given parent record
	 *
	 * @param parentData
	 *            rows for parent
	 * @param driver
	 * @param sheetName
	 * @param cascadeFilter
	 * @param ctx
	 */
	public void filterForParents(DataSheet parentData, DbDriver driver, String sheetName, boolean cascadeFilter,
			ServiceContext ctx) {
		DataSheet result = this.createSheet(false, false);
		int n = parentData.length();
		if (n == 0) {
			return;
		}
		if (this.allParentKeys.length > 1) {
			this.filterForMultiParentKeys(parentData, driver, result);
		} else {
			this.filterForSingleParentKey(parentData, driver, result);
		}
		String sn = sheetName;
		if (sn == null) {
			sn = this.getDefaultSheetName();
		}
		ctx.putDataSheet(sn, result);

		if (result.length() > 0 && cascadeFilter) {
			this.filterChildRecords(result, driver, ctx);
		}
	}

	/**
	 * @param parentData
	 * @param driver
	 * @param result
	 */
	private void filterForSingleParentKey(DataSheet parentData, DbDriver driver, DataSheet result) {
		String keyName = this.allParentKeys[0].referredField;
		int n = parentData.length();
		Value[] values = parentData.getColumnValues(keyName);
		StringBuilder sbf = new StringBuilder(this.filterSql);
		sbf.append(this.allParentKeys[0].columnName);
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
		driver.extractFromSql(sbf.toString(), values, result, false);
		if (this.encryptedFields != null) {
			this.crypt(result, true);
		}
	}

	/**
	 * read rows from this record for a given parent record
	 *
	 * @param parentData
	 *            rows for parent
	 * @param driver
	 * @param sheetName
	 * @param cascadeFilter
	 * @param ctx
	 */
	private void filterForMultiParentKeys(DataSheet parentData, DbDriver driver, DataSheet outSheet) {
		String sql = this.filterSql + this.getParentWhereClause();
		int nbrRows = parentData.length();
		Value[][] allValues = new Value[nbrRows][];
		int idx = 0;
		for (FieldsInterface prentRow : parentData) {
			allValues[idx++] = this.getParentValues(prentRow);
		}
		driver.extractFromSql(sql, allValues, outSheet);
		if (this.encryptedFields != null) {
			this.crypt(outSheet, true);
		}
	}

	/**
	 * if this record has child records, filter them based on this parent sheet
	 *
	 * @param parentSheet
	 *            sheet that has rows for this record
	 * @param driver
	 * @param ctx
	 */
	public void filterChildRecords(DataSheet parentSheet, DbDriver driver, ServiceContext ctx) {
		if (this.childrenToBeRead == null) {
			return;
		}
		for (String childName : this.childrenToBeRead) {
			Record cr = ComponentManager.getRecord(childName);
			cr.filterForParents(parentSheet, driver, cr.getDefaultSheetName(), true, ctx);
		}
	}

	/**
	 * read rows from this record for a given parent record
	 *
	 * @param parentData
	 *
	 * @param driver
	 * @return sheet that contains rows from this record for the parent rows
	 */
	public DataSheet filterForAParent(FieldsInterface parentData, DbDriver driver) {
		DataSheet result = this.createSheet(false, false);
		Value[] values = this.getParentValues(parentData);
		String sql = this.filterSql + this.getParentWhereClause();
		driver.extractFromSql(sql, values, result, false);
		if (this.encryptedFields != null) {
			this.crypt(result, true);
		}
		return result;
	}

	private String getParentWhereClause() {
		StringBuilder sbf = new StringBuilder();
		sbf.append(this.allParentKeys[0].columnName).append(EQUAL_PARAM);
		for (int i = 1; i < this.allParentKeys.length; i++) {
			sbf.append(" AND ").append(this.allParentKeys[i].columnName).append(EQUAL_PARAM);
		}
		return sbf.toString();
	}

	/**
	 * en/de crypt a value
	 *
	 * @param value
	 * @param toDecrypt
	 *            true means decrypt, else encrypt
	 * @return
	 */
	private Value crypt(Value value, boolean toDecrypt) {
		if (Value.isNull(value)) {
			return value;
		}
		String txt = value.toString();
		if (toDecrypt) {
			txt = TextUtil.decrypt(txt);
		} else {
			txt = TextUtil.encrypt(txt);
		}
		return Value.newTextValue(txt);
	}

	/**
	 * encrypt/decrypt columns in this data sheet
	 *
	 * @param sheet
	 * @param toDecrypt
	 *            true means decrypt, else encrypt
	 */
	private void crypt(DataSheet sheet, boolean toDecrypt) {
		int nbrRows = sheet.length();
		if (nbrRows == 0) {
			Tracer.trace("Data sheet has no data and hance no encryption.");
			return;
		}
		for (Field field : this.encryptedFields) {
			int colIdx = sheet.getColIdx(field.name);
			if (colIdx == -1) {
				Tracer.trace("Data sheet has no column named " + field.name + " hance no encryption.");
				continue;
			}
			for (int rowIdx = 0; rowIdx < nbrRows; rowIdx++) {
				Value[] row = sheet.getRow(rowIdx);
				row[colIdx] = this.crypt(row[colIdx], toDecrypt);
			}
		}
		Tracer.trace(nbrRows + " rows and " + this.encryptedFields.length + " columns " + (toDecrypt ? "un-" : "")
				+ "obsfuscated");
	}

	/*
	 * we have a possible issue with referred records. If A refers to B and B
	 * refers to A, we have an error on hand. How do we track this? as of now,
	 * we will track this during getReady() invocation. getReady() will ask for
	 * a referred record. That record will execute getReady() before returning.
	 * It may ask for another record, so an and so forth.
	 *
	 * There are two ways to solve this problem.
	 *
	 * One way is to differentiate between normal-request and reference-request
	 * for a record. Pass history during reference request so that we can detect
	 * circular reference. Issue with this is that getRequest() is a generic
	 * method and hence we can not customize it.
	 *
	 * Other approach is to use thread-local that is initiated by getReady().
	 *
	 * our algorithm is :
	 *
	 * 1. we initiate refHistory before getReady() and current record to
	 * pendingOnes.
	 *
	 * 2. A referred field may invoke parent.getRefrecord() Referred record is
	 * requested from ComponentManager.getRecord();
	 *
	 * 3. that call will trigger getReady() on the referred record. This chain
	 * will go-on..
	 *
	 * 4. before adding to pending list we check if it already exists. That
	 * would be a circular reference.
	 *
	 * 5. Once we complete getReady(), we remove this record from pendingOnes.
	 * And if there no more pending ones, we remove it. and that completes the
	 * check.
	 */
	/**
	 * tracks recursive reference calls between records and referred records for
	 * referred fields
	 */
	static ThreadLocal<RefHistory> referenceHistory = new ThreadLocal<Record.RefHistory>();

	class RefHistory {
		/**
		 * recursive reference history of record names
		 */
		List<String> pendingOnes = new ArrayList<String>();
		/**
		 * records that have completed loading as part of this process
		 */
		Map<String, Record> finishedOnes = new HashMap<String, Record>();
	}

	/**
	 * called from field when it refers to a field in another record
	 *
	 * @param recordName
	 * @return
	 */
	Record getRefRecord(String recordName) {
		RefHistory history = referenceHistory.get();
		if (history == null) {
			throw new ApplicationError("Record.java has an issue with getReady() logic. history is null");
		}
		/*
		 * do we have it in our cache?
		 */
		Record record = history.finishedOnes.get(recordName);
		if (record != null) {
			return record;
		}
		/*
		 * is this record already in the pending list?
		 */
		if (history.pendingOnes.contains(recordName)) {
			/*
			 * we have a circular reference issue.
			 */
			StringBuilder sbf = new StringBuilder();
			if (recordName.equals(this.getQualifiedName())) {
				sbf.append("Record ").append(recordName).append(
						" has at least one field that refers to another field in this record itself. Sorry, you can't do that.");
			} else {
				sbf.append(
						"There is a circular reference of records amongst the following records. Please review and fix.\n{\n");
				int nbr = history.pendingOnes.size();
				for (int i = 0; i < nbr; i++) {

					sbf.append(i).append(": ").append(history.pendingOnes.get(i)).append('\n');
				}
				sbf.append(nbr).append(": ").append(recordName).append('\n');
				sbf.append('}');
			}
			throw new ApplicationError(sbf.toString());
		}
		return ComponentManager.getRecord(recordName);
	}

	/**
	 * called before starting getReady()
	 *
	 * @return true if we initiated the trail..
	 */
	private boolean recordGettingReady() {
		String recName = this.getQualifiedName();
		RefHistory history = referenceHistory.get();
		if (history == null) {
			history = new RefHistory();
			history.pendingOnes.add(recName);
			referenceHistory.set(history);
			return true;
		}
		if (history.pendingOnes.contains(recName) == false) {
			history.pendingOnes.add(recName);
			return false;
		}
		/*
		 * we have a circular reference issue.
		 */

		StringBuilder sbf = new StringBuilder();
		if (history.pendingOnes.size() == 1) {
			sbf.append("Record ").append(recName).append(
					" has at least one field that refers to another field in this record itself. Sorry, you can't do that.");
		} else {
			sbf.append(
					"There is a circular reference of records amongst the following records. Please review and fix.\n{\n");
			int nbr = history.pendingOnes.size();
			for (int i = 0; i < nbr; i++) {

				sbf.append(i).append(". ").append(history.pendingOnes.get(i)).append('\n');
			}
			sbf.append(nbr).append(". ").append(recName).append('\n');
			sbf.append('}');
		}
		Tracer.trace(sbf.toString());
		return false;
		// throw new ApplicationError(sbf.toString());
	}

	/**
	 * called at the end of getReady();
	 */
	private void recordGotReady(boolean originator) {
		String recName = this.getQualifiedName();
		RefHistory history = referenceHistory.get();
		if (history == null) {
			Tracer.trace("There is an issue with the way Record " + recName
					+ "  is trying to detect circular reference. History has disappeared.");
			return;
		}
		if (originator == false) {
			history.pendingOnes.remove(recName);
			history.finishedOnes.put(recName, this);
			return;
		}
		if (history.pendingOnes.size() > 1) {
			StringBuilder sbf = new StringBuilder();
			for (String s : history.pendingOnes) {
				sbf.append(s).append(' ');
			}
			Tracer.trace("There is an issue with the way Record " + recName
					+ "  is trying to detect circular reference. pending list remained as " + sbf.toString());
		}
		referenceHistory.remove();
	}

	@Override
	public void getReady() {
		if (TABLE_ACTION_FIELD == null) {
			TABLE_ACTION_FIELD = Field.getDefaultField(TABLE_ACTION_FIELD_NAME, ValueType.TEXT);
		}
		if (this.fields == null) {
			throw new ApplicationError("Record " + this.getQualifiedName() + " has no fields.");
		}
		if (this.tableName == null) {
			this.tableName = this.name;
		}
		if (this.defaultSheetName == null) {
			this.defaultSheetName = this.name;
		}
		if (this.recordType != RecordUsageType.STORAGE) {
			this.readOnly = true;
		}

		if (this.keyToBeGenerated) {
			if (DbDriver.generatorNameRequired()) {
				if (this.sequenceName == null) {
					this.sequence = this.tableName + DEFAULT_SEQ_SUFFIX;
					Tracer.trace("sequence not specified for table " + this.tableName + ". default sequence name  "
							+ this.sequence + " is assumed.");
				} else {
					this.sequence = this.sequenceName + ".NEXTVAL";
				}
			}
		}
		/*
		 * we track referred records. push to stack
		 */
		boolean originator = this.recordGettingReady();
		Record refRecord = null;
		if (this.defaultRefRecord != null) {
			refRecord = this.getRefRecord(this.defaultRefRecord);
		}

		this.fieldNames = new String[this.fields.length];
		int nbrPrimaries = 0;
		int nbrParents = 0;
		int nbrEncrypted = 0;
		List<Field> pkeys = new ArrayList<Field>();
		for (int i = 0; i < this.fields.length; i++) {
			Field field = this.fields[i];
			if (this.forFixedWidthRow) {
				this.minRecordLength += field.fieldWidth;
			}
			field.getReady(this, refRecord, this.recordType == RecordUsageType.VIEW);
			String fName = field.name;
			this.fieldNames[i] = fName;
			this.indexedFields.put(fName, field);
			if (!this.hasInterFieldValidations && field.hasInterFieldValidations()) {
				this.hasInterFieldValidations = true;
			}
			if (field.isEncrypted) {
				nbrEncrypted++;
			}
			FieldType ft = field.getFieldType();
			if (FieldType.isPrimaryKey(ft)) {
				pkeys.add(field);
				nbrPrimaries++;
			}
			if (FieldType.isParentKey(ft)) {
				nbrParents++;
			}
			if (ft == FieldType.MODIFIED_TIME_STAMP) {
				this.checkDuplicateError(this.modifiedStampField);
				this.modifiedStampField = field;
			} else if (ft == FieldType.CREATED_BY_USER) {
				this.checkDuplicateError(this.createdUserField);
				this.createdUserField = field;
			} else if (ft == FieldType.MODIFIED_BY_USER) {
				this.checkDuplicateError(this.modifiedUserField);
				this.modifiedUserField = field;
			} else if (ft == FieldType.RECORD || ft == FieldType.RECORD_ARRAY || ft == FieldType.VALUE_ARRAY) {
				this.isComplexStruct = true;
			}
		}
		if(pkeys.size()>0)
			this.allPrimaryKeys = pkeys.toArray(new Field[pkeys.size()]);
		/*
		 * last field is allowed to be flexible in a fixed width record
		 */
		if (this.forFixedWidthRow) {
			this.minRecordLength -= this.fields[this.fields.length - 1].fieldWidth;
		}
		/*
		 * because of possible composite keys, we save keys in arrays
		 */
		if (nbrPrimaries > 0 || nbrParents > 0 || nbrEncrypted > 0) {
			this.cacheSpecialFields(nbrPrimaries, nbrParents, nbrEncrypted);
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
				throw new ApplicationError("Record " + this.name + " uses " + this.modifiedStampField.name
						+ " as modiedAt field, but has defined it as a " + this.modifiedStampField.getValueType()
						+ ". It should be defined as a TIMESTAMP for it to be used for concurrency check.");
			}
		}
		if (this.defaultSheetName == null) {
			this.defaultSheetName = this.name;
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
		if (this.readOnly == false) {
			this.createWriteSqls();
		}

		/*
		 * we have successfully loaded. remove this record from stack.
		 */
		this.recordGotReady(originator);
	}

	/**
	 * @param nbrPrimaries
	 */
	private void cacheSpecialFields(int nbrPrimaries, int nbrParents, int nbrEncrypted) {
		if (nbrPrimaries > 0) {
			this.allPrimaryKeys = new Field[nbrPrimaries];
		}
		if (nbrParents > 0) {
			this.allParentKeys = new Field[nbrParents];
		}
		if (nbrEncrypted > 0) {
			this.encryptedFields = new Field[nbrEncrypted];
		}
		int primaryIdx = 0;
		int parentIdx = 0;
		int encrIdx = 0;
		for (Field field : this.fields) {
			if (FieldType.isPrimaryKey(field.fieldType)) {
				this.allPrimaryKeys[primaryIdx] = field;
				primaryIdx++;
			}
			if (FieldType.isParentKey(field.fieldType)) {
				this.allParentKeys[parentIdx] = field;
				parentIdx++;
			}
			if (field.isEncrypted) {
				this.encryptedFields[encrIdx] = field;
				encrIdx++;
			}
		}
	}

	private void checkDuplicateError(Field savedField) {
		if (savedField == null) {
			return;
		}

		throw new ApplicationError("Record " + this.getQualifiedName() + " defines more than one field with field type "
				+ savedField.fieldType.name() + ". This feature is not supported");
	}

	/**
	 * Create read and filter sqls
	 */
	private void createReadSqls() {

		/*
		 * start with read sqls
		 */
		StringBuilder select = new StringBuilder("SELECT ");

		boolean isFirstField = true;
		for (Field field : this.fields) {
			if (isFirstField) {
				isFirstField = false;
			} else {
				select.append(Record.COMMA);
			}
			select.append(field.columnName).append(" \"").append(field.name).append('"');
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

	/**
	 * set sql strings. We are setting four fields at the end. For clarity, you
	 * should trace one string at a time and understand what we are trying to
	 * do. Otherwise it looks confusing
	 */
	private void createWriteSqls() {
		String timeStamp = DbDriver.getTimeStamp();
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
		for (Field field : this.fields) {
			/*
			 * some fields are not updatable
			 */
			if (field.canUpdate() || field.fieldType == FieldType.MODIFIED_TIME_STAMP) {
				if (firstUpdatableField) {
					firstUpdatableField = false;
				} else {
					update.append(COMMA);
				}
				update.append(field.columnName).append(Record.EQUAL);
				if (field.fieldType == FieldType.MODIFIED_TIME_STAMP) {
					update.append(timeStamp);
				} else {
					update.append(Record.PARAM);
					this.nbrUpdateFields++;
				}
			}
			FieldType fieldType = field.fieldType;
			/*
			 * if primary key is managed by rdbms, we do not bother about it?
			 */
			if (FieldType.isPrimaryKey(fieldType) && this.keyToBeGenerated && this.sequence == null) {
				continue;
			}
			if (firstInsertField) {
				firstInsertField = false;
			} else {
				insert.append(Record.COMMA);
				vals.append(Record.COMMA);
			}
			insert.append(field.columnName);
			/*
			 * value is hard coded for time stamps
			 */
			if (field.fieldType == FieldType.MODIFIED_TIME_STAMP || field.fieldType == FieldType.CREATED_TIME_STAMP) {
				vals.append(timeStamp);
			} else if (FieldType.isPrimaryKey(fieldType) && this.keyToBeGenerated) {
				vals.append(this.sequence);
			} else {
				vals.append(Record.PARAM);
				this.nbrInsertFields++;
			}
		}
		/*
		 * set insert sql
		 */
		insert.append(vals.append(')'));
		this.insertSql = insert.toString();

		/*
		 * where clause of delete and update are same, but they are valid only
		 * if we have a primary key
		 */
		if (this.allPrimaryKeys != null) {
			this.updateSql = update.append(this.primaryWhereClause).toString();
			this.deleteSql = "DELETE FROM " + this.tableName + this.primaryWhereClause;
		}
	}

	private void setPrimaryWhere() {
		StringBuilder where = new StringBuilder(" WHERE ");

		boolean firstTime = true;
		for (Field field : this.allPrimaryKeys) {
			if (firstTime) {
				firstTime = false;
			} else {
				where.append(" AND ");
			}
			where.append(field.columnName).append(Record.EQUAL).append(Record.PARAM);
		}

		this.primaryWhereClause = where.toString();
	}

	private void setListSql() {
		Field field = this.getField(this.listFieldName);
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
		if (this.allPrimaryKeys == null || this.listFieldName.equals(this.allPrimaryKeys[0].name)) {
			this.valueListTypes = new ValueType[1];
			this.valueListTypes[0] = field.getValueType();
		} else {
			/*
			 * we have to select the primary key and the list field
			 */
			Field keyField = this.allPrimaryKeys[0];
			sbf.append(keyField.columnName).append(" id,");
			this.valueListTypes = new ValueType[2];
			this.valueListTypes[0] = keyField.getValueType();
			this.valueListTypes[1] = field.getValueType();
		}
		sbf.append(field.columnName).append(" value from ").append(this.tableName);
		if (this.listGroupKeyName != null) {
			field = this.getField(this.listGroupKeyName);
			if (field == null) {
				this.invalidFieldName(this.listGroupKeyName);
				return;
			}
			sbf.append(" WHERE ").append(field.columnName).append(EQUAL_PARAM);
			this.valueListKeyType = field.getValueType();
		}
		this.listSql = sbf.toString();
	}

	private void setSuggestSql() {
		Field field = this.getField(this.suggestionKeyName);
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
			Field f = this.getField(fieldName);
			if (f == null) {
				this.invalidFieldName(this.suggestionKeyName);
				return;
			}
			sbf.append(f.columnName).append(' ').append(f.name).append(COMMA);
		}
		sbf.setLength(sbf.length() - 1);
		sbf.append(" from ").append(this.tableName).append(" WHERE ").append(field.columnName).append(" LIKE ?");
		this.suggestSql = sbf.toString();
	}

	/**
	 * get list of values, typically for drop-down control
	 *
	 * @param keyValue
	 * @param driver
	 * @param userId
	 * @return sheet that has the data
	 */
	public DataSheet list(String keyValue, DbDriver driver, Value userId) {
		Value[] values = null;
		if (this.listGroupKeyName != null) {
			if (keyValue == null || keyValue.length() == 0) {
				return null;
			}
			values = new Value[1];
			values[0] = Value.parseValue(keyValue, this.valueListKeyType);
		}
		DataSheet sheet = null;
		if (this.valueListTypes.length == 1) {
			sheet = new MultiRowsSheet(SINGLE_HEADER, this.valueListTypes);
		} else {
			sheet = new MultiRowsSheet(DOUBLE_HEADER, this.valueListTypes);
		}
		driver.extractFromSql(this.listSql, values, sheet, false);
		return sheet;
	}

	/**
	 * extract rows matching/starting with supplied chars. Typically for a
	 * suggestion list
	 *
	 * @param keyValue
	 * @param matchStarting
	 * @param driver
	 * @param userId
	 * @return sheet that has the data
	 */
	public DataSheet suggest(String keyValue, boolean matchStarting, DbDriver driver, Value userId) {
		String text = keyValue + DbDriver.LIKE_ANY;
		if (!matchStarting) {
			text = DbDriver.LIKE_ANY + text;
		}
		Value[] values = new Value[1];
		values[0] = Value.newTextValue(text);
		DataSheet sheet = this.createSheet(this.suggestionOutputNames, false, false);
		driver.extractFromSql(this.suggestSql, values, sheet, false);
		return sheet;
	}

	/**
	 *
	 * @return all fields of this record.
	 */
	public Field[] getFields() {
		return this.fields;
	}

	/**
	 * @return all fields mapped by their names
	 */
	public Map<String, Field> getFieldsMap() {
		Map<String, Field> map = new HashMap<String, Field>();
		for (Field field : this.fields) {
			map.put(field.name, field);
		}
		return map;
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

	/**
	 * extract and validate a data sheet
	 *
	 * @param inData
	 * @param names
	 *            leave it null if you want all fields
	 * @param errors
	 * @param purpose
	 * @param saveActionExpected
	 * @return data sheet, or null in case of validation errors
	 */
	public DataSheet extractSheet(String[][] inData, String[] names, List<FormattedMessage> errors, DataPurpose purpose,
			boolean saveActionExpected) {
		Field[] fieldsToExtract = this.getFieldsToBeExtracted(names, purpose, saveActionExpected);
		int nbrFields = fieldsToExtract.length;
		/*
		 * array index in the input rows with matching name for this field. -1
		 * if input has no column for this field
		 */
		int[] indexes = new int[nbrFields];
		String[] header = inData[0];
		ValueType[] types = new ValueType[nbrFields];
		String[] allNames = new String[nbrFields];
		/*
		 * set values for inputFields, types and indexes
		 */
		int idx = 0;
		for (Field field : fieldsToExtract) {
			String fn = field.name;
			int map = -1;
			for (int j = 0; j < header.length; j++) {
				if (fn.equals(header[j])) {
					map = j;
					break;
				}
			}
			indexes[idx] = map;
			types[idx] = field.getValueType();
			allNames[idx] = fn;
			idx++;
		}

		if (inData.length == 1) {
			/*
			 * only header, no data. Let us also create a sheet with no data.
			 */
			return new MultiRowsSheet(fieldsToExtract);
		}

		boolean fieldsAreOptional = purpose == DataPurpose.SUBSET;
		/*
		 * we are all set to extract data from each row now
		 */
		List<Value[]> values = new ArrayList<Value[]>();
		for (int i = 1; i < inData.length; i++) {
			String[] inputRow = inData[i];
			Value[] outputRow = new Value[nbrFields];
			for (int j = 0; j < fieldsToExtract.length; j++) {
				Field field = fieldsToExtract[j];
				int map = indexes[j];
				String textValue = map == -1 ? "" : inputRow[map];
				Value value = field.parseField(textValue, errors, fieldsAreOptional, this.name);
				if (value == null) {
					value = Value.newUnknownValue(field.getValueType());
				}
				outputRow[j] = value;
			}
			values.add(outputRow);
		}
		DataSheet ds = new MultiRowsSheet(allNames, values);
		if (this.hasInterFieldValidations) {
			for (FieldsInterface row : ds) {
				for (Field field : fieldsToExtract) {
					field.validateInterfield(row, errors, this.name);
				}
			}
		}
		return ds;

	}

	/**
	 * extract all fields or named fields for the given purpose
	 *
	 * @param inData
	 * @param namesToExtract
	 *            null if all fields are to be extract
	 * @param extractedValues
	 * @param errors
	 * @param purpose
	 *            what fields are extracted depends on the purpose
	 * @param saveActionExpected
	 * @return number of fields extracted
	 */
	public int extractFields(Map<String, String> inData, String[] namesToExtract, FieldsInterface extractedValues,
			List<FormattedMessage> errors, DataPurpose purpose, boolean saveActionExpected) {
		/*
		 * is the caller providing a list of fields? else we use all
		 */
		Field[] fieldsToExtract = this.getFieldsToBeExtracted(namesToExtract, purpose, saveActionExpected);
		if (purpose == DataPurpose.FILTER) {
			Tracer.trace("Extracting filter fields");
			return this.extractFilterFields(inData, extractedValues, fieldsToExtract, errors);
		}
		int result = 0;
		boolean fieldsAreOptional = purpose == DataPurpose.SUBSET;
		for (Field field : fieldsToExtract) {
			String text = inData.get(field.name);
			Value value = field.parseField(text, errors, fieldsAreOptional, this.name);
			if (value != null) {
				extractedValues.setValue(field.name, value);
				result++;
			}
		}
		if (this.hasInterFieldValidations && fieldsAreOptional == false && result > 1) {
			for (Field field : fieldsToExtract) {
				field.validateInterfield(extractedValues, errors, this.name);
			}
		}
		return result;
	}

	/**
	 * get fields based on names, or all fields
	 *
	 * @param names
	 *            null if all fields are to be extracted
	 * @param purpose
	 * @param extractSaveAction
	 * @return fields
	 */
	public Field[] getFieldsToBeExtracted(String[] names, DataPurpose purpose, boolean extractSaveAction) {
		/*
		 * are we being dictated as to what fields to be used?
		 */
		if (names != null) {
			Field[] result = new Field[names.length];
			int i = 0;
			for (String s : names) {
				Field field = this.indexedFields.get(s);
				if (field == null) {
					if (s.equals(TABLE_ACTION_FIELD_NAME)) {
						field = TABLE_ACTION_FIELD;
					} else {
						throw new ApplicationError(
								s + " is not a valid fields in Record " + this.name + ". Field can not be extracted.");
					}
				}
				result[i] = field;
				i++;
			}
			return result;
		}
		/*
		 * we pick fields based on specific purpose
		 */
		if (purpose == DataPurpose.READ) {
			return this.allPrimaryKeys;
		}
		if (extractSaveAction == false) {
			return this.fields;
		}
		Field[] result = new Field[this.fields.length + 1];
		int i = 0;
		for (Field field : this.fields) {
			result[i] = field;
			i++;
		}
		result[i] = TABLE_ACTION_FIELD;
		return result;
	}

	/**
	 * filter fields are special fields that have comparators etc..
	 *
	 * @param inData
	 * @param extractedValues
	 * @param fieldsToExtract
	 * @param errors
	 * @return
	 */
	private int extractFilterFields(Map<String, String> inData, FieldsInterface extractedValues,
			Field[] fieldsToExtract, List<FormattedMessage> errors) {
		int result = 0;
		for (Field field : fieldsToExtract) {
			result += field.parseFilter(inData, extractedValues, errors, this.name);
		}
		/*
		 * some additional fields for filter, like sort
		 */
		/*
		 * what about sort ?
		 */
		String fieldName = ServiceProtocol.SORT_COLUMN_NAME;
		String textValue = inData.get(fieldName);
		if (textValue != null) {
			Value value = ComponentManager.getDataType(DataType.ENTITY_LIST).parseValue(textValue);
			if (value == null) {
				errors.add(
						new FormattedMessage(Messages.INVALID_ENTITY_LIST, this.defaultSheetName, fieldName, null, 0));
			} else {
				extractedValues.setValue(fieldName, value);
			}
		}

		fieldName = ServiceProtocol.SORT_ORDER;
		textValue = inData.get(fieldName);
		if (textValue != null) {
			textValue = textValue.toLowerCase();
			if (textValue.equals(ServiceProtocol.SORT_ORDER_ASC) || textValue.equals(ServiceProtocol.SORT_ORDER_DESC)) {
				extractedValues.setValue(fieldName, Value.newTextValue(textValue));
			} else {
				errors.add(
						new FormattedMessage(Messages.INVALID_SORT_ORDER, this.defaultSheetName, fieldName, null, 0));
			}
		}
		return result;
	}

	/**
	 * @return field names in this record
	 */
	public String[] getFieldNames() {
		return this.fieldNames;
	}

	/**
	 * get a subset of fields.
	 *
	 * @param namesToGet
	 * @return array of fields. ApplicationError is thrown in case any field is
	 *         not found.
	 */
	public Field[] getFields(String[] namesToGet) {
		if (namesToGet == null) {
			return this.fields;
		}
		Field[] result = new Field[namesToGet.length];
		int i = 0;
		for (String s : namesToGet) {
			Field f = this.indexedFields.get(s);
			if (f == null) {
				throw new ApplicationError("Record " + this.getQualifiedName() + " is asked to get a field named " + s
						+ ". Such a field is not defined for this record.");
			}
			result[i] = f;
			i++;
		}
		return result;
	}

	/**
	 * @return are there fields with inter-field validations?
	 */
	public boolean hasInterFieldValidations() {
		return this.hasInterFieldValidations;
	}

	/**
	 *
	 * @param recs
	 *            list to which output records are to be added
	 */
	public void addOutputRecords(List<OutputRecord> recs) {
		/*
		 * of course, this record is to be output.
		 */
		recs.add(new OutputRecord(this));

		if (this.childrenToBeRead == null) {
			return;
		}

		/*
		 * children can be output, but only if we have links...
		 */
		if (this.allPrimaryKeys == null) {
			Tracer.trace("Record " + this.getQualifiedName() + " has defined childrenToBeRead=" + this.childrenToBeRead
					+ " but it has not defined a primary key.");
			return;
		}

		for (String child : this.childrenToBeRead) {
			Record cr = ComponentManager.getRecord(child);
			cr.ChildOutputRecords(recs, this);
		}
	}

	/**
	 *
	 * @param recs
	 *            list to which output records are to be added
	 * @param parentSheetName
	 * @param parentKey
	 */
	private void ChildOutputRecords(List<OutputRecord> recs, Record parentRec) {
		String sheetName = parentRec.getDefaultSheetName();
		Field[] parentKeys = parentRec.allPrimaryKeys;
		Field[] refKeys = this.allParentKeys;
		if (parentKeys == null || refKeys == null || parentKeys.length != refKeys.length) {
			throw new ApplicationError("Parent record " + parentRec.getQualifiedName()
					+ " defines number of children to be read . This would work only if this record defines key field(s) and the child records define corresponding links using parentKeyFields");
		}
		int nbr = parentKeys.length;
		if (nbr == 1) {
			recs.add(new OutputRecord(this.defaultSheetName, sheetName, refKeys[0].name, parentKeys[0].name));

		} else {
			String[] pk = new String[nbr];
			String[] rk = new String[nbr];
			for (int i = 0; i < pk.length; i++) {
				pk[i] = parentKeys[i].name;
				rk[i] = refKeys[i].name;
			}
			recs.add(new OutputRecord(this.defaultSheetName, sheetName, rk, pk));
		}
		if (this.childrenToBeRead == null) {
			return;
		}
		for (String child : this.childrenToBeRead) {
			Record cr = ComponentManager.getRecord(child);
			cr.ChildOutputRecords(recs, this);
		}
	}

	/**
	 * @param parentSheetName
	 *            if this sheet is to be output as a child. null if a normal
	 *            sheet
	 * @return output record that will copy data sheet to output
	 */
	public InputRecord getInputRecord(String parentSheetName) {
		if (parentSheetName == null) {
			return new InputRecord(this.getQualifiedName(), this.defaultSheetName);
		}
		return new InputRecord(this.getQualifiedName(), this.defaultSheetName, parentSheetName,
				this.allParentKeys[0].name, this.allParentKeys[0].referredField);
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

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.kernel.comp.Component#validate(org.simplity.kernel.comp.
	 * ValidationContext)
	 */
	@Override
	public int validate(ValidationContext ctx) {
		/*
		 * this is probably one of our longest methods. Since processing is
		 * sequential, we have not bothered to cut them to pieces :-)
		 */
		ctx.beginValidation(MY_TYPE, this.getQualifiedName());
		try {
			int count = 0;
			/*
			 * of course we have to have fields
			 */
			if (this.fields.length == 0) {
				ctx.addError("Record has no fields");
				count++;
			}
			/*
			 * reference to other records
			 */
			count += ctx.checkRecordExistence(this.defaultRefRecord, "defaultRefRecord", false);
			if (this.childrenToBeRead != null) {
				for (String child : this.childrenToBeRead) {
					count += ctx.checkRecordExistence(child, "childRecord", true);
				}
			}
			if (this.childrenToBeSaved != null) {
				for (String child : this.childrenToBeSaved) {
					count += ctx.checkRecordExistence(child, "childRecord", true);
				}
			}
			/*
			 * validate fields, and accumulate them in a map for other
			 * validations
			 */
			Map<String, Field> fieldMap = new HashMap<String, Field>();
			Map<String, Field> columnMap = new HashMap<String, Field>();
			Set<String> referredFields = new HashSet<String>();
			int nbrKeys = 0;
			Field pkey = null;
			for (Field field : this.fields) {
				/*
				 * validate this field
				 */
				count += field.validate(ctx, this, referredFields);
				if (this.forFixedWidthRow && field.fieldWidth == 0) {
					ctx.addError(field.name
							+ " should specify fieldWidth since the record is designed for a fixed-width row.");
					count++;
				}
				if (FieldType.isPrimaryKey(field.fieldType)) {
					pkey = field;
					nbrKeys++;
				}

				/*
				 * look for duplicate field name
				 */
				if (fieldMap.containsKey(field.name)) {
					ctx.addError(field.name + " is a duplicate field name");
					count++;
				} else {
					fieldMap.put(field.name, field);
				}

				/*
				 * duplicate column name?
				 */
				if (field.fieldType != FieldType.TEMP) {
					String colName = field.getColumnName();
					if (columnMap.containsKey(colName)) {
						ctx.addError(colName + " is a duplicate column name");
						count++;
					} else {
						columnMap.put(colName, field);
					}
				}
			}
			/*
			 * we can generate key, but only if it is of integral type
			 */
			if (this.keyToBeGenerated) {
				if (pkey == null) {
					ctx.addError("Primary key is not defined but keyToBeGenerated is set to true.");
					count++;
				} else if (nbrKeys > 0) {
					ctx.addError(
							"keyToBeGenerated is set to true, but primary key is a composite of more than one fields.");
					count++;
				} else {
					DataType dt = ComponentManager.getDataTypeOrNull(pkey.dataType);
					if (dt != null && dt.getValueType() != ValueType.INTEGER) {
						ctx.addError("keyToBeGenerated is set to true, but primary key field is of value type "
								+ dt.getValueType() + ". We generate only integers.");
						count++;
					}
				}
			}

			/*
			 * we can manage concurrency, but only if a time-stamp field is
			 * defined
			 */
			if (this.useTimestampForConcurrency) {
				if (this.modifiedStampField == null) {
					ctx.addError("useTimestampForConcurrency=true, but no field of type modifiedAt.");
					count++;
				} else if (this.modifiedStampField.getValueType() != ValueType.TIMESTAMP) {
					ctx.addError("useTimestampForConcurrency=true, but the timestamp field "
							+ this.modifiedStampField.name + " is defined as " + this.modifiedStampField.getValueType()
							+ ". It should be defined as a timestamp.");
					count++;
				}
			}
			/*
			 * add referred fields
			 */
			if (this.listFieldName != null) {
				referredFields.add(this.listFieldName);
			}
			if (this.listGroupKeyName != null) {
				referredFields.add(this.listGroupKeyName);
			}
			if (this.suggestionKeyName != null) {
				referredFields.add(this.suggestionKeyName);
			}
			if (this.suggestionOutputNames != null) {
				for (String sug : this.suggestionOutputNames) {
					referredFields.add(sug);
				}
			}
			if (referredFields.size() > 0) {
				for (String fn : referredFields) {
					if (fieldMap.containsKey(fn) == false) {
						ctx.addError("Referred field name " + fn + " is not defined for this record");
						count++;
					}
				}
			}
			/*
			 * check from db if fields are Ok with the db
			 */
			if (this.recordType != RecordUsageType.STRUCTURE) {
				count += this.validateTable(columnMap, ctx);
			}
			if (this.schemaName != null && DbDriver.isSchmeaDefined(this.schemaName) == false) {
				ctx.addError("schemaName is set to " + this.schemaName
						+ " but it is not defined as one of additional schema names in application.xml");
			}
			return count;
		} finally {
			ctx.endValidation();
		}
	}

	private int validateTable(Map<String, Field> columnMap, ValidationContext ctx) {
		String nam = this.tableName;
		if (nam == null) {
			nam = this.name;
		}
		DataSheet columns = DbDriver.getTableColumns(null, nam);
		if (columns == null) {
			ctx.addError(this.tableName + " is not a valid table/view defined in the data base");
			return 1;
		}
		int count = 0;
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
			if (field.dataType != null) {
				DataType dt = ComponentManager.getDataTypeOrNull(field.dataType);
				if (dt != null && dt.getValueType() == ValueType.TEXT) {
					int len = (int) ((IntegerValue) row[5]).getLong();
					int dtLen = dt.getMaxLength();
					if (dtLen > len) {
						ctx.addError("Field " + field.name + " allows a length of " + dtLen + " but db allows a max of "
								+ len + " chars");
					}
				}
			}
		}
		if (columnMap.size() > 0) {
			for (String key : columnMap.keySet()) {
				ctx.addError(key + " is not a valid column name in the data base table/view");
				count++;
			}
		}
		return count;
	}

	@Override
	public ComponentType getComponentType() {
		return MY_TYPE;
	}

	/**
	 * get schema name that the table/view associated with this record is part
	 * of. null if it is part of default schema
	 *
	 * @return null if it is default. Otherwise schema name.
	 */
	public String getSchemaName() {
		return this.schemaName;
	}

	/**
	 *
	 * @return is it okay to cache the list generated from this record?
	 */
	public boolean getOkToCache() {
		return this.listFieldName != null && this.okToCacheList;
	}

	/**
	 *
	 * @return this record is data object if any of its field is non-primitive
	 */
	public boolean isComplexStruct() {
		return this.isComplexStruct;
	}

	/**
	 * Create an array of struct from json that is suitable to be used as a
	 * stored procedure parameter
	 *
	 * @param array
	 * @param con
	 * @param ctx
	 * @param sqlTypeName
	 * @return Array object suitable to be assigned to the callable statement
	 * @throws SQLException
	 */
	public Array createStructArrayForSp(JSONArray array, Connection con, ServiceContext ctx, String sqlTypeName)
			throws SQLException {
		int nbr = array.length();
		Struct[] structs = new Struct[nbr];
		for (int i = 0; i < structs.length; i++) {
			Object childObject = array.get(i);
			if (childObject == null) {
				continue;
			}
			if (childObject instanceof JSONObject == false) {
				ctx.addMessage(Messages.INVALID_VALUE,
						"Invalid input data structure. we were expecting an object inside the array but got "
								+ childObject.getClass().getSimpleName());
				return null;
			}
			structs[i] = this.createStructForSp((JSONObject) childObject, con, ctx, null);

		}
		return DbDriver.createStructArray(con, structs, sqlTypeName);
	}

	/**
	 * extract data as per data structure from json
	 *
	 * @param json
	 * @param ctx
	 * @param con
	 * @param sqlTypeName
	 * @return a struct that can be set as parameter to a stored procedure
	 *         parameter
	 * @throws SQLException
	 */
	public Struct createStructForSp(JSONObject json, Connection con, ServiceContext ctx, String sqlTypeName)
			throws SQLException {
		List<FormattedMessage> errors = new ArrayList<FormattedMessage>();
		int nbrFields = this.fields.length;
		Object[] data = new Object[nbrFields];
		for (int i = 0; i < this.fields.length; i++) {
			Field field = this.fields[i];
			Object obj = json.opt(field.name);
			if (obj == null) {
				Tracer.trace("No value for attribute " + field.name);
				continue;
			}
			/*
			 * array of values
			 */
			if (field.fieldType == FieldType.VALUE_ARRAY) {
				if (obj instanceof JSONArray == false) {
					errors.add(new FormattedMessage(Messages.INVALID_DATA,
							"Input value for parameter. " + field.name + " is expected to be an array of values."));
					continue;
				}
				Value[] arr = field.parseArray(JsonUtil.toObjectArray((JSONArray) obj), errors, this.name);
				data[i] = DbDriver.createArray(con, arr, field.sqlTypeName);
				continue;
			}

			/*
			 * struct (record or object)
			 */
			if (field.fieldType == FieldType.RECORD) {
				if (obj instanceof JSONObject == false) {
					errors.add(new FormattedMessage(Messages.INVALID_DATA,
							"Input value for parameter. " + field.name + " is expected to be an objects."));
					continue;
				}
				Record childRecord = ComponentManager.getRecord(field.referredRecord);
				data[i] = childRecord.createStructForSp((JSONObject) obj, con, ctx, field.sqlTypeName);
				continue;
			}

			/*
			 * array of struct
			 */
			if (field.fieldType == FieldType.RECORD_ARRAY) {
				if (obj instanceof JSONArray == false) {
					errors.add(new FormattedMessage(Messages.INVALID_DATA,
							"Input value for parameter. " + field.name + " is expected to be an array of objects."));
					continue;
				}
				Record childRecord = ComponentManager.getRecord(field.referredRecord);
				data[i] = childRecord.createStructArrayForSp((JSONArray) obj, con, ctx, field.sqlTypeName);
				continue;
			}
			/*
			 * simple value
			 */
			Value value = field.parseObject(obj, errors, false, this.name);
			if (value != null) {
				data[i] = value.toObject();
			}
		}
		/*
		 * did we get into trouble?
		 */
		if (errors.size() > 0) {
			ctx.addMessages(errors);
			return null;
		}
		String nameToUse = sqlTypeName;
		if (nameToUse == null) {
			nameToUse = this.sqlStructName;
		}
		return DbDriver.createStruct(con, data, nameToUse);
	}

	/**
	 * Create a json array from an object returned from an RDBMS
	 *
	 * @param data
	 *            as returned from jdbc driver
	 * @return JSON array
	 */
	public JSONArray createJsonArrayFromStruct(Object data) {
		if (data instanceof Object[][] == false) {
			throw new ApplicationError(
					"Input data from procedure is expected to be Object[][] but we got " + data.getClass().getName());
		}
		return this.toJsonArray((Object[][]) data);
	}

	/**
	 * Create a json Object from an object returned from an RDBMS
	 *
	 * @param data
	 *            as returned from jdbc driver
	 * @return JSON Object
	 */
	public JSONObject createJsonObjectFromStruct(Object data) {
		if (data instanceof Object[] == false) {
			throw new ApplicationError(
					"Input data from procedure is expected to be Object[] but we got " + data.getClass().getName());
		}
		return this.toJsonObject((Object[]) data);
	}

	private JSONArray toJsonArray(Object[][] data) {
		JSONArray array = new JSONArray();
		for (Object[] struct : data) {
			array.put(this.toJsonObject(struct));
		}
		return array;
	}

	private JSONObject toJsonObject(Object[] data) {
		int nbrFields = this.fields.length;
		if (data.length != nbrFields) {
			throw this.getAppError(data.length, null, null, data);
		}

		JSONObject json = new JSONObject();
		for (int i = 0; i < data.length; i++) {
			Field field = this.fields[i];
			Object obj = data[i];
			if (obj == null) {
				json.put(field.name, (Object) null);
				continue;
			}
			/*
			 * array of values
			 */
			if (field.fieldType == FieldType.VALUE_ARRAY) {
				if (obj instanceof Object[] == false) {
					throw this.getAppError(-1, field, " is an array of primitives ", obj);
				}
				json.put(field.name, obj);
				continue;
			}

			/*
			 * struct (record or object)
			 */
			if (field.fieldType == FieldType.RECORD) {
				if (obj instanceof Object[] == false) {
					throw this.getAppError(-1, field, " is a record that expects an array of objects ", obj);
				}
				Record childRecord = ComponentManager.getRecord(field.referredRecord);
				json.put(field.name, childRecord.toJsonObject((Object[]) obj));
				continue;
			}

			/*
			 * array of struct
			 */
			if (field.fieldType == FieldType.RECORD_ARRAY) {
				if (obj instanceof Object[][] == false) {
					throw this.getAppError(-1, field, " is an array record that expects an array of array of objects ",
							obj);
				}
				Record childRecord = ComponentManager.getRecord(field.referredRecord);
				json.put(field.name, childRecord.toJsonArray((Object[][]) obj));
				continue;
			}
			/*
			 * simple value
			 */
			json.put(field.name, obj);
		}
		return json;
	}

	private ApplicationError getAppError(int nbr, Field field, String txt, Object value) {
		StringBuilder sbf = new StringBuilder();
		sbf.append("Error while creating JSON from output of stored procedure using record ")
				.append(this.getQualifiedName()).append(". ");
		if (txt == null) {
			sbf.append("We expect an array of objects with " + this.fields.length + " elements but we got ");
			if (nbr != -1) {
				sbf.append(nbr).append(" elements.");
			} else {
				sbf.append(" an instance of " + value.getClass().getName());
			}
		} else {
			sbf.append("Field ").append(field.name).append(txt).append(" but we got an instance of")
					.append(value.getClass().getName());
		}
		return new ApplicationError(sbf.toString());
	}

	/**
	 * Write an object to writer that represents a JOSONObject for this record
	 *
	 * @param array
	 *
	 * @param writer
	 * @throws SQLException
	 * @throws JSONException
	 */
	public void toJsonArrayFromStruct(Object[] array, JSONWriter writer) throws JSONException, SQLException {
		if (array == null) {
			writer.value(null);
			return;
		}
		writer.array();
		for (Object struct : array) {
			this.toJsonObjectFromStruct((Struct) struct, writer);
		}
		writer.endArray();
	}

	/**
	 * Write an object to writer that represents a JOSONObject for this record
	 *
	 * @param struct
	 *
	 * @param writer
	 * @throws SQLException
	 * @throws JSONException
	 */
	public void toJsonObjectFromStruct(Struct struct, JSONWriter writer) throws JSONException, SQLException {
		Object[] data = struct.getAttributes();
		int nbrFields = this.fields.length;
		if (data.length != nbrFields) {
			throw this.getAppError(data.length, null, null, data);
		}

		writer.object();
		for (int i = 0; i < data.length; i++) {
			Field field = this.fields[i];
			Object obj = data[i];
			writer.key(field.name);
			if (obj == null) {
				writer.value(null);
				continue;
			}
			/*
			 * array of values
			 */
			if (field.fieldType == FieldType.VALUE_ARRAY) {
				if (obj instanceof Array == false) {
					throw this.getAppError(-1, field, " is an array of primitives ", obj);
				}
				writer.array();
				for (Object val : (Object[]) ((Array) obj).getArray()) {
					writer.value(val);
				}
				writer.endArray();
				continue;
			}

			/*
			 * struct (record or object)
			 */
			if (field.fieldType == FieldType.RECORD) {
				if (obj instanceof Struct == false) {
					throw this.getAppError(-1, field, " is an array of records ", obj);
				}
				Record childRecord = ComponentManager.getRecord(field.referredRecord);
				childRecord.toJsonObjectFromStruct((Struct) obj, writer);
				continue;
			}

			/*
			 * array of struct
			 */
			if (field.fieldType == FieldType.RECORD_ARRAY) {
				if (obj instanceof Array == false) {
					throw new ApplicationError("Error while creating JSON from output of stored procedure. Field "
							+ field.name + " is an of record for which we expect an array of object arrays. But we got "
							+ obj.getClass().getName());
				}
				Record childRecord = ComponentManager.getRecord(field.referredRecord);
				Object[] array = (Object[]) ((Array) obj).getArray();
				childRecord.toJsonArrayFromStruct(array, writer);
				continue;
			}
			/*
			 * simple value
			 */
			writer.value(obj);
		}
		writer.endObject();
		return;
	}

	/**
	 * @param ctx
	 * @return an array of values for al fields in this record extracted from
	 *         ctx
	 */
	public Value[] getData(ServiceContext ctx) {
		Value[] result = new Value[this.fields.length];
		for (int i = 0; i < this.fields.length; i++) {
			Field field = this.fields[i];
			result[i] = ctx.getValue(field.name);
		}
		return result;
	}

	/**
	 * crates a default record component for a table from rdbms
	 *
	 * @param schemaName
	 *            null to use default schema. non-null to use that specific
	 *            schema that this table belongs to
	 * @param qualifiedName
	 *            like modulename.recordName
	 * @param tableName
	 *            as in rdbms
	 * @param conversion
	 *            how field names are to be derived from columnName
	 * @param suggester
	 *            data type suggester
	 * @return default record component for a table from rdbms
	 */
	public static Record createFromTable(String schemaName, String qualifiedName, String tableName,
			DbToJavaNameConversion conversion, DataTypeSuggester suggester) {
		DataSheet columns = DbDriver.getTableColumns(schemaName, tableName);
		if (columns == null) {
			String msg = "No table in db with name " + tableName;
			if (schemaName != null) {
				msg += " in schema " + schemaName;
			}
			Tracer.trace(msg);
			return null;
		}
		Record record = new Record();
		record.name = qualifiedName;
		int idx = qualifiedName.lastIndexOf('.');
		if (idx != -1) {
			record.name = qualifiedName.substring(idx + 1);
			record.moduleName = qualifiedName.substring(0, idx);
		}
		record.tableName = tableName;

		int nbrCols = columns.length();
		Field[] fields = new Field[nbrCols];
		for (int i = 0; i < fields.length; i++) {
			Value[] row = columns.getRow(i);
			Field field = new Field();
			fields[i] = field;
			String nam = row[2].toText();
			field.columnName = nam;
			if (conversion == null) {
				field.name = nam;
			} else {
				field.name = conversion.toJavaName(nam);
			}
			String sqlTypeName = row[4].toString();
			field.sqlTypeName = sqlTypeName;

			int sqlType = (int) ((IntegerValue) row[3]).getLong();
			int len = (int) ((IntegerValue) row[5]).getLong();
			int nbrDecimals = (int) ((IntegerValue) row[6]).getLong();
			field.dataType = suggester.suggest(sqlType, sqlTypeName, len, nbrDecimals);
			field.isNullable = ((BooleanValue) row[8]).getBoolean();
			field.isRequired = !field.isNullable;
		}
		record.fields = fields;
		return record;
	}

	/**
	 * generate and save draft record.xmls for all tables and views in the rdbms
	 *
	 * @param folder
	 *            where record.xmls are to be saved. Should be a valid folder.
	 *            Created if the path is valid but folder does not exist. since
	 *            we replace any existing file, we recommend that you call with
	 *            a new folder, and then do file copying if required
	 * @param conversion
	 *            how do we form record/field names table/column
	 * @return number of records saved
	 */
	public static int createAllRecords(File folder, DbToJavaNameConversion conversion) {
		if (folder.exists() == false) {
			folder.mkdirs();
			Tracer.trace("Folder created for path " + folder.getAbsolutePath());
		} else if (folder.isDirectory() == false) {
			Tracer.trace(folder.getAbsolutePath() + " is a file but not a folder. Record generation abandoned.");
			return 0;
		}
		String path = folder.getAbsolutePath() + '/';
		DataSheet tables = DbDriver.getTables(null, null);
		if (tables == null) {
			Tracer.trace("No tables in the db. Records not created.");
			return 0;
		}
		Tracer.trace("Found " + tables.length() + " tables for which we are going to create records.");
		DataTypeSuggester suggester = new DataTypeSuggester();
		String[][] rows = tables.getRawData();
		int nbrTables = 0;
		/*
		 * first row is header. Start from second row.
		 */
		for (int i = 1; i < rows.length; i++) {
			String[] row = rows[i];
			String schemaName = row[0];
			if (schemaName != null && schemaName.isEmpty()) {
				schemaName = null;
			}
			String tableName = row[1];
			String recordName = tableName;
			if (conversion != null) {
				recordName = conversion.toJavaName(tableName);
			}

			Record record = Record.createFromTable(schemaName, recordName, tableName, conversion, suggester);
			if (record == null) {
				Tracer.trace("Record " + recordName + " could not be generated from table/view " + tableName);
				continue;
			}
			if (row[2].equals("VIEW")) {
				record.recordType = RecordUsageType.VIEW;
				record.readOnly = true;
			}
			File file = new File(path + recordName + ".xml");
			OutputStream out = null;
			try {
				if (file.exists() == false) {
					file.createNewFile();
				}
				out = new FileOutputStream(file);
				if (XmlUtil.objectToXml(out, record)) {
					nbrTables++;
				}
			} catch (Exception e) {
				Tracer.trace(e, "Record " + recordName + " generated from table/view " + tableName
						+ " but could not be saved. ");
				continue;
			} finally {
				if (out != null) {
					try {
						out.close();
					} catch (Exception ignore) {
						//
					}
				}
			}

		}
		return nbrTables;
	}

	/**
	 * @return table name for this record
	 */
	public String getTableName() {
		return this.tableName;
	}

	/**
	 *
	 * @param fields
	 */
	public void setFields(Field[] fields) {
		this.fields = fields;
	}

	/**
	 * parses a fixed length text into values for fields.
	 *
	 * @param rowText
	 * @param errors
	 * @return row of values
	 */
	public Value[] parseRow(String rowText, List<FormattedMessage> errors) {
		Value[] values = new Value[this.fields.length];
		int idx = 0;
		int startAt = 0;
		for (Field field : this.fields) {
			int endAt = startAt + field.fieldWidth;
			String text = rowText.substring(startAt, endAt);
			values[idx] = field.parseField(text, errors, false, this.getDefaultSheetName());
			idx++;
			startAt = endAt;
		}

		return values;
	}

	/**
	 * @param inRecord
	 *            record to be used to input filter fields
	 * @param inData
	 *            that has the values for filter fields
	 * @param driver
	 * @param useCompactFormat
	 *            json compact format is an array of arrays of data, with first
	 *            row as header. Otherwise, each row is an object
	 * @param writer
	 *            json writer to which we will output 0 or more objects or
	 *            arrays. (Caller should have started an array. and shoudl end
	 *            array after this call
	 *
	 */
	public void filterToJson(Record inRecord, FieldsInterface inData, DbDriver driver, boolean useCompactFormat,
			ResponseWriter writer) {
		/*
		 * we have to create where clause with ? and corresponding values[]
		 */
		SqlAndValues temp = this.getSqlAndValues(inData, inRecord);
		String[] names = this.getFieldNames();
		/*
		 * in compact form, we write a header row values
		 */
		if (useCompactFormat) {
			writer.array();
			for (String nam : names) {
				writer.value(nam);
			}
			writer.endArray();
			names = null;
		}
		driver.sqlToJson(temp.sql, temp.values, this.getValueTypes(), names, writer);
	}

	/**
	 * worker method to create a prepared statement and corresponding values for
	 * filter method
	 *
	 * @param inData
	 * @param inRecord
	 * @return struct that has both sql and values
	 */
	private SqlAndValues getSqlAndValues(FieldsInterface inData, Record inRecord) {
		StringBuilder sql = new StringBuilder(this.filterSql);
		List<Value> filterValues = new ArrayList<Value>();
		boolean firstTime = true;
		for (Field field : inRecord.fields) {
			String fieldName = field.name;
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
			Value otherValue = inData.getValue(fieldName + ServiceProtocol.COMPARATOR_SUFFIX);
			if (otherValue != null && otherValue.isUnknown() == false) {
				condition = FilterCondition.parse(otherValue.toText());
			}

			/**
			 * handle the special case of in-list
			 */
			if (condition == FilterCondition.In) {
				Value[] values = Value.parse(value.toString().split(","), field.getValueType());
				/*
				 * we are supposed to have validated this at the input gate...
				 * but playing it safe
				 */
				if (values == null) {
					throw new ApplicationError(value + " is not a valid comma separated list for field " + field.name);
				}
				sql.append(field.columnName).append(" in (?");
				filterValues.add(values[0]);
				for (int i = 1; i < values.length; i++) {
					sql.append(",?");
					filterValues.add(values[i]);
				}
				sql.append(") ");
				continue;
			}

			if (condition == FilterCondition.Like) {
				value = Value.newTextValue(Record.PERCENT + DbDriver.escapeForLike(value.toString()) + Record.PERCENT);
			} else if (condition == FilterCondition.StartsWith) {
				value = Value.newTextValue(DbDriver.escapeForLike(value.toString()) + Record.PERCENT);
			}

			sql.append(field.columnName).append(condition.getSql()).append("?");
			filterValues.add(value);

			if (condition == FilterCondition.Between) {
				otherValue = inData.getValue(fieldName + ServiceProtocol.TO_FIELD_SUFFIX);
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
		Value sorts = inData.getValue(ServiceProtocol.SORT_COLUMN_NAME);
		if (sorts != null) {
			sql.append(" ORDER BY ").append(sorts.toString());
		}
		return new SqlAndValues(sql.toString(), values);
	}

	/**
	 * format a row based on values in the fields collection
	 *
	 * @param outDataFormat
	 * @param fieldValues
	 * @return text that serializes data as per the format
	 */
	public String formatFlatRow(FlatFileRowType outDataFormat, FieldsInterface fieldValues) {
		if (outDataFormat == FlatFileRowType.FIXED_WIDTH) {
			return DataSerializationType.FIXED_WIDTH.serializeFields(fieldValues, this.getFields());
		}
		if (outDataFormat == FlatFileRowType.COMMA_SEPARATED) {
			return DataSerializationType.COMMA_SEPARATED.serializeFields(fieldValues, this.getFields());
		}
		return null;
	}

	/**
	 * extract values from a flat-file text row
	 *
	 * @param inText
	 *            flat-file text row
	 * @param inDataFormat
	 * @param errors
	 * @return array of values, or null in case of any validation error. Error
	 *         message would have been added to errors
	 */
	public Value[] extractFromFlatRow(String inText, FlatFileRowType inDataFormat, List<FormattedMessage> errors) {
		/*
		 * split input into individual text values
		 */
		String[] inputTexts;
		if (inDataFormat == FlatFileRowType.FIXED_WIDTH) {
			inputTexts = this.splitFixedWidthInput(inText, errors);
			if (inputTexts == null) {
				/*
				 * error: message already added by called method
				 */
				return null;
			}
		} else {
			inputTexts = inText.split(",");
			if (inputTexts.length != this.fields.length) {
				FormattedMessage msg = new FormattedMessage("kernel.invalidInputStream", inText);
				msg.addData(inText);
				errors.add(msg);
				return null;
			}
		}
		/*
		 * validate and extract
		 */
		Value[] values = new Value[this.fields.length];
		for (int i = 0; i < this.fields.length; i++) {
			Field field = this.fields[i];
			String text = inputTexts[i];
			values[i] = field.parseField(text, errors, false, this.name);
		}
		return values;
	}

	/**
	 * parse fields from a flat file row text into fields, of course with
	 * validation
	 *
	 * @param inText
	 * @param inDataFormat
	 * @param fieldValues
	 * @param errors
	 *            any validation errors are added to this collection
	 */
	public void parseFlatFileRow(String inText, FlatFileRowType inDataFormat, FieldsInterface fieldValues,
			List<FormattedMessage> errors) {
		Value[] values = this.extractFromFlatRow(inText, inDataFormat, errors);

		if (values != null) {
			for (int i = 0; i < values.length; i++) {
				fieldValues.setValue(this.fieldNames[i], values[i]);
			}
		}
	}

	/**
	 * split fixed-width row text into its field texts
	 *
	 * @param inText
	 * @return
	 */
	private String[] splitFixedWidthInput(String inText, List<FormattedMessage> errors) {
		if (inText.length() < this.minRecordLength) {
			FormattedMessage msg = new FormattedMessage("kernel.invalidInputStream",
					"fixed-width input row has " + inText.length() + " chracters while this record " + this.name
							+ " is designed for a minimum of" + this.minRecordLength + " characters");
			msg.addData(inText);
			errors.add(msg);
			return null;
		}
		String[] texts = new String[this.fields.length];
		int beginIdx = 0;
		/*
		 * last field takes whatever is available. so keep that out of the loop
		 */
		int nbr = texts.length - 1;
		for (int i = 0; i < nbr; i++) {
			int width = this.fields[i].fieldWidth;
			int endIdx = beginIdx + width;
			texts[i] = inText.substring(beginIdx, endIdx);
			beginIdx = endIdx;
		}
		texts[nbr] = inText.substring(beginIdx);
		return texts;
	}

	/**
	 * definitions object
	 */
	private static final String DEFS_ATTR = "definitions";
	private static final String REF_ATTR = "$ref";
	private static final String DEFAULT_ATTR = "default";
	private static final String FIELD_NAME_ATTR = "x-fieldName";
	private static final String COLN_FORMAT_ATT = "collectionFormat";
	private static final String FORMAT_ATT = "format";
	private static final String REQ_ATTR = "required";
	private static final String PROPERTIES_ATTR = "properties";
	private static final String TYPE_ATTR = "type";

	/**
	 * creates a record component based on schema specification from a swagger
	 * document
	 *
	 * @param schema
	 *            non-null non-empty object with each attribute representing a
	 *            field
	 * @param recordName
	 *            non-null simple name of the record
	 * @param moduleName
	 *            can be null. module name. Full name of record would be
	 *            moduleName.recordNme
	 * @return record, or null in case the schema is not valid.
	 */
	public static Record fromSwagger(JSONObject schema, String recordName, String moduleName,String[] defNames) {
		if (schema == null) {
			Tracer.trace("schema for record " + recordName + " is null");
			return null;
		}
		JSONObject attrs = schema.optJSONObject(PROPERTIES_ATTR);
		if(attrs == null ||attrs.length() == 0){
			Tracer.trace("Schema for record " + recordName + " has no fields");
			return null;
		}
		/*
		 * list of required fields
		 */
		JSONArray arr = schema.optJSONArray(REQ_ATTR);
		Set<String> reqs = null;
		if(arr != null){
			reqs = new HashSet<String>();
			for(Object r : reqs){
				reqs.add(r.toString());
			}
		}

		Record record = new Record();
		record.name = recordName;
		record.moduleName = moduleName;
		List<String> childRecs = new ArrayList<String>();
		String[] names = JSONObject.getNames(attrs);
		int nbr = names.length;
		List<Field> fieldsList = new ArrayList<Field>();
		Field[] fields = new Field[nbr];
		for (int i = 0; i < fields.length; i++) {
			String fn = names[i];
			JSONObject attr = attrs.optJSONObject(fn);
			if (attr == null) {
				Tracer.trace("Schema has improper details for field " + fn);
				return null;
			}
			Field field = new Field();			
			field.name = attr.optString(FIELD_NAME_ATTR, fn);
			field.isRequired = reqs !=null && reqs.contains(fn);
			Object def = attr.opt(DEFAULT_ATTR);
			if (def != null) {
				field.defaultValue = def.toString();
			}
			setTypeForSwaggerType(field, attr);
			/*
			 * if this is an object, we have to recurse
			 */
			if(field.fieldType == FieldType.RECORD || field.fieldType == FieldType.VALUE_ARRAY){
				/*
				 * good practice is to define all records at the root level, and refer them anywhere. Hence we will not do this recursion
				 */
	
				for(String s: defNames){
					String refDefinition = attr.optJSONObject("items").optString(REF_ATTR);
					if(s.equalsIgnoreCase(refDefinition.substring(refDefinition.lastIndexOf('/')+1))){
						if(moduleName != null){
							childRecs.add(moduleName+"."+s);
						}else{
							childRecs.add(s);
						}
						break;
					}
				}
				continue;
			}
			fieldsList.add(field);
		}
		int nbrOfChildRecs = childRecs.size();
		if(nbrOfChildRecs > 0){
			record.childrenToBeSaved = new String[nbrOfChildRecs];
			record.childrenToBeRead = new String[nbrOfChildRecs];
			record.childrenToBeSaved = childRecs.toArray(record.childrenToBeSaved);
			record.childrenToBeRead = childRecs.toArray(record.childrenToBeRead);
		}
		fields = fieldsList.toArray(new Field[0]);
		record.okToSelectAll = true;
		record.fields = fields;
		return record;
	}

	/**
	 * @param parm
	 */
	public static void setTypeForSwaggerType(Field field, JSONObject parm) {
		String type = parm.optString(TYPE_ATTR, null);
		String format = parm.optString(FORMAT_ATT, null);
		/*
		 * default text
		 */
		field.dataType = DataType.DEFAULT_TEXT;
		if (type == null) {
			return;
		}
		if (type.equals("string")) {
			if (format != null) {
				if (format.equals("date")) {
					field.dataType = DataType.DEFAULT_DATE;
				} else if (format.equals("date-time")) {
					field.dataType = DataType.DEFAULT_DATE_TIME;
				}
			}
			return;
		}
		if (type.equals("integer")) {
			field.dataType = DataType.DEFAULT_NUMBER;
			return;
		}
		if (type.equals("number")) {
			field.dataType = DataType.DEFAULT_DECIMAL;
			return;
		}
		if (type.equals("boolean")) {
			field.dataType = DataType.DEFAULT_BOOLEAN;
			return;
		}
		if (type.equals("file")) {
			Tracer.trace("Simplity does not handle files as a dat type in dat amodels. text/string is assumed");
			return;
		}
		if (type.equals("array")) {
			if (parm.opt(COLN_FORMAT_ATT) != null) {
				Tracer.trace("array with a serialization type has been defined as text.");
				return;
			}
			field.fieldType = FieldType.VALUE_ARRAY;
			return;
		}

		if (type.equals("object")) {
			field.fieldType = FieldType.RECORD;
			return;
		}
		/*
		 * use text as default
		 */
		Tracer.trace("Unable to determine the type, and hence resporting to text");
		return;
	}

	/**
	 * create record for each of the schema defined in the swagger in the
	 * definitions section
	 *
	 * @param moduleName
	 * @param defs
	 * @return array of records generated
	 */
	public static Record[] fromSwaggerDefinitions(String moduleName, JSONObject defs) {
		Record[] empty = new Record[0];
		if (defs == null) {
			return empty;
		}
		String[] names = JSONObject.getNames(defs);
		if (names.length == 0) {
			return empty;
		}

		List<Record> records = new ArrayList<Record>();
		for (String recName : names) {
			parseSchema(recName, moduleName, defs.getJSONObject(recName), records,names);
		}

		if (records.size() == 0) {
			return empty;
		}
		return records.toArray(empty);
	}

	/**
	 *
	 * @param recName
	 * @param schema
	 * @param records
	 */

	private static void parseSchema(String recName, String moduleName, JSONObject schema, List<Record> records,String[] defNames) {
		if (schema.opt(REF_ATTR) != null) {
			Tracer.trace("schema " + recName + " is referencing another schema, and hence ignored.");
			return;
		}
		Record record = fromSwagger(schema, recName, moduleName,defNames);
		if (record != null) {
			records.add(record);
		}
	}
	/**
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		String rootFolder = "D:/";
		String txt = FileManager.readFile(new File(rootFolder + "troubleTicket.json"));
		JSONObject swagger = new JSONObject(txt);
		JSONObject defs = swagger.optJSONObject(DEFS_ATTR);
		if(defs == null){
			Tracer.trace("No defintions found");
			return;
		}

		Tracer.trace("going to scan " + defs.length() + " schemas at the root level");
		Record[] recs = Record.fromSwaggerDefinitions(null, defs);
		for(Record rec : recs){
			Tracer.trace(XmlUtil.objectToXmlString(rec));
		}

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