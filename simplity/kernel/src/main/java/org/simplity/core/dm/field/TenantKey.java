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

import org.simplity.core.data.IFieldsCollection;
import org.simplity.core.service.ServiceContext;
import org.simplity.core.value.Value;

/**
 * column that is used in every table in a multi-tenancy design to store data
 * about different customer-organizations in the same database
 */
public class TenantKey extends CreatedByUser {
	/**
	 *
	 */
	public TenantKey() {
		this.fieldType = FieldType.TENANT_KEY;
		this.insertable = true;
	}

	@Override
	public Value getValue(IFieldsCollection values, ServiceContext ctx) {
		if (ctx != null) {
			return ctx.getTenantId();
		}
		return super.getValue(values, ctx);
	}
}
