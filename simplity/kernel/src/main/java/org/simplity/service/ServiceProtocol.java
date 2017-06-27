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
package org.simplity.service;

/***
 * All names/Constants used for communication with the engine
 *
 * @author simplity.org
 *
 */
public abstract class ServiceProtocol {

	/*
	 * 1. Conventions
	 */

	/**
	 * as of now, we are hard coding. we do not see a need to make it flexible..
	 */
	public static final String CHAR_ENCODING = "UTF-8";

	/*
	 * client to server. These are also returned back to client for convenience
	 */
	/**
	 * name of service to be executed
	 */
	public static final String SERVICE_NAME = "_serviceName";

	/**
	 * an authenticated user for whom this service is to be executed
	 */
	public static final String USER_ID = "_userId";

	/**
	 * typically the password, but it could be third-party token etc.. for
	 * logging in
	 */
	public static final String USER_TOKEN = "_userToken";

	/**
	 * name/attribute that has the status of request. Possible values are
	 * STATUS_*
	 */
	public static final String REQUEST_STATUS = "_requestStatus";

	/**
	 * HTTP status all OK
	 */
	public static final String STATUS_OK = "ok";
	/**
	 * User needs to login for this service
	 */
	public static final String STATUS_NO_LOGIN = "noLogin";
	/**
	 * Service failed. Could be data error, or rejected because of business
	 * rules. Or it can be an internal error
	 */
	public static final String STATUS_ERROR = "error";

	/**
	 * time taken by this engine to execute this service in milliseconds
	 */
	public static final String SERVICE_EXECUTION_TIME = "_serviceExecutionTime";
	/**
	 * message type : some specific operation/action succeeded.
	 */
	public static final String MESSAGE_SUCCESS = "success";
	/**
	 * message type : general information.
	 */
	public static final String MESSGAE_INFO = "info";
	/**
	 * message type : warning/alert
	 */
	public static final String MESSAGE_WARNING = "warning";
	/**
	 * message type : ERROR
	 */
	public static final String MESSAGE_ERROR = "error";

	/**
	 * field name that directs a specific save action for the table/record
	 */
	public static final String TABLE_ACTION_FIELD_NAME = "_saveAction";
	/**
	 * tableSaveTask can get the action at run time
	 */
	public static final String TABLE_ACTION_ADD = "add";
	/**
	 * tableSaveTask can get the action at run time
	 */
	public static final String TABLE_ACTION_MODIFY = "modify";
	/**
	 * tableSaveTask can get the action at run time
	 */
	public static final String TABLE_ACTION_DELETE = "delete";
	/**
	 * tableSaveTask can get the action at run time
	 */
	public static final String TABLE_ACTION_SAVE = "save";

	/**
	 * list service typically sends a key value
	 */
	public static final String LIST_SERVICE_KEY = "_key";

	/*
	 * -------------- filter field comparators ----------------
	 */
	/**
	 *
	 */
	public static final String EQUAL = "=";
	/**
	 *
	 */
	public static final String NOT_EQUAL = "!=";
	/**
	 *
	 */
	public static final String LESS = "<";
	/**
	 *
	 */
	public static final String LESS_OR_EQUAL = "<=";
	/**
	 *
	 */
	public static final String GREATER = ">";
	/**
	 *
	 */
	public static final String GREATER_OR_EQUAL = ">=";
	/**
	 *
	 */
	public static final String LIKE = "~";
	/**
	 *
	 */
	public static final String STARTS_WITH = "^";
	/**
	 *
	 */
	public static final String BETWEEN = "><";

	/**
	 * one of the entries in a list
	 */
	public static final String IN_LIST = "@";
	/**
	 * suffix for the to-Field If field is "age" then to-field would be "ageTo"
	 */
	public static final String TO_FIELD_SUFFIX = "To";

	/**
	 * like ageComparator
	 */
	public static final String COMPARATOR_SUFFIX = "Operator";
	/**
	 * comma separated names of columns that are to be used for sorting rows
	 */
	public static final String SORT_COLUMN_NAME = "_sortColumns";

	/**
	 * sort order asc or desc. asc is the default
	 */
	public static final String SORT_ORDER = "_sortOrder";
	/**
	 * ascending
	 */
	public static final String SORT_ORDER_ASC = "asc";
	/**
	 * descending
	 */
	public static final String SORT_ORDER_DESC = "desc";

	/**
	 * trace from server execution. Available only in non-production mode
	 */
	public static final String TRACE_FIELD_NAME = "_trace";
	/**
	 * non-error messages from server execution.
	 */
	public static final String MESSAGES = "_messages";
	/**
	 * special service that deals with server-side pagination
	 */
	public static final String PAGINATION_SERVICE = "_p";

	/**
	 * if a service request wants a table to be paginated in its way back t
	 * client, then it should send the page size in a field names sheetName
	 * suffixed with this indicator
	 */
	public static final String PAGE_SIZE_SUFFIX = "PageSize";

	/**
	 * if a sheet is paginated, server would return total count of rows in an
	 * additional field for the sheet name with this suffix
	 */
	public static final String TOTAL_COUNT_SUFFIX = "TotalCount";

	/**
	 * field name that has the table name while requesting for a specific page
	 */
	public static final String PAGINATION_TABLE = "_tableName";

	/**
	 * field name that has the page size for pagination service
	 */
	public static final String PAGINATION_SIZE = "_pageSize";

	/**
	 * field name that has the page number for pagination service
	 */
	public static final String PAGINATION_PAGE_NUMBER = "_pageNumber";

	/**
	 * should suggestion service suggest matching strings that start with the
	 * starting key?
	 */
	public static final String SUGGEST_STARTING = "_matchStarting";

	/**
	 * list of values can be separated with this. e.g. a,b,s
	 */
	public static final char LIST_SEPARATOR = ',';
	/**
	 * value can have an internal value and a display value, separated with this
	 * character. e.g. True:1,False:0
	 */
	public static final char LIST_VALUE_SEPARATOR = ':';

	/**
	 * whenever field list is expected, we can use this special name to indicate
	 * all
	 */
	public static final String ALL_FIELDS = "_allFields";

	/**
	 * field name to indicate whether the session timed out, or user requested a
	 * logout
	 */
	public static final String TIMED_OUT = "timedOut";

	/**
	 * header field that has the name of the file being uploaded
	 */
	public static final String HEADER_MIME_TYPE = "_mimeType";

	/**
	 * header field that has the name of the file being uploaded
	 */
	public static final String HEADER_FILE_NAME = "_fileName";

	/**
	 * header field that has the token for the uploaded file returned from
	 * server. This token needs to to sent back to server as a reference for the
	 * uploaded file
	 */
	public static final String HEADER_FILE_TOKEN = "_fileToken";
	/**
	 * if file name is not set for an uploaded file we use this as the default
	 * file name
	 */
	public static final String DEFAULT_FILE_NAME = "_noName";
	/**
	 * value of header field SERVICE_NAME to request to discard an media that
	 * was uploaded earlier
	 */

	public static final String SERVICE_DELETE_FILE = "_discard";

	/**
	 * special file name that indicates logs instead of a file content
	 */
	public static final String FILE_NAME_FOR_LOGS = "_logs";
	/**
	 * whenever an attachment field is updated, its existing value in the data
	 * base can be set to a field with the name+prefix. Service will process
	 * this and take care of removing the attachment from storage based on input
	 * specification
	 *
	 */
	public static final String OLD_ATT_TOKEN_SUFFIX = "Old";

	/**
	 * name of the field in service context that indicates whether this login attempt is by auto-login feature. For example a servce.xml being used as login service can use executeOnCondition="_isAutoLogin"
	 */
	public static final String IS_AUTO_LOGIN = "_isAutoLogin";
	/**
	 * name of the field with the rowText in the context
	 */
	public static final String ROW_TEXT = "_rowText";
	/**
	 * name of the field with the row line in the context
	 */
	public static final String LINE_NUM = "_lineNum";
	/**
	 * name of the file currently being processed
	 */
	public static final String FIlE_BATCH = "_fileBatch";

	/**
	 * http response code to be used by the server to respond back to client
	 */

	public static final String HTTP_RESP_CODE_FIELD_NAME = "_httpResponseCode";

	/**
	 * in case client sends data as non-json object, that would be extracted to inData with this name, unless there is any other name specified using any of the features available with the client agent
	 */
	public static final String DEFAULT_BODY_FIELD_NAME = "_body";
}