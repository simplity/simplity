/*
 * Copyright (c) 2017 simplity.org
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

package org.simplity.rest.param;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;
import org.simplity.json.JSONWriter;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.Tracer;
import org.simplity.rest.Tags;

/**
 * @author simplity.org
 *
 */
public class ArrayParameter extends Parameter {
	private CollectionType colnType;
	private Parameter item;
	private boolean uniqueItem;
	private int minItems;
	private int maxItems;

	/**
	 * construct based on api spec
	 *
	 * @param paramSpec
	 */
	protected ArrayParameter(JSONObject paramSpec) {
		super(paramSpec);
		if (this.validValues != null) {
			return;
		}

		String txt = paramSpec.optString(Tags.COLN_FORMAT_ATT, null);
		if (txt != null) {
			this.colnType = CollectionType.valueOf(txt);
			if (this.colnType == null) {
				throw new ApplicationError(
						"We do not understand array collection type " + txt + " as is used for parameter " + this.name);
			}
			if (this.colnType == CollectionType.multi) {
				Tracer.trace(
						"We handle multi-format array in query and form fields while forming input Json, and hence this attribute is ignored and an array value is assumed");
				this.colnType = null;
			}
		}

		JSONObject obj = paramSpec.optJSONObject(Tags.ITEMS_ATT);
		if (obj == null) {
			throw new ApplicationError("items specification is missing for array field " + this.name);
		}
		this.item = Parameter.parse(this.name + "[item]", obj);
		this.uniqueItem = paramSpec.optBoolean(Tags.UNIQUE_ATT, false);
		this.minItems = paramSpec.optInt(Tags.MIN_ITEMS_ATT, 0);
		this.maxItems = paramSpec.optInt(Tags.MAX_ITEMS_ATT, 0);
	}

	/**
	 * @param name
	 * @param parameterSpec
	 */
	public ArrayParameter(String name, JSONObject parameterSpec) {
		this(parameterSpec);
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.rest.parm.Parameter#validate(java.lang.Object,
	 * java.util.List)
	 */
	@Override
	public Object doValidate(Object value, List<FormattedMessage> messages) {
		JSONArray arr = null;
		boolean ok = true;
		if (this.colnType == null) {
			/*
			 * this is a josn object
			 */
			if (value instanceof JSONArray == false) {
				Tracer.trace("Field " + this.name + " expects as an array but got " + value.getClass().getName());
				return this.invalidValue(messages);
			}
			arr = (JSONArray) value;
			for (int i = arr.length() - 1; i >= 0; i--) {
				Object val = this.item.validate(arr.get(i), messages);
				if (val == null) {
					ok = false;
				} else {
					arr.put(i, val);
				}
			}
		} else {
			/*
			 * this is an array that is serialized into string
			 */
			String[] vals = this.colnType.textToArray(value.toString());
			arr = new JSONArray();
			for (String val : vals) {
				Object obj = this.item.validate(val, messages);
				if (obj == null) {
					ok = false;
				} else {
					arr.put(obj);
				}
			}
		}
		if (ok == false) {
			return null;
		}
		/*
		 * max/min items?
		 */
		int len = arr.length();
		if (len < this.minItems || (this.maxItems != 0 && len > this.maxItems)) {
			Tracer.trace("Array field " + this.name + " has minItems=" + this.minItems + " and maxItems="
					+ this.maxItems + " but it received " + len + " items");
			return this.invalidValue(messages);
		}

		/*
		 * unique items?
		 */
		if (this.uniqueItem) {
			for (int i = 0; i < len; i++) {
				for (int j = i + 1; j < len; j++) {
					if (arr.get(i).equals(arr.get(j))) {
						Tracer.trace(
								"Array field " + this.name + " got duplicate values at (0 based)" + i + " and " + j);
						return this.invalidValue(messages);
					}
				}
			}
		}
		return arr;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.rest.param.Parameter#toWriter(org.simplity.json.JSONWriter,
	 * org.simplity.json.JSONObject, boolean)
	 */
	@Override
	public void toWriter(JSONWriter writer, Object data, boolean asAttribute) {
		if (asAttribute) {
			writer.key(this.name);
		}
		if (this.colnType == null) {
			/*
			 * this is an array of values
			 */
			writer.array();
			if (data instanceof JSONArray) {
				for (Object ele : (JSONArray) data) {
					this.item.toWriter(writer, ele, false);
				}
			} else {
				this.item.toWriter(writer, data, false);
			}
			writer.endArray();
			return;
		}
		/**
		 * this is a serialized text field
		 */
		String text;
		if (data instanceof JSONArray) {
			text = this.colnType.arrayToText((JSONArray) data);
		} else {
			text = data.toString();
		}
		writer.value(text);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.rest.param.Parameter#setHeader(javax.servlet.http.
	 * HttpServletResponse, org.simplity.json.JSONObject)
	 */
	@Override
	public void setHeader(HttpServletResponse resp, Object data) {
		if (data == null) {
			return;
		}
		String text;
		if (data instanceof JSONArray) {
			JSONArray arr = (JSONArray) data;
			if (this.colnType == null) {
				Tracer.trace("Field " + this.name
						+ " is an array, and is being used to write to header, but it has no collectionType. comma separation is assumed");
				text = CollectionType.csv.arrayToText(arr);
			} else {
				text = this.colnType.arrayToText(arr);
			}
		} else {
			text = data.toString();
		}
		resp.setHeader(this.name, text);
	}

	enum CollectionType {
		csv(","), ssv(" "), tsv("\t"), pipes("|"), multi("&");
		private String sep = ",";

		CollectionType(String sep) {
			this.sep = sep;
		}

		String[] textToArray(String txt) {
			return txt.split(this.sep);
		}

		String arrayToText(JSONArray arr) {
			if (arr == null || arr.length() == 0) {
				return "";
			}
			StringBuilder sbf = new StringBuilder();
			for (Object ele : arr) {
				sbf.append(ele.toString()).append(this.sep);
			}
			sbf.setLength(sbf.length() - 1);
			return sbf.toString();
		}
	}
}
