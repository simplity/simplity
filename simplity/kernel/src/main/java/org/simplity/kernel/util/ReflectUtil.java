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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.expr.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author simplity.org */
public class ReflectUtil {
	private static final Logger logger = LoggerFactory.getLogger(ReflectUtil.class);

	/**
	 * set attribute value to a field, if the attribute is not already set
	 *
	 * @param object
	 * @param fieldName
	 * @param fieldValue
	 *            text value is parsed to the right type
	 * @param setOnlyIfFieldIsNull
	 *            set value only if the current value is null (empty, 0 or
	 *            false)
	 */
	public static void setAttribute(Object object, String fieldName, Object fieldValue, boolean setOnlyIfFieldIsNull) {
		Field field = getField(object, fieldName);
		if (field == null) {
			return;
		}
		field.setAccessible(true);
		try {
			if (setOnlyIfFieldIsNull && isSpecified(field.get(object))) {
				logger.info(fieldName + " already has a value of " + field.get(object) + " and hence a value of "
						+ fieldValue + " is not set");
				return;
			}
			field.set(object, fieldValue);
		} catch (Exception e) {
			throw new ApplicationError(e, "Error while assigning a value of " + fieldValue + " to field " + fieldName
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
		return getAllFields(object, false);
	}

	/**
	 * return all fields of this object instance, including inherited ones.
	 *
	 * @param object
	 * @param getPrivateAsWell
	 * @return all fields for this class
	 */
	public static Map<String, Field> getAllFields(Object object, boolean getPrivateAsWell) {
		Class<?> type = object.getClass();
		Map<String, Field> fields = new HashMap<String, Field>();
		while (type.equals(Object.class) == false) {
			for (Field f : type.getDeclaredFields()) {
				int mod = f.getModifiers();
				if (Modifier.isStatic(mod) || Modifier.isTransient(mod) || Modifier.isVolatile(mod)) {
					continue;
				}
				if (!getPrivateAsWell && Modifier.isPrivate(mod)) {
					continue;
				}
				fields.put(f.getName(), f);
			}
			type = type.getSuperclass();
		}
		return fields;
	}

	/**
	 * get a field from an object's class or any of its super class.
	 *
	 * @param object
	 * @param fieldName
	 * @return field or null if no such field
	 */
	public static Field getField(Object object, String fieldName) {
		Class<?> cls = object.getClass();
		while (cls.equals(Object.class) == false) {
			try {
				Field field = cls.getDeclaredField(fieldName);
				if (field != null) {
					return field;
				}
			} catch (SecurityException e) {
				logger.info("Thrown out by a Bouncer while looking at field " + fieldName + ". " + e.getMessage());
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
	 * we treat all value types as primitive
	 *
	 * @param value
	 * @return true if it is primitive by our definition
	 */
	public static boolean isPrimitiveValue(Object value) {
		if (value == null) {
			return true;
		}
		return isValueType(value.getClass());
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

				logger.info(
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

				logger.info(fromAttribute.getName() + " copied as " + fromValue);

				toAttribute.set(toObject, fromValue);

			} catch (Exception e) {
				String msg = "Unable to copy attribute " + attName + " from object of type "
						+ fromObject.getClass().getSimpleName() + " to an object of type "
						+ toObject.getClass().getSimpleName() + ". " + e.getMessage();
				throw new ApplicationError(e, msg);
			}
		}
	}

	/**
	 *
	 * @param object
	 * @param fieldName
	 *            simple field name, can not contain '.'
	 * @return value of this field in the object. null if the value is null, or
	 *         if the field does not exist
	 */
	public static Object getValue(Object object, String fieldName) {
		Field field = getField(object, fieldName);
		if (field == null) {
			return null;
		}
		try {
			field.setAccessible(true);
			return field.get(object);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * get the value of a primitive member, possibly down the hierarchy of field
	 * members
	 *
	 * @param root
	 *            root object
	 * @param path
	 *            possibly of the form a.b.c...
	 * @return primitive value, or null if the value is null, or the path could
	 *         not be traversed. If any of the child is an array or list, its
	 *         first member is used as the child
	 */
	public static Object getChildPrimitive(Object root, String path) {
		Object obj = getChildValue(root, path);
		if (obj == null) {
			return null;
		}
		if (isPrimitiveValue(obj)) {
			return obj;
		}
		return null;
	}

	/**
	 * set the value of a primitive member, possibly down the hierarchy of field
	 * members
	 *
	 * @param root
	 *            root object
	 * @param path
	 *            possibly of the form a.b.c...
	 * @param fieldValue
	 *            text value that is suitable to be parsed into the field
	 * @return true if all ok
	 */
	public static boolean setChildPrimitive(Object root, String path, String fieldValue) {
		FieldAndObj fa = getLeaf(root, path);
		if (fa == null) {
			return false;
		}
		try {
			Object value = TextUtil.parse(fieldValue, fa.field.getType());
			fa.field.set(fa.parent, value);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * get the value of a member, possibly down the hierarchy of field members
	 *
	 * @param root
	 *            root object
	 * @param path
	 *            possibly of the form a.b.c...
	 * @return primitive, object or list value, or null if the value is null, or
	 *         the path could not be traversed. If any of the child is an array
	 *         or list, its first member is used as the child
	 */
	public static Object getChildValue(Object root, String path) {
		FieldAndObj fa = getLeaf(root, path);
		if (fa == null) {
			return false;
		}
		try {
			return fa.field.get(fa.parent);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * set the value of a member, possibly down the hierarchy of field members
	 *
	 * @param root
	 *            root object
	 * @param path
	 *            possibly of the form a.b.c...
	 * @param fieldValue
	 *            value to be assigned. object must be compatible for the field
	 * @return true if value was set. false otherwise
	 */
	public static boolean setChildValue(Object root, String path, Object fieldValue) {
		FieldAndObj fa = getLeaf(root, path);
		if (fa == null) {
			return false;
		}
		try {
			fa.field.set(fa.parent, fieldValue);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * check if this object is a candidate for sub-objects and return such an
	 * object.
	 *
	 * @param object
	 * @return null if this object is null, or is primitive. if the object is
	 *         list/array then its first element is tried
	 */
	@SuppressWarnings("rawtypes")
	public static Object getPathObject(Object object) {
		if (object == null) {
			return null;
		}
		Object result = object;
		if (result.getClass().isArray()) {
			try {
				int n = Array.getLength(result);
				if (n == 0) {
					return null;
				}
				result = Array.get(result, 0);
			} catch (Exception e) {
				return null;
			}
		} else if (result instanceof List) {
			List list = (List) result;
			if (list.size() == 0) {
				return null;
			}
			result = list.get(0);
		}

		if (result == null || isPrimitiveValue(result)) {
			return null;
		}
		return result;
	}

	/**
	 * get the parent object and field to access the leaf specified by the path
	 *
	 * @param root
	 * @param path
	 * @return field and parent, or null in case of any trouble
	 */
	public static FieldAndObj getLeaf(Object root, String path) {
		Object parent = root;
		Field field = null;
		String fn = null;
		/*
		 * following for loop traverses down and comes out with the leaf Field
		 * and its parent obj
		 */
		for (String fieldName : path.split("\\.")) {
			if (field != null) {
				try {
					parent = field.get(parent);
				} catch (Exception ignore) {
					// we will handle it as obj == null
				}
			}

			parent = getPathObject(parent);
			if (parent == null) {
				logger.warn("Value for field {} is not valid while traversing path {} ", fn, path);
				return null;
			}
			field = getField(parent, fieldName);
			if (field == null) {
				logger.warn("No field named {} for object {} while traversing path {} ", fieldName,
						parent.getClass().getName(),
						path);
				return null;
			}
			field.setAccessible(true);
			fn = fieldName;
		}
		return new FieldAndObj(parent, field);
	}

	/**
	 * get the value of a non-primitive field. Create and assign an instance if
	 * required.
	 *
	 * @param object
	 *            non-null. from which to get the field value
	 * @param field
	 *            non-null. from which to get the value
	 * @param simpleClassName
	 *            can be null. class name to be used to create the instance.
	 *            non-null concrete sub-class name to be used if the if the
	 *            declared type is abstract or interface. null if the field
	 *            already as value, or the declared type is a concrete class
	 * @return non-null object value of the field. null in case of an error in
	 *         instantiating the required object
	 */
	public static Object createAndSetField(Object object, Field field, String simpleClassName) {
		try {
			Object fieldValue = field.get(object);
			if (fieldValue != null) {
				return fieldValue;
			}
			Class<?> fieldClass = field.getType();
			if (simpleClassName == null) {
				fieldValue = fieldClass.newInstance();
			} else {

				ClassLoader loader = fieldClass.getClassLoader();
				fieldValue = loader.loadClass(fieldClass.getPackage().getName() + '.' + simpleClassName).newInstance();
			}
			field.set(object, fieldValue);
			return fieldValue;
		} catch (Exception e) {
			logger.error("Error while instantiating and setting value for field {} in object {}. Error : {}",
					field.getName(), object.getClass().getName(), e.getMessage());
			return null;
		}
	}

	/**
	 * data structure that holds leaf field and its parent object in a hierarchy
	 *
	 * @author simplity.org
	 *
	 */
	public static class FieldAndObj {
		/**
		 * leaf field
		 */
		public final Field field;
		/**
		 * parent object of the leaf field
		 */
		public final Object parent;

		/**
		 * construct with members
		 *
		 * @param parent
		 * @param field
		 */
		public FieldAndObj(Object parent, Field field) {
			this.field = field;
			this.parent = parent;
		}
	}
}
