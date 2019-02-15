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

package org.simplity.core.dm;

import org.simplity.core.util.TextUtil;

/** @author simplity.org */
public enum DbToJavaNameConversion {
	/** no change */
	NONE {
		@Override
		public String toJavaName(String dbEntityname) {
			return dbEntityname;
		}

		@Override
		public String toDbEntityName(String javaName) {
			return javaName;
		}
	},

	/** convert to lower case CUST_NAME becomes cust_name */
	LOWER_CASE {
		@Override
		public String toJavaName(String dbEntityname) {
			if (dbEntityname == null) {
				return null;
			}
			return dbEntityname.toLowerCase();
		}

		@Override
		public String toDbEntityName(String javaName) {
			if (javaName == null) {
				return null;
			}
			return javaName.toUpperCase();
		}
	},
	/** change'_' convention to camelCase CUST_NAME becomes custName */
	CAMEL_CASE {
		@Override
		public String toJavaName(String dbEntityname) {
			return TextUtil.undoUnderscore(dbEntityname);
		}

		@Override
		public String toDbEntityName(String javaName) {
			return TextUtil.toUnderscore(javaName);
		}
	};
	/**
	 * @param dbEntityName
	 * @return java name for this column name
	 */
	public abstract String toJavaName(String dbEntityName);

	/**
	 * @param javaName
	 * @return name to be used in db as per standard
	 */
	public abstract String toDbEntityName(String javaName);
}
