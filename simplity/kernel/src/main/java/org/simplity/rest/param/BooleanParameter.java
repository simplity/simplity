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

import org.simplity.json.JSONObject;
import org.simplity.kernel.FormattedMessage;

/**
 * @author simplity.org
 *
 */
public class BooleanParameter extends Parameter {
	private static String[] TRUE_VALUES = {"true", "1", "yes"};
	private static String[] FALSE_VALUES = {"false", "0", "no"};

	/**
	 * spec when name is not part of it, For example inside a schema
	 * @param paramSpec
	 * @param name
	 */
	protected BooleanParameter(String name, JSONObject paramSpec) {
		this(paramSpec);
		this.name = name;
	}
	/**
	 * construct based on api spec
	 * @param paramSpec
	 */
	protected BooleanParameter(JSONObject paramSpec){
		super(paramSpec);
	}

	/* (non-Javadoc)
	 * @see org.simplity.rest.parm.Parameter#validate(java.lang.Object, java.util.List)
	 */
	@Override
	public Object doValidate(Object value, List<FormattedMessage> messages) {
		if(value instanceof Boolean ){
			return value;
		}

		String val = value.toString();
		for(String text : TRUE_VALUES){
			if(text.equals(val)){
				return Boolean.TRUE;
			}
		}
		for(String text : FALSE_VALUES){
			if(text.equals(val)){
				return Boolean.FALSE;
			}
		}

		return this.invalidValue(messages);
	}

}
