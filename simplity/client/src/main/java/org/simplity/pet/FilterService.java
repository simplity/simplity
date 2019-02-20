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
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.dm.DbTable;
import org.simplity.core.idb.DbAccessType;
import org.simplity.core.idb.IDbClient;
import org.simplity.core.idb.IDbHandle;
import org.simplity.core.idb.IReadOnlyHandle;
import org.simplity.core.rdb.DbUsage;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.trans.IProcessor;
import org.simplity.core.trans.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * important to remember that the processor class, like servlets, is
 * instantiated once and used repeatedly in a JVM. Hence, its instance fields
 * are to be immutable during method calls. We use the worker-class instance to
 * have run-time fields during service execution
 *
 * @author simplity.org
 *
 */
public class FilterService implements IProcessor {
	protected static Logger logger = LoggerFactory.getLogger(FilterService.class);
	private static final String OWNER = "pet.owner";
	private static final String PET = "pet.petDetail";

	@Override
	public void execute(ServiceContext ctx) {
		/*
		 * all infrastructure support is provided by app. get a handle to that
		 * first
		 */
		Application app = Application.getActiveInstance();
		logger.debug("service invoked and worker is going to be created");
		Worker worker = new Worker(ctx, app);
		app.getRdbSetup().accessDb(worker, DbAccessType.READ_ONLY, null);

	}

	@Override
	public boolean executeAsAction(ServiceContext ctx) {
		/*
		 * implement this if you want other services to call this service as an
		 * action/step in their processing logic
		 */
		return false;
	}

	@Override
	public void validate(IValidationContext vtx, Service service) {
		/*
		 * this can be used to validate any external dependency this service has
		 * at design time. called by eclipse-plugin
		 */
	}

	@Override
	public void getReady(Service service) {
		// we are ever-ready
	}

	@Override
	public DbUsage getDbUsage() {
		return DbUsage.READ_WRITE;
	}

	private class Worker implements IDbClient {
		private final ServiceContext ctx;
		private final Application app;

		protected Worker(ServiceContext ctx, Application app) {
			this.ctx = ctx;
			this.app = app;
		}

		@Override
		public boolean accessDb(IDbHandle dbHandle) {
			DbTable owner = (DbTable) this.app.getRecord(OWNER);
			IDataSheet ds = owner.filter(owner, this.ctx, (IReadOnlyHandle) dbHandle, this.ctx.getUserId());
			this.ctx.putDataSheet(owner.getDefaultSheetName(), ds);

			/*
			 * read rows from pets for the filtered owners
			 */
			int nbrRows = ds.length();
			if (nbrRows > 0) {
				logger.info("{} rows filtered for owners. going to read pet details for these owners");
				DbTable pet = (DbTable) this.app.getRecord(PET);
				nbrRows = pet.filterForParents(ds, (IReadOnlyHandle) dbHandle, pet.getDefaultSheetName(), false,
						this.ctx);
				logger.info("{} rows filtered from pets as pet details for the filtered-owners");
			} else {
				logger.info("NO rows in owners found for input criterion");
			}
			return true;
		}
	}
}
