/*
 * Copyright (c) 2018 simplity.org
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


package org.simplity.kernel.adapter.source;

import org.simplity.kernel.adapter.IDataListSource;
import org.simplity.kernel.data.IDataSheet;

/**
 * Data sheet wrapped as list source
 *
 * @author simplity.org
 *
 */
public class DataSheetListSource implements IDataListSource {
	private IDataSheet source;

	/**
	 * create ListSource with a data sheet
	 *
	 * @param sheet
	 */
	public DataSheetListSource(IDataSheet sheet) {
		this.source = sheet;
	}

	@Override
	public int length() {
		return this.source.length();
	}

	@Override
	public FieldsDataSource getChildSource(int zeroBasedIdx) {
		if (zeroBasedIdx < 0 || zeroBasedIdx >= this.length()) {
			return null;
		}
		return new FieldsDataSource(this.source.getRowAsFields(zeroBasedIdx));
	}
}
