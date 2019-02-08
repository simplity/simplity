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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.simplity.core.ApplicationError;
import org.simplity.core.app.AppConventions;
import org.simplity.core.app.Application;
import org.simplity.core.comp.ComponentType;
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IComponent;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationMessage;
import org.simplity.core.comp.ValidationUtil;
import org.simplity.core.data.DataPurpose;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.data.MultiRowsSheet;
import org.simplity.core.data.SingleRowSheet;
import org.simplity.core.dm.field.Field;
import org.simplity.core.service.InputRecord;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.util.TextUtil;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author simplity.org
 *
 */
public class Record implements IComponent {
	private static final Logger logger = LoggerFactory.getLogger(Record.class);

	private static String TABLE_ACTION_FIELD_NAME = AppConventions.Name.TABLE_ACTION;
	/*
	 * initialization deferred because it needs bootstrapping..
	 */
	private static Field TABLE_ACTION_FIELD = null;

	/**
	 * * simple name, unique within a group
	 */
	@FieldMetaData(isRequired = true)
	String name;

	/**
	 * module name + name would be unique for a component type within an
	 * application. we also insist on a java-like convention that the the
	 * resource is stored in a folder structure that mimics module name
	 */
	String moduleName;

	/**
	 * what is the sheet name to be used as input/output sheet. (specifically
	 * used in creating services on the fly)
	 */
	String defaultSheetName = null;

	/**
	 * in case this is a view, then the record from which fields are referred by
	 * default
	 */
	@FieldMetaData(isReferenceToComp = true, referredCompType = ComponentType.REC)
	String defaultRefRecord;

	/**
	 * fields that this record is made-up of
	 */
	@FieldMetaData(isRequired = true)
	Field[] fields = new Field[0];

	/**
	 * Is there at least one field that has validations linked to another field.
	 * Initialized during init operation
	 */
	protected boolean hasInterFieldValidations;
	/** we need the set to validate field names at time */
	protected final Map<String, Field> indexedFields = new HashMap<String, Field>();

	/** and field names of course. cached after loading */
	protected String[] fieldNames;

	protected Field[] encryptedFields;

	protected RecordUsageType recordUsageType;

	private boolean isComplex;

	/**
	 * Default Constructor
	 */
	public Record() {
		this.recordUsageType = RecordUsageType.DATA_STRUCTURE;
	}

	@Override
	public String getSimpleName() {
		return this.name;
	}

	@Override
	public ComponentType getComponentType() {
		return ComponentType.REC;
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

	@Override
	public String getQualifiedName() {
		if (this.moduleName == null) {
			return this.name;
		}
		return this.moduleName + '.' + this.name;
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
		int n = this.fields.length;
		Field[] allFields = Arrays.copyOf(this.fields, n + 1);
		allFields[n] = TABLE_ACTION_FIELD;
		return allFields;
	}

	/**
	 * create an empty data sheet based on this record
	 *
	 * @param forSingleRow
	 *            true if you intend to store only one row. False otherwise
	 * @param addActionColumn
	 *            should we add a column to have action to be performed on each
	 *            row
	 * @return an empty sheet ready to receive data
	 */
	public IDataSheet createSheet(boolean forSingleRow, boolean addActionColumn) {
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
	public IDataSheet createSheet(String[] colNames, boolean forSingleRow, boolean addActionColumn) {
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
	 * @return referred record, without getting into an infinite loop because of
	 *         referred records
	 */
	public Record getRefRecord(String recordName) {
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
		return Application.getActiveInstance().getRecord(recordName);
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

		logger.error(sbf.toString());
		return false;
	}

	/**
	 * called at the end of getReady();
	 */
	private void recordGotReady(boolean originator) {
		String recName = this.getQualifiedName();
		RefHistory history = referenceHistory.get();
		if (history == null) {
			logger.error("There is an issue with the way Record " + recName
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

			logger.error("There is an issue with the way Record " + recName
					+ "  is trying to detect circular reference. pending list remained as " + sbf.toString());
		}
		referenceHistory.remove();
	}

	@Override
	public void getReady() {
		if (TABLE_ACTION_FIELD == null) {
			TABLE_ACTION_FIELD = Field.getDefaultField(AppConventions.Name.TABLE_ACTION, ValueType.TEXT);
		}
		if (this.fields == null) {
			throw new ApplicationError("Record " + this.getQualifiedName() + " has no fields.");
		}
		if (this.defaultSheetName == null) {
			this.defaultSheetName = this.name;
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
		int nbrEncrypted = 0;

		for (int i = 0; i < this.fields.length; i++) {
			Field field = this.fields[i];
			if (field.isPrimitive() == false) {
				this.isComplex = true;
			}
			field.getReady(this, refRecord);
			String fName = field.getName();
			this.fieldNames[i] = fName;
			this.indexedFields.put(fName, field);
			if (!this.hasInterFieldValidations && field.hasInterFieldValidations()) {
				this.hasInterFieldValidations = true;
			}
			if (field.isEncrypted()) {
				nbrEncrypted++;
			}
		}

		if (nbrEncrypted > 0) {
			this.cacheEncryptedFields(nbrEncrypted);
		}
		this.getReadyExtension(refRecord);
		/*
		 * we have successfully loaded. remove this record from stack.
		 */
		this.recordGotReady(originator);
	}

	/**
	 * @param refRecord
	 */
	protected void getReadyExtension(Record refRecord) {
		// this is for sub-classes to put code specific to them
	}

	/**
	 *
	 * @return does this record require that the storage mechanism generate a
	 *         key
	 */
	public boolean isKeyGenerated() {
		return false;
	}

	private void cacheEncryptedFields(int nbrEncrypted) {
		this.encryptedFields = new Field[nbrEncrypted];
		int encrIdx = 0;
		for (Field field : this.fields) {
			if (field.isEncrypted()) {
				this.encryptedFields[encrIdx] = field;
				encrIdx++;
			}
		}
	}

	/**
	 * @return all fields of this record.
	 */
	public Field[] getFields() {
		return this.fields;
	}

	/**
	 * @return all fields mapped by their names
	 */
	public Map<String, Field> getFieldsMap() {
		return this.indexedFields;
	}

	/*
	 * this method is to be over-ridden by dbTable to take care of keys etc..
	 */
	/**
	 * get fields based on names, or all fields
	 *
	 * @param names
	 *            null if all fields are to be extracted
	 * @param purpose
	 * @param extractSaveAction
	 * @return fields
	 */
	public Field[] getFieldsToBeInput(String[] names, DataPurpose purpose, boolean extractSaveAction) {
		/*
		 * is the caller choosing a subset of fields?
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
								s + " is not a valid field in Record " + this.name + ". Field can not be extracted.");
					}
				}
				result[i] = field;
				i++;
			}
			return result;
		}
		if (extractSaveAction == false) {
			return this.fields;
		}
		/*
		 * append save action as well
		 */
		int n = this.fields.length;
		Field[] result = Arrays.copyOf(this.fields, n + 1);
		result[n] = TABLE_ACTION_FIELD;
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

	@Override
	public void validate(IValidationContext vtx) {
		ValidationUtil.validateMeta(vtx, this);
		/*
		 * validate fields, and accumulate them in a map for other validations
		 */
		Set<String> names = new HashSet<String>();
		Set<String> externs = new HashSet<String>();
		Set<String> referredFields = new HashSet<String>();
		for (Field field : this.fields) {
			field.validate(vtx, this, referredFields);

			/*
			 * look for duplicate field name
			 */
			String fn = field.getName();
			if (names.add(fn) == false) {
				vtx.message(new ValidationMessage(field, ValidationMessage.SEVERITY_ERROR, "Field " + fn +
						" is a duplicate", "name"));
			}

			/*
			 * duplicate external name?
			 */
			fn = field.getExternalName();
			if (externs.add(fn) == false) {
				vtx.message(new ValidationMessage(field, ValidationMessage.SEVERITY_ERROR,
						"Field " + field.getName() + " has its external name set to " +
								fn + ". This column name is duplicated",
						"externalName"));

			}
		}

		if (referredFields.size() > 0) {
			for (String fn : referredFields) {
				if (names.contains(fn) == false) {
					vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
							"Referred field name " + fn + " is not defined for this record", null));
				}
			}
		}
	}

	/**
	 * @param ctx
	 * @return an array of values for all fields in this record extracted from
	 *         ctx
	 */
	public Value[] getValues(ServiceContext ctx) {
		Value[] result = new Value[this.fields.length];
		for (int i = 0; i < this.fields.length; i++) {
			Field field = this.fields[i];
			result[i] = ctx.getValue(field.getName());
		}
		return result;
	}

	/**
	 * encrypt/decrypt columns in this data sheet
	 *
	 * @param sheet
	 * @param toDecrypt
	 *            true means decrypt, else encrypt
	 */
	protected void crypt(IDataSheet sheet, boolean toDecrypt) {
		int nbrRows = sheet.length();
		if (nbrRows == 0) {

			logger.info("Data sheet has no data and hance no encryption.");

			return;
		}
		for (Field field : this.encryptedFields) {
			int colIdx = sheet.getColIdx(field.getName());
			if (colIdx == -1) {

				logger.info("Data sheet has no column named " + field.getName() + " hance no encryption.");

				continue;
			}
			for (int rowIdx = 0; rowIdx < nbrRows; rowIdx++) {
				Value[] row = sheet.getRow(rowIdx);
				row[colIdx] = this.crypt(row[colIdx], toDecrypt);
			}
		}
		logger.info(nbrRows + " rows and " + this.encryptedFields.length + " columns " + (toDecrypt ? "un-" : "")
				+ "obsfuscated");
	}

	/**
	 * encrypt/decrypt a value
	 *
	 * @param value
	 * @param toDecrypt
	 *            true means decrypt, else encrypt
	 * @return
	 */
	protected Value crypt(Value value, boolean toDecrypt) {
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
	 * @param parentSheetName
	 *            data sheet/object inside which this sheet/object is expected
	 *            as input. null if input is expected on-its-own.
	 * @return an input record that can be used in an input data specification
	 */
	public InputRecord getInputRecord(String parentSheetName) {
		return new InputRecord(this.getQualifiedName(), this.defaultSheetName);
	}

	/**
	 * @return record usage type
	 */
	public RecordUsageType getRecordUsageType() {
		return this.recordUsageType;
	}

	/**
	 * @return sqlTypeName associated with this record
	 */
	public Object getSqlTypeName() {
		return null;
	}

	/**
	 * are there any non-primitive fields in this record?
	 *
	 * @return true if there is at least one non-primitive field. false if all
	 *         fields are primitive
	 */
	public boolean hasNonPrimitiveFields() {
		return this.isComplex;
	}

}
