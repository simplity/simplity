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

import java.io.OutputStream;
import java.io.Writer;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.simplity.core.ApplicationError;
import org.simplity.core.app.IResponseWriter;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Important to note that this writer automatically starts an object so that the
 * caller can start writing right away. Hence, done() must be called to ensure
 * that the end-tag is issued properly
 *
 * @author simplity.org
 *
 */
public class XmlRespWriter implements IResponseWriter {
	private static final Logger logger = LoggerFactory.getLogger(XmlRespWriter.class);
	private static final String ARRAY_TAG_NAME = "elements";

	private XMLStreamWriter xmlWriter;
	private String arrayTagName = ARRAY_TAG_NAME;

	/**
	 * crate a xml writer that uses the underlying writer
	 *
	 * @param riter
	 *            underlying writer that will be wrapped as XMLStreamWriter that
	 *            will receive the output
	 * @throws XMLStreamException
	 */
	public XmlRespWriter(Writer riter) throws XMLStreamException {
		this.xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(riter);
		this.xmlWriter.writeStartDocument();
	}

	/**
	 * crate a xml writer that uses the underlying writer
	 *
	 * @param stream
	 *            underlying writer that will be wrapped as XMLStreamWriter that
	 *            will receive the output
	 * @throws XMLStreamException
	 */
	public XmlRespWriter(OutputStream stream) throws XMLStreamException {
		this.xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(stream);
		this.xmlWriter.writeStartDocument();
	}

	@Override
	public void done() {
		try {
			this.xmlWriter.writeEndDocument();
		} catch (XMLStreamException e) {
			throw new ApplicationError(e, "Error while closing xml writering ");
		}
	}

	@Override
	public XmlRespWriter setField(String fieldName, Object value) {
		try {
			this.xmlWriter.writeStartElement(fieldName);
			this.xmlWriter.writeCharacters(value.toString());
			this.xmlWriter.writeEndElement();
		} catch (XMLStreamException e) {
			throw new ApplicationError(e, "Error while writing out put for field name " + fieldName);
		}
		return this;
	}

	@Override
	public XmlRespWriter setField(String fieldName, Value value) {
		try {
			this.xmlWriter.writeStartElement(fieldName);
			this.xmlWriter.writeCharacters(value.toString());
			this.xmlWriter.writeEndElement();
		} catch (XMLStreamException e) {
			throw new ApplicationError(e, "Error while writing out put for field name " + fieldName);
		}
		return this;
	}

	@Override
	public XmlRespWriter addToArray(Object value) {
		if (value instanceof Value == false) {
			logger.warn("Xml writer can not write arbitrary objects. array element is assumed to be primitive");
			return this;
		}
		this.setField(this.arrayTagName, (Value) value);
		return this;
	}

	@Override
	public XmlRespWriter setObject(String fieldName, Object value) {
		throw new ApplicationError(
				"XmlResponseWriter is not designed to write arbitrary objects. Caller has to use lower level methods.");
	}

	@Override
	public XmlRespWriter setArray(String arrayName, Object[] arr) {
		if (arr == null) {
			return this;
		}
		this.beginArray(arrayName);
		int nbr = arr.length;
		if (nbr > 0) {
			logger.warn("XmlResponseWriter can not write array of objects directly. primitive value is assumed.");
			for (Object obj : arr) {
				this.setField(arrayName, obj);
			}
		}
		this.endArray();
		return this;
	}

	@Override
	public XmlRespWriter setArray(String arrayName, IDataSheet sheet) {
		this.beginArray(arrayName);
		String[] fieldNames = sheet.getColumnNames();
		int nbrRows = sheet.length();
		for (int i = 0; i < nbrRows; i++) {
			this.setRow(arrayName, fieldNames, sheet.getRow(i));
		}
		this.endArray();
		return this;
	}

	private void setRow(String arrayName, String[] fieldNames, Value[] row) {
		this.beginObject(arrayName);
		for (int i = 0; i < fieldNames.length; i++) {
			this.setField(fieldNames[i], row[i]);
		}
		this.endObject();
	}

	@Override
	public XmlRespWriter beginObject(String objectName) {
		try {
			this.xmlWriter.writeStartElement(objectName);
		} catch (XMLStreamException e) {
			throw new ApplicationError(e, "error while writing xml stream");
		}
		return this;

	}

	@Override
	public XmlRespWriter beginObjectAsArrayElement() {
		try {
			this.xmlWriter.writeStartElement(this.arrayTagName);
		} catch (XMLStreamException e) {
			throw new ApplicationError(e, "error while writing xml stream");
		}
		return this;
	}

	@Override
	public XmlRespWriter endObject() {
		try {
			this.xmlWriter.writeEndElement();
		} catch (XMLStreamException e) {
			throw new ApplicationError(e, "error while writing xml stream");
		}
		return this;
	}

	@Override
	public XmlRespWriter beginArray(String arrayName) {
		this.arrayTagName = arrayName;
		try {
			this.xmlWriter.writeStartElement(arrayName);
		} catch (XMLStreamException e) {
			throw new ApplicationError(e, "error while writing xml stream");
		}
		return this;
	}

	@Override
	public XmlRespWriter beginArrayAsArrayElement() {
		try {
			this.xmlWriter.writeStartElement(this.arrayTagName);
		} catch (XMLStreamException e) {
			throw new ApplicationError(e, "error while writing xml stream");
		}
		return this;
	}

	@Override
	public XmlRespWriter endArray() {
		this.arrayTagName = ARRAY_TAG_NAME;
		try {
			this.xmlWriter.writeEndElement();
		} catch (XMLStreamException e) {
			throw new ApplicationError(e, "error while writing xml stream");
		}
		return this;
	}
}
