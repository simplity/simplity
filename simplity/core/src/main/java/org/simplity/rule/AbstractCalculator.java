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
import java.util.Set;

/**
 * @author simplity.org
 *
 */
public abstract class AbstractCalculator {
	/*
	 * functions used in rules
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
	protected static long maxOf(long... args) {
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
	 * @param val1
	 *            value to be returned if condition is true
	 * @param val2
	 *            value to be returned if condition is false
	 * @return if condition is true, then value1 else value2 value from the
	 *         arguments.
	 */
	public static long ifThenElse(boolean bool, long val1, long val2) {
		return bool ? val1 : val2;
	}

	/**
	 * calculate a set of output values for the list of input values. number and
	 * sequence of input and output parameters is pre-agreed and is not covered
	 * by this interface
	 *
	 * @param inputValues
	 *            values for the pre-agreed list of input fields
	 * @return calculated values for pre-agreed output parameters
	 * @throws RuleException
	 */
	public abstract long[] calculate(long[] inputValues) throws RuleException;

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
	public abstract String[] getOututParameters();

	/**
	 *
	 * @return all the functions that this calculator supports
	 */
	public static Set<String> getPredefinedFunctions() {
		return FUNCTIONS;
	}
}
