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
public class NumberParameter extends Parameter {
	private double min;
	private boolean exclusiveMin;
	private double max;
	private boolean exclusiveMax;
	private boolean isFloat;

	/**
	 * spec when name is not part of it, For example inside a schema
	 * @param paramSpec
	 * @param name
	 */
	protected NumberParameter(String name, String fieldName, JSONObject paramSpec) {
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
	protected NumberParameter(JSONObject paramSpec){
		super(paramSpec);
		if(this.validValues != null){
			return;
		}
		this.min = paramSpec.optDouble(Tags.MIN_ATT, Double.MIN_VALUE);
		this.exclusiveMin = paramSpec.optBoolean(Tags.EXCL_MIN_ATT, false);
		this.max = paramSpec.optDouble(Tags.MAX_ATT, Double.MAX_VALUE);
		this.exclusiveMax = paramSpec.optBoolean(Tags.EXCL_MAX_ATT, false);
		String text = paramSpec.optString(Tags.FORMAT_ATT, null);
		if(text != null && text.equals(Tags.FLOAT_FORMAT)){
			this.isFloat = true;
		}
	}

	/* (non-Javadoc)
	 * @see org.simplity.rest.parm.Parameter#validate(java.lang.Object, java.util.List)
	 */
	@Override
	public Object doValidate(Object value, List<FormattedMessage> messages) {
		Double val;

		if(this.isFloat){
			val = this.getFloat(value);
		}else{
			val = this.getDouble(value);
		}
		if(val != null){
			if(this.isValid(val.doubleValue())){
				return val;
			}
		}
		return this.invalidValue(messages);
	}
	/**
	 * @param value
	 * @return
	 */
	private Double getDouble(Object value) {
		if(value instanceof Double){
			return (Double)value;
		}
		if(value instanceof Number){
			return new Double(((Number)value).doubleValue());
		}
		try{
			return new Double (Double.parseDouble(value.toString()));
		}catch(Exception e){
			return null;
		}
	}

	/**
	 * @param value
	 * @return
	 */
	private Double getFloat(Object value) {
		if(value instanceof Float){
			return new Double(((Float) value).floatValue());
		}
		if(value instanceof Number){
			return new Double( ((Number)value).floatValue());
		}
		try{
			return new Double(Float.parseFloat(value.toString()));
		}catch(Exception e){
			return null;
		}
	}

	private boolean isValid(double val){
		if(this.exclusiveMax){
			if(val >= this.max){
				return false;
			}
		}else{
			if(val > this.max){
				return false;
			}
		}
		if(this.exclusiveMin){
			if(val <= this.min){
				return false;
			}
		}else{
			if(val < this.min){
				return false;
			}
		}
		return true;
	}
}
