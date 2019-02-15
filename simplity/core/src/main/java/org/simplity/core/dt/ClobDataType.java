/*
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

import org.simplity.core.file.FileManager;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;

/**
 * Clob is a pseudo data type, because we actually do not allow this data type
 * to be used for data element. However, the RDBMS has this data type for
 * storage, and we need to store/retrieve this. ClobDataType is a wrapper on
 * textDataType to facilitate this. Actual Clob data is saved (before saving, or
 * after retrieving) in a temp-file. Field that is associated with BlobDataType
 * has a value of text that is the key to this file.
 *
 * @author simplity.org
 */
public class ClobDataType extends DataType {
	/**
	 * max number of characters expected. Let us keep it safe for possible key
	 * length
	 */
	private static final int MAX_LENGTH = 50;

	private static final String DESC = "This is an internally generated key. You should not synthesize, but re-send what you have received";

	/*
	 * (non-Javadoc)Should we check whether the key indeed points to a file?
	 * Actually a good idea, except for performance. Let us go ahead with it at
	 * this time
	 *
	 */
	@Override
	public Value validateValue(Value value) {
		if (FileManager.getTempFile(value.toString()) == null) {
			/*
			 * this is not a valid pointer to a temp file
			 */
			return null;
		}
		return value;
	}

	@Override
	public ValueType getValueType() {
		return ValueType.CLOB;
	}

	@Override
	public int getMaxLength() {
		return MAX_LENGTH;
	}

	@Override
	protected String synthesiseDscription() {
		return DESC;
	}
}
