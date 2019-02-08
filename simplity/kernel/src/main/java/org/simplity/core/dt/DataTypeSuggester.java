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

package org.simplity.core.dt;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.simplity.core.app.Application;
import org.simplity.core.comp.ComponentType;
import org.simplity.core.util.RdbUtil;
import org.simplity.core.value.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * utility class that suggests a suitable data type based on known attributes,
 * and list of existing data types. We keep improving this based on usage
 * patterns
 *
 * @author simplity.org
 */
public class DataTypeSuggester {
	private static final Logger logger = LoggerFactory.getLogger(DataTypeSuggester.class);

	/*
	 * lengths of available text data types in increasing order
	 */
	private int[] lengths;
	/*
	 * data type names corresponding to lengths[]
	 */
	private String[] names;

	/** */
	public DataTypeSuggester() {
		Map<Integer, String> types = new HashMap<Integer, String>();
		for (Object obj : Application.getActiveInstance().getPreloadedComps(ComponentType.DT)) {
			if (obj instanceof TextDataType == false) {
				continue;
			}
			TextDataType dt = (TextDataType) obj;
			if (dt.getRegex() != null) {
				continue;
			}
			types.put(new Integer(dt.getMaxLength()), dt.getName());
		}
		int n = types.size();
		if (n == 0) {

			logger.info(
					"There are no text data types to suggest from. we will ALWAYS suggest default one.");
		}

		this.lengths = new int[n];
		this.names = new String[n];
		int i = 0;
		for (Integer len : types.keySet()) {
			this.lengths[i++] = len.intValue();
		}
		Arrays.sort(this.lengths);
		i = 0;
		for (int len : this.lengths) {
			this.names[i++] = types.get(new Integer(len));
		}
	}

	/**
	 * @param sqlTypeInt
	 * @param sqlTypeText
	 * @param size
	 * @param nbrDecimals
	 * @return an existing data type for known sql types, or sqlTypeName for
	 *         types that we do not manage
	 */
	public String suggest(int sqlTypeInt, String sqlTypeText, int size, int nbrDecimals) {

		ValueType vt = RdbUtil.sqlTypeToValueType(sqlTypeInt);
		if (vt != ValueType.TEXT) {
			return vt.getDefaultDataType();
		}
		if (this.lengths != null) {
			int i = 0;
			for (int max : this.lengths) {
				if (size <= max) {
					return this.names[i];
				}
				i++;
			}
		}
		return ValueType.TEXT.getDefaultDataType() + size;
	}

}
