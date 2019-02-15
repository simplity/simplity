/*
 * Copyright (c) 2015 EXILANT Technologies Private Limited (www.exilant.com)
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
package org.simplity.core.fn;

import org.simplity.core.comp.IComponent;
import org.simplity.core.data.IFieldsCollection;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;

/**
 * We have defined this an interface so that any class that implements some business functionality
 * can be exposed as a function, as well as in any other way that suits the application. No need to
 * write wrappers around such classes
 *
 * @author simplity.org
 */
public interface IFunction extends IComponent {

  /**
   * a function MUSt return a value. Normally functions are designed to have a specific value type
   * as return value. However, we can live with the type being dynamic.
   *
   * @return value type, or null to indicate that the return type may vary.
   */
  public ValueType getReturnType();

  /** @return value types of arguments. A null as the last element means that it is var-args */
  public ValueType[] getArgDataTypes();

  /**
   * main method : execute and return value
   *
   * @param arguments as per specification.
   * @param data optional common data that is available at this execution time
   * @return value of the function
   */
  public Value execute(Value[] arguments, IFieldsCollection data);
}
