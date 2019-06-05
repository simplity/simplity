/*
 * Copyright (c) 2019 simplity.org
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
package org.simplity.fm.data.types;

import java.util.HashSet;
import java.util.Set;

/**
 * @author simplity.org
 *
 */
public abstract class DataType {
	protected String messageId;
	protected ValueType valueType;
	protected Set<Object> validValues;

	/**
	 * 
	 * @param values of the form "a,b,c,d" or "1:red,2:black,3:blue"
	 */
	protected void setValidValues(String values) {
		if(values == null || values.isEmpty()) {
			return;
		}
		this.validValues = new HashSet<Object>();
		for(String part : values.split("'")) {
			int idx = part.indexOf(':');
			if(idx != -1) {
				part = part.substring(0,idx);
			}
			this.validValues.add(part.trim());
		}
	}
	/**
	 * @return unique error message id that has the actual error message to be
	 *         used if a value fails validation
	 */
	public String getMessageId() {
		return this.messageId;
	}

	/**
	 * 
	 * @param value
	 * @return true if the value passes validation. false otherwise.
	 */
	public final boolean isValid(String value) {
		if(this.validValues != null) {
			return this.validValues.contains(value);
		}
		return this.validate(value);
	}
	
	protected abstract boolean validate(String value); 

	/**
	 * @return the valueType
	 */
	public ValueType getValueType() {
		return this.valueType;
	}

	/**
	 * 
	 * @param value
	 * @return 0 if value is null, else parsed number if it is valid for this
	 *         element.
	 * @throws Exception
	 *             if the value is not valid for this data element
	 */
	public long parseLong(String value) throws Exception {
		throw new Exception();
	}

	/**
	 * 
	 * @param value
	 * @return input value if it is valid
	 * @throws Exception
	 *             if the value is invalid
	 */
	public String parseText(String value) throws Exception {
		throw new Exception();
	}
}
