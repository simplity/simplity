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

import java.util.Map;

/**
 * @author simplity.org
 *
 */
public class FormStructure {
	/**
	 * this is the unique id given this to this form, it is an independent
	 * form. It is the section name in case it is a section of a composite form
	 */
	private String formId;

	/**
	 * index to the values array for the key fields
	 */
	private int[] keyIndexes;
	/**
	 * data elements are sequenced so that the values can be saved in an array
	 */
	private Field[] fields;

	/**
	 * name of grids. null if there are no grids;
	 */
	private String[] gridNames;
	/**
	 * structure of grids. null if there are no grids
	 */
	private FormStructure[] grids;
	/**
	 * fields are also stored as Maps for ease of access
	 */
	private Map<String, Field> fieldsMap;
	/**
	 * grid structures are also stored in map for ease of access
	 */
	private Map<String, FormStructure> gridsMap;

	/**
	 * describes all the inter-field validations, and form-level validations
	 */
	private IFormValidation[] validations;
	/**
	 * for validating data.
	 */
	private int minRows;
	/**
	 * for validating data
	 */
	private int maxRows;

	/**
	 * zero based index of this section, if this is a section ofa composite form
	 */
	private int sequenceIdx;

	/**
	 * message to be used if the grid has less than the min or greater than the
	 * max rows. null if no min/max restrictions
	 */
	private String gridMessageId;

	/**
	 * unique id assigned to this form. like customerDetails. This is unique
	 * across all types of forms within a project
	 * 
	 * @return non-null unique id
	 */
	public String getFormId() {
		return this.formId;
	}

	/**
	 * @return the keyIndexes
	 */
	public int[] getKeyIndexes() {
		return this.keyIndexes;
	}

	/**
	 * @return the fieldNames. non-null. could be empty
	 */
	public Field[] getFields() {
		return this.fields;
	}

	/**
	 * @return the grid names. non-null. could be empty
	 */
	public FormStructure[] getGridStructures() {
		return this.grids;
	}

	/**
	 * @param fieldName
	 * @return data element or null if there is no such field
	 */
	public Field getField(String fieldName) {
		return this.fieldsMap.get(fieldName);
	}

	/**
	 * 
	 * @param gridName
	 * @return form structure that represents this grid, or null if no such grid
	 */
	public FormStructure getGridStructure(String gridName) {
		return this.gridsMap.get(gridName);
	}

	/**
	 * @return the sequenceIdx
	 */
	public int getSequenceIdx() {
		return this.sequenceIdx;
	}

	/**
	 * @return the gridNames
	 */
	public String[] getGridNames() {
		return this.gridNames;
	}

	/**
	 * @return the minRows
	 */
	public int getMinRows() {
		return this.minRows;
	}

	/**
	 * @return the maxRows
	 */
	public int getMaxRows() {
		return this.maxRows;
	}

	/**
	 * @return the validations
	 */
	public IFormValidation[] getValidations() {
		return this.validations;
	}

	/**
	 * @return message id to be used if the grid does not have the right number
	 *         of rows
	 */
	public String getGridMessageId() {
		return this.gridMessageId;
	}
}
