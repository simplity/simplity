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
 * rule that specifies how to calculate based depending on the value of an
 * enumerated field. Like a multiple if-then-else blocks based on possible
 * enumerated values
 *
 * @author simplity.org
 */
public class EnumRule extends AbstractRule {

	/**
	 * field that has enumerated value. That is, this field can have values from
	 * 0 to n.
	 */
	String basedOnField;
	/**
	 * one value for each possible value of the basedOnField, starting with 0,
	 * and one extra for default. That is,the last value of the array is used if
	 * the value does not match 0 to n
	 */
	Expression[] values;

	/**
	 * @param key
	 * @param locals
	 * @param values
	 */
	public EnumRule(String key, AbstractRule[] locals, Expression[] values) {
		super(key, locals);
		this.values = values;
	}

	@Override
	protected void emit(StringBuilder src, Map<String, Integer> constIndexes, Map<String, Integer> inputIndexes,
			Map<String, Integer> globalIndexes, String fieldName) {
		String f = translateName(this.basedOnField, constIndexes, inputIndexes, globalIndexes);
		src.append(L1).append("switch((int)").append(f).append("){");
		int lastIdx = this.values.length - 1;
		int idx = 0;
		for (Expression expr : this.values) {
			src.append(L1);
			if (idx == lastIdx) {
				src.append("default :");
			} else {
				src.append("case ").append(idx).append(':');
			}
			src.append(L2).append(fieldName).append(" = ");
			emitExpr(expr, src, constIndexes, inputIndexes, globalIndexes);
			src.append(';');
			src.append(L2).append("break;");
			idx++;
		}
		src.append(L1_CLOSE);
	}

	@Override
	protected void validateSpecific(Set<String> localNames, Set<String> globalNames, Set<String> functionNames,
			List<String> errors, boolean isLocal) {
		if (this.basedOnField == null) {
			errors.add("basedOnField is null for rule " + this.name);
		}
		if (this.values == null || this.values.length == 0) {
			errors.add("No values specified for enumerated field " + this.basedOnField + " in rule " + this.name);
			if (this.values == null) {
				this.values = new Expression[0];
			}
		}
		for (Expression expr : this.values) {
			validateExpr(expr, localNames, globalNames, functionNames, errors, isLocal);
		}
	}

}
