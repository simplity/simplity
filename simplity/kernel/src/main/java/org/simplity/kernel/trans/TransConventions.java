/*
 * Copyright (c) 2018 simplity.org
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

package org.simplity.kernel.trans;

/**
 * commonly used constants/parameters in the trans related classes
 *
 * @author simplity.org
 *
 */
public class TransConventions {
	private TransConventions() {
		// prohibited
	}

	/**
	 * field name with which result of a action is available in service context
	 */
	public static final String NAME_SUFFIX_FOR_RESULT = "Result";

	/**
	 * returned value from a action has some special meaning.
	 */
	public static class JumpTo {
		/**
		 * is this a signal or tag name?
		 *
		 * @param whatNext
		 * @return true if whatNext is one of the special texts for signal.
		 *         false if it is a action name
		 */
		public static final boolean isSignal(String whatNext) {
			if (whatNext == null) {
				return false;
			}
			return whatNext.equals(STOP) || whatNext.equals(BREAK_LOOP) || whatNext.equals(NEXT_LOOP);
		}

		/**
		 * stop taking actions, and get out
		 */
		public static final String STOP = "_s";
		/**
		 * skip next actions in this loop and loop next
		 */
		public static final String NEXT_LOOP = "_n";
		/**
		 * stop taking actions, and get out
		 */
		public static final String BREAK_LOOP = "_b";
	}
}
