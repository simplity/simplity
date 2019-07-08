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
package org.simplity.core.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.simplity.core.ApplicationError;
import org.simplity.core.app.AppConventions;
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.data.IFieldsCollection;
import org.simplity.core.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

/**
 * * Utility that binds xml to object graph. A simple substitute to JAXB. We use
 * it because we try to keep the objects as clean as possible. Using this
 * utility we get away without annotations(well almost) and setters. This is
 * meant specifically for the design components that are saved as xml. This
 * design is not suitable for binding domain/business data. Note that this is
 * one-way binding. We expect that the xmls are maintained using either
 * xsd-based editors, or eclipse plugins. Here we focus only on run-time.
 * Following conventions are used to keep it simple.
 *
 * <p>
 * 1. We consider number, boolean, char, string, date, enum, pattern (regex) and
 * Expr (our version of expression) as primitive, or more aptly values.
 *
 * <p>
 * 2. in the xml schema, all primitive/value attributes are preferably specified
 * as attributes. Like age="22". But not <age>22</age>. This is generally
 * simple. However, if the text contains special characters, a CDATA could be a
 * better alternative.
 *
 * <p>
 * e.g. <expression>[[-----------------------]]</expression>
 *
 * <p>
 * 3. Array of primitives are specified as a comma separated list. We do not use
 * comma as a valid character in any of the text values that we intend to put in
 * a list, and hence this is okay. for example colors="red,blue green".
 *
 * <p>
 * 4. We support Map<> with only String as key. This field MUST be initialized
 * in the class definition. Since Java does not retain the class of the Map
 * value, we assume that the class is in the same package as the container
 * class. In case it is different, use MapDetails annotation to specify. Also,
 * we use name as the attribute to index the object on. In case you have
 * something different use annotation. As of now you can not have sub-classes of
 * value that belong to different packages. (we will extend the design when we
 * reach there)
 *
 * <p>
 * like Map<String, MyAbstractClass> fieldName = new HashMap<String,
 * MyAbstractClass>();
 *
 * <p>
 * In the xml, use the field name as the wrapper element to the list of Map
 * member elements
 *
 * <pre>
 * <fieldName>
 * 	<concreteClass a1="v1" a2="v2"....... />
 * 		.......
 * 	.......
 * </fieldName>
 * </pre>
 *
 * 5. Arrays are not to be initialized in object. In case of array of objects
 * (non-primitive) use xml schema same as for map explained above.
 *
 * <p>
 * 6. Non-primitive field. If you expect the same class, and not any sub-class,
 * then you should initialize it in your class.
 *
 * <p>
 * MyClass myField = new MyClass();
 *
 * <p>
 * 7. Non-primitive field - Super class declaration with a choice of sub-class.
 * In this case, obviously, you can not instantiate it in your class. Use a
 * wrapper element with fieldName as tag name, with exactly one child element
 * with the concrete class name as tag name. For elegance, you may use camel
 * case of the class name, and we take care of ClassCasing it.
 *
 * <p>
 * for example
 *
 * <pre>
 * <myDataType>
 * 		<textDataType ........ />
 * </myDataType>
 * </pre>
 */
public class XmlUtil {
	private static final Logger logger = LoggerFactory.getLogger(XmlUtil.class);

	/**
	 * name of attribute that has the line number in the file where this element
	 * tag occurred
	 */
	public static final String ATTR_LINE_NUMBER = "_lineNo";

	private static final String DEFAULT_MAP_KEY = "name";
	private static final String TRUE_VALUE = "true";
	private static final String FALSE_VALUE = "false";

	private static final String COMP_LIST = "_compList";
	private static final String COMPONENTS = "components";
	private static final String NAME_ATTRIBUTE = "name";
	private static final String ENTRY = "entry";
	private static final String CLASS_NAME_ATTRIBUTE = "className";

	/**
	 * a document builder instance that can be used to parse docs
	 */
	private static DocumentBuilderFactory docBuilderFactory = newDocumentBuilderFactory();

	/**
	 * a dom transformer for converting xml from one form to the other
	 */
	public static final Transformer transformer = instantiateTransFactory();

	/**
	 * @param ele
	 * @return array of all child node names. This includes all attribute names
	 *         as well as all unique child-element names
	 */
	public static String[] getAllNodeNames(Element ele) {
		NamedNodeMap atts = ele.getAttributes();
		int nbrAtts = atts == null ? 0 : atts.getLength();

		int nbrChildren = 0;
		Set<String> childNames = null;
		NodeList children = ele.getChildNodes();
		if (children != null) {
			nbrChildren = children.getLength();
			childNames = new HashSet<String>();
			for (int i = 0; i < nbrChildren; i++) {
				childNames.add(children.item(i).getNodeName());
			}
			nbrChildren = childNames.size();
		}
		int total = nbrAtts + nbrChildren;
		String[] names = new String[total];
		int idx = 0;
		if (atts != null) {
			for (int i = 0; i < nbrAtts; i++) {
				names[idx] = atts.item(i).getNodeName();
				idx++;
			}
		}
		if (childNames != null) {
			for (String childName : childNames) {
				names[idx] = childName;
				idx++;
			}
		}
		return names;
	}

	/**
	 * parse a text into a document.
	 *
	 * @param text
	 * @return dom. null in case the text is not parsed properly.
	 */
	public static Document textToDoc(String text) {
		try {
			StreamSource source = new StreamSource(new StringReader(text));
			javax.xml.transform.dom.DOMResult result = new DOMResult();
			transformer.transform(source, result);
			return (Document) result.getNode();
		} catch (Exception e) {
			logger.error("xml text could not be parsed into a document. {}. An empty document is returned.",
					e.getMessage());
			return null;
		}
	}

	/**
	 * convert a dom to text
	 *
	 * @param xml
	 * @return string that can be parsed back into this dom
	 */
	public String docToText(Document xml) {
		Source source = new DOMSource(xml);
		StringWriter writer = new StringWriter();
		try {
			transformer.transform(source, new StreamResult(writer));
			return writer.toString();
		} catch (TransformerException e) {
			logger.error("Error while converting dom to text. {}. Empty string returned.", e.getMessage());
			return "";
		}
	}

	/**
	 * bind data from an xml stream into object
	 *
	 * @param stream
	 *            with valid xml
	 * @param object
	 *            instance to which the data from xml is to be loaded to
	 * @return true if all OK. false if the resource is not laoded
	 * @throws XmlParseException
	 */
	public static boolean xmlToObject(InputStream stream, Object object) throws XmlParseException {
		try {
			Element rootElement = getDocument(stream).getDocumentElement();
			elementToObject(rootElement, object);
			return true;
		} catch (Exception e) {
			logger.info("Error while reading resource " + e.getMessage());
			return false;
		}
	}

	/**
	 * bind data from an xml stream into object
	 *
	 * @param resName
	 *            relative to file manager's root, and not absolute path
	 * @param object
	 *            instance to which the data from xml is to be loaded to
	 * @return true if we are able to load. false otherwise
	 * @throws XmlParseException
	 */
	public static boolean xmlToObject(String resName, Object object) throws XmlParseException {
		try (InputStream stream = IoUtil.getStream(resName)) {
			if (stream == null) {
				logger.error("{} could not be located for reading.", resName);
				return false;
			}
			return xmlToObject(stream, object);
		} catch (Exception e) {
			logger.error("Resource " + resName + " failed to load.", e);
		}
		return false;
	}

	/**
	 * load components or name-className maps into collection
	 *
	 * @param stream
	 *            xml
	 * @param objects
	 *            collection to which components/entries are to be added to
	 * @param packageName
	 *            package name ending with a '.', so that when we add simple
	 *            class name it becomes a fully-qualified class name
	 * @throws XmlParseException
	 */
	public static void xmlToCollection(InputStream stream, Map<String, ?> objects, String packageName)
			throws XmlParseException {
		elementToCollection(getDocument(stream).getDocumentElement(), objects, packageName);
		return;
	}

	/**
	 * load components or name-className maps into collection
	 *
	 * @param element
	 *            root element
	 * @param objects
	 *            collection to which components/entries are to be added to
	 * @param packageName
	 *            package name ending with a '.', so that when we add simple
	 *            class name it becomes a fully-qualified class name
	 * @throws XmlParseException
	 */
	public static void elementToCollection(Element element, Map<String, ?> objects, String packageName)
			throws XmlParseException {
		elementToCollection(element, objects, packageName, null);
	}

	/**
	 * load components or name-className maps into collection
	 *
	 * @param element
	 *            root element
	 * @param objects
	 *            collection to which components/entries are to be added to
	 * @param packageName
	 *            package name ending with a '.', so that when we add simple
	 *            class name it becomes a fully-qualified class name
	 * @param lineNumbers
	 *            can be null. line number of teh tag for the object that is
	 *            created
	 * @throws XmlParseException
	 */
	public static void elementToCollection(Element element, Map<String, ?> objects, String packageName,
			Map<Object, String> lineNumbers)
			throws XmlParseException {
		Node node = element.getFirstChild();
		while (node != null) {
			if (node.getNodeName().equals(COMPONENTS) == false) {
				node = node.getNextSibling();
				continue;
			}
			/*
			 * we got the element we need
			 */
			Node firstNode = node.getFirstChild();
			if (firstNode == null) {
				break;
			}
			if (packageName == null) {
				/*
				 * list of name and className
				 */
				loadEntries(firstNode, objects);
			} else {
				/*
				 * list of components
				 */
				loadObjects(firstNode, objects, packageName, lineNumbers);
			}
			return;
		}

		/*
		 * we did not get the component element at all
		 */

		logger.info("XML has no components in it.");
	}

	/**
	 * load components or name-className maps into collection
	 *
	 * @param resourcePath
	 *            relative to FileManager root, and not absolute path of the
	 *            file xml
	 * @param objects
	 *            collection to which components/entries are to be added to
	 * @param packageName
	 *            package name ending with a '.', so that when we add simple
	 *            class name it becomes a fully-qualified class name
	 * @return true if we are able to load from the file. false otherwise
	 */
	public static boolean xmlToCollection(String resourcePath, Map<String, ?> objects, String packageName) {
		try (InputStream stream = IoUtil.getStream(resourcePath)) {
			if (stream == null) {
				logger.info("Unable to open file " + resourcePath + " failed to load.");
				return false;
			}
			xmlToCollection(stream, objects, packageName);
			return true;
		} catch (Exception e) {
			logger.error("Resource " + resourcePath + " failed to load.", e);
			return false;
		}
	}

	/**
	 * elements are loaded and added to objects collection
	 *
	 * @param firstNode
	 * @param objects
	 * @param packageName
	 *            includes a '.' at the end so that packageName + className is a
	 *            valid qualified classNAme
	 * @throws XmlParseException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void loadObjects(Node firstNode, Map objects, String pkgName, Map<Object, String> lineNumbers)
			throws XmlParseException {
		String packageName = pkgName;
		if (packageName.endsWith(".") == false) {
			packageName += '.';
		}
		Node node = firstNode;
		while (node != null) {
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element ele = (Element) node;
				String className = TextUtil.nameToClassName(node.getNodeName());
				Object object = fromClassName(packageName + className);
				if (object == null) {
					logger.error("Element {} not loaded as we could not get a target object insatnce for it",
							node.getNodeName());
				} else {
					String compName = ele.getAttribute(NAME_ATTRIBUTE);
					if (objects.containsKey(compName)) {
						logger.info(compName + " is a duplicate " + className + ". Component definition skipped.");
					} else {
						elementToObject(ele, object, lineNumbers);
						objects.put(compName, object);
					}
				}
			}
			node = node.getNextSibling();
		}
	}

	/**
	 * name-className pairs are added to objects collection
	 *
	 * @param firstNode
	 * @param objects
	 * @param initializeThem
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void loadEntries(Node firstNode, Map objects) {
		Node node = firstNode;
		while (node != null) {
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				String nodeName = node.getNodeName();
				if (nodeName.equals(ENTRY)) {
					Element ele = (Element) node;
					String compName = ele.getAttribute(NAME_ATTRIBUTE);
					String className = ele.getAttribute(CLASS_NAME_ATTRIBUTE);
					if (compName == null || className == null) {

						logger.info("We expect attributes " + NAME_ATTRIBUTE + " and " + CLASS_NAME_ATTRIBUTE
								+ " as attributes of element " + ENTRY + ". Element ignored");

					} else {
						if (objects.containsKey(compName)) {

							logger.info(compName + " is a duplicate entry. class name definition ignored.");

						} else {
							objects.put(compName, className);
						}
					}
				} else {

					logger.info(
							"Expecting an element named " + ENTRY + " but found " + nodeName + ". Element ignored.");
				}
			}
			node = node.getNextSibling();
		}
	}

	/**
	 * * parses a file into a DOM
	 *
	 * @param stream
	 * @return DOM for the xml that the file contains
	 */
	private static Document getDocument(InputStream stream) throws XmlParseException {
		Document doc = null;
		String msg = null;

		try {
			doc = getDocBuilder().parse(stream);
		} catch (SAXParseException e) {
			msg = "Error while parsing xml text. " + e.getMessage() + "\n At line " + e.getLineNumber() + " and column "
					+ e.getColumnNumber();
		} catch (Exception e) {
			msg = "Error while reading resource file. " + e.getMessage();
		}
		if (msg != null) {
			throw new XmlParseException(msg);
		}
		return doc;
	}

	/**
	 * * create the factory, once and for all
	 *
	 * @return Factory to create DOM
	 */
	private static DocumentBuilderFactory newDocumentBuilderFactory() {
		/*
		 * workaround for some APP servers that have classLoader related issue
		 * with using xrececs
		 */
		ClassLoader savedClassloader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(XmlUtil.class.getClassLoader());
		DocumentBuilderFactory factory = new DocumentBuilderFactoryImpl();
		Thread.currentThread().setContextClassLoader(savedClassloader);
		factory.setIgnoringComments(true);
		factory.setValidating(false);
		factory.setCoalescing(false);
		factory.setXIncludeAware(false);
		factory.setNamespaceAware(false);
		return factory;
	}

	/**
	 *
	 * @return document builder suitable for the kind of docs we use
	 */
	public static DocumentBuilder getDocBuilder() {
		try {
			return docBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			logger.error("Error while creaing a dom builder. No XML will work. ERROR : {}", e.getMessage());
			return null;
		}
	}

	/**
	 * * create the factory, once and for all
	 *
	 * @return Factory to create DOM
	 */
	private static Transformer instantiateTransFactory() {
		/*
		 * workaround for some APP servers that have classLoader related issue
		 * with using xrececs
		 */
		ClassLoader savedClassloader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(XmlUtil.class.getClassLoader());
		Transformer trans = null;
		try {
			trans = TransformerFactory.newInstance().newTransformer();
		} catch (TransformerConfigurationException e) {
			logger.error("Error while instantiating a trasformer for xml. {}", e.getMessage());
		} finally {
			Thread.currentThread().setContextClassLoader(savedClassloader);
		}
		return trans;
	}

	/**
	 * bind data from an element to the object, provided you have followed our
	 * conventions
	 *
	 * @param element
	 * @param object
	 * @throws XmlParseException
	 */
	public static void elementToObject(Element element, Object object) throws XmlParseException {

		elementToObject(element, object, null);
		return;
	}

	/**
	 * copy objects from list into map
	 *
	 * @param map
	 *            to which objects are to be added
	 * @param objects
	 *            to be added to the map
	 * @throws XmlParseException
	 */
	private static void fillMap(Map<String, Object> map, List<?> objects, String keyFieldName)
			throws XmlParseException {
		if (objects.size() == 0) {
			return;
		}
		StringBuilder msg = new StringBuilder();
		try {
			for (Object object : objects) {
				/*
				 * get the field value for indexing this object
				 */
				Field field = getField(object.getClass(), keyFieldName);
				if (field != null) {
					field.setAccessible(true);
					Object key = field.get(object);
					if (key != null) {
						map.put(key.toString(), object);
						continue;
					}
				}
				msg.append("\nUnable to get value of attribute " + keyFieldName + " for an instance of class "
						+ object.getClass().getName());
			}
		} catch (Exception e) {
			msg.append("\nError while adding a member into map using " + keyFieldName + " as key\n" + e.getMessage());
		}
		if (msg.length() > 0) {
			throw new XmlParseException(msg.toString());
		}
	}

	/**
	 * @param type
	 * @param fieldName
	 * @return
	 */
	private static Field getField(Class<?> type, String fieldName) {
		Class<?> currentType = type;
		while (!currentType.equals(Object.class)) {
			for (Field field : currentType.getDeclaredFields()) {
				if (field.getName().equals(fieldName)) {
					return field;
				}
			}
			currentType = currentType.getSuperclass();
		}
		return null;
	}

	/**
	 * get all fields for a class
	 *
	 * @param type
	 * @return all fields indexed by their names
	 */
	private static Map<String, Field> getAllFields(Class<?> type) {
		Map<String, Field> fields = new HashMap<String, Field>();
		Class<?> currentType = type;
		while (!currentType.equals(Object.class)) {
			for (Field field : currentType.getDeclaredFields()) {
				int mod = field.getModifiers();
				/*
				 * by convention, our fields should not have any modifier
				 */
				if (mod == 0 || Modifier.isProtected(mod) && !Modifier.isStatic(mod)) {
					fields.put(field.getName(), field);
				}
			}
			currentType = currentType.getSuperclass();
		}
		return fields;
	}

	/**
	 * @param element
	 * @param map
	 * @return
	 */
	private static boolean fillAttsIntoMap(Element element, Map<String, String> map) {
		/*
		 * this case is applicable only if there are no child elements
		 */
		NodeList children = element.getChildNodes();
		if (children != null && children.getLength() > 0) {
			return false;
		}

		/*
		 * in case this is not a special case, it will not have attributes, and
		 * hence we are safe
		 */
		NamedNodeMap attrs = element.getAttributes();
		if (attrs != null) {
			int n = attrs.getLength();
			for (int i = 0; i < n; i++) {
				Node att = attrs.item(i);
				map.put(att.getNodeName(), att.getNodeValue());
			}
		}
		return true;
	}

	/**
	 * special map of string string
	 *
	 * @param element
	 *            whose child nodes are map entries
	 * @param map
	 *            map
	 */
	private static void fillMap(Element element, Map<String, String> map) {
		String valueName = null;
		Node child = element.getFirstChild();
		while (child != null) {
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element) child;
				if (valueName == null) {
					NamedNodeMap attribs = child.getAttributes();
					if (attribs == null || attribs.getLength() != 2) {
						throw new ApplicationError("Special element " + COMP_LIST
								+ " should have child nodes with just two attributes, name and value");
					}
					String attrName = attribs.item(0).getNodeName();
					if (attrName.equals(DEFAULT_MAP_KEY)) {
						valueName = attribs.item(1).getNodeName();
					} else {
						if (attribs.item(1).getNodeName().equals(DEFAULT_MAP_KEY) == false) {
							throw new ApplicationError("Special element " + COMP_LIST
									+ " should have child nodes with just two attributes, name and value");
						}
						valueName = attrName;
					}
				}
				String key = childElement.getAttribute(DEFAULT_MAP_KEY);
				String value = childElement.getAttribute(valueName);
				if (key == null || value == null) {
					throw new ApplicationError("key or value missing for a value map");
				}
				map.put(key, value);
			}
			child = child.getNextSibling();
		}
	}

	/**
	 * set attributes from an element as primitive values of fields
	 *
	 * @param object
	 *            to which fields are assigned
	 * @param fields
	 *            collection of all fields for this class
	 * @param element
	 *            that has the attributes
	 * @throws XmlParseException
	 * @throws DOMException
	 */
	private static void setAttributes(Object object, Map<String, Field> fields, Element element)
			throws DOMException, XmlParseException {
		NamedNodeMap attributes = element.getAttributes();
		if (attributes == null) {
			return;
		}
		int nbr = attributes.getLength();
		for (int i = 0; i < nbr; i++) {
			Node attribute = attributes.item(i);
			String fieldName = attribute.getNodeName();
			Field field = fields.get(fieldName);
			if (field != null) {
				ReflectUtil.setPrimitive(object, field, attribute.getNodeValue().trim());
			}
		}
	}

	/**
	 * Check if the element contains just a text/cdata, in which case return
	 * that value. Else return null;
	 *
	 * @param element
	 * @return null if this is not a textElement as we see it. Value of single
	 *         text/CData child otherwise
	 */
	private static String getElementValue(Element element) {
		NamedNodeMap attribs = element.getAttributes();
		if (attribs != null && attribs.getLength() > 0) {
			return null;
		}

		NodeList children = element.getChildNodes();
		if (children == null) {
			return null;
		}
		String value = null;
		int nbrChildren = children.getLength();
		for (int i = 0; i < nbrChildren; i++) {
			Node child = children.item(i);
			short childType = child.getNodeType();
			if (childType == Node.ELEMENT_NODE) {
				return null;
			}
			if (childType == Node.CDATA_SECTION_NODE || childType == Node.TEXT_NODE) {
				if (value != null) {
					return null;
				}
				value = child.getNodeValue();
			}
		}
		return value;
	}

	/**
	 * write serialized object as an xml as per our object-xml mapping
	 * convention
	 *
	 * @param outStream
	 * @param object
	 * @return true if we succeeded in writing to the stream.
	 */
	public static boolean objectToXml(OutputStream outStream, Object object) {
		String eleName = object.getClass().getSimpleName();
		eleName = eleName.substring(0, 1).toLowerCase() + eleName.substring(1);
		try {
			Document doc = getDocBuilder().newDocument();
			Element ele = doc.createElementNS("http://www.simplity.org/schema", eleName);
			objectToEle(object, doc, ele);
			doc.appendChild(ele);
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(outStream);
			Transformer trans = TransformerFactory.newInstance().newTransformer();
			trans.setOutputProperty(OutputKeys.INDENT, "yes");
			trans.transform(source, result);
			return true;
		} catch (Exception e) {

			logger.error(eleName + " could not be saved as xml. ", e);

			return false;
		}
	}

	/**
	 * add all fields of an object to the elements as attribute/elements
	 *
	 * @param object
	 * @param doc
	 * @param defaultEle
	 *            if null, new element is created for this object. Else this
	 *            element is used
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @return element defaultEle, or new one
	 * @throws XmlParseException
	 */
	private static Element objectToEle(Object object, Document doc, Element defaultEle)
			throws IllegalArgumentException, IllegalAccessException, XmlParseException {
		Class<?> objectType = object.getClass();
		// Tracer.trace("Gong to create an element for a " +
		// objectType.getSimpleName());
		/*
		 * create element if required
		 */
		Element ele = null;
		if (defaultEle != null) {
			ele = defaultEle;
		} else {
			String eleName = objectType.getSimpleName();
			eleName = eleName.substring(0, 1).toLowerCase() + eleName.substring(1);
			ele = doc.createElement(eleName);
		}

		for (Field field : getAllFields(object.getClass()).values()) {
			field.setAccessible(true);
			Object value = field.get(object);

			if (value == null) {
				// Tracer.trace("Field " + field.getName() + " has no value.");
				continue;
			}

			String fieldName = field.getName();
			Class<?> type = field.getType();

			if (type.isEnum()) {
				String stringValue = TextUtil.constantToValue(value.toString());
				ele.setAttribute(fieldName, stringValue);
				continue;
			}

			if (ReflectUtil.isValueType(type)) {
				// Tracer.trace("Field " + fieldName + " has a primitive value
				// of " + value);
				String stringValue = nonDefaultPrimitiveValue(value);
				if (stringValue != null) {
					ele.setAttribute(fieldName, stringValue);
				}
				continue;
			}

			if (type.isArray()) {
				Object[] objects = (Object[]) value;
				// Tracer.trace("Field " + fieldName + " is an array with a
				// length = " + objects.length);
				if (objects.length == 0) {
					continue;
				}

				/*
				 * array of primitives is converted into comma separated string
				 */
				if (ReflectUtil.isValueType(type.getComponentType())) {
					StringBuilder sbf = new StringBuilder(primitiveValue(objects[0]));
					for (int i = 1; i < objects.length; i++) {
						sbf.append(',').append(primitiveValue(objects[i]));
					}
					ele.setAttribute(fieldName, sbf.toString());
					continue;
				}
				/*
				 * an element with this field name is added. Objects in the
				 * array are added as child elements of that element
				 */
				Element objectEle = doc.createElement(fieldName);
				ele.appendChild(objectEle);
				// Tracer.trace("field " + fieldName + " is added as an element
				// and not as an attribute");
				for (Object obj : objects) {
					if (obj == null) {
						// Tracer.trace("An element of array " + fieldName + "
						// is null. Ignored.");
					} else {
						objectEle.appendChild(objectToEle(obj, doc, null));
					}
				}
				continue;
			}
			/*
			 * an element with field name with the map contents as child
			 * elements
			 */
			if (value instanceof Map) {
				Map<?, ?> objects = (Map<?, ?>) value;
				// Tracer.trace("Field " + fieldName + " is a MAP with size = "
				// + objects.size());
				if (objects.size() == 0) {
					continue;
				}
				Element objectEle = doc.createElement(fieldName);
				ele.appendChild(objectEle);
				for (Object obj : objects.values()) {
					if (obj == null) {
						// Tracer.trace("An element of array " + fieldName + "
						// is null. Ignored.");
					} else {
						objectEle.appendChild(objectToEle(obj, doc, null));
					}
				}
				continue;
			}
			/*
			 * it is another object. we have an element with the field name,
			 * with one child element for this object
			 */
			Element objectEle = doc.createElement(fieldName);
			// Tracer.trace("Field " + fieldName + " is an object. An element is
			// added for that.");
			ele.appendChild(objectToEle(value, doc, objectEle));
		}
		return ele;
	}

	/**
	 * @param value
	 * @return text value of the primitive
	 */
	private static String primitiveValue(Object value) {
		/*
		 * just that we expect 80% calls where value is String.
		 */
		if (value instanceof String) {
			return value.toString();
		}

		if (value instanceof LocalDate) {
			return value.toString();
		}

		Class<?> type = value.getClass();
		if (type.isEnum()) {
			return TextUtil.constantToValue(value.toString());
		}
		if (type.equals(boolean.class)) {
			if (((Boolean) value).booleanValue()) {
				return TRUE_VALUE;
			}
			return FALSE_VALUE;
		}
		/*
		 * no floats please. attributes have to be double.
		 */
		if (type.equals(double.class)) {
			/*
			 * piggyback on decimal value for formatting?
			 */
			return Value.newDecimalValue(((Double) value).doubleValue()).toString();
		}
		return value.toString();
	}

	/**
	 * @param value
	 * @return text value of the primitive, if it is not the default (empty,
	 *         false and 0)
	 */
	private static String nonDefaultPrimitiveValue(Object value) {
		if (value == null) {
			return null;
		}
		/*
		 * just that we expect 80% calls where value is String.
		 */
		if (value instanceof String) {
			String s = value.toString();
			if (s.isEmpty()) {
				return null;
			}
			return s;
		}

		if (value instanceof Number) {
			long nbr = ((Number) value).longValue();
			if (nbr == 0) {
				return null;
			}
			return value.toString();
		}
		if (value instanceof Boolean) {
			if (((Boolean) value).booleanValue()) {
				return TRUE_VALUE;
			}
			return null;
		}

		if (value.getClass().isEnum()) {
			return TextUtil.constantToValue(value.toString());
		}
		return value.toString();
	}

	/**
	 * @param object
	 * @return xml text for the object's data-state, or null in ase of any error
	 */
	public static String objectToXmlString(Object object) {
		try (OutputStream out = new ByteArrayOutputStream()) {
			objectToXml(out, object);
			return out.toString();
		} catch (Exception e) {
			logger.error("Error while converting xml object to text", e);
			return null;
		}
	}

	/**
	 * extract all attributes from the root node. We extract attributes, as well
	 * as simple elements as fields. This is not suitable for arbitrary
	 * object/data structure with multiple levels
	 *
	 * @param xml
	 * @param fields
	 * @return number of fields extracted
	 */
	public static int extractAll(String xml, IFieldsCollection fields) {
		try (InputStream is = new ByteArrayInputStream(xml.getBytes(AppConventions.CHAR_ENCODING))) {
			/*
			 * get the doc first
			 */
			Node node = getDocument(is).getDocumentElement();

			/*
			 * data could be modeled as attributes...
			 */
			int nbrExtracted = 0;
			NamedNodeMap attrs = node.getAttributes();
			if (attrs != null) {
				int n = attrs.getLength();
				for (int i = 0; i < n; i++) {
					Node att = attrs.item(i);
					fields.setValue(att.getNodeName(), Value.newTextValue(att.getNodeValue()));
				}
				nbrExtracted += n;
			}
			/*
			 * data could also be modeled as elements with just text data.
			 */
			NodeList childs = node.getChildNodes();
			if (childs != null) {
				int n = childs.getLength();
				for (int i = 0; i < n; i++) {
					Node child = childs.item(i);
					if (child.getNodeType() == Node.ELEMENT_NODE) {
						String value = ((Element) child).getTextContent();
						fields.setValue(child.getNodeName(), Value.newTextValue(value));
					}
				}
				nbrExtracted += n;
			}

			logger.info(nbrExtracted + " fields extracted from root node. " + node.getNodeName());

			return nbrExtracted;
		} catch (Exception e) {
			throw new ApplicationError(e, " Error while extracting fields from an xml.\n" + xml);
		}
	}

	/*
	 * ******************* needs refactoring. COpied from new utils
	 * ********************
	 */
	/**
	 * get the root dom element from the document loaded from the specified
	 * resource
	 *
	 * @param resName
	 *            resource name from where xml is to be loaded. Must be able to
	 *            get a URL from this
	 * @return root element of the xml. null in case of any error. Also null if
	 *         the xml is empty and has no root element
	 */
	public static Element rootElementFromResource(String resName) {
		Document doc = fromResource(resName);
		if (doc == null) {
			return null;
		}
		Element xml = doc.getDocumentElement();
		if (xml == null) {
			logger.error("Document is empty.");
			return null;
		}
		logger.info("XML document with root node {} read.", xml.getTagName());
		return xml;
	}

	/**
	 * get the document loaded from the specified resource
	 *
	 * @param resName
	 *            resource name from where xml is to be loaded. Must be able to
	 *            get a URL from this
	 * @return document. null in case of any error.
	 */
	public static Document fromResource(String resName) {
		try (InputStream ins = IoUtil.getStream(resName)) {
			if (ins == null) {
				logger.error("Unable to create stream from resource {}. Resource may not exist", resName);
				return null;
			}
			return fromStream(ins);
		} catch (Exception e) {
			logger.error("Error while loading resource {} as an xml. Treating this as internal error. {}", resName,
					e.getMessage());
			return null;
		}
	}

	/**
	 * get the document loaded from the specified resource
	 *
	 * @param stream
	 * @return document or null in case of any error
	 */
	public static Document fromStream(InputStream stream) {
		try {
			Source source = new StreamSource(stream);
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			DOMResult res = new DOMResult(doc);
			transformer.transform(source, res);
			return doc;
		} catch (Exception e) {
			logger.error("Error while loading xml from stream. Treating this as internal error. {}", e.getMessage());
			return null;
		}
	}

	/**
	 *
	 * @param doc
	 * @param stream
	 * @return true if all ok. false in case of any error
	 */
	public static boolean toStream(Document doc, OutputStream stream) {
		try {
			Source source = new DOMSource(doc);
			Result xmlResult = new StreamResult(stream);
			transformer.transform(source, xmlResult);
			return true;
		} catch (Exception e) {
			logger.error("Document could not be Written to output stream. {}", e.getMessage());
			return false;
		}
	}

	/**
	 *
	 * @param rootElement
	 *            root dom element to be written into an xml
	 * @param stream
	 * @return true if all ok. false in case of any error
	 */
	public static boolean toStream(Element rootElement, OutputStream stream) {
		try {
			Source source = new DOMSource(rootElement);
			Result xmlResult = new StreamResult(stream);
			transformer.transform(source, xmlResult);
			return true;
		} catch (Exception e) {
			logger.error("Document could not be Written to output stream. {}", e.getMessage());
			return false;
		}
	}

	/**
	 *
	 * @param doc
	 * @param writer
	 * @return true if all ok. false in case of any error
	 */
	public static boolean toStream(Document doc, Writer writer) {
		try {
			Source source = new DOMSource(doc);
			Result xmlResult = new StreamResult(writer);
			transformer.transform(source, xmlResult);
			return true;
		} catch (Exception e) {
			logger.error("Document could not be Written to output stream. {}", e.getMessage());
			return false;
		}
	}

	/**
	 *
	 * @param rootElement
	 *            root dom element to be written into an xml
	 * @param writer
	 * @return true if all ok. false in case of any error
	 */
	public static boolean toStream(Element rootElement, Writer writer) {
		try {
			Source source = new DOMSource(rootElement);
			Result xmlResult = new StreamResult(writer);
			transformer.transform(source, xmlResult);
			return true;
		} catch (Exception e) {
			logger.error("Document could not be Written to output stream. {}", e.getMessage());
			return false;
		}
	}

	/**
	 *
	 * @param doc
	 * @return String form of xml. null in case of error
	 */
	public static String docToString(Document doc) {
		try {
			Source source = new DOMSource(doc);
			Writer writer = new StringWriter();
			Result xmlResult = new StreamResult(writer);
			transformer.transform(source, xmlResult);
			return writer.toString();
		} catch (Exception e) {
			logger.error("Document could not be converted to String. {}", e.getMessage());
			return null;
		}
	}

	/**
	 *
	 * @param rootElement
	 *            root dom element to be converted
	 * @return dom string for the root element. null in case of error
	 */
	public static String eleToString(Element rootElement) {
		try {
			Source source = new DOMSource(rootElement);
			Writer writer = new StringWriter();
			Result xmlResult = new StreamResult(writer);
			transformer.transform(source, xmlResult);
			return writer.toString();
		} catch (Exception e) {
			logger.error("Document could not be Written to output stream. {}", e.getMessage());
			return null;
		}
	}

	/**
	 * get the document loaded from the specified resource
	 *
	 * @param reader
	 * @return document or null in case of any error
	 */
	public static Document fromStream(Reader reader) {
		try {
			Source source = new StreamSource(reader);
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			DOMResult res = new DOMResult(doc);
			transformer.transform(source, res);
			return doc;
		} catch (Exception e) {
			logger.error("Error while loading xml from stream. Treating this as internal error. {}", e.getMessage());
			return null;
		}
	}

	/**
	 * bind data from an element to the object, provided you have followed our
	 * conventions
	 *
	 * @param element
	 * @param object
	 * @param lineNumbers
	 *            map to which line numbers are to be added for each element
	 *            that is converted into object. null if this feature is not
	 *            required
	 * @throws XmlParseException
	 */
	public static void elementToObject(Element element, Object object, Map<Object, String> lineNumbers)
			throws XmlParseException {
		Map<String, Field> fields = ReflectUtil.getAllFields(object);
		if (lineNumbers != null) {
			addLineNo(object, element, lineNumbers);
		}
		/*
		 * attributes of the element are mapped to value/primitive fields
		 */
		setAttributes(object, fields, element);

		/*
		 * child elements could be either primitive or a class
		 */
		Node child = element.getFirstChild();
		while (child != null) {
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Field field = fields.get(child.getNodeName());
				if (field != null) {
					Element childElement = (Element) child;
					String value = getElementValue(childElement);
					if (value != null) {
						/*
						 * element had a primitive value
						 */
						ReflectUtil.setPrimitive(object, field, value);
					} else {
						/*
						 * element represents another object
						 */
						setObject(object, field, childElement, lineNumbers);
					}
				} else {
					logger.info("xml element " + child.getNodeName()
							+ " is ignored because there is no target field with that name");
				}
			}
			child = child.getNextSibling();
		}
	}

	private static void addLineNo(Object object, Element element, Map<Object, String> lineNumbers) {
		String ln = element.getAttribute(ATTR_LINE_NUMBER);
		if (ln == null) {
			logger.info(
					"Element {} does not have vallue for attribute {}, and hence line number for this object is nod added to the map",
					element.getNodeName(), ATTR_LINE_NUMBER);
		} else {
			lineNumbers.put(object, ln);
		}

	}

	/**
	 * parse element into an object and set it as value of the field.
	 *
	 * @param object
	 *            of which this is a field
	 * @param field
	 *            to which object value is to be assigned to
	 * @param element
	 *            from which object is to be parsed
	 * @param lineNumbers
	 * @throws XmlParseException
	 */
	@SuppressWarnings("unchecked")
	private static void setObject(Object object, Field field, Element element, Map<Object, String> lineNumbers)
			throws XmlParseException {
		field.setAccessible(true);
		Object fieldObject = null;
		try {
			Class<?> fieldType = field.getType();
			if (fieldType.isArray()) {
				Class<?> componentType = fieldType.getComponentType();
				List<?> objects = elementToList(element, componentType, object, lineNumbers);
				if (objects == null || objects.size() == 0) {
					return;
				}
				int nbr = objects.size();
				fieldObject = Array.newInstance(componentType, nbr);
				for (int i = 0; i < nbr; i++) {
					Array.set(fieldObject, i, objects.get(i));
				}
				field.set(object, fieldObject);
				if (lineNumbers != null) {
					addLineNo(fieldObject, element, lineNumbers);
				}
				return;
			}

			fieldObject = field.get(object);
			/*
			 * if the field is already initialized, it is Map or Concrete class
			 * object
			 */
			if (fieldObject != null) {
				if (fieldObject instanceof Map) {
					if (lineNumbers != null) {
						addLineNo(fieldObject, element, lineNumbers);
					}
					/*
					 * we have a special case of componentList
					 */
					if (field.getName().equals(COMP_LIST)) {
						fillMap(element, (Map<String, String>) fieldObject);
						return;
					}
					/*
					 * another special case of attr-value of element itself
					 * being saved as map
					 */
					if (fillAttsIntoMap(element, (Map<String, String>) fieldObject)) {
						return;
					}
					String mapKey = DEFAULT_MAP_KEY;
					Class<?> refClass = null;
					FieldMetaData ante = field.getAnnotation(FieldMetaData.class);
					if (ante != null) {
						String txt = ante.indexFieldName();
						if (txt != null && txt.isEmpty() == false) {
							mapKey = txt;
						}
						refClass = ante.memberClass();
						if (refClass.equals(Object.class)) {
							refClass = object.getClass();
						}
					} else {
						refClass = object.getClass();
					}
					List<?> objects = elementToList(element, refClass, object, lineNumbers);
					fillMap((Map<String, Object>) fieldObject, objects, mapKey);
				} else {
					elementToObject(element, fieldObject, lineNumbers);
				}
				return;
			}
			if (fieldType.isInterface() || Modifier.isAbstract(fieldType.getModifiers())) {
				/*
				 * It is super class/interface. As per our syntax, this element
				 * would be wrapper for the concrete class-element
				 */
				fieldObject = elementWrapperToSubclass(element, field, object, lineNumbers);
				if (fieldObject == null) {
					logger.info("No instance provided for field " + field.getName());
				} else {
					field.set(object, fieldObject);
				}
				return;
			}
			/*
			 * we have an object as the child
			 */
			fieldObject = fieldType.newInstance();
			elementToObject(element, fieldObject, lineNumbers);
			field.set(object, fieldObject);
		} catch (Exception e) {
			throw new XmlParseException("Error while binding xml to object : " + e.getMessage());
		}
	}

	/**
	 * parse child elements of the passed element into a List
	 *
	 * @param element
	 *            parent element
	 * @param field
	 *            to which this list is destined for. Used to check for
	 *            annotation for package.
	 * @param referenceType
	 *            class, super-class or parent class of the expected object.
	 *            Used to get the package name.
	 * @param parentObject
	 *            parent of this object
	 * @param lineNumbers
	 * @return
	 * @throws XmlParseException
	 */
	private static List<?> elementToList(Element element, Class<?> referenceType, Object parentObject,
			Map<Object, String> lineNumbers)
			throws XmlParseException {
		List<Object> objects = new ArrayList<Object>();
		Node child = element.getFirstChild();
		while (child != null) {
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Object object = elementToSubclass((Element) child, referenceType, parentObject, lineNumbers);
				if (object == null) {
					logger.error("Node {} not parsed because we could not get an instane of target object",
							child.getNodeName());
				} else {
					objects.add(object);
				}
			}
			child = child.getNextSibling();
		}
		return objects;
	}

	/**
	 * @param element
	 *            with concrete class name as its tag. This class is expected to
	 *            be in the same package as referenceType, unless the field is
	 *            annotated with the package name. A special attribute name
	 *            "_class", if present would be the class name
	 * @param referenceType
	 *            non-null a class whose package may be shared with this class
	 * @param parentObject
	 *            parent of this object
	 * @param lineNumbers
	 * @return
	 * @throws XmlParseException
	 */
	private static Object elementToSubclass(Element element, Class<?> referenceType, Object parentObject,
			Map<Object, String> lineNumbers)
			throws XmlParseException {

		Object thisObject = null;
		String className = TextUtil.nameToClassName(element.getTagName());
		String packageName = referenceType.getPackage().getName();
		/*
		 * we take package name either from referenceType or parent object
		 */
		thisObject = fromClassName(packageName + '.' + className);
		if (thisObject != null) {
			elementToObject(element, thisObject, lineNumbers);
		}
		return thisObject;
	}

	private static Object fromClassName(String className) {
		Class<?> cls = null;
		Object object = null;
		try {
			cls = Class.forName(className);
			object = cls.newInstance();
		} catch (Exception e) {
			if (cls == null) {
				logger.error("clssName {} could not be used to get a class. Are you missing any jar file?", className);
			} else {
				if (cls.isInterface()) {
					logger.error("{} is an interface. Object can not be instantiated with an interface", className);
				} else if (Modifier.isAbstract(cls.getModifiers())) {
					logger.error("{} is an abstract class. Object can not be instantiated with an interface",
							className);
				} else {
					logger.error("A new object instance could not be created for class {}", className);
				}
			}
			logger.error(e.getMessage());
		}
		return object;
	}

	/**
	 * element with field name as tag followed with a child element with the
	 * concrete class name as tag
	 *
	 * @param wrapper
	 *            element with field name as tag
	 * @param field
	 *            to which this object is to be assigned to
	 * @param lineNumbers
	 * @return
	 * @throws XmlParseException
	 */
	private static Object elementWrapperToSubclass(Element wrapper, Field field, Object parentObject,
			Map<Object, String> lineNumbers)
			throws XmlParseException {

		/*
		 * though we expect exactly one element, you never know about comments
		 */
		Node element = wrapper.getFirstChild();
		while (element != null) {
			if (element.getNodeType() == Node.ELEMENT_NODE) {
				return elementToSubclass((Element) element, field.getType(), parentObject, lineNumbers);
			}
			element = element.getNextSibling();
		}
		return null;
	}

	/**
	 * @return an empty document
	 */
	public static Document newEmptyDocument() {
		return getDocBuilder().newDocument();
	}

	/**
	 * pretty print an xml element
	 *
	 * @param domElement
	 *            to be pretty printed
	 * @return pretty printed document
	 */
	public static String prettyPrint(Element domElement) {
		StringWriter writer = new StringWriter();
		try {
			transformer.transform(new DOMSource(domElement), new StreamResult(writer));
			return writer.toString();
		} catch (TransformerException e) {
			return "Error while pretty printing the xml document with root node " + domElement.getTagName();
		}
	}
}
