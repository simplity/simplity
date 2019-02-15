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
package org.simplity.core.value;

/**
 * Represents an exceptional condition that an invalid value is being used to create Value object
 *
 * @author simplity.org
 */
public class InvalidValueException extends Exception {

  private static final long serialVersionUID = 1L;
  private String message;

  /**
   * parse exception
   *
   * @param textValue value that is parsed
   * @param valueType target value type
   */
  protected InvalidValueException(String textValue, ValueType valueType) {
    this.message = textValue + " is not a valid value for value type " + valueType;
  }

  /**
   * source and target types are not compatible
   *
   * @param sourceType
   * @param targetType
   */
  protected InvalidValueException(ValueType sourceType, ValueType targetType) {
    this.message =
        "A value of type " + sourceType + " is encountered when we expected type " + targetType;
  }

  /** @param errorMessage */
  public InvalidValueException(String errorMessage) {
    this.message = errorMessage;
  }

  @Override
  public String getMessage() {
    return this.message;
  }
}
