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
package org.simplity.core.sql;

import org.simplity.core.ApplicationError;
import org.simplity.core.app.Application;
import org.simplity.core.app.IResponseWriter;
import org.simplity.core.comp.ComponentType;
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IComponent;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationMessage;
import org.simplity.core.comp.ValidationUtil;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.data.IFieldsCollection;
import org.simplity.core.data.MultiRowsSheet;
import org.simplity.core.data.SingleRowSheet;
import org.simplity.core.dm.Record;
import org.simplity.core.dm.field.Field;
import org.simplity.core.idb.IReadOnlyHandle;
import org.simplity.core.idb.IResultSetReader;
import org.simplity.core.idb.IRowWithNameConsumer;
import org.simplity.core.idb.ITransactionHandle;
import org.simplity.core.util.RdbUtil;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;

/**
 * A prepared statement with which to interacts with the data base.
 *
 * @author simplity.org
 */
public class Sql implements IComponent {
	private static final ComponentType MY_TYPE = ComponentType.SQL;

	/** unique within a module */
	String name;

	/** module + name is unique */
	String moduleName;
	/** prepared statement. */
	@FieldMetaData(isRequired = true)
	String preparedStatement;

	/**
	 * purpose of this sql/procedure. Important to specify whether you are
	 * expecting output, and if so whether we may get more than one rows
	 */
	SqlType sqlType;
	/** input parameters. In the same order as in prepared statement. */
	SqlParameter[] inputParameters;

	/**
	 * output parameters if this is a select sql. Alternately, you may specify
	 * an output record. You should not specify both.
	 */
	SqlParameter[] outputParameters;

	/**
	 * if you already have a record that has the right fields as input for this
	 * sql, this is easier than specifying the fields in inputParameters
	 */
	@FieldMetaData(isReferenceToComp = true, referredCompType = ComponentType.REC)
	String inputRecordName;
	/** If you already have a record that has the right fields for this sql.. */
	@FieldMetaData(isReferenceToComp = true, referredCompType = ComponentType.REC)
	String outputRecordName;
	/** we need names and types repeatedly. Better cache them */
	private String[] outputNames;

	private ValueType[] outputTypes;

	/** @return unqualified name */
	@Override
	public String getSimpleName() {
		return this.name;
	}

	/** @return fully qualified name typically module.name */
	@Override
	public String getQualifiedName() {
		if (this.moduleName == null) {
			return this.name;
		}
		return this.moduleName + '.' + this.name;
	}

	/**
	 * @param inSheet
	 *            data sheet that has input data base don which prepared
	 *            statement is to be populated,one per row
	 * @param handle
	 * @return extracted data
	 */
	public IDataSheet extractBatch(IDataSheet inSheet, IReadOnlyHandle handle) {
		/*
		 * are we running this sql once, or multiple times?
		 */
		int nbrRows = inSheet.length();
		if (nbrRows == 1) {
			return this.extract(inSheet, handle);
		}
		IDataSheet outSheet = this.createOutputSheet();
		handle.readBatch(this.preparedStatement, this.getInputRows(inSheet), outSheet);
		return outSheet;
	}

	/**
	 * use data in the fields collection to prepare the statement.
	 *
	 * @param inputFields
	 *            that has the input values for the prepared statement. Field
	 *            values are validated before using them
	 * @param handle
	 * @return extracted data
	 */
	public IDataSheet extract(IFieldsCollection inputFields, IReadOnlyHandle handle) {
		Value[] values = this.getInputValues(inputFields);
		return this.extract(values, handle);
	}

	/**
	 * @param values
	 *            array of values for the prepared statement. Values are
	 *            validated before using them.
	 * @param handle
	 * @return extracted data
	 */
	public IDataSheet extract(Value[] values, IReadOnlyHandle handle) {
		this.checkReader();

		this.validateValuesInput(values);
		IDataSheet outSheet = this.createOutputSheet();
		IResultSetReader reader = null;
		if (this.sqlType == SqlType.SINGLE_SELECT) {
			reader = RdbUtil.newNamesBasedExtractor(outSheet, outSheet.getColumnNames(), outSheet.getValueTypes());
		} else {
			reader = RdbUtil.newMultiRowsReader(outSheet);
		}
		handle.read(this.preparedStatement, values, reader);
		return outSheet;
	}

	private void validateValuesInput(Value[] values) {
		if (values == null || values.length == 0) {
			if (this.inputParameters != null && this.inputParameters.length > 0) {
				return;
			}
			throw new ApplicationError(
					"Design Error: We were expecting " + this.inputParameters.length + " values but received none.");
		}

		if (this.inputParameters == null) {
			throw new ApplicationError(
					"Design Error: Sql has no parameters, but " + values.length + " values received.");
		}
		if (this.inputParameters.length != values.length) {
			throw new ApplicationError("Design Error: We were expecting " + this.inputParameters.length
					+ " values but received " + values.length);
		}
	}

	/**
	 * @param inputData
	 *            from which values are picked-up for the prepared statement
	 * @param handle
	 * @param rowConsumer
	 *            that is called for each row
	 * @return number of rows extracted
	 */
	public int processRows(IFieldsCollection inputData, IReadOnlyHandle handle, IRowWithNameConsumer rowConsumer) {
		this.checkReader();
		Value[] values = this.getInputValues(inputData);
		return handle.read(this.preparedStatement, values, RdbUtil.newReaderForConsumer(rowConsumer));
	}

	private void checkReader() {
		if (this.sqlType != SqlType.UPDATE) {
			throw new ApplicationError(
					"Sql " + this.getQualifiedName() + " is meant for update, but it is called for data extraction");
		}
	}

	private void checkUpdater() {
		if (this.sqlType == SqlType.UPDATE) {
			throw new ApplicationError(
					"Sql " + this.getQualifiedName() + " is meant for reading, but it is called for update");
		}
	}

	/**
	 * @param dataRow
	 * @param handle
	 * @param treatErrorAsNoAction
	 *            if true, sql exception is assumed to be because of some
	 *            constraints, and hence rows affected is set to 0
	 * @return number of affected rows
	 */
	public int execute(IFieldsCollection dataRow, ITransactionHandle handle, boolean treatErrorAsNoAction) {
		this.checkUpdater();
		return handle.execute(this.preparedStatement, this.getInputValues(dataRow), treatErrorAsNoAction);
	}

	/**
	 * @param inSheet
	 * @param handle
	 * @param treatErrorAsNoAction
	 * @return number of affected rows
	 */
	public int executeBatch(IDataSheet inSheet, ITransactionHandle handle, boolean treatErrorAsNoAction) {
		this.checkUpdater();
		int nbrRows = inSheet.length();
		if (nbrRows == 0) {
			return handle.execute(this.preparedStatement, this.getInputValues(inSheet), treatErrorAsNoAction);
		}
		int[] result = handle.executeBatch(this.preparedStatement, this.getInputRows(inSheet), treatErrorAsNoAction);
		nbrRows = 0;
		for (int i : result) {
			if (i == -1) {
				return -1;
			}
			nbrRows += i;
		}
		return nbrRows;
	}

	/** @return a suitable output sheet */
	private IDataSheet createOutputSheet() {
		if (this.outputRecordName != null) {
			Record record = Application.getActiveInstance().getRecord(this.outputRecordName);
			return record.createSheet(this.sqlType == SqlType.MULTI_SELECT, false);
		}
		if (this.sqlType == SqlType.MULTI_SELECT) {
			return new MultiRowsSheet(this.outputNames, this.outputTypes);
		}
		return new SingleRowSheet(this.outputNames, this.outputTypes);
	}

	/**
	 * get input values based on the supplied name-value pair
	 *
	 * @param inValues
	 * @return
	 */
	private Value[] getInputValues(IFieldsCollection inValues) {
		/*
		 * user record
		 */
		if (this.inputRecordName != null) {
			Record record = Application.getActiveInstance().getRecord(this.inputRecordName);
			Field[] fields = record.getFields();
			Value[] values = new Value[fields.length];
			int i = 0;
			for (Field field : fields) {
				values[i++] = field.getValue(inValues, null);
			}
			return values;
		}
		/*
		 * use parameters
		 */
		if (this.inputParameters != null) {
			Value[] values = new Value[this.inputParameters.length];
			int i = 0;
			for (SqlParameter param : this.inputParameters) {
				values[i++] = param.getValue(inValues);
			}
			return values;
		}

		Value[] values = {};
		return values;
	}

	/**
	 * get input values based on the supplied name-value pair
	 *
	 * @param inValues
	 * @return
	 */
	private Value[][] getInputRows(IDataSheet inSheet) {
		int nbrRows = inSheet.length();
		Value[][] values = new Value[nbrRows][];
		for (IFieldsCollection row : inSheet) {
			values[nbrRows++] = this.getInputValues(row);
		}
		return values;
	}

	/** called by loader after loading this class. */
	@Override
	public void getReady() {
		if (this.inputParameters != null) {
			for (SqlParameter parm : this.inputParameters) {
				parm.getReady();
			}
		}
		if (this.outputParameters != null) {
			int nbr = this.outputParameters.length;
			this.outputNames = new String[nbr];
			this.outputTypes = new ValueType[nbr];
			for (int i = 0; i < this.outputParameters.length; i++) {
				SqlParameter parm = this.outputParameters[i];
				parm.getReady();
				this.outputNames[i] = parm.name;
				this.outputTypes[i] = parm.getValueType();
			}
		}
	}

	@Override
	public ComponentType getComponentType() {
		return MY_TYPE;
	}

	@Override
	public void validate(IValidationContext vtx) {
		ValidationUtil.validateMeta(vtx, this);
		if (this.preparedStatement == null) {
			return;
		}

		if (!this.preparedStatement.contains("?")) {
			vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
					"preparedStatement does not have any parameters", "preparedStatement"));
			return;
		}
		int nbrParams = this.preparedStatement.length() - this.preparedStatement.replace("?", "").length();

		if (this.inputParameters != null) {
			if (nbrParams != this.inputParameters.length) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
						"There are " + nbrParams + " parameters in prepared statement, but "
								+ this.inputParameters + " number input parameters.",
						"inputParameters"));
			}
			for (SqlParameter p : this.inputParameters) {
				p.validate(vtx);
			}
			if (this.inputRecordName != null) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
						"Specify either input parameters or inputRecordName but not both.", "inputRecordName"));
			}
		}

		if (this.outputParameters != null) {
			if (this.sqlType == SqlType.UPDATE) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
						"This is for update but outputParameters specified.", "outputParameters"));
			}
			for (SqlParameter p : this.inputParameters) {
				p.validate(vtx);
			}
			if (this.outputRecordName != null) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
						"Both output parameters and outputRecordName are specified.", "outputRecordName"));
			}
		}

		if (this.outputRecordName != null) {
			if (this.sqlType == SqlType.UPDATE) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
						"This is for update but outputRecordName is specified.", "outputRecordName"));
			}
		}

	}

	/** @return the sqlType */
	public SqlType getSqlType() {
		return this.sqlType;
	}

	/**
	 * Create a elements of a josn array directly from the output of this sql
	 *
	 * @param inData
	 *            source of values for input fields
	 * @param handle
	 * @param useCompactFormat
	 *            if true, a header array is written first with column names,
	 *            followed by an array of values for each row. If false, an
	 *            object is written for each row.
	 * @param writer
	 * @return number of rows written
	 */
	public int sqlToJson(IFieldsCollection inData, IReadOnlyHandle handle, boolean useCompactFormat,
			IResponseWriter writer) {
		Value[] values = this.getInputValues(inData);
		String[] names = this.outputNames;
		ValueType[] types = this.outputTypes;
		if (names == null) {
			Record record = Application.getActiveInstance().getRecord(this.outputRecordName);
			names = record.getFieldNames();
			types = record.getValueTypes();
		}
		return handle.read(this.preparedStatement, values,
				RdbUtil.newReaderForResponseWriter(writer, useCompactFormat, names, types));
	}
}
