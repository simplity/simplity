/*
 * Copyright (c) 2017 simplity.org
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

package org.simplity.rest.param;

import org.simplity.json.JSONObject;
import org.simplity.kernel.ApplicationError;
import org.simplity.rest.Tags;

/**
 * represents a parameter type as in a swagger document
 *
 * @author simplity.org
 *
 */
public enum ParameterType {
	/**
	 * array
	 */
	ARRAY {
		@Override
		public Parameter parseParameter(JSONObject parameterSpec) {
			return new ArrayParameter(parameterSpec);
		}

		@Override
		public Parameter parseParameter(String name, String fieldName, JSONObject parameterSpec) {
			return new ArrayParameter(name, fieldName, parameterSpec);
		}
	},
	/**
	 * boolean
	 */
	BOOLEAN {
		@Override
		public Parameter parseParameter(JSONObject parameterSpec) {
			return new BooleanParameter(parameterSpec);
		}

		@Override
		public Parameter parseParameter(String name, String fieldName, JSONObject parameterSpec) {
			return new BooleanParameter(name, fieldName, parameterSpec);
		}
	},
	/**
	 * file
	 */
	FILE {
		@Override
		public Parameter parseParameter(JSONObject parameterSpec) {
			throw new ApplicationError("Paramater type 'file' is not yet implemented");
		}

		@Override
		public Parameter parseParameter(String name, String fieldName, JSONObject parameterSpec) {
			throw new ApplicationError("Paramater type 'file' is not yet implemented");
		}
	},
	/**
	 * integer
	 */
	INTEGER {
		@Override
		public Parameter parseParameter(JSONObject parameterSpec) {
			return new IntParameter(parameterSpec);
		}

		@Override
		public Parameter parseParameter(String name, String fieldName, JSONObject parameterSpec) {
			return new IntParameter(name, fieldName, parameterSpec);
		}
	},
	/**
	 * possibly with a decimal
	 */
	NUMBER {
		@Override
		public Parameter parseParameter(JSONObject parameterSpec) {
			return new NumberParameter(parameterSpec);
		}

		@Override
		public Parameter parseParameter(String name, String fieldName, JSONObject parameterSpec) {
			return new NumberParameter(name, fieldName, parameterSpec);
		}
	},
	/**
	 * object
	 */
	OBJECT {
		@Override
		public Parameter parseParameter(JSONObject parameterSpec) {
			return new ObjectParameter(parameterSpec);
		}

		@Override
		public Parameter parseParameter(String name, String fieldName, JSONObject parameterSpec) {
			return new ObjectParameter(name, fieldName, parameterSpec);
		}
	},
	/**
	 * text. could be date etc based on format
	 */
	STRING {
		@Override
		public Parameter parseParameter(JSONObject parameterSpec) {
			if (this.isDate(parameterSpec)) {
				return new DateParameter(parameterSpec);
			}
			return new TextParameter(parameterSpec);
		}

		@Override
		public Parameter parseParameter(String name, String fieldName, JSONObject parameterSpec) {
			if (this.isDate(parameterSpec)) {
				return new DateParameter(name, fieldName, parameterSpec);
			}
			return new TextParameter(name, fieldName, parameterSpec);
		}

		private boolean isDate(JSONObject param) {
			String text = param.optString(Tags.FORMAT_ATT, null);
			if (text != null && text.startsWith("date")) {
				return true;
			}
			return false;
		}
	};

	/**
	 * create a Parameter instance based on the spec
	 *
	 * @param parameterSpec
	 * @return Parameter instance
	 */
	public abstract Parameter parseParameter(JSONObject parameterSpec);

	/**
	 * create a Parameter instance based on the spec
	 *
	 * @param name
	 *            as used by swagger
	 * @param fieldName
	 *            as used by service, if it is different from name. null implies
	 *            that it is same as name
	 * @param parameterSpec
	 * @return Parameter instance
	 */
	public abstract Parameter parseParameter(String name, String fieldName, JSONObject parameterSpec);
}
