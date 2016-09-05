/*
 * Copyright (c) simplity.org
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
/**
 * conventions while communicating with a simplity based server
 * 
 */
var POCOL = {
	/**
	 * as of now, we are hard coding. we do not see a need to make it flexible..
	 */
	CHAR_ENCODING : "UTF-8",

	/*
	 * client to server.
	 */
	/**
	 * name of service to be executed
	 */
	SERVICE_NAME : "_serviceName",

	/**
	 * used for login-process along with USER_TOKEN.
	 */
	USER_ID : "_userId",

	/**
	 * used for login-process along with USER_ID.
	 */
	USER_TOKEN : "_userToken",

	/**
	 * HTTP header with which to exchange CSRF token
	 */
	CSRF_HEADER : "X-CSRF-Token",
	/**
	 * value of CCRF token that indicates that it is a command to remove it
	 */
	REMOVE_CSRF : "remove",
	/*
	 * server response back to client
	 */
	/**
	 * what happened to the request? REQUEST_OK means all OK, and response is
	 * data. Any other value means that the response is an array of messages
	 */
	REQUEST_STATUS : "_requestStatus",
	/**
	 * server trace being pumped to client
	 */
	TRACE_FIELD_NAME : "_trace",
	/*
	 * possible values for REQUEST_STATUS
	 */
	/**
	 * valid response is delivered as all was well. Any value other than this
	 * indicate that the response is an array of messages.
	 */
	REQUEST_OK : "0",
	/**
	 * time taken by this engine to execute this service in milliseconds
	 */
	SERVICE_EXECUTION_TIME : "_serviceExecutionTime",
	/**
	 * message type : some specific operation/action succeeded.
	 */
	MESSAGE_SUCCESS : "success",
	/**
	 * message type : general information.
	 */
	MESSGAE_INFO : "info",
	/**
	 * message type : warning/alert
	 */
	MESSAGE_WARNING : "warning",
	/**
	 * message type : ERROR
	 */
	MESSAGE_ERROR : "error",

	/*
	 * Some conventions used by server special features
	 */
	/**
	 * field name that directs a specific save action for the table/record
	 */
	TABLE_ACTION_FIELD_NAME : "_saveAction",
	/**
	 * tableSaveTask can get the action at run time
	 */
	TABLE_ACTION_ADD : "add",
	/**
	 * tableSaveTask can get the action at run time
	 */
	TABLE_ACTION_MODIFY : "modify",
	/**
	 * tableSaveTask can get the action at run time
	 */
	TABLE_ACTION_DELETE : "delete",
	/**
	 * tableSaveTask can get the action at run time
	 */
	TABLE_ACTION_SAVE : "save",

	/*
	 * filter-field is a special field that has associated comparator for
	 * communicating filtering criterion with the server. -------------- filter
	 * field comparators ----------------
	 */
	EQUAL : "=",
	NOT_EQUAL : "!=",
	LESS : "<",
	LESS_OR_EQUAL : "<=",
	GREATER : ">",
	GREATER_OR_EQUAL : ">=",
	LIKE : "~",
	STARTS_WITH : "^",
	BETWEEN : "><",

	/**
	 * suffix for the to-Field If field is "age" then to-field would be "ageTo"
	 */
	TO_FIELD_SUFFIX : "To",

	/**
	 * like ageComparator
	 */
	COMPARATOR_SUFFIX : "Comparator",
	/**
	 * comma separated names of columns that are to be used for sorting rows
	 */
	SORT_COLUMN_NAME : "_sortColumns",

	/**
	 * sort order asc or desc. asc is the default
	 */
	SORT_ORDER : "_sortOrder",
	/**
	 * ascending
	 */
	SORT_ORDER_ASC : "asc",
	/**
	 * descending
	 */
	SORT_ORDER_DESC : "desc",

	PAGINATION_SERVICE : "_p",

	/**
	 * if a service request wants a table to be paginated in its way back t
	 * client, then it should send the page size in a field names sheetName
	 * suffixed with this indicator
	 */
	PAGE_SIZE_SUFFIX : "PageSize",

	/**
	 * if a sheet is paginated, server would return total count of rows in an
	 * additional field for the sheet name with this suffix
	 */
	TOTAL_COUNT_SUFFIX : "TotalCount",

	/**
	 * field name that has the table name while requesting for a specific page
	 */
	PAGINATION_TABLE : "_tableName",

	/**
	 * field name that has the page size for pagination service
	 */
	PAGINATION_SIZE : "_pageSize",

	/**
	 * field name that has the page number for pagination service
	 */
	PAGINATION_PAGE_NUMBER : "_pageNumber",
	/**
	 * list service typically sends a key value
	 */
	LIST_SERVICE_KEY : "_key",

	/**
	 * should suggestion service suggest matching strings that start with the
	 * starting key?
	 */
	SUGGEST_STARTING : "_matchStarting",
	/**
	 * use this special name to indicate all fields whenever a list of fields is
	 * expected
	 */
	ALL_FIELDS : '_allFields',
};
/**
 * Simple way to get response from your service
 */
var Simplity = (function() {
	/**
	 * simple function to escape "<" ad "&" so that innerHTML is safe
	 * 
	 * @method
	 * @param {string}
	 *            text - text to be escaped
	 */
	var htmlEscape = function(text) {
		if (!text || !text.replace) {
			return text;
		}
		return text.replace(/&/g, "&amp;").replace(/</g, "&lt;");
	};

	/**
	 * @method log an error message.
	 * @param {string}
	 *            msg - to be logged
	 */
	var error = function(msg) {
		alert("ERROR\n" + msg);
	};

	/**
	 * @method log a warning text
	 * @param {string}
	 *            msg - to be logged
	 */
	var warn = function(msg) {
		alert("Warning\n" + msg);
	};

	/**
	 * @method. log a message
	 * @param {string}
	 *            msg - to be logged
	 * @param {boolean}
	 *            isHtml - if this is formatted as html. (In case log is written
	 *            out to as html, this msg is not escaped)
	 */
	var log = function(msg, isHtml) {
		console.log(msg);
	};
	/**
	 * @method pipe logging from our default console to your standard functions
	 * 
	 * @param {
	 *            function } errorFn - function for reporting error - required.
	 * @param {function}
	 *            warningFn - function for reporting warning - required.
	 * @param {function}
	 *            logFn - optional function for logging. If this is null, log
	 *            will continue to go to console
	 */
	var pipeLogging = function(errorFn, warnFn, logFn) {
		if (errorFn && warnFn) {
			error = errorFn;
			warn = warningFn;
		} else {
			reportError('You need to specify both errorFn and warningFn for re-plumbing logging with pipeLogging() method');
			return;
		}
		if (logFn) {
			log = logFn;
		}
	};

	var showMessage = function(message) {
		alert(message);
	};

	var showMessages = function(messages) {
		if (!messages || !messages.length) {
			alert('There are no messages');
		}
		var t = [];
		for (var i = 0; i < messages.length; i++) {
			var msg = messages[i];
			if (msg.messageType) {
				t.push(msg.messageType.toUpperCase());
				t.push('\n');
			}
			if(msg.name){
				t.push(msg.name);
				t.push(' : ');
			}
			t.push(msg.text);
			t.push('\n\n');
		}
		showMessage(t.join(''));
	};

	/**
	 * @method over-ride our alert to any exotic way of showing message.
	 * @param {function}
	 *            your function that over-rides the default
	 */
	var overrideShowMessage = function(fn) {
		showMessage = fn;
	};
	/**
	 * @method over-ride our alert to any exotic way of showing message.
	 * @param {function}
	 *            your function that over-rides the default
	 */
	var overrideShowMessages = function(fn) {
		showMessages = fn;
	};
	/**
	 * agent who knows how to contact server
	 */
	var METHOD = "POST";
	var URL = 's';
	var LOGIN_URL = 'i';
	var LOGOUT_URL = 'o';
	var TIMEOUT = 120000;
	/**
	 * this is the token to be flashed at the server security guard. This is
	 * obtained after a login.
	 */
	var token = null;
	/**
	 * @method login to server with credentials
	 * @param {text}
	 *            login name
	 * @param {text}
	 *            abrakadabra
	 */
	var login = function(userId, userToken) {
		var xhr = new XMLHttpRequest();
		xhr.onreadystatechange = function() {
			if (this.readyState == '4') {
				token = xhr.getResponseHeader(POCOL.CSRF_HEADER);
				if (token) {
					showMessages([{messageType:'success', text:'Successfully logged-in.'}]);
				} else {
					showMessage([{messageType:'error', name:'InvalidCredentials',text:
							'Ooops. Your credentials were not honoured. Please re-try.'}]);
				}
			}
		};
		xhr.ontimeout = function() {
			showMessages([{messageType:"error", name:"serverTimeout", text:
					"Sorry, there seem to be some red-tapism on the server. Can't wait any more for a decision. Giving up."}]);
		};
		try {
			xhr.open(METHOD, LOGIN_URL, true);
			xhr.timeout = TIMEOUT;
			xhr.setRequestHeader("Content-Type", "text/html; charset=utf-8");
			xhr.setRequestHeader(POCOL.USER_ID, userId);
			if (userToken) {
				xhr.setRequestHeader(POCOL.USER_TOKEN, userToken);
			}
			xhr.send('');
		} catch (e) {
			showMessages([{messageType:'error', text:'Unable to connect to server. Error : ' + e}]);
		}
	};

	/**
	 * @method one-way communication - we tell server to logout
	 */
	var logout = function() {
		var xhr = new XMLHttpRequest();
		try {
			xhr.open(METHOD, LOGOUT_URL, true);
			xhr.setRequestHeader("Content-Type", "text/html; charset=utf-8");
			if (token) {
				xhr.setRequestHeader(POCOL.USER_TOKEN, token);
			}
			xhr.send('');
		} catch (e) {
			showMessages([{text:'Unable to connect to server. Error : ' + e, messageType:'error'}]);
		}
		/*
		 * our token is no more valid
		 */
		token = null;
	};

	/**
	 * returns a promise for the specified service that is invoked with supplied
	 * data
	 */
	var getResponse = function(serviceName, data) {
		return new Promise(function(fullfill, reject) {
			if (!serviceName) {
				log('No service');
				reject([{
						messageType : 'error',
					text : 'No serviceName specified'
				}]);
				return;
			}
			log('Service ' + serviceName + ' invoked');
			var xhr = new XMLHttpRequest();
			xhr.onreadystatechange = function() {
				if (this.readyState != '4') {
					return;
				}
				/*
				 * any issue with HTTP Connection?
				 */
				if (xhr.status != 200 && xhr.status != 0) {
					log('non 200 status : ' + xhr.status);
					reject([{
						messageType : 'error',
						text : 'Our courier returned with an error code='
								+ xhr.status
								+ ' look at logs for more details.'
					}]);
					return;
				}
				/*
				 * any issue with our web agent?
				 */
				var st = xhr.getResponseHeader(POCOL.REQUEST_STATUS);
				if(st == null){
					reject([{text:'Unknown error on the server. It failed to indicate any status.', messageType:'error'}]);
					return;
				}
				var ms = xhr.getResponseHeader(POCOL.SERVICE_EXECUTION_TIME);
				if(ms){
					log('server claims it took ' + ms +'ms');
				}
				var json = xhr.responseText ? JSON.parse(xhr.responseText) : {};
				if (st === POCOL.REQUEST_OK) {
					fullfill(json);
				} else {
					log('service returned with status=' + st);
					reject(json);
				}
			};
			xhr.ontimeout = function() {
				log('time out');
				reject([{messageType : 'error', text:'Sorry, there seem to be some red-tapism on the server. giving-up'}]);
			};
			try {
				xhr.open(METHOD, URL, true);
				xhr.timeout = TIMEOUT;
				xhr
						.setRequestHeader("Content-Type",
								"text/html; charset=utf-8");
				xhr.setRequestHeader(POCOL.SERVICE_NAME, serviceName);
				if (token) {
					xhr.setRequestHeader(POCOL.USER_TOKEN, token);
				}
				xhr.send(data);
			} catch (e) {
				error("error during xhr : " + e);
				reject([ {
					text : 'Unable to connect to server. Error : ' + e,
					messageType : 'error'
				} ]);
			}

		});
	};

	/*
	 * what we want to expose as API
	 */
	return {
		htmlEscape : htmlEscape,
		log : log,
		error : error,
		warn : warn,
		pipeLogging : pipeLogging,
		showMessage : showMessage,
		showMessages : showMessages,
		overrideShowMessage : overrideShowMessage,
		overrideShowMessages : overrideShowMessages,
		getResponse : getResponse,
		login : login,
		logout : logout,
	};
})();
