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

package org.simplity.ide.comp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * parses an xml into dom with an additional attribute for line no added to each
 * dom element
 *
 * @author simplity.org
 *
 */
public class XmlParser {
	protected static final Logger logger = LoggerFactory.getLogger(XmlParser.class);

	/**
	 * read an xml into a document, with an additional attribute for line number
	 * attribute for each element
	 *
	 * @param stream
	 * @param lineNoAttr
	 * @return xml document. an attribute with line number in the source file is
	 *         added to each element
	 */
	public static Document readXmlWithLineNumbers(InputStream stream, String lineNoAttr) {
		try {
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			final Stack<Element> elementStack = new Stack<>();
			DefaultHandler handler = new DefaultHandler() {
				private Locator locator;

				@Override
				public void setDocumentLocator(Locator locator) {
					this.locator = locator;
				}

				@Override
				public void startElement(String uri, String localName, String qName, Attributes attributes)
						throws SAXException {
					Element el = doc.createElement(qName);
					if (elementStack.isEmpty()) {
						doc.appendChild(el);
					} else {
						elementStack.peek().appendChild(el);
					}
					elementStack.push(el);
					for (int i = 0; i < attributes.getLength(); i++) {
						el.setAttribute(attributes.getQName(i), attributes.getValue(i));
					}
					int ln = this.locator.getLineNumber();
					if (ln != -1) {
						el.setAttribute(lineNoAttr, "" + ln);
					}
				}

				@Override
				public void endElement(String uri, String localName, String qName) {
					elementStack.pop();
				}

			};
			parser.parse(stream, handler);
			return doc;
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Error while parsing an xml resource", e);
		}
	}

}
