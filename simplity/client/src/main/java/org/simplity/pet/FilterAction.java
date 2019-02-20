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

package org.simplity.pet;

import org.simplity.core.app.Application;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.dm.DbTable;
import org.simplity.core.idb.IDbHandle;
import org.simplity.core.idb.IReadOnlyHandle;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.trans.ILogicWithDbAccess;

/**
 * example java code to be called as an action/task inside a simplity service.
 * Refer to pet.filterWithJavaAction
 *
 * @author simplity.org
 *
 */
public class FilterAction implements ILogicWithDbAccess {
	private static final String OWNER = "pet.owner";
	private static final String PET = "pet.petDetail";

	@Override
	public boolean execute(ServiceContext ctx, IDbHandle dbHandle) {
		Application app = ctx.getApp();
		DbTable owner = (DbTable) app.getRecord(OWNER);
		IDataSheet ds = owner.filter(owner, ctx, (IReadOnlyHandle) dbHandle, ctx.getUserId());
		ctx.putDataSheet(owner.getDefaultSheetName(), ds);

		/*
		 * read rows from pets for the filtered owners
		 */
		int nbrRows = ds.length();
		if (nbrRows > 0) {
			DbTable pet = (DbTable) app.getRecord(PET);
			pet.filterForParents(ds, (IReadOnlyHandle) dbHandle, pet.getDefaultSheetName(), false, ctx);
		}
		return nbrRows > 0;
	}
}
