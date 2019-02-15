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

import org.simplity.core.util.DateUtil;
import org.simplity.core.value.DateValue;
import org.simplity.core.value.InvalidValueException;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;

/**
 * models a binary operator that operates like operand1 Operator operand2. Example a + b
 *
 * @author simplity.org
 */
public enum BinaryOperator {
  /** multiply */
  Multiply {

    @Override
    protected Value doOperate(Value leftValue, Value rightValue) throws InvalidValueException {
      ValueType lt = leftValue.getValueType();
      ValueType rt = rightValue.getValueType();
      int opType = this.getNumericType(lt, rt);
      if (opType == BinaryOperator.INTEGER_OPERATION) {
        return Value.newIntegerValue(leftValue.toInteger() * rightValue.toInteger());
      }
      if (opType == BinaryOperator.DECIMAL_OPERATION) {
        return Value.newDecimalValue(leftValue.toDecimal() * rightValue.toDecimal());
      }
      throw new InvalidValueException("");
    }

    @Override
    int getPrecedence() {
      return BinaryOperator.PREC_MULTPLY_DIVIDE;
    }

    @Override
    public String toString() {
      return "" + Chars.MULT;
    }
  },
  /** divide */
  Divide {

    @Override
    protected Value doOperate(Value leftValue, Value rightValue) throws InvalidValueException {
      ValueType lt = leftValue.getValueType();
      ValueType rt = rightValue.getValueType();
      int opType = this.getNumericType(lt, rt);
      if (opType == BinaryOperator.INTEGER_OPERATION) {
        return Value.newIntegerValue(leftValue.toInteger() / rightValue.toInteger());
      }
      if (opType == BinaryOperator.DECIMAL_OPERATION) {
        return Value.newDecimalValue(leftValue.toDecimal() / rightValue.toDecimal());
      }
      throw new InvalidValueException("");
    }

    @Override
    int getPrecedence() {
      return BinaryOperator.PREC_MULTPLY_DIVIDE;
    }

    @Override
    public String toString() {
      return "" + Chars.DIVIDE;
    }
  },
  /** Modulo /Remainder */
  Modulo {

    @Override
    protected Value doOperate(Value leftValue, Value rightValue) throws InvalidValueException {
      ValueType lt = leftValue.getValueType();
      ValueType rt = rightValue.getValueType();
      int opType = this.getNumericType(lt, rt);
      if (opType == BinaryOperator.INTEGER_OPERATION) {
        return Value.newIntegerValue(leftValue.toInteger() % rightValue.toInteger());
      }
      throw new InvalidValueException("");
    }

    @Override
    int getPrecedence() {
      return BinaryOperator.PREC_MULTPLY_DIVIDE;
    }

    @Override
    public String toString() {
      return "" + Chars.MODULO;
    }
  },
  /** add */
  Plus {

    @Override
    protected Value doOperate(Value leftValue, Value rightValue) throws InvalidValueException {
      ValueType lt = leftValue.getValueType();
      ValueType rt = rightValue.getValueType();
      int opType = this.getNumericType(lt, rt);
      if (opType == INTEGER_OPERATION) {
        return Value.newIntegerValue(leftValue.toInteger() + rightValue.toInteger());
      }
      if (opType == DECIMAL_OPERATION) {
        return Value.newDecimalValue(leftValue.toDecimal() + rightValue.toDecimal());
      }
      if (opType == DATE_ADD_OPERATION) {
        return Value.newDateValue(DateUtil.addDays(leftValue.toDate(), rightValue.toInteger()));
      }
      /*
       * plus is concatenate for nun-number
       */
      return Value.newTextValue(leftValue.toText() + rightValue.toText());
    }

    @Override
    int getPrecedence() {
      return BinaryOperator.PREC_ADD_DELETE;
    }

    @Override
    public String toString() {
      return "" + Chars.PLUS;
    }
  },
  /** subtract */
  Minus {

    @Override
    protected Value doOperate(Value leftValue, Value rightValue) throws InvalidValueException {
      ValueType lt = leftValue.getValueType();
      ValueType rt = rightValue.getValueType();
      int opType = this.getNumericType(lt, rt);
      if (opType == BinaryOperator.INTEGER_OPERATION) {
        return Value.newIntegerValue(leftValue.toInteger() - rightValue.toInteger());
      }
      if (opType == BinaryOperator.DECIMAL_OPERATION) {
        return Value.newDecimalValue(leftValue.toDecimal() - rightValue.toDecimal());
      }
      if (opType == BinaryOperator.DATE_SUBTRACT_OPERATION) {
        return Value.newIntegerValue(
            DateUtil.daysBetweenDates(
                ((DateValue) leftValue).getDate(), ((DateValue) rightValue).getDate()));
      }
      throw new InvalidValueException("");
    }

    @Override
    int getPrecedence() {
      return BinaryOperator.PREC_ADD_DELETE;
    }

    @Override
    public String toString() {
      return "" + Chars.MINUS;
    }
  },
  /** less than */
  Less {

    @Override
    protected Value doOperate(Value leftValue, Value rightValue) throws InvalidValueException {
      ValueType lt = leftValue.getValueType();
      ValueType rt = rightValue.getValueType();
      int opType = this.getNumericType(lt, rt);
      if (opType == BinaryOperator.INTEGER_OPERATION) {
        return Value.newBooleanValue(leftValue.toInteger() < rightValue.toInteger());
      }
      if (opType == BinaryOperator.DECIMAL_OPERATION) {
        return Value.newBooleanValue(leftValue.toDecimal() < rightValue.toDecimal());
      }

      if (lt == ValueType.BOOLEAN || lt != rt) {
        throw new InvalidValueException("");
      }
      if (lt == ValueType.DATE) {
        return Value.newBooleanValue(leftValue.toDate().getTime() < rightValue.toDate().getTime());
      }
      return Value.newBooleanValue(leftValue.toText().compareToIgnoreCase(rightValue.toText()) < 0);
    }

    @Override
    int getPrecedence() {
      return BinaryOperator.PREC_COMPARE;
    }

    @Override
    public String toString() {
      return "" + Chars.LESS;
    }
  },
  /** less than or equal */
  LessOrEqual {

    @Override
    protected Value doOperate(Value leftValue, Value rightValue) throws InvalidValueException {
      ValueType lt = leftValue.getValueType();
      ValueType rt = rightValue.getValueType();
      int opType = this.getNumericType(lt, rt);
      if (opType == BinaryOperator.INTEGER_OPERATION) {
        return Value.newBooleanValue(leftValue.toInteger() <= rightValue.toInteger());
      }
      if (opType == BinaryOperator.DECIMAL_OPERATION) {
        return Value.newBooleanValue(leftValue.toDecimal() <= rightValue.toDecimal());
      }

      if (lt == ValueType.BOOLEAN || lt != rt) {
        throw new InvalidValueException("");
      }
      if (lt == ValueType.DATE) {
        return Value.newBooleanValue(leftValue.toDate().getTime() <= rightValue.toDate().getTime());
      }
      return Value.newBooleanValue(
          leftValue.toText().compareToIgnoreCase(rightValue.toText()) <= 0);
    }

    @Override
    int getPrecedence() {
      return BinaryOperator.PREC_COMPARE;
    }

    @Override
    public String toString() {
      return BinaryOperator.LESS_THAN_OR_EQUAL;
    }
  },
  /** greater than */
  Greater {

    @Override
    protected Value doOperate(Value leftValue, Value rightValue) throws InvalidValueException {
      ValueType lt = leftValue.getValueType();
      ValueType rt = rightValue.getValueType();
      int opType = this.getNumericType(lt, rt);
      if (opType == BinaryOperator.INTEGER_OPERATION) {
        return Value.newBooleanValue(leftValue.toInteger() > rightValue.toInteger());
      }
      if (opType == BinaryOperator.DECIMAL_OPERATION) {
        return Value.newBooleanValue(leftValue.toDecimal() > rightValue.toDecimal());
      }

      if (lt == ValueType.BOOLEAN || lt != rt) {
        throw new InvalidValueException("");
      }
      if (lt == ValueType.DATE) {
        return Value.newBooleanValue(leftValue.toDate().getTime() > rightValue.toDate().getTime());
      }
      return Value.newBooleanValue(leftValue.toText().compareToIgnoreCase(rightValue.toText()) > 0);
    }

    @Override
    int getPrecedence() {
      return BinaryOperator.PREC_COMPARE;
    }

    @Override
    public String toString() {
      return "" + Chars.GREATER;
    }
  },
  /** greater than or equal */
  GreaterOrEqual {

    @Override
    protected Value doOperate(Value leftValue, Value rightValue) throws InvalidValueException {
      ValueType lt = leftValue.getValueType();
      ValueType rt = rightValue.getValueType();
      int opType = this.getNumericType(lt, rt);
      if (opType == BinaryOperator.INTEGER_OPERATION) {
        return Value.newBooleanValue(leftValue.toInteger() >= rightValue.toInteger());
      }
      if (opType == BinaryOperator.DECIMAL_OPERATION) {
        return Value.newBooleanValue(leftValue.toDecimal() >= rightValue.toDecimal());
      }

      if (lt == ValueType.BOOLEAN || lt != rt) {
        throw new InvalidValueException("");
      }
      if (lt == ValueType.DATE) {
        return Value.newBooleanValue(leftValue.toDate().getTime() >= rightValue.toDate().getTime());
      }
      return Value.newBooleanValue(
          leftValue.toText().compareToIgnoreCase(rightValue.toText()) >= 0);
    }

    @Override
    int getPrecedence() {
      return BinaryOperator.PREC_COMPARE;
    }

    @Override
    public String toString() {
      return BinaryOperator.GREATER_THAN_OR_EQUAL;
    }
  },
  /** equal */
  Equal {

    @Override
    protected Value doOperate(Value leftValue, Value rightValue) throws InvalidValueException {
      ValueType lt = leftValue.getValueType();
      ValueType rt = rightValue.getValueType();
      int opType = this.getNumericType(lt, rt);
      if (opType == BinaryOperator.INTEGER_OPERATION) {
        return Value.newBooleanValue(leftValue.toInteger() == rightValue.toInteger());
      }
      if (opType == BinaryOperator.DECIMAL_OPERATION) {
        return Value.newBooleanValue(
            leftValue.toDecimal() - rightValue.toDecimal() < BinaryOperator.DECIMAL_ZERO);
      }

      if (lt != rt) {
        throw new InvalidValueException("");
      }
      if (lt == ValueType.DATE) {
        return Value.newBooleanValue(leftValue.toDate().getTime() == rightValue.toDate().getTime());
      }
      if (lt == ValueType.BOOLEAN) {
        return Value.newBooleanValue(leftValue.toBoolean() == rightValue.toBoolean());
      }
      return Value.newBooleanValue(
          leftValue.toText().compareToIgnoreCase(rightValue.toText()) == 0);
    }

    @Override
    int getPrecedence() {
      return BinaryOperator.PREC_COMPARE;
    }

    @Override
    public String toString() {
      return "" + Chars.EQUAL;
    }
  },
  /** not equal */
  NotEqual {

    @Override
    protected Value doOperate(Value leftValue, Value rightValue) throws InvalidValueException {
      ValueType lt = leftValue.getValueType();
      ValueType rt = rightValue.getValueType();
      int opType = this.getNumericType(lt, rt);
      if (opType == BinaryOperator.INTEGER_OPERATION) {
        return Value.newBooleanValue(leftValue.toInteger() != rightValue.toInteger());
      }
      if (opType == BinaryOperator.DECIMAL_OPERATION) {
        return Value.newBooleanValue(
            leftValue.toDecimal() - rightValue.toDecimal() > BinaryOperator.DECIMAL_ZERO);
      }

      if (lt != rt) {
        throw new InvalidValueException("");
      }
      if (lt == ValueType.DATE) {
        return Value.newBooleanValue(leftValue.toDate().getTime() != rightValue.toDate().getTime());
      }
      if (lt == ValueType.BOOLEAN) {
        return Value.newBooleanValue(leftValue.toBoolean() != rightValue.toBoolean());
      }
      return Value.newBooleanValue(
          leftValue.toText().compareToIgnoreCase(rightValue.toText()) != 0);
    }

    @Override
    int getPrecedence() {
      return BinaryOperator.PREC_COMPARE;
    }

    @Override
    public String toString() {
      return BinaryOperator.NOT_EQUAL;
    }
  },

  /** logical AND */
  And {

    @Override
    protected Value doOperate(Value leftValue, Value rightValue) throws InvalidValueException {
      return Value.newBooleanValue(leftValue.toBoolean() && rightValue.toBoolean());
    }

    @Override
    int getPrecedence() {
      return BinaryOperator.PREC_AND;
    }

    @Override
    public String toString() {
      return "" + Chars.AND;
    }
  },
  /** Logical Or */
  Or {

    @Override
    protected Value doOperate(Value leftValue, Value rightValue) throws InvalidValueException {
      return Value.newBooleanValue(leftValue.toBoolean() && rightValue.toBoolean());
    }

    @Override
    int getPrecedence() {
      return BinaryOperator.PREC_OR;
    }

    @Override
    public String toString() {
      return "" + Chars.OR;
    }
  },
  /** list (comma) is a pseudo operator to handle list of arguments for a function */
  List {

    @Override
    protected Value doOperate(Value leftValue, Value rightValue) throws InvalidValueException {
      throw new InvalidValueException("");
    }

    @Override
    int getPrecedence() {
      return BinaryOperator.PREC_LIST;
    }

    @Override
    public String toString() {
      return "" + Chars.LIST;
    }
  };
  /*
   * type of operation based on operand types
   */
  private static final int NON_NUMERIC_OPERATION = 0;
  private static final int DECIMAL_OPERATION = 1;
  private static final int INTEGER_OPERATION = 2;
  private static final int DATE_ADD_OPERATION = 3;
  private static final int DATE_SUBTRACT_OPERATION = 4;

  /*
   * precedence
   */
  private static final int PREC_MULTPLY_DIVIDE = 1;
  private static final int PREC_ADD_DELETE = 2;
  private static final int PREC_COMPARE = 3;
  private static final int PREC_AND = 4;
  private static final int PREC_OR = 5;
  private static final int PREC_LIST = 6;

  /*
   * string for operators that have more than one char
   */
  private static final String NOT_EQUAL = "!=";
  private static final String LESS_THAN_OR_EQUAL = "<=";
  private static final String GREATER_THAN_OR_EQUAL = ">=";

  /** decimal comparison is dangerous. Use this as zero */
  private static final double DECIMAL_ZERO = 0.0000001;

  /**
   * get an instance of the desired operator
   *
   * @param operator
   * @return binary operator, or null if the input char has no operator associated with that
   */
  static BinaryOperator getOperator(char operator) {
    switch (operator) {
      case Chars.PLUS:
        return BinaryOperator.Plus;
      case Chars.MINUS:
        return BinaryOperator.Minus;
      case Chars.MULT:
        return BinaryOperator.Multiply;
      case Chars.DIVIDE:
        return BinaryOperator.Divide;
      case Chars.MODULO:
        return BinaryOperator.Modulo;
      case Chars.GREATER:
        return BinaryOperator.Greater;
      case Chars.GREATER_OR_EQUAL:
        return BinaryOperator.GreaterOrEqual;
      case Chars.LESS:
        return BinaryOperator.Less;
      case Chars.LESS_OR_EQUAL:
        return BinaryOperator.LessOrEqual;
      case Chars.EQUAL:
        return BinaryOperator.Equal;
      case Chars.NOT_EQUAL:
        return BinaryOperator.NotEqual;
      case Chars.AND:
        return BinaryOperator.And;
      case Chars.OR:
        return BinaryOperator.Or;
      case Chars.LIST:
        return BinaryOperator.List;
      default:
        return null;
    }
  }

  /**
   * carry out the operation on patients and return the result. (operation successful and patients
   * dead :-) )
   *
   * @param leftValue
   * @param rightValue
   * @return resultant value. Type of value depends on the operation and operands
   * @throws InvalidOperationException
   */
  public Value operate(Value leftValue, Value rightValue) throws InvalidOperationException {
    if (leftValue == null
        || rightValue == null
        || leftValue.isUnknown()
        || rightValue.isUnknown()) {
      return Value.newUnknownValue(ValueType.TEXT);
    }
    try {
      return this.doOperate(leftValue, rightValue);
    } catch (InvalidValueException e) {
      throw new InvalidOperationException(
          "Binary operation "
              + this
              + " is not possible between "
              + leftValue.getValueType()
              + "("
              + leftValue
              + ") and "
              + rightValue.getValueType()
              + "("
              + rightValue
              + ")");
    }
  }

  /**
   * carry out this binary operation on the operands
   *
   * @param leftValue
   * @param rightValue
   * @return result of this binary operation
   * @throws InvalidValueException
   */
  protected abstract Value doOperate(Value leftValue, Value rightValue)
      throws InvalidValueException;

  protected int getNumericType(ValueType lt, ValueType rt) {
    /*
     * if left side is numeric, right side HAS to be numeric for a numeric
     * operation
     */
    if (lt == ValueType.DECIMAL) {
      if (rt == ValueType.DECIMAL || rt == ValueType.INTEGER) {
        return BinaryOperator.DECIMAL_OPERATION;
      }
      return BinaryOperator.NON_NUMERIC_OPERATION;
    }
    if (lt == ValueType.INTEGER) {
      if (rt == ValueType.DECIMAL) {
        return BinaryOperator.DECIMAL_OPERATION;
      }
      if (rt == ValueType.INTEGER) {
        return BinaryOperator.INTEGER_OPERATION;
      }
      return BinaryOperator.NON_NUMERIC_OPERATION;
    }
    /*
     * if left side is date, we can have right side as either date or number
     */
    if (lt == ValueType.DATE) {
      if (rt == ValueType.DECIMAL || rt == ValueType.INTEGER) {
        return BinaryOperator.DATE_ADD_OPERATION;
      }
      if (rt == ValueType.DATE) {
        return BinaryOperator.DATE_SUBTRACT_OPERATION;
      }
    }
    return BinaryOperator.NON_NUMERIC_OPERATION;
  }

  /**
   * lower precedence implies higher priority
   *
   * @return precedence
   */
  abstract int getPrecedence();
}
