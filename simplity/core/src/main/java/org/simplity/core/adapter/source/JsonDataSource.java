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
import org.simplity.core.util.JsonUtil;
import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;

/**
 * JSON Object as a data source
 *
 * @author simplity.org
 *
 */
public class JsonDataSource {

	/**
	 * get a JSON based data source for this object
	 *
	 * @param obj
	 * @return data source if possible. null if this object is not JSON Object
	 *         or array
	 */
	public static IDataSource getSource(Object obj) {
		if (obj == null) {
			return null;
		}
		if (obj instanceof JSONObject) {
			return new Source((JSONObject) obj);
		}
		if (obj instanceof JSONArray) {
			JSONObject json = ((JSONArray) obj).optJSONObject(0);
			if (json != null) {
				return new Source(json);
			}
		}
		return null;
	}

	/**
	 * JSONObject wrapped as a data source
	 *
	 * @author simplity.org
	 *
	 */
	private static class Source implements IDataSource {
		private JSONObject source;

		/**
		 * JSONObject wrapped as source of data
		 *
		 * @param json
		 */
		protected Source(JSONObject json) {
			this.source = json;
		}

		@Override
		public String getPrimitiveValue(String fieldName) {
			Object obj = JsonUtil.getChildValue(this.source, fieldName);
			if (obj == null) {
				return null;
			}
			return obj.toString();
		}

		@Override
		public IDataSource getChildSource(String fieldName) {
			return JsonDataSource.getSource(JsonUtil.getChildValue(this.source, fieldName));
		}

		@Override
		public IDataListSource getChildListSource(String fieldName) {
			return JsonListSource.getSource(JsonUtil.getChildValue(this.source, fieldName));
		}

		@Override
		public Object getStruct(String fieldName) {
			Object obj = JsonUtil.getChildValue(this.source, fieldName);
			if (obj instanceof JSONObject) {
				return obj;
			}
			if (obj instanceof JSONArray) {
				return ((JSONArray) obj).optJSONObject(0);
			}
			return null;
		}

		@Override
		public LocalDate getDateValue(String fieldName) {
			Object obj = JsonUtil.getChildValue(this.source, fieldName);
			if (obj == null) {
				return null;
			}
			if (obj instanceof LocalDate) {
				return (LocalDate) obj;
			}
			try {
				LocalDate.parse(obj.toString());
			} catch (Exception e) {
				//
			}
			return null;
		}
	}
}
