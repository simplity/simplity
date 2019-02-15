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
package org.simplity.core.trans;

import org.simplity.core.ApplicationError;
import org.simplity.core.app.Application;
import org.simplity.core.app.IResponseWriter;
import org.simplity.core.comp.ComponentType;
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.idb.IDbHandle;
import org.simplity.core.idb.IReadOnlyHandle;
import org.simplity.core.rdb.DbUsage;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.sql.Sql;

/**
 * Read a row/s from as output of a prepared statement/sql
 *
 * @author simplity.org
 */
public class ExtractDirectlyToResponse extends AbstractDbAction {

	/**
	 * fully qualified sql name
	 */
	@FieldMetaData(isRequired = true, isReferenceToComp = true, referredCompType = ComponentType.SQL)
	String sqlName;

	/**
	 * qualified name of the field this result is going to be assigned to. For
	 * example if this is orderDetails of an order for a customer, (and customer
	 * is the root object) then orders.orderDetails is the qualified field name.
	 * TODO: as of now it works only at the root level.
	 */
	String qualifiedFieldName;

	@Override
	protected boolean actWithDb(ServiceContext ctx, IDbHandle handle) {
		IResponseWriter writer = ctx.getWriter();
		if (writer == null) {
			throw new ApplicationError(
					"Design Error: ExtractDirrectlyToResponse task works only if teh response writer is made available in the service context");
		}
		Sql sql = Application.getActiveInstance().getSql(this.sqlName);
		writer.beginObject(this.qualifiedFieldName);
		sql.sqlToJson(ctx, (IReadOnlyHandle) handle, false, writer);
		writer.endObject();
		return true;
	}

	@Override
	public DbUsage getDbUsage() {
		return DbUsage.READ_ONLY;
	}

	@Override
	protected void validateDbAction(IValidationContext vtx, TransactionProcessor task) {
		// nothing
	}
}
