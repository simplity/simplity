/*
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

package org.simplity.core.test;

import org.simplity.core.comp.ComponentType;
import org.simplity.core.comp.IComponent;
import org.simplity.core.comp.IValidationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Sequence of test cases that are run in that order */
public class TestRun implements IComponent {
	private static final Logger logger = LoggerFactory.getLogger(TestRun.class);

	String testName;
	String moduleName;

	TestCase[] testCases;

	/**
	 * run all test cases and report number of failure
	 *
	 * @param ctx
	 * @return number of failures
	 */
	public int run(TestContext ctx) {
		if (this.testCases == null) {

			logger.info("No test cases to run.. reporting success by default");

			return 0;
		}
		int nbrFailure = 0;

		for (TestCase tc : this.testCases) {
			String msg = tc.run(ctx);
			if (msg != null) {
				nbrFailure++;
			}
		}
		return nbrFailure;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.comp.Component#getSimpleName()
	 */
	@Override
	public String getSimpleName() {
		return this.testName;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.comp.Component#getQualifiedName()
	 */
	@Override
	public String getQualifiedName() {
		if (this.moduleName == null) {
			return this.testName;
		}
		return this.moduleName + '.' + this.testName;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.comp.Component#getReady()
	 */
	@Override
	public void getReady() {
		// This component is not saved and re-used in memory. Hence no
		// preparation on load.
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.comp.Component#getComponentType()
	 */
	@Override
	public ComponentType getComponentType() {
		return ComponentType.TEST_RUN;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.kernel.comp.Component#validate(org.simplity.kernel.comp.
	 * ValidationContext)
	 */
	@Override
	public void validate(IValidationContext vtx) {
		if (this.testCases != null) {
			for (TestCase testCase : this.testCases) {
				testCase.validate(vtx);
			}
		}
	}
}
