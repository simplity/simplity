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
package org.simplity.core.util;

import org.simplity.core.value.Value;
import org.simplity.core.value.ValueType;

/**
 * utilities to work with Arrays
 *
 * @author simplity.org
 */
public class ArrayUtil {

  /**
   * extend a string array
   *
   * @param array existing array
   * @param newValue value to be added to existing array
   * @return extended array
   */
  public static String[] extend(String[] array, String newValue) {
    String[] newArray = new String[array.length + 1];
    int i = 0;
    for (String s : array) {
      newArray[i++] = s;
    }
    newArray[i] = newValue;
    return newArray;
  }

  /**
   * extend a valueType array
   *
   * @param array existing array
   * @param newValue value to be added to existing array
   * @return extended array
   */
  public static ValueType[] extend(ValueType[] array, ValueType newValue) {
    ValueType[] newArray = new ValueType[array.length + 1];
    int i = 0;
    for (ValueType s : array) {
      newArray[i++] = s;
    }
    newArray[i] = newValue;
    return newArray;
  }

  /**
   * extend a Value array
   *
   * @param array existing array
   * @param newValue value to be added to existing array
   * @return extended array
   */
  public static Value[] extend(Value[] array, Value newValue) {
    Value[] newArray = new Value[array.length + 1];
    int i = 0;
    for (Value s : array) {
      newArray[i++] = s;
    }
    newArray[i] = newValue;
    return newArray;
  }

  /**
   * @param row
   * @param sbf
   */
  public static void ArrayToString(Object[] row, StringBuilder sbf) {
    for (Object obj : row) {
      sbf.append(obj).append(',');
    }
    sbf.setCharAt(sbf.length() - 1, '\n');
  }

  /**
   * for the names in forNames array, get the array index from inNames with a matching name. index
   * is -1 in case no matching name is found
   *
   * @param forNames names that you are interested in copying from the other
   * @param inNames source of names
   * @return index of the second array for matching name, -1 if no match
   */
  public static int[] getIndexMap(String[] forNames, String[] inNames) {
    int[] map = new int[forNames.length];
    for (int i = 0; i < forNames.length; i++) {
      int idx = -1;
      String forName = forNames[i];
      for (int j = 0; j < inNames.length; j++) {
        if (forName.equals(inNames[j])) {
          idx = j;
          break;
        }
      }
      map[i] = idx;
    }
    return map;
  }
}
