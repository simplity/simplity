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
package org.simplity.core.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.simplity.core.ApplicationError;
import org.simplity.core.app.AppConventions;
import org.simplity.core.app.IResponseWriter;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationMessage;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.dm.field.Field;
import org.simplity.core.msg.FormattedMessage;
import org.simplity.core.util.TextUtil;
import org.simplity.core.value.Value;
import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that specifies what inputs are expected
 *
 * @author simplity.org
 *
 */

public class OutputData {
	private static final Logger logger = LoggerFactory.getLogger(OutputData.class);

	static final String EMPTY_RESPONSE = "{\"" + AppConventions.Name.REQUEST_STATUS + "\":\""
			+ AppConventions.Value.Status.OK
			+ "\"}";

	/**
	 * create an output for a demo service
	 *
	 * @param outputObjectName
	 *            name in which the output json is saved in
	 * @param itIsAnArray
	 * @return an instance that expects a json in a field name to be sent to
	 *         client as response.
	 */
	public static OutputData getDemoSpec(String outputObjectName, boolean itIsAnArray) {
		OutputData result = new OutputData();
		result.outputIsAnArray = itIsAnArray;
		result.readyOutputObjectName = outputObjectName;
		return result;
	}

	/**
	 * name with which the output is readily made available in the context
	 */
	String readyOutputObjectName;
	/**
	 * special case when we output an array, and not an object. When this is set
	 * to true, there should be just one record specified with output, with no
	 * externalNam.
	 */
	boolean outputIsAnArray;

	/**
	 * comma separated list of fields to be output. Use this feature for fields
	 * that have the same internal and external names
	 */
	String[] fieldNames;

	/**
	 * if the output names are different from internal name, we can use
	 * OutputField array that allows this feature.
	 */
	OutputField[] outputFields;
	/**
	 * comma separated list of arrays. Values for arrays are in data sheet with
	 * a single column
	 */
	String[] arrayNames;
	/**
	 * sheets/fields to be output based on record definitions
	 */
	OutputRecord[] outputRecords;

	/**
	 * comma separated data sheets to be output
	 */
	String[] dataSheets;

	/**
	 * comma separated list of objects to be output
	 */
	String[] objectNames;

	/**
	 * comma separated list of field names that carry key to attachments. these
	 * are processed as per attachmentManagement, and revised key is replaced as
	 * the field-value
	 */
	String[] attachmentFields;
	/**
	 * comma separated list of column names in the form
	 * sheetName.columnName,sheetName1.columnName2....
	 */
	String[] attachmentColumns;

	/**
	 * @param outputRecords
	 *            the outputRecords to set
	 */
	public void setOutputRecords(OutputRecord[] outputRecords) {
		this.outputRecords = outputRecords;
	}

	/**
	 * get ready for a long-haul service :-)
	 */
	public void getReady() {
		if (this.outputRecords == null) {
			return;
		}

		Set<String> parents = new HashSet<>();
		if (this.outputRecords != null) {
			for (OutputRecord rec : this.outputRecords) {
				rec.getReady();
				String parentSheetName = rec.parentSheetName;
				if (parentSheetName != null) {
					parents.add(parentSheetName);
				}
			}
		}

		if (parents.size() == 0) {
			return;
		}
		/*
		 * OK. we have to deal with hierarchical data output
		 */
		List<OutputRecord> children = new ArrayList<>();
		OutputRecord[] empty = new OutputRecord[0];
		for (String parentSheetName : parents) {
			children.clear();
			OutputRecord parent = null;
			for (OutputRecord rec : this.outputRecords) {
				if (parentSheetName.equals(rec.name)) {
					parent = rec;
				} else if (parentSheetName.equals(rec.parentSheetName)) {
					children.add(rec);
				}
			}
			if (parent == null) {
				throw new ApplicationError("Parent sheet name" + parentSheetName + " is missing from output records");
			}
			parent.setChildren(children.toArray(empty));
		}
	}

	/**
	 * validate this specification
	 *
	 * @param vtx
	 */
	public void validate(IValidationContext vtx) {
		/*
		 * duplicate field names
		 */
		if (this.fieldNames != null && this.fieldNames.length > 0) {
			Set<String> keys = new HashSet<String>();
			for (String key : this.fieldNames) {
				if (keys.add(key) == false) {
					vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
							key + " is a duplicate field name for output.", "fieldNames"));
				}
			}
		}
		/*
		 * duplicate data sheets?
		 */
		if (this.dataSheets != null && this.dataSheets.length > 0) {
			Set<String> keys = new HashSet<String>();
			for (String key : this.dataSheets) {
				if (keys.add(key) == false) {
					vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
							key + " is a duplicate sheet name for output.", "dataSheets"));
				}
			}
		}

		if (this.outputRecords == null) {
			return;
		}

		/*
		 * validate output records, and also keep sheet-record mapping for other
		 * validations.
		 */
		Map<String, OutputRecord> allSheets = new HashMap<String, OutputRecord>();
		int nbrParents = 0;
		for (OutputRecord rec : this.outputRecords) {
			rec.validate(vtx);
			allSheets.put(rec.name, rec);
			if (rec.parentSheetName != null) {
				nbrParents++;
			}
		}

		if (nbrParents == 0) {
			return;
		}
		/*
		 * any infinite loops with cyclical relationships?
		 */
		for (OutputRecord rec : this.outputRecords) {
			if (rec.parentSheetName != null) {
				this.validateParent(rec, allSheets, vtx);
			}
		}
	}

	private void validateParent(OutputRecord outRec, Map<String, OutputRecord> allSheets, IValidationContext vtx) {
		/*
		 * check for existence of parent, as well
		 */
		Set<String> parents = new HashSet<String>();
		String sheet = outRec.name;
		String parent = outRec.parentSheetName;
		while (true) {
			OutputRecord rec = allSheets.get(parent);
			/*
			 * do we have the parent?
			 */
			if (rec == null) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR, "output sheet " + sheet
						+ " uses parentSheetName=" + parent
						+ " but that sheet name is not used in any outputRecord. Note that all sheets that aprticipate iin parent-child relationship must be defined using outputRecord elements.",
						"parentSheetName"));
				return;
			}
			/*
			 * are we cycling in a circle?
			 */
			if (parents.add(parent) == false) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
						"output record with sheetName=" + sheet + " has its parentSheetName set to " + parent
								+ ". This is creating a cyclical child-parent relationship.",
						"sheetName"));
				return;
			}
			/*
			 * is the chain over?
			 */
			if (rec.parentSheetName == null) {
				/*
				 * we are fine with this outoutRecord
				 */
				return;
			}
			sheet = rec.name;
			parent = rec.parentSheetName;
		}
	}

	/**
	 * check whether fieldNames of this specification clashes with fields from a
	 * record
	 *
	 * @param fields
	 * @return
	 */
	boolean okToOutputFieldsFromRecord(Field[] fields) {
		if (this.fieldNames == null || this.fieldNames.length == 0 || fields == null || fields.length == 0) {
			return true;
		}

		Set<String> allNames = new HashSet<String>(this.fieldNames.length);
		for (String aName : this.fieldNames) {
			allNames.add(aName);
		}
		for (Field field : fields) {
			if (allNames.contains(field.getName())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * set response and session parameters
	 *
	 * @param writer
	 *
	 * @param ctx
	 */
	public void write(IResponseWriter writer, ServiceContext ctx) {
		if (this.readyOutputObjectName != null) {
			this.outputObject(writer, ctx);
			return;
		}
		this.prepareForOutput(ctx);
		/*
		 * extract attachments if required
		 */
		if (this.attachmentFields != null) {
			InputData.storeFieldAttaches(this.attachmentFields, ctx, false);
		}

		if (this.attachmentColumns != null) {
			InputData.storeColumnAttaches(this.attachmentColumns, ctx, false);
		}

		/*
		 * messages
		 */

		List<FormattedMessage> msgs = ctx.getMessages();
		if (msgs.isEmpty() == false) {
			FormattedMessage[] messages = msgs.toArray(new FormattedMessage[0]);
			IDataSheet sheet = FormattedMessage.toDataSheet(messages);
			OutputRecord.writeSheet(writer, AppConventions.Name.MESSAGES, sheet);
		}

		if (this.fieldNames != null) {
			this.writeFields(writer, ctx);
		}

		if (this.outputFields != null) {
			for (OutputField field : this.outputFields) {
				field.write(writer, ctx);
			}
		}

		if (this.dataSheets != null) {
			this.writeSheets(writer, ctx);
		}

		if (this.arrayNames != null) {
			this.writeArrays(writer, ctx);
		}

		if (this.objectNames != null) {
			this.writeObjects(writer, ctx);
		}

		if (this.outputRecords != null) {
			for (OutputRecord rec : this.outputRecords) {
				rec.write(writer, ctx);
			}
		}
	}

	/**
	 * @param ctx
	 */
	private void outputObject(IResponseWriter writer, ServiceContext ctx) {
		Object obj = ctx.getObject(this.readyOutputObjectName);
		if (obj == null) {
			logger.error("Output object named {} is missing in the cotext", this.readyOutputObjectName);
			return;
		}
		logger.info("output object is of type {}", obj.getClass().getName());
		if (obj instanceof JSONObject) {
			JSONObject json = (JSONObject) obj;
			for (String key : json.keySet()) {
				logger.info("{} = {}", key, json.opt(key).getClass().getName());
			}
			if (this.outputIsAnArray) {
				writer.addToArray(json);
				return;
			}
			for (String key : json.keySet()) {
				logger.info("Going to set {} = {}", key, json.opt(key).getClass().getName());
				writer.setObject(key, json.opt(key));
				logger.info("Done writing value for key {} ", key);
			}
			logger.info("done with outputting JSON");
			return;
		}
		if (obj instanceof JSONArray) {
			if (!this.outputIsAnArray) {
				writer.setField("value", obj);
				return;
			}
			JSONArray json = (JSONArray) obj;
			int nbr = json.length();
			for (int i = 0; i < nbr; i++) {
				writer.addToArray(json.opt(i));
			}
			return;
		}
	}

	private void writeFields(IResponseWriter writer, ServiceContext ctx) {
		for (String fieldName : this.fieldNames) {
			fieldName = TextUtil.getFieldValue(ctx, fieldName).toText();
			Value value = ctx.getValue(fieldName);
			if (value != null) {
				if (value.isUnknown() == false) {
					writer.setField(fieldName, value);
				}
				continue;
			}
			Object obj = ctx.getObject(fieldName);
			if (obj != null) {
				writer.setObject(fieldName, obj);
			}
		}
	}

	private void writeSheets(IResponseWriter writer, ServiceContext ctx) {
		for (String sheetName : this.dataSheets) {
			sheetName = TextUtil.getFieldValue(ctx, sheetName).toString();
			IDataSheet sheet = ctx.getDataSheet(sheetName);
			if (sheet == null) {
				logger.info("Service context has no sheet with name {} for output.", sheetName);
			} else {
				OutputRecord.writeSheet(writer, sheetName, sheet);
			}
		}
	}

	private void writeArrays(IResponseWriter writer, ServiceContext ctx) {
		for (String arrayName : this.arrayNames) {
			IDataSheet sheet = ctx.getDataSheet(arrayName);
			if (sheet != null) {
				writer.setArray(arrayName, sheet);
				continue;
			}

			Value value = ctx.getValue(arrayName);
			if (value != null) {
				Object[] arr = { value };
				writer.setArray(arrayName, arr);
			} else {
				logger.info("Service context has no sheet with name " + arrayName + " for output.");
			}

		}
	}

	private void writeObjects(IResponseWriter writer, ServiceContext ctx) {
		for (String objectName : this.objectNames) {
			Object obj = ctx.getObject(objectName);
			if (obj != null) {
				writer.setObject(objectName, obj);
				continue;
			}
		}
	}

	/**
	 * set output fields
	 *
	 * @param fields
	 */

	public void setOutputFields(String[] fields) {
		this.fieldNames = fields;
	}

	/**
	 * prepare data, possibly for writer to pull
	 *
	 * @param ctx
	 */
	public void prepareForOutput(ServiceContext ctx) {
		/*
		 * there may be hierarchical data
		 */
		if (this.outputRecords != null) {
			for (OutputRecord rec : this.outputRecords) {
				rec.getReadyForOutput(ctx);
			}
		}
	}

	/**
	 * when the service is being executed in the same jvm, then data is copied
	 * using this spec
	 *
	 * @param fromCtx
	 * @param toCtx
	 */
	public void copy(ServiceContext fromCtx, ServiceContext toCtx) {

		if (this.fieldNames != null) {
			for (String fieldName : this.fieldNames) {
				Value value = fromCtx.getValue(fieldName);
				if (Value.isNull(value) == false) {
					toCtx.setValue(fieldName, value);
				}
			}
		}

		if (this.dataSheets != null) {
			for (String sheetName : this.dataSheets) {
				IDataSheet sheet = fromCtx.getDataSheet(sheetName);
				if (sheet != null) {
					toCtx.putDataSheet(sheetName, sheet);
				}
			}
		}

		if (this.arrayNames != null) {
			for (String sheetName : this.arrayNames) {
				IDataSheet sheet = fromCtx.getDataSheet(sheetName);
				if (sheet != null) {
					toCtx.putDataSheet(sheetName, sheet);
				}
			}
		}

		if (this.outputRecords != null) {
			for (OutputRecord rec : this.outputRecords) {
				rec.copy(fromCtx, toCtx);
			}
		}

	}

	/**
	 * @return true if out put is an array. false if it is an object
	 */
	public boolean outputIsAnArray() {
		return this.outputIsAnArray;
	}

}
