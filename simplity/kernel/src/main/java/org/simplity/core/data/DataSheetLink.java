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

package org.simplity.core.data;

/**
 * data structure that has key names for linking parent-child sheets
 *
 * @author simplity.org
 *
 */
public class DataSheetLink {

	/**
	 * child sheet name
	 */
	public final String childSheetName;

	/**
	 * parent sheet name
	 */
	public final String parentSheetName;

	/**
	 * field names in the parent sheet that are used for linking
	 */
	public final int[] parentIndexes;

	/**
	 * field names in the child sheet, corresponding to the names in
	 * parentKeyNames
	 */
	public final int[] childIndexes;

	/**
	 * default constructor with all attributes
	 *
	 * @param parentName
	 * @param childName
	 * @param pindexes
	 * @param chindexes
	 */
	public DataSheetLink(String parentName, String childName, int[] pindexes, int[] chindexes) {
		this.parentIndexes = pindexes;
		this.childIndexes = chindexes;
		this.parentSheetName = parentName;
		this.childSheetName = childName;
	}
}
