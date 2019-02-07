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

package org.simplity.kernel.comp;

/**
 * data structure that holds all details of reference to a component
 *
 * @author simplity.org
 *
 */
public class ValidationReference {

	/**
	 * object that is has a reference to another component. non-null
	 */
	public final Object referringObject;
	/**
	 * attribute/field name in the referring object that is used to refer to the
	 * external component
	 */
	public final String fieldName;
	/**
	 * id/qualified name of the component being referred. non-null;
	 */
	public final String qualifiedName;

	/**
	 * componentType - non-null
	 */
	public final ComponentType componentType;

	/**
	 * constructor with all attributes
	 *
	 * @param referringObject
	 *            object that is has a reference to another component. non-null
	 *
	 * @param fieldName
	 *            attribute/field name in the referring object that is used to
	 *            refer to the external component
	 * @param qualifiedName
	 *            id/qualified name. non-null.
	 * @param componentType
	 */
	public ValidationReference(Object referringObject, String fieldName, String qualifiedName,
			ComponentType componentType) {
		this.referringObject = referringObject;
		this.fieldName = fieldName;
		this.qualifiedName = qualifiedName;
		this.componentType = componentType;
	}
}
