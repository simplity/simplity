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

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Struct;

import org.simplity.core.ApplicationError;
import org.simplity.core.app.Application;
import org.simplity.core.data.DataPurpose;
import org.simplity.core.dm.field.ChildRecord;
import org.simplity.core.dm.field.Field;
import org.simplity.core.dm.field.RecordArray;
import org.simplity.core.dm.field.ValueArray;
import org.simplity.core.idb.IReadOnlyHandle;
import org.simplity.core.msg.FormattedMessage;
import org.simplity.core.msg.Messages;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.util.JsonUtil;
import org.simplity.core.value.Value;
import org.simplity.json.JSONArray;
import org.simplity.json.JSONException;
import org.simplity.json.JSONObject;
import org.simplity.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author simplity.org
 *
 */
public class ComplexRecord extends Record {
	private static final Logger logger = LoggerFactory.getLogger(ComplexRecord.class);
	/**
	 * If this record represents a data structure corresponding to an object
	 * defined in the RDBMS, what is the Object name in the sql. This is used
	 * while handling stored procedure parameters that pass objects and array of
	 * objects
	 */
	String sqlStructName;

	/**
	 * Create an array of struct from json that is suitable to be used as a
	 * stored procedure parameter
	 *
	 * @param array
	 * @param handle
	 * @param ctx
	 * @param sqlTypeName
	 * @return Array object suitable to be assigned to the callable statement
	 * @throws SQLException
	 */
	public Array createStructArrayForSp(JSONArray array, IReadOnlyHandle handle, ServiceContext ctx,
			String sqlTypeName)
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
			structs[i] = this.createStructForSp((JSONObject) childObject, handle, ctx, null);
		}
		return handle.createStructArray(structs, sqlTypeName);
	}

	/**
	 * extract data as per data structure from json
	 *
	 * @param json
	 * @param ctx
	 * @param handle
	 * @param sqlTypeName
	 * @return a struct that can be set as parameter to a stored procedure
	 *         parameter
	 * @throws SQLException
	 */
	public Struct createStructForSp(JSONObject json, IReadOnlyHandle handle, ServiceContext ctx, String sqlTypeName)
			throws SQLException {
		int nbrFields = this.fields.length;
		Object[] data = new Object[nbrFields];
		for (int i = 0; i < this.fields.length; i++) {
			Field field = this.fields[i];
			Object obj = json.opt(field.getName());
			if (obj == null) {
				logger.info("No value for attribute " + field.getName());
				continue;
			}
			/*
			 * array of values
			 */
			if (field instanceof ValueArray) {
				if (obj instanceof JSONArray == false) {
					ctx.addMessage(new FormattedMessage(Messages.INVALID_DATA,
							"Input value for parameter. " + field.getName()
									+ " is expected to be an array of values."));
					continue;
				}
				Value[] arr = field.parseArray(JsonUtil.toObjectArray((JSONArray) obj), this.name, ctx);
				data[i] = handle.createArray(arr, ((ValueArray) field).getSqlTypeName());
				continue;
			}

			/*
			 * struct (record or object)
			 */
			if (field instanceof ChildRecord) {
				if (obj instanceof JSONObject == false) {
					ctx.addMessage(new FormattedMessage(Messages.INVALID_DATA,
							"Input value for parameter. " + field.getName() + " is expected to be an objects."));
					continue;
				}
				ComplexRecord childRecord = (ComplexRecord) Application.getActiveInstance()
						.getRecord(field.getReferredRecord());
				data[i] = childRecord.createStructForSp((JSONObject) obj, handle, ctx,
						((ChildRecord) field).getSqlTypeName());
				continue;
			}

			/*
			 * array of struct
			 */
			if (field instanceof RecordArray) {
				if (obj instanceof JSONArray == false) {
					ctx.addMessage(new FormattedMessage(Messages.INVALID_DATA,
							"Input value for parameter. " + field.getName()
									+ " is expected to be an array of objects."));
					continue;
				}
				ComplexRecord childRecord = (ComplexRecord) Application.getActiveInstance()
						.getRecord(field.getReferredRecord());
				data[i] = childRecord.createStructArrayForSp((JSONArray) obj, handle, ctx,
						((RecordArray) field).getSqlTypeName());
				continue;
			}
			/*
			 * simple value
			 */
			Value value = field.parseObject(obj, DataPurpose.OTHERS, ctx);
			if (value != null) {
				data[i] = value.toObject();
			}
		}
		String nameToUse = sqlTypeName;
		if (nameToUse == null) {
			nameToUse = this.sqlStructName;
		}
		return handle.createStruct(data, nameToUse);
	}

	/**
	 * Create a json array from an object returned from an RDBMS
	 *
	 * @param data
	 *            as returned from jdbc handle
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
	 *            as returned from jdbc handle
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
				json.put(field.getName(), (Object) null);
				continue;
			}
			/*
			 * array of values
			 */
			if (field instanceof ValueArray) {
				if (obj instanceof Object[] == false) {
					throw this.getAppError(-1, field, " is an array of primitives ", obj);
				}
				json.put(field.getName(), obj);
				continue;
			}

			/*
			 * struct (record or object)
			 */
			if (field instanceof ChildRecord) {
				if (obj instanceof Object[] == false) {
					throw this.getAppError(-1, field, " is a record that expects an array of objects ", obj);
				}
				ComplexRecord childRecord = (ComplexRecord) Application.getActiveInstance()
						.getRecord(field.getReferredRecord());
				json.put(field.getName(), childRecord.toJsonObject((Object[]) obj));
				continue;
			}

			/*
			 * array of struct
			 */
			if (field instanceof RecordArray) {
				if (obj instanceof Object[][] == false) {
					throw this.getAppError(-1, field, " is an array record that expects an array of array of objects ",
							obj);
				}
				ComplexRecord childRecord = (ComplexRecord) Application.getActiveInstance()
						.getRecord(field.getReferredRecord());
				json.put(field.getName(), childRecord.toJsonArray((Object[][]) obj));
				continue;
			}
			/*
			 * simple value
			 */
			json.put(field.getName(), obj);
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
			sbf.append("Field ").append(field.getName()).append(txt).append(" but we got an instance of")
					.append(value.getClass().getName());
		}
		return new ApplicationError(sbf.toString());
	}

	/**
	 * Write an object to writer that represents a JOSONObject for this record
	 *
	 * @param array
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
			writer.key(field.getName());
			if (obj == null) {
				writer.value(null);
				continue;
			}
			/*
			 * array of values
			 */
			if (field instanceof ValueArray) {
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
			if (field instanceof ChildRecord) {
				if (obj instanceof Struct == false) {
					throw this.getAppError(-1, field, " is an array of records ", obj);
				}
				ComplexRecord childRecord = (ComplexRecord) Application.getActiveInstance()
						.getRecord(field.getReferredRecord());
				childRecord.toJsonObjectFromStruct((Struct) obj, writer);
				continue;
			}

			/*
			 * array of struct
			 */
			if (field instanceof RecordArray) {
				if (obj instanceof Array == false) {
					throw new ApplicationError("Error while creating JSON from output of stored procedure. Field "
							+ field.getName()
							+ " is an of record for which we expect an array of object arrays. But we got "
							+ obj.getClass().getName());
				}
				ComplexRecord childRecord = (ComplexRecord) Application.getActiveInstance()
						.getRecord(field.getReferredRecord());
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

}
