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
package org.simplity.kernel.app;

import org.simplity.kernel.comp.ComponentType;

/**
 * * All names/Constants used for communication with the engine
 *
 * @author simplity.org
 */
public class AppConventions {

	private AppConventions() {
		// conveys that this is not meant to be instantiated
	}

	/**
	 * as of now, we are hard coding. we do not see a need to make it flexible..
	 */
	public static final String CHAR_ENCODING = "UTF-8";

	/** list of values can be separated with this. e.g. a,b,s */
	public static final char LIST_SEPARATOR = ',';
	/**
	 * value can have an internal value and a display value, separated with this
	 * character. e.g. True:1,False:0
	 */
	public static final char LIST_VALUE_SEPARATOR = ':';

	/**
	 * separator character between key field values to form a single string of
	 * key
	 */
	public static final char CACHE_KEY_SEP = '\0';

	/**
	 * comps are defined as enum, but we have a convention of arranging them in
	 * an order. This is for some classes to use array instead of collections
	 */
	public static final ComponentType[] COMP_TYPES = { ComponentType.DT, ComponentType.MSG, ComponentType.REC,
			ComponentType.SERVICE, ComponentType.SQL, ComponentType.SP, ComponentType.FUNCTION, ComponentType.TEST_RUN,
			ComponentType.JOBS, ComponentType.EXTERN, ComponentType.ADAPTER };

	/**
	 * field names for some common data elements
	 */
	public static class Name {

		/**
		 * parameter name for resourceRoot for the application
		 */
		public static final String RESOURCE_ROOT = "org.simplity.resourceRoot";

		/**
		 * name of the application configuration file that is expected in the
		 * resource root directory
		 */
		public static final String CONFIG_FILE_NAME = "application.xml";
		/**
		 * resource prefix for system resources
		 */
		public static final String BUILT_IN_COMP_PREFIX = "org/simplity/comp/";
		/**
		 * file name that has the comps used internally by Simplity. These are
		 * part of the simplity jar. for e.e org/simplity/comp/dt/simplity.xml
		 * contains all data types used internally by Simplity
		 */
		public static final String BUILT_IN_COMP_FILE_NAME = "simplity.xml";

		/**
		 * folder names under which comps are saved. Array is in the same order
		 * as COMP_TYPES
		 */
		public static final String[] COMP_FOLDER_NAMES = { "dt/", "msg/", "rec/", "service/", "sql/", "sp/", "fn/",
				"test/", "jobs/", "extern/", "adapter/" };
		/*
		 * client to server. These are also returned back to client for
		 * convenience
		 */
		/** name of service to be executed */
		public static final String SERVICE_NAME = "_serviceName";

		/** an authenticated user for whom this service is to be executed */
		public static final String USER_ID = "_userId";

		/**
		 * typically the password, but it could be third-party token etc.. for
		 * logging in
		 */
		public static final String USER_TOKEN = "_userToken";

		/** time taken by this engine to execute this service in milliseconds */
		public static final String SERVICE_EXECUTION_TIME = "_serviceExecutionTime";
		/**
		 * field name that directs a specific save action for the table/record
		 */
		public static final String TABLE_ACTION = "_saveAction";
		/** list service typically sends a key value */
		public static final String LIST_SERVICE_KEY = "_key";
		/**
		 * suffix for the to-Field If field is "age" then to-field would be
		 * "ageTo"
		 */
		public static final String TO_FIELD_SUFFIX = "To";

		/** like ageComparator */
		public static final String COMPARATOR_SUFFIX = "Comparator";
		/**
		 * comma separated names of columns that are to be used for sorting rows
		 */
		public static final String SORT_COLUMN = "_sortColumns";
		/** sort order asc or desc. asc is the default */
		public static final String SORT_ORDER = "_sortOrder";
		/** non-error messages from server execution. */
		public static final String MESSAGES = "_messages";
		/**
		 * should suggestion service suggest matching strings that start with
		 * the starting key?
		 */
		public static final String SUGGEST_STARTING = "_matchStarting";

		/**
		 * whenever field list is expected, we can use this special name to
		 * indicate all
		 */
		public static final String ALL_FIELDS = "_allFields";
		/** header field that has the name of the file being uploaded */
		public static final String HEADER_MIME_TYPE = "_mimeType";

		/** header field that has the name of the file being uploaded */
		public static final String HEADER_FILE_NAME = "_fileName";

		/**
		 * header field that has the token for the uploaded file returned from
		 * server. This token needs to to sent back to server as a reference for
		 * the uploaded file
		 */
		public static final String HEADER_FILE_TOKEN = "_fileToken";
		/**
		 * if file name is not set for an uploaded file we use this as the
		 * default file name
		 */
		public static final String DEFAULT_FILE_NAME = "_noName";
		/**
		 * value of header field SERVICE_NAME to request to discard an media
		 * that was uploaded earlier
		 */
		public static final String SERVICE_DELETE_FILE = "_discard";

		/** special file name that indicates logs instead of a file content */
		public static final String FILE_NAME_FOR_LOGS = "_logs";
		/**
		 * whenever an attachment field is updated, its existing value in the
		 * data base can be set to a field with the name+prefix. Service will
		 * process this and take care of removing the attachment from storage
		 * based on input specification
		 */
		public static final String OLD_ATT_TOKEN_SUFFIX = "Old";

		/**
		 * name of the field in service context that indicates whether this
		 * login attempt is by auto-login feature. For example a servce.xml
		 * being used as login service can use executeOnCondition="_isAutoLogin"
		 */
		public static final String IS_AUTO_LOGIN = "_isAutoLogin";
		/** name of the field with the rowText in the context */
		public static final String ROW_TEXT = "_rowText";
		/** name of the field with the row line in the context */
		public static final String LINE_NUM = "_lineNum";
		/** name of the file currently being processed */
		public static final String FIlE_BATCH = "_fileBatch";

		/**
		 * name/attribute that has the status of request. Possible values are
		 * STATUS_*
		 */
		public static final String REQUEST_STATUS = "_requestStatus";

	}

	/**
	 * pre-defined values of standard fields. Field names are in Names, and
	 * their possible values, if any, are defined inside this group
	 *
	 * @author simplity.org
	 *
	 */
	public static class Value {
		/**
		 * we assume that the resources are deployed in jar/classes with res/ as
		 * prefix
		 */
		public static final String RESOURCE_ROOT_DEFAULT = "res/";

		/**
		 * values for TABLE_ACTION
		 *
		 * @author simplity.org
		 *
		 */
		public static class TableAction {
			/** tableSaveTask can get the action at run time */
			public static final String TABLE_ACTION_ADD = "add";
			/** tableSaveTask can get the action at run time */
			public static final String TABLE_ACTION_MODIFY = "modify";
			/** tableSaveTask can get the action at run time */
			public static final String TABLE_ACTION_DELETE = "delete";
			/** tableSaveTask can get the action at run time */
			public static final String TABLE_ACTION_SAVE = "save";

		}

		/**
		 * values for status field in response
		 *
		 * @author simplity.org
		 *
		 */
		public static class Status {
			/** HTTP status all OK */
			public static final String OK = "ok";
			/** User needs to login for this service */
			public static final String NO_LOGIN = "noLogin";
			/**
			 * Service failed. Could be data error, or rejected because of
			 * business rules. Or it can be an internal error
			 */
			public static final String ERROR = "error";
		}

		/**
		 * message severity
		 *
		 * @author simplity.org
		 *
		 */
		public static class MessageSeverity {
			/** message type : some specific operation/action succeeded. */
			public static final String MESSAGE_SUCCESS = "success";
			/** message type : general information. */
			public static final String MESSGAE_INFO = "info";
			/** message type : warning/alert */
			public static final String MESSAGE_WARNING = "warning";
			/** message type : ERROR */
			public static final String MESSAGE_ERROR = "error";

		}

		/**
		 * values for field SORT_ORDER
		 *
		 * @author simplity.org
		 *
		 */
		public static class SortOrder {
			/** ascending */
			public static final String ASC = "asc";
			/** descending */
			public static final String DESC = "desc";

		}
	}

	/**
	 * comparators, typically used in expressions and row selection criteria
	 *
	 * @author simplity.org
	 *
	 */
	public static class Comparator {
		/** */
		public static final String EQUAL = "=";
		/** */
		public static final String NOT_EQUAL = "!=";
		/** */
		public static final String LESS = "<";
		/** */
		public static final String LESS_OR_EQUAL = "<=";
		/** */
		public static final String GREATER = ">";
		/** */
		public static final String GREATER_OR_EQUAL = ">=";
		/** */
		public static final String LIKE = "~";
		/** */
		public static final String STARTS_WITH = "^";
		/** */
		public static final String BETWEEN = "><";

		/** one of the entries in a list */
		public static final String IN_LIST = "@";
	}
}
