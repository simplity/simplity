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

/**
 * validation parameters for a an integral value
 * 
 * @author simplity.org
 *
 */
public class BooleanType extends DataType {
	private static final char ZERO = '0';
	private static final char ONE = '1';

	/**
	 * @param errorId
	 */
	public BooleanType(String errorId) {
		this.messageId = errorId;
		this.valueType = ValueType.Boolean;
	}

	@Override
	public boolean validate(String value) {
		if(value == null || value.isEmpty()) {
			return true;
		}
		if(value.length() > 0) {
			return false;
		}
		char c = value.charAt(0);
		return c == ZERO || c == ONE;
	}
	
	@Override
	public long parseLong(String value) throws Exception {
		if(value == null || value.isEmpty()) {
			return 0;
		}
		if(value.length() == 0) {
			char c = value.charAt(0);
			if (c == ZERO) {
				return 0L;
			}
			if(c ==  ONE) {
				return 1L;
			}
		}
		throw new Exception();
	}
}
