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

import java.lang.reflect.Array;
import java.util.List;

import org.simplity.core.adapter.IDataListSource;
import org.simplity.core.adapter.IDataSource;

/**
 *
 * @author simplity.org
 *
 */
public class PojoListSource {

	/**
	 *
	 * @param obj
	 *            should be non-primitive non-null object. if array/list then
	 *            proper list source is returned. otherwise the sole object is
	 *            wrapped as the sole member of a list
	 * @return a list source wrapper on this object
	 */
	@SuppressWarnings("unchecked")
	public static IDataListSource getListSource(Object obj) {
		if (obj == null) {
			return null;
		}
		if (obj.getClass().isArray()) {
			return new ArrList(obj);
		}
		if (obj instanceof List) {
			return new ListList((List<? extends Object>) obj);
		}
		return new ObjList(obj);
	}

	/**
	 * list source based on java.util.List
	 * 
	 * @author simplity.org
	 *
	 */
	private static class ListList implements IDataListSource {
		private final List<? extends Object> list;

		protected ListList(List<? extends Object> list) {
			this.list = list;
		}

		@Override
		public int length() {
			return this.list.size();
		}

		@Override
		public IDataSource getChildSource(int zeroBasedIdx) {
			return PojoDataSource.getDataSource(this.list.get(zeroBasedIdx));
		}
	}

	/**
	 * list source based on Array
	 * 
	 * @author simplity.org
	 *
	 */
	private static class ArrList implements IDataListSource {
		private final Object array;

		protected ArrList(Object array) {
			this.array = array;
		}

		@Override
		public int length() {
			return Array.getLength(this.array);
		}

		@Override
		public IDataSource getChildSource(int zeroBasedIdx) {
			try {
				return PojoDataSource.getDataSource(Array.get(this.array, zeroBasedIdx));
			} catch (Exception e) {
				return null;
			}
		}
	}

	/**
	 * list source based on POJO
	 * 
	 * @author simplity.org
	 *
	 */
	private static class ObjList implements IDataListSource {
		private final Object source;

		protected ObjList(Object source) {
			this.source = source;
		}

		@Override
		public int length() {
			return 1;
		}

		@Override
		public IDataSource getChildSource(int zeroBasedIdx) {
			if (zeroBasedIdx != 0) {
				return null;
			}
			return PojoDataSource.getDataSource(this.source);
		}
	}
}
