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

import org.simplity.json.JSONObject;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.AttachmentManager;
import org.simplity.kernel.Messages;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.service.ServiceProtocol;

/**
 * Component that specifies what inputs are expected
 *
 * @author simplity.org
 *
 */

public class InputData {

	/**
	 * do not parse the request text. Just set it to this field. Service will
	 * take care of that
	 */
	String setInputToFieldName;

	/**
	 * No specification. Trust the client and extract whatever is input
	 */
	boolean justInputEveryThing;

	/**
	 * fields to be extracted from input
	 */

	InputField[] inputFields;

	/**
	 * data sheets to be extracted from input
	 */
	InputRecord[] inputRecords;

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
	 * extract and validate data from input service data into service context
	 *
	 * @param inputText
	 *            text
	 *            non-null pay-load received from client
	 * @param ctx
	 *            into which data is to be extracted to
	 */
	public void extractFromJson(String inputText, ServiceContext ctx) {
		JSONObject json = null;
		if (inputText == null) {
			json = new JSONObject();
		} else {
			String jsonText = inputText.trim();
			if (jsonText.isEmpty()) {
				json = new JSONObject();
			} else {
				json = new JSONObject(jsonText);
			}
		}
		this.extractFromJson(json, ctx);
	}

	/**
	 * extract and validate data from input service data into service context
	 *
	 * @param json
	 *            non-null pay-load received from client
	 * @param ctx
	 *            into which data is to be extracted to
	 */
	public void extractFromJson(JSONObject json, ServiceContext ctx) {
		if (this.setInputToFieldName != null) {
			ctx.setTextValue(this.setInputToFieldName, json.toString());
			Tracer.trace("Request text is not parsed but set as object value of " + this.setInputToFieldName);
			return;
		}
		if (this.justInputEveryThing) {
			JsonUtil.extractAll(json, ctx);
			return;
		}

		int n = 0;
		if (this.inputFields != null) {
			for (InputField field : this.inputFields) {
				if (field.extractInput(json.opt(field.name), ctx)) {
					n++;
				}
			}
			Tracer.trace(n + " fields extracted for input");
		}
		if (this.inputRecords != null) {
			for (InputRecord inRec : this.inputRecords) {
				inRec.extractInput(json, ctx);
			}

		}

		if (this.attachmentFields != null) {
			storeFieldAttaches(this.attachmentFields, ctx, true);
		}

		if (this.attachmentColumns != null) {
			storeColumnAttaches(this.attachmentColumns, ctx, true);
		}
	}

	/**
	 * this is made static to allow re-use by OutputData as well
	 *
	 * @param columns
	 * @param ctx
	 * @param toStore
	 */
	static void storeColumnAttaches(String[] columns, ServiceContext ctx, boolean toStore) {
		for (String ac : columns) {
			int idx = ac.lastIndexOf('.');
			if (idx == -1) {
				throw new ApplicationError("Invalid attachmentColumns specification");
			}
			String sheetName = ac.substring(0, idx);
			String colName = ac.substring(idx + 1);
			DataSheet sheet = ctx.getDataSheet(sheetName);
			if (sheet == null) {
				Tracer.trace("Data sheet " + sheetName + " not input. Hence no attachment management on its column "
						+ colName);
				continue;
			}
			idx = sheet.getColIdx(colName);
			if (idx == -1) {
				Tracer.trace("Data sheet " + sheetName + " does not have a column named " + colName
						+ " No attachment management on this column");
				continue;
			}
			int nbr = sheet.length();
			if (nbr == 0) {
				Tracer.trace("Data sheet " + sheetName + " has no rows. No attachment management on this column");
				continue;
			}
			for (int i = 0; i < nbr; i++) {
				Value key = sheet.getColumnValue(colName, i);

				if (Value.isNull(key) || key.toString().isEmpty()) {
					continue;
				}
				String newKey = null;
				if (toStore) {
					newKey = AttachmentManager.moveToStorage(key.toText());
				} else {
					newKey = AttachmentManager.moveFromStorage(key.toText());
				}
				if (newKey == null) {
					throw new ApplicationError(
							"Unable to move attachment content with key=" + key + " from/to temp area");
				}
				Tracer.trace("Attachment key " + key + " replaced with " + newKey
						+ " after swapping content from/to temp area");
				sheet.setColumnValue(colName, i, Value.newTextValue(newKey));
			}
		}
	}

	/**
	 *
	 * @param fields
	 * @param ctx
	 * @param toStor
	 */
	static void storeFieldAttaches(String[] fields, ServiceContext ctx, boolean toStor) {
		for (String af : fields) {
			String key = ctx.getTextValue(af);
			if (key == null || key.isEmpty()) {
				Tracer.trace("Attachment field " + af + " is not specified. Skipping it.");
				continue;
			}
			String newKey = null;
			if (toStor) {
				newKey = AttachmentManager.moveToStorage(key);
			} else {
				newKey = AttachmentManager.moveFromStorage(key);
			}
			if (newKey == null) {
				Tracer.trace("Error while managing attachment key " + key);
				ctx.addValidationMessage(Messages.INVALID_ATTACHMENT_KEY, af, null, null, 0, newKey);
			} else {
				Tracer.trace("Attachment key " + key + " replaced with " + newKey
						+ " after swapping the contents from/to temp area");
				ctx.setTextValue(af, newKey);
			}
		}
	}

	/**
	 * get ready for a long-haul service :-)
	 */
	public void getReady() {
		if (this.inputFields != null) {
			for (InputField field : this.inputFields) {
				field.getReady();
			}
		}
		if (this.inputRecords != null) {
			for (InputRecord inRec : this.inputRecords) {
				inRec.getReady();
			}
		}
	}

	/**
	 * validate this specification
	 *
	 * @param ctx
	 * @return number of errors added
	 */
	public int validate(ValidationContext ctx) {
		if (this.inputRecords == null) {
			if (this.inputFields == null) {
				ctx.addError(
						"input data has no input records and no input fields. If no data is expected, just skip InputData.");
				return 1;
			}
			return 0;
		}
		int count = 0;
		for (InputRecord rec : this.inputRecords) {
			count += rec.validate(ctx);
		}
		if (this.inputFields != null) {
			for (InputField fields : this.inputFields) {
				count += fields.validate(ctx);
			}
		}
		if (this.attachmentColumns != null) {
			for (String txt : this.attachmentColumns) {
				int idx = txt.lastIndexOf('.');
				if (idx == -1) {
					ctx.addError(
							"attachmentColumns is set to " + txt + ". This should be of the form sheetName.columnName");
					count++;
				}
			}
		}
		return count;
	}

	/**
	 * Now that the service has succeeded, is there anything that the input had
	 * done that need to be cleaned-up? As of now, we have attachments that may
	 * have been superseded..
	 *
	 * @param ctx
	 */
	public void cleanup(ServiceContext ctx) {
		if (this.attachmentFields == null) {
			return;
		}
		for (String attId : this.attachmentFields) {
			String fieldName = attId + ServiceProtocol.OLD_ATT_TOKEN_SUFFIX;
			String token = ctx.getTextValue(fieldName);
			if (token == null) {
				Tracer.trace(attId + " is an attachment input field. No value found in " + fieldName
						+ " on exit of service, and hence this attachment is not removed from storage");
			} else {
				AttachmentManager.removeFromStorage(token);
				Tracer.trace("Attachment field " + attId + " had an existing token " + token
						+ ". That is now removed from storage");
			}

		}
	}

	/**
	 * @param inRecs
	 */
	public void setRecords(InputRecord[] inRecs) {
		this.inputRecords = inRecs;

	}

	/**
	 *
	 * @param inFields
	 */
	public void setInputFields(InputField[] inFields){
		this.inputFields = inFields;
	}
	
	/**
	 * 
	 * @return
	 */
	public InputField[] getInputFields() {
		return inputFields;
	}
}
