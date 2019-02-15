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

package org.simplity.json;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.simplity.core.data.IFieldsCollection;
import org.simplity.core.value.Value;

/**
 * @author simplity.org
 *
 */
public class JSONFields implements IFieldsCollection {
	private final JSONObject jsonObject;

	/**
	 * create a new data store. this store can be fetched with getJsonObject()
	 * method
	 */
	public JSONFields() {
		this.jsonObject = new JSONObject();
	}

	/**
	 * wrap this josn as a JSONFields
	 *
	 * @param json
	 *            existing json to be used as data store
	 */
	public JSONFields(JSONObject json) {
		if (json == null) {
			this.jsonObject = new JSONObject();
		} else {
			this.jsonObject = json;
		}
	}

	@Override
	public Value getValue(String fieldName) {
		Object obj = this.jsonObject.opt(fieldName);
		if (obj == null) {
			return null;
		}
		if (obj instanceof Value) {
			return (Value) obj;
		}
		return Value.parseObject(obj);
	}

	@Override
	public void setValue(String fieldName, Value value) {
		this.jsonObject.put(fieldName, value);
	}

	@Override
	public boolean hasValue(String fieldName) {
		Value value = this.getValue(fieldName);
		return value != null;
	}

	@Override
	public Value removeValue(String fieldName) {
		Value value = this.getValue(fieldName);
		if (value != null) {
			this.jsonObject.remove(fieldName);
			return value;
		}
		return null;
	}

	@Override
	public Set<Entry<String, Value>> getAllFields() {
		Map<String, Value> map = new HashMap<>();
		for (String key : this.jsonObject.keySet()) {
			Value value = this.getValue(key);
			if (value != null) {
				map.put(key, value);
			}
		}
		return map.entrySet();
	}

	/**
	 *
	 * @return underlying json object
	 */
	public JSONObject getJson() {
		return this.jsonObject;
	}
}
