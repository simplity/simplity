/*
 * Copyright (c) 2015 EXILANT Technologies Private Limited (www.exilant.com)
 * Copyright (c) 2016 simplity.org
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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.core.testing;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.simplity.core.data.DynamicSheet;
import org.simplity.core.data.IDataSheet;
import org.simplity.core.expr.Expression;
import org.simplity.core.expr.InvalidExpressionException;
import org.simplity.core.expr.InvalidOperationException;
import org.simplity.core.value.InvalidValueException;
import org.simplity.core.value.Value;

/**
 * internal class to test expression during development. To be re-factored into
 * a Maven structure and put into test folder
 *
 * @author simplity.org
 */
public class ExpressionTest {

	/**
	 *
	 */
	@Rule
	public final ExpectedException exception = ExpectedException.none();

	/** bad example for reference/testing */
	private static final String[] badExamples = {
			null,
			"",
			" ",
			"\n\t\r  ",
			"()",
			"(())",
			".a",
			"_a",
			"a a",
			"a$a",
			"1a",
			"+1",
			".1",
			"1.1.1",
			"1,1",
			"'''",
			"\"\"\"",
			"/a/",
			"//",
			"/12-/",
			"/2015-13-13/",
			"/2015-12-32",
			"/2015-02-29/",
			"/2015.02.01/",
			"--a",
			"!?a",
			"?1",
			"-''",
			"~12",
			"!\"a\"",
			"?foo()",
			"a ++ b",
			" a + b - ",
			" + a - b",
			"(a}",
			"foo{{}"
	};

	/**
	 * good examples for reference/testing. "4 & 4" is good for parsing, but
	 * will fail during execution
	 */
	private static final String[] goodExamples = {
			"a",
			"a.a",
			"a1",
			"a__a",
			"1",
			"1.005",
			"1.",
			"''",
			"'\"'",
			"\"''' -_!@#$%^&*()_+{}[]|\\:;'<,>.?/\"",
			"/1960-05-06/",
			"/2016-02-29/",
			"!a",
			"?a",
			"~b",
			"-a",
			"-1.234",
			"a+b",
			"a - b",
			"a--b",
			"a +-12 * foo(1,a, ( 1 + 2))",
			"(a)",
			"a <= 12  "
	};
	/**
	 * expressions that should parse OK but will generate exception during
	 * execution
	 */
	private static final String[] runTimeErrors = { "4 < 'abd'" };

	/** expressions that should evaluate to a value as in testResults */
	private static final String[] integralExpressions = {
			"1 + 2",
			" 1 + -2",
			"-1 + -1",
			"123456789 - 12345678",
			" -2 * -3",
			"2 * 4",
			"2 * -5",
			"6 / 2",
			"7/2",
			"2/5",
			"7%5",
			"8%8",
			" 2 + 3 * 6 - 4 + 4 / 3",
			"(2 + 4) * ( 5 -3) "
	};

	/** expected result for testExpressions */
	private static final long[] integralResults = {
			3, -1, -2, 111111111, 6, 8, -10, 3, 3, 0, 2, 0, 17, 12, -580, 12
	};

	/**
	 *
	 * @throws InvalidExpressionException
	 */
	@SuppressWarnings("unused")
	@Test
	public final void testInvalids() throws InvalidExpressionException {
		for (String text : ExpressionTest.badExamples) {
			this.exception.expect(InvalidExpressionException.class);
			new Expression(text);
		}
	}

	/**
	 *
	 * @throws InvalidExpressionException
	 */
	@Test
	@SuppressWarnings("unused")
	public final void testValids() throws InvalidExpressionException {
		for (String text : ExpressionTest.goodExamples) {
			new Expression(text);
		}
	}

	/**
	 *
	 * @throws InvalidExpressionException
	 * @throws InvalidOperationException
	 */
	@Test
	public final void testRuntimeErrors()
			throws InvalidExpressionException, InvalidOperationException {
		IDataSheet data = new DynamicSheet();
		for (String text : ExpressionTest.runTimeErrors) {
			Expression expr = new Expression(text);
			// Expecting exception
			this.exception.expect(InvalidOperationException.class);
			expr.evaluate(data);
		}
	}

	/**
	 *
	 * @throws InvalidExpressionException
	 * @throws InvalidOperationException
	 * @throws InvalidValueException
	 */
	@Test
	public final void testResults()
			throws InvalidExpressionException, InvalidOperationException, InvalidValueException {
		IDataSheet data = new DynamicSheet();
		for (int i = 0; i < ExpressionTest.integralExpressions.length; i++) {
			String text = ExpressionTest.integralExpressions[i];
			long result = ExpressionTest.integralResults[i];
			Expression expr = new Expression(text);
			Value value = expr.evaluate(data);
			assertEquals(result, value.toInteger());
		}
	}
}
