/*
 * Copyright (c) 2017 simplity.org
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

package org.simplity.test;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.InputStream;

import org.simplity.json.JSONWriter;
import org.simplity.kernel.Application;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.AttachmentAssistant;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.test.TestContext;
import org.simplity.test.TestRun;

/**
 * stub to test attachment handling. Prefixes saved_ before token to simulate storage, and expected
 * the same prefix for retrieval
 *
 * @author simplity.org
 */
public class AttachmentAssistantStub implements AttachmentAssistant {
  static final Logger logger = Logger.getLogger(AttachmentAssistantStub.class.getName());

  private static final String PREFIX = "saved_";
  private static final String TOKEN = PREFIX + "token";
  private static final int START = PREFIX.length();
  /*
   * (non-Javadoc)
   *
   * @see org.simplity.kernel.AttachmentAssistant#remove(java.lang.String)
   */
  @Override
  public void remove(String tokan) {
    // what to remove when we never kept anything !!!
  }

  /*
   * (non-Javadoc)
   *
   * @see org.simplity.kernel.AttachmentAssistant#retrieve(java.lang.String)
   */
  @Override
  /**
   * we expect that the token starts with "saved_" and we return the the token after removing this
   * prefix. Raise ApplicationError if this prefix is not found
   */
  public String retrieve(String token) {
    if (token == null || token.startsWith(PREFIX) == false) {
      throw new ApplicationError("No attachment found with token " + token);
    }
    return token.substring(START);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.simplity.kernel.AttachmentAssistant#store(java.io.InputStream)
   */
  @Override
  /** we return a token named saved_token always */
  public String store(InputStream arg0) {
    return TOKEN;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.simplity.kernel.AttachmentAssistant#store(java.lang.String)
   */
  @Override
  /** we return a token named saved_token always */
  public String store(String arg0) {
    return TOKEN;
  }

  public static void main(String[] args) {
    String root = "c:/repos/simplity/test/WebContent/WEB-INF/comp/";
    try {
      Application.bootStrap(root);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    TestContext ctx = new TestContext();
    ctx.start("111", null);
    TestRun tr = ComponentManager.getTestRun("action.httpClient");
    tr.run(ctx);
    JSONWriter writer = new JSONWriter();
    writer.object();
    writer.key("report");
    JsonUtil.addObject(writer, ctx.getReport());
    writer.endObject();

    logger.log(Level.INFO, writer.toString());
    Tracer.trace(writer.toString());
  }
}
