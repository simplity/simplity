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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.simplity.core.expr.Expression;
import org.simplity.core.expr.InvalidExpressionException;

/**
 * value is enumerated based on another field value
 *
 * @author simplity.org
 */
public class RangeRule extends AbstractRule {

	/**
	 * field that has enumerated value. That is, this field can have values from
	 * 0 to n
	 */
	String basedOnField;
	/**
	 * range of values. first range is from zero to < first value. So,each range
	 * includes lower value, and excludes higher value. There will be an
	 * additional range for values equal to or greater than the last entry.
	 *
	 * for e.g.if 60, 80 are the two values in the array, then the tree ranges
	 * are 0-59, 60-79 and 80-above
	 */
	int[] upperLimits;
	/**
	 * one value for each possible range, and one value for value higher than
	 * the last range. this array will have one element more than the ramge
	 * array.
	 */
	Expression[] values;

	/**
	 * @param key
	 * @param locals
	 * @param ranges
	 * @param values
	 */
	public RangeRule(String key, AbstractRule[] locals, int[] ranges, Expression[] values) {
		super(key, locals);
		this.upperLimits = ranges;
		this.values = values;
	}

	@Override
	protected void emit(StringBuilder src, Map<String, Integer> constIndexes, Map<String, Integer> inputIndexes,
			Map<String, Integer> globalIndexes, String fieldName) {
		String f = translateName(this.basedOnField, constIndexes, inputIndexes, globalIndexes);
		src.append(L1).append("long __range = ").append(f).append(';');
		src.append(L1);
		for (int i = 0; i < this.values.length; i++) {
			src.append("if( __range < ").append(this.upperLimits[i]).append("){");
			src.append(L2).append(fieldName).append(" = ");
			emitExpr(this.values[i], src, constIndexes, inputIndexes, globalIndexes);
			src.append(';');
			src.append(L1).append("} else ");
		}
		src.append("{");
		src.append(L2).append(fieldName).append(" = ");
		emitExpr(this.values[this.upperLimits.length], src, constIndexes, inputIndexes, globalIndexes);
		src.append(';');
		src.append(L1_CLOSE);
	}

	@Override
	protected void validateSpecific(Set<String> localNames, Set<String> globalNames, Set<String> functionNames,
			List<String> errors, boolean isLocal) {
		if (this.basedOnField == null) {
			errors.add("basedOnField is null for rule " + this.name);
		}
		if (this.upperLimits == null || this.upperLimits.length == 0) {
			errors.add("No range upper limits specified for field " + this.basedOnField + " in rule " + this.name);
			if (this.upperLimits == null) {
				this.upperLimits = new int[0];
			}
		}
		if (this.values == null) {
			errors.add("No values specified for field " + this.basedOnField + " in rule " + this.name);
			this.values = new Expression[0];
		}
		for (Expression expr : this.values) {
			validateExpr(expr, localNames, globalNames, functionNames, errors, isLocal);
		}
		int nbr = this.upperLimits.length + 1;
		if (this.values.length != nbr) {
			errors.add("There should be " + nbr + " values for the ranges, but " + this.values.length
					+ " values are specified in rule " + this.name);
			int oldNbr = this.values.length;
			this.values = Arrays.copyOf(this.values, nbr);
			if (oldNbr < nbr) {
				try {
					for (int i = oldNbr; i < nbr; i++) {
						this.values[i] = new Expression("0");
					}
				} catch (InvalidExpressionException e) {
					// we know this will not happen
				}
			}
		}
	}
}
