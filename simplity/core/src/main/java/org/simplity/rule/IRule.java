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
 * @author simplity.org
 *
 */
public interface IRule {
	/**
	 * validate this rule for a set of global fields and constants
	 *
	 * @param indexedName
	 *            name by which this is eternaly known.
	 *
	 * @param globalNames
	 *            all global fields that can be used in the rule
	 * @param functionNames
	 *            all functions that can be used
	 * @param errors
	 *            list to which any error to be added to
	 */
	public void validate(String indexedName, Set<String> globalNames, Set<String> functionNames, List<String> errors);

	/**
	 * emit required source
	 *
	 * @param src
	 *            to which source is to be appended to
	 * @param constIndexes
	 *            constants defined for the rule-set
	 * @param inputIndexes
	 *            input field names defined for the rule set
	 * @param globalIndexes
	 *            global fields defined for the rule set
	 */
	public void toJava(StringBuilder src, Map<String, Integer> constIndexes,
			Map<String, Integer> inputIndexes, Map<String, Integer> globalIndexes);
}
