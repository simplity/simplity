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

package org.simplity.kernel.adapter;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.IComponent;
import org.simplity.kernel.comp.IValidationContext;
import org.simplity.kernel.service.ServiceContext;

/**
 * Specifications for copying data from source to target
 *
 * @author simplity.org
 *
 */

public class DataAdapter implements IComponent {

	/**
	 * unique name within a module
	 */
	String name;

	/**
	 * optional. adapters can be organized nicely into modules. Also, this helps
	 * in creating sub-apps
	 */
	String moduleName;

	/**
	 * fields to be copied using this adapter
	 */
	AbstractField[] fields;

	/**
	 * copy data from the source to the target
	 *
	 * @param source
	 *            non-null source to copy data from
	 * @param target
	 *            non-null target to set data to
	 * @param ctx
	 *            non-null service context
	 */
	public void copy(IDataSource source, IDataTarget target, ServiceContext ctx) {
		for (AbstractField field : this.fields) {
			field.copy(source, target, ctx);
		}
	}

	/**
	 *
	 * @return fields. null to imply "all possible fields" wherever such a
	 *         concept is possible
	 */
	public AbstractField[] getFields() {
		return this.fields;
	}

	@Override
	public void getReady() {
		if (this.fields == null) {
			throw new ApplicationError("Adapter requires one or more input fields");
		}
		for (AbstractField field : this.fields) {
			field.getReady();
		}
	}

	@Override
	public String getSimpleName() {
		return this.name;
	}

	@Override
	public String getQualifiedName() {
		if (this.moduleName == null) {
			return this.name;
		}
		return this.moduleName + '.' + this.name;
	}

	@Override
	public void validate(IValidationContext vtx) {
		if (this.fields != null) {
			for (AbstractField field : this.fields) {
				field.validate(vtx);
			}
		}
		// return 0;
	}

	@Override
	public ComponentType getComponentType() {
		return ComponentType.ADAPTER;
	}
}
