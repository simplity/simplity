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
package org.simplity.core.service;

import org.simplity.core.ApplicationError;
import org.simplity.core.FilterCondition;
import org.simplity.core.app.AppConventions;
import org.simplity.core.app.Application;
import org.simplity.core.app.IRequestReader;
import org.simplity.core.app.IRequestReader.InputValueType;
import org.simplity.core.comp.ComponentType;
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationMessage;
import org.simplity.core.comp.ValidationUtil;
import org.simplity.core.data.DataPurpose;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.data.IFieldsCollection;
import org.simplity.core.data.MultiRowsSheet;
import org.simplity.core.dm.Record;
import org.simplity.core.dm.field.ChildRecord;
import org.simplity.core.dm.field.Field;
import org.simplity.core.dm.field.RecordArray;
import org.simplity.core.dm.field.ValueArray;
import org.simplity.core.dt.DataType;
import org.simplity.core.msg.MessageType;
import org.simplity.core.msg.Messages;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;
import org.simplity.json.JSONArray;
import org.simplity.json.JSONFields;
import org.simplity.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * represents a record/row/table that is used as input for a service
 */
public class InputRecord {
	private static final Logger logger = LoggerFactory.getLogger(InputRecord.class);

	/**
	 * name of sheet/object to extract data into. ignored if writeAs=fields
	 */
	String name = null;

	/**
	 * in case the client sends this sheet/object with a different name.
	 * Defaults to name
	 */
	String externalName;
	/**
	 * fully qualified name of the record that we are expecting as input. In
	 * very special case, like some utility service that is internally used, we
	 * may skip record. If record is skipped, input is accepted as it is with no
	 * validation. Must be used with care.
	 */
	@FieldMetaData(isReferenceToComp = true, referredCompType = ComponentType.REC)
	String recordName;

	/**
	 * in what form is the data expected in. SHEET is not a valid value.
	 * getReady() throws an exception in that case
	 */
	DataStructureType readAs = DataStructureType.FIELDS;

	/**
	 * in what form are we to extract this into the context?
	 */
	DataStructureType writeAs = DataStructureType.FIELDS;

	/**
	 * For the purpose of input parsing and validation, assume this record has
	 * only these subset of fields. null if full row is in force.
	 */
	@FieldMetaData(relevantBasedOnField = "recordName")
	String[] fieldNames = null;

	/**
	 * min rows expected. Used for validating input. If this is an object and is
	 * required, set this to 1.
	 */
	int minRows = 0;

	/**
	 * certainly a good idea to limit rows from a practical view.
	 */
	int maxRows = Integer.MAX_VALUE;
	/**
	 * why is this record being input? we extract and validate input based on
	 * this purpose
	 */
	DataPurpose purpose = DataPurpose.OTHERS;

	/**
	 * are we expecting the special field that indicates how to save data?
	 */
	boolean saveActionExpected;

	/**
	 * client may use object paradigm and send data as hierarchical object
	 * structure. We need to extract them into related sheets. Is this sheet
	 * expected as child rows of a parent sheet? Rows for this sheet are
	 * available with an attribute name same as this sheet name
	 */
	@FieldMetaData(alternateField = "name")
	String parentSheetName;

	/**
	 * comma separated field names in this sheet used for linking with the
	 * parent
	 */
	@FieldMetaData(leaderField = "parentSheetName")
	String[] linkFieldsInThisSheet;
	/**
	 * comma separated field names in parent sheet for linking.
	 */
	@FieldMetaData(leaderField = "parentSheetName")
	String[] linkFieldsInParentSheet;

	/**
	 * cached fields
	 */
	private Field[] fields = null;
	/**
	 * derived based on field attributes
	 */
	private boolean hasInterFieldValidations = false;

	/**
	 * if this is a data structure/object structure
	 */
	private boolean isComplexStructure;

	/**
	 * list of child records. Inferred based on parentName in other records
	 */
	private InputRecord[] children;

	/**
	 * default constructor
	 */
	public InputRecord() {
		// default
	}

	/**
	 * create an output record for a data
	 *
	 * @param recName
	 *
	 * @param sheetName
	 */
	public InputRecord(String recName, String sheetName) {
		this.recordName = recName;
		this.name = sheetName;
	}

	/**
	 * create input record for a child sheet with multiple link columns
	 *
	 * @param recName
	 * @param sheetName
	 * @param parentSheetName
	 * @param childColNames
	 * @param parentColNames
	 */
	public InputRecord(String recName, String sheetName, String parentSheetName, String[] childColNames,
			String[] parentColNames) {
		this.recordName = recName;
		this.name = sheetName;
		this.parentSheetName = parentSheetName;
		this.linkFieldsInThisSheet = childColNames;
		this.linkFieldsInParentSheet = parentColNames;
	}

	/**
	 * called once on loading the component
	 */
	public void getReady() {

		if (this.readAs == DataStructureType.SHEET) {
			throw new ApplicationError("Client input can not be a data sheet.");
		}

		if (this.recordName == null) {
			return;
		}
		/*
		 * special feature for utility routines to cheat the xsd and not give
		 * recordName
		 */
		if (this.recordName.equals(".")) {
			this.recordName = null;
			return;
		}
		Record record = Application.getActiveInstance().getRecord(this.recordName);

		this.fields = record.getFieldsToBeInput(this.fieldNames, this.purpose, this.saveActionExpected);
		this.hasInterFieldValidations = record.hasInterFieldValidations();
		this.isComplexStructure = record.hasNonPrimitiveFields();
	}

	/**
	 * add child-sheets to this input record
	 *
	 * @param children
	 */
	public void setChildren(InputRecord[] children) {
		this.children = children;
	}

	void validate(IValidationContext vtx) {
		ValidationUtil.validateMeta(vtx, this);
		if (this.parentSheetName != null) {
			if (this.writeAs != DataStructureType.SHEET) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
						"When parentSheetName is specified, this record must use outputType=sheet.", "writeAs"));
			}
		}

		if (this.minRows < 0) {
			vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
					"minRows has to be positive ", "minRows"));
		}
		if (this.maxRows < 0) {
			vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
					"maxRows has to be positive", "maxRows"));
		}
		if (this.minRows > this.maxRows) {
			vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
					"minRows and maxRows are to be positive and min should not be greater than max.", "minRows"));
		}
		if ((this.readAs == DataStructureType.FIELDS) && (this.minRows > 1 || this.maxRows > 1)) {
			String fieldName;
			String msg = "minRows=" + this.minRows + " and maxRows=" + this.maxRows;
			if (this.name == null) {
				fieldName = "sheetName";
				msg += " but no sheetName specified.";
			} else {
				fieldName = "extractionType";
				msg += " but extractionType is set to fields.";
			}
			vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR, msg, fieldName));
		}
	}

	/**
	 * @param recName
	 */
	public void setRecordName(String recName) {
		this.recordName = recName;

	}

	/**
	 * @param purpose
	 *            the purpose to set
	 */
	public void setPurpose(DataPurpose purpose) {
		this.purpose = purpose;
	}

	/**
	 * set saveActionExpected = true
	 */
	public void enableSaveAction() {
		this.saveActionExpected = true;
	}

	/**
	 * @param sheetName
	 */
	public void setSheetName(String sheetName) {
		this.name = sheetName;

	}

	/**
	 * @param externalName
	 */
	public void setExternalName(String externalName) {
		this.externalName = externalName;

	}

	/**
	 * Read input data as per this record specification
	 *
	 * @param reader
	 *
	 * @param ctx
	 *            into which data is extracted
	 * @return number of fields or number of rows extracted. Validation errors,
	 *         if any, are added to the ctx
	 */
	public int read(IRequestReader reader, ServiceContext ctx) {
		if (this.parentSheetName != null) {
			logger.info(
					"Sheet {} is a child sheet and is read directly. It will be triggered as and when a row from its parent {} is read.",
					this.externalName, this.parentSheetName);
			return 0;
		}
		/*
		 * are we going to get what we are expecting?
		 */
		if (this.readAs != DataStructureType.FIELDS) {
			String nameToUse = this.externalName == null ? this.name : this.externalName;
			InputValueType vt = reader.getValueType(nameToUse);
			if (!this.isVtValid(vt)) {
				logger.error("We expected input to be {} but got {} for ", this.readAs, vt, this.externalName);
				return 0;
			}
		}

		/*
		 * input for this record can be specified as fields, object or array. it
		 * may be output/copied/saved in one of four formats : fields, object,
		 * array or data sheet We go by the format with which it is to be saved,
		 * and handle teh input type in each of those methods
		 */
		int nbrRead = 0; // tracked to check for min-mx
		switch (this.writeAs) {
		case FIELDS:
			/*
			 * min/max rows not applicable to fields. so we exit from here
			 */
			return this.readIntoContext(reader, ctx, ctx);

		case OBJECT:
			JSONObject json = this.readIntoObject(reader, ctx);
			if (json != null) {
				ctx.setObject(this.externalName, json);
				nbrRead = 1;
			}
			break;

		case ARRAY:
			JSONArray arr = this.readIntoArray(reader, ctx);
			if (arr != null) {
				ctx.setObject(this.externalName, arr);
				nbrRead = arr.length();
			}
			break;

		case SHEET:
			IDataSheet sheet = this.readIntoSheet(reader, ctx);
			if (sheet != null) {
				ctx.putDataSheet(this.externalName, sheet);
				nbrRead = sheet.length();
			}
			break;

		default:
			throw new ApplicationError("extractionType " + this.writeAs + " is not yet implmented ");
		}
		if (this.nbrRowsOk(nbrRead, ctx)) {
			return nbrRead;
		}
		return 0;

	}

	private boolean isVtValid(InputValueType vt) {
		switch (this.readAs) {
		case OBJECT:
			return vt == InputValueType.OBJECT || vt == InputValueType.ARRAY_OR_OBJECT;
		case ARRAY:
		case SHEET:
			return vt == InputValueType.ARRAY || vt == InputValueType.ARRAY_OR_OBJECT;
		default:
			return true;
		}
	}

	/**
	 * read as per this record into fields in the context
	 *
	 * @param reader
	 * @param values
	 *            collection into which extracted input fields are to be saved
	 *            into
	 * @param ctx
	 * @return number of fields saved into ctx. 0 if no fields saved.
	 */
	private int readIntoContext(IRequestReader reader, IFieldsCollection values, ServiceContext ctx) {
		switch (this.readAs) {
		case FIELDS:
			return this.readFields(reader, values, ctx);

		case OBJECT:
			reader.openObject(this.externalName);
			int n = this.readFields(reader, values, ctx);
			reader.closeObject();
			return n;

		case ARRAY:
			reader.openArray(this.externalName);
			if (!reader.openObject(0)) {
				logger.error("First element of array {} is to be an object, but it is not.", this.externalName);
				ctx.addMessage(Messages.INVALID_DATA, this.externalName);
			}
			n = this.readFields(reader, values, ctx);
			reader.closeObject();
			reader.closeArray();
			return n;

		default:
			throw new ApplicationError(this.readAs);
		}
	}

	private int readFields(IRequestReader reader, IFieldsCollection values, ServiceContext ctx) {
		int nbr = 0;
		if (this.purpose == DataPurpose.FILTER) {
			nbr = this.readFilterFields(reader, values, ctx);
			logger.info("{} filter fields read", nbr);
			return nbr;
		}

		for (Field field : this.fields) {
			if (field.isPrimitive() == false) {
				logger.info("field {} is skipped as it is non-primitive. It will be read as a sub-structure");
				continue;
			}

			String fieldInuputName = field.getExternalName();
			Object obj = reader.getValue(fieldInuputName);
			Value value = field.parseObject(obj, this.purpose, ctx);
			if (value != null) {
				values.setValue(field.getName(), value);
				nbr++;
			}
		}
		logger.info("{} fields read from inputRecord {} ", nbr, this.name);
		if (ctx.isInError() == false) {
			if (this.hasInterFieldValidations) {
				for (Field field : this.fields) {
					field.validateInterfield(values, null, ctx);
				}
			}
		}

		return nbr;
	}

	private int readFilterFields(IRequestReader reader, IFieldsCollection values, ServiceContext ctx) {
		int nbr = 0;
		for (Field field : this.fields) {
			String fieldName = field.getName();
			String fieldInputName = field.getExternalName();

			Object obj = reader.getValue(fieldInputName);
			if (obj == null) {
				logger.info("no value found for externalName {}", fieldInputName);
				continue;
			}

			/*
			 * what is the comparator
			 */
			FilterCondition f = FilterCondition.Equal;
			String compName = fieldInputName + AppConventions.Name.COMPARATOR_SUFFIX;
			Object otherObj = reader.getValue(compName);
			if (otherObj != null) {
				f = FilterCondition.parse(otherObj.toString());
				if (f == null) {
					logger.error("{} is not a valid filter condition");
					ctx.addValidationMessage(Messages.INVALID_DATA, null, compName, null, 0, otherObj.toString());
					continue;
				}
			}

			/*
			 * handle the special case of in list
			 */
			Value value = null;
			String textValue = obj.toString();

			ValueType valueType = field.getValueType();
			if (FilterCondition.In == f) {
				if (valueType != ValueType.TEXT && valueType != ValueType.INTEGER && valueType != ValueType.DECIMAL) {
					ctx.addValidationMessage(Messages.INVALID_VALUE, null, compName, null, 0,
							" inList condition is valid for numeric and text fields only ");
					continue;
				}
				Value[] vals = Value.parse(textValue.split(","), valueType);
				if (vals == null) {
					ctx.addValidationMessage(Messages.INVALID_VALUE, null, compName, null, 0, textValue);
					continue;
				}
				values.setValue(fieldName, Value.newTextValue(textValue));
				values.setValue(compName, Value.newTextValue(f.name()));
				nbr++;
				continue;
			}

			value = valueType.fromObject(obj);
			if (value == null) {
				logger.error("{} is not valid for field {}", obj, fieldInputName);
				ctx.addValidationMessage(Messages.INVALID_DATA, null, fieldInputName, null, 0, obj.toString());
				continue;
			}

			/*
			 * between requires another value
			 *
			 */
			if (f == FilterCondition.Between) {
				String toName = fieldInputName + AppConventions.Name.TO_FIELD_SUFFIX;
				otherObj = reader.getValue(toName);
				if (otherObj == null) {
					ctx.addValidationMessage(Messages.VALUE_REQUIRED, null, toName, null, 0);
					continue;
				}
				Value toValue = valueType.parseObject(otherObj);
				if (toValue == null) {
					ctx.addValidationMessage(Messages.INVALID_VALUE, null, toName, null, 0, otherObj.toString());
					continue;
				}
				values.setValue(fieldName + AppConventions.Name.TO_FIELD_SUFFIX, toValue);
			}

			values.setValue(fieldName, value);
			values.setValue(fieldName + AppConventions.Name.COMPARATOR_SUFFIX, Value.newTextValue(f.name()));
			nbr++;
		}
		return nbr;
	}

	private JSONObject readIntoObject(IRequestReader reader, ServiceContext ctx) {
		/*
		 * create a JSON object and wrap is as a fieldsCollection to receive
		 * fields that are read
		 */
		JSONObject json = new JSONObject();
		this.readFields(reader, new JSONFields(json), ctx);
		if (this.isComplexStructure) {
			this.readChildObjects(reader, json, ctx);
		}
		return json;
	}

	/**
	 * @param reader
	 * @param ctx
	 * @return
	 */
	private JSONArray readIntoArray(IRequestReader reader, ServiceContext ctx) {
		JSONArray arr = new JSONArray();
		switch (this.readAs) {
		case FIELDS:
			arr.put(this.readIntoObject(reader, ctx));
			return arr;

		case OBJECT:
			reader.openObject(this.externalName);
			arr.put(this.readIntoObject(reader, ctx));
			reader.closeObject();
			return arr;

		case ARRAY:
			reader.openArray(this.externalName);
			int n = reader.getNbrElements();
			for (int i = 0; i < n; i++) {
				if (!reader.openObject(i)) {
					logger.error("Element at {} of array {} is to be an object, but it is not.", i, this.externalName);
					ctx.addMessage(Messages.INVALID_DATA, this.externalName);
				} else {
					arr.put(this.readIntoObject(reader, ctx));
					reader.closeObject();
				}
			}
			reader.closeArray();
			return arr;
		default:
			throw new ApplicationError(this.readAs);
		}

	}

	/**
	 * @param reader
	 * @param ctx
	 * @return
	 */
	private IDataSheet readIntoSheet(IRequestReader reader, ServiceContext ctx) {
		switch (this.readAs) {
		case FIELDS:
			IDataSheet sheet = new MultiRowsSheet(this.fields);
			sheet.appendEmptyRows(1);
			this.readIntoContext(reader, sheet.getRowAsFields(0), ctx);
			return sheet;

		case OBJECT:
			reader.openObject(this.externalName);
			sheet = new MultiRowsSheet(this.fields);
			sheet.appendEmptyRows(1);
			this.readIntoContext(reader, sheet.getRowAsFields(0), ctx);
			reader.closeObject();
			return sheet;

		case ARRAY:
			return this.extractSheet(reader, ctx, null);

		default:
			throw new ApplicationError(this.readAs);
		}
	}

	/**
	 * extract data into a data sheet, after ensuring that an array exists for
	 * this record
	 *
	 * @param reader
	 * @param ctx
	 * @return number of rows
	 */
	private IDataSheet extractSheet(IRequestReader reader, ServiceContext ctx, Value[] parentKeys) {
		reader.openArray(this.externalName);
		try {
			/*
			 * if this is the first time this sheet is being extracted, then we
			 * have to create the data sheet
			 */
			MultiRowsSheet sheet = (MultiRowsSheet) ctx.getDataSheet(this.name);
			if (sheet == null) {
				sheet = new MultiRowsSheet(this.fields);
				ctx.putDataSheet(this.name, sheet);
			}
			int nbrAdded = 0;
			int n = reader.getNbrElements();
			for (int i = 0; i < n; i++) {
				if (reader.openObject(i) == false) {
					this.invalidContent(ctx);
					return sheet;
				}
				try {
					/*
					 * add an empty row, and get that as a fields collection to
					 * read fields into
					 */
					sheet.appendEmptyRows(1);
					IFieldsCollection values = sheet.getRowAsFields(nbrAdded);
					int nbrRead = this.readFields(reader, values, ctx);
					if (nbrRead > 0) {
						nbrAdded++;
						if (this.parentSheetName != null) {
							for (int link = 0; link < this.linkFieldsInParentSheet.length; link++) {
								values.setValue(this.linkFieldsInThisSheet[link], parentKeys[link]);
							}
						}
						/*
						 * trigger child sheets reading, if any
						 */
						if (this.children != null) {
							for (InputRecord child : this.children) {
								InputValueType vt = reader.getValueType(child.externalName);
								if (vt == InputValueType.ARRAY || vt == InputValueType.ARRAY_OR_OBJECT) {
									this.noRows(ctx, vt);
									return sheet;
								}
								Value[] childKeys = new Value[child.linkFieldsInParentSheet.length];
								for (int link = 0; link < child.linkFieldsInParentSheet.length; link++) {
									childKeys[link] = values.getValue(child.linkFieldsInParentSheet[link]);
								}
								child.extractSheet(reader, ctx, childKeys);
							}
						}
					} else {
						sheet.deleteRow(nbrAdded);
					}
				} catch (Exception e) {
					ctx.addMessageRow(Messages.INVALID_FIELD, MessageType.ERROR, "invalid input format",
							this.externalName, null, this.externalName, i + 1);
					return sheet;
				} finally {
					reader.closeObject();
				}
			}
			return sheet;
		} catch (Exception e) {
			ctx.addMessageRow(Messages.INVALID_FIELD, MessageType.ERROR, "invalid input format", this.externalName,
					null, null, 0);
			return null;
		} finally {
			reader.closeArray();
		}
	}

	private boolean nbrRowsOk(int nbr, ServiceContext ctx) {
		if (nbr < this.minRows) {
			ctx.addMessage(ServiceMessages.MIN_INPUT_ROWS, "" + this.minRows, "" + this.maxRows);
		}

		if (this.maxRows == 0) { // no max limit
			return true;
		}
		if (nbr < this.maxRows) {
			ctx.addMessage(ServiceMessages.MAX_INPUT_ROWS, "" + this.minRows, "" + this.maxRows);
			return false;
		}
		return false;
	}

	private int invalidContent(ServiceContext ctx) {
		ctx.addMessage(Messages.INVALID_DATA, this.externalName);
		return 0;
	}

	private int noRows(ServiceContext ctx, InputValueType vt) {
		logger.error("Attribute {} is of type {} while we expected an array.", this.externalName, vt);
		if (this.minRows > 0) {
			ctx.addMessage(Messages.VALUE_REQUIRED, this.externalName);
		}
		return 0;
	}

	private int readChildObjects(IRequestReader reader, JSONObject parentJson, ServiceContext ctx) {
		int nbr = 0;
		for (Field field : this.fields) {
			if (field.isPrimitive()) {
				continue;
			}
			String inuputName = field.getExternalName();
			InputValueType vt = reader.getValueType(inuputName);
			if (field.isRequired() && vt == InputValueType.NULL) {
				ctx.addValidationMessage(Messages.VALUE_REQUIRED, null, inuputName, null, 0, "");
				continue;
			}
			Object val = null;
			if (field instanceof ValueArray) {
				val = this.readValueArray(reader, (ValueArray) field, vt, ctx);
			} else if (field instanceof ChildRecord) {
				val = this.readChildObject(reader, field, vt, ctx);
			} else {
				val = this.readChildArray(reader, (RecordArray) field, vt, ctx);
			}
			if (val != null) {
				parentJson.put(field.getName(), val);
				nbr++;
			}
		}
		return nbr;
	}

	/**
	 * read child object.
	 *
	 * @param reader
	 * @param field
	 * @param vt
	 * @param ctx
	 * @return
	 */
	private Object readChildObject(IRequestReader reader, Field field, InputValueType vt, ServiceContext ctx) {
		if (vt != InputValueType.OBJECT && vt != InputValueType.ARRAY_OR_OBJECT) {
			logger.error("attribute {} is exptected to be an object but we found {}", field.getExternalName(), vt);
			this.invalidContent(ctx);
			return null;
		}
		/*
		 * create an InputRecord to read this child into ctx with a temp name
		 */
		String tempName = "_temp_object_";
		InputRecord child = new InputRecord(field.getReferredRecord(), tempName);
		child.externalName = field.getExternalName();
		child.readAs = DataStructureType.OBJECT;
		child.getReady();
		child.read(reader, ctx);
		/*
		 * let us pick it up from ctx
		 */
		return ctx.removeObject(tempName);
	}

	/**
	 * @param reader
	 * @param field
	 * @param vt
	 * @param ctx
	 * @return
	 */
	private Object readChildArray(IRequestReader reader, RecordArray field, InputValueType vt, ServiceContext ctx) {
		if (vt != InputValueType.ARRAY && vt != InputValueType.ARRAY_OR_OBJECT) {
			logger.error("attribute {} is exptected to be an array but we found {}", field.getExternalName(), vt);
			this.invalidContent(ctx);
			return null;
		}
		/*
		 * create an InputRecord to read this child into ctx with a temp name
		 */
		String tempName = "_temp_array_";
		InputRecord child = new InputRecord(field.getReferredRecord(), tempName);
		child.externalName = field.getExternalName();
		child.writeAs = DataStructureType.ARRAY;
		child.getReady();
		child.read(reader, ctx);
		/*
		 * let us pick it up from ctx
		 */
		return ctx.removeObject(tempName);
	}

	/**
	 * array can be a text with comma separated values, or a json array with
	 * value as elements
	 *
	 * @param reader
	 * @param ctx
	 * @return
	 */
	private JSONArray readValueArray(IRequestReader reader, ValueArray field, InputValueType vt, ServiceContext ctx) {
		String[] texts = null;
		if (vt == InputValueType.VALUE) {
			String text = reader.getValue(field.getExternalName()).toString();
			texts = text.split(",");
		} else if (vt == InputValueType.ARRAY || vt == InputValueType.ARRAY_OR_OBJECT) {
			reader.openArray(field.getExternalName());
			try {
				int nbr = reader.getNbrElements();
				texts = new String[nbr];
				for (int i = 0; i < nbr; i++) {
					texts[i] = reader.getValue(i).toString();
				}
			} catch (Exception e) {
				this.invalidContent(ctx);
				return null;
			} finally {
				reader.closeArray();
			}
		} else {
			logger.error(
					"{} is an array that should either be a comma separated list of values, or an array of values. We found {}",
					field.getExternalName(), vt);
			this.invalidContent(ctx);
			return null;
		}

		JSONArray arr = new JSONArray();
		Value[] values = Value.parse(texts, field.getValueType());
		if (values == null) {
			this.invalidContent(ctx);
			return null;
		}

		DataType dt = field.getDataType();
		for (Value value : values) {
			value = dt.validateValue(value);
			if (value == null) {
				this.invalidContent(ctx);
				return null;
			}
			arr.put(value);
		}
		return arr;
	}

	/**
	 * called by InputData before starting the reading process to facilitate
	 * hierarchical sheets
	 *
	 * @param ctx
	 */
	void addEmptySheet(ServiceContext ctx) {
		if (this.writeAs == DataStructureType.SHEET && this.fields != null) {
			ctx.putDataSheet(this.name, new MultiRowsSheet(this.fields));
		}
	}

	/**
	 * copy data from one context to other for this input record specification
	 *
	 * @param fromCtx
	 * @param toCtx
	 */
	public void copy(ServiceContext fromCtx, ServiceContext toCtx) {
		if (this.name == null) {
			for (Field field : this.fields) {
				String fieldName = field.getName();
				Value val = fromCtx.getValue(fieldName);
				if (Value.isNull(val) == false) {
					toCtx.setValue(fieldName, val);
				}
			}
			return;
		}
		switch (this.writeAs) {
		case OBJECT:
		case ARRAY:
			Object obj = fromCtx.getObject(this.name);
			if (obj != null) {
				toCtx.setObject(this.name, obj);
			}
			return;

		case SHEET:
			IDataSheet sheet = fromCtx.getDataSheet(this.name);
			if (sheet != null) {
				toCtx.putDataSheet(this.name, sheet);
			}
			return;
		default:
			logger.warn("data for record {} not copied because it has sheet name, but extraction type is {}", this.name,
					this.writeAs);
		}
	}

	/**
	 * return an output record meant for list service
	 *
	 * @param recordName
	 *
	 * @param sheetName
	 * @param purpose
	 *            purpose of inputting data
	 * @param asSheet
	 *            is input expected in a sheet/object or is it part of root
	 *            object fields
	 * @return an output record meant for list service
	 */
	public static InputRecord getInputRecord(String recordName, String sheetName, DataPurpose purpose,
			boolean asSheet) {
		InputRecord rec = new InputRecord();
		rec.recordName = recordName;
		rec.name = rec.externalName = sheetName;
		rec.purpose = purpose;
		if (purpose == DataPurpose.SAVE) {
			rec.saveActionExpected = true;
		}
		if (asSheet) {
			rec.readAs = DataStructureType.ARRAY;
			rec.writeAs = DataStructureType.SHEET;
		} else {
			rec.readAs = DataStructureType.FIELDS;
			rec.writeAs = DataStructureType.FIELDS;
		}
		return rec;
	}
}
