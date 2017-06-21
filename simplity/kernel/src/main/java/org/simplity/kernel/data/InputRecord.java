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
package org.simplity.kernel.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.dm.Field;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.service.ServiceMessages;

/**
 * represents a record/row/table that is used as input for a service
 */
public class InputRecord {

	/**
	 * fully qualified name of the record that we are expecting as input. In
	 * very special case, like some utility service that is internally used, we
	 * may skip record. If record is skipped, input is accepted as it is with no
	 * validation. Must be used with care.
	 */
	String recordName;

	/**
	 * it is possible that the client is sending data in an array, but you want
	 * the fields extracted into fields collection
	 */
	boolean extractIntoFields;

	/**
	 * For the purpose of input parsing and validation, assume this record has
	 * only these subset of fields. null if full row is in force.
	 */
	String[] fieldNames = null;

	/**
	 * min rows expected. Used for validating input.
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
	 * name of sheet in which we are expecting data. null if data is not
	 * expected in a sheet
	 */
	String sheetName = null;

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
	String parentSheetName;

	/**
	 * and we need to know how to identify child rows for a parent row. we use
	 * common columns in the two sheets to link them
	 */
	String linkColumnInThisSheet;

	/**
	 * field name in parent sheet that links a parent row to rows in this sheet
	 */
	String linkColumnInParentSheet;

	/**
	 * in case the linking key is a compound key with more than one column.
	 * This is a separate attribute because this is rare, and we want to keep
	 * the common case simple
	 */
	String[] listOfLinkColumnsInThisSheet;
	/**
	 * in case the linking key is a compound key with more than one column.
	 * This is a separate attribute because this is rare, and we want to keep
	 * the common case simple
	 */
	String[] listOfLinkColumnsInParentSheet;
	private Field[] fields = null;
	private boolean hasInterFieldValidations = false;

	/**
	 * if this is a data structure/object structure
	 */
	private boolean isComplexStructure;

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
		this.sheetName = sheetName;
	}

	/**
	 * create input record for a child sheet
	 *
	 * @param recName
	 *
	 * @param sheetName
	 * @param parentSheetName
	 * @param childColName
	 * @param parentColName
	 */
	public InputRecord(String recName, String sheetName, String parentSheetName,
			String childColName, String parentColName) {
		this.recordName = recName;
		this.sheetName = sheetName;
		this.parentSheetName = parentSheetName;
		this.linkColumnInThisSheet = childColName;
		this.linkColumnInParentSheet = parentColName;
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
	public InputRecord(String recName, String sheetName, String parentSheetName,
			String[] childColNames, String[] parentColNames) {
		this.recordName = recName;
		this.sheetName = sheetName;
		this.parentSheetName = parentSheetName;
		this.listOfLinkColumnsInThisSheet = childColNames;
		this.listOfLinkColumnsInParentSheet = parentColNames;
	}

	/**
	 * parse fields of this record from inData into context. Error if any, will
	 * be added to context. We parse all fields irrespective of validation
	 * errors.
	 *
	 * @param json
	 *            from which data is to be extracted
	 * @param ctx
	 *            into which data is extracted
	 */
	public void extractInput(JSONObject json, ServiceContext ctx) {

		if (this.isComplexStructure) {
			this.extractComplexSheet(json, ctx);
			return;
		}
		/*
		 * special case where we are to blindly take whatever is coming in.
		 * this is restricted to utility services, and not for production
		 * service. Hopefully enforced by design reviews :-)
		 */
		if (this.fields == null) {
			JsonUtil.extractWithNoValidation(json, ctx, this.sheetName,
					this.parentSheetName);
			return;
		}

		/*
		 * if sheet name is not specified, we are to extract fields
		 */
		if (this.sheetName == null) {
			this.extractFields(json, ctx);
			return;
		}

		/*
		 * are we to extract to fields even when sheet is specified?
		 */
		if (this.extractIntoFields) {
			this.extractSheetToFields(json, ctx);
			return;
		}

		List<FormattedMessage> errors = new ArrayList<FormattedMessage>();
		/*
		 * use record for validating input
		 */
		DataSheet sheet = null;
		if (this.parentSheetName == null) {
			sheet = this.getSheetFromArray(json, ctx, errors);
		} else {
			sheet = this.getSheetFromParent(json, ctx, errors);
		}
		/*
		 * got trouble?
		 */
		if (errors.isEmpty() == false) {
			ctx.addMessages(errors);
			return;
		}
		/*
		 * sheet is null if no data is found
		 */
		if (sheet == null || sheet.length() == 0) {
			if (this.minRows > 1) {
				ctx.addMessage(ServiceMessages.MIN_INPUT_ROWS, "" + this.minRows, ""
						+ this.maxRows);
			} else {
				Tracer.trace("No data for sheet " + this.sheetName);
			}
			return;
		}

		int nbrRows = sheet.length();
		if (nbrRows < this.minRows || (this.maxRows != 0 && nbrRows > this.maxRows)) {
			if (this.minRows > 0) {
				ctx.addMessage(ServiceMessages.MIN_INPUT_ROWS, "" + this.minRows, ""
						+ this.maxRows);
			} else {
				ctx.addMessage(ServiceMessages.MAX_INPUT_ROWS, "" + this.maxRows, ""
						+ this.maxRows);
			}
			return;
		}
		if (this.hasInterFieldValidations) {
			for (FieldsInterface row : sheet) {
				for (Field field : this.fields) {
					field.validateInterfield(row, errors, this.sheetName);
				}
			}

		}
		ctx.putDataSheet(this.sheetName, sheet);
		Tracer.trace("Datasheet " + this.sheetName + " with " + nbrRows
				+ " rows added to the context.");
	}

	/**
	 *
	 * current design is to keep the json as it is and validate it at
	 * the time of its use
	 *
	 * @param json
	 * @param ctx
	 */
	private void extractComplexSheet(JSONObject json, ServiceContext ctx) {
		Object object = json.opt(this.sheetName);
		if (object != null) {
			ctx.setObject(this.sheetName, object);
			Tracer.trace("Data input for " + this.sheetName + " saved as " + object
					.getClass().getName() + " for later use.");
		} else if (this.minRows > 0) {
			ctx.addMessage(ServiceMessages.MIN_INPUT_ROWS, "" + this.minRows, ""
					+ this.maxRows);

		} else {
			Tracer.trace("No data for sheet " + this.sheetName);
		}
		return;
	}

	private void extractSheetToFields(JSONObject json, ServiceContext ctx) {
		Object object = json.opt(this.sheetName);
		if (object == null) {
			/*
			 * do we have this sheet already in the context?
			 */
			DataSheet ds = ctx.getDataSheet(this.sheetName);
			if (ds != null) {
				/*
				 * we skip validation. Our understanding is that this is indeed
				 * prepared by a server component
				 */
				return;
			}
			/*
			 * let us not give-up, but try at the top itself
			 */
			this.extractFields(json, ctx);
			return;
		}
		/*
		 * if it is array, we get the first element
		 */
		if (object instanceof JSONArray) {
			object = ((JSONArray) object).opt(0);
		}
		if (object instanceof JSONObject) {
			this.extractFields((JSONObject) object, ctx);
			return;
		}

		Tracer.trace(
				"We expected an object, or an array with an objectas its first element, but got "
						+ object + ". This is ignored.");
		/*
		 * let us not give-up, but try at the top itself
		 */
		this.extractFields(json, ctx);
	}

	private DataSheet getSheetFromParent(JSONObject json, ServiceContext ctx,
			List<FormattedMessage> errors) {
		/*
		 * this is a child sheet. parent may or may not be a sheet
		 */
		JSONArray data = json.optJSONArray(this.parentSheetName);
		if (data != null && data.length() > 0) {
			return this.getChildSheet(data, errors);
		}

		/*
		 * no parent sheet. do we have child sheet at least?
		 */
		data = json.optJSONArray(this.sheetName);
		if (data == null) {
			Tracer.trace("No rows for " + this.sheetName);
			return null;
		}

		Object obj = json.opt(this.linkColumnInParentSheet);
		if (obj == null) {
			/*
			 * degenerated into a simple sheet case
			 */
			return this.getSheetFromArray(json, ctx, errors);
		}
		/*
		 * assume a single row of parent in fields collection, and extract all
		 * child rows
		 */
		Field[] inputFields = this.fields;
		if (inputFields == null) {
			inputFields = this.guessFieldsFromChildRows(data, json);
			if (inputFields == null) {
				Tracer.trace("We did not get any non-empty rows for " + this.sheetName
						+ " and hence no rows extracted.");
				return null;
			}
		}
		DataSheet ds = new MultiRowsSheet(inputFields);
		int parentKeyIdx = ds.getColIdx(this.linkColumnInThisSheet);
		this.addRowsFromParent(json, inputFields, parentKeyIdx, ds, errors);
		return ds;
	}

	private DataSheet getChildSheet(JSONArray parentRows, List<FormattedMessage> errors) {
		/*
		 * parentRows corresponds to following json. We are to accumulate child
		 * rows
		 * across all main rows
		 *
		 * [...,"attName"=[{},{}....],..],[....,"attName"=[{},{}.... ],..]....
		 */
		Field[] inputFields = this.fields;
		if (inputFields == null) {
			inputFields = this.guessFieldsFromParentRows(parentRows);
		}
		if (inputFields == null) {
			/*
			 * we do not have any child rows in the parent sheet...
			 */
			return null;
		}
		/*
		 * multiple-keys is handled in a separate method
		 */
		if (this.listOfLinkColumnsInThisSheet != null) {
			return this.getChildSheetWithMultipleLinks(parentRows, inputFields, errors);
		}

		DataSheet ds = new MultiRowsSheet(inputFields);
		/*
		 * we are not sure of getting a valid child row in first element. So,
		 * let us have a flexible strategy
		 */
		int nbrParentRows = parentRows.length();
		/*
		 * col number in child sheet to which we have to copy parent key
		 */
		int parentKeyIdx = ds.getColIdx(this.linkColumnInThisSheet);
		/*
		 * for each parent row
		 */
		for (int parentIdx = 0; parentIdx < nbrParentRows; parentIdx++) {
			JSONObject pr = parentRows.optJSONObject(parentIdx);
			if (pr == null) {
				continue;
			}
			this.addRowsFromParent(pr, inputFields, parentKeyIdx, ds, errors);
		}
		return ds;
	}

	/**
	 * method to extract child rows from parent when the link involves multiple
	 * fields
	 *
	 * @param parentRows
	 * @param inputFields
	 * @param errors
	 * @return data sheet with all child rows, with link fields duly copied from
	 *         parent to child
	 */
	private DataSheet getChildSheetWithMultipleLinks(JSONArray parentRows,
			Field[] inputFields, List<FormattedMessage> errors) {

		DataSheet ds = new MultiRowsSheet(inputFields);

		int nbrParentRows = parentRows.length();
		int nbrFields = inputFields.length;
		boolean allFieldsAreOptional = this.purpose == DataPurpose.SUBSET;
		/*
		 * how do we copy parent key values to child rows? Several ways with
		 * conflicting complexities and efficiencies. However, keeping in mind
		 * that the number of keys is very very rarely rarely more than 2, it
		 * does not really matter. We have chosen to keep them in a map
		 */
		Map<String, Object> keyObjects = new HashMap<String, Object>();
		for (int i = 0; i < nbrParentRows; i++) {
			Object obj = parentRows.opt(i);
			if (obj == null) {
				continue;
			}
			if (obj instanceof JSONObject == false) {
				Tracer.trace("Child row element is expected to be an object but we got "
						+ obj.getClass().getSimpleName() + ". Input ignored.");
				continue;
			}
			JSONObject parentObject = (JSONObject) obj;
			JSONArray rows = parentObject.optJSONArray(this.sheetName);
			if (rows == null) {
				continue;
			}
			for (String key : this.listOfLinkColumnsInParentSheet) {
				keyObjects.put(key, parentObject.opt(key));
			}
			int n = rows.length();
			for (int childIdx = 0; childIdx < n; childIdx++) {
				JSONObject child = rows.optJSONObject(childIdx);
				if (child == null) {
					Tracer.trace("Child element at " + (childIdx + 1)
							+ " is not an object. Row ignored.");
					continue;
				}
				Value[] row = new Value[nbrFields];
				for (int fieldIdx = 0; fieldIdx < inputFields.length; fieldIdx++) {
					Field field = inputFields[i];
					String fieldName = field.getName();
					Object val = keyObjects.get(fieldName);
					if (val == null) {
						val = child.opt(fieldName);
					}
					row[fieldIdx] = field.parseObject(val, errors, allFieldsAreOptional,
							this.sheetName);
				}
				ds.addRow(row);
			}
		}
		return ds;
	}

	/**
	 * adding all child rows from a given parent object
	 *
	 * @param parentObject
	 *            that has the rows for children
	 * @param inputFields
	 *            actual fields to be added, including the parent key field
	 * @param parentKeyIdx
	 *            index to the above array to locate the parentKey
	 * @param ds
	 *            data sheet to which we extract rows into
	 * @param errors
	 *            accumulate any error while parsing data
	 */
	private void addRowsFromParent(JSONObject parentObject, Field[] inputFields,
			int parentKeyIdx, DataSheet ds, List<FormattedMessage> errors) {
		JSONArray rows = parentObject.optJSONArray(this.sheetName);
		if (rows == null) {
			return;
		}
		int n = rows.length();
		Object parentVal = parentObject.opt(this.linkColumnInParentSheet);
		for (int childIdx = 0; childIdx < n; childIdx++) {
			JSONObject obj = rows.optJSONObject(childIdx);
			if (obj == null) {
				continue;
			}
			int fieldIdx = 0;
			Value[] row = new Value[inputFields.length];
			for (Field field : inputFields) {
				Object val = null;
				if (fieldIdx == parentKeyIdx) {
					val = parentVal;
				} else {
					val = obj.opt(field.getName());
				}
				row[fieldIdx] = field.parseObject(val, errors,
						this.purpose == DataPurpose.SUBSET, this.sheetName);
				fieldIdx++;
			}
			ds.addRow(row);
		}
	}

	/**
	 * @param parentRows
	 * @return
	 */
	private Field[] guessFieldsFromParentRows(JSONArray parentRows) {
		/*
		 * go down till we get the first row for this child, and we get all
		 * attributes as fields.
		 *
		 */
		int n = parentRows.length();
		for (int i = 0; i < n; i++) {
			JSONObject pr = parentRows.optJSONObject(0);
			if (pr == null) {
				continue;
			}
			JSONArray children = pr.optJSONArray(this.sheetName);
			if (children == null) {
				continue;
			}
			Field[] f = this.guessFieldsFromChildRows(children, pr);
			if (f != null) {
				return f;
			}
		}
		return null;
	}

	/**
	 * @param parentRows
	 * @return
	 */
	private Field[] guessFieldsFromChildRows(JSONArray childRows,
			JSONObject parentObject) {
		/*
		 * go down till we get the first row for this child, and we get all
		 * attributes as fields.
		 *
		 */
		int nbr = childRows.length();
		Object parentValue = parentObject.opt(this.linkColumnInParentSheet);
		for (int j = 0; j < nbr; j++) {
			JSONObject child = childRows.optJSONObject(j);
			if (child != null) {
				return JsonUtil.getFields(child, this.linkColumnInThisSheet, parentValue);
			}
		}
		return null;
	}

	private DataSheet getSheetFromArray(JSONObject json, ServiceContext ctx,
			List<FormattedMessage> errors) {
		Object object = json.opt(this.sheetName);
		if (object == null) {
			/*
			 * data sheet not recd. We try fields
			 */
			if (this.minRows <= 1) {
				Tracer.trace("No data for sheet " + this.sheetName
						+ ". We try and add fields instead.");
				this.extractFields(json, ctx);
			}
			return null;
		}
		boolean allFieldsAreOptional = this.purpose == DataPurpose.SUBSET;
		if (object instanceof JSONArray) {
			return JsonUtil.getSheet((JSONArray) object, this.fields, errors,
					allFieldsAreOptional, null, null);
		}

		if (object instanceof JSONObject) {
			JSONArray arr = new JSONArray();
			arr.put(object);
			return JsonUtil.getSheet(arr, this.fields, errors, allFieldsAreOptional, null,
					null);
		}
		Tracer.trace("Receieved " + object + " as value for " + this.sheetName
				+ " while we would have been happy with an array or an object. Input ignored");
		return null;

	}

	private void extractFields(JSONObject json, ServiceContext ctx) {
		boolean allFieldsAreOptional = this.purpose == DataPurpose.SUBSET;
		List<FormattedMessage> errors = new ArrayList<FormattedMessage>();
		int nbr = 0;
		if (this.purpose == DataPurpose.FILTER) {
			nbr = JsonUtil.extractFilterFields(json, this.fields, ctx, errors);

		} else {
			nbr = JsonUtil.extractFields(json, this.fields, ctx, errors,
					allFieldsAreOptional);
		}
		Tracer.trace(nbr + " fields into ctx based on record " + this.recordName);

		if (errors.size() == 0 && this.hasInterFieldValidations
				&& allFieldsAreOptional == false && nbr > 1) {
			for (Field field : this.fields) {
				field.validateInterfield(ctx, errors, null);
			}
		}
		if (errors.size() > 0) {
			Tracer.trace(" We got " + errors.size()
					+ " validaiton errors during this record input");
			ctx.addMessages(errors);
		}
		return;
	}

	/**
	 * called once on loading the component
	 */
	public void getReady() {
		if (this.recordName == null) {
			return;
		}
		/**
		 * special feature for utility routines to cheat the xsd and not give
		 * recordName
		 */
		if (this.recordName.charAt(0) == '.') {
			this.recordName = null;
			return;
		}
		Record record = ComponentManager.getRecord(this.recordName);
		if (record.isComplexStruct()) {
			this.isComplexStructure = true;
			return;
		}

		this.fields = record.getFieldsToBeExtracted(this.fieldNames, this.purpose,
				this.saveActionExpected);
		this.hasInterFieldValidations = record.hasInterFieldValidations();
	}

	/**
	 * @param ctx
	 * @return number of errors added
	 */
	int validate(ValidationContext ctx) {
		int count = 0;
		if (this.recordName == null) {
			if (this.sheetName == null) {
				ctx.addError("Input record has no record or sheet specified");
				count++;
			}
			if (this.fieldNames != null) {
				ctx.addError(
						"fieldNames are meant to specify a subset of fields in a record. Since record is not specified, this list is invalid.");
				count++;
			}
		}
		count += ctx.checkRecordExistence(this.recordName, "recordName", false);

		/*
		 * can not have parent sheet if sheet itself is not there
		 */
		if (this.parentSheetName != null) {
			if (this.sheetName == null) {
				ctx.addError(
						"Can not have a parent sheet name without a sheet name for this record");
				count++;
			}
			if (this.linkColumnInParentSheet == null){
				if(this.linkColumnInThisSheet != null){
					ctx.reportUnusualSetting(
							"linkColumnInThisSheet is not relevant unless linkColumnInParentSheet is specified.");
				}
				if(this.listOfLinkColumnsInThisSheet == null){
					ctx.addError(
							"linkColumnInThisSheet or listOfLinkClumnsInParentSheet to be specified when parentSheetNme is specified.");
				}
			}else{
				if(this.linkColumnInThisSheet == null){
					ctx.addError(
							"linkColumnInThisSheet must be specified in tune with linkColumnInParentSheet.");
				}
				if(this.listOfLinkColumnsInThisSheet != null){
					ctx.reportUnusualSetting(
							"listOfLinkColumnsInThisSheet is not relevant when  linkColumnInThisSheet is specified.");
				}
			}
			if(this.linkColumnInThisSheet == null) {
				ctx.addError(
						"linkColumnInParentSheet and linkColumnInThisSheet are required to link this sheet with parent sheet.");
				count++;
			}
			if (this.extractIntoFields) {
				ctx.addError(
						"extractIntoFields is set to true, but parentSheetName is specified. If this is a child it HAS to be a sheet.");
				count++;
			}
		}

		if (this.minRows < 0 || this.maxRows < 0 || this.minRows > this.maxRows) {
			ctx.addError(
					"minRows and maxRows are to be positive and min should not be greater than max.");
			count++;
		}
		if ((this.sheetName == null || this.extractIntoFields) && (this.minRows > 1
				|| this.maxRows > 1)) {
			String msg = "minRows=" + this.minRows + " and maxRows=" + this.maxRows;
			if (this.sheetName == null) {
				msg += " but no sheetName specified.";
			} else {
				msg += " but extractIntoFields is set to true.";
			}
			ctx.addError(msg);
			count++;
		}
		return count;
	}

	/**
	 * @param recName
	 */
	public void setRecordName(String recName) {
		this.recordName = recName;

	}
	/**
	 * @param purpose the purpose to set
	 */
	public void setPurpose(DataPurpose purpose) {
		this.purpose = purpose;
	}

	/**
	 * set saveActionExpected = true
	 */
	public void enableSaveAction(){
		this.saveActionExpected = true;
	}

	/**
	 * @param sheetName
	 */
	public void setSheetName(String sheetName) {
		this.sheetName = sheetName;

	}
}
