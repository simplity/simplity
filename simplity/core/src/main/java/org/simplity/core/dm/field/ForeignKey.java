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

import java.util.Set;

import org.simplity.core.ApplicationError;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.comp.ValidationMessage;
import org.simplity.core.dm.Record;

/**
 * Primary key field
 */
public class ForeignKey extends DbField {
	/**
	 *
	 */
	public ForeignKey() {
		this.fieldType = FieldType.FOREIGN_KEY;
		this.toBeInput = true;
		this.insertable = true;
	}

	@Override
	public void validate(IValidationContext vtx, Record record, Set<String> referredFields) {
		super.validate(vtx, record, referredFields);
		if (this.referredRecord == null) {
			vtx.message(new ValidationMessage(this, ValidationMessage.SEVERITY_ERROR,
					"foreignKey field must refer to a record using referredRecord attribute", null));
		}
	}

	@Override
	public void getReady(Record parentRecord, Record defaultReferredRecord) {
		super.getReady(parentRecord, defaultReferredRecord);
		if (this.referredRecord == null) {
			throw new ApplicationError("Field " + this.name + " in record " + parentRecord.getQualifiedName()
					+ " should use referredRecord attribute to refer to another record");
		}
	}
}
