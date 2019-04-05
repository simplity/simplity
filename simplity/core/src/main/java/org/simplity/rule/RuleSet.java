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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.simplity.core.expr.Expression;
import org.simplity.core.expr.Operand;
import org.simplity.core.util.TextUtil;
import org.simplity.core.value.IntegerValue;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;
import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;

/**
 * @author simplity.org
 *
 */
public class RuleSet {
	private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	static {
		DATE_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	private static final char EMPTY_LINE = '\n';
	private static final String L0 = "\n\t";
	private static final String L0_CLOSE = "\n\t}";
	private static final String L1 = "\n\t\t";
	private static final String L1_CLOSE = "\n\t\t}";
	private static final String L2 = "\n\t\t\t";

	/**
	 * unique name of this rule-set
	 */
	String name;
	/**
	 * meta-data/externalized-constants used by rules. That is, fields that have
	 * pre-fixed values
	 */
	Map<String, Long> constants = new HashMap<>();

	/**
	 * fields that require rules to calculate them
	 */
	Map<String, IRule> rules = new HashMap<>();

	/**
	 * rules are backward-chained to ensure that the fields are lazily
	 * calculated (only when they are required) However, this may lead to
	 * increased call-depth (recursions). If some fields are almost always
	 * required, it may be a good idea to calculate them eagerly (up-front).
	 * Note that these are not output parameters. (they are calculated anyways).
	 */
	String[] fieldsToInitialize = new String[0];

	/**
	 * this rule set is designed to get these parameters (in that order) as
	 * input. input names shoudl not clash with any other global fields.
	 */
	String[] inputParameters;

	/**
	 * this rule set is designed to calculate and return values for these
	 * parameters as output. an output parameter should either be a constant or
	 * a global field
	 */
	String[] outputParameters;

	/**
	 * appends java code for class declaration, excluding package declaration.
	 *
	 * @param baseClass
	 *            base class that the generated class extends. it should be
	 *            either <code>AbstractCalculator</code> or its sub-class
	 *
	 * @param src
	 *            source code for this class. should have all code before class
	 *            declaration, like package and copyright statements
	 * @param tst
	 *            optional. null if this is not required. junit source code to
	 *            test this class. Should have all the code before class
	 *            declarations, including includes for junit
	 * @param testValues
	 *            non-null if tst is non-null. Each row is one test case. In
	 *            each first n columns are input values and the rest are
	 *            expected output values. Input and output parameters must
	 *            follow the pre-designed sequence
	 * @param errors
	 *            any error in the rules are added to this list. Source is
	 *            generated even when there are errors
	 */
	public void generateSource(Class<AbstractCalculator> baseClass, StringBuilder src, StringBuilder tst,
			long[][] testValues,
			List<String> errors) {
		/*
		 * validate will add all possible errors. Also, it ensures that all the
		 * attributes are non-null (could be empty though)
		 */
		this.validate(errors);
		/*
		 * Should we generate even when there are errors? We will because we can
		 * !!. Up to the caller to decide what to do with that
		 */
		src.append("package ").append(baseClass.getPackage().getName())
				.append(";\n/**\n * generated class for rule set ").append(this.name);
		src.append("\n * generated at ").append(DATE_FORMATTER.format(new Date())).append("\n **/");
		src.append("public class ").append(TextUtil.nameToClassName(this.name)).append(" extends ")
				.append(baseClass.getSimpleName()).append(" {");
		/*
		 * we assign index numbers to constants and globals
		 */
		int idx = 0;
		Map<String, Integer> globalIndexes = new HashMap<>();
		for (String f : this.rules.keySet()) {
			globalIndexes.put(f, idx++);
		}
		/*
		 * small optimization : collect index while looping for emitting
		 * declaration
		 */
		Map<String, Integer> constIndexes = this.emitConstants(src);
		Map<String, Integer> inputIndexes = this.emitInputParams(src);
		this.emitOutputParams(src);

		int nbrGlobals = this.rules.size();
		src.append(L0).append("private long[] val = new long[").append(nbrGlobals).append("];");
		src.append(L0).append("private boolean[] isReady;");
		src.append(L0).append("private boolean[] isLooping;");
		src.append(L0).append("private long[] ival;");

		this.emitMainFunctions(src, globalIndexes);
		for (Map.Entry<String, IRule> entry : this.rules.entrySet()) {
			this.emitRule(src, entry.getKey(), entry.getValue(), constIndexes, inputIndexes, globalIndexes);
		}

		src.append("}\n");

		if (tst != null && testValues != null) {
			this.generateJunit(tst, testValues);
		}
		return;
	}

	private void validate(List<String> errors) {
		if (this.name == null || this.name.isEmpty()) {
			this.name = "unknownRuleSet";
			errors.add("Rule-set must have a name that is used as name of its generated class.");
		}

		/*
		 * avoid dealing with nulls
		 */
		if (this.inputParameters == null) {
			this.inputParameters = new String[0];
		}
		if (this.outputParameters == null) {
			this.outputParameters = new String[0];
		}
		if (this.constants == null) {
			this.constants = new HashMap<>();
		}
		if (this.fieldsToInitialize == null) {
			this.fieldsToInitialize = new String[0];
		}
		if (this.rules == null) {
			this.rules = new HashMap<>();
		}

		/*
		 * are names okay?
		 */
		this.validateInput(errors);
		this.validateOutput(errors);
		this.validateInit(errors);
		this.validateConstants(errors);
		this.validateGlobals(errors);

		/*
		 * validate individual rules
		 */
		Set<String> globalNames = new HashSet<>();
		globalNames.addAll(this.rules.keySet());
		globalNames.addAll(this.constants.keySet());
		Set<String> functionNames = AbstractCalculator.getPredefinedFunctions();
		for (Map.Entry<String, IRule> entry : this.rules.entrySet()) {
			IRule rule = entry.getValue();
			if (rule != null) {
				rule.validate(entry.getKey(), globalNames, functionNames, errors);
			}
		}
	}

	private void validateInput(List<String> errors) {
		for (String fieldName : this.inputParameters) {
			this.validateGlobalName(fieldName, errors);
			if (this.rules.containsKey(fieldName)) {
				errors.add("Input field name " + fieldName
						+ " is also used as a globalfield with a rule associated with that.");
			}
			if (this.constants.containsKey(fieldName)) {
				errors.add("Input field name " + fieldName + " is also defined as a constant");
			}
		}
	}

	private void validateOutput(List<String> errors) {
		for (String fieldName : this.outputParameters) {
			if (this.rules.containsKey(fieldName)) {
				return;
			}
			if (this.constants.containsKey(fieldName)) {
				return;
			}
			errors.add("Output field " + fieldName
					+ " must be one of the global fields. (either a constant or a field associated with a rule");
		}
	}

	private void validateInit(List<String> errors) {
		for (String fieldName : this.fieldsToInitialize) {
			if (this.rules.containsKey(fieldName)) {
				return;
			}
			errors.add("Field " + fieldName + " is not associated with a rule. it can not be initialized");
		}
	}

	private void validateConstants(List<String> errors) {
		for (Map.Entry<String, Long> entry : this.constants.entrySet()) {
			String fieldName = entry.getKey();
			this.validateGlobalName(fieldName, errors);
			Long value = entry.getValue();
			if (value == null) {
				errors.add("Constant " + fieldName + " must have an integral value but is set to " + value);
			}
			if (this.rules.containsKey(fieldName)) {
				errors.add("Constant field " + fieldName + " also has a rule associated with it.");
			}
		}
	}

	private void validateGlobals(List<String> errors) {
		for (Map.Entry<String, IRule> entry : this.rules.entrySet()) {
			String fieldName = entry.getKey();
			this.validateGlobalName(fieldName, errors);
			IRule rule = entry.getValue();
			if (rule == null) {
				errors.add("Rule associated with field " + fieldName + " is null");
			}
		}
	}

	/**
	 *
	 * @param fieldName
	 * @param errors
	 */
	public void validateGlobalName(String fieldName, List<String> errors) {
		if (fieldName.charAt(0) == '_') {
			errors.add("Field name " + fieldName
					+ " is a global field, but it starts with '_' that is reserved for local fields");
		}
	}

	/**
	 *
	 * @param fieldName
	 * @param errors
	 */
	public void validateLocalName(String fieldName, List<String> errors) {
		if (fieldName.charAt(0) != '_') {
			errors.add("Field name " + fieldName
					+ " is a local field, but is does not start with '_'. This convention is used to avoid clash between global and local field names");
		}
	}

	private Map<String, Integer> emitConstants(StringBuilder src) {
		Map<String, Integer> indexes = new HashMap<>();
		src.append(L0).append("private static final long[] C = {");
		int idx = 0;
		for (Map.Entry<String, Long> entry : this.constants.entrySet()) {
			if (idx != 0) {
				src.append(", ");
			}
			indexes.put(entry.getKey(), idx++);
			Long value = entry.getValue();
			if (value == null) {
				value = 0L;
			}
			src.append(value);

		}
		src.append("};");
		return indexes;
	}

	private Map<String, Integer> emitInputParams(StringBuilder src) {
		Map<String, Integer> indexes = new HashMap<>();
		src.append(L0).append("private static final String[] INPUTS = {");
		int idx = 0;
		for (String fieldName : this.inputParameters) {
			if (idx != 0) {
				src.append(", ");
			}
			src.append('"').append(fieldName).append('"');
			indexes.put(fieldName, idx);
		}
		src.append("};");
		return indexes;
	}

	private void emitOutputParams(StringBuilder src) {
		src.append(L0).append("private static final String[] OUTPUTS = {");
		boolean firstOne = true;
		for (String fieldName : this.outputParameters) {
			if (firstOne) {
				firstOne = false;
			} else {
				src.append(", ");
			}
			src.append('"').append(fieldName).append('"');
		}
		src.append("};");
	}

	private static final String OVERRIDE = L0 + "@Override" + L0;

	private void emitMainFunctions(StringBuilder src, Map<String, Integer> globalIndexes) {
		src.append(OVERRIDE).append("public String[] getInputParameters(){");
		src.append(L1).append("return INPUTS;");
		src.append(L0_CLOSE);

		src.append(OVERRIDE).append("public String[] getOutputParameters(){");
		src.append(L1).append("return OUTPUTS;");
		src.append(L0_CLOSE);

		String excp = RuleException.class.getName();
		src.append(OVERRIDE).append("public long[] calculate(long[] inp)throws ")
				.append(excp).append("{");
		/*
		 * set input values
		 */
		src.append(L1).append("if (inp == null){");
		src.append(L2).append("throw new ").append(excp).append("(\"input array is null\");");
		src.append(L1_CLOSE);

		int inpNbr = this.inputParameters.length;
		src.append(L1).append("if (inp.length != ").append(inpNbr).append("){");
		src.append(L2).append("throw new ").append(excp)
				.append("(\"input array has \" + inp.length + \" elements but we epxected ").append(inpNbr)
				.append(" elements\");");
		src.append(L1_CLOSE);

		int nbrGlobals = this.rules.size();
		src.append(L1).append("this.ival = inp;");
		src.append(L1).append("this.isReady = new boolean[").append(nbrGlobals).append("];");
		src.append(L1).append("this.isLooping = new boolean[").append(nbrGlobals).append("];");
		/*
		 * calculate initial fields
		 */
		for (String fieldName : this.fieldsToInitialize) {
			Integer idx = globalIndexes.get(fieldName);
			if (idx == null) {
				src.append(L1).append("//invalid parameter ->").append(fieldName);
			} else {
				src.append(L1).append("_g").append(idx).append("();");
			}
		}
		/*
		 * calculate and prepare output array
		 */
		src.append(L1).append("long[] _r = {");
		boolean firstOne = true;
		for (String fieldName : this.outputParameters) {
			if (firstOne) {
				firstOne = false;
			} else {
				src.append(", ");
			}
			Integer idx = globalIndexes.get(fieldName);
			if (idx == null) {
				src.append("0");
			} else {
				src.append("_g").append(idx).append("()");
			}
		}
		src.append("};");
		src.append(L1).append("return _r;");
		src.append(L0_CLOSE);
		/*
		 * global check before calcr
		 */
		src.append(L0).append("private  bool _toCalc(int idx){");

		src.append(L1).append("if (isReady[idx]){");
		src.append(L2).append("return false;");
		src.append(L1_CLOSE);

		src.append(EMPTY_LINE);
		src.append(L1).append("if (isLooping[idx]){");
		src.append(L2).append("throw new ").append(RuleException.class.getName()).append(
				"(\"Semantic error in rules : global field for index \" + idx + \" has a rule that recursivel requires its own value, there by causing an infinite loop\"); ");
		src.append(L1_CLOSE);

		src.append(EMPTY_LINE);
		src.append(L1).append("return true;");
		src.append(L0_CLOSE);

		/*
		 * global check for after calc
		 */
		src.append(L0).append("private  void _calced(int idx){");

		src.append(L1).append("isReady[idx] = true;");
		src.append(L2).append("isLooping[idx] = false;");
		src.append(L0_CLOSE);
	}

	private void emitRule(StringBuilder src, String fieldName, IRule rule, Map<String, Integer> constIndexes,
			Map<String, Integer> inputIndexes, Map<String, Integer> globalIndexes) {
		Integer obj = globalIndexes.get(fieldName);
		if (rule == null || obj == null) {
			src.append(EMPTY_LINE);
			src.append(L0).append("// no rule for global field ").append(fieldName);
			src.append(EMPTY_LINE);
			return;
		}

		int idx = obj;
		src.append(L0).append("private  long _g").append(idx).append("(){");
		src.append(L1).append("if (this._isReady(").append(idx).append(")){");
		src.append(L1).append("return val[").append(idx).append("];");
		src.append(L1_CLOSE);

		src.append(L1).append("long _r = 0;");
		/*
		 * rule has to put code to set value to _r;
		 */
		rule.toJava(src, constIndexes, inputIndexes, globalIndexes);
		src.append(L1).append("val[").append(idx).append("] = _r;");
		src.append(L1).append("this._calced(").append(idx).append(");");
		src.append(L1).append("return _r;");
		src.append(L0_CLOSE);
	}

	/**
	 * @param tst
	 * @param testValues
	 */
	private void generateJunit(StringBuilder tst, long[][] testValues) {
		// TODO Auto-generated method stub

	}

	/**
	 *
	 * @param json
	 * @return errors detected while loading the josn into this rule set.
	 */
	public List<String> fromJson(JSONObject json) {
		List<String> errors = new ArrayList<>();
		Object val = null;
		boolean warned = false;
		for (String key : json.keySet()) {
			val = json.opt(key);
			if (key.equals("name")) {
				if (val instanceof String) {
					this.name = (String) val;
				} else {
					errors.add("name attribute must be a string");
				}
			} else if (key.equals("inputParameters")) {
				this.inputParameters = parseTextArray(val, "inputParameters", errors);
			} else if (key.equals("outputParameters")) {
				this.outputParameters = parseTextArray(val, "outputParameters", errors);
			} else if (key.equals("fieldsToInitialize")) {
				this.fieldsToInitialize = parseTextArray(val, "fieldsToInitialize", errors);
			} else if (key.equals("fields")) {
				this.parseFields(val, errors);
			} else {
				errors.add(key + " is an invalid attribute");
				if (!warned) {
					warned = true;
					errors.add(key
							+ "name, inputParameters, outputParameters, fieldsToInitialize and fields are the only valid attributes at the root");
				}
			}
		}
		if (errors.size() == 0) {
			return null;
		}
		return errors;
	}

	private void parseFields(Object val, List<String> errors) {
		if (val == null || val instanceof JSONObject == false) {
			errors.add("Value for fields attribute shoudl be an object with field names as its attributes.");
			return;
		}
		JSONObject json = (JSONObject) val;
		Iterator<String> keys = json.keys();

		while (keys.hasNext()) {
			String key = keys.next();
			Object obj = json.opt(key);
			if (obj == null) {
				errors.add("Field " + key + " has null as its value.");
				this.constants.put(key, 0L);
				continue;
			}

			if (obj instanceof JSONObject) {
				AbstractRule rule = parseRule(key, (JSONObject) obj, errors, false);
				if (rule != null) {
					this.rules.put(key, rule);
				}
				continue;
			}
			Expression expr = parseExpr(obj, errors);
			if (expr == null) {
				this.constants.put(key, 0L);
				continue;
			}
			/*
			 * it may be a constant, let us simplify
			 */
			Long l = tryAsLong(expr);
			if (l == null) {
				this.rules.put(key, new ExprRule(key, null, expr));
			} else {
				this.constants.put(key, l);
			}
		}
	}

	private static Long tryAsLong(Expression expr) {
		Operand[] operands = expr.getOperands();
		if (operands.length > 1) {
			return null;
		}
		Operand op = operands[0];
		if (op.getOperandType() != Operand.CONSTANT) {
			return null;
		}
		Value value = op.getOperandValue();
		if (value.getValueType() != ValueType.INTEGER) {
			return null;
		}
		return ((IntegerValue) value).getLong();
	}

	private static AbstractRule parseRule(String key, JSONObject json, List<String> errors, boolean isLocal) {
		String ruleName = key;
		AbstractRule[] locals = null;
		Expression expr = null;
		Expression[] values = null;
		int[] ranges = null;
		RuleStep[] steps = null;
		for (String att : json.keySet()) {
			Object value = json.opt(att);

			if ("name".equals(att)) {
				if (value == null || value instanceof String == false) {
					errors.add("rule name shoudl be a string");
				} else {
					if (isLocal) {
						ruleName = value.toString();
					} else {
						errors.add("name should not be specified for a rule, except if it is a local one.");
					}
				}
			}
			if ("localRules".equals(att)) {
				if (isLocal) {
					errors.add("Rule " + key + " is a local rule, and hence it can not have local rules again.");
				} else {
					locals = parseLocalRules(att, value, errors);
				}
			} else if ("expression".equals(att)) {
				expr = parseExpr(value, errors);
			} else if ("values".equals(att)) {
				values = parseExprArray(value, errors);
			} else if ("rangeUpperLimits".equals(att)) {
				ranges = parseIntArray(value, errors);
			} else if ("steps".equals(att)) {
				steps = parseSteps(value, errors);
			} else {
				errors.add(att + " is not a valid attribute for a rule. Ignored");
			}
		}
		if (ruleName == null) {
			errors.add("Local rule must have a name.");
			ruleName = "_unknown";
		}
		AbstractRule rule = null;
		if (steps != null) {
			rule = new RuleWithSteps(ruleName, locals, steps);
		} else if (ranges != null) {
			rule = new RangeRule(ruleName, locals, ranges, values);
		} else if (values != null) {
			rule = new EnumRule(ruleName, locals, values);
		} else if (expr != null) {
			rule = new ExprRule(ruleName, locals, expr);
		} else {
			errors.add("Field " + key
					+ " has an invalid object as rule. one of the attributes should be steps, values, ranges, or exprssion");
			return null;
		}
		return rule;
	}

	private static RuleStep[] parseSteps(Object value, List<String> errors) {
		if (value == null) {
			errors.add("You must specify one or more steps for attribute 'steps'");
			return null;
		}
		if (value instanceof JSONArray == false) {
			errors.add("You must specify one or more steps for attribute 'steps'. found " + value + " instead");
			return null;
		}
		List<RuleStep> steps = new ArrayList<>();
		for (Object obj : (JSONArray) value) {
			if (obj instanceof JSONObject == false) {
				errors.add("each element of steps should be an object");
				continue;
			}
			JSONObject step = (JSONObject) obj;
			Expression condition = null;
			Expression expr = null;
			for (String key : step.keySet()) {
				Object val = step.opt(key);
				if (key.equals("condition")) {
					condition = parseExpr(val, errors);
				} else if (key.equals("expression")) {
					expr = parseExpr(val, errors);
				} else {
					errors.add(key + " is not a valid attribute of a rule step");
				}
			}
			if (expr == null) {
				errors.add("Each step must have an expression");
			} else {
				steps.add(new RuleStep(condition, expr));
			}
		}
		if (steps.size() == 0) {
			return null;
		}
		return steps.toArray(new RuleStep[0]);
	}

	private static AbstractRule[] parseLocalRules(String key, Object value, List<String> errors) {
		if (value == null || value instanceof JSONArray == false) {
			errors.add("Expeted an array of rules, but found " + value);
			return null;
		}
		List<AbstractRule> list = new ArrayList<>();
		for (Object obj : (JSONArray) value) {
			if (obj == null || obj instanceof JSONObject == false) {
				errors.add("found " + obj + " when we expected a local rule");
			} else {
				AbstractRule rule = parseRule(null, (JSONObject) obj, errors, true);
				if (rule != null) {
					list.add(rule);
				}
			}
		}
		return list.toArray(new AbstractRule[0]);
	}

	private static Expression[] parseExprArray(Object value, List<String> errors) {
		if (value == null || value instanceof JSONArray == false) {
			errors.add("You must specify one or more values");
			return null;
		}
		List<Expression> exprs = new ArrayList<>();
		for (Object obj : (JSONArray) value) {
			Expression expr = parseExpr(obj, errors);
			if (expr != null) {
				exprs.add(expr);
			}
		}
		int n = exprs.size();
		if (n == 0) {
			return null;
		}
		return exprs.toArray(new Expression[0]);
	}

	private static int[] parseIntArray(Object value, List<String> errors) {
		if (value == null || value instanceof JSONArray == false) {
			errors.add("You must specify one or integer values for attribute 'rangeUpperLimits'");
			return null;
		}
		List<Integer> limits = new ArrayList<>();
		for (Object obj : (JSONArray) value) {
			Integer i = parseInt(obj, errors);
			if (i != null) {
				limits.add(i);
			}
		}
		int n = limits.size();
		if (n == 0) {
			return null;
		}
		int[] arr = new int[n];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = limits.get(i);
		}
		return arr;
	}

	private static String[] parseTextArray(Object val, String attName, List<String> errors) {
		if (val == null || (val instanceof JSONArray == false)) {
			errors.add(attName + " must be an array of Strings");
			return null;
		}
		List<String> names = new ArrayList<>();
		for (Object obj : (JSONArray) val) {
			if (obj != null && obj instanceof String) {
				names.add((String) obj);
			} else {
				errors.add("found " + obj + " when we expected a string value for " + attName + ". Ignored");
			}
		}
		return names.toArray(new String[0]);
	}

	private static Expression parseExpr(Object value, List<String> errors) {
		if (value == null || value instanceof JSONObject || value instanceof JSONArray) {
			errors.add("Expeted an integer expression, but found " + value);
			return null;
		}
		try {
			return new Expression(value.toString());
		} catch (Exception e) {
			errors.add("Expeted an integer expression but found " + value);
			return null;
		}
	}

	private static Integer parseInt(Object value, List<String> errors) {
		if (value == null || value instanceof JSONObject || value instanceof JSONArray) {
			errors.add("Expeted an integeral value, but found " + value);
			return null;
		}
		try {
			return Integer.parseInt(value.toString());
		} catch (Exception e) {
			errors.add("Expeted an integeral value, but found " + value);
			return null;
		}
	}

}