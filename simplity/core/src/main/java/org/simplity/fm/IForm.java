/*
 * Copyright (c) 2019 simplity.org
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

package org.simplity.fm;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * This is the core of the forms-management (fm) module. It handles the
 * following aspects of form management
 * <ul>
 * <li>De-serialize data being transmitted/persisted as string into proper data
 * elements and populate itself</li>
 * <li>Validate data being populated and provide error messages on any
 * validation failures</li>
 * <li>allow services to get/set values</li>
 * <li>serialize data into string that can be persisted/transmitted that can be
 * de-serialized back</li>
 * <li>form also supports data exchange with standard data carriers like JSON
 * and XML (functionality to be added on a need basis)</li>
 * </ul>
 * A form can contain values (fields) and <code>IGrid</code>s. No arbitrary
 * object structure is allowed. Only exception is that a form can simply contain
 * other form, but just one level. Such a form is called CompositeForm.
 * <br/>
 * <br/>
 * Form is expected to contain small amount of data, and hence no methods
 * provided for streaming data. It deals with strings and objects instead
 * 
 * @author simplity.org
 *
 */
public interface IForm {
	/**
	 * unique id assigned to this form. like customerDetails. This is unique
	 * across all types of forms within a project
	 * 
	 * @return non-null unique id
	 */
	public String getFormId();

	/**
	 *
	 * @return unique key/id for the document/record for which this form is
	 *         currently having data. It would be typically formed based on the
	 *         primary key(s) of the underlying document
	 */
	public String getDocumentId();

	/**
	 * de-serialize text into data. used when the data is known to be valid, and
	 * need not be validated. Typically when this is de-serialized from
	 * persistence layer.
	 * 
	 * @param data
	 *            text that is the result of serialize() of this DataStructure.
	 * 
	 */
	public void deserialize(String data);

	/**
	 * de-serialize with validations
	 * 
	 * @param data
	 *            coming from a client
	 * @param errors
	 *            list to which, any validation errors are added
	 * @return true if allOk. No errors are added to the list. False in case one
	 *         or more validation errors are added to the list
	 */
	public boolean deserialize(String data, List<Message> errors);

	/**
	 * 
	 * @return a string that contains all the data from this data-structure.
	 *         This string can be used to transmit all data across
	 *         layers/network and can be de-serialized back to this data
	 *         structure
	 */
	public String serialize();

	/**
	 * load from a JSON node with no validation. To be called when loading from
	 * a dependable source
	 * 
	 * @param json
	 */
	public void load(ObjectNode json);

	/**
	 * load from a JSON node that is not dependable. Like input from a client
	 * 
	 * @param values
	 *            non-null
	 * @param errors
	 *            non-null to which any validation errors are added
	 */
	public void validateAndLoad(Map<String, String> values, List<Message> errors);

	/**
	 * load from a JSON node that is not dependable. Like input from a client
	 * 
	 * @param jsonNode
	 *            non-null
	 * @param errors
	 *            non-null to which any validation errors are added
	 */
	public void validateAndLoad(ObjectNode jsonNode, List<Message> errors);

	/**
	 * @param outs 
	 * @throws IOException 
	 */
	public void serializeAsJson(OutputStream outs)  throws IOException ;

	/**
	 * 
	 * @param fieldName
	 * @return null if there is no such field. number returned as text in case
	 *         the field value is numeric
	 */
	public String getValue(String fieldName);

	/**
	 * 
	 * @param fieldName
	 * @return value of the field as a number. 0 if it is not a number, or it s
	 *         not a field
	 */
	public long getLongValue(String fieldName);

	/**
	 * to be used by service layer. Value is NOT validated
	 * 
	 * @param fieldName
	 *            name of the field
	 * @param value
	 *            will be converted to long if required
	 * @return true if value was indeed set. false if field is not defined.
	 */
	public boolean setValue(String fieldName, String value);

	/**
	 * to be used by service layer. Value is NOT validated
	 * 
	 * @param fieldName
	 *            name of the field
	 * @param value
	 *            will be converted to text if required
	 * @return true if value was indeed set. false if field is not defined
	 */
	public boolean setLongValue(String fieldName, long value);
}
