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
package org.simplity.kernel.adapter;

import java.util.Date;

/**
 * target that can receive data from an adapter
 *
 * @author simplity.org
 *
 */
public interface IDataTarget {
	/**
	 * receive a primitive value. Triggered ONLY IF the value is non-null. Data
	 * Adapter is designed to call set methods only if the data is non-null
	 *
	 * @param fieldName
	 *            non-null name can be of the form a.b.c to go thru the object
	 *            hierarchy
	 * @param fieldValue
	 *            non-null string.
	 */
	public void setPrimitiveValue(String fieldName, String fieldValue);

	/**
	 * receive a date value. called only if the vaoue is non-null
	 *
	 * @param fieldName
	 *            non-null name can be of the form a.b.c to go thru the object
	 *            hierarchy
	 * @param fieldValue
	 *            non-null value
	 */
	public void setDateValue(String fieldName, Date fieldValue);

	/**
	 * receive a data structure
	 *
	 * @param fieldName
	 *            non-null name can be of the form a.b.c to go thru the object
	 *            hierarchy
	 * @param value
	 *            non-null object representation of data structure
	 *
	 */
	public void setStruct(String fieldName, Object value);

	/**
	 *
	 * @param fieldName
	 *            non-null name can be of the form a.b.c to go thru the object
	 *            hierarchy
	 * @param childClassName
	 *            can be null, or a valid class name that can be used to
	 *            instantiate an instance
	 * @return a target suitable to receive data from another object, or null if
	 *         such a target can not be created
	 */
	public IDataTarget getChildTarget(String fieldName, String childClassName);

	/**
	 *
	 * @param fieldName
	 *            non-null name can be of the form a.b.c to go thru the object
	 *            hierarchy
	 * @param cildClassName
	 *            optional. useful with targets that use Generic list instead of
	 *            Array
	 * @return target that can receive list of data. null if the current target
	 *         has no list-target for the specified field.
	 *
	 */
	public IDataListTarget getChildListTarget(String fieldName, String cildClassName);
}
