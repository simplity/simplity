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

import org.simplity.core.adapter.IDataListSource;
import org.simplity.core.adapter.IDataSource;
import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;

/**
 * Json array as a list source
 *
 * @author simplity.org
 *
 */
public class JsonListSource {

	/**
	 * get a list source for the object
	 *
	 * @param obj
	 *            can be null.
	 * @return source if the object is either JSONOBject or JSONArray. null
	 *         otherwise
	 */
	public static IDataListSource getSource(Object obj) {
		if (obj == null) {
			return null;
		}
		if (obj instanceof JSONArray) {
			return new Source((JSONArray) obj);
		}
		if (obj instanceof JSONObject) {
			JSONArray arr = new JSONArray();
			arr.put(obj);
			return new Source(arr);
		}
		return null;
	}

	/**
	 * json array wrapped as a list source
	 *
	 * @author simplity.org
	 *
	 */
	private static class Source implements IDataListSource {
		private JSONArray source;

		/**
		 * create list source with a JSON Array
		 *
		 * @param array
		 */
		public Source(JSONArray array) {
			this.source = array;
		}

		@Override
		public int length() {
			return this.source.length();
		}

		@Override
		public IDataSource getChildSource(int zeroBasedIdx) {
			return JsonDataSource.getSource(this.source.opt(zeroBasedIdx));
		}
	}
}
