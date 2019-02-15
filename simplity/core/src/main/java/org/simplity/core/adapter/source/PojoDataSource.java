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

import java.util.Date;

import org.simplity.core.adapter.IDataListSource;
import org.simplity.core.adapter.IDataSource;
import org.simplity.core.util.DateUtil;
import org.simplity.core.util.ReflectUtil;

/**
 * Java Object as a data source
 *
 * @author simplity.org
 *
 */
public class PojoDataSource {

	/**
	 *
	 * @param pojo
	 *            if null or primitive, null is returned. if list/array, their
	 *            first member is tried as source.
	 * @return data source based on this object. null if this can not be wrapped
	 *         as a source.
	 */
	public static IDataSource getDataSource(Object pojo) {
		Object obj = ReflectUtil.getPathObject(pojo);
		if (obj == null) {
			return null;
		}
		return new Source(obj);
	}

	private static class Source implements IDataSource {
		private final Object source;

		protected Source(Object source) {
			this.source = source;
		}

		@Override
		public String getPrimitiveValue(String fieldName) {
			Object obj = ReflectUtil.getChildPrimitive(this.source, fieldName);
			if (obj == null) {
				return null;
			}
			if (obj instanceof Date) {
				return DateUtil.formatDateTime((Date) obj);
			}
			return obj.toString();
		}

		@Override
		public IDataSource getChildSource(String fieldName) {
			Object obj = ReflectUtil.getChildValue(this.source, fieldName);
			return getDataSource(obj);
		}

		@Override
		public IDataListSource getChildListSource(String fieldName) {
			Object obj = ReflectUtil.getChildValue(this.source, fieldName);
			return PojoListSource.getListSource(obj);
		}

		@Override
		public Object getStruct(String fieldName) {
			Object obj = ReflectUtil.getChildValue(this.source, fieldName);
			/*
			 * we want pure object, first element of list/array, no primitive.
			 */
			return ReflectUtil.getPathObject(obj);
		}

		@Override
		public Date getDateValue(String fieldName) {
			Object obj = ReflectUtil.getChildPrimitive(this.source, fieldName);
			if (obj == null) {
				return null;
			}
			if (obj instanceof Date) {
				return (Date) obj;
			}
			return DateUtil.parseDateWithOptionalTime(obj.toString());
		}
	}
}
