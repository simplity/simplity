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
package org.simplity.kernel.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.xerces.impl.dv.util.Base64;
import org.simplity.kernel.ApplicationError;

import org.simplity.kernel.data.IFieldsCollection;
import org.simplity.kernel.expr.Expression;
import org.simplity.kernel.expr.InvalidExpressionException;
import org.simplity.kernel.service.ServiceContext;
import org.simplity.kernel.value.Value;

/**
 * utility methods relating to text handling and manipulation
 *
 * @author simplity.org
 */
public class TextUtil {
	private static final Logger logger = LoggerFactory.getLogger(TextUtil.class);

  private static final Pattern FIELD = Pattern.compile("[$][{][\\w]*[}]");
  private static final String[] TRUE_VALUES = {"1", "TRUE", "YES"};
  private static final String ARRAY_DELIMITER = ",";
  private static final String ROW_DLIMITER = ";";
  private static final String DOLLAR_STR = "$";
  private static final char DOLLAR = '$';
  private static final char LOWER_A = 'a';
  private static final char LOWER_Z = 'z';
  private static final char A = 'A';
  private static final char Z = 'Z';
  private static final char UNDERSCORE = '_';
  private static final String UNDER_STR = "_";
  private static final int TO_LOWER = LOWER_A - A;
  private static final char DELIMITER = '.';
  private static final String UTF8 = "UTF-8";

  /**
   * convert a name that follows variableName notation to CONSTANT_NAME notation
   *
   * @param variable
   * @return converted name
   */
  public static String valueToConstant(String variable) {
    StringBuilder result = new StringBuilder();
    char[] chars = variable.toCharArray();
    for (char ch : chars) {
      if (ch <= Z && ch >= A) {
        result.append(UNDERSCORE);
      } else if (ch <= LOWER_Z && ch >= LOWER_A) {
        ch = (char) (ch - TO_LOWER);
      }
      result.append(ch);
    }
    return result.toString();
  }

  /**
   * convert a name that follows CONSTANT_NAME notation to variableName notation
   *
   * @param constant
   * @return converted name
   */
  public static String constantToValue(String constant) {
    /*
     * 90 % of our enums are single words
     */
    String result = constant.toLowerCase();
    if (constant.indexOf(UNDERSCORE) == -1) {
      return result;
    }
    String[] parts = constant.split("_");
    /*
     * We do not have any enum with more than two words as of now, hence we
     * do not use string builder
     */
    StringBuilder buffer = new StringBuilder(parts[0].toLowerCase());
    for (int i = 1; i < parts.length; i++) {
      String part = parts[i];
      buffer.append(part.charAt(0)).append(part.substring(1).toLowerCase());
    }
    return buffer.toString();
  }

  /**
   * simple utility to get the last part of the name
   *
   * @param qualifiedName
   * @return simple name
   */
  public static String getSimpleName(String qualifiedName) {
    int n = qualifiedName.lastIndexOf(DELIMITER);
    if (n == -1) {
      return qualifiedName;
    }
    return qualifiedName.substring(n + 1);
  }

  /**
   * parse into a primitive object
   *
   * @param text
   * @param type
   * @return parse object, or null if it could not be parsed
   * @throws XmlParseException if any issue with parsing the text into appropriate type
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static Object parse(String text, Class type) throws XmlParseException {
    String value = text.trim();
    if (type.equals(String.class)) {
      return value;
    }

    if (type.isPrimitive()) {
      if (type.equals(int.class)) {
        return new Integer(value);
      }

      if (type.equals(long.class)) {
        return new Long(value);
      }

      if (type.equals(short.class)) {
        return new Short(value);
      }

      if (type.equals(byte.class)) {
        return new Byte(value);
      }

      if (type.equals(char.class)) {
        if (value.length() == 0) {
          return new Integer(' ');
        }
        return new Integer(value.toCharArray()[0]);
      }

      if (type.equals(boolean.class)) {
        return Boolean.valueOf(parseBoolean(value));
      }

      if (type.equals(float.class)) {
        return new Float(value);
      }

      if (type.equals(double.class)) {
        return new Double(value);
      }
    } else if (type.isEnum()) {
      return Enum.valueOf(type, TextUtil.valueToConstant(value));
    } else if (type.isArray()) {
      Class<?> eleType = type.getComponentType();
      if (ReflectUtil.isValueType(eleType)) {
        return parseArray(eleType, value);
      } else if (eleType.isArray() && ReflectUtil.isValueType(eleType.getComponentType())) {
        /*
         * 2-d array of values?
         */
        return parse2dArray(eleType, value);
      }
    } else if (type.equals(Expression.class)) {
      try {
        return new Expression(value);
      } catch (InvalidExpressionException e) {
        throw new XmlParseException(e.getMessage());
      }
    } else if (type.equals(Date.class)) {
      Date date = DateUtil.parseDate(value);
      if (date == null) {
        throw new XmlParseException(value + " is not in yyyy-mm-dd format");
      }
      return date;
    } else if (type.equals(Pattern.class)) {
      return Pattern.compile(value);
    } else if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
      try {
        return Class.forName(value).newInstance();
      } catch (Exception e) {

        logger.info(
            value
                + " is expected to be an implementation/extension of class "
                + type.getName()
                + " but there was an error when it is used for create an instance."
                + e.getMessage());

        return null;
      }
    }

    return null;
  }

  /**
   * parse a comma separated string into an array
   *
   * @param type of the elements of the array
   * @param value to be parsed
   * @return array that is parsed from text value
   * @throws XmlParseException for any issue while parsing the text into an array of this type
   */
  public static Object parseArray(Class<?> type, String value) throws XmlParseException {
    String[] parts = value.split(ARRAY_DELIMITER);
    int nbr = parts.length;
    Object array = Array.newInstance(type, nbr);
    for (int i = 0; i < parts.length; i++) {
      Object thisObject = parse(parts[i].trim(), type);
      Array.set(array, i, thisObject);
    }
    return array;
  }

  /**
   * parse an array of arrays: rows separated by ; and columns separated by ,
   *
   * @param type of the elements of the main array. This would be an Array[]. That is this
   *     class.getComponentType() would be a primitive.
   * @param text to be parsed
   * @return array[][] that is parsed from text value
   * @throws XmlParseException for any issue while parsing the text
   */
  public static Object parse2dArray(Class<?> type, String text) throws XmlParseException {
    String[] parts = text.split(ROW_DLIMITER);
    int nbr = parts.length;
    Object array = Array.newInstance(type, nbr);
    Class<?> valueType = type.getComponentType();
    for (int i = 0; i < parts.length; i++) {
      String[] subParts = parts[i].trim().split(ARRAY_DELIMITER);
      int nbrSub = subParts.length;
      Object subArray = Array.newInstance(valueType, nbrSub);
      {
        for (int j = 0; j < subParts.length; j++) {
          Object val = parse(subParts[j].trim(), valueType);
          Array.set(subArray, j, val);
        }
      }
      Array.set(array, i, subArray);
    }
    return array;
  }

  /**
   * parse a text into boolean
   *
   * @param value
   * @return
   */
  private static boolean parseBoolean(String value) {
    String val = value.toUpperCase();
    for (String trueVal : TRUE_VALUES) {
      if (trueVal.equals(val)) {
        return true;
      }
    }
    return false;
  }

  /**
   * get the field name for class name
   *
   * @param className
   * @return field name for the class name
   */
  public static String classNameToName(String className) {
    String result = className;
    char c = result.charAt(0);
    if (c >= A && c <= Z) {
      c = (char) (c + TO_LOWER);
      result = c + result.substring(1);
    }
    return result;
  }

  /**
   * @param name
   * @return class name
   */
  public static String nameToClassName(String name) {
    String result = name;
    char c = result.charAt(0);
    if (c >= LOWER_A && c <= LOWER_Z) {
      c = (char) (c - TO_LOWER);
      result = c + result.substring(1);
    }
    return result;
  }

  /**
   * camelCase is converted into constant like myName to MY_NAME
   *
   * @param name
   * @return name suitable as constant
   */
  public static String toUnderscore(String name) {
    StringBuilder sbf = new StringBuilder();
    for (char c : name.toCharArray()) {
      if (c >= A && c <= Z) {
        c = (char) (c + TO_LOWER);
        sbf.append(UNDERSCORE);
      }
      sbf.append(c);
    }
    return sbf.toString();
  }

  /**
   * convert constants to camel case. like MY_NAME to myName
   *
   * @param name
   * @return camelCased name
   */
  public static String undoUnderscore(String name) {
    String[] parts = name.split(UNDER_STR);
    if (parts.length == 0) {
      return name;
    }
    StringBuilder result = new StringBuilder(parts[0]);
    for (int i = 1; i < parts.length; i++) {
      result.append(nameToClassName(parts[i]));
    }
    return result.toString();
  }

  /**
   * check for name of type $fieldName. if this is true, return fieldName, else return null.
   *
   * @param name
   * @return field name, if name follows convention for field name inside constants
   */
  public static String getFieldName(String name) {
    /*
     * even "$" is null
     */
    if (name == null || name.length() <= 1) {
      return null;
    }
    if (name.charAt(0) == DOLLAR) {
      char c = name.charAt(1);
      if ((c >= LOWER_A && c <= LOWER_Z) || (c >= A && c <= Z) || c == UNDERSCORE) {
        return name.substring(1);
      }
    }
    return null;
  }
  /**
   * check for value of $fieldName. , else return null.
   *
   * @param ctx
   * @param name
   * @return field value either as-is or from context
   */
  public static Value getFieldValue(ServiceContext ctx, String name) {
    /*
     * even "$" is null
     */
    String parsedName = getFieldName(name);
    if (parsedName != null) {
      return ctx.getValue(parsedName);
    }
    return Value.newTextValue(name);
  }
  /**
   * parse into parts where [0] is the text before first variable, [1] is the first fieldName, [2]
   * is next constant between first and second field name etc.. [last] is the constant after the
   * last field name. This array can be used used to optimize substitution of values at run time
   *
   * @param textWithFieldNames text that may contain one or more ${...} in it.
   * @return null if the text has no field names in it. array, with a minimum of three elements. You
   *     may supply this as argument to substituteParts() constants
   */
  public static String[] parseToParts(String textWithFieldNames) {
    /*
     * we expect the caller to have optimized or case where there is no $.
     */
    Matcher matcher = FIELD.matcher(textWithFieldNames);
    java.util.List<String> parts = new ArrayList<String>();
    /*
     * our constant starts here, and ends before the first match character
     */
    int idx = 0;
    while (true) {
      if (matcher.find() == false) {
        parts.add(textWithFieldNames.substring(idx));
        break;
      }

      int fieldStart = matcher.start();
      parts.add(textWithFieldNames.substring(idx, fieldStart));
      idx = matcher.end();
      parts.add(textWithFieldNames.substring(fieldStart + 2, idx - 1));
    }
    if (parts.size() <= 1) {
      return null;
    }
    return parts.toArray(new String[0]);
  }

  /**
   * return a text value that is formed by substituting ${...} field names in the input with their
   * values from the collection
   *
   * @param textWithFieldNames
   * @param fieldValues
   * @return resultant text
   */
  public static String substituteFields(String textWithFieldNames, IFieldsCollection fieldValues) {
    /*
     * if there is no $?
     */
    int idx = textWithFieldNames.indexOf(DOLLAR);
    if (idx == -1) {
      return textWithFieldNames;
    }

    Matcher matcher = FIELD.matcher(textWithFieldNames);
    /*
     * our constant starts here, and ends before the first match character
     */
    StringBuilder sbf = new StringBuilder();
    idx = 0;
    while (true) {
      if (matcher.find() == false) {
        sbf.append(textWithFieldNames.substring(idx));
        return sbf.toString();
      }

      int fieldStart = matcher.start();
      sbf.append(textWithFieldNames.substring(idx, fieldStart));
      idx = matcher.end();
      String fieldName = textWithFieldNames.substring(fieldStart + 2, idx - 1);
      Value value = fieldValues.getValue(fieldName);
      if (Value.isNull(value) == false) {
        sbf.append(value.toString());
      }
    }
  }

  /**
   * return a text value that is formed by substituting fields in the parts with their values from
   * the collection
   *
   * @param textParts that was returned by a call to parseToParts
   * @param fieldValues
   * @return resultant text
   */
  public static String substituteFields(String[] textParts, IFieldsCollection fieldValues) {
    StringBuilder sbf = new StringBuilder(textParts[0]);
    int done = textParts.length;
    int idx = 1;
    while (idx < done) {
      Value value = fieldValues.getValue(textParts[idx]);
      if (Value.isNull(value) == false) {
        sbf.append(value.toString());
      }
      idx++;
      sbf.append(textParts[idx]);
      idx++;
    }
    return sbf.toString();
  }

  /**
   * get a filename filter based on the provided regex
   *
   * @param pattern
   * @return a fileNameFIlter that can be used to filter file names from a folder
   */
  public static FilenameFilter getFileNameFilter(String pattern) {
    return new FileFilterWorker(pattern);
  }

  /**
   * if the pattern is $fieldName, then the value of field name from fields collection is returned
   *
   * <p>replace place holders in name pattern. {name} is replaced with the file name while {ext} is
   * replaced with file extension. Note that extension does not include '.' example
   *
   * <pre>
   * with inName = a.txt
   * {name}.out -> a.out
   * {name}.{ext}.out -> a.txt.out
   * b{ext} ->bout
   *
   * with inName = a
   * {name}.out -> a.out
   * {name}.{ext}.out -> a..out
   * b{ext} ->b
   * </pre>
   *
   * @param filePattern can not be null
   * @param inName can be null;
   * @param fields
   * @return file name after replacing place-holders, if any
   */
  public static String getFileName(String filePattern, String inName, IFieldsCollection fields) {
    String result;
    if (filePattern.startsWith(DOLLAR_STR)) {
      Value val = null;
      if (fields != null) {
        val = fields.getValue(filePattern.substring(1));
      }
      if (val == null || val.isUnknown()) {
        throw new ApplicationError(
            "Field "
                + filePattern.substring(1)
                + " not found in context. This is required as the name of a file.");
      }
      return val.toString();
    }

    int idx = filePattern.indexOf('{');
    if (idx == -1) {
      return filePattern;
    }

    String inFile = "";
    String inExtn = "";
    if (inName != null) {
      inFile = inName;
      idx = inName.lastIndexOf('.');
      if (idx != -1) {
        inFile = inName.substring(0, idx);
        inExtn = inName.substring(idx + 1);
      }
    }
    result = filePattern.replaceAll("\\{name\\}", inFile);
    result = result.replaceAll("\\{ext\\}", inExtn);
    return result;
  }

  /**
   * @param string
   * @return Base64 encrypted string
   */
  public static String encrypt(String string) {
    try {
      return Base64.encode(string.getBytes(UTF8));
    } catch (UnsupportedEncodingException ignore) {
      // we do know that it is supported
      return null;
    }
  }

  /**
   * @param string
   * @return decrypted string
   */
  public static String decrypt(String string) {
    try {
      return new String(Base64.decode(string), UTF8);
    } catch (UnsupportedEncodingException ignore) {
      // we do know that it is supported
      return null;
    }
  }
}

/** @author simplity.org */
class FileFilterWorker implements FilenameFilter {
  private final Pattern pattern;

  FileFilterWorker(String filePattern) {
    this.pattern = Pattern.compile(filePattern);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
   */
  @Override
  public boolean accept(File dir, String fileName) {
    return this.pattern.matcher(fileName).matches();
  }
}
