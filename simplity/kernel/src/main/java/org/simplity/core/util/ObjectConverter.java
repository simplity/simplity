/*
 * Copyright (c) 2018 simplity.org
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

package org.simplity.core.util;

import java.io.IOException;
import java.io.Writer;

import org.simplity.core.app.Application;
import org.simplity.core.dm.Record;
import org.simplity.core.dm.field.Field;
import org.simplity.core.dm.field.FieldType;
import org.simplity.core.msg.FormattedMessage;
import org.simplity.core.msg.Messages;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;
import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * utilities for working with both JSON and XML TODO: XML DOciment is a fairly
 * heavy object. We should use a direct parser to convert XML stream directly to
 * JSON, if performance is important
 *
 * @author simplity.org
 *
 */
public class ObjectConverter {
	private static final Logger logger = LoggerFactory.getLogger(ObjectConverter.class);
	private static final char OPEN_START = '<';
	private static final char CLOSE = '>';
	private static final String OPEN_END = "</";

	/**
	 * create a JSONObject for an Xml element based on a record specification
	 *
	 * @param xml
	 *            that has the required data
	 * @param record
	 *            defines the structure and validations
	 * @param ctx
	 *            to which any validation errors are added
	 * @return JsonObject.nullin case of any error, and the error messages are
	 *         added to context
	 */
	public static JSONObject xmlToJson(Element xml, Record record, ServiceContext ctx) {
		JSONObject json = new JSONObject();
		Field[] fields = record.getFields();
		boolean allOk = true;

		for (Field field : fields) {
			FieldType ft = field.getFieldType();
			boolean fieldAdded = false;

			switch (ft) {
			case RECORD:
				fieldAdded = addRecordField(json, field, xml, ctx);
				break;

			case RECORD_ARRAY:
				fieldAdded = addRecordArrayField(json, field, xml, ctx);
				break;

			case VALUE_ARRAY:
				fieldAdded = addValueArrayField(json, field, xml, ctx);
				break;

			default:
				fieldAdded = addValueField(json, field, xml, ctx);
				break;
			}

			allOk = allOk && fieldAdded;
		}

		if (allOk) {
			return json;
		}

		logger.error("There were input validation errors because of which data is not parsed into strcut");
		return null;
	}

	/**
	 * add a value field to JSON object based on the field specification, and
	 * xml element
	 *
	 * @param jsonObject
	 *            to which field is to be added to
	 * @param field
	 *            data type specification for this data element
	 * @param xml
	 *            that has the data for this field as a child-element
	 * @param ctx
	 *
	 * @return true if all OK. false in case of any errors
	 */
	private static boolean addValueField(JSONObject jsonObject, Field field, Element xml,
			ServiceContext ctx) {
		String fieldName = field.getName();
		ValueType vt = field.getValueType();

		/*
		 * field can be either an attribute, or a child element
		 */
		String text = xml.getAttribute(fieldName);
		if (text == null) {
			Node node = xml.getElementsByTagName(fieldName).item(0);
			if (node != null) {
				text = node.getTextContent();
			}
		}

		if (text == null) {
			if (field.isRequired()) {
				ctx.addMessage(new FormattedMessage(Messages.VALUE_REQUIRED, fieldName));
				return false;
			}
			return true;
		}

		Value value = Value.parseValue(text, vt);
		if (value == null) {
			ctx.addMessage(new FormattedMessage(Messages.INVALID_DATA, fieldName));
			return false;
		}

		jsonObject.put(fieldName, value);
		return true;
	}

	/**
	 * add an array of primitive values. xml is expected to have one
	 * child-element for each array element
	 *
	 * @param jsonObject
	 *            to which data is to be added to
	 * @param field
	 *            field specification for this array
	 * @param xml
	 *            element
	 * @param ctx
	 * @return
	 */
	private static boolean addValueArrayField(JSONObject jsonObject, Field field, Element xml,
			ServiceContext ctx) {
		String fieldName = field.getName();
		NodeList nodes = xml.getElementsByTagName(fieldName);
		int nbr = nodes.getLength();

		if (nbr == 0) {
			if (field.isRequired()) {
				ctx.addMessage(new FormattedMessage(Messages.VALUE_REQUIRED, fieldName));
				return false;
			}
			return true;
		}

		ValueType vt = field.getValueType();
		JSONArray arr = new JSONArray();
		boolean allOk = true;
		for (int i = 0; i < nbr; i++) {
			String text = nodes.item(i).getTextContent();
			Value value = Value.parseValue(text, vt);
			if (value == null) {
				ctx.addMessage(new FormattedMessage(Messages.INVALID_DATA, fieldName));
				allOk = false;
			}
			arr.put(value);
		}

		if (allOk) {
			jsonObject.put(fieldName, arr);
		}

		return allOk;
	}

	/**
	 * add an array of child-objects to JSON object based on record
	 * specification and data in XML
	 *
	 * @param jsonObjecton
	 *            to which data is to be added to
	 * @param field
	 *            that has the child-record specification
	 * @param xml
	 *            that has the data
	 * @param ctx
	 *
	 * @return true if all OK. false in case of any error
	 */
	private static boolean addRecordArrayField(JSONObject jsonObjecton, Field field, Element xml,
			ServiceContext ctx) {
		String fieldName = field.getName();
		NodeList nodes = xml.getElementsByTagName(fieldName);
		int nbr = nodes.getLength();

		if (nbr == 0) {
			if (field.isRequired()) {
				ctx.addMessage(new FormattedMessage(Messages.VALUE_REQUIRED, fieldName));
				return false;
			}
			return true;
		}

		Record record = Application.getActiveInstance().getRecord(field.getReferredRecord());
		JSONArray arr = new JSONArray();

		boolean allOk = true;
		for (int i = 0; i < nbr; i++) {
			Node node = nodes.item(i);
			if (node instanceof Element == false) {
				ctx.addMessage(new FormattedMessage(Messages.INVALID_DATA, fieldName));
				allOk = false;
				continue;
			}

			JSONObject child = xmlToJson((Element) node, record, ctx);
			if (child == null) {
				allOk = false;
			} else {
				arr.put(child);
			}
		}
		if (allOk) {
			jsonObjecton.put(fieldName, arr);
		}
		return allOk;
	}

	/**
	 * add a child-object to JSON object from XML
	 *
	 * @param jsonObject
	 *            to which data from XML is to be added to
	 * @param field
	 *            data type specification. XML is expected to have a child
	 *            element with this name
	 * @param xml
	 *            from which data is extracted
	 * @param errors
	 *            list to which any errors are added
	 * @return true if all OK. false if any error is added to the error list
	 */
	private static boolean addRecordField(JSONObject jsonObject, Field field, Element xml,
			ServiceContext ctx) {
		String fieldName = field.getName();
		NodeList nodes = xml.getElementsByTagName(fieldName);
		int nbr = nodes.getLength();

		if (nbr == 0) {
			if (field.isRequired()) {
				ctx.addMessage(new FormattedMessage(Messages.VALUE_REQUIRED, fieldName));
				return false;
			}
		}

		if (nbr > 1) {
			logger.warn("Multiple values found for field {}. Only first one is used and the rest are discarded",
					fieldName);
		}

		Node node = nodes.item(0);
		if (node instanceof Element == false) {
			ctx.addMessage(new FormattedMessage(Messages.INVALID_DATA, fieldName));
			return false;
		}

		Record record = Application.getActiveInstance().getRecord(field.getReferredRecord());
		JSONObject child = xmlToJson((Element) node, record, ctx);
		if (child == null) {
			return false;
		}

		jsonObject.put(fieldName, child);
		return true;
	}

	/**
	 * populate an xml element from JSON object based on record specification.
	 * Fields in the record are used as the primary driver to look for data in
	 * JSON object to be added to XML
	 *
	 * @param jsonObject
	 *            to be added to XML
	 * @param xml
	 *            to which JSON object is to be added to
	 * @param record
	 *            that has the fields specification (structure)
	 * @param ctx
	 * @return true if all OK. false otherwise.
	 */
	public static boolean jsonToXml(JSONObject jsonObject, Element xml, Record record, ServiceContext ctx) {
		Field[] fields = record.getFields();
		boolean allOk = true;
		Document doc = xml.getOwnerDocument();

		for (Field field : fields) {
			String fieldName = field.getName();
			Object obj = jsonObject.get(fieldName);

			if (obj == null) {
				if (field.isRequired()) {
					ctx.addMessage(new FormattedMessage(Messages.VALUE_REQUIRED, fieldName));
					allOk = false;
					/*
					 * this being a validation error, we continue and accumulate
					 * any more errors
					 */
				}
				continue;
			}

			FieldType ft = field.getFieldType();
			Element child = doc.createElement(fieldName);
			xml.appendChild(child);
			boolean fieldAdded = false;

			switch (ft) {
			case RECORD:
				fieldAdded = addXmlRecord(obj, field, child, ctx);
				break;
			case RECORD_ARRAY:
				fieldAdded = addXmlRecordArray(obj, field, child, ctx);
				break;

			case VALUE_ARRAY:
				fieldAdded = addXmlValueArray(obj, field, child, ctx);
				break;

			default:
				Value value = field.parseObject(obj, false, ctx);
				if (value != null) {
					fieldAdded = true;
					child.setTextContent(value.toString());
				}
				break;
			}
			allOk = allOk && fieldAdded;
		}

		if (allOk) {
			return true;
		}

		logger.error("There were input validation errors because of which data is not parsed into strcut");
		return false;
	}

	/**
	 * add a JSON object as child-element to an xml element
	 *
	 * @param jsonObject
	 *            to be added to xml
	 * @param field
	 *            that has the record details for the child object
	 * @param xml
	 *            element to which this JSON object is to be added
	 * @return true if all OK. false in case of any error.
	 */
	private static boolean addXmlRecord(Object jsonObject, Field field, Element xml, ServiceContext ctx) {
		String fieldName = field.getName();
		if (jsonObject instanceof JSONObject == false) {
			ctx.addMessage(new FormattedMessage(Messages.INVALID_DATA, fieldName));
			return false;
		}

		Record record = Application.getActiveInstance().getRecord(field.getReferredRecord());
		return jsonToXml((JSONObject) jsonObject, xml, record, ctx);
	}

	/**
	 * add an array of values to xml. one child element per array element. each
	 * child element has "value" as tag-name
	 *
	 * @param jsonArray
	 *            that has values as its elements
	 * @param field
	 *            from record that has the data type of values
	 * @param xml
	 *            xml element to which the array is to be added to
	 * @return true if all ok. false in case of any error.
	 */
	private static boolean addXmlValueArray(Object jsonArray, Field field, Element xml, ServiceContext ctx) {
		String fieldName = field.getName();
		if (jsonArray instanceof JSONArray == false) {
			ctx.addMessage(new FormattedMessage(Messages.INVALID_DATA, fieldName));
			return false;
		}

		JSONArray arr = (JSONArray) jsonArray;
		int nbr = arr.length();
		Document doc = xml.getOwnerDocument();

		for (int i = 0; i < nbr; i++) {
			Object obj = arr.get(i);
			Value value = field.parseObject(obj, false, ctx);
			if (value == null) {
				return false;
			}
			Element child = doc.createElement(fieldName);
			xml.appendChild(child);
			child.setTextContent(value.toString());
		}

		return true;
	}

	/**
	 * add an array of objects to xml using the record format
	 *
	 * @param jsonArray
	 *            JSONArray of JSONObjects that is to be added to the XML
	 * @param field
	 *            field in the parent record that defines the child record to be
	 *            used
	 * @param xml
	 *            to which the child elements are to be added to
	 * @param ctx
	 *            to which error is added, if any.
	 * @return true if all ok. false in case of any error in data. error message
	 *         in this case is added to errors list
	 */
	private static boolean addXmlRecordArray(Object jsonArray, Field field, Element xml,
			ServiceContext ctx) {
		String fieldName = field.getName();
		if (jsonArray instanceof JSONArray == false) {
			ctx.addMessage(new FormattedMessage(Messages.INVALID_DATA, fieldName));
			return false;
		}

		JSONArray arr = (JSONArray) jsonArray;
		int nbr = arr.length();
		Record record = Application.getActiveInstance().getRecord(field.getReferredRecord());
		Document doc = xml.getOwnerDocument();

		for (int i = 0; i < nbr; i++) {
			JSONObject json = arr.getJSONObject(i);
			if (json == null) {
				ctx.addMessage(new FormattedMessage(Messages.INVALID_DATA, fieldName));
				return false;
			}
			Element childEle = doc.createElement(fieldName);
			xml.appendChild(childEle);
			/*
			 * add details of this JSON child-object into this XML child-element
			 */
			jsonToXml(json, childEle, record, ctx);
		}

		return true;
	}

	/**
	 * populate an xml element from JSON object based on record specification.
	 * Fields in the record are used as the primary driver to look for data in
	 * JSON object to be added to XML
	 *
	 * @param jsonObject
	 *            to be added to XML
	 * @param record
	 *            that has the fields specification (structure)
	 * @param writer
	 * @return true if all OK. false otherwise.
	 * @throws IOException
	 */
	public static boolean jsonToXmlStream(JSONObject jsonObject, Record record, Writer writer) throws IOException {
		Field[] fields = record.getFields();
		boolean allOk = true;
		Application app = Application.getActiveInstance();
		for (Field field : fields) {
			String fieldName = field.getName();
			Object obj = jsonObject.get(fieldName);

			if (obj == null) {
				continue;
			}

			FieldType ft = field.getFieldType();
			startTag(fieldName, writer);
			boolean fieldAdded = false;

			Record childRecord = null;
			switch (ft) {
			case RECORD:
				if (obj instanceof JSONObject == false) {
					return invalidData();
				}

				childRecord = app.getRecord(field.getReferredRecord());
				fieldAdded = jsonToXmlStream((JSONObject) obj, childRecord, writer);
				break;

			case RECORD_ARRAY:
				if (obj instanceof JSONArray == false) {
					return invalidData();
				}
				childRecord = app.getRecord(field.getReferredRecord());
				fieldAdded = jsonArrayToXmlStream((JSONArray) obj, childRecord, fieldName, writer);
				break;

			case VALUE_ARRAY:
				if (obj instanceof JSONArray == false) {
					return invalidData();
				}
				fieldAdded = jsonArrayToXmlStream((JSONArray) obj, fieldName, writer);
				break;

			default:
				writer.write(xmlEscape(obj.toString()));
				break;
			}
			startTag(fieldName, writer);
			allOk = allOk && fieldAdded;
		}

		if (!allOk) {
			logger.error(
					"JSON object did not have the rigth data as per Record specification. XML may stream may not be valid");
		}
		return allOk;
	}

	private static boolean jsonArrayToXmlStream(JSONArray jsonArray, String fieldName, Writer writer)
			throws IOException {
		int nbr = jsonArray.length();
		for (int i = 0; i < nbr; i++) {
			Object obj = jsonArray.get(i);
			if (obj != null) {
				startTag(fieldName, writer);
				writer.write(obj.toString());
				endTag(fieldName, writer);
			}
		}
		return true;
	}

	private static boolean jsonArrayToXmlStream(JSONArray jsonArray, Record record, String tagName, Writer writer)
			throws IOException {
		int nbr = jsonArray.length();
		for (int i = 0; i < nbr; i++) {
			JSONObject json = jsonArray.getJSONObject(i);
			if (json != null) {
				startTag(tagName, writer);
				jsonToXmlStream(json, record, writer);
				endTag(tagName, writer);
			}
		}

		return true;
	}

	private static void startTag(String tagName, Writer writer) throws IOException {
		writer.write(OPEN_START);
		writer.write(tagName);
		writer.write(CLOSE);
	}

	private static void endTag(String tagName, Writer writer) throws IOException {
		writer.write(OPEN_END);
		writer.write(tagName);
		writer.write(CLOSE);
	}

	/**
	 *
	 * @return write an error log and return false
	 */
	private static boolean invalidData() {
		logger.error(
				"JSON object has invalid data as per record specification. Data writing operation abandoned in mid stream..");
		return false;

	}

	private static String xmlEscape(String text) {
		if (text == null) {
			return "";
		}
		return text.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;");
	}
}
