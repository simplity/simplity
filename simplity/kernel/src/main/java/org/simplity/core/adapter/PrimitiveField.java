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

package org.simplity.core.adapter;

import org.simplity.core.ApplicationError;
import org.simplity.core.app.internal.ParameterRetriever;
import org.simplity.core.expr.Expression;
import org.simplity.core.expr.InvalidOperationException;
import org.simplity.core.service.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * primitive field that takes string as its value.
 *
 * @author simplity.org
 *
 */
public class PrimitiveField extends AbstractField {
	private static final Logger logger = LoggerFactory.getLogger(PrimitiveField.class);
	/**
	 * default Value, if it is fixed at design time.
	 */
	String defaultValue;
	/**
	 * default value as a run-time property name
	 */
	String defaultProperty;
	/**
	 * if the default value is to be calculated at run time, and if it is simple
	 * enough to be expressed as an expression
	 */
	Expression defaultExpression;

	@Override
	public void copy(IDataSource source, IDataTarget target, ServiceContext ctx) {
		String fieldValue = null;
		if (this.fromName != null) {
			fieldValue = source.getPrimitiveValue(this.fromName);
		}
		if (fieldValue == null) {
			fieldValue = this.getDefaultValue(ctx);
		}

		if (fieldValue == null) {
			logger.info("Source has no value named {} for target field {} ", this.fromName, this.toName);
		} else {
			target.setPrimitiveValue(this.toName, fieldValue);
			logger.info("field {} set to {}", this.toName, fieldValue);
		}
	}

	private String getDefaultValue(ServiceContext ctx) {
		if (this.defaultValue != null) {
			return this.defaultValue;
		}
		if (this.defaultProperty != null) {
			return ParameterRetriever.getValue(this.defaultProperty, ctx);
		}
		if (this.defaultExpression != null) {
			try {
				return this.defaultExpression.evaluate(ctx).toString();
			} catch (InvalidOperationException e) {
				throw new ApplicationError(e, " error while evaluating expression " + this.defaultExpression);
			}
		}
		return null;
	}

	@Override
	public void getReady() {
		super.getReady();
		if(this.toName == null) {
			logger.warn("Primitive field with fromName={} serves no purpose as there is no toField", this.fromName);
		}
		if(this.fromName == null && this.defaultValue == null && this.defaultProperty == null && this.defaultExpression == null) {
			logger.warn("Primitive field with toName={} serves no purpose as there is no fromField or default value", this.toName);
		}
	}
}
