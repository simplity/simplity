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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.kernel.comp;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Map;

import org.simplity.kernel.util.ReflectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author simplity.org */
public class ValidationUtil {
	private static final Logger logger = LoggerFactory.getLogger(ValidationUtil.class);

	/**
	 * this is a method with a "trick". it returns null if the value, by "our
	 * convention" is not specified. non-null if specified. array is specified
	 * if it has at least one element. string is specified if it is non-empty.
	 * int is specified if it is non-zero, and boolean is specified if it is
	 * true
	 *
	 * @param obj
	 * @param field
	 * @return
	 */
	private static Object getFieldValue(Field field, Object obj) {
		Object value = null;
		try {
			field.setAccessible(true);
			value = field.get(obj);
		} catch (Exception e) {
			logger.error("Surprising that field.get() on obj={}, field={} threw an excpetion : {}", obj,
					field.getName(), e.getMessage());
			return null;
		}
		if (value == null) {
			return null;
		}
		if (value instanceof String) {
			if (((String) value).isEmpty()) {
				return null;
			}
			return value;
		}

		Class<?> cls = value.getClass();
		if (cls.isArray()) {
			if (Array.getLength(value) == 0) {
				return null;
			}
			return value;
		}
		if (cls.equals(boolean.class)) {
			if ((boolean) value) {
				return value;
			}
			return null;
		}
		if (cls.equals(int.class)) {
			if ((int) value == 0) {
				return null;
			}
			return value;
		}
		return value;
	}

	private static Field getField(Map<String, Field> allFields, String fieldName) {
		if (fieldName.isEmpty()) {
			return null;
		}
		Field f = allFields.get(fieldName);
		if (f == null) {
			logger.error("{} is an invalid field name used as ref field in annotation");
			return null;
		}
		return f;

	}

	/**
	 * validate this component based on meta-annotations on its fields. Caller
	 * should have initiated the validation process with calls to
	 * ValidationContext.beginXX etc..
	 *
	 * @param vtx
	 *
	 * @param comp
	 */
	public static void validateMeta(IValidationContext vtx, Object comp) {
		Map<String, Field> allFields = ReflectUtil.getAllFields(comp);
		for (Map.Entry<String, Field> entry : allFields.entrySet()) {
			Field field = entry.getValue();
			String fieldName = entry.getKey();
			FieldMetaData meta = field.getAnnotation(FieldMetaData.class);
			if (meta == null) {
				continue;
			}
			String fn;
			fn = meta.leaderField();
			Field leaderField = fn.isEmpty() ? null : getField(allFields, fn);

			fn = meta.relevantBasedOnField();
			Field relField = fn.isEmpty() ? null : getField(allFields, fn);

			fn = meta.irrelevantBasedOnField();
			Field irrelField = fn.isEmpty() ? null : getField(allFields, fn);

			fn = meta.alternateField();
			Field altField = fn.isEmpty() ? null : getField(allFields, fn);

			Object value = getFieldValue(field, comp);
			/*
			 * start with case when field has no value
			 */
			if (value == null) {

				if (meta.isRequired()) {
					vtx.message(new ValidationMessage(comp, ValidationMessage.SEVERITY_ERROR,
							"Value is required for field " + fieldName,
							fieldName));
					continue;
				}

				if (leaderField != null && getFieldValue(leaderField, comp) == null) {
					vtx.message(new ValidationMessage(comp, ValidationMessage.SEVERITY_ERROR,
							fieldName + " should have value when " + leaderField.getName() + " has value",
							fieldName));
				}

				if (altField != null && getFieldValue(altField, comp) == null) {
					vtx.message(new ValidationMessage(comp, ValidationMessage.SEVERITY_ERROR,
							fieldName + " must have value when " + altField.getName() + " has no value",
							fieldName));
				}

				continue;
			}
			/*
			 * this field has value..
			 */
			if (leaderField != null && getFieldValue(leaderField, comp) == null) {
				vtx.message(new ValidationMessage(comp, ValidationMessage.SEVERITY_ERROR,
						fieldName + " should not have value when " + leaderField.getName() + " has no value",
						fieldName));
			}

			if (relField != null && getFieldValue(relField, comp) != null) {
				vtx.message(new ValidationMessage(comp, ValidationMessage.SEVERITY_ERROR,
						fieldName + " is relevant only when " + relField.getName() + " has value. Hence "
								+ fieldName + " should not have value",
						fieldName));
			}

			if (irrelField != null && getFieldValue(irrelField, comp) != null) {
				vtx.message(new ValidationMessage(comp, ValidationMessage.SEVERITY_ERROR,
						fieldName + " is not relevant, when " + irrelField.getName() + " has value. Hence "
								+ fieldName + " should not have value",
						fieldName));
			}

			if (altField != null && getFieldValue(altField, comp) != null) {
				vtx.message(new ValidationMessage(comp, ValidationMessage.SEVERITY_ERROR,
						fieldName + " should not have value when " + altField.getName() + " has value", fieldName));
			}

			// we do know that fieldValue is non-null
			String textValue = value.toString();

			/*
			 * regex restrictions?
			 */
			String regex = meta.regex();
			if (!regex.isEmpty()) {
				if (field.getType().isArray()) {
					String[] values = (String[]) value;
					for (String val : values) {
						if (val.matches(regex) == false) {
							vtx.message(messageForRegex(comp, fieldName, regex));
						}
					}
				} else if (!textValue.matches(regex)) {
					vtx.message(messageForRegex(comp, fieldName, regex));
				}
			}

			/*
			 * is this a class that is to implement an interface? (we can't have
			 * null, hence use Object.class as not-specified
			 */
			Class<?> interFace = meta.superClass();
			if (interFace.equals(Object.class) == false) {
				Class<?> cls = null;
				try {
					cls = Class.forName(textValue);
				} catch (Exception e) {
					logger.error("Class {} could not be located. Are you missing any jar file ?. error:{}", textValue,
							e.getMessage());
				}
				if (cls == null) {
					vtx.message(new ValidationMessage(comp, ValidationMessage.SEVERITY_ERROR,
							textValue + " could not be used as a class name. May be a jar file is missing.",
							textValue));
				} else if (!interFace.isAssignableFrom(cls)) {
					vtx.message(new ValidationMessage(comp, ValidationMessage.SEVERITY_ERROR,
							"Class " + textValue + " is not a sub-class of " + interFace.getName(), fieldName));
				}
			}

			/*
			 * is this a reference to another component?
			 */
			if (meta.isReferenceToComp()) {
				if (field.getType().isArray()) {
					String[] values = (String[]) value;
					for (String val : values) {
						vtx.reference(new ValidationReference(comp, fieldName, val, meta.referredCompType()));
					}
				} else {
					vtx.reference(
							new ValidationReference(comp, fieldName, textValue, meta.referredCompType()));
				}
			}
		}
	}

	/**
	 * create a standard message for missing value for a field that is mandatory
	 *
	 * @param comp
	 *
	 * @param fieldName
	 * @return message object that can be used to call
	 *         <code>ValidationContext</code>.message()
	 */
	public static ValidationMessage messageForMissingValue(Object comp, String fieldName) {
		return new ValidationMessage(comp, ValidationMessage.SEVERITY_ERROR, "Value is required for field " + fieldName,
				fieldName);
	}

	/**
	 * create a standard message for missing value of an accompanying field
	 *
	 * @param comp
	 *
	 * @param fieldName
	 * @param accompliceName
	 * @return message object that can be used to call
	 *         <code>ValidationContext</code>.message()
	 */
	public static ValidationMessage messageForAccomplice(Object comp, String fieldName, String accompliceName) {
		return new ValidationMessage(comp, ValidationMessage.SEVERITY_ERROR,
				"Value is required for field " + accompliceName
						+ " whenever a value is specified for field " + fieldName,
				fieldName);
	}

	/**
	 * create a standard message when field that is prohibited (based on value
	 * on another field)
	 *
	 * @param comp
	 *
	 * @param fieldName
	 * @param opponentName
	 * @return message object that can be used to call
	 *         <code>ValidationContext</code>.message()
	 */
	public static ValidationMessage messageForProhibition(Object comp, String fieldName, String opponentName) {
		return new ValidationMessage(comp, ValidationMessage.SEVERITY_ERROR, "Field " + opponentName
				+ " should not be specified whenever a value is specified for field " + fieldName, fieldName);
	}

	/**
	 * create a standard message for missing value for a field that is mandatory
	 *
	 * @param comp
	 *
	 * @param fieldName
	 * @param regex
	 * @return message object that can be used to call
	 *         <code>ValidationContext</code>.message()
	 */
	public static ValidationMessage messageForRegex(Object comp, String fieldName, String regex) {
		return new ValidationMessage(comp, ValidationMessage.SEVERITY_ERROR,
				"Field " + fieldName + " has an invalid value. Value is to conform to regex " + regex, fieldName);
	}
}
