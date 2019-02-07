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

package org.simplity.kernel.app.internal;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.simplity.json.JSONWriter;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.app.IResponseWriter;
import org.simplity.kernel.data.IDataSheet;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.kernel.value.Value;

/**
 * writer to which a json can be written out in parts
 *
 * @author simplity.org
 *
 */
public class JsonRespWriter implements IResponseWriter {
	private JSONWriter writer;
	/**
	 * is this writer for an array? In that case we do not start an object, and
	 * wait for beginArray() to be called
	 */
	private boolean forAnArray;

	/**
	 * crate a string writer.
	 *
	 * @param writer
	 *            that will receive the output
	 */
	public JsonRespWriter(Writer writer) {
		this.writer = new JSONWriter(writer);
		this.writer.object();
	}

	/**
	 * crate a string writer.
	 *
	 * @param writer
	 *            that will receive the output. can be null
	 * @param forAnArray
	 *            true if an array is going to be written, and not an object
	 */
	public JsonRespWriter(Writer writer, boolean forAnArray) {
		this.writer = new JSONWriter(writer);
		if (forAnArray) {
			this.forAnArray = true;
			this.writer.array();
		} else {
			this.writer.object();
		}
	}

	/**
	 * crate a string writer.
	 *
	 * @param stream
	 *            that will receive the output
	 */
	public JsonRespWriter(OutputStream stream) {
		this.writer = new JSONWriter(new OutputStreamWriter(stream));
		this.writer.object();
	}

	@Override
	public void done() {
		if (this.forAnArray) {
			this.writer.endArray();
		} else {
			this.writer.endObject();
		}
	}

	@Override
	public JsonRespWriter setField(String fieldName, Object value) {
		this.writer.key(fieldName).value(value);
		return this;
	}

	@Override
	public JsonRespWriter setField(String fieldName, Value value) {
		this.writer.key(fieldName).value(value);
		return this;
	}

	@Override
	public JsonRespWriter addToArray(Object value) {
		this.writer.value(value);
		return this;
	}

	@Override
	public JsonRespWriter setObject(String fieldName, Object value) {
		this.writer.key(fieldName);
		JsonUtil.addObject(this.writer, value);
		return this;
	}

	@Override
	public JsonRespWriter setArray(String arrayName, Object[] arr) {
		this.writer.key(arrayName).array();
		if (arr != null && arr.length != 0) {
			for (Object value : arr) {
				JsonUtil.addObject(this.writer, value);
			}
		}
		this.writer.endArray();
		return this;
	}

	@Override
	public JsonRespWriter setArray(String arrayName, IDataSheet sheet) {
		this.writer.key(arrayName).array();
		if (sheet != null && sheet.length() > 0 && sheet.width() > 0) {
			for (Value[] row : sheet.getAllRows()) {
				Value value = row[0];
				if (value != null) {
					this.writer.value(value);
				}
			}
		}
		this.writer.endArray();
		return this;
	}

	@Override
	public JsonRespWriter beginObject(String objectName) {
		if (objectName == null) {
			if (this.forAnArray) {
				this.writer.object();
				return this;
			}
			throw new ApplicationError(
					"Writer is initiated for a root-object based json, but a subsequent call is made for an array as root.");
		}
		this.writer.key(objectName).object();
		return this;

	}

	@Override
	public JsonRespWriter beginObjectAsArrayElement() {
		this.writer.object();
		return this;
	}

	@Override
	public JsonRespWriter endObject() {
		this.writer.endObject();
		return this;
	}

	@Override
	public JsonRespWriter beginArray(String arrayName) {
		if (arrayName == null) {
			if (this.forAnArray) {
				this.writer.array();
				return this;
			}
			throw new ApplicationError(
					"Writer is initiated for a root-object based json, but a subsequent call is made for an array as root.");
		}
		this.writer.key(arrayName).array();
		return this;
	}

	@Override
	public JsonRespWriter beginArrayAsArrayElement() {
		this.writer.array();
		return this;
	}

	@Override
	public JsonRespWriter endArray() {
		this.writer.endArray();
		return this;
	}
}
