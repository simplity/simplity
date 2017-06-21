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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.expr.Expression;

/**
 * @author simplity.org
 *
 */
public class ReflectUtil {

	/**
	 * set attribute value to a field, if the attribute is not already set
	 *
	 * @param object
	 * @param fieldName
	 * @param textValue
	 *            text value is parsed to the right type
	 * @param setOnlyIfFieldIsNull
	 *            set value only if the current value is null (empty, 0 or
	 *            false)
	 */
	public static void setAttribute(Object object, String fieldName, String textValue, boolean setOnlyIfFieldIsNull) {
		Field field = getField(object, fieldName);
		if (field == null) {
			return;
		}
		Class<?> fieldType = field.getType();
		field.setAccessible(true);
		try {
			if (setOnlyIfFieldIsNull) {
				if (isSpecified(field.get(object))) {
					Tracer.trace(fieldName + " already has a value of " + field.get(object) + " and hence a value of "
							+ textValue + " is not set");
					return;
				}
			}
			Object fieldValue = TextUtil.parse(textValue, fieldType);
			if (fieldValue != null) {
				Tracer.trace(fieldName + " is set a value of " + textValue + " from data field");
				field.set(object, fieldValue);
			}
		} catch (Exception e) {
			throw new ApplicationError(e, "Error while assigning a value of " + textValue + " to field " + fieldName
					+ " of " + object.getClass().getSimpleName() + ". ");
		}
	}

	/**
	 * set attribute value to a field, if the attribute is not already set
	 *
	 * @param object
	 * @param fieldName
	 * @param textValue
	 *            text value is parsed to the right type
	 * @param setOnlyIfFieldIsNull
	 *            set value only if the current value is null (empty, 0 or
	 *            false)
	 * @param considerModifiers
	 */
	public static void setAttribute(Object object, String fieldName, String textValue, boolean setOnlyIfFieldIsNull,
			boolean considerModifiers) {
		Field field = getField(object, fieldName, true);
		if (field == null) {
			return;
		}
		Class<?> fieldType = field.getType();
		field.setAccessible(true);
		try {
			if (setOnlyIfFieldIsNull) {
				if (isSpecified(field.get(object))) {
					Tracer.trace(fieldName + " already has a value of " + field.get(object) + " and hence a value of "
							+ textValue + " is not set");
					return;
				}
			}
			Object fieldValue = TextUtil.parse(textValue, fieldType);
			if (fieldValue != null) {
				Tracer.trace(fieldName + " is set a value of " + textValue + " from data field");
				field.set(object, fieldValue);
			}
		} catch (Exception e) {
			throw new ApplicationError(e, "Error while assigning a value of " + textValue + " to field " + fieldName
					+ " of " + object.getClass().getSimpleName() + ". ");
		}
	}

	/**
	 * return all fields declared as default for this object instance, including
	 * inherited ones.
	 *
	 * @param object
	 * @return default scoped fields for this class
	 */
	public static Map<String, Field> getAllFields(Object object) {
		Class<?> type = object.getClass();
		Map<String, Field> fields = new HashMap<String, Field>();
		while (type.equals(Object.class) == false) {
			for (Field f : type.getDeclaredFields()) {
				int mod = f.getModifiers();
				if (mod == 0 || Modifier.isProtected(mod)) {
					fields.put(f.getName(), f);
				}
			}
			type = type.getSuperclass();
		}
		return fields;
	}

	/**
	 * return all fields of this object instance, including
	 * inherited ones.
	 *
	 * @param object
	 * @param considerModifiers
	 * @return all fields for this class
	 */
	public static Map<String, Field> getAllFields(Object object, boolean considerModifiers) {
		Class<?> type = object.getClass();
		Map<String, Field> fields = new HashMap<String, Field>();
		while (type.equals(Object.class) == false) {
			for (Field f : type.getDeclaredFields()) {
				fields.put(f.getName(), f);
			}
			type = type.getSuperclass();
		}
		return fields;
	}

	/**
	 * get a field from an object's class or any of its super class. We deal
	 * with fields that have no modifiers (default/package-private)
	 *
	 * @param object
	 * @param fieldName
	 * @return field or null if no such field
	 */
	private static Field getField(Object object, String fieldName) {
		Class<?> cls = object.getClass();
		while (cls.equals(Object.class) == false) {
			try {
				Field field = cls.getDeclaredField(fieldName);
				if (field != null) {
					if (field.getModifiers() == 0) {
						return field;
					}
					Tracer.trace(fieldName + " is a field, but it is has modifiers, and hence not selected.");
					return null;
				}
			} catch (SecurityException e) {
				Tracer.trace("Thrown out by a Bouncer while looking at field " + fieldName + ". " + e.getMessage());
				return null;
			} catch (NoSuchFieldException e) {
				// keep going...
			}
			cls = cls.getSuperclass();
		}
		return null;
	}

	/**
	 * get a field from an object's class or any of its super class. We deal
	 * with fields that have no modifiers (default/package-private)
	 *
	 * @param object
	 * @param fieldName
	 * @param considerModifiers
	 * @return field or null if no such field
	 */
	private static Field getField(Object object, String fieldName, boolean considerModifiers) {
		Class<?> cls = object.getClass();
		while (cls.equals(Object.class) == false) {
			try {
				Field field = cls.getDeclaredField(fieldName);
				if (field != null) {
					return field;
				}
			} catch (SecurityException e) {
				Tracer.trace("Thrown out by a Bouncer while looking at field " + fieldName + ". " + e.getMessage());
				return null;
			} catch (NoSuchFieldException e) {
				// keep going...
			}
			cls = cls.getSuperclass();
		}
		return null;
	}

	/**
	 * we treat all value types as primitive
	 *
	 * @param type
	 * @return true if it is primitive by our definition
	 */
	public static boolean isValueType(Class<?> type) {
		if (type.isPrimitive() || type.isEnum() || type.equals(String.class) || type.equals(Expression.class)
				|| type.equals(Date.class) || type.equals(Pattern.class)) {
			return true;
		}
		return false;
	}

	/**
	 * set value to the field of the object
	 *
	 * @param object
	 *            that the field belongs to
	 * @param field
	 *            to which value is to be assigned to
	 * @param value
	 *            to be parsed and assigned to field
	 * @throws XmlParseException
	 */
	public static void setPrimitive(Object object, Field field, String value) throws XmlParseException {
		Class<?> fieldType = field.getType();
		Object valueObect = TextUtil.parse(value, fieldType);
		try {
			field.setAccessible(true);
			field.set(object, valueObect);
			return;
		} catch (Exception e) {
			throw new XmlParseException(
					"A value of |" + value + "| could not be parsed and set to field " + field.getName());
		}
	}

	/**
	 * is this value considered to be specified by our definition? null, 0, ""
	 * and false are considered to be not-specified.
	 *
	 * @param value
	 * @return true if value is non-null, non-empty, non-zero or true
	 */
	public static boolean isSpecified(Object value) {
		if (value == null) {
			return false;
		}
		if (value instanceof String) {
			return true;
		}
		if (value instanceof Boolean) {
			return ((Boolean) value).booleanValue();
		}
		if (value instanceof Integer) {
			return ((Integer) value).intValue() != 0;
		}
		return true;
	}

	/**
	 * copy compatible attributes, but only the attributes that are not
	 * specified in fromObject, but are specified in toObject.
	 *
	 * @param fromObject
	 * @param toObject
	 */
	public static void overrideAttributes(Object fromObject, Object toObject) {

		Map<String, Field> toAttributes = ReflectUtil.getAllFields(toObject);
		for (Field fromAttribute : getAllFields(fromObject).values()) {
			String attName = fromAttribute.getName();
			Field toAttribute = toAttributes.get(attName);
			if (toAttribute == null) {
				continue;
			}
			if (fromAttribute.getType().equals(toAttribute.getType()) == false) {
				Tracer.trace(
						attName + " is a common attribute but it is of type " + fromAttribute.getType().getSimpleName()
								+ " in from object but of type " + toAttribute.getType().getSimpleName()
								+ " in the toObject. Hence the attribute is not copied.");
				continue;
			}
			try {
				fromAttribute.setAccessible(true);
				toAttribute.setAccessible(true);
				Object fromValue = fromAttribute.get(fromObject);
				Object toValue = toAttribute.get(toObject);
				if (isSpecified(toValue) || !isSpecified(fromValue)) {
					continue;
				}
				Tracer.trace(fromAttribute.getName() + " copied as " + fromValue);
				toAttribute.set(toObject, fromValue);

			} catch (Exception e) {
				String msg = "Unable to copy attribute " + attName + " from object of type "
						+ fromObject.getClass().getSimpleName() + " to an object of type "
						+ toObject.getClass().getSimpleName() + ". " + e.getMessage();
				throw new ApplicationError(e, msg);
			}
		}
	}
}
