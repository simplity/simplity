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

package org.simplity.kernel.trans;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.comp.FieldMetaData;
import org.simplity.kernel.expr.Expression;
import org.simplity.kernel.expr.InvalidOperationException;
import org.simplity.kernel.service.ServiceContext;
import org.simplity.kernel.util.TextUtil;
import org.simplity.kernel.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * set value to a field in the service context
 *
 * @author simplity.org
 */
public class SetValue extends AbstractNonDbAction {
	private static final Logger logger = LoggerFactory.getLogger(SetValue.class);

	/** field name */
	@FieldMetaData(isRequired = true)
	String fieldName;

	/**
	 * value, if it is known at design time, or a single field, like 23 or
	 * $customerName
	 */
	String fieldValue;
	/** if value is more complex.. */
	Expression expression;

	/*
	 * parsed and cached value
	 */
	private Value parsedValue;
	/*
	 * if fieldValue is a field, then we keep that parsed name
	 */
	private String parsedField;

	@Override
	protected boolean act(ServiceContext ctx) {
		Value value = null;
		if (this.parsedValue != null) {
			value = this.parsedValue;
		} else if (this.parsedField != null) {
			value = ctx.getValue(this.parsedField);
		} else if (this.expression != null) {
			try {
				value = this.expression.evaluate(ctx);
			} catch (InvalidOperationException e) {
				throw new ApplicationError(
						"Expression = "
								+ this.expression.toString()
								+ "\n error while evaluating : "
								+ e.getMessage());
			}
		} else {

			logger.info("Field " + this.fieldName + " is removed from context");
		}
		ctx.setValue(this.fieldName, value);
		return true;
	}

	@Override
	public void getReady(int idx, TransactionProcessor task) {
		super.getReady(idx, task);
		if (this.fieldValue != null) {
			if (this.expression != null) {
				throw new ApplicationError(
						"SetValue action '"
								+ this.actionName
								+ "' has confused me by specifying both expression and fieldValue. Only one of this should be specified ");
			}
			this.parsedField = TextUtil.getFieldName(this.fieldValue);
			if (this.parsedField == null) {
				this.parsedValue = Value.parseValue(this.fieldValue);
				if (this.parsedValue == null) {
					throw new ApplicationError(
							"SetValue action "
									+ this.actionName
									+ " has an invalid fieldValue="
									+ this.fieldValue);
				}
			}
		}
	}
}
