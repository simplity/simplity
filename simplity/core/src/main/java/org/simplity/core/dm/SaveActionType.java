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
package org.simplity.core.dm;

import java.util.HashMap;
import java.util.Map;

/** @author simplity.org */
public enum SaveActionType {
  /** rows are added (create/new) */
  ADD
  /** update (modify) */
  ,
  MODIFY
  /** delete */
  ,
  DELETE
  /** if key is supplied, update it, else add it */
  ,
  SAVE;

  private static Map<String, SaveActionType> myTypes = new HashMap<String, SaveActionType>();

  static {
    SaveActionType.fillMap();
  }

  /**
   * @param action
   * @return parsed type, or null if it is not a valid action
   */
  public static SaveActionType parse(String action) {
    return SaveActionType.myTypes.get(action.toUpperCase());
  }

  private static void fillMap() {
    for (SaveActionType sat : SaveActionType.values()) {
      SaveActionType.myTypes.put(sat.name(), sat);
    }
  }
}
