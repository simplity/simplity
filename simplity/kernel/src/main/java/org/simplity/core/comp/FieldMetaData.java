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
package org.simplity.core.comp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Java generics does not keep declared classes at run time. For our
 * data-binding, we get into trouble in case the class of map members is in a
 * package other than the class that defines it. Use this annotation to mark the
 * package of map members.
 *
 * @author simplity.org
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FieldMetaData {
	/**
	 * @return is this a mandatory field
	 */
	public boolean isRequired() default false;

	/**
	 *
	 * @return to be used for validating the value
	 */
	public String regex() default "";

	/**
	 *
	 * @return field to mimic. If that field has value, this field should also
	 *         have value. If that field has no value, this field should also
	 *         not have value. error if one of them has value and the other one
	 *         does not have.
	 */
	public String leaderField() default "";

	/**
	 *
	 * @return that field should have value for this field to be relevant. Error
	 *         if this field has value when that field does not have.
	 */
	public String relevantBasedOnField() default "";

	/**
	 *
	 * @return this field is relevant only if the that field has no value. Error
	 *         if this field has value when the other field has value.
	 */
	public String irrelevantBasedOnField() default "";

	/**
	 *
	 * @return this field should have value if that field does not have, and
	 *         this field should not have value if that field has value. error
	 *         if either both have value, or both do not have value.
	 */
	public String alternateField() default "";

	/**
	 *
	 * @return relevant only if accompanyIfValueEqualTo is set to true.
	 */
	public ComponentType referredCompType() default ComponentType.DT;

	/**
	 *
	 * @return relevant if this field refers to another component.
	 */
	public boolean isReferenceToComp() default false;

	/**
	 *
	 * @return required if this field is the fully-qualified class name. it is
	 *         the interface that this class is to implement
	 */
	public Class<?> superClass() default Object.class;

	/**
	 *
	 * @return required if this field is a <code>Map</code>. Since Java does not
	 *         retain the type, and we may need the flexibility to use
	 *         sub-class, we need to have the package name to convert the tag to
	 *         qualified class. Current limitation is that all the classes are
	 *         in same package. Also, if this class is a concrete class, then
	 *         that class is used to create an instance irrespective of the tag
	 *         name. This feature provides flexibility for the xml schema to use
	 *         different tag name to different "types" of the class. e.g.
	 *         record.field
	 */
	public Class<?> memberClass() default Object.class;

	/**
	 *
	 * @return relevant for a field that is <code>Map</code>. name of the field
	 *         that is used for indexing in the MAP. "name" is the default
	 */
	public String indexFieldName() default "name";

	/**
	 *
	 * @return field that should not be specified if this field is specified
	 */
	public String prohibitedField() default "";

	/**
	 * TODO: this feature is not finalized. We still use restriction on package
	 * to manage components. That is, all the sub-classes belong to the same
	 * package as parent class
	 *
	 * @return if the field is a map, or if it is an abstract/interface, then
	 *         the tag determines the actual class of the field value. By
	 *         default we use the package name of the parent object as the
	 *         package name for this field value also. However, for elements
	 *         that can be extended by application specific code, this becomes
	 *         an issue. This class is an implementation of ICompFactory. We did
	 *         not go for generics to avoid the issue with default value. Of
	 *         course, we also decided against having a separate annotation for
	 *         this
	 */
	public Class<?> childObjectFactory() default Object.class;

}
