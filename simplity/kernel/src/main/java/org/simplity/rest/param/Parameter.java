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

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;
import org.simplity.json.JSONWriter;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.Messages;
import org.simplity.rest.Tags;

/**
 * Base class for different types of parameters.
 *
 * @author simplity.org
 *
 */
public abstract class Parameter {

	/**
	 * parse swagger document node into an appropriate Parameter
	 *
	 * @param param
	 * @return parameter as per the specification
	 */
	public static Parameter parse(JSONObject param) {
		return getParameterType(param).parseParameter(param);
	}

	/**
	 * parse swagger document node into an appropriate Parameter
	 *
	 * @param name
	 *            as used in swagger
	 * @param fieldName
	 *            if service uses a different name
	 * @param param
	 * @return parameter as per the specification
	 */
	public static Parameter parse(String name, String fieldName, JSONObject param) {
		return getParameterType(param).parseParameter(name, fieldName, param);
	}

	private static ParameterType getParameterType(JSONObject obj) {
		String text = obj.optString(Tags.TYPE_ATTR, null);
		if (text == null) {
			/*
			 * is this all-of
			 */
			JSONArray allOf = obj.optJSONArray(Tags.ALL_OF_ATTR);
			if (allOf == null) {
				throw new ApplicationError("Missing type/schema attribute for a data item");
			}
			throw new ApplicationError("We are yet to implement allOf feature. Please contact support team");
		}
		ParameterType tp = ParameterType.valueOf(text.toUpperCase());
		if (tp == null) {
			throw new ApplicationError("Prameter type " + text + " is not implemented.");
		}
		return tp;
	}

	/**
	 * name of this parameter as used by swagger for input/output
	 */
	protected String name;

	/**
	 * name as used by actual service. input parameter extracts data into this
	 * name while output parameter extracts data using this name. Defaults to
	 * name if this is not specified in the spec
	 */
	protected String fieldName;
	/**
	 * is this mandatory
	 */
	protected boolean isRequired;

	/**
	 * default value to be used if this is optional, and client has not sent a
	 * value
	 */
	protected Object defaultValue;

	/**
	 * list of valid values
	 */
	protected Object[] validValues;

	/**
	 * set attribute at this level. Must be called from sub-classes before they
	 * set their attributes
	 *
	 * @param paramSpec
	 */
	protected Parameter(JSONObject paramSpec) {
		/*
		 * for array item, name could be null as we recurse into it.
		 */
		this.name = paramSpec.optString(Tags.PARAM_NAME_ATTR, "array-item");
		this.fieldName = paramSpec.optString(Tags.FIELD_NAME_ATTR, this.name);
		this.defaultValue = paramSpec.opt(Tags.DEFAULT_ATTR);
		this.isRequired = paramSpec.optBoolean(Tags.REQUIRED_ATTR, false);

		JSONArray vals = paramSpec.optJSONArray(Tags.ENUM_ATT);
		if (vals != null) {
			this.validValues = new Object[vals.length()];
			int i = 0;
			for (Object obj : vals) {
				this.validValues[i] = obj;
				i++;
			}
		}
	}

	/**
	 * spec when name is not part of it, For example inside a schema
	 *
	 * @param paramSpec
	 * @param name
	 */
	protected Parameter(String name, String fn, JSONObject paramSpec) {
		this(paramSpec);
		/*
		 * passed name real name, but passed field name is used only if it is
		 * non-null;
		 */
		this.name = name;
		if (fn != null) {
			this.fieldName = fn;
		}
	}

	/**
	 * @return name of this parameter
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return name of the field. For an input parameter, this is the name with
	 *         which data is to be written service data after extraction from
	 *         input. For an output parameter, this is the to be used to extract
	 *         data from service output.
	 */
	public String getFieldName() {
		return this.fieldName;
	}

	/**
	 * @param value
	 *            could be null, in which case returned value is null even if
	 *            validation succeeds.
	 * @param messages
	 *            error message added to this list in case of validation
	 *            failure. checking for
	 * @return validated, and possibly formatted, object. null if validation
	 *         fails, or if input was null.
	 */
	public final Object validate(Object value, List<FormattedMessage> messages) {
		if (value == null) {
			if (this.isRequired) {
				messages.add(new FormattedMessage(Messages.VALUE_REQUIRED, null, this.name, null, 0, ""));
				return null;
			}
			return this.defaultValue;
		}
		if (this.validValues != null) {
			String txt = value.toString();
			for (Object obj : this.validValues) {
				if (obj.toString().equals(txt)) {
					return value;
				}
			}
			return this.invalidValue(messages);
		}

		return this.doValidate(value, messages);
	}

	/**
	 * @param value
	 *            non-null value
	 * @param messages
	 * @return
	 */
	protected abstract Object doValidate(Object value, List<FormattedMessage> messages);

	/**
	 * sub-classes use this method to return when they detect an invalid value
	 *
	 * @param messages
	 *
	 * @return always null
	 */
	protected Object invalidValue(List<FormattedMessage> messages) {
		messages.add(new FormattedMessage(Messages.INVALID_VALUE, null, this.name, null, 0, ""));
		return null;
	}

	/**
	 * @param resp
	 * @param data
	 */
	public void setHeader(HttpServletResponse resp, Object data) {
		/*
		 * this method works for all value-types. Array and Object need to
		 * over-ride this
		 */
		if (data != null) {
			resp.setHeader(this.name, data.toString());
		}
	}

	/**
	 * write this parameter into a json writer. For example "fieldName":"value",
	 * or just "value" if justTheValue is true
	 *
	 * @param writer
	 * @param data
	 * @param asAttribute
	 *            if this is true, then we write key,value pair, else we write
	 *            just the value
	 *            and not as an attribute.
	 */
	public void toWriter(JSONWriter writer, Object data, boolean asAttribute) {
		/*
		 * this method works for all value-types. Array and Object need to
		 * over-ride this
		 */

		/*
		 * do we write even if it is null? we are saying yes.
		 * And, we are assuming that server is always right!! Why waste cpu
		 * cycles validating data
		 */
		if (asAttribute) {
			writer.key(this.name);
		}
		writer.value(data);
	}

	/**
	 * make this parameter mandatory
	 */
	public void enableRequired() {
		this.isRequired = true;

	}
}
