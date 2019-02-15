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
/**
 * Implementation of an expression as used in the context of computing. Yes. we have re-invented the
 * wheel by implementing it here. But look at the wheel we have invented. We obviously think that it
 * is much better wheel :-)
 *
 * <p>Our objective is to compile an expression for repeated use of evaluating with different set of
 * values. This is a substitute to writing a set of java code at design time. That is, instead of
 * writing a set of java code at design time, you write an "expression" that gets "compiled" at run
 * time (but once) and then executes in a way similar to java code.
 *
 * <p>In our visualization, an expression is just one operand, or a sequence of n operands with n-1
 * binary operators connecting them.
 *
 * <p>Well, that is not the complete picture. What about unary operators? what about brackets? what
 * about functions?
 *
 * <p>unary operators are part of an operand. Our notion of "operand" includes a possible unary
 * operator operating it.
 *
 * <p>And, finally, brackets just change the natural precedence of the operator. To take care of
 * this, we also parse an 'execution sequence" that takes care of any change in precedence.
 *
 * <p>We have devised several classes, all of which are internal to us. Expression is the only class
 * that is exposed as public. That gives us flexibility to tweak the implementation as we get
 * feedback based on actual usage.
 *
 * <p>We have designed Expression to be completely thread safe, by making it immutable, and
 * state-less.
 *
 * @author simplity.org
 */
package org.simplity.core.expr;
