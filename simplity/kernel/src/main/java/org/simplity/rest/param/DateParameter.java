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

import java.util.Date;
import java.util.List;

import org.simplity.json.JSONObject;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.util.DateUtil;
import org.simplity.rest.Tags;

/**
 * @author simplity.org
 *
 */
public class DateParameter extends Parameter {
	private boolean hasTime;

	/**
	 * spec when name is not part of it, For example inside a schema
	 *
	 * @param paramSpec
	 * @param name
	 */
	protected DateParameter(String name, JSONObject paramSpec) {
		this(paramSpec);
		this.name = name;
	}

	/**
	 * construct based on api spec
	 *
	 * @param paramSpec
	 */
	protected DateParameter(JSONObject paramSpec) {
		super(paramSpec);
		if (this.validValues != null) {
			return;
		}

		String txt = paramSpec.optString(Tags.FORMAT_ATT, null);
		if (txt != null && txt.equals(Tags.DATE_TIME_FORMAT)) {
			this.hasTime = true;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.rest.parm.Parameter#validate(java.lang.Object,
	 * java.util.List)
	 */
	@Override
	public Object doValidate(Object value, List<FormattedMessage> messages) {
		Date date = null;
		if (value instanceof Date) {
			return value;
		}

		String val = value.toString();
		if (this.hasTime) {
			date = DateUtil.parseDateTime(val);
		} else {
			date = DateUtil.parseDate(val);
		}
		if (date == null) {
			return this.invalidValue(messages);
		}
		return value;
	}
}
