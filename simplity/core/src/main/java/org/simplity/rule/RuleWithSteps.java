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

/**
 * rule that has multiple steps
 *
 * @author simplity.org
 */
public class RuleWithSteps extends AbstractRule {

	/**
	 * steps of the rule
	 */
	RuleStep[] steps;

	/**
	 * @param key
	 * @param locals
	 * @param steps
	 */
	public RuleWithSteps(String key, AbstractRule[] locals, RuleStep[] steps) {
		super(key, locals);
		this.steps = steps;
	}

	@Override
	protected void emit(StringBuilder src, Map<String, Integer> constIndexes, Map<String, Integer> inputIndexes,
			Map<String, Integer> globalIndexes, String fieldName) {
		src.append(L1);
		int idx = 1;
		int lastOne = this.steps.length - 1;
		for (RuleStep step : this.steps) {
			if (idx > 0) {
				src.append(" else ");
			}

			if (idx == lastOne) {
				src.append("{");
			} else {
				src.append("if(");
				if (step.condition == null) {
					src.append("true");
				} else {
					emitExpr(step.condition, src, constIndexes, inputIndexes, globalIndexes);
				}
				src.append("){");
			}
			src.append(L2).append(fieldName).append(" = ");
			src.append(L1_CLOSE);
		}
	}

	@Override
	protected void validateSpecific(Set<String> localNames, Set<String> globalNames, Set<String> functionNames,
			List<String> errors, boolean isLocal) {
		if (this.steps == null || this.steps.length == 0) {
			errors.add("calculation steps are required for rule " + this.name);
			if (this.steps == null) {
				this.steps = new RuleStep[0];
			}
		}
		int lastIdx = this.steps.length - 1;
		int idx = 0;
		for (RuleStep step : this.steps) {
			if (step.expression == null) {
				errors.add("Rule step " + idx + " (0 based) iin rule " + this.name + " has no expression.");
			} else {
				validateExpr(step.expression, localNames, globalNames, functionNames, errors, isLocal);
			}
			if (step.condition == null) {
				if (idx != lastIdx) {
					errors.add("Rule step " + idx + " (0 based) of rule " + this.name
							+ " is an unconditional one. This will make the rest of the steps redundant");
				}
			} else {
				if (idx == lastIdx) {
					errors.add("Last step of of " + this.name
							+ " is conditional. It should be unconditional to guarantee a value");
				}
				validateExpr(step.condition, localNames, globalNames, functionNames, errors, isLocal);
			}
			idx++;
		}
	}
}
