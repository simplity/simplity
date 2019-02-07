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

import org.simplity.kernel.data.IDataSheet;
import org.simplity.kernel.value.Value;

/**
 *
 * @author simplity.org
 *
 */
public interface IResponseWriter {
	/**
	 * write a field as attribute-value pair. Simplity deals with primitives as
	 * Value objects, and hence a specific method for this. use object() to set
	 * non-value attributes
	 *
	 * @param fieldName
	 * @param value
	 * @return writer, so that methods can be cascaded
	 */
	public IResponseWriter setField(String fieldName, Value value);

	/**
	 * write a field as attribute-value pair where value is primitive data (not
	 * array or object of other data)
	 *
	 * @param fieldName
	 * @param value
	 *            is primitive, including date and string. It is not an
	 *            arbitrary object
	 * @return writer, so that methods can be cascaded
	 */
	public IResponseWriter setField(String fieldName, Object value);

	/**
	 * A non-primitive object. Writer may reject object assignments that it does
	 * not know how to handle.
	 *
	 * @param fieldName
	 * @param obj
	 * @return writer, so that methods can be cascaded
	 */
	public IResponseWriter setObject(String fieldName, Object obj);

	/**
	 * write a name-array
	 *
	 * @param arrayName
	 * @param arr
	 * @return writer, so that methods can be cascaded
	 */
	public IResponseWriter setArray(String arrayName, Object[] arr);

	/**
	 * use the first column of the data sheet as an array
	 *
	 * @param arrayName
	 * @param sheet
	 * @return writer, so that methods can be cascaded
	 */
	public IResponseWriter setArray(String arrayName, IDataSheet sheet);

	/*
	 * It may be expensive to build data sheet and then write them out.
	 * Especially if we are dealing with hierarchical data. So we provide
	 * methods to write them out as and when the lower level data is available,
	 * there by avoiding creation of large data-objects
	 */
	/**
	 * start name-object pair, and allow its attributes to be written out
	 *
	 * @param objectName
	 * @return writer, so that methods can be cascaded
	 */
	public IResponseWriter beginObject(String objectName);

	/**
	 * start an object as an element of an array (hence no name)
	 *
	 * @return writer, so that methods can be cascaded
	 */
	public IResponseWriter beginObjectAsArrayElement();

	/**
	 * close the last open object
	 *
	 * @return writer, so that methods can be cascaded
	 */
	public IResponseWriter endObject();

	/**
	 * begin an array as an attribute (hence name)
	 *
	 * @param arrayName
	 * @return writer, so that methods can be cascaded
	 */
	public IResponseWriter beginArray(String arrayName);

	/**
	 * begin an array as an element of parent array (hence no name)
	 *
	 * @return writer, so that methods can be cascaded
	 */
	public IResponseWriter beginArrayAsArrayElement();

	/**
	 * close the array
	 *
	 * @return writer, so that methods can be cascaded
	 */
	public IResponseWriter endArray();

	/**
	 * write a field as an element of an array (hence no name). this is valid
	 * only when a beginArray() is active.
	 *
	 * @param value
	 * @return writer, so that methods can be cascaded
	 */
	public IResponseWriter addToArray(Object value);

	/**
	 * extremely important to call this when you are done, so that end tags if
	 * required, are written out
	 */
	public void done();
}
