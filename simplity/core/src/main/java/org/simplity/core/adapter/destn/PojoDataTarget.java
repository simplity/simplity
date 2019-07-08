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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.core.adapter.destn;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

import org.simplity.core.adapter.IDataListTarget;
import org.simplity.core.adapter.IDataTarget;
import org.simplity.core.util.ReflectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Java Object as a target to c=receive data from an adapter
 *
 * @author simplity.org
 *
 */
public class PojoDataTarget {
	protected static final Logger logger = LoggerFactory.getLogger(PojoDataTarget.class);

	/**
	 * get a data target for this object
	 *
	 * @param pojo
	 *            if this happens to be a list/array, its first element is being
	 *            tried as target
	 * @return data target for this object. null if this is not possible
	 */
	public static IDataTarget getTarget(Object pojo) {
		Object obj = ReflectUtil.getPathObject(pojo);
		if (obj == null) {
			logger.warn("{} is not a valid data target.", pojo == null ? "null" : pojo.getClass().getName());
			return null;
		}
		return new Target(obj);
	}

	/**
	 * create a data target for a child object of a POJO
	 *
	 * @param root
	 * @param fieldName
	 * @param memberClassName
	 *            required only if the member is a List
	 * @return data source, or null in case of any issue
	 */
	@SuppressWarnings("unchecked")
	public static IDataTarget getTarget(Object root, String fieldName, String memberClassName) {
		ReflectUtil.FieldAndObj fno = ReflectUtil.getLeaf(root, fieldName);
		if (fno == null) {
			logger.warn("{} is not a field in {}. Data target can not be created.", root.getClass().getName(),
					fieldName);
			return null;
		}
		Field field = fno.field;
		Object parent = fno.parent;
		Object obj = null;
		Class<?> fieldType = field.getType();
		if (ReflectUtil.isPrimitiveValue(fieldType)) {
			logger.warn("{} is a primitive. Can not receive data", fieldName);
			return null;
		}
		if (fieldType.isArray()) {
			try {
				Object arr = Array.newInstance(fieldType.getComponentType(), 1);
				obj = fieldType.getComponentType().newInstance();
				Array.set(arr, 0, obj);
				field.set(parent, arr);
				return new Target(obj);
			} catch (Exception e) {
				logger.warn("Error while creating and assigning an array to field {} in object {}. Error:{}",
						field.getName(), parent.getClass().getName(), e.getMessage());
				return null;
			}
		}

		/*
		 * we may want to instantiate in case the Pojo has not done it...
		 */
		field.setAccessible(true);
		try {
			obj = field.get(parent);
			if (obj == null) {
				obj = fieldType.newInstance();
				field.set(parent, obj);
			}
		} catch (Exception ignore) {
			logger.warn("Unable to access field {} in object", field.getName(), parent.getClass().getName());
			return null;
		}

		if (obj instanceof List) {
			if (memberClassName == null) {
				logger.warn(
						"Field {} is a list in the class {}, and hence it requires componentClassName to create member instances",
						field.getName(), parent.getClass().getName());
				return null;
			}
			try {
				@SuppressWarnings("rawtypes")
				List list = (List) obj;
				obj = Class.forName(memberClassName).newInstance();
				list.add(obj);
				return new Target(obj);
			} catch (Exception e) {
				logger.warn("Member class name {} could not be instantiated as data source within a list",
						memberClassName);
				return null;
			}
		}

		/*
		 * single object beig treated as a list target
		 */
		return new Target(obj);

	}

	private static class Target implements IDataTarget {
		private Object target;

		/**
		 * use this object as target of data from an adapter
		 *
		 * @param pojo
		 */
		protected Target(Object pojo) {
			this.target = pojo;
		}

		@Override
		public void setPrimitiveValue(String fieldName, String fieldValue) {
			boolean done = ReflectUtil.setChildPrimitive(this.target, fieldName, fieldValue);
			if (!done) {
				logger.warn("Primitive value of {} not assigned to field {} in target {}", fieldValue, fieldName,
						this.target.getClass().getName());
				return;
			}
		}

		@Override
		public IDataTarget getChildTarget(String fieldName, String className) {
			return getTarget(this.target, fieldName, className);
		}

		@Override
		public IDataListTarget getChildListTarget(String fieldName, String memberClassName) {
			return PojoListTarget.getTarget(this.target, fieldName, memberClassName);
		}

		@Override
		public void setStruct(String fieldName, Object fieldValue) {
			boolean done = ReflectUtil.setChildValue(this.target, fieldName, fieldValue);
			if (!done) {
				logger.warn("Object value of {} not assigned to field {} in target {}",
						fieldValue.getClass().getTypeName(), fieldName,
						this.target.getClass().getName());
				return;
			}
		}

		@Override
		public void setDateValue(String fieldName, LocalDate fieldValue) {
			ReflectUtil.setAttribute(this.target, fieldName, fieldValue, false);
		}
	}
}
