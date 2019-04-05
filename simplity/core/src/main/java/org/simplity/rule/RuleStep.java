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

package org.simplity.rule;

import org.simplity.core.comp.FieldMetaData;
import org.simplity.core.expr.Expression;

/**
 * a step in a rule.
 *
 * @author simplity.org
 *
 */
public class RuleStep {
	/**
	 * boolean expression that serves as "if" for this step. Only the lst step
	 * of a rule can have this as null
	 */
	Expression condition;
	/**
	 * expression to be evaluated to get the value for the objective.
	 */
	@FieldMetaData(isRequired = true)
	Expression expression;

	/**
	 *
	 */
	public RuleStep() {
		//
	}

	/**
	 * @param condition
	 * @param expr
	 */
	public RuleStep(Expression condition, Expression expr) {
		this.condition = condition;
		this.expression = expr;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		if (this.condition != null) {
			s.append("IF (").append(this.condition.toString()).append(") ");
		}
		s.append("Value  = ").append(this.expression.toString());
		return s.toString();
	}
}
