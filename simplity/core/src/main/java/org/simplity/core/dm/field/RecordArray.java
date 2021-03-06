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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.core.dm.field;

import org.simplity.core.ApplicationError;
import org.simplity.core.dm.Record;

/**
 * array of data structure that is defined in another record
 */
public class RecordArray extends DataStructureField {

	@Override
	protected void resolverReference(Record parentRecord, Record defaultRefferedRecord) {
		// reference has different meaning in this context
	}

	@Override
	public void getReady(Record parentRecord, Record defaultReferredRecord) {
		super.getReady(parentRecord, defaultReferredRecord);
		if (this.sqlTypeName != null) {
			Record ref = parentRecord.getRefRecord(this.referredRecord);
			if (ref.getSqlTypeName() == null) {
				throw new ApplicationError("Record " + ref.getQualifiedName()
						+ " is used as an array element of a stored procedure parameter by field " + this.name
						+ " in record " + parentRecord.getQualifiedName() + " and hence it should have sqlTypeName");
			}
		}
	}
}
