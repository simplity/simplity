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

/**
 * <p>
 * defines all interfaces for a db driver. this is a layer between application
 * and a jdbc driver.
 * <p>
 *
 * <p>
 * Whenever an interface has only one method, we keep debating whether it should
 * be a functional interface (lambda function) instead. Our view is that a
 * "function" is meant to return a value based on the input parameters. A
 * function is not meant to manipulate/mutate its parameters. In that sense, it
 * is nothing but an "expression" that evaluates to a value. We ended up having
 * no functions.
 * </p>
 *
 * <p>
 * Data base connections are expensive. One of the most important aspect of app
 * design is to ensure the the life-cycle of a connection is well managed. With
 * this in mind, we have created interface such that the life-cycle is
 * controlled by the driver implementations, rather than the consumers of these
 * API. We generally assume that the driver development is much better managed
 * than the application programming process.
 * </p>
 *
 * @author simplity.org
 *
 */
package org.simplity.kernel.idb;