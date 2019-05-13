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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.simplity.core.expr.Expression;

/**
 * simplest way to calculate a value : just evaluate an expression
 *
 * @author simplity.org
 */
public class ExprRule extends AbstractRule {

	/**
	 * evaluate this expression to get the value
	 */
	Expression expression;

	/**
	 *
	 * @param ruleName
	 * @param localRules
	 * @param expr
	 */
	public ExprRule(String ruleName, AbstractRule[] localRules, Expression expr) {
		super(ruleName, localRules);
		this.expression = expr;
	}

	@Override
	protected void emit(StringBuilder src, Map<String, Integer> constIndexes, Map<String, Integer> inputIndexes,
			Map<String, Integer> globalIndexes, String fieldName) {
		src.append(L1).append(fieldName).append(" = ");
		if (this.expression == null) {
			src.append("0 /* null expression */");
		} else {
			emitExpr(this.expression, src, constIndexes, inputIndexes, globalIndexes);
		}
		src.append(";");
	}

	@Override
	protected void validateSpecific(Set<String> localNames, Set<String> globalNames, Set<String> functionNames,
			List<String> errors, boolean isLocal) {
		if (this.expression == null) {
			errors.add("Expression is null for rule " + this.name);
		}
		validateExpr(this.expression, localNames, globalNames, functionNames, errors, isLocal);
	}

}
