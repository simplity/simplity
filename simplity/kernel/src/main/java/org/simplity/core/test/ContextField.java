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

package org.simplity.core.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.simplity.core.util.JsonUtil;
import org.simplity.json.JSONObject;

/** Specification for a field from output of a service test to be added to the test context */
public class ContextField {
	private static final Logger logger = LoggerFactory.getLogger(ContextField.class);

  /**
   * source of this field in the output JSON e.g. customerName or orders.lines[2].price. Special
   * case "." (just dot ) to select the entire JSON itself
   */
  String fieldSelector;
  /**
   * name under which this is to be added to the context. This is the name that the next service
   * uses to retrieve this value. defaults to fieldSelector
   */
  String nameInContext;

  /**
   * save/add this field value into context
   *
   * @param json
   * @param ctx
   */
  public void addToContext(JSONObject json, TestContext ctx) {
    Object value = JsonUtil.getValue(this.fieldSelector, json);
    if (value == null) {

      logger.info(
          "Value for " + this.fieldSelector + " is null and hence is not added to the context");

      return;
    }
    if (this.nameInContext == null) {
      ctx.setValue(this.fieldSelector, value);
    } else {
      ctx.setValue(this.nameInContext, value);
    }
  }
}
