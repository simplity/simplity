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

package org.simplity.core.app.internal;

import java.util.Stack;

import org.simplity.core.app.IRequestReader;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.data.MultiRowsSheet;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.util.XmlUtil;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;
import org.simplity.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * request reader for xml input.
 *
 * @author simplity.org
 *
 */
public class XmlReqReader implements IRequestReader {
	private static final Logger logger = LoggerFactory.getLogger(IRequestReader.class);
	/**
	 * payload parsed into a document Object. Null if input is not a valid dom
	 */
	private final Document inputXml;
	/**
	 * stack of open objects.
	 */
	private Stack<Object> openObjects = new Stack<Object>();

	/**
	 * current object being read. Could be Element, NodeList or ElementAsList
	 */
	private Object currentObject;

	/**
	 * true if currentIbject is an element. False if it is treated as a list
	 */
	private boolean workingWithList = false;

	/**
	 * instantiate input translator for a document
	 *
	 * @param doc
	 */
	public XmlReqReader(Document doc) {
		this.inputXml = doc;
		this.currentObject = doc.getDocumentElement();
		logger.info("Payload is set to a document with root element {}", ((Element) this.currentObject).getTagName());
	}

	@Override
	public InputValueType getValueType(String fieldName) {
		if (this.workingWithList) {
			return InputValueType.NULL;
		}
		Element ele = (Element) this.currentObject;

		return getValueType(getFieldValue(ele, fieldName));
	}

	@Override
	public InputValueType getValueType(int idx) {
		if (!this.workingWithList) {
			return InputValueType.NULL;
		}
		if (this.currentObject instanceof ElementAsList) {
			if (idx == 0) {
				ElementAsList list = (ElementAsList) this.currentObject;
				return getValueType(list.getElement());
			}
			return null;
		}
		NodeList list = (NodeList) this.currentObject;
		return getValueType(list.item(idx));
	}

	@Override
	public Object getValue(String fieldName) {
		if (this.workingWithList) {
			return null;
		}
		return getFieldValue((Element) this.currentObject, fieldName);
	}

	@Override
	public Object getValue(int zeroBasedIdx) {
		if (!this.workingWithList) {
			return null;
		}
		if (this.currentObject instanceof NodeList) {
			return ((NodeList) this.currentObject).item(zeroBasedIdx);
		}
		if (zeroBasedIdx == 0) {
			return ((ElementAsList) this.currentObject).getElement();
		}
		return null;
	}

	@Override
	public boolean openObject(String attributeName) {
		if (this.workingWithList) {
			return false;
		}
		return this.setCurrentObject(this.getValue(attributeName));
	}

	@Override
	public boolean openObject(int idx) {
		if (!this.workingWithList) {
			return false;
		}
		Node node = null;
		if (this.currentObject instanceof NodeList) {
			node = ((NodeList) this.currentObject).item(idx);
		} else if (idx == 0) {
			node = ((ElementAsList) this.currentObject).getElement();
		}
		return this.setCurrentObject(node);
	}

	@Override
	public boolean closeObject() {
		if (this.workingWithList) {
			return false;
		}
		return this.pop();
	}

	@Override
	public boolean openArray(String attributeName) {
		if (this.workingWithList) {
			return false;
		}
		return this.setCurrentArray(this.getValue(attributeName));
	}

	@Override
	public boolean openArray(int zeroBasedIdx) {
		if (!this.workingWithList) {
			return false;
		}
		return this.setCurrentArray(this.getValue(zeroBasedIdx));
	}

	@Override
	public boolean closeArray() {
		if (!this.workingWithList) {
			return false;
		}
		return this.pop();
	}

	@Override
	public int getNbrElements() {
		if (!this.workingWithList) {
			return 0;
		}
		if (this.currentObject instanceof NodeList) {
			return ((NodeList) this.currentObject).getLength();
		}
		return 1;
	}

	@Override
	public String[] getAttributeNames() {
		if (this.workingWithList) {
			logger.error("Call made to getAttributeNames when looking at a list!! empty string returned.");
			return new String[0];
		}
		return XmlUtil.getAllNodeNames((Element) this.currentObject);
	}

	@Override
	public void pushDataToContext(ServiceContext ctx) {
		if (this.inputXml == null) {
			logger.info("No input xml assigned to the reader before extracting data.");
			return;
		}
		String[] names = this.getAttributeNames();
		if (names == null || names.length == 0) {
			logger.info("Input xml is empty. No data extracted.");
			return;
		}
		for (String key : names) {
			Object value = this.getValue(key);
			InputValueType vt = getValueType(value);
			switch (vt) {
			case ARRAY:
				JSONArray arr = (JSONArray) value;
				if (arr.length() != 0) {
					IDataSheet sheet = getSheet((NodeList) value);
					if (sheet == null) {
						logger.error("Table {} could not be extracted into context.", key);
					} else {
						ctx.putDataSheet(key, sheet);
						logger.info("Table {} extracted with {} rows.", key, sheet.length());
					}
				}
				break;

			case OBJECT:
			case ARRAY_OR_OBJECT:
				IDataSheet sheet = getSheet((Element) value);
				if (sheet != null) {
					ctx.putDataSheet(key, sheet);
					logger.info("Object " + key + " extracted as a single-row data sheet.");
				}
				break;
			case VALUE:
				ctx.setValue(key, Value.parse(value));
				break;
			case NULL:
				break;
			default:
				logger.error("Reader is not designed to handle value type {}. Value for key {} ignored", vt, key);
			}

		}
	}

	/**
	 * @param value
	 * @return
	 */
	private static IDataSheet getSheet(Element element) {
		String[] names = XmlUtil.getAllNodeNames(element);
		int nbr = names.length;
		ValueType[] types = new ValueType[nbr];
		Value[] values = new Value[nbr];

		for (int i = 0; i < names.length; i++) {
			Object obj = getFieldValue(element, names[i]);
			InputValueType ivt = getValueType(obj);

			if (ivt == null) {
				types[i] = ValueType.TEXT;
				values[i] = Value.newUnknownValue(ValueType.TEXT);
				continue;
			}

			if (ivt == InputValueType.VALUE) {
				Value value = Value.parse(obj);
				values[i] = value;
				types[i] = value.getValueType();
				continue;
			}

			/*
			 * we can not handle embedded object structures
			 */
			logger.error(
					"Input contains arbitrary object structure that can not be parsed without input specification. Value not extracted");
			return null;

		}
		IDataSheet ds = new MultiRowsSheet(names, types);
		ds.addRow(values);
		return ds;
	}

	private static IDataSheet getSheet(NodeList nodes) {
		if (nodes == null) {
			return null;
		}
		Node node = nodes.item(0);
		if (node == null || node instanceof Element == false) {
			return null;
		}

		IDataSheet ds = getSheet((Element) node);
		String[] names = XmlUtil.getAllNodeNames((Element) node);
		int nbrCols = names.length;
		int nbrRows = nodes.getLength();
		for (int i = 1; i < nbrRows; i++) {
			node = nodes.item(i);
			if (node == null || node instanceof Element == false) {
				logger.info("Row " + (i + 1) + " is null or not an object. Not extracted");
				continue;
			}
			Element ele = (Element) node;
			Value[] row = new Value[nbrCols];
			for (int j = 0; j < names.length; j++) {
				row[j] = Value.parse(getFieldValue(ele, names[j]));
			}
			ds.addRow(row);
		}
		return ds;
	}

	private boolean pop() {
		if (this.openObjects.isEmpty()) {
			return false;
		}
		this.currentObject = this.openObjects.pop();
		this.workingWithList = !(this.currentObject instanceof Element);
		return true;
	}

	private boolean setCurrentArray(Object node) {
		InputValueType vt = getValueType(node);
		switch (vt) {
		case NULL:
		case VALUE:
			return false;

		case ARRAY:
			this.openObjects.push(this.currentObject);
			this.currentObject = node;
			this.workingWithList = true;
			return true;

		case ARRAY_OR_OBJECT:
		case OBJECT:
			this.openObjects.push(this.currentObject);
			this.currentObject = new ElementAsList((Element) node);
			this.workingWithList = true;
			return true;

		default:
			logger.error("InputValueType {} is not handled properly by openObject(). false returned by default");
			return false;
		}
	}

	private boolean setCurrentObject(Object obj) {
		InputValueType vt = getValueType(obj);
		switch (vt) {
		case NULL:
		case VALUE:
			return false;

		case ARRAY:
			return false;

		case ARRAY_OR_OBJECT:
		case OBJECT:
			this.openObjects.push(this.currentObject);
			this.currentObject = obj;
			this.workingWithList = false;
			return true;

		default:
			logger.error("InputValueType {} is not handled properly by openObject(). false returned by default");
			return false;
		}

	}

	private static InputValueType getValueType(Object value) {
		if (value == null) {
			return InputValueType.NULL;
		}
		if (value instanceof Node) {
			return getValueType((Node) value);
		}
		if (value instanceof NodeList) {
			return InputValueType.ARRAY;
		}
		return InputValueType.VALUE;
	}

	private static InputValueType getValueType(Node node) {
		if (node == null) {
			return InputValueType.NULL;
		}
		short nodeType = node.getNodeType();
		if (nodeType == Node.ATTRIBUTE_NODE) {
			return InputValueType.VALUE;
		}
		if (nodeType == Node.TEXT_NODE) {
			return InputValueType.VALUE;
		}
		NodeList nodes = node.getChildNodes();
		if (nodes == null) {
			return InputValueType.NULL;
		}
		int nbr = nodes.getLength();
		if (nbr == 0) {
			return InputValueType.NULL;
		}
		if (nbr == 1 && nodes.item(0).getNodeType() == Node.TEXT_NODE) {
			return InputValueType.VALUE;
		}
		return InputValueType.ARRAY_OR_OBJECT;
	}

	private static Object getFieldValue(Element ele, String fieldName) {
		if (ele == null) {
			return null;
		}
		String attr = ele.getAttribute(fieldName);
		if (attr != null) {
			return attr;
		}
		NodeList nodes = ele.getChildNodes();
		if (nodes == null) {
			return null;
		}
		int nbr = nodes.getLength();
		if (nbr == 0) {
			return null;
		}
		if (nbr == 1) {
			return nodes.item(0);
		}
		return nodes;
	}
}

class ElementAsList {
	private final Element element;

	ElementAsList(Element ele) {
		this.element = ele;
	}

	public Element getElement() {
		return this.element;
	}
}
