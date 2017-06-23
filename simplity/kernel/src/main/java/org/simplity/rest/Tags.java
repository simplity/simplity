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

package org.simplity.rest;

/**
 * contains definition of all tag/attribute names used from Open API
 *
 * @author simplity.org
 *
 */
public class Tags {
	/*
	 * attributes from open API that we use
	 */
	/**
	 * definitions object
	 */
	public static final String DEFS_ATTR = "definitions";
	/**
	 * refer to another node in a json
	 */
	public static final String REF_ATTR = "$ref";
	/**
	 * $ref values start with this prefix
	 */
	public static final String REF_START = "#/definitions/";
	/**
	 * base path of the spec.
	 */
	public static final String BASE_PATH_ATTR = "basePath";

	/**
	 * operation id associated with an operation (for a path and method
	 * combination)
	 */
	public static final String OP_ID_ATTR = "operationId";
	/**
	 * paths that are allowed inside the base path
	 */
	public static final String PATHS_ATTR = "paths";

	/**
	 * input parameters of an operation
	 */
	public static final String PARAMS_ATTR = "parameters";
	/**
	 * responses in an operation
	 */
	public static final String RESP_ATTR = "responses";
	/**
	 * name of parameter
	 */
	public static final String PARAM_NAME_ATTR = "name";
	/**
	 * location of data for this parameter
	 */
	public static final String IN_ATTR = "in";
	/**
	 * value of in attribute for header
	 */
	public static final String IN_HEADER = "header";
	/**
	 * value of in attribute for header
	 */
	public static final String IN_QUERY = "query";
	/**
	 * value of in attribute for header
	 */
	public static final String IN_BODY = "body";
	/**
	 * value of in attribute for header
	 */
	public static final String IN_PATH = "path";
	/**
	 * value of in attribute for header
	 */
	public static final String IN_FORM = "formData";
	/**
	 * parameter is required
	 */
	public static final String REQUIRED_ATTR = "required";
	/**
	 * min length of a text parameter
	 */
	public static final String MIN_LEN_ATT = "minLength";

	/**
	 * max length of a text parameter
	 */
	public static final String MAX_LEN_ATT = "maxLength";

	/**
	 * min value of a number
	 */
	public static final String MIN_ATT = "minimum";

	/**
	 * max value of a number
	 */
	public static final String MAX_ATT = "maximum";

	/**
	 * whether the value should be strictly less than the max value length of a
	 * text parameter
	 */
	public static final String EXCL_MIN_ATT = "exclusiveMinimum";

	/**
	 * whether value should be strictly greater than the min
	 */
	public static final String EXCL_MAX_ATT = "exclusiveMaximum";

	/**
	 * default value. Used only if it is optional
	 */
	public static final String DEFAULT_ATT = "default";

	/**
	 * regex pattern of a text parameter
	 */
	public static final String PATTERN_ATT = "pattern";

	/**
	 * min nbr of items in an array
	 */
	public static final String MIN_ITEMS_ATT = "minItems";

	/**
	 * max number of items in an array
	 */
	public static final String MAX_ITEMS_ATT = "maxItems";

	/**
	 * should items in an array should be unique. no duplicates
	 */
	public static final String UNIQUE_ATT = "uniqueItems";

	/**
	 * list of valid values. if this is given, we do not use other attributes
	 * for validation
	 */
	public static final String ENUM_ATT = "enum";

	/**
	 * whether number should be a multiple of this integer
	 */
	public static final String MULT_ATT = "multipleOf";

	/**
	 * how the collections are formatted into a string. like csv, etc..
	 */
	public static final String COLN_FORMAT_ATT = "collectionFormat";

	/**
	 * attr for items of a schema
	 */
	public static final String ITEMS_ATT = "items";

	/**
	 * can the parameter value be missing/empty?
	 */
	public static final String ALLOW_EMPTY_ATT = "allowEmptyValue";

	/**
	 * parameter format
	 */
	public static final String FORMAT_ATT = "format";

	/**
	 * float (not double)
	 */
	public static final String FLOAT_FORMAT = "float";
	/**
	 * int (not long)
	 */
	public static final String INT_FORMAT = "int32";
	/**
	 * date with no time
	 */
	public static final String DATE_FORMAT = "date";
	/**
	 * date with time
	 */
	public static final String DATE_TIME_FORMAT = "date-time";
	/**
	 *
	 */
	public static final String SCHEMA_ATTR = "schema";
	/**
	 * type of value
	 */
	public static final String TYPE_ATTR = "type";
	/**
	 * value of in attribute for headers
	 */
	public static final String HEADERS_ATTR = "headers";
	/**
	 * properties in a schema for object
	 */
	public static final String PROPERTIES_ATTR = "properties";

	/*
	 * custom tags defined and used by Simplity
	 */

	/**
	 * module name to be used for all services in this api
	 */
	public static final String MODULE_ATTR = "x-moduleName";
	/**
	 * service name. This is appended with moduleName, if any, for fully
	 * qualified service name
	 */
	public static final String SERVICE_NAME_ATTR = "x-serviceName";
	/**
	 * service translator at an operation level
	 */
	public static final String TRANSLATOR_ATTR = "x-serviceTranslator";
	/**
	 * Service implementation will take care of all validation. just accept all
	 * data from client and pass onto service
	 */
	public static final String ACCEPT_ALL_ATTR = "x-acceptAllData";
	/**
	 * do not use output parameter specification, but send all data
	 */
	public static final String SEND_ALL_ATTR = "x-sendAllData";

	/**
	 * field name associated with the body of response. This is at
	 * responseObject level
	 */
	public static final String FIELD_NAME_ATTR = "x-fieldName";
	/**
	 * open api json is invalid
	 */

	public static final String INVALID_API = "Open API document has invalid paths";
	/**
	 * service part delimiter
	 */
	public static final char SERVICE_SEP_CHAR = '.';

	/**
	 * path part delimiter as string
	 */
	public static final String PATH_SEP_STR = "/";
	/**
	 * path part delimiter as char
	 */
	public static final char PATH_SEP_CHAR = '/';
	/**
	 * all of data type
	 */
	public static final String ALL_OF_ATTR = "allOf";

	private Tags() {
		// as an indication that this is a static class
	}
}
