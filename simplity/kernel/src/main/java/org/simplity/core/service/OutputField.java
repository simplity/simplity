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

package org.simplity.core.service;

import org.simplity.core.app.IResponseWriter;
import org.simplity.core.value.Value;
import org.simplity.json.JSONWriter;

/**
 * @author simplity.org
 *
 *         represents a field to be output to the client
 */
public class OutputField {
	/**
	 * field name
	 */
	String name;

	/**
	 * name to be used for outputting, if it is different from name
	 */
	String outputName;

	/**
	 * write a name-value pair to the writer for this field. writer MUST be in a
	 * state that a writer.key().value() shoudl be valid
	 *
	 * @param writer
	 * @param ctx
	 */
	public void write(JSONWriter writer, ServiceContext ctx) {
		Value val = ctx.getValue(this.name);
		if (Value.isNull(val)) {
			return;
		}
		String nameToUse = this.outputName == null ? this.name : this.outputName;
		writer.key(nameToUse).value(val);
	}
	/**
	 * write a name-value pair to the writer for this field. writer MUST be in a
	 * state that a writer.key().value() shoudl be valid
	 *
	 * @param writer
	 * @param ctx
	 */
	public void write(IResponseWriter writer, ServiceContext ctx) {
		Value val = ctx.getValue(this.name);
		if (Value.isNull(val)) {
			return;
		}
		String nameToUse = this.outputName == null ? this.name : this.outputName;
		writer.setField(nameToUse, val);
	}
}
