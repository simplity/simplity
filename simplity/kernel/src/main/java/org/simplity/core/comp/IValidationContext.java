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

package org.simplity.core.comp;

/**
 * COntext in which a comp is being validated. This is design for an IDE
 * context. Defined as an interface to separate the IDE functionality from core
 * component functionalities
 *
 * @author simplity.org
 */
public interface IValidationContext {

	/**
	 * report a validation message
	 *
	 * @param message
	 *            non-null
	 */
	public void message(ValidationMessage message);

	/**
	 * register a reference made by the current component to another component.
	 * It is possible that the same reference may be reported gain, in case the
	 * underlying component makes the same reference more than once
	 *
	 * @param reference
	 *            non-null
	 */
	public void reference(ValidationReference reference);
}
