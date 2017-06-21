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
import java.util.regex.Pattern;

import org.simplity.json.JSONObject;
import org.simplity.kernel.FormattedMessage;
import org.simplity.rest.Tags;

/**
 * @author simplity.org
 *
 */
public class TextParameter extends Parameter {
	private int minLength;
	private int maxLength;
	private Pattern pattern;

	/**
	 * @param name
	 * @param parameterSpec
	 */
	public TextParameter(String name, JSONObject parameterSpec) {
		this(parameterSpec);
		this.name = name;
	}
	/**
	 * construct based on api spec
	 * @param paramSpec
	 */
	protected TextParameter(JSONObject paramSpec){
		super(paramSpec);
		if(this.validValues !=null){
			return;
		}

		String txt = paramSpec.optString(Tags.PATTERN_ATT, null);
		if(txt != null && txt.isEmpty() == false){
			this.pattern = Pattern.compile(txt);
		}
		this.minLength = paramSpec.optInt(Tags.MIN_LEN_ATT, 0);
		this.maxLength = paramSpec.optInt(Tags.MAX_LEN_ATT, 0);
	}

	/* (non-Javadoc)
	 * @see org.simplity.rest.parm.Parameter#validate(java.lang.Object, java.util.List)
	 */
	@Override
	public Object doValidate(Object value, List<FormattedMessage> messages) {
		String val = value.toString();
		if( this.pattern != null &&  this.pattern.matcher(val).matches() == false){
			return this.invalidValue(messages);
		}
		int len = val.length();
		if( len < this.minLength || (this.maxLength > 0 && len > this.maxLength)){
			return this.invalidValue(messages);
		}
		return val;
	}
}
