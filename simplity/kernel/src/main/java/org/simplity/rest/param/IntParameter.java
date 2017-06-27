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
import org.simplity.rest.Tags;

/**
 * @author simplity.org
 *
 */
public class IntParameter extends Parameter {
	private long min;
	private boolean exclusiveMin;
	private long max;
	private boolean exclusiveMax;
	private boolean isInt;
	private int multipleOf;


	/**
	 * spec when name is not part of it, For example inside a schema
	 * @param paramSpec
	 * @param name
	 */
	protected IntParameter(String name, String fieldName, JSONObject paramSpec) {
		this(paramSpec);
		this.name = name;
		if(fieldName != null){
			this.fieldName = fieldName;
		}
	}
	/**
	 * construct based on api spec
	 * @param paramSpec
	 */
	protected IntParameter(JSONObject paramSpec){
		super(paramSpec);
		if(this.validValues != null){
			return;
		}
		this.min = paramSpec.optLong(Tags.MIN_ATT, Long.MIN_VALUE);
		this.exclusiveMin = paramSpec.optBoolean(Tags.EXCL_MIN_ATT, false);
		this.max = paramSpec.optLong(Tags.MAX_ATT, Long.MAX_VALUE);
		this.exclusiveMax = paramSpec.optBoolean(Tags.EXCL_MAX_ATT, false);

		String text = paramSpec.optString(Tags.FORMAT_ATT, null);
		if(text != null && text.equals(Tags.INT_FORMAT)){
			this.isInt = true;
		}
		this.multipleOf = paramSpec.optInt(Tags.MULT_ATT, 0);
	}

	/* (non-Javadoc)
	 * @see org.simplity.rest.parm.Parameter#validate(java.lang.Object, java.util.List)
	 */
	@Override
	public Object doValidate(Object value, List<FormattedMessage> messages) {
		Long val;
		if(this.isInt){
			val = this.getInt(value);
		}else{
			val = this.getLong(value);
		}
		if(val != null){
			if(this.isValid(val.longValue())){
				return val;
			}
		}
		return this.invalidValue(messages);
	}

	private Long getLong(Object value){
		if(value instanceof Long ){
			return (Long)value;
		}
		if(value instanceof Number){
			return  new Long(((Number)value).longValue());
		}
		try{
			return new Long( Long.parseLong(value.toString()));
		}catch(Exception e){
			return null;
		}
	}

	private Long getInt(Object value){
		if(value instanceof Integer ){
			int val = ((Integer)value).intValue();
			return new Long(val);
		}
		if(value instanceof Number){
			return  new Long(((Number)value).intValue());
		}
		try{
			return new Long( Integer.parseInt(value.toString()));
		}catch(Exception e){
			return null;
		}

	}
	private boolean isValid(long val){
		/*
		 * three conditions for validity
		 * 1. max
		 */
		if(this.exclusiveMax){
			if(val >= this.max){
				return false;
			}
		}else{
			if(val > this.max){
				return false;
			}
		}
		//2. min
		if(this.exclusiveMin){
			if(val <= this.min){
				return false;
			}
		}else{
			if(val < this.min){
				return false;
			}
		}
		//3. multiple-of
		if(this.multipleOf > 0){
			if(val % this.multipleOf != 0){
				return false;
			}
		}
		return true;
	}
}
