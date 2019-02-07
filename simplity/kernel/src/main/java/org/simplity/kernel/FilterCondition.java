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
package org.simplity.kernel;

import org.simplity.kernel.app.AppConventions;

/** @author rg bhandi */
public enum FilterCondition {
	/** equal */
	Equal(AppConventions.Comparator.EQUAL),
	/** not equal */
	NotEqual(AppConventions.Comparator.NOT_EQUAL),
	/** greater. remember it is greater and not "more" */
	Greater(AppConventions.Comparator.GREATER),
	/** greater or equal */
	GreaterOrEqual(AppConventions.Comparator.GREATER_OR_EQUAL),
	/**
	 * we prefer to call small rather than less because we say greater and not
	 * more :-)
	 */
	Smaller(AppConventions.Comparator.LESS),
	/** we prefer to smaller to less than more :-) */
	SmallerOrEqual(AppConventions.Comparator.LESS_OR_EQUAL),
	/** like */
	Like(AppConventions.Comparator.LIKE, FilterCondition.LIKE),
	/** starts with */
	StartsWith(AppConventions.Comparator.STARTS_WITH, FilterCondition.LIKE),
	/** between */
	Between(AppConventions.Comparator.BETWEEN, FilterCondition.BETWEEN),
	/** one in the list */
	In(AppConventions.Comparator.IN_LIST, FilterCondition.IN);
	private static final String IN = " IN ";
	private static final String LIKE = " LIKE ";
	private static final String BETWEEN = " BETWEEN ";
	private String textValue;
	private String sql;

	private FilterCondition(String text) {
		this.textValue = text;
		this.sql = text;
	}

	private FilterCondition(String text, String sqlText) {
		this.textValue = text;
		this.sql = sqlText;
	}

	/**
	 * parse a text into enum
	 *
	 * @param text
	 *            text to be parsed into enum
	 * @return filter condition, or null if there is no filter for this text
	 */
	public static FilterCondition parse(String text) {
		if (text == null || text.length() == 0) {
			return Equal;
		}
		for (FilterCondition f : FilterCondition.values()) {
			if (f.textValue.equals(text)) {
				return f;
			}
		}
		return null;
	}

	/**
	 * @return text to be used in a sql for this condition. like, startWith and
	 *         between require custom logic by caller
	 */
	public String getSql() {
		return this.sql;
	}
}
