/*
 * Copyright (c) 2016 simplity.org
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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.kernel.comp;

/**
 * component of this application engine. Every component has to have some
 * attributes, that are used by others. Hence we have some getters
 *
 * @author simplity.org
 */
public interface IComponent {

	/**
	 * @return simple name, with no prefix (module). Component name is unique
	 *         within a module, but may not be across modules. Hence a
	 *         moduleName is to be used to refer to a components
	 */
	public String getSimpleName();

	/**
	 * @return qualified name. Dotted if you use modules and sub-modules.
	 *         Similar to java class and package notation.
	 */
	public String getQualifiedName();

	/**
	 * Components may have to set-up their shops after loading the attributes.
	 * Typically ensuring referential integrity, caching etc..
	 *
	 * <p>
	 * In case of components having sub-components, it is generally a good idea
	 * to have setUp() only for the parent component, from where you may trigger
	 * set-up of child components
	 */
	public void getReady();

	/**
	 * Validate this component.
	 *
	 * @param vtx
	 *            validation context
	 */
	public void validate(IValidationContext vtx);

	/** @return type of this component */
	public ComponentType getComponentType();
}
