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
package org.simplity.core.adapter.destn;

import java.time.LocalDate;

import org.simplity.core.adapter.IDataListTarget;
import org.simplity.core.adapter.IDataTarget;
import org.simplity.core.util.JsonUtil;
import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;

/**
 * JSON object to receive data from an adapater
 *
 * @author simplity.org
 *
 */
public class JsonDataTarget {

	/**
	 * create a data target if this object is JSON object or json array
	 *
	 * @param obj
	 * @return null if it is neither json object or json array
	 */
	public static IDataTarget getTarget(Object obj) {
		if (obj == null) {
			return null;
		}
		if (obj instanceof JSONObject) {
			return new Target((JSONObject) obj);
		}
		if (obj instanceof JSONArray) {
			JSONObject json = ((JSONArray) obj).optJSONObject(0);
			if (json != null) {
				return new Target(json);
			}
		}
		return null;
	}

	private static class Target implements IDataTarget {
		private JSONObject target;

		/**
		 * JSONObject wrapped as target of data from a VO
		 *
		 * @param json
		 */
		protected Target(JSONObject json) {
			this.target = json;
		}

		@Override
		public void setPrimitiveValue(String fieldName, String fieldValue) {
			JsonUtil.setChildValue(this.target, fieldName, fieldValue);
		}

		@Override
		public IDataTarget getChildTarget(String fieldName, String memberClassName) {
			JsonUtil.LeafObject lo = JsonUtil.getLeaf(this.target, fieldName, true);
			IDataTarget t = getTarget(lo.parent.opt(lo.fieldName));
			if (t != null) {
				return t;
			}
			JSONObject json = new JSONObject();
			lo.parent.put(lo.fieldName, json);
			return new Target(json);
		}

		@Override
		public IDataListTarget getChildListTarget(String fieldName, String childClassName) {
			JsonUtil.LeafObject lo = JsonUtil.getLeaf(this.target, fieldName, true);
			IDataListTarget t = JsonListTarget.getTarget(lo.parent.opt(lo.fieldName));
			if (t != null) {
				return t;
			}
			JSONArray arr = new JSONArray();
			lo.parent.put(lo.fieldName, arr);
			return JsonListTarget.getTarget(arr);
		}

		@Override
		public void setStruct(String fieldName, Object fieldValue) {
			JsonUtil.setChildValue(this.target, fieldName, fieldValue);
		}

		@Override
		public void setDateValue(String fieldName, LocalDate fieldValue) {
			JsonUtil.setChildValue(this.target, fieldName, fieldValue);
		}
	}
}
