/*
 * Copyright (c) 2017 simplity.org
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

package org.simplity.kernel.app;

import org.simplity.kernel.service.ServiceContext;

/**
 * translate data from data source to service context based on
 * InputDataSpecification, or create output data from service context based on
 * OutputData specification
 *
 * @author simplity.org
 *
 */
public interface IRequestReader {

	/**
	 * get the value type of the input that is received with this name
	 *
	 * @param attributeName
	 * @return type of value of this attribute
	 */
	public InputValueType getValueType(String attributeName);

	/**
	 * @param attributeName
	 * @return value of the attribute. null if this is not an object, or there
	 *         is no such attribute
	 */
	public Object getValue(String attributeName);

	/**
	 * open an object
	 *
	 * @param attributeName
	 * @return true if all ok. false if this is not an object, or there is no
	 *         such attribute, or the value of that attribute is not an object
	 */
	public boolean openObject(String attributeName);

	/**
	 * open an object at this position in the array
	 *
	 * @param zeroBasedIdx
	 * @return true if we could do it. false in case this is not an array, the
	 *         index is out-of-bound, or it is not an object at that spot
	 */
	public boolean openObject(int zeroBasedIdx);

	/**
	 * close an object that was opened recently. Ensure that your open-close
	 * calls are well co-ordinated
	 *
	 * @return true if we did close
	 */
	public boolean closeObject();

	/**
	 * open array with this attribute name
	 *
	 * @param attributetName
	 * @return true if we could do it. false if this attriute does not exist, or
	 *         if it is not an array
	 */
	public boolean openArray(String attributetName);

	/**
	 * open the array for reading at the specified position
	 *
	 * @param zeroBasedIdx
	 * @return true if we could do that. Do expect unexpected behavior if
	 *         open-close are not properly managed :-)
	 */
	public boolean openArray(int zeroBasedIdx);

	/**
	 * get primitive value at the specified position of the array
	 *
	 * @param zeroBasedIdx
	 * @return primitive value, or null in case this is not an array, or the
	 *         index is out-of-bound. Any non-null, non-primitive object is
	 *         converted to text and returned as string
	 */
	public Object getValue(int zeroBasedIdx);

	/**
	 * get the value type of the element at specified position
	 *
	 * @param zeroBasedIdx
	 * @return value type, or null if the index is out-of-range
	 */
	public InputValueType getValueType(int zeroBasedIdx);

	/**
	 * get number of elements in the array valid only if an array is open
	 *
	 * @return number of elements. 0 if this is not an array, or the array is
	 *         empty
	 */
	public int getNbrElements();

	/**
	 * get names of attributes for this object. valid only if an object is open
	 *
	 * @return array of attribute names. Empty array, if this is not an object,
	 *         or the object is empty
	 */
	public String[] getAttributeNames();

	/**
	 * @return true if we did close
	 */
	public boolean closeArray();

	/**
	 * read all data into service context. This should be used only if we are
	 * sure to have protected ourselves against arbitrary data coming in from
	 * client. Should be used ONLY IF the reader itself driven by input
	 * specification, like open-api or protobuff. Another use-case is for
	 * utility services used internally.
	 *
	 * @param ctx
	 */
	public void pushDataToContext(ServiceContext ctx);

	/**
	 * type of value received as input
	 *
	 * @author simplity.org
	 *
	 */
	public enum InputValueType {
		/**
		 * no value
		 */
		NULL,
		/**
		 * primitive value text, number, boolean or date-time
		 */
		VALUE,
		/**
		 * on object, other than an array
		 */
		OBJECT,
		/**
		 * array of objects or values
		 */
		ARRAY,

		/**
		 * array or object. In xml, array is nothing but repeated tag. Hence
		 * even if there is exactly one tag, then we can consider it as object,
		 * as well as array
		 */
		ARRAY_OR_OBJECT
	}
}
