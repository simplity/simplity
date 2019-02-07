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
package org.simplity.kernel.msg;

import java.util.Arrays;
import java.util.Collection;

import org.simplity.json.JSONWriter;
import org.simplity.json.JsonWritable;
import org.simplity.kernel.app.Application;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.data.IDataSheet;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * formatted message data structure.
 *
 * @author simplity.org
 */
public class FormattedMessage implements JsonWritable {
	private static final Logger logger = LoggerFactory.getLogger(FormattedMessage.class);

	/** name of the message */
	public final String name;
	/** message text */
	public final String text;
	/** message type */
	public final MessageType messageType;
	/** if this message is regarding a field/column that was sent from client */
	public String fieldName;
	/**
	 * if this message is regarding two fields, this is the other field (like
	 * from-to)
	 */
	public String relatedFieldName;
	/** if this message is regarding a column in a sheet */
	public String tableName;
	/** in case the error is for a specific row (1-based) of a sheet */
	public int rowNumber;

	/** run-time values that may have to be inserted into the message */
	public String[] values;

	/** custom data used by various actions */
	public String[] data;

	/**
	 * we require these three fields that can not be changed afterwards. Other
	 * attributes can be optionally set
	 *
	 * @param name
	 * @param type
	 * @param text
	 */
	public FormattedMessage(String name, MessageType type, String text) {
		this.name = name;
		this.messageType = type;
		this.text = text;
	}

	/**
	 * @param msg
	 *            message component that has no parameters in its text
	 */
	public FormattedMessage(Message msg) {
		this.name = msg.getQualifiedName();
		this.messageType = msg.getMessageType();
		this.text = msg.text;
	}

	/**
	 * @param messageName
	 *            message name
	 * @param params
	 *            message parameters
	 */
	public FormattedMessage(String messageName, String... params) {
		Message msg = (Message) Application.getActiveInstance().getComponentOrNull(ComponentType.MSG, messageName);
		if (msg == null) {
			this.name = messageName;
			this.text = messageName + " : description for this message is not found.";
			this.messageType = MessageType.WARNING;
			logger.info("Missing message : " + messageName);
		} else {
			this.name = msg.getQualifiedName();
			this.messageType = msg.getMessageType();
			this.text = msg.toString(params);
			this.values = params;
		}
	}

	/**
	 * @param msgName
	 *            message name
	 * @param tableName
	 *            table/sheet associated with this message
	 * @param fieldName
	 *            name of the field/column associated with this message
	 * @param otherFieldName
	 *            other field, (like from-to) that this message refers to
	 * @param rowNumber
	 *            if a table/sheet is associated, then the row number
	 * @param params
	 *            additional parameters for the message
	 */
	public FormattedMessage(
			String msgName,
			String tableName,
			String fieldName,
			String otherFieldName,
			int rowNumber,
			String... params) {
		this(msgName, params);
		this.tableName = tableName;
		this.fieldName = fieldName;
		this.relatedFieldName = otherFieldName;
		this.rowNumber = rowNumber;
	}

	@Override
	public void writeJsonValue(JSONWriter writer) {
		writer
				.object()
				.key("name")
				.value(this.name)
				.key("text")
				.value(this.text)
				.key("messageType")
				.value(this.messageType)
				.key("data")
				.value(this.data);
		if (this.fieldName != null) {
			writer.key("fieldName").value(this.fieldName);
		}
		if (this.relatedFieldName != null) {
			writer.key("relatedFieldName").value(this.relatedFieldName);
		}
		if (this.tableName != null) {
			writer.key("tableName").value(this.tableName);
		}
		if (this.rowNumber != 0) {
			writer.key("rowNumber").value(this.rowNumber);
		}
		if (this.values != null) {
			writer.key("values");
			writer.array();
			for (String val : this.values) {
				writer.value(val);
			}
			writer.endArray();
		}
		writer.endObject();
	}

	/**
	 * add data to a formatted message
	 *
	 * @param d
	 */
	public void addData(String d) {
		if (this.data == null) {
			this.data = new String[1];
			this.data[0] = d;
			return;
		}
		String[] tempData = Arrays.copyOf(this.data, this.data.length + 1);
		tempData[this.data.length] = d;
		this.data = tempData;
		return;
	}

	private static ValueType[] MESSAGE_COMPONENT_TYPES = { ValueType.TEXT, ValueType.TEXT, ValueType.TEXT,
			ValueType.TEXT };
	private static String[] MESSAGE_COMPONENT_NAMES = { "name", "text", "messageType", "fieldName" };

	/**
	 * put the messages into a data sheet
	 *
	 * @param messages
	 * @return non-null data sheet, possibly empty
	 */
	public static IDataSheet toDataSheet(FormattedMessage[] messages) {
		IDataSheet result = new MultiRowsSheet(MESSAGE_COMPONENT_NAMES, MESSAGE_COMPONENT_TYPES);
		if (messages == null || messages.length == 0) {
			return result;
		}
		for (FormattedMessage message : messages) {
			Value[] row = new Value[MESSAGE_COMPONENT_TYPES.length];
			row[0] = Value.newTextValue(message.name);
			row[1] = Value.newTextValue(message.text);
			row[2] = Value.newTextValue(message.messageType.name());
			row[3] = Value.newTextValue(message.fieldName);
			result.addRow(row);
		}
		return result;
	}

	/**
	 * append this message as descriptive text to a string builder
	 *
	 * @param sbf
	 */
	public void toString(StringBuilder sbf) {
		sbf.append(this.text);
		sbf.append(" FieldName=").append(this.fieldName);
		if (this.tableName != null) {
			sbf.append(" tableName=").append(this.tableName);
		}
		if (this.rowNumber != 0) {
			sbf.append(" row=").append(this.rowNumber);
		}
		if (this.relatedFieldName != null) {
			sbf.append(" related field Name=").append(this.relatedFieldName);
		}
	}

	/**
	 *
	 * @param messages
	 * @return new-line separated text for all messages
	 */
	public static String toString(Collection<FormattedMessage> messages) {
		StringBuilder sbf = new StringBuilder();
		for (FormattedMessage msg : messages) {
			msg.toString(sbf);
			sbf.append('\n');
		}
		return sbf.toString();
	}

	/**
	 *
	 * @param messages
	 * @return new-line separated text for all messages
	 */
	public static String toString(FormattedMessage[] messages) {
		StringBuilder sbf = new StringBuilder();
		for (FormattedMessage msg : messages) {
			msg.toString(sbf);
			sbf.append('\n');
		}
		return sbf.toString();
	}

}
