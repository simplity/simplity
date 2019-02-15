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
 * We use generic data structure for carrying value across. One approach is to use just Object as
 * the class for these. And use the standard wrapper class. Like Map&lt;String, Object&gt; as a
 * value list. However, this is not amenable to implementing some operations, like implementing
 * expressions etc.. hence we have designed a wrapper class called Value that wraps the common value
 * types used. Application software deals with business data. Hence we have designed integral(whole
 * number), decimal, textual, boolean and date as the five value types.
 *
 * <p>To keep things simple, we have textual representation of all these values, so that the values
 * can be transported across networks using text-based protocol, like HTTP. While numbers naturally
 * become digits, we have chosen "1" for true, "0" for false, and milliseconds-from-epoch for date.
 * We are fairly strict about this, because we expect that the API users are programmers who can do
 * with some good discipline :-)
 *
 * <p>Business data has been already "inflicted" with the concept of null. While the meaning of null
 * is unknown, there is wide-spread confusion amongst programmer community, as the name also clashes
 * with the programming paradigm. We support null as valid value for each value type. However, we
 * still have issue about text value of null. After lots and lots of discussion, experiment and
 * thought, we have decided to allow empty string as a valid value of text. But then, what is the
 * text value of null text? Good question. We have no answer for that. That means you can not
 * transmit null value of a text. you can set null to text value only through an API. If you
 * serialize a null text and deserialize it back, it will become a non-null text-value with empty
 * string as its value., we are fine with this as of now!
 *
 * <p>By the way, we use long as value of date, instead of Date object. With the introduction of
 * calendar we believe the mutable Date() is pretty-much avoidable.
 */
package org.simplity.core.value;
