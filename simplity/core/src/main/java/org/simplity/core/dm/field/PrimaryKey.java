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

import org.simplity.core.data.DataPurpose;
import org.simplity.core.dm.DbTable;
import org.simplity.core.dm.Record;

/**
 * Primary key field
 */
public class PrimaryKey extends DbField {
	private boolean isGenerated;

	@Override
	public boolean canUpdate() {
		return false;
	}

	@Override
	public void getReady(Record parentRecord, Record defaultReferredRecord) {
		super.getReady(parentRecord, defaultReferredRecord);
		this.isGenerated = ((DbTable) parentRecord).isKeyGenerated();
	}

	@Override
	public boolean canInsert() {
		return !this.isGenerated;
	}

	@Override
	public boolean isPrimaryKey() {
		return true;
	}

	@Override
	protected boolean isNullOk(DataPurpose purpose) {
		return purpose == DataPurpose.SAVE && this.isGenerated;
	}
}
