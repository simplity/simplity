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

import org.simplity.core.adapter.DataAdapter;
import org.simplity.core.adapter.IDataSource;
import org.simplity.core.adapter.IDataTarget;
import org.simplity.core.adapter.destn.ContextDataTarget;
import org.simplity.core.adapter.source.ContextDataSource;
import org.simplity.core.app.Application;
import org.simplity.core.comp.ComponentType;
import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.service.ServiceContext;

/**
 * A action that has no processing. It always succeeds. Hence it can be used
 * <p>
 * <li>marking a target action for the navigation/flow</li>
 * <li>adding a message. use messageOnSuccess.</li>
 *
 * @author simplity.org
 *
 */
public class CopyData extends AbstractNonDbAction {

	/**
	 * fully qualified name of adapter to be used for copying data
	 */
	@FieldMetaData(isRequired = true, isReferenceToComp = true, referredCompType = ComponentType.ADAPTER)
	String adapterName;

	/**
	 * name of the object/data sheet to copy data from. skip this if data from
	 * service context is to be copied
	 */
	String sourceObjectName;

	/**
	 * name of target object. skip this if data is to be copied to the service
	 * context itself
	 */

	String targetObjectName;

	/**
	 * fully qualified name of the class to be used to create the target object.
	 * Skip this if the object is already available in the context
	 */
	String targetClassName;

	@Override
	protected boolean act(ServiceContext ctx) {
		DataAdapter adapter = Application.getActiveInstance().getDataAdapter(this.adapterName);
		IDataSource source = null;
		IDataTarget target = null;
		if (this.sourceObjectName != null) {
			source = ctx.getDataSource(this.sourceObjectName);
		} else {
			source = new ContextDataSource(ctx);
		}
		if (this.targetObjectName != null) {
			target = ctx.getDataTarget(this.targetObjectName, this.targetClassName);
		} else {
			target = new ContextDataTarget(ctx);
		}
		adapter.copy(source, target, ctx);
		return true;
	}
}
