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
package org.simplity.core.expr;

/**
 * models a unary operator that operates on a value. Only this abstract class is public. Concrete
 * sub-classes are implemented as singletons through a static newXXX() method on this abstract
 * class. negative (-), Not (!), Exists (?) and Missing(~) are the four operators implemented with
 * this class.
 *
 * @author simplity.org
 */
public enum UnaryOperator {
  /** valid only for numeric data */
  Minus {
    @Override
    public String toString() {
      return "" + UnaryOperator.MINUS;
    }
  },
  /** valid only for boolean data */
  Not {
    @Override
    public String toString() {
      return "" + UnaryOperator.NOT;
    }
  },
  /** pseudo operator - true if field has value at run time, false otherwise */
  IsKnown {
    @Override
    public String toString() {
      return "" + UnaryOperator.IS_KNOWN;
    }
  },
  /** pseudo operator - true if field does not value at run time, false if it has value */
  IsUnknown {
    @Override
    public String toString() {
      return "" + UnaryOperator.IS_UNKNOWN;
    }
  };
  /** char value of minus operator */
  public static final char MINUS = '-';
  /** char value of Not operator */
  public static final char NOT = '!';
  /** char value of IsKnown operator */
  public static final char IS_KNOWN = '?';
  /** char value of IsUnknown operator */
  public static final char IS_UNKNOWN = '~';

  /**
   * get operator enum for a char
   *
   * @param c
   * @return null if the char is not one of -!?~
   */
  public static UnaryOperator getOperator(char c) {
    switch (c) {
      case MINUS:
        return UnaryOperator.Minus;
      case NOT:
        return UnaryOperator.Not;
      case IS_UNKNOWN:
        return IsUnknown;
      case IS_KNOWN:
        return IsKnown;
      default:
        return null;
    }
  }
}
