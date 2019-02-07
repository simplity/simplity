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
package org.simplity.kernel.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.AttachmentManager;
import org.simplity.kernel.app.AppConventions;
import org.simplity.kernel.app.IRequestReader;
import org.simplity.kernel.comp.IValidationContext;
import org.simplity.kernel.comp.ValidationMessage;
import org.simplity.kernel.data.IDataSheet;
import org.simplity.kernel.msg.Messages;
import org.simplity.kernel.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that specifies what inputs are expected
 *
 * @author simplity.org
 *
 */

public class InputData {
	private static final Logger logger = LoggerFactory.getLogger(InputData.class);

	/**
	 * input is managed by the service itself. Keep the reader in the context as
	 * it is.
	 */
	boolean managedByService;

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
			IDataSheet sheet = ctx.getDataSheet(sheetName);
			if (sheet == null) {
				logger.info("Data sheet " + sheetName + " not input. Hence no attachment management on its column "
						+ colName);
				continue;
			}
			idx = sheet.getColIdx(colName);
			if (idx == -1) {
				logger.info("Data sheet " + sheetName + " does not have a column named " + colName
						+ " No attachment management on this column");
				continue;
			}
			int nbr = sheet.length();
			if (nbr == 0) {
				logger.info("Data sheet " + sheetName + " has no rows. No attachment management on this column");
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
				logger.info("Attachment key " + key + " replaced with " + newKey
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
				logger.info("Attachment field " + af + " is not specified. Skipping it.");
				continue;
			}
			String newKey = null;
			if (toStor) {
				newKey = AttachmentManager.moveToStorage(key);
			} else {
				newKey = AttachmentManager.moveFromStorage(key);
			}
			if (newKey == null) {
				logger.info("Error while managing attachment key " + key);
				ctx.addValidationMessage(Messages.INVALID_ATTACHMENT_KEY, af, null, null, 0, newKey);
			} else {
				logger.info("Attachment key " + key + " replaced with " + newKey
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
		Set<String> parents = new HashSet<>();
		if (this.inputRecords != null) {
			for (InputRecord inRec : this.inputRecords) {
				inRec.getReady();
				if (inRec.parentSheetName != null) {
					parents.add(inRec.parentSheetName);
				}
			}
		}
		if (parents.size() == 0) {
			return;
		}
		/*
		 * OK. we have to deal with hierarchical data output
		 */
		List<InputRecord> children = new ArrayList<>();
		InputRecord[] empty = new InputRecord[0];
		for (String parentSheetName : parents) {
			children.clear();
			InputRecord parent = null;
			for (InputRecord rec : this.inputRecords) {
				if (rec.name.equals(parentSheetName)) {
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
		if (this.attachmentColumns != null) {
			for (String txt : this.attachmentColumns) {
				int idx = txt.lastIndexOf('.');
				if (idx == -1) {
					vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
							"attachmentColumn should be of the form sheetName.columnName", "attachmentColumns"));
				}
			}
		}

		if (this.justInputEveryThing == true) {
			/*
			 * we do not expect fields/records
			 */
			if (this.inputRecords != null || this.inputFields != null) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
						"Input records and inputFields can not be specified because justInputEveryThing/setInputToFieldName specified.",
						"inputFields"));
			}
		} else {
			/*
			 * we do expect fields/records
			 */
			if (this.inputRecords == null && this.inputFields == null) {
				vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
						"input data has no input records and no input fields. If no data is expected, just skip InputData.",
						"inputFields"));
				return;
			}
		}
		if (this.inputRecords != null) {
			for (InputRecord rec : this.inputRecords) {
				rec.validate(vtx);
			}
		}
		if (this.inputFields != null) {
			for (InputField fields : this.inputFields) {
				fields.validate(vtx);
			}
		}
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
			String fieldName = attId + AppConventions.Name.OLD_ATT_TOKEN_SUFFIX;
			String token = ctx.getTextValue(fieldName);
			if (token == null) {
				logger.info(attId + " is an attachment input field. No value found in " + fieldName
						+ " on exit of service, and hence this attachment is not removed from storage");
			} else {
				AttachmentManager.removeFromStorage(token);
				logger.info("Attachment field " + attId + " had an existing token " + token
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
	public void setInputFields(InputField[] inFields) {
		this.inputFields = inFields;
	}

	/**
	 * extract and validate data from input service data into service context
	 *
	 * @param reader
	 *            non-null pay-load received from client
	 * @param ctx
	 *            into which data is to be extracted to
	 */
	public void read(IRequestReader reader, ServiceContext ctx) {

		if (this.managedByService) {
			logger.info("Input will be used during service execution. No data extracted upfront.");
			ctx.setReader(reader);
			return;

		}

		if (this.justInputEveryThing) {
			logger.info("Input data is copied with no spec.");
			reader.pushDataToContext(ctx);
			return;
		}

		if (this.inputFields != null) {
			int n = 0;
			for (InputField field : this.inputFields) {
				n += field.read(reader, ctx);
			}
			logger.info(n + " fields extracted for input");
		}

		if (this.inputRecords != null) {
			for (InputRecord inRec : this.inputRecords) {
				inRec.read(reader, ctx);
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
	 *
	 * @return input fields list
	 */
	public InputField[] getInputFields() {
		return this.inputFields;
	}

	/**
	 * copy data from one service context to the other. Since this is within our
	 * boundary, we simply copy, with no validations
	 *
	 * @param fromCtx
	 * @param toCtx
	 */
	public void copy(ServiceContext fromCtx, ServiceContext toCtx) {
		if (this.justInputEveryThing || this.managedByService) {
			logger.info("Copying everyting from context for new service");
			toCtx.copyFrom(fromCtx);
			return;
		}

		if (this.inputFields != null) {
			for (InputField field : this.inputFields) {
				Value val = fromCtx.getValue(field.name);
				if (Value.isNull(val) == false) {
					toCtx.setValue(field.name, val);
				}
			}
		}

		if (this.inputRecords != null) {
			for (InputRecord rec : this.inputRecords) {
				rec.copy(fromCtx, toCtx);
			}
		}
	}
}
