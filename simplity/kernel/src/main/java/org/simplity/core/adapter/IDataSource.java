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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.core.adapter;

import java.util.Date;

/**
 * source of data. defines methods to get input data from a data source
 *
 * @author simplity.org
 *
 */
public interface IDataSource {
	/**
	 *
	 * @param fieldName
	 *            non-null. name can be of the form a.b.c to go thru the object
	 *            hierarchy
	 * @return primitive value as String. null if there is no such field, or if
	 *         the field does not have a primitive value
	 */
	public String getPrimitiveValue(String fieldName);

	/**
	 *
	 * @param fieldName
	 *            non-null.name can be of the form a.b.c to go thru the object
	 *            hierarchy
	 * @return value as Date. null if no such field, or if it is not a date
	 *         field.
	 */
	public Date getDateValue(String fieldName);

	/**
	 *
	 * @param fieldName
	 *            non-null. name can be of the form a.b.c to go thru the object
	 *            hierarchy
	 * @return field value of this source as a data structure. null if the filed
	 *         does not exist, or if it has a null value, or if its value is
	 *         primitive (not a struct)
	 *
	 */
	public Object getStruct(String fieldName);

	/**
	 *
	 * @param fieldName
	 *            non-null. name can be of the form a.b.c to go thru the object
	 *            hierarchy
	 * @return field data source to supply data as child of current source. null
	 *         if you can not read/supply data as that child source. For e.g. in
	 *         POJO if such a field is not defined as an object
	 */
	public IDataSource getChildSource(String fieldName);

	/**
	 *
	 * @param fieldName
	 *            non-null. name can be of the form a.b.c to go thru the object
	 *            hierarchy
	 * @return source of list data. null if a list does not exist
	 */
	public IDataListSource getChildListSource(String fieldName);
}
