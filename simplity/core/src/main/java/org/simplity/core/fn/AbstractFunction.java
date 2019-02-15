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
package org.simplity.core.fn;

import org.simplity.core.comp.ComponentType;
import org.simplity.core.comp.IValidationContext;
import org.simplity.core.util.TextUtil;

/**
 * use this as the base class if you are developing a function meant only for
 * this. the core function methods for concrete class
 */
public abstract class AbstractFunction implements IFunction {
	protected static final ComponentType MY_TYPE = ComponentType.FUNCTION;

	@Override
	public String getSimpleName() {
		String name = this.getClass().getSimpleName();
		return TextUtil.classNameToName(name);
	}

	@Override
	public String getQualifiedName() {
		return this.getSimpleName();
	}

	@Override
	public void getReady() {
		//
	}

	@Override
	public ComponentType getComponentType() {
		return MY_TYPE;
	}

	@Override
	public void validate(IValidationContext ctx) {
		//
	}
}
