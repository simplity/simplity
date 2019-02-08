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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.simplity.core.data.DynamicSheet;
import org.simplity.core.data.IFieldsCollection;
import org.simplity.core.util.DateUtil;
import org.simplity.core.value.InvalidValueException;
import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;

/**
 * Expression can contain operands :
 *
 * <ul>
 *   <li>numeric constant as in 0.4, 4 but not .6 and definitely not 2.3e5
 *   <li>text/string literal like 'abcd', "d'Souza" but not "d\"Souza"
 *   <li>date literal like /2012-12-31/ no other format at all, no time etc..
 *   <li>a fieldName like usual rules, . and _ are the only special characters allowed
 *   <li>a function, name is same as field name. function should be defined by the project
 * </ul>
 *
 * <p>Unary operators : allowed for operands of the right type
 *
 * <ul>
 *   <li>- for decimal and integer types only
 *   <li>! for logical(boolean) type
 *   <li>? pronounced as "is known". true if the field name is found in data source at the time of
 *       execution
 *   <li>~ pronounced as "is unknown". true if the field name is not defined in data source. not ?
 * </ul>
 *
 * <p>Binary operator : between two operands
 * <li>arithmetic + - * / with our addition if : for remainder valid only between integers
 * <li>+ as string/text appender
 * <li>+ to add number of days to a date as in /2015-12-31/ + 21
 * <li>- to get number of days between two dates like in deliveredDate - orderedDate
 * <li>\< \> = != \<= \>= as comparators between compatible types.
 * <li>& | between booleans, and not && ||. this is not java :-). Important to note that we do
 *     lazy-evaluation. That is, if first operand is true we do not evaluate the second one in an |
 *     operation.
 * <li>, is a pseudo operator between two operands to satisfy syntax. that is how you specify
 *     argument list for a function like foo(a, b, 12)
 * </ul>
 *
 * <p>Braces
 *
 * <ul>
 *   <li>( { [ to begin and ) } } to end. objective is to alter the natural precedence of the
 *       operators. In our scheme of things we consider it a sub-expression. A close brace not-only
 *       has to have a corresponding open brace, their type also has to match. We were lenient on
 *       this earlier, but realized that being strict helps users
 *   <li>( ) to provide list of arguments for function like foo() or faa(a,23)
 * </ul>
 *
 * <p>White space - the usual suspects space tab and newLine
 *
 * <p>Expressions are typically design components, and are loaded/compiled once and used repeatedly
 * as part of service delivery
 *
 * <p>Nulls : We do not allow Value object itself to be (java) null. That is the reason we provided
 * known(?) and (~)unknown unary operators. However, a Value instance may have null as its
 * (legitimate) value. If such a value participates in any operation, resultant will be null.
 * Important to note the meaning of "participates". In case of logical &, | we may not even look at
 * the second operand in case value of the first one determines the resultant value, even if the
 * second would have turned out to be null
 *
 * @author simplity.org
 */
public class Expression {
	private static final Logger logger = LoggerFactory.getLogger(Expression.class);

  /*
   * We have taken a '"manual approach" in our expression. But why? Just like
   * that -) For the intended audience, we do not expect hundreds of
   * expressions in a service. Our advise is very clear. If you end up using
   * too many expressions, just switch to a java class. <p> Our algorithm
   * mimics a manual way of simplifying an expression step-by-step. In our
   * model, if an expression has n operands, it is solved in n-1 steps, each
   * step carrying out one binary operation between two operands replacing
   * them with the resultant value.
   *
   * <p>And, we handle braces by parsing them into 'sub-expression'.
   * sub-expression is treated like an operand.
   */
  /** text that is parsed into this expression */
  String expressionText;

  /** expression is parsed into operands, in the same order as they appear */
  Operand[] operands = null;
  /**
   * Calculation step is similar to the one if you and I were to do it manually. Each step carries
   * out one binary operation and simplifies the expression into one less operand. This is left as
   * null if we have only one operand (or of course there are no operands)
   */
  Step[] calculationSteps = null;
  /**
   * Expression with comma operator are used exclusively for function argument list. we keep this
   * count handy to call to evaluate this expression
   */
  int nbrCommas = 0;

  /**
   * this is an immutable instance that is thread-safe. Hence expression is set at the time of
   * construction, and an exception is thrown in case of syntax error
   *
   * @param inputText
   * @throws InvalidExpressionException in case of syntax error
   */
  public Expression(String inputText) throws InvalidExpressionException {
    if (inputText == null) {
      throw new InvalidExpressionException(null, "Expression is null.", 0);
    }

    this.expressionText = inputText.trim();
    if (this.expressionText.length() == 0) {
      throw new InvalidExpressionException("", "Expression is empty.", 0);
    }

    try {
      ExpressionParser parser = new ExpressionParser();
      parser.parse(this.expressionText.toCharArray(), 0, false);
      parser.setupShop();
    } catch (InternalParseException e) {
      throw new InvalidExpressionException(this.expressionText, e.error, e.errorAt);
    }
  }

  /**
   * internal constructor for parsing sub-expression
   *
   * @param chars
   * @param startingAt parsing to start at this char position (0 based)
   * @param commaOk if this sub-expression is for function
   * @throws InternalParseException
   */
  Expression(char[] chars, int startingAt, boolean commaOk) throws InternalParseException {

    ExpressionParser worker = new ExpressionParser();
    worker.parse(chars, startingAt, commaOk);
    this.expressionText = new String(chars, startingAt + 1, worker.parsingAt - startingAt);
    worker.setupShop();
  }

  /**
   * evaluate this expression against a data source
   *
   * @param data
   * @return value of this expression
   * @throws InvalidOperationException
   */
  public Value evaluate(IFieldsCollection data) throws InvalidOperationException {
    /*
     * this is just a safety. As per our current design, this should not
     * happen
     */
    if (this.operands == null) {
      return Value.newUnknownValue(ValueType.TEXT);
    }
    int nbrOperands = this.operands.length;

    if (nbrOperands == 1) {
      return this.operands[0].getValue(data);
    }

    Value[] values = new Value[nbrOperands];
    this.takeSteps(values, data);
    return values[0];
  }

  /**
   * internally used to get arguments list for a function
   *
   * @param data
   * @return
   * @throws InvalidValueException
   */
  Value[] getValueList(IFieldsCollection data) throws InvalidOperationException {
    if (this.operands == null) {
      return new Value[0];
    }

    int nbrOperands = this.operands.length;
    Value[] values = new Value[nbrOperands];
    if (nbrOperands == 1) {
      values[0] = this.operands[0].getValue(data);
    } else {
      this.takeSteps(values, data);
    }
    int nbrArgs = this.nbrCommas + 1;
    /*
     * argument values are at the beginning.. simple case where each
     * argument was a single operand
     */
    if (nbrArgs == nbrOperands) {
      return values;
    }

    /*
     * there are nulls at the end.
     */
    Value[] valuesToreturn = new Value[nbrArgs];
    for (int i = 0; i < nbrArgs; i++) {
      valuesToreturn[i] = values[i];
    }
    return valuesToreturn;
  }

  /**
   * execute each step in this.steps
   *
   * @param values
   * @param data
   * @throws InvalidOperationException
   */
  private void takeSteps(Value[] values, IFieldsCollection data) throws InvalidOperationException {
    /*
     * steps involving commas (for functions) would be at the end. We comma
     * operators would be at the end. We will take them outside of the loop
     */
    int actualSteps = this.calculationSteps.length - this.nbrCommas;

    for (int i = 0; i < actualSteps; i++) {
      Step step = this.calculationSteps[i];
      /*
       * values[] contains null, unless a calculated value is placed in a
       * previous step.
       */
      Value leftValue = values[step.left];
      if (leftValue == null) {
        leftValue = this.operands[step.left].getValue(data);
      }
      Operand rightOperand = this.operands[step.right];
      Value rightValue = values[step.right];
      Value result = null;
      /*
       * special cases of or/and where we are not supposed to evaluate
       * right operand unless it is required. Instead of going in for a
       * generic design, we have hard coded the condition here
       */
      try {
        if (step.bop == BinaryOperator.And) {
          boolean b;
          b = leftValue.toBoolean();
          if (b) {
            if (rightValue == null) {
              rightValue = rightOperand.getValue(data);
            }
            b = rightValue.toBoolean();
          }
          result = Value.newBooleanValue(b);
        } else if (step.bop == BinaryOperator.Or) {
          boolean b = leftValue.toBoolean();
          if (!b) {
            if (rightValue == null) {
              rightValue = rightOperand.getValue(data);
            }
            b = rightValue.toBoolean();
          }
          result = Value.newBooleanValue(b);

        } else {
          if (rightValue == null) {
            rightValue = rightOperand.getValue(data);
          }
          result = step.bop.operate(leftValue, rightValue);
        }
      } catch (InvalidValueException e) {
        throw new InvalidOperationException(
            step.bop, leftValue.getValueType(), rightValue.getValueType());
      }
      values[step.left] = result;
    }

    /*
     * if there are no comma operators, then the result would be in
     * values[0]. And we are done.
     */
    if (this.nbrCommas == 0) {
      return;
    }

    /*
     * first argument is always at idx=0. idx of subsequent arguments in
     * value[] are available as step.right attribute.
     *
     * For example if it is a simple (a,b), then there is one step with
     * left=0, and right=1
     *
     * if it is (a+b, c) then left=0, right=2 for step[1]
     */

    /*
     * if the first argument was not an expression, let us copy the
     * argument-value to value[0];
     */
    if (values[0] == null) {
      values[0] = this.operands[0].getValue(data);
    }
    /*
     * each of the step gives us the idx of the next argument as its right.
     */
    int stepIdx = actualSteps;
    for (int i = 1; i <= this.nbrCommas; i++) {
      int idx = this.calculationSteps[stepIdx].right;
      Value value = values[idx];
      /*
       * value is null if it is just an operand, and not an expression.
       * Hence copy its operand-value
       */
      if (value == null) {
        value = this.operands[idx].getValue(data);
      }
      values[i] = value;
      stepIdx++;
    }
  }

  /** @return textual representation of parsed state for us to have an insight */
  public String toTrace() {

    StringBuilder sbf = new StringBuilder();
    int i = 1;
    int nbrSubExpr = 0;
    for (Operand operand : this.operands) {
      sbf.append(i).append(" : ").append(operand.uop == null ? "" : operand.uop);
      if (operand.value != null) {
        sbf.append(operand.value);
      }
      if (operand.expression != null) {
        nbrSubExpr++;
        sbf.append("(subExpr-").append(nbrSubExpr).append(")");
      }
      sbf.append("\n");
      i++;
    }
    sbf.append("   STEPS    \n");
    i = 1;
    for (Step step : this.calculationSteps) {
      sbf.append(i)
          .append(" : operand-")
          .append(step.left + 1)
          .append("  ")
          .append(step.bop)
          .append("  ")
          .append(step.right + 1)
          .append("\n");
      i++;
    }
    if (nbrSubExpr > 0) {
      sbf.append(" ----- sub expressions ----\n");
      i = 1;
      for (Operand operand : this.operands) {
        if (operand.expression != null) {
          sbf.append("subExpr-" + i).append('\n');
          i++;
          sbf.append(operand.expression.toTrace());
        }
      }
    }
    return sbf.toString();
  }

  /** inner worker class for parsing. */
  class ExpressionParser {
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    /** comma is OK if we are parsing as argument list */
    private boolean commaOk = false;
    /** what we have to parse */
    private char[] chars;
    /** just to avoid repeated chars.length() */
    private int nbrChars;
    /** we parse the chars into a list of operands. That is our job */
    List<Operand> operandList = new ArrayList<Operand>();

    List<BinaryOperator> bopList = new ArrayList<BinaryOperator>();
    /** current one that is being parsed into */
    Operand currentOperand = new Operand();
    /** current pointer to chars[] */
    int parsingAt = 0;
    /** we start with an operand */
    boolean operandExpected = true;

    /** max of precedence of any operator */
    int maxPrecede = 0;

    /** min precedence */
    int minPrecede = Integer.MAX_VALUE;

    /** convenience sub-class to keep state while parsing */
    ExpressionParser() {}

    void parse(char[] charsToParse, int startAt, boolean forFunction)
        throws InternalParseException {
      this.chars = charsToParse;
      this.nbrChars = charsToParse.length;
      this.commaOk = forFunction;

      char openingBracket = 0;
      if (startAt > 0) {
        /*
         * this is a sub-expression
         */
        openingBracket = this.chars[startAt - 1];
      }
      /*
       * loop is not for each character though we re incrementing char
       * pointer in this for loop. That is because the tokenizers alter
       * its value
       */
      for (this.parsingAt = startAt; this.parsingAt < this.nbrChars; this.parsingAt++) {
        char c = this.chars[this.parsingAt];

        if (this.isWhiteSpace(c)) {
          continue;
        }

        if (this.isCloseBracket(c)) {
          if (openingBracket == 0) {
            throw new InternalParseException(
                "close brace found with no matching open brace ", this.parsingAt);
          }
          if (openingBracket == Chars.OPEN_CURVE && c == Chars.CLOSE_CURVE
              || openingBracket == Chars.OPEN_SQUARE && c == Chars.CLOSE_SQUARE
              || openingBracket == Chars.OPEN_FLOWER && c == Chars.CLOSE_FLOWER) {
            /*
             * reset to 0 to indicate that we did find corresponding
             * one
             */
            openingBracket = 0;
            break;
          }

          throw new InternalParseException(
              startAt
                  + " "
                  + this.parsingAt
                  + " "
                  + "Opening brace "
                  + this.chars[startAt - 1]
                  + " found a matching, but different closing brace "
                  + c,
              this.parsingAt);
        }
        if (this.operandExpected) {
          this.parseForOperand(c);
        } else {
          this.parseForOperator(c);
        }
      }

      /*
       * was it an unexpected end?
       */
      if (openingBracket != 0) {
        throw new InternalParseException(
            "No closing brace found for the opening brace "
                + openingBracket
                + " that was at "
                + startAt,
            this.parsingAt);
      }

      if (this.operandExpected) {
        /*
         * unless we havn't parsed anything yet..
         */
        if (this.operandList.size() > 0 || this.currentOperand.uop != null) {
          throw new InternalParseException("expression ended on a wrong foot!! ", this.parsingAt);
        }
      }
      /*
       * last operand may not have been added
       */
      if (this.currentOperand.operandType != Operand.NONE) {
        this.operandList.add(this.currentOperand);
      }
      /*
       * is it empty? ok for function
       */
      if (this.operandList.size() == 0 && forFunction == false) {
        throw new InternalParseException("nothing found between braces", this.parsingAt);
      }
    }

    /**
     * based on what we have parsed, set the expression ready for repeated evaluation.
     *
     * <p>our job is to set operands and steps for our beloved parent!!
     */
    void setupShop() {
      if (this.operandList.size() > 0) {
        Expression.this.operands = this.operandList.toArray(new Operand[0]);
      }
      /*
       * no steps if there is only one operand
       */
      int nbrOperators = this.bopList.size();
      if (nbrOperators < 1) {
        return;
      }
      /*
       * there will be exactly one step less than number of operands
       */
      Step[] steps = new Step[nbrOperators];
      Expression.this.calculationSteps = steps;
      int nbrSteps = 0;
      /*
       * last operand does not have an operator, we iterate for left
       * operands only
       */
      for (int prec = this.minPrecede; prec <= this.maxPrecede; prec++) {
        /*
         * We have precedence from 1 to 5. We create sub-expression for
         * each bracket. We do not expect more than a hand-full
         * operands. As if that is not small enough, we have
         * minPrecedence and maxPrecedence. No further optimisation of
         * loops. With each precedence, we go over all operands, and
         * create steps for operators with matching precedences
         */
        int left = 0;
        for (int i = 0; i < nbrOperators; i++) {
          BinaryOperator bop = this.bopList.get(i);
          int thisPrec = bop.getPrecedence();
          if (thisPrec == prec) {
            /*
             * operand index is one more than operator index
             */
            steps[nbrSteps] = new Step(left, i + 1, bop);
            nbrSteps++;
          } else if (thisPrec > prec) {
            left = i + 1;
          }
        }
      }
    }

    /** we are just starting, or we have parsed an operand as the last token */
    private void parseForOperand(char c) throws InternalParseException {
      /*
       * let us hope that we will find an operand, and set this in
       * anticipation
       */
      this.operandExpected = false;

      if (this.isOpenBracket(c)) {
        this.currentOperand.operandType = Operand.EXPRESSION;
        this.currentOperand.expression = this.parseSubExpression(false);
      } else if (this.isAlpha(c)) {
        String fieldName = this.parseName();
        if (fieldName.equals(TRUE)) {
          this.currentOperand.value = Value.VALUE_TRUE;
          this.currentOperand.operandType = Operand.CONSTANT;
        } else if (fieldName.equals(FALSE)) {
          this.currentOperand.value = Value.VALUE_FALSE;
          this.currentOperand.operandType = Operand.CONSTANT;
        } else {
          this.currentOperand.value = Value.newTextValue(fieldName);
          /*
           * assume it to be field. Next loop may convert this to
           * function
           */
          this.currentOperand.operandType = Operand.FIELD;
        }
      } else if (this.isNumber(c)) {
        this.currentOperand.operandType = Operand.CONSTANT;
        this.currentOperand.value = this.parseNumber();
      } else if (this.isQuote(c)) {
        this.currentOperand.operandType = Operand.CONSTANT;
        this.currentOperand.value = this.parseLiteral();
      } else if (this.currentOperand.uop == null && this.isUnaryOperator(c)) {
        this.currentOperand.uop = UnaryOperator.getOperator(c);
        this.operandExpected = true;
        return;
      } else {
        /*
         * we got something unexpected
         */
        throw new InternalParseException(
            "Got " + c + " when an operand is expected.", this.parsingAt);
      }
      /*
       * look for invalid unary operators on constants
       */
      if (this.currentOperand.isValid() == false) {
        throw new InternalParseException(
            "This operand has an invalid unary opeator " + this.currentOperand.uop,
            this.parsingAt - 1);
      }
    }

    private Expression parseSubExpression(boolean forFunction) throws InternalParseException {
      /*
       * skip the open bracket
       */
      this.parsingAt++;
      Expression expr = new Expression(this.chars, this.parsingAt, forFunction);
      /*
       * pointer is to point to the last char parsed. point to the close
       * bracket that ended the sub-expression.
       */
      this.parsingAt += expr.expressionText.length();
      return expr;
    }

    private void parseForOperator(char c) throws InternalParseException {
      /*
       * we may get '(' if the last operand was a function
       */
      if (this.isOpenBracket(c)) {
        if (this.currentOperand.operandType == Operand.FIELD) {
          this.currentOperand.operandType = Operand.FUNCTION;
          /*
           * let us re- the operand after this change of type
           */
          if (!this.currentOperand.isValid()) {
            throw new InternalParseException(
                "This operand has an invalid unary opeator " + this.currentOperand.uop,
                this.parsingAt - 1);
          }
          Expression exp = this.parseSubExpression(true);
          if (exp.operands != null) {
            /*
             * expression should remain as null for function that is
             * invoked with no parameters
             */
            this.currentOperand.expression = exp;
          }
          /*
           * we will continue to expect an operator
           */
          return;
        }
        throw new InternalParseException(
            "Got " + c + " when an operator is expected.", this.parsingAt);
      }
      /*
       * that was the only exception. we do expect a binary operator now
       */
      if (this.isBinaryOperator(c) == false) {
        throw new InternalParseException(
            "Got " + c + " when an operator is expected.", this.parsingAt);
      }
      if (c == Chars.LIST) {
        if (this.commaOk) {
          Expression.this.nbrCommas++;
        } else {
          throw new InternalParseException(
              "Comma is valid only as parameter separator in an arguemnt list for function.",
              this.parsingAt);
        }
      }
      this.operandExpected = true;
      BinaryOperator bop = this.parseBinaryOperator(c);
      if (bop != null) {
        int prec = bop.getPrecedence();
        if (prec > this.maxPrecede) {
          this.maxPrecede = prec;
        }
        if (prec < this.minPrecede) {
          this.minPrecede = prec;
        }
      }
      this.operandList.add(this.currentOperand);
      this.bopList.add(bop);
      this.currentOperand = new Operand();
    }

    /**
     * literals start with ' or " for a text value. /yyyy-mm-dd/ is a date literal.
     *
     * @return
     * @throws InternalParseException
     */
    private Value parseLiteral() throws InternalParseException {
      char delimiter = this.chars[this.parsingAt];
      int foundAt = -1;
      this.parsingAt++;
      for (int i = this.parsingAt; i < this.nbrChars; i++) {
        if (this.chars[i] == delimiter) {
          foundAt = i;
          break;
        }
      }
      if (foundAt == -1) {
        throw new InternalParseException(
            "No matching delimiter " + delimiter + " found for literal.", this.nbrChars);
      }
      String token = new String(this.chars, this.parsingAt, foundAt - this.parsingAt);
      Value value = null;
      if (delimiter == Chars.DIVIDE) {
        Date date = DateUtil.parseDateWithOptionalTime(token);
        if (date == null) {
          throw new InternalParseException(
              token
                  + " is not a valid date. yyyy-mm-dd and yyyy-mm-ddThh:mm:ss.sssZ (UTC standard) are the only acceptable format for date.",
              this.parsingAt);
        }
        value = Value.newDateValue(date);
      } else {
        value = Value.newTextValue(token);
      }
      this.parsingAt = foundAt;
      return value;
    }

    private Value parseNumber() throws InternalParseException {
      boolean dotParsed = false;
      int startedAt = this.parsingAt;
      for (this.parsingAt++; this.parsingAt < this.nbrChars; this.parsingAt++) {
        char c = this.chars[this.parsingAt];
        if (this.isNumber(c)) {
          continue;
        }
        if (c == Chars.DOT && dotParsed == false) {
          dotParsed = true;
          continue;
        }
        break;
      }
      String token = new String(this.chars, startedAt, this.parsingAt - startedAt);
      /*
       * we have already pointing to the next char. step back.
       */
      this.parsingAt--;
      try {
        if (dotParsed) {
          return Value.newDecimalValue(Double.parseDouble(token));
        }
        return Value.newIntegerValue(Long.parseLong(token));
      } catch (NumberFormatException e) {
        throw new InternalParseException(token + " is not a valid number", this.parsingAt);
      }
    }

    private BinaryOperator parseBinaryOperator(char c) throws InternalParseException {

      /*
       * c is guaranteed to be a binary operator. let us check if it is a
       * two-char operator
       */
      int j = this.parsingAt + 1;
      if (j >= this.nbrChars) {
        throw new InternalParseException("Reached end when an operand is expected", this.nbrChars);
      }
      char c1 = this.chars[j];
      /*
       * c1 is our is it is = & |
       */
      if (c1 == Chars.EQUAL) {
        this.parsingAt++;
        if (c == Chars.LESS) {
          return BinaryOperator.getOperator(Chars.LESS_OR_EQUAL);
        }
        if (c == Chars.GREATER) {
          return BinaryOperator.getOperator(Chars.GREATER);
        }
        if (c == Chars.NOT) {
          return BinaryOperator.getOperator(Chars.NOT_EQUAL);
        }
        /*
         * we are tolerant to Java programmers who are used to put ==
         */
        if (c == Chars.EQUAL) {
          return BinaryOperator.getOperator(Chars.EQUAL);
        }
      } else if (c1 == Chars.AND) {
        if (c == Chars.AND) { // grant && to Java guys
          this.parsingAt++;
          return BinaryOperator.getOperator(Chars.AND);
        }
      } else if (c1 == Chars.OR) {
        if (c == Chars.OR) { // grant || to Java guys
          this.parsingAt++;
          return BinaryOperator.getOperator(Chars.OR);
        }
      } else if (c != Chars.NOT) { // ! should have c1 == EQUAL
        return BinaryOperator.getOperator(c);
      }
      throw new InternalParseException(
          c + Chars.EQUAL + " is not a valid binary operator", this.parsingAt + 1);
    }

    private String parseName() {
      int endAt = this.parsingAt + 1;
      while (endAt < this.chars.length && this.isAlphaNumeric(this.chars[endAt])) {
        endAt++;
      }
      /*
       * endAt is pointing one char next to our name
       */
      String parsedName = new String(this.chars, this.parsingAt, endAt - this.parsingAt);
      this.parsingAt = endAt - 1;
      return parsedName;
    }

    private boolean isWhiteSpace(char c) {
      return c == Chars.SPACE || c == Chars.TAB || c == Chars.NL || c == Chars.CR;
    }

    private boolean isBinaryOperator(char c) {
      return c == Chars.PLUS
          || c == Chars.MINUS
          || c == Chars.MULT
          || c == Chars.DIVIDE
          || c == Chars.MODULO
          || c == Chars.EQUAL
          || c == Chars.NOT
          || c == Chars.LESS
          || c == Chars.GREATER
          || c == Chars.AND
          || c == Chars.OR
          || c == Chars.LIST;
    }

    private boolean isUnaryOperator(char c) {
      return c == Chars.MINUS || c == Chars.NOT || c == Chars.IS_KNOWN || c == Chars.IS_UNKNOWN;
    }

    private boolean isOpenBracket(char c) {
      return c == Chars.OPEN_CURVE || c == Chars.OPEN_FLOWER || c == Chars.OPEN_SQUARE;
    }

    private boolean isCloseBracket(char c) {
      return c == Chars.CLOSE_CURVE || c == Chars.CLOSE_FLOWER || c == Chars.CLOSE_SQUARE;
    }

    private boolean isQuote(char c) {
      return c == Chars.SINGLE_QUOTE || c == Chars.DOUBLE_QUOTE || c == Chars.DIVIDE;
    }

    private boolean isAlpha(char c) {
      return (c >= Chars.LOWER_A && c <= Chars.LOWER_Z)
          || (c >= Chars.UPPER_A && c <= Chars.UPPER_Z)
          || c == Chars.UNDER_SCORE;
    }

    private boolean isNumber(char c) {
      return c >= Chars.ZERO && c <= Chars.NINE;
    }

    private boolean isAlphaNumeric(char c) {
      return this.isAlpha(c) || this.isNumber(c) || c == Chars.DOT;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return this.expressionText;
  }

  /** @param args */
  public static void main(String[] args) {
    String text = "/2015-12-12/ - /2016-12-22/";
    // String text =
    // "concat(\"a\" + \"A\", \"b\"+ \"B\", foo(123, faa(456 - abcd), 12),
    // 14)";
    // String text = "\"a\" , \"A\", \"b\", \"B\", 123 , 456)";
    try {

      logger.info("TRYING " + text);

      Expression expr = new Expression(text);
      DynamicSheet ds = new DynamicSheet();
      Value val = expr.evaluate(ds);

      logger.info("Expression : " + expr.toString() + " got evaluated to " + val);

    } catch (Exception e) {

      logger.error("unable to parse/execute expression : " + e.getMessage(), e);
    }
  }
}
