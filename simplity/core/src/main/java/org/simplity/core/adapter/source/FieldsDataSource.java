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

package org.simplity.core.adapter.source;

import java.time.LocalDate;

import org.simplity.core.adapter.IDataListSource;
import org.simplity.core.adapter.IDataSource;
import org.simplity.core.data.IFieldsCollection;
import org.simplity.core.value.DateValue;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;

/**
 * collection of name-primitiveValue pairs as source of data
 *
 * @author simplity.org
 *
 */
public class FieldsDataSource implements IDataSource {
	private IFieldsCollection source;

	/**
	 * use underlying fields collection as data source
	 *
	 * @param fields
	 */
	public FieldsDataSource(IFieldsCollection fields) {
		this.source = fields;
	}

	@Override
	public String getPrimitiveValue(String sourceFieldName) {
		Value value = this.source.getValue(sourceFieldName);
		if (Value.isNull(value)) {
			return null;
		}
		return value.toString();
	}

	@Override
	public IDataSource getChildSource(String childSourceName) {
		// fields collection contains only primitive values
		return null;
	}

	@Override
	public IDataListSource getChildListSource(String sourceFieldName) {
		// fields collection contains only primitive values
		return null;
	}

	@Override
	public Object getStruct(String fieldName) {
		// fields collection contains only primitive values
		return null;
	}

	@Override
	public LocalDate getDateValue(String fieldName) {
		Value value = this.source.getValue(fieldName);
		if (Value.isNull(value)) {
			return null;
		}
		ValueType vt = value.getValueType();
		if (vt == ValueType.DATE) {
			return (((DateValue) value).getDate());
		}
		if (vt == ValueType.TEXT) {
			try {
				return LocalDate.parse(value.toString());
			} catch (Exception e) {
				//
			}
		}
		return null;
	}
}
