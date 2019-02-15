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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.core.trans;

import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.service.ServiceContext;

/**
 * An action that has no processing. It always succeeds. Hence it can be used
 * <p>
 * <li>marking a target action for the navigation/flow</li>
 * <li>adding a message. use messageOnSuccess.</li>
 *
 * @author simplity.org
 *
 */
public class CopyRows extends AbstractNonDbAction {
	/** sheet to which we want to add rows */
	@FieldMetaData(isRequired = true)
	String toSheetName;

	/** sheet from which to copy rows */
	@FieldMetaData(isRequired = true)
	String fromSheetName;

	@Override
	protected boolean act(ServiceContext ctx) {
		IDataSheet fromSheet = ctx.getDataSheet(this.fromSheetName);
		if (fromSheet == null) {
			return false;
		}
		int nbrRows = fromSheet.length();
		if (nbrRows == 0) {
			return false;
		}
		IDataSheet toSheet = ctx.getDataSheet(this.toSheetName);
		if (toSheet == null) {
			ctx.putDataSheet(this.toSheetName, fromSheet);
		} else {
			toSheet.appendRows(fromSheet);
		}

		return true;
	}
}
