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

package org.simplity.ide.comp;

import java.io.InputStream;

/**
 * represents a repository of components and resources that contained them in a
 * project root path
 *
 * @author simplity.org
 *
 */
public interface IRepository {

	/**
	 *
	 * @return root path is like the id of this repository. this is typically
	 *         the folder path relative the project. this folder should follow
	 *         the conventions for storing simplity components
	 */
	public String getRootPath();

	/**
	 * add a resource to the repository. resource is parsed by the repository
	 * for its content of components
	 *
	 * @param path
	 *            path relative to project root.e.g dt/mod1.xml or
	 *            service/mod1/submod1/ser1.xml
	 * @param stream
	 *            using which the resource content can be read
	 */
	public void addResource(String path, InputStream stream);

	/**
	 * content of this resource has changed.
	 *
	 * @param path
	 *            path relative to project root.e.g dt/mod1.xml or
	 *            service/mod1/submod1/ser1.xml
	 * @param stream
	 *            using which the resource content can be read
	 */
	public void modifyResource(String path, InputStream stream);

	/**
	 * remove the existing resource and add this one. same as remove +
	 * add.
	 *
	 * @param path
	 *            non-null. path relative to project root.e.g dt/mod1.xml or
	 *            service/mod1/submod1/ser1.xml
	 * @param oldPath
	 *            non-null. project-relative path from where this resource is
	 *            moved/renamed
	 * @param stream
	 *            using which the resource content can be read
	 */
	public void replaceResource(String path, String oldPath, InputStream stream);

	/**
	 * remove this resource from repository. all components loaded from this
	 * resource are now considered non-existent
	 *
	 * @param path
	 *            path relative to project root.e.g dt/mod1.xml or
	 *            service/mod1/submod1/ser1.xml
	 */
	public void removeResource(String path);

	/**
	 * validate a resource that is not modified.
	 *
	 * @param path
	 *            path relative to project root.e.g dt/mod1.xml or
	 *            service/mod1/submod1/ser1.xml
	 * @param stream
	 *            using which the resource content can be read
	 * @param listener
	 *            receives call-backs on errors
	 */
	public void validateResource(String path, InputStream stream, IValidationListener listener);

	/**
	 * get a list of valid comp names that can be used as a value for this field
	 *
	 * @param fieldName
	 * @param typedValue
	 * @return list of valid values, or null if this field does not refer to any
	 *         comp. empty array if there are no valid comps for this field.
	 */
	public String[] getSuggestedComps(String fieldName, String typedValue);

	/**
	 * get the resource name, relative to repo root, in which this comp is
	 * defined
	 *
	 * @param fieldName
	 *            that has the comp name as its value. We infer comp type based
	 *            on the meta-data about the fields
	 * @param compName
	 * @return resource name, or null if this component is not loaded from any
	 *         resource
	 */
	public String getResourceForComp(String fieldName, String compName);

}
