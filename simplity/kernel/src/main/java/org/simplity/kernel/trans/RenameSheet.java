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
package org.simplity.kernel.trans;

import org.simplity.kernel.comp.FieldMetaData;
import org.simplity.kernel.data.IDataSheet;
import org.simplity.kernel.service.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rename a data sheet. Returns true if renaming is successful, false otherwise
 *
 * @author simplity.org
 */
public class RenameSheet extends AbstractNonDbAction {
	private static final Logger actionLogger = LoggerFactory.getLogger(RenameSheet.class);

	/** current name */
	@FieldMetaData(isRequired = true)
	String sheetName;

	/** new name */
	@FieldMetaData(isRequired = true)
	String newSheetName;

	/*
	 * true if sheet got renamed. false otherwise. No error raised even if sheet
	 * is missing
	 */
	@Override
	protected boolean act(ServiceContext ctx) {
		IDataSheet sheet = ctx.removeDataSheet(this.sheetName);
		if (sheet == null) {

			actionLogger.info(
					"Data sheet {} not found, and hence is not renamed to", this.sheetName, this.newSheetName);

			return false;
		}
		ctx.putDataSheet(this.newSheetName, sheet);
		return true;
	}
}
