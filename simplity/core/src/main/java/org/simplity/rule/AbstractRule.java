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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.simplity.core.expr.BinaryOperator;
import org.simplity.core.expr.Expression;
import org.simplity.core.expr.Operand;
import org.simplity.core.expr.UnaryOperator;

/**
 * a rule describes how to calculate value for a field
 *
 * @author simplity.org
 */
public abstract class AbstractRule implements IRule {

	protected static final char EMPTY_LINE = '\n';
	protected static final String L0 = "\n\t";
	protected static final String L0_CLOSE = "\n\t}";
	protected static final String L1 = "\n\t\t";
	protected static final String L1_CLOSE = "\n\t\t}";
	protected static final String L2 = "\n\t\t\t";

	/**
	 * name of a rule has to be unique within a rulesContext so that it is
	 * uniquely identified by this name
	 */
	String name;
	/**
	 * in case local fields are used in the calculation steps, we need steps to
	 * calculate these local fields. Local fields are calculated on a need
	 * basis. However, it is assumed that the local fields are to be calculated
	 * in the same order as in this array. That is, if the third field is
	 * required, first, second and the third one are calculated in that order
	 */
	AbstractRule[] localRules;

	/**
	 * default
	 */
	public AbstractRule() {
		//
	}

	/**
	 *
	 * @param name
	 * @param localRules
	 */
	public AbstractRule(String name, AbstractRule[] localRules) {
		this.name = name;
		this.localRules = localRules;
	}

	@Override
	public void toJava(StringBuilder src, Map<String, Integer> constIndexes,
			Map<String, Integer> inputIndexes, Map<String, Integer> globalIndexes) {
		/*
		 * caller has already defined a field named "_r" our job is to assign
		 * value that
		 */
		/*
		 * define and calculate local fields if required
		 */
		if (this.localRules != null && this.localRules.length > 0) {
			for (AbstractRule local : this.localRules) {
				src.append(L1).append("long ").append(local.name).append(" = 0;");
				local.emit(src, constIndexes, inputIndexes, globalIndexes, local.name);
			}
		}
		this.emit(src, constIndexes, inputIndexes, globalIndexes, "_r");
	}

	protected abstract void emit(StringBuilder src, Map<String, Integer> constIndexes,
			Map<String, Integer> inputIndexes, Map<String, Integer> globalIndexes, String fieldName);

	@Override
	public void validate(String indexedName, Set<String> globalNames, Set<String> functionNames, List<String> errors) {
		if (this.name == null || this.name.isEmpty()) {
			errors.add("Rule that is associated with " + indexedName
					+ " has not set its name attribute. It must be set to " + indexedName);
			this.name = indexedName;
		} else if (this.name.equals(indexedName) == false) {
			errors.add("Rule is named as " + this.name + " but it is indexed as " + indexedName
					+ ". This will lead to null-pointer exception at run time."
					+ " we have set it temporarily for this run");
			this.name = indexedName;
		}
		Set<String> localNames = new HashSet<>();
		if (this.localRules != null) {
			for (AbstractRule local : this.localRules) {
				if (local.localRules != null && local.localRules.length > 0) {
					errors.add("Local rules can not have local rules in them again.");
				}
				local.validateSpecific(localNames, globalNames, functionNames, errors, true);
				localNames.add(local.name);
			}
		}
		this.validateSpecific(localNames, globalNames, functionNames, errors, false);
	}

	protected abstract void validateSpecific(Set<String> localNames, Set<String> globalNames, Set<String> functionNames,
			List<String> errors, boolean isLocal);

	private static boolean isLocal(String fieldName) {
		return fieldName.charAt(0) == '_';
	}

	protected static void validateExpr(Expression exp, Set<String> localNames, Set<String> globalNames,
			Set<String> functionNames,
			List<String> errors, boolean isLocal) {
		for (Operand op : exp.getOperands()) {
			int opType = op.getOperandType();
			UnaryOperator uop = op.getUnaryOperator();
			if (uop != null) {
				if (uop == UnaryOperator.Not || uop == UnaryOperator.Minus) {
					// we are ok
				} else {
					errors.add("Only - and ! are allowed as unary operators. Should not use " + uop);
				}
			}
			if (opType == Operand.FIELD) {
				String fieldName = op.getOperandValue().toString();
				if (isLocal(fieldName)) {
					if (localNames.contains(fieldName) == false) {
						if (isLocal) {
							errors.add("Local field " + fieldName
									+ " can not be used before it is defined. "
									+ "Ensure that the local rules are defiined in the right order, so that each localfield is used only after it is calculated");
						} else {
							errors.add(
									"Local field " + fieldName + " is used, but has no local rule to calculate it");
						}
					}
				} else if (!globalNames.contains(fieldName)) {
					errors.add("Feild " + fieldName
							+ " is not a valid global field. (it is not a constant, input, or global field)");
				}
				continue;
			}
			if (opType == Operand.CONSTANT) {
				continue;
			}

			if (opType == Operand.EXPRESSION) {
				validateExpr(op.getOperandExpression(), localNames, globalNames, functionNames, errors, isLocal);
				continue;
			}

			if (opType == Operand.FUNCTION) {
				String functionName = op.getOperandValue().toString();
				if (functionNames.contains(functionName) == false) {
					errors.add("Function " + functionName + " is not defined for the rule set");
				}
				continue;
			}

			errors.add("An expression uses " + op
					+ " as an operand. This type of operand is not allowed in the expression");
			continue;
		}
	}

	protected static void emitExpr(Expression exp, StringBuilder src, Map<String, Integer> constIndexes,
			Map<String, Integer> inputIndexes,
			Map<String, Integer> globalIndexes) {
		for (Operand op : exp.getOperands()) {
			UnaryOperator uop = op.getUnaryOperator();
			if (uop != null) {
				src.append(uop);
			}
			int opType = op.getOperandType();
			if (opType == Operand.FIELD) {
				String fieldName = op.getOperandValue().toString();
				src.append(translateName(fieldName, constIndexes, inputIndexes, globalIndexes));
			} else if (opType == Operand.CONSTANT) {
				src.append(op.getOperandValue());
			} else if (opType == Operand.EXPRESSION) {
				src.append('(');
				emitExpr(op.getOperandExpression(), src, constIndexes, inputIndexes, globalIndexes);
				src.append(')');
			} else if (opType == Operand.FUNCTION) {
				src.append(op.getOperandValue()).append('(');
				emitExpr(op.getOperandExpression(), src, constIndexes, inputIndexes, globalIndexes);
				src.append(')');
			} else {
				src.append("0 /* invalid operand type ").append(opType).append(" */ ");
			}
			BinaryOperator bop = op.getBinaryOperator();
			if (bop != null) {
				src.append(' ').append(bop).append(' ');
			}
		}
	}

	protected static String translateName(String fieldName, Map<String, Integer> constIndexes,
			Map<String, Integer> inputIndexes,
			Map<String, Integer> globalIndexes) {
		if (isLocal(fieldName)) {
			return fieldName;
		}
		Integer obj = constIndexes.get(fieldName);
		if (obj != null) {
			return "this.cVal[" + obj.toString() + ']';
		}
		obj = inputIndexes.get(fieldName);
		if (obj != null) {
			return "this.iVal[" + obj.toString() + ']';
		}
		obj = globalIndexes.get(fieldName);
		if (obj != null) {
			return "this.f" + obj.toString() + "()";
		}
		return "0 /*" + fieldName + "*/";
	}
}
