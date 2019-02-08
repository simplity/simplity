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
package org.simplity.core.sql;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Types;

import org.simplity.core.ApplicationError;
import org.simplity.core.app.Application;
import org.simplity.core.comp.ComponentType;
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationUtil;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.data.IFieldsCollection;
import org.simplity.core.data.MultiRowsSheet;
import org.simplity.core.dm.ComplexRecord;
import org.simplity.core.dt.DataType;
import org.simplity.core.idb.IReadOnlyHandle;
import org.simplity.core.msg.Messages;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;
import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;
import org.simplity.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * parameter used in a prepared statement or stored procedure
 *
 * @author simplity.org
 */
public class ProcedureParameter {
	private static final Logger logger = LoggerFactory.getLogger(ProcedureParameter.class);

	/** name in which field value is found in input */
	@FieldMetaData(isRequired = true)
	String name;
	/** data type */
	@FieldMetaData(isReferenceToComp = true, referredCompType = ComponentType.DT)
	String dataType;
	/**
	 * Value to be used in case field value id omitted. null implies that the
	 * value is mandatory.
	 */
	String defaultValue;

	/** in, out or inOut */
	InputOutputType inOutType = InputOutputType.INPUT;
	/**
	 * if this is a data structure/object, specify the type name as declared in
	 * the db or stored procedure. This MUST match the declared data type
	 */
	@FieldMetaData(leaderField = "recordName")
	String sqlObjectType;
	/**
	 * if sqlType is specified, you MUST define a record that has the
	 * corresponding structure.
	 */
	@FieldMetaData(alternateField = "dataType", isReferenceToComp = true, referredCompType = ComponentType.REC)
	String recordName;
	/** is this an array parameter? */
	boolean isArray;
	/**
	 * if this is an array, then we have to specify the arrayType name as
	 * defined in the stored procedure
	 */
	@FieldMetaData(leaderField = "isArray")
	String sqlArrayType;

	/** is this parameter mandatory */
	boolean isRequired;

	/**
	 * special case where this is a complex parameter - data structure that
	 * contains another data structure (arbitrary structure). Note that the
	 * inputRecord for this should also specify this keyword so that data from
	 * client is extracted into objectData.
	 */
	boolean useObjectDataForInput;
	/** cached for performance */
	private DataType dataTypeObject;
	/** cached for performance */
	private Value defaultValueObjet;

	/** 1-based index of this parameter */
	private int myPosn;

	/**
	 * get ready after loading. Called only once after loading
	 *
	 * @param posn
	 */
	void getReady(int posn) {
		if (this.name == null) {
			throw new ApplicationError("Procedure parameter has an identity crisis. No name is given to it!!");
		}
		this.myPosn = posn;
		if (this.recordName == null) {
			if (this.dataType == null) {
				throw new ApplicationError(
						"Procedure parameter should be either a struct, in which case recordName is specified, or a primitive of a dataType.");
			}
			this.dataTypeObject = Application.getActiveInstance().getDataType(this.dataType);
			if (this.defaultValue != null) {
				this.defaultValueObjet = this.dataTypeObject.parseValue(this.defaultValue);
				if (this.defaultValueObjet == null) {
					throw new ApplicationError("sql parameter " + this.name + " has an invalid default value.");
				}
			}
		} else {
			if (this.sqlObjectType == null) {
				throw new ApplicationError("Stored procedure parameter " + this.name
						+ " has a record associated with it, implying that it is a struct/object. Please specify sqlObjectType as the type of this parameter in the stored procedure");
			}
			this.sqlObjectType = this.sqlObjectType.toUpperCase();
		}
		if (this.isArray) {
			if (this.sqlArrayType == null) {
				throw new ApplicationError("Stored procedure parameter " + this.name
						+ " is an array, and hence sqlArrayType must be specified as the type with which this parameter is defined in the stored procedure");
			}
			this.sqlArrayType = this.sqlArrayType.toUpperCase();
		}
	}

	/**
	 * called before executing the statement. If this has input, we have to set
	 * its value. If it is for output, we have to register it
	 *
	 * @param stmt
	 * @param handle
	 *            db handle that has created this statement
	 * @param inputFields
	 *            from which value for this par
	 * @param ctx
	 *            in case this parameter is an array/object, we get data sheet
	 *            from service context
	 * @return true if all OK, false if we had issue and the process should not
	 *         be continues
	 * @throws SQLException
	 */
	@SuppressWarnings("resource")
	// warning on con : we are not to close it by design
	public boolean setParameter(CallableStatement stmt, IReadOnlyHandle handle, IFieldsCollection inputFields,
			ServiceContext ctx)
			throws SQLException {
		/*
		 * register this param if it is out or in-out
		 */
		if (this.inOutType != InputOutputType.INPUT) {
			this.registerForOutput(stmt);
		}

		if (this.inOutType == InputOutputType.OUTPUT) {
			/*
			 * not an input. we are done.
			 */
			return true;
		}

		if (this.isArray == false && this.recordName == null) {
			/*
			 * it is a simple primitive value
			 */
			Value value = inputFields.getValue(this.name);
			if (value == null || Value.isNull(value)) {
				value = this.defaultValueObjet;
			}
			if (value == null) {
				if (this.isRequired) {
					ctx.addMessage(org.simplity.core.msg.Messages.VALUE_REQUIRED, this.name);
					return false;
				}
				stmt.setNull(this.myPosn, this.getValueType().getSqlType());
				return true;
			}
			value.setToStatement(stmt, this.myPosn);
			return true;
		}

		/*
		 * non-primitive value is generally found in ctx as a data sheet
		 */
		IDataSheet ds = ctx.getDataSheet(this.name);

		if (this.recordName == null) {
			/*
			 * array of primitives
			 */
			Value[] vals = null;
			if (ds == null) {
				/*
				 * we do not give-up that easily. Is this a field with comma
				 * separated values?
				 */
				String txt = ctx.getTextValue(this.name);
				if (txt != null) {
					vals = Value.parse(txt.split(","), this.getValueType());
				}
			} else if (ds.length() > 0) {
				vals = ds.getColumnValues(this.name);
				if (vals == null && ds.width() == 1) {
					/*
					 * Told you, we do not give-up that easily. This is a ds
					 * with only one column. Why bother about matching the
					 * column name
					 */
					vals = ds.getColumnValues(ds.getColumnNames()[0]);
				}
			}
			if (vals == null) {
				this.setNullParam(stmt, ctx);
				return true;
			}

			Array data = handle.createArray(vals, this.sqlArrayType);
			stmt.setArray(this.myPosn, data);
			return true;
		}

		/*
		 * this involves a data structure
		 */
		ComplexRecord record = (ComplexRecord) Application.getActiveInstance()
				.getRecord(this.recordName);

		if (record.hasNonPrimitiveFields()) {
			return this.setComplexStruct(record, stmt, handle, ctx);
		}

		/*
		 * Simple data structure
		 */
		if (this.isArray == false) {
			Value[] values = null;
			if (ds == null) {
				values = record.getValues(ctx);
			} else if (ds.length() > 0) {
				values = ds.getRow(0);
			}

			if (values == null) {
				return this.setNullParam(stmt, ctx);
			}
			Struct struct = handle.createStruct(values, this.sqlObjectType);
			stmt.setObject(this.myPosn, struct, Types.STRUCT);
			return true;
		}
		/*
		 * finally, we have reached an array of struct
		 */
		if (ds == null || ds.length() == 0) {
			return this.setNullParam(stmt, ctx);
		}
		int nbrRows = ds.length();
		Struct[] structs = new Struct[nbrRows];
		for (int i = 0; i < nbrRows; i++) {
			structs[i] = handle.createStruct(ds.getRow(i), this.sqlObjectType);
		}
		Array array = handle.createStructArray(structs, this.sqlArrayType);
		stmt.setArray(this.myPosn, array);
		return true;
	}

	/**
	 * register this parameter for output
	 *
	 * @param stmt
	 * @throws SQLException
	 */
	private void registerForOutput(CallableStatement stmt) throws SQLException {

		/*
		 * array
		 */
		if (this.isArray) {
			stmt.registerOutParameter(this.myPosn, Types.ARRAY, this.sqlArrayType);
			return;
		}
		/*
		 * struct
		 */
		if (this.recordName != null) {
			stmt.registerOutParameter(this.myPosn, Types.STRUCT, this.sqlObjectType);
			return;
		}
		/*
		 * primitive value
		 */
		int scale = this.dataTypeObject.getScale();
		if (scale == 0) {
			stmt.registerOutParameter(this.myPosn, this.getValueType().getSqlType());
			return;
		}

		stmt.registerOutParameter(this.myPosn, this.getValueType().getSqlType(), scale);
		return;
	}

	private boolean setNullParam(CallableStatement stmt, ServiceContext ctx) throws SQLException {
		if (this.isRequired) {
			ctx.addMessage(Messages.VALUE_REQUIRED, this.name);
			return false;
		}
		if (this.isArray) {
			stmt.setNull(this.myPosn, Types.ARRAY, this.sqlArrayType);
		} else {
			stmt.setNull(this.myPosn, Types.STRUCT, this.sqlObjectType);
		}
		return true;
	}

	/**
	 * @return
	 * @throws SQLException
	 */
	private boolean setComplexStruct(ComplexRecord record, CallableStatement stmt, IReadOnlyHandle handle,
			ServiceContext ctx)
			throws SQLException {
		Object obj = ctx.getObject(this.name);
		if (obj == null) {
			return this.setNullParam(stmt, ctx);
		}
		if (this.isArray) {
			if (obj instanceof JSONArray == false) {

				logger.info("Service Context has an object as source for stored procedure " + this.name
						+ " but while we expected it to be an instance of JSONArray (array of objects) it turned out to be "
						+ obj.getClass().getName() + ". Assumed no value for this parameter");

				return this.setNullParam(stmt, ctx);
			}
			Array array = record.createStructArrayForSp((JSONArray) obj, handle, ctx, this.sqlArrayType);
			stmt.setArray(this.myPosn, array);
			return true;
		}
		if (obj instanceof JSONObject == false) {

			logger.info("Service Context has an object as source for stored procedure " + this.name
					+ " but while we expected it to be an instance of JSONObject it turned out to be "
					+ obj.getClass().getName() + ". Assumed no value for this parameter");

			return this.setNullParam(stmt, ctx);
		}
		Struct struct = record.createStructForSp((JSONObject) obj, handle, ctx, this.sqlObjectType);
		stmt.setObject(this.myPosn, struct, Types.STRUCT);
		return true;
	}

	/**
	 * extract output, if required, for this parameter
	 *
	 * @param stmt
	 * @param outputFields
	 *            to which out field is to be extracted into
	 * @param ctx
	 *            Service COntext
	 * @throws SQLException
	 */
	public void extractOutput(CallableStatement stmt, IFieldsCollection outputFields, ServiceContext ctx)
			throws SQLException {
		if (this.inOutType == InputOutputType.INPUT) {
			return;
		}

		/*
		 * simple value
		 */
		if (this.recordName == null && this.isArray == false) {
			Value value = this.getValueType().extractFromSp(stmt, this.myPosn);
			if (Value.isNull(value)) {

				logger.info("Null value received for stored procedure parameter " + this.name
						+ ". Data is not added to context.");

			} else {
				outputFields.setValue(this.name, value);
			}
			return;
		}
		/*
		 * struct/array etc..
		 */
		Object object = stmt.getObject(this.myPosn);
		if (object == null) {

			logger.info(
					"Got null as value of stored procedure parameter " + this.name + ". Data is not added to context.");

			return;
		}

		Object[] array = null;
		if (this.isArray) {
			if (object instanceof Array == false) {
				throw new ApplicationError("procedure parameter " + this.name
						+ " is probably not set properly. We received an object of type " + object.getClass().getName()
						+ " at run time while we expected an Array");
			}
			array = (Object[]) ((Array) object).getArray();
		}
		/*
		 * array of primitives
		 */
		if (this.recordName == null) {
			ctx.putDataSheet(this.name, this.arrayToDs(array));
			return;
		}

		ComplexRecord record = (ComplexRecord) Application.getActiveInstance()
				.getRecord(this.recordName);

		if (this.isArray == false) {
			if (object instanceof Struct == false) {
				throw new ApplicationError("procedure parameter " + this.name
						+ " is probably not set properly. We received an object of type " + object.getClass().getName()
						+ " at run time while we expected Struct");
			}
			Struct struct = (Struct) object;
			if (record.hasNonPrimitiveFields()) {
				ctx.setObject(this.name, this.extractToJson(record, struct, stmt));
			} else {
				ctx.putDataSheet(this.name, this.structToDs(struct));
			}
			return;
		}
		/*
		 * array of structs. the most complex case.
		 */
		if (record.hasNonPrimitiveFields()) {
			ctx.setObject(this.name, this.extractToJsonArray(record, array, stmt));
		} else {
			ctx.putDataSheet(this.name, this.structsToDs(record, array));
		}
	}

	/**
	 * @param struct
	 * @param record
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	private JSONObject extractToJson(ComplexRecord record, Struct struct, CallableStatement stmt)
			throws SQLException {
		JSONWriter writer = new JSONWriter();
		record.toJsonObjectFromStruct(struct, writer);
		return new JSONObject(writer.toString());
	}

	/**
	 * @param array
	 * @param record
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	private JSONArray extractToJsonArray(ComplexRecord record, Object[] array, CallableStatement stmt)
			throws SQLException {
		JSONWriter writer = new JSONWriter();
		record.toJsonArrayFromStruct(array, writer);
		return new JSONArray(writer.toString());
	}

	/**
	 * create a data sheet with the given struct as its only row
	 *
	 * @param struct
	 * @return
	 * @throws SQLException
	 */
	private IDataSheet structToDs(Struct struct) throws SQLException {
		/*
		 * struct is extracted into a data sheet with just one row
		 */
		IDataSheet ds = Application.getActiveInstance().getRecord(this.recordName).createSheet(true, false);
		ValueType[] types = ds.getValueTypes();
		Value[] row = this.getRowFromStruct(struct.getAttributes(), types);
		ds.addRow(row);
		return ds;
	}

	/**
	 * create a data sheet out of an array of structs
	 *
	 * @param array
	 *            that is received from db converted into array of array of
	 *            objects
	 * @return data sheet into which data from the db object is extracted
	 * @throws SQLException
	 */
	private IDataSheet structsToDs(ComplexRecord record, Object[] array) throws SQLException {
		IDataSheet ds = record.createSheet(false, false);
		ValueType[] types = ds.getValueTypes();
		for (Object struct : array) {
			if (struct == null || struct instanceof Struct == false) {

				logger.info("Found an empty row or a non-struct object for stored procedure parameter " + this.name
						+ ". skipping this row, but not throwing an error.");

				continue;
			}
			Object[] rowData = ((Struct) struct).getAttributes();
			Value[] row = this.getRowFromStruct(rowData, types);
			ds.addRow(row);
		}
		return ds;
	}

	/**
	 * convert a struct into a row of a data sheet
	 *
	 * @param struct
	 * @return
	 */
	private Value[] getRowFromStruct(Object[] struct, ValueType[] types) {
		/*
		 * get values from struct as an array of objects
		 */
		Value[] row = new Value[struct.length];
		int col = 0;
		for (Object val : struct) {
			row[col] = types[col].parseObject(val);
			col++;
		}
		return row;
	}

	/**
	 * @param row
	 * @return data sheet with one column
	 * @throws SQLException
	 */
	private IDataSheet arrayToDs(Object[] row) throws SQLException {
		String[] columnNames = { this.name };
		ValueType vt = this.getValueType();
		if (row.length == 0) {
			ValueType[] types = { this.dataTypeObject.getValueType() };
			return new MultiRowsSheet(columnNames, types);
		}
		Value[][] values = { vt.toValues(row) };
		return new MultiRowsSheet(columnNames, values);
	}

	/** @return */
	ValueType getValueType() {
		return this.dataTypeObject.getValueType();
	}

	/**
	 * @param e
	 */
	public void reportError(Exception e) {
		StringBuilder msg = new StringBuilder("Procedure parameter ");
		msg.append(this.name).append(" at number ").append(this.myPosn).append(" has caused an exception.");
		if (this.isArray) {
			msg.append("Verify that this array parameter is defined as type " + this.sqlArrayType)
					.append("which is an array of ");
			if (this.recordName != null) {
				msg.append(this.sqlObjectType);
			} else {
				msg.append(this.getValueType());
			}
		} else if (this.recordName == null) {
			msg.append("Ensure that the db type of this parameter is compatible with our type " + this.getValueType());
		}
		if (this.recordName != null) {
			msg.append("Verify that the fields in record ").append(this.recordName)
					.append(" are of the right type/sequence as compared to the type ").append(this.sqlObjectType)
					.append(" in db.");
		}
		throw new ApplicationError(e, msg.toString());
	}

	/**
	 * validate the loaded attributes of this sub-component
	 *
	 * @param vtx
	 */
	void validate(IValidationContext vtx) {
		ValidationUtil.validateMeta(vtx, this);
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}
}
