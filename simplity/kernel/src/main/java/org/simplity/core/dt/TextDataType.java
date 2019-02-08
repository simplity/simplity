/*
 * Copyright (c) 2015 EXILANT Technologies Private Limited (www.exilant.com)
 * Copyright (c) 2016 simplity.org
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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.core.dt;

import java.util.regex.Pattern;

import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationMessage;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;

/**
 * Text/string
 *
 * @author simplity.org
 */
public class TextDataType extends DataType {

	/** min number of characters expected. */
	int minLength;
	/** max number of characters expected */
	int maxLength = Integer.MAX_VALUE;
	/**
	 * any pattern that this has to follow. designer is expected to learn Java
	 * Pattern!!!
	 */
	Pattern regex;

	@Override
	public Value validateValue(Value value) {
		String textValue = value.toText();
		int nbrChars = textValue.length();
		if (nbrChars < this.minLength || nbrChars > this.maxLength) {
			return null;
		}
		if (this.regex != null && this.regex.matcher(textValue).matches() == false) {
			return null;
		}
		/*
		 * convert it to text value if required
		 */
		if (value.getValueType() == ValueType.TEXT) {
			return value;
		}
		return Value.newTextValue(textValue);
	}

	@Override
	public ValueType getValueType() {
		return ValueType.TEXT;
	}

	@Override
	public int getMaxLength() {
		return this.maxLength;
	}

	@Override
	protected void validateSpecific(IValidationContext vtx) {
		if (this.minLength > this.maxLength) {
			String msg = "Min length is set to "
					+ this.minLength
					+ " that is greater than max length of "
					+ this.maxLength;
			vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR, msg, "minLength"));
			vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR, msg, "maxLength"));
		}
		if (this.minLength < 0) {
			String msg = "Min length is set to a negative value of " + this.minLength;
			vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR, msg, "minLength"));
		}
		if (this.maxLength < 0) {
			String msg = "Max length is set to a negative value of " + this.maxLength;
			vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR, msg, "maxLength"));
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.dt.DataType#synthesiseDscription()
	 */
	@Override
	protected String synthesiseDscription() {
		StringBuilder sbf = new StringBuilder("Expecting text ");
		boolean started = false;
		if (this.minLength != 0) {
			sbf.append("with a min of ").append(this.minLength).append(" characters ");
			started = true;
		}
		if (this.maxLength != Integer.MAX_VALUE) {
			if (!started) {
				sbf.append("with a max of ");
				started = true;
			} else {
				sbf.append(" and a max of ");
			}
			sbf.append(this.maxLength).append(" characters ");
		}
		if (this.regex != null) {
			sbf.append("that conforms to the pattern ").append(this.regex.toString());
		}
		return sbf.toString();
	}

	/** @return regex */
	public Object getRegex() {
		return this.regex;
	}
}
