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
import java.util.ArrayList;
import java.util.List;

import org.simplity.core.adapter.IDataListTarget;
import org.simplity.core.adapter.IDataTarget;
import org.simplity.core.util.ReflectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Java array or List as target to receive list of data
 *
 * @author simplity.org
 *
 */
@SuppressWarnings("rawtypes")
public class PojoListTarget {
	protected static final Logger logger = LoggerFactory.getLogger(PojoListTarget.class);

	/**
	 * list target for a List
	 *
	 * @param target
	 *            - non-null list
	 * @param memberClassName
	 *            non-null qualified class name
	 * @return null in case the member class could not be loaded
	 */
	public static IDataListTarget getTarget(List target, String memberClassName) {
		try {
			Class<?> cls = Class.forName(memberClassName);
			return new ListTarget(target, cls);
		} catch (Exception e) {
			logger.warn("Class {} could not be loaded. null List target returned");
			return null;
		}
	}

	/**
	 * Single object to be treated as a list target with just one member
	 * capacity
	 *
	 * @param target
	 *            - non-null object
	 * @return non-null list target
	 */
	public static IDataListTarget getTarget(Object target) {
		return new ObjTarget(target);
	}

	/**
	 * create a child list target for a parent
	 *
	 * @param root
	 *            root object
	 * @param fieldName
	 *            name of the field, possibly with ... to be used as list target
	 * @param memberClassName
	 *            required in case the member is a list
	 * @return list target, or null in case of any issue
	 */
	public static IDataListTarget getTarget(Object root, String fieldName, String memberClassName) {
		ReflectUtil.FieldAndObj fno = ReflectUtil.getLeaf(root, fieldName);
		if (fno == null) {
			logger.warn("{} is not a valid field for a list target in {}. target not created.", fieldName,
					root.getClass().getName());
			return null;
		}
		Field field = fno.field;
		Object parent = fno.parent;
		Class<?> fieldType = field.getType();
		if (ReflectUtil.isPrimitiveValue(fieldType)) {
			logger.warn("{} is a primitive. Can not receive list of data", fieldName);
			return null;
		}

		Class<?> compType = null;
		if (memberClassName != null) {
			try {
				compType = Class.forName(memberClassName);
			} catch (Exception e) {
				logger.warn(" {} is not a valid class name. list target not created for field {} ", memberClassName,
						fieldName);
				return null;
			}
		}
		if (fieldType.isArray()) {
			if (compType == null) {
				compType = fieldType.getComponentType();
			}
			return new ArrayTarget(compType, field, parent);
		}
		/*
		 * we may want to instantiate in case the Pojo has not done it...
		 */
		field.setAccessible(true);
		Object obj = null;
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
			if (compType == null) {
				logger.warn(
						"Field {} is a list in the class {}, and hence it requires componentClassName to create member instances",
						field.getName(), parent.getClass().getName());
				return null;
			}
			return new ListTarget((List) obj, compType);
		}

		/*
		 * single object being treated as a list target
		 */
		return new ObjTarget(obj);
	}

	private static class ListTarget implements IDataListTarget {
		protected final List target;
		protected final Class<?> memberType;

		protected ListTarget(List target, Class<?> memberType) {
			this.target = target;
			this.memberType = memberType;
		}

		@SuppressWarnings("unchecked")
		@Override
		public IDataTarget getChildTarget(int zeroBasedIdx) {
			try {
				Object obj = this.memberType.newInstance();
				this.target.add(obj);
				return PojoDataTarget.getTarget(obj);
			} catch (Exception e) {
				logger.warn("Unable to get an instance for class ", this.memberType.getName());
				return null;
			}
		}

		@Override
		public void listDone() {
			//
		}
	}

	private static class ArrayTarget extends ListTarget {
		private final Object parentObject;
		private final Field field;

		protected ArrayTarget(Class<?> memberType, Field field, Object parentObject) {
			super(new ArrayList(), memberType);
			this.parentObject = parentObject;
			this.field = field;
		}

		@Override
		public void listDone() {
			if (this.target.size() == 0) {
				logger.warn("No member added to list target. Array not assigned in the target object {}",
						this.parentObject.getClass().getName());
				return;
			}
			Object[] arr = (Object[]) Array.newInstance(this.memberType, 0);
			try {
				this.field.set(this.parentObject, arr);
			} catch (Exception e) {
				logger.warn("Unable to set value of {} to field {} of object {}", arr.getClass().getName(),
						this.field.getName(), this.parentObject.getClass().getName());
			}
		}
	}

	private static class ObjTarget implements IDataListTarget {
		protected final Object target;

		protected ObjTarget(Object target) {
			this.target = target;
		}

		@Override
		public IDataTarget getChildTarget(int zeroBasedIdx) {
			if (zeroBasedIdx != 0) {
				logger.error("List target is a single object {}. can have only 0 index, not {}",
						this.target.getClass().getName(), zeroBasedIdx);
				return null;
			}
			return PojoDataTarget.getTarget(this.target);
		}

		@Override
		public void listDone() {
			//
		}
	}
}
