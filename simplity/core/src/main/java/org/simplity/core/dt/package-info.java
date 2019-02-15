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
 * A data-type is a design component. It captures the restrictions on the range of values a field
 * can have. In this package, we implement those aspects of data-type that are relevant for
 * delivering service from server side. That is, the classes defined here MAY NOT have all
 * attributes of data-type design component. They only have those that are required by the server
 * side. For example we do not deal with formatting.
 *
 * <p>It is important to understand the relationship between value-types and data-types. A data-type
 * has an underlying value-type. Data-type puts further restriction on the possible values.
 *
 * <p>For example an integerDataType with its maxValue set to 3578 implies: 1. that the value has to
 * be an integer. 2. Further, it can have a maximum value of 3578.
 *
 * @author simplity.org
 */
package org.simplity.core.dt;
