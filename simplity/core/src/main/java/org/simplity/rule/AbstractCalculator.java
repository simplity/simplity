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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class that calculates set of output fields based on the input fields and the
 * rules to calculate the output fields
 *
 * @author simplity.org
 *
 */
public abstract class AbstractCalculator {
	/*
	 * functions used in rules. If this class is extended, the extended class
	 * should statically add its methods into the set
	 */
	private static final Set<String> FUNCTIONS = new HashSet<>();
	static {
		FUNCTIONS.add("maxOf");
		FUNCTIONS.add("minOf");
		FUNCTIONS.add("sumOf");
		FUNCTIONS.add("ifThenElse");
	}

	/**
	 * find the max value from the list
	 *
	 * @param args
	 * @return maximum value from the arguments.
	 */
	public static long maxOf(long... args) {
		long max = Long.MIN_VALUE;
		for (long val : args) {
			if (val > max) {
				max = val;
			}
		}
		return max;
	}

	/**
	 * find the max value from the list
	 *
	 * @param args
	 * @return minimum value from the arguments.
	 */
	public static long minOf(long... args) {
		long min = Long.MAX_VALUE;
		for (long val : args) {
			if (val < min) {
				min = val;
			}
		}
		return min;
	}

	/**
	 * calculate the sum of all values
	 *
	 * @param args
	 * @return sum of all arguments.
	 */
	public static long sumOf(long... args) {
		long r = 0;
		for (long val : args) {
			r += val;
		}
		return r;
	}

	/**
	 * find the max value from the list
	 *
	 * @param bool
	 *            condition to be tested.
	 * @param trueValue
	 *            value to be returned if condition is true
	 * @param falseValue
	 *            value to be returned if condition is false
	 * @return if condition is true, then value1 else value2 value from the
	 *         arguments.
	 */
	public static long ifThenElse(boolean bool, long trueValue, long falseValue) {
		return bool ? trueValue : falseValue;
	}

	/**
	 *
	 * @return all the functions that this calculator supports
	 */
	public static Set<String> getPredefinedFunctions() {
		return FUNCTIONS;
	}

	/**
	 * this is a clone of static constants. Cloned to facilitate unit-testing
	 * with altered values
	 */
	protected long[] cVal;
	/**
	 * all input values. these have to be input
	 */
	protected long[] iVal;
	/**
	 * global values. These are calculated on a need basis using the associated
	 * rule.
	 */
	protected long[] gVal;
	/**
	 * associated with gVal. Tracks whether the value is needs to be calculated
	 * or it is already calculated.
	 */
	protected boolean[] isReady;
	/**
	 * for detecting infinite-loops
	 */
	protected boolean[] isLooping;

	protected abstract long[] doCalculate(long[] inputValues) throws InvalidRuleException;

	/**
	 *
	 * @return name of the input parameters that this class is designed for.
	 *         calculate() method expects values for these parameters in this
	 *         order as input
	 */
	public abstract String[] getInputParameters();

	/**
	 *
	 * @return name of the output parameters that this class is designed for.
	 *         calculate() method returns values for these parameters in this
	 *         order
	 */
	public abstract String[] getOutputParameters();

	/**
	 *
	 * @return all the global parameters. Each global parameter has a rule using
	 *         its value is calculated on a need basis. However, a global
	 *         parameter can be assigned a value for unit-testing purpose;
	 */
	public abstract String[] getGlobalParameters();

	/**
	 *
	 * @return all the constants defined by this rule set. COnstants have fixed
	 *         value. But they can be set for unit-testing purposes
	 */
	public abstract String[] getConstants();

	protected abstract long[] getConstantValues();

	protected abstract void init(long[] constValues, long[] inputValues);

	/**
	 * calculate a set of output values for the list of input values. number and
	 * sequence of input and output parameters is pre-agreed and is not covered
	 * by this interface
	 *
	 * @param inputValues
	 *            values for the pre-agreed list of input fields
	 * @return calculated values for pre-agreed output parameters
	 * @throws InvalidRuleException
	 */
	public long[] calculate(long[] inputValues) throws InvalidRuleException {
		if (inputValues == null || inputValues.length == 0) {
			throw new InvalidRuleException("Input values are required.");
		}
		if (inputValues.length != this.getInputParameters().length) {
			throw new InvalidRuleException("expecting " + this.getInputParameters().length
					+ " input values but received " + inputValues.length + " values instead.");
		}
		return this.doCalculate(inputValues);
	}

	/**
	 * calculate value of a field as a test case with supplied input values.
	 * This is to be used for unit-testing individual rules, and not at
	 * production.
	 *
	 * @param fieldName
	 *            name of the rule/field
	 * @param inputValues
	 *            all the fields that the rule depends on. Note that this method
	 *            does not verify whether all the inputs are indeed supplied.
	 *            Missing values are assumed to be zero
	 * @return calculated value
	 * @throws InvalidRuleException
	 */
	public long testCalculate(String fieldName, Map<String, Long> inputValues) throws InvalidRuleException {
		String[] globals = this.getGlobalParameters();
		String[] ins = this.getInputParameters();
		String[] consts = this.getConstants();

		/*
		 * if we don't clone, we may end-up changing the static array of
		 * constant values
		 */
		long[] c = this.getConstantValues();
		this.init(Arrays.copyOf(c, c.length), new long[ins.length]);

		/*
		 * set input value to appropriate array element
		 */
		for (Map.Entry<String, Long> entry : inputValues.entrySet()) {
			String fName = entry.getKey();
			long fValue = entry.getValue();
			if (this.setValue(fName, fValue, ins, this.iVal)) {
				continue;
			}
			if (this.setValue(fName, fValue, globals, this.gVal)) {
				continue;
			}
			if (this.setValue(fName, fValue, consts, this.cVal)) {
				continue;
			}
			throw new InvalidRuleException(fName + " is not a valid input/global/constant field");
		}
		/*
		 * get the index for output field
		 */
		int idx = -1;
		for (int i = 0; i < globals.length; i++) {
			if (fieldName.equals(globals[i])) {
				idx = i;
				break;
			}
		}
		if (idx == -1) {
			throw new InvalidRuleException(
					fieldName + " is not associated with any rule. It cannot be calculated");
		}
		/*
		 * execute the function for the output field to get the output value
		 */
		try {
			Method fn = this.getClass().getDeclaredMethod("f" + idx);
			fn.setAccessible(true);
			Object obj = fn.invoke(this);
			return (Long) obj;
		} catch (Exception e) {
			e.printStackTrace();
			throw new InvalidRuleException(e.getMessage());
		}
	}

	private boolean setValue(String fieldName, long value, String[] names, long[] values) {
		for (int i = 0; i < names.length; i++) {
			if (names[i].equals(fieldName)) {
				values[i] = value;
				return true;
			}
		}
		return false;
	}
}
