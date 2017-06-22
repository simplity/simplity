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
package org.simplity.kernel.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.simplity.json.JSONWriter;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.MessageType;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.dm.Field;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.kernel.util.TextUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.PayloadType;
import org.simplity.service.ResponseWriter;
import org.simplity.service.ServiceContext;
import org.simplity.service.ServiceData;
import org.simplity.service.ServiceProtocol;

/**
 * Component that specifies what inputs are expected
 *
 * @author simplity.org
 *
 */

public class OutputData {

	static final String EMPTY_RESPONSE = "{\"" + ServiceProtocol.REQUEST_STATUS + "\":\"" + ServiceProtocol.STATUS_OK
			+ "\"}";
	/**
	 * no need to extract data for response. This field has the response text
	 * ready
	 */
	String responseTextFieldName;
	boolean justOutputEveryThing;
	/**
	 * get response from the writer in service context
	 */
	boolean outputFromWriter;
	/**
	 * comma separated list of fields to be output.
	 */
	String[] fieldNames;

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
	 * if this service wants to set/reset some session fields. Note that this
	 * directive is independent of fieldNames or outputRecords. That is if a is
	 * set as sessionFields, it is not sent to client, unless "a" is also
	 * specified as fieldNames
	 */
	String[] sessionFields;

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
	 * set response and session parameters
	 *
	 * @param ctx
	 * @param outData
	 * @param payloadType
	 */
	public void setResponse(ServiceContext ctx, ServiceData outData, PayloadType payloadType) {
		if (this.outputFromWriter) {
			/*
			 * payload is ready in the writer
			 */
			Tracer.trace("Picking up response from writer");
			ResponseWriter writer = ctx.getWriter();
			/*
			 * add status
			 */
			writer.key(ServiceProtocol.REQUEST_STATUS).value(ServiceProtocol.STATUS_OK);

			outData.setPayLoad(writer.getResponse());
			return;
		}
		if (this.responseTextFieldName != null) {
			/*
			 * service is supposed to have kept response ready for us
			 */
			Object obj = ctx.getObject(this.responseTextFieldName);
			if (obj == null) {
				obj = ctx.getValue(this.responseTextFieldName);
			}
			if (obj == null) {
				Tracer.trace("We expected a ready response in service context with name " + this.responseTextFieldName
						+ " . We are sorry that we could not locate it, and we are sending an empty response.");
			} else {
				outData.setPayLoad(obj.toString());
			}
			return;
		}

		/*
		 * extract attachments if required
		 */
		if (this.attachmentFields != null) {
			InputData.storeFieldAttaches(this.attachmentFields, ctx, false);
		}

		if (this.attachmentColumns != null) {
			InputData.storeColumnAttaches(this.attachmentColumns, ctx, false);
		}
		/**
		 * session data if any
		 */
		if (this.sessionFields != null) {
			for (String f : this.sessionFields) {
				Object val = ctx.getValue(f);
				if (val == null) {
					val = ctx.getObject(f);
				}
				/*
				 * we may set null to remove existing values
				 */
				outData.setSessionField(f, val);
			}
		}
		if (payloadType == PayloadType.NONE) {
			this.prepareOutData(outData, ctx);
		} else {
			outData.setPayLoad(this.getJsonPayload(ctx));
		}
	}

	private String getJsonPayload(ServiceContext ctx) {
		JSONWriter writer = new JSONWriter();
		writer.object();
		if (this.justOutputEveryThing) {
			this.allDataToJson(writer, ctx);
		} else {
			/*
			 * we have to attach a pay load of the right type.
			 * as of now, we know only about json
			 */
			this.dataToJson(writer, ctx);
		}
		/*
		 * we also push non-error messages
		 */
		writer.key(ServiceProtocol.MESSAGES).array();
		for (FormattedMessage msg : ctx.getMessages()) {
			if (msg.messageType != MessageType.ERROR) {
				msg.writeJsonValue(writer);
			}
		}
		writer.endArray();

		writer.endObject();
		return writer.toString();
	}

	private void prepareOutData(ServiceData outData, ServiceContext ctx) {
		if (this.justOutputEveryThing) {
			this.copyAllToOutData(outData, ctx);
			return;
		}
		if (this.fieldNames != null) {
			for (String fieldName : this.fieldNames) {
				outData.put(fieldName, ctx.getValue(fieldName));
			}
		}

		if (this.dataSheets != null) {
			for (String sheetName : this.dataSheets) {
				DataSheet sheet = ctx.getDataSheet(sheetName);
				if (sheet == null) {
					Tracer.trace("Service context has no sheet with name " + sheetName + " for output.");
				} else {
					outData.put(sheetName, ctx.getDataSheet(sheetName));
				}
			}
		}
		if (this.outputRecords != null) {
			for (OutputRecord rec : this.outputRecords) {
				outData.put(rec.sheetName, ctx.getDataSheet(rec.sheetName));
			}
		}
		if (this.arrayNames != null) {
			for (String arrayName : this.arrayNames) {
				DataSheet sheet = ctx.getDataSheet(arrayName);
				if (sheet == null) {
					Value value = ctx.getValue(arrayName);
					if (value == null) {
						Tracer.trace("Service context has no sheet with name " + arrayName + " for output.");
						continue;
					}
					outData.put(arrayName, ctx.getValue(arrayName));
				} else {
					outData.put(arrayName, sheet);
				}
			}
		}
	}

	private void copyAllToOutData(ServiceData outData, ServiceContext ctx) {
		for (Map.Entry<String, Value> entry : ctx.getAllFields()) {
			outData.put(entry.getKey(), entry.getValue());
		}

		for (Map.Entry<String, DataSheet> entry : ctx.getAllSheets()) {
			outData.put(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * return a json text based on the data spec and teh actual data in service context
	 * @param ctx
	 * @return json text
	 */
	public String dataToJsonText(ServiceContext ctx){
		JSONWriter writer = new JSONWriter();
		writer.object();
		this.dataToJson(writer, ctx);
		writer.endObject();
		return writer.toString();
	}
	/**
	 * write data to the json writer based on this spec, and data available in
	 * the context
	 *
	 * @param writer
	 *            should be ready to receive key-value pairs. That is, writer
	 *            should have issued a .object()
	 * @param ctx
	 */
	public void dataToJson(JSONWriter writer, ServiceContext ctx) {
		if (this.fieldNames != null) {
			JsonUtil.addAttributes(writer, this.fieldNames, ctx);
		}

		if (this.dataSheets != null) {
			for (String sheetName : this.dataSheets) {
				sheetName = TextUtil.getFieldValue(ctx, sheetName).toString();
				DataSheet sheet = ctx.getDataSheet(sheetName);
				if (sheet == null) {
					Tracer.trace("Service context has no sheet with name " + sheetName + " for output.");
				} else {
					writer.key(sheetName);
					JsonUtil.sheetToJson(writer, sheet, null, false);
				}
			}
		}
		if (this.outputRecords != null) {
			for (OutputRecord rec : this.outputRecords) {
				rec.toJson(writer, ctx);
			}
		}
		if (this.arrayNames != null) {
			for (String arrayName : this.arrayNames) {
				DataSheet sheet = ctx.getDataSheet(arrayName);
				if (sheet == null) {
					Value value = ctx.getValue(arrayName);
					if (value == null) {
						Tracer.trace("Service context has no sheet with name " + arrayName + " for output.");
						continue;
					}
					writer.key(arrayName).array().value(value).endArray();
				} else {
					writer.key(arrayName);
					JsonUtil.sheetToArray(writer, sheet);
				}
			}
		}
	}

	/**
	 * @param writer
	 * @param ctx
	 */
	private void allDataToJson(JSONWriter writer, ServiceContext ctx) {
		for (Map.Entry<String, Value> entry : ctx.getAllFields()) {
			writer.key(entry.getKey()).value(entry.getValue());
		}

		for (Map.Entry<String, DataSheet> entry : ctx.getAllSheets()) {
			writer.key(entry.getKey());
			DataSheet sheet = entry.getValue();
			JsonUtil.sheetToJson(writer, sheet, null, false);
		}
	}

	/**
	 * to be called before starting the service
	 *
	 * @param ctx
	 */
	public void onServiceStart(ServiceContext ctx) {
		if (this.outputFromWriter) {
			Tracer.trace("Started Writer for this service");
			ResponseWriter writer = new JSONWriter();
			writer.init();
			ctx.setWriter(writer);
		}
	}

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

		int nbrChildren = 0;
		if (this.outputRecords != null) {
			for (OutputRecord rec : this.outputRecords) {
				rec.getReady(this);
				if (rec.parentSheetName != null) {
					nbrChildren++;
				}
			}
		}

		if (nbrChildren == 0) {
			return;
		}
		/*
		 * OK. we have to deal with hierarchical data output
		 */
		/*
		 * store child records for a parent. we know the upper limit on the
		 * array. Hence we use an array instead of list. We take care of null
		 * entries while handling it
		 */
		OutputRecord[] children = new OutputRecord[nbrChildren];

		/*
		 * we go by parent, and accumulate children
		 */
		for (OutputRecord parent : this.outputRecords) {
			String sheetName = parent.sheetName;
			if (sheetName == null) {
				continue;
			}
			/*
			 * look for possible child claiming this sheet be parent :-)
			 */
			int idx = 0;
			for (OutputRecord child : this.outputRecords) {
				if (sheetName.equals(child.parentSheetName)) {
					children[idx++] = child;
					nbrChildren--; // for tracking
				}
			}
			if (idx != 0) {
				parent.setChildren(children, idx);
				/*
				 * are we done?
				 */
				if (nbrChildren == 0) {
					break;
				}
				/*
				 * we utilized current array. Create new one for next parent.
				 */
				children = new OutputRecord[nbrChildren];
			}
		}
		/*
		 * are there children still looking for parents?
		 */
		if (nbrChildren != 0) {
			throw new ApplicationError("Please check parentSheetName attribute for inputRecords. We found "
					+ nbrChildren + " sheets with missing parent sheets!!");
		}
	}

	/**
	 * validate this specification
	 *
	 * @param ctx
	 * @return number of errors added
	 */
	int validate(ValidationContext ctx) {
		int count = 0;
		/*
		 * duplicate field names
		 */
		if (this.fieldNames != null && this.fieldNames.length > 0) {
			Set<String> keys = new HashSet<String>();
			for (String key : this.fieldNames) {
				if (keys.add(key) == false) {
					ctx.addError(key + " is a duplicate field name for output.");
					count++;
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
					ctx.addError(key + " is a duplicate data sheet name for output.");
					count++;
				}
			}
		}

		if (this.outputRecords == null) {
			return 0;
		}

		/*
		 * validate output records, and also keep sheet-record mapping for other
		 * validations.
		 */
		Map<String, OutputRecord> allSheets = new HashMap<String, OutputRecord>();
		int nbrParents = 0;
		for (OutputRecord rec : this.outputRecords) {
			count += rec.validate(ctx);
			allSheets.put(rec.sheetName, rec);
			if (rec.parentSheetName != null) {
				nbrParents++;
			}
		}

		if (nbrParents == 0) {
			return count;
		}
		/*
		 * any infinite loops with cyclical relationships?
		 */
		for (OutputRecord rec : this.outputRecords) {
			if (rec.parentSheetName != null) {
				count += this.validateParent(rec, allSheets, ctx);
			}
		}
		return count;
	}

	/**
	 * @return
	 */
	private int validateParent(OutputRecord outRec, Map<String, OutputRecord> allSheets, ValidationContext ctx) {
		/*
		 * check for existence of parent, as well
		 */
		Set<String> parents = new HashSet<String>();
		String sheet = outRec.sheetName;
		String parent = outRec.parentSheetName;
		while (true) {
			OutputRecord rec = allSheets.get(parent);
			/*
			 * do we have the parent?
			 */
			if (rec == null) {
				ctx.addError("output sheet " + sheet + " uses parentSheetName=" + parent
						+ " but that sheet name is not used in any outputRecord. Note that all sheets that aprticipate iin parent-child relationship must be defined using outputRecord elements.");
				return 1;
			}
			/*
			 * are we cycling in a circle?
			 */
			if (parents.add(parent) == false) {
				ctx.addError("output record with sheetName=" + sheet + " has its parentSheetName set to " + parent
						+ ". This is creating a cyclical child-parent relationship.");
				return 1;
			}
			/*
			 * is the chain over?
			 */
			if (rec.parentSheetName == null) {
				/*
				 * we are fine with this outoutRecord
				 */
				return 0;
			}
			sheet = rec.sheetName;
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
	 * enable output from a writer
	 */
	public void enableOutputFromWriter() {
		this.outputFromWriter = true;

	}
}
