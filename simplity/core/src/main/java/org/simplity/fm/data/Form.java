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

package org.simplity.fm.data;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.simplity.fm.ApplicationError;
import org.simplity.fm.IForm;
import org.simplity.fm.Message;
import org.simplity.fm.data.types.ValueType;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author simplity.org
 *
 */
public class Form implements IForm {
	private static final char KEY_JOINER = '_';
	/**
	 * data structure describes the template for which this object provides
	 * actual data
	 */
	private FormStructure structure;
	/**
	 * field values. empty array, but not null if this template has no fields
	 */
	private Object[] fieldValues;
	/**
	 * grid data. empty array, but not null if this template has no fields
	 */
	private Object[][][] gridData;

	@Override
	public String getFormId() {
		return this.structure.getFormId();
	}

	@Override
	public String getDocumentId() {
		/*
		 * concatenate key fields to get document id
		 */
		int[] indexes = this.structure.getKeyIndexes();
		if (indexes == null || indexes.length == 0) {
			throw new ApplicationError("Form " + this.getFormId() + " has no key fields");
		}
		Object obj = this.fieldValues[indexes[0]];
		if (obj == null) {
			return null;
		}
		String key = obj.toString();
		if (indexes.length > 1) {
			for (int i = 1; i < indexes.length; i++) {
				obj = this.fieldValues[indexes[i]];
				if (obj == null) {
					return null;
				}
				key += KEY_JOINER + obj.toString();
			}
		}
		return key;
	}

	@Override
	public void deserialize(String data) {
		throw new ApplicationError(
				"internal serialization method not yet implemented for form. Use json format instead");
	}

	@Override
	public boolean deserialize(String data, List<Message> errors) {
		throw new ApplicationError(
				"internal serialization method not yet implemented for form. Use json format instead");
	}

	@Override
	public String serialize() {
		throw new ApplicationError(
				"internal serialization method not yet implemented for form. Use json format instead");
	}

	@Override
	public void load(ObjectNode json) {
		this.validateAndLoad(json, null);
	}

	@Override
	public void validateAndLoad(Map<String, String> values, List<Message> errors) {
		for (Field f : this.structure.getFields()) {
			this.validateAndSet(f, values.get(f.getFieldName()), this.fieldValues, errors);
		}
		this.validateForm(errors);
	}

	/**
	 * @param validations
	 * @return
	 */
	private boolean validateForm(List<Message> messages) {
		IFormValidation[] validations = this.structure.getValidations();
		if (validations == null) {
			return true;
		}
		boolean allOk = true;
		for (IFormValidation vln : validations) {
			if (!vln.validate(this, messages)) {
				allOk = false;
			}
		}
		return allOk;
	}

	@Override
	public void validateAndLoad(ObjectNode json, List<Message> errors) {
		this.setFeilds(json, this.structure, this.fieldValues, errors);

		String[] gridNames = this.structure.getGridNames();
		if (gridNames == null) {
			return;
		}

		this.gridData = new Object[gridNames.length][][];
		FormStructure[] structs = this.structure.getGridStructures();

		for (int i = 0; i < gridNames.length; i++) {
			String gn = gridNames[i];
			JsonNode child = json.get(gn);
			FormStructure struct = structs[i];
			ArrayNode node = null;
			if (child != null && child.getNodeType() == JsonNodeType.ARRAY) {
				node = (ArrayNode) child;
			}
			int n = 0;
			if (node != null) {
				n = node.size();
			}

			if (errors != null) {
				if (n < struct.getMinRows() || n > struct.getMaxRows()) {
					errors.add(Message.getValidationMessage(gn, struct.getGridMessageId()));
					continue;
				}
			}
			if (n == 0 || node == null) {
				continue;
			}

			for (int j = 0; j < n; j++) {
				JsonNode col = node.get(j);
				if (col == null || col.getNodeType() != JsonNodeType.OBJECT) {
					if (errors != null) {
						errors.add(Message.getValidationMessage(gn, struct.getGridMessageId()));
					}
					break;
				}

				Object[] row = new Object[struct.getFields().length];
				this.setFeilds((ObjectNode) col, struct, row, errors);

			}
		}
	}

	private void setFeilds(ObjectNode json, FormStructure struct, Object[] row, List<Message> errors) {
		for (Field field : struct.getFields()) {
			JsonNode node = json.get(field.getFieldName());
			Object value = null;
			if (node != null) {
				JsonNodeType nt = node.getNodeType();
				if (nt != JsonNodeType.NULL && nt != JsonNodeType.MISSING) {
					if (nt == JsonNodeType.NUMBER) {
						value = node.asLong();
					} else if (nt != JsonNodeType.NULL && nt != JsonNodeType.MISSING) {
						value = nt.toString();
					}
				}
			}

			this.validateAndSet(field, value, row, errors);
		}
		this.validateForm(errors);
	}

	private void validateAndSet(Field field, Object value, Object[] row, List<Message> errors) {

		if (errors == null && value == null) {
			return;
		}

		String textValue = value == null ? null : value.toString();
		if (errors != null) {
			if (field.isValid(textValue) == false) {
				errors.add(Message.getValidationMessage(field.getFieldName(), field.getMessageId()));
				return;
			}
		}

		if (value != null) {
			row[field.getSequenceIdx()] = value;
		}
	}

	@Override
	public void serializeAsJson(OutputStream outStream) throws IOException {
		try (JsonGenerator gen = new JsonFactory().createGenerator(outStream)) {
			gen.writeStartObject();
			this.writeFields(gen, this.fieldValues, this.structure.getFields());
			if (this.gridData != null) {
				this.writeGrids(gen);
			}
			gen.writeEndObject();
		}
	}

	private void writeGrids(JsonGenerator gen) throws IOException {
		FormStructure[] structures = this.structure.getGridStructures();
		String[] gridNames = this.structure.getGridNames();
		for (int i = 0; i < this.gridData.length; i++) {
			Object[][] grid = this.gridData[i];
			if (grid == null) {
				continue;
			}
			gen.writeArrayFieldStart(gridNames[i]);
			this.writeGrid(gen, grid, structures[i]);
			gen.writeEndArray();
		}
	}

	private void writeGrid(JsonGenerator gen, Object[][] grid, FormStructure gridStructure) throws IOException {
		Field[] columns = gridStructure.getFields();
		for (int i = 0; i < grid.length; i++) {
			gen.writeStartObject();
			this.writeFields(gen, grid[i], columns);
			gen.writeEndObject();
		}
	}

	private void writeFields(JsonGenerator gen, Object[] values, Field[] fields) throws IOException {
		for (int j = 0; j < values.length; j++) {
			Object value = values[j];
			if (value == null) {
				continue;
			}
			Field field = fields[j];
			String fn = field.getFieldName();
			if (field.getValueType() == ValueType.Text) {
				gen.writeStringField(fn, value.toString());
			} else {
				gen.writeNumberField(fn, (Long) value);
			}
		}
	}

	@Override
	public String getValue(String fieldName) {
		Field field = this.structure.getField(fieldName);
		if (field == null) {
			return null;
		}
		Object obj = this.fieldValues[field.getSequenceIdx()];
		if (obj == null) {
			return null;
		}
		return obj.toString();
	}

	@Override
	public long getLongValue(String fieldName) {
		Field field = this.structure.getField(fieldName);
		if (field == null || field.getValueType() == ValueType.Text) {
			return 0;
		}
		Object obj = this.fieldValues[field.getSequenceIdx()];
		if (obj == null) {
			return 0;
		}
		return (Long) obj;
	}

	@Override
	public boolean setValue(String fieldName, String value) {
		Field field = this.structure.getField(fieldName);
		if (field == null) {
			return false;
		}
		int idx = field.getSequenceIdx();
		if (field.getValueType() == ValueType.Text) {
			this.fieldValues[idx] = value;
			return true;
		}

		long val = 0;
		if (value != null) {
			try {
				val = Long.parseLong(value);
			} catch (Exception e) {
				return false;
			}
		}
		this.fieldValues[idx] = val;
		return true;
	}

	@Override
	public boolean setLongValue(String fieldName, long value) {
		Field field = this.structure.getField(fieldName);
		if (field == null) {
			return false;
		}
		int idx = field.getSequenceIdx();
		if (field.getValueType() == ValueType.Text) {
			this.fieldValues[idx] = "" + value;
		} else {
			this.fieldValues[idx] = value;
		}
		return true;
	}
}
