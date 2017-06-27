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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.simplity.json.JSONObject;
import org.simplity.json.JSONWriter;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.Tracer;
import org.simplity.rest.Tags;

/**
 * @author simplity.org
 *
 */
public class ObjectParameter extends Parameter {
	private Parameter[] items;
	private boolean uniqueItem;
	private int minItems;
	private int maxItems;

	/**
	 * @param name
	 * @param fieldName
	 * @param parameterSpec
	 */
	public ObjectParameter(String name, String fieldName, JSONObject parameterSpec) {
		this(parameterSpec);
		this.name = name;
		if(fieldName != null){
			this.fieldName = fieldName;
		}
	}

	/**
	 * construct based on api spec
	 *
	 * @param paramSpec
	 */
	protected ObjectParameter(JSONObject paramSpec) {
		super(paramSpec);

		/*
		 * just a safety. valid values is not valid for this type
		 */
		this.validValues = null;
		String text = paramSpec.optString(Tags.REQUIRED_ATTR, null);
		Set<String> requiredParams = null;
		if(text != null && text.isEmpty() == false){
			requiredParams = new HashSet<String>();
			for(String part : text.split(",")){
				requiredParams.add(part.trim());
			}
		}

		JSONObject props = paramSpec.optJSONObject(Tags.PROPERTIES_ATTR);
		if (props == null) {
			throw new ApplicationError("Attributes are missing for object-parameter " + this.name + " with josn as " + paramSpec.toString());
		}
		int nbrItems = props.length();
		this.items = new Parameter[nbrItems];
		int i = 0;
		for (String key : props.keySet()) {
			Parameter p  = Parameter.parse(key, null, props.getJSONObject(key));
			if(requiredParams != null && requiredParams.contains(key)){
				p.enableRequired();
			}
			this.items[i] = p;
			i++;
		}
		this.minItems = paramSpec.optInt(Tags.MIN_ITEMS_ATT, 0);
		this.maxItems = paramSpec.optInt(Tags.MAX_ITEMS_ATT, 0);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.rest.param.Parameter#doValidate(java.lang.Object,
	 * java.util.List)
	 */
	@Override
	protected Object doValidate(Object value, List<FormattedMessage> messages) {
		if (value instanceof JSONObject == false) {
			return this.invalidValue(messages);
		}
		JSONObject data = (JSONObject) value;
		JSONObject result = new JSONObject();
		for (Parameter item : this.items) {
			Object val = item.validate(data.opt(item.getName()), messages);

			if (val != null) {
				result.put(item.getFieldName(), val);
			}
		}
		/*
		 * max/min items?
		 */
		int len = result.length();
		if (len < this.minItems || (this.maxItems != 0 && len > this.maxItems)) {
			Tracer.trace(this.name + " has " + len + " items against a specification of maxItems=" +this.maxItems + " and a minItems=" + this.minItems + " as " );
			return this.invalidValue(messages);
		}

		/*
		 * unique items?
		 */
		if (this.uniqueItem) {
			String[] keys = JSONObject.getNames(result);

			for (int i = 0; i < keys.length; i++) {
				for (int j = i + 1; j < keys.length; j++) {
					if (result.get(keys[i]).equals(result.get(keys[j]))) {
						Tracer.trace(this.name + " has to have unique values, but " + keys[i] + " and " + keys[j] + " have same values");
						return this.invalidValue(messages);
					}
				}
			}
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.simplity.rest.param.Parameter#toWriter(org.simplity.json.JSONWriter, org.simplity.json.JSONObject, boolean)
	 */
	@Override
	public void toWriter(JSONWriter writer, Object data, boolean asAttribute) {
		if(asAttribute){
			writer.key(this.name);
		}
		writer.object();
		/*
		 * write child attributes, if any..
		 */
		if(data != null){
			if(data instanceof JSONObject == false){
				throw new ApplicationError("An object is expected for field " + this.name + "but we got " + data.getClass().getName() );
			}
			JSONObject childData = (JSONObject) data;
			for(Parameter item : this.items){
				item.toWriter(writer, childData.opt(item.getFieldName()), true);
			}
		}
		writer.endObject();
	}

	/* (non-Javadoc)
	 * @see org.simplity.rest.param.Parameter#setHeader(javax.servlet.http.HttpServletResponse, org.simplity.json.JSONObject)
	 */
	@Override
	public void setHeader(HttpServletResponse resp, Object data) {
		if (data == null) {
			return;
		}
		if(data instanceof JSONObject == false){
			throw new ApplicationError("An object is expected for field " + this.name + "but we got " + data.getClass().getName() );
		}
		JSONObject json = (JSONObject)data;
		for(Parameter item : this.items){
			if(item instanceof ObjectParameter){
				throw new ApplicationError("Field " + this.name + " is an object with an attribute named " + item.getName() + " as a child objecy in it. Such an embedded object data can not be used to set header values.");
			}
			item.setHeader(resp, json.opt(item.getFieldName()));
		}
	}

}
