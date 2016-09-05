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
	/**
	 * non-error messages returned along with data. like {...,
	 * "_mesages":[{}..]...}
	 */
	TRACE_FIELD_NAME : "_messages",
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
	 * @param {Function}
	 *            warningFn - function for reporting warning - required.
	 * @param {Function}
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
			if (msg.name) {
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
	 * @param {Function}
	 *            your function that over-rides the default
	 */
	var overrideShowMessage = function(fn) {
		showMessage = fn;
	};
	/**
	 * @method over-ride our alert to any exotic way of showing message.
	 * @param {Function}
	 *            your function that over-rides the default
	 */
	var overrideShowMessages = function(fn) {
		showMessages = fn;
	};

	var ROW_PART_DELIM = '__';
	/**
	 * @method push the data into dynamic areas of the page. we use a simple
	 *         naming convention for this.
	 * 
	 * <pre>
	 * for fields: 
	 * if a tag with that id is found, value is set (either as value or innerHTML). 
	 * However, if this tag has an attribute named 'data-value' then the value is set
	 * to that attribute instead of value/innerHTML.
	 * 
	 * If no tag is found, we try a tag with id fieldName-true and fieldName-false. 
	 * If found, the tag is shown/hidden based on the field value
	 * 
	 * for tables : if a tag is is found with this id, 
	 * 	a. we clone that tag for each row in the table. cloned tag has an attribute named
	 * 		'data-idx' that is 1-based index into the table, and its id is set to 'tableName-idx'
	 *      e.g. data-idx='3', id='orderLine-3'...
	 *      
	 *  b. push data form rows to these tags by replacing --colName-- with value of that cell
	 *  c. replace the innerHTML of parent with all these
	 *  d. hide the original row with id
	 * 
	 * </pre>
	 */
	var pushDataToPage = function(json, doc) {
		if (Array.isArray(json)) {
			log('Simplity funciton pushDataToPage() requires that the json is an object-map, but not an array. Data not pushed to page');
			return;
		}

		if (!doc) {
			doc = window.document;
		}
		var nbrUnused = 0;
		/*
		 * go with each attribute of the json
		 */
		for ( var att in json) {
			var val = json[att];
			log(att + ' = ' + val);
			var ele;
			if (Array.isArray(val)) {
				/*
				 * array can be source for a dom element with id="__att__"
				 */
				ele = doc.getElementById(ROW_PART_DELIM + att + ROW_PART_DELIM);
				if (ele) {
					setTableToEle(ele, val, att);
					continue;
				}
				/*
				 * if we did not find an element for this table, we will try
				 * other options later
				 */
				log('No destinaiton found for table ' + att);
				nbrUnused++;
				continue;
			}
			/*
			 * it could be a primitive, or an object. In any case, we need an
			 * element
			 */
			var ele = doc.getElementById(att);
			if (ele) {
				setValueToEle(ele, att, val);
				continue;
			}
			/*
			 * we try show-hide element for this
			 */
			var ele1 = doc.getElementById(att + '-true');
			var ele2 = doc.getElementById(att + '-false');
			if (ele1 || ele2) {
				showOrHideEle(ele1, ele2, val);
				continue;
			}
			/*
			 * no destination found
			 */
			log(att + ' has a value of ' + val
					+ ' but we do not know what to do with this');
		}
		if (!nbrUnused) {
			return;
		}
		/*
		 * remaining tables could be for drop-downs
		 */
		var selectTables = getSelectSources(doc);
		if (!selectTables) {
			return;
		}

		for (att in selectTables) {
			var table = json[att];
			if (!table) {
				continue;
			}
			var eles = selectTables[att];
			for (var i = eles.length - 1; i >= 0; i--) {
				setOptionsForEle(eles[i], table);
			}
		}
	};

	var setOptionsForEle = function(ele, vals) {

	};

	/**
	 * @method assign rows data to a dom element. row is of the form <tag
	 *         id="__thisId__"...><tag1>__colName1__</tag1><tag2
	 *         data-value="__colName2__...
	 * @param {DOMElement}
	 *            ele
	 * @param {Array}
	 *            table - data with first row as header
	 */
	var setTableToEle = function(ele, table, tableName) {
		var nbrRows = table.length;
		/*
		 * We have to remove existing rows. that is by setting innerHTML of
		 * parent to outerHTML of this row.
		 */
		ele.style.display = 'none';
		if (nbrRows < 2) {
			ele.parentNode.innerHTML = ele.outerHTML;
			return;
		}
		var txt = ele.outerHTML;
		/*
		 * t is the array that will have the new intterHTMl fpr parent. Let us
		 * push the master row first
		 */
		var t = [ txt ];
		/*
		 * ele has to be cloned. So we take outer html.
		 */
		var parts = txt.split(ROW_PART_DELIM);
		/*
		 * parts[0] is <tag... id=" , parts[1] is namme, then we will have
		 * pairs: parts[n] is html, and parts[n+1] column name... last part is
		 * html. so we have to have odd number of parts.
		 */
		var nbrParts = parts.length;
		if (nbrParts % 2 !== 1) {
			error('Element for table '
					+ tableName
					+ ' does not follow the convention proerly. Look for pairs of '
					+ ROW_PART_DELIM);
			return;
		}
		for (var i = 0; i < nbrRows; i++) {
			var row = table[i];
			t.push(parts[0]);
			/*
			 * push id
			 */
			t.push(tableName + '_' + i);
			t.push(parts[2]);
			/*
			 * push pairs of column value an dhtml
			 */
			var idx = 3;
			while (idx < nbrParts) {
				t.push(parts[idx++]);
				var colName = parts[idx++];
				if (colName === 'i') {
					/*
					 * special name for just pushing 1-based index
					 */
					t.push(i + 1);
				} else {
					t.push(convertValue(row[colName]));
				}
			}
		}
		ele.parentNode.innerHTML = t.join('');
	};

	/**
	 * @method convert server value to local value. We convert date to local
	 *         date format
	 * @param {object}
	 *            val to be converted
	 * @return {string}
	 */
	var convertValue = function(val) {
		if (val && val.toLocaleDateString) {
			val = val.toLocaleDateString();
		}
		return val;
	};
	/*
	 * @method get sources of list values for select/drop-downs @param
	 * {Document} doc @returns collection of arrays of ele.ids indexed by the
	 * table names. null if no select tag uses data-table.
	 */
	var getSelectSources = function(doc) {
		var eles = doc.getElementsByTagName('select');
		var nbr = 0;
		var sources = {};
		for (var i = eles.length - 1; i >= 0; i--) {
			var ele = eles[i];
			var att = ele.getAttribute('data-table');
			if (att) {
				var list = sources[att];
				if (!list) {
					list = sources[att] = [];
				}
				list.push(ele.id);
				nbr++;
			}
		}
		if (nbr) {
			return sources;
		}
		return null;
	};

	/**
	 * @method Hide or show complementary elements based on field value
	 * @param {boolean}
	 *            val
	 * @param {DOMElement}
	 *            eleTrue element to be shown on tue, and hidden on false
	 * @param {DOMElement}
	 *            element to be hidden on true, and shown on false
	 */
	var showOrHideEle = function(val, eleTrue, eleFalse) {
		if (eleTrue) {
			eleTrue.style.display == val ? '' : 'none';
		}
		if (eleFalse) {
			eleFalse.style.display == val ? 'none' : '';
		}
	};
	/**
	 * @method push value to a field
	 * @param {DOMElement}
	 *            ele
	 * @param {string}
	 *            fieldName
	 * @param {string}
	 *            fieldValue
	 */
	var setValueToEle = function(ele, fieldName, fieldValue) {
		var tag = ele.tagName.toLowerCase();
		log('Trying a value of ' + fieldValue + ' for a ' + tag + " with id "
				+ fieldName);
		/*
		 * most common - input field
		 */
		if (tag === 'input') {
			if (ele.type.toLowerCase() === 'checkbox') {
				ele.checked = ele.value ? true : false;
			} else {
				ele.value = value;
			}
			return;
		}
		/*
		 * drop-down, possibly multi
		 */
		if (tag === 'select') {
			if (ele.multiple) {
				var vals = getVals(fieldValue);
				var el = ele.firstChild;
				while (el) {
					if (vals[el.value]) {
						ele.setAttribute('selected', 'selected');
					} else {
						ele.removeAttribute('selected');
					}
					el = el.nextSibling;
				}
			} else {
				ele.value = fieldValue;
			}
			return;
		}
		/*
		 * meant for changing style
		 */
		if (ele.hasAttribute('data-value')) {
			ele.setAttribute('data-value', fieldValue);
			return;
		}
		/*
		 * a radio group
		 */
		if (ele.hasAttribute('data-radio')) {
			var eles = ele.getElementsByTagName('input');
			if (!eles.length) {
				log('we found tag for '
						+ fieldName
						+ ' as data-radio but it does not have radio child nodes');
				return;
			}
			for (var i = eles.length - 1; i >= 0; i--) {
				var el = eles[i];
				el.checked = el.value == fieldValue;
			}
			return;
		}
		/*
		 * check-box group that takes comma separated list of values
		 */
		if (ele.hasAttribute('data-checkbox')) {
			var vals = getVals(fieldValue);
			var eles = ele.getElementsByTagName('input');
			if (!eles.length) {
				log('we found tag for ' + fieldName
						+ ' but it does not have check-box child nodes');
				return;
			}
			for (var i = eles.length - 1; i >= 0; i--) {
				var el = eles[i];
				el.checked = vals[el.value];
			}
			return;
		}
		/*
		 * when all else fails, we have innerHTML..
		 */
		ele.innerHTML = htmlEscape(fieldValue);

	};

	/**
	 * agent who knows how to contact server
	 */
	var METHOD = "POST";
	var URL = 'a._s';
	var LOGIN_URL = 'a._i';
	var LOGOUT_URL = 'a._o';
	var TIMEOUT = 12000;
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
					showMessages([ {
						messageType : 'success',
						text : 'Successfully logged-in.'
					} ]);
				} else {
					showMessage([ {
						messageType : 'error',
						name : 'InvalidCredentials',
						text : 'Ooops. Your credentials were not honoured. Please re-try.'
					} ]);
				}
			}
		};
		xhr.ontimeout = function() {
			showMessages([ {
				messageType : "error",
				name : "serverTimeout",
				text : "Sorry, there seem to be some red-tapism on the server. Can't wait any more for a decision. Giving up."
			} ]);
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
			showMessages([ {
				messageType : 'error',
				text : 'Unable to connect to server. Error : ' + e
			} ]);
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
			showMessages([ {
				text : 'Unable to connect to server. Error : ' + e,
				messageType : 'error'
			} ]);
		}
		/*
		 * our token is no more valid
		 */
		token = null;
	};

	/**
	 * gets response from server for the service and invokes call-back function
	 * with the response
	 * 
	 * @param {string}
	 *            serviceName qualified service name to be invoked
	 * @param {string}
	 *            optional json string to be sent to server as input for this
	 *            service
	 * @param {Function}
	 *            onSuccess function is called with jsonObject (not json string)
	 *            that is returned from server. if this is not specified,
	 *            Simplity.pushDataToPage() is used.
	 * @param {Function}
	 *            onError optional. function that is invoked in case of any
	 *            error. it is called with an array of message objects.
	 *            Simplity.showMessages() is used as default.
	 */
	var getResponse = function(serviceName, data, onSuccess, onError) {
		onSuccess = onSuccess || pushDataToPage;
		onError = onError || showMessages;
		if (!serviceName) {
			log('No service');
			onError([ {
				messageType : 'error',
				text : 'No serviceName specified'
			} ]);
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
				onError([ {
					messageType : 'error',
					text : 'Our courier returned with an error code='
							+ xhr.status + ' look at logs for more details.'
				} ]);
				return;
			}
			/*
			 * any issue with our web agent?
			 */
			var st = xhr.getResponseHeader(POCOL.REQUEST_STATUS);
			if (st == null) {
				onError([ {
					text : 'Unknown error on the server. It failed to indicate any status.',
					messageType : 'error'
				} ]);
				return;
			}

			var json = {};
			if (xhr.responseText) {
				json = JSON.parse(xhr.responseText);
			}
			if (st === POCOL.REQUEST_OK) {
				onSuccess(json);
			} else {
				log('service returned with status=' + st);
				onError(json);
			}
		};
		xhr.ontimeout = function() {
			log('time out');
			onError([ {
				messageType : 'error',
				text : 'Sorry, there seem to be some red-tapism on the server. giving-up'
			} ]);
		};
		try {
			xhr.open(METHOD, URL, true);
			xhr.timeout = TIMEOUT;
			xhr.setRequestHeader("Content-Type", "text/html; charset=utf-8");
			xhr.setRequestHeader(POCOL.SERVICE_NAME, serviceName);
			if (token) {
				xhr.setRequestHeader(POCOL.USER_TOKEN, token);
			}
			xhr.send(data);
		} catch (e) {
			error("error during xhr : " + e);
			onError([ {
				text : 'Unable to connect to server. Error : ' + e,
				messageType : 'error'
			} ]);
		}
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
		pushDataToPage : pushDataToPage,
	};
})();
