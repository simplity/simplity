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

import org.simplity.core.adapter.IDataListTarget;
import org.simplity.core.adapter.IDataTarget;
import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;

/**
 * JSON array as a target of list of data from an adapter
 *
 * @author simplity.org
 *
 */
public class JsonListTarget {
	/**
	 * get a list source for the object
	 *
	 * @param obj
	 *            can be null.
	 * @return source if the object is either JSONOBject or JSONArray. null
	 *         otherwise
	 */
	public static IDataListTarget getTarget(Object obj) {
		if (obj == null) {
			return null;
		}
		if (obj instanceof JSONArray) {
			return new Target((JSONArray) obj);
		}
		if (obj instanceof JSONObject) {
			JSONArray arr = new JSONArray();
			arr.put(obj);
			return new Target(arr);
		}
		return null;
	}

	private static class Target implements IDataListTarget {
		private JSONArray target;

		/**
		 * create VoDataListTarget with a JSON Array
		 *
		 * @param array
		 */
		protected Target(JSONArray array) {
			this.target = array;
		}

		@Override
		public IDataTarget getChildTarget(int zeroBasedIdx) {
			IDataTarget t = JsonDataTarget.getTarget(this.target.opt(zeroBasedIdx));
			if (t != null) {
				return t;
			}
			JSONObject json = new JSONObject();
			this.target.put(zeroBasedIdx, json);
			return JsonDataTarget.getTarget(json);
		}

		@Override
		public void listDone() {
			//
		}
	}
}
