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
/**
 * conventions while communicating with a simplity based server
 * 
 */
var POCOL = {
	/**
	 * as of now, we are hard coding. we do not see a need to make it flexible..
	 */
	"CHAR_ENCODING" : "UTF-8"

	/*
	 * client to server.
	 */
	/**
	 * name of service to be executed
	 */
	,SERVICE_NAME : "_serviceName"

	/**
	 * used for login-process.
	 */
	,USER_TOKEN : "_userToken"

	/**
	 * file-upload call, header field for optional name of file
	 */
	,FILE_NAME : "_fileName"

	/**
	 * mime type of the file being uploaded.
	 */
	,MIME_TYPE : "_mimeType"

	/*
	 * server response back to client
	 */
	/**
	 * what happened to the request? REQUEST_OK means all OK, and response is
	 * data. Any other value means that the response is an array of messages
	 */
	,REQUEST_STATUS : "_requestStatus"

	/**
	 * messages returned along with data. like {..., "_mesages":[{}..]...}
	 */
	,MESSAGES : "_messages"
	/*
	 * possible values for status field in response
	 */
	/**
	 * valid response is delivered as all is well. There may be messages, but
	 * they are not error messages.
	 */
	,STATUS_OK : 'ok'
	/**
	 * Error. Either service related issues, like validation error, or server
	 * error like internal error. Usually, response will only have messages.
	 */
	,STATUS_ERROR : 'error'
	/**
	 * time taken by this engine to execute this service in milliseconds
	 */
	,SERVICE_EXECUTION_TIME : "_serviceExecutionTime"
	/**
	 * message type : some specific operation/action succeeded.
	 */
	,MESSAGE_SUCCESS : "success"
	/**
	 * message type : general information.
	 */
	,MESSGAE_INFO : "info"
	/**
	 * message type : warning/alert
	 */
	,MESSAGE_WARNING : "warning"
	/**
	 * message type : ERROR
	 */
	,MESSAGE_ERROR : "error"

	/*
	 * Some conventions used by server special features
	 */
	/**
	 * field name that directs a specific save action for the table
	 */
	,TABLE_ACTION_FIELD_NAME : "_saveAction"
	/**
	 * tableSaveTask can get the action at run time
	 */
	,TABLE_ACTION_ADD : "add"
	/**
	 * tableSaveTask can get the action at run time
	 */
	,TABLE_ACTION_MODIFY : "modify"
	/**
	 * tableSaveTask can get the action at run time
	 */
	,TABLE_ACTION_DELETE : "delete"
	/**
	 * tableSaveTask can get the action at run time
	 */
	,TABLE_ACTION_SAVE : "save"

	/*
	 * filter-field is a special field that has associated comparator for
	 * communicating filtering criterion with the server. -------------- filter
	 * field comparators ----------------
	 */
	,EQUAL : "="
	,NOT_EQUAL : "!="
	,LESS : "<"
	,LESS_OR_EQUAL : "<="
	,GREATER : ">"
	,GREATER_OR_EQUAL : ">="
	,LIKE : "~"
	,STARTS_WITH : "^"
	,BETWEEN : "><"
	,IN_LIST : "@"

	/**
	 * suffix for the to-Field If field is "age" then to-field would be "ageTo"
	 */
	,TO_FIELD_SUFFIX : "To"

	/**
	 * like ageComparator
	 */
	,COMPARATOR_SUFFIX : "Comparator"
	/**
	 * comma separated names of columns that are to be used for sorting rows
	 */
	,SORT_COLUMN_NAME : "_sortColumns"

	/**
	 * sort order asc or desc. asc is the default
	 */
	,SORT_ORDER : "_sortOrder"
	/**
	 * ascending
	 */
	,SORT_ORDER_ASC : "asc"
	/**
	 * descending
	 */
	,SORT_ORDER_DESC : "desc"

	/**
	 * list service typically sends a key value
	 */
	,LIST_SERVICE_KEY : "_key"

	/**
	 * should suggestion service suggest matching strings that start with the
	 * starting key?
	 */
	,SUGGEST_STARTING : "_matchStarting"
	/**
	 * use this special name to indicate all fields whenever a list of fields is
	 * expected
	 */
	,ALL_FIELDS : '_allFields'

	/**
	 * mark column names in tables like __colName__
	 */
	,COL_MARKER : '__'
	/**
	 * mark a space to put the index suffix. like row__i__ to suffix row with _i
	 * where is the row number
	 */
	,IDX_MARKER : 'i'
	/**
	 * attribute name to mark an element as target for a data table
	 */
	,DATA_TABLE : 'data-table'
	/**
	 * attribute to mark an element as row for a table. this element is repeated
	 * for each row of a table
	 */
	,DATA_ROW : 'data-row'
	/**
	 * attribute to indicate that this table is hierarchical
	 */
	,HAS_CHILDREN : 'data-has-children'
	/**
	 * attribute set to true to hide this element if the associated table has no
	 * data. Otherwise, only the row element is hidden, and the table element is
	 * shown. For example, if this is not set, you may see the table header row,
	 * but no data
	 */
	,HIDE_IF_NO_DATA : 'data-hide-if-no-data'
	/**
	 * window level var that has the value of the last json object response.
	 * This gets replaced with the next response
	 */
	,LAST_JSON : '_lastJson'

	/**
	 * name with which a json is saved in local storage (sessionStorage?) for
	 * db.
	 */
	,LOCAL_STORAGE_NAME : '_localData'
	/**
	 * name of the object in page-specific script that has functions for service
	 * indexed by serviceName
	 */
	,LOCAL_SERVICES : '_localServices'
	/**
	 * name of the object in page-specific script that has ready response object
	 * for service indexed by serviceName
	 */
	,LOCAL_RESPONSES : '_localResponses'
};
/**
 * Simple way to get response from your service
 */
var Simplity = (function() {
	var htmlEscape = function(txt) {
		if (!txt || !txt.replace) {
			return txt;
		}
		return txt.replace(/&/g, '&amp;').replace(/</g, '&lt;');
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
			if (msg.fieldName) {
				t.push('Field : ');
				t.push(msg.fieldName);
				t.push(' - ');
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
		var selectTables = getSelectSources(doc);
		/*
		 * go with each attribute of the json
		 */
		for ( var att in json) {
			/*
			 * attribute starting with _ are not ours
			 */
			if (att.indexOf('_') === 0) {
				log('Ignoring the reserved attribute ' + att);
				continue;
			}
			var val = json[att];
			var ele;
			/*
			 * we have an issue with null. Let us try that as value as well as
			 * empty array
			 */
			if (val === null || Array.isArray(val)) {
				if (!val) {
					val = [];
				}
				/*
				 * array can be used in two ways. Simple table or hierarchical.
				 * can be source for a dom element with id="__att__"
				 */
				ele = doc.getElementById(POCOL.COL_MARKER + att
						+ POCOL.COL_MARKER);
				if (ele) {
					setDataToTable(ele, val || [], att);
					continue;
				}
				ele = doc.getElementById(att);
				if (ele && ele.getAttribute(POCOL.DATA_TABLE)) {
					setHierarchicalDataToTable(ele, val || [], att);
					continue;
				}
				var eles = selectTables && selectTables[att];
				if (eles) {
					for (var i = eles.length - 1; i >= 0; i--) {
						setOptionsForEle(eles[i], val || []);
					}
					continue;
				}
				/*
				 * if we did not find an element for this table, we will try
				 * other options later
				 */
				if (val) {
					log('No destination found for table ' + att);
					continue;
				}
			}
			/*
			 * it could be a primitive, or an object. In any case, we need an
			 * element
			 */
			ele = doc.getElementById(att);
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
		}
	};

	var setOptionsForEle = function(ele, data) {
		var opt = ele.options;
		var val = null;
		if (opt && opt.length) {
			opt = opt[ele.selectedIndex];
			val = opt && opt.value;
		}
		var t = [];
		var n = data.length;
		for (var i = 0; i < n; i++) {
			var pair = data[i];
			t.push('<option value="');
			t.push(pair.key);
			if (val && val == pair.key) {
				val = null;
				t.push('" selected="selected">');
			} else {
				t.push('">');
			}
			t.push(htmlEscape(pair.value));
			t.push('</option>');
		}
		ele.innerHTML = t.join('');
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
	var setDataToTable = function(ele, table, tableName) {
		var nbrRows = table.length;
		/*
		 * We have to remove existing rows. that is by setting innerHTML of
		 * parent to outerHTML of this row.
		 */
		ele.style.display = 'none';
		var parentEle = ele.parentNode;
		if (!nbrRows) {
			log('No data in table ' + tableName);
			parentEle.innerHTML = ele.outerHTML;
			return;
		}
		/*
		 * we are going to manipulate ele.parentNode. Let us hide it when we are
		 * doing that....
		 */
		parentEle.style.display = 'none';
		/*
		 * t is the array that will have the new innerHTML for parent. Let us
		 * push the master row first
		 */
		var t = [ ele.outerHTML ];

		/*
		 * we clone the innerHTML of master row, but not when it is hidden !!
		 */
		ele.style.display = '';
		/*
		 * ele has to be cloned. So we take outer html.
		 */
		var parts = ele.outerHTML.split(POCOL.COL_MARKER);
		/*
		 * parts[0] is <tag... id=" , parts[1] is name, then we will have pairs:
		 * parts[n] is html, and parts[n+1] column name... last part is html. so
		 * we have to have odd number of parts.
		 */
		var nbrParts = parts.length;
		if (nbrParts % 2 !== 1) {
			error('Element for table '
					+ tableName
					+ ' does not follow the convention proerly. Look for pairs of '
					+ POCOL.COL_MARKER);
			return;
		}
		for (var i = 0; i < nbrRows; i++) {
			var row = table[i];
			t.push(parts[0]);
			/*
			 * push id in place of part[1]
			 */
			t.push(tableName + '_' + i);
			t.push(parts[2]);
			/*
			 * push pairs of column value and html
			 */
			var idx = 3;
			while (idx < nbrParts) {
				var colName = parts[idx];
				idx++;
				if (colName === 'i') {
					/*
					 * special name for just pushing 1-based index
					 */
					t.push(i + 1);
				} else {
					if (row.hasOwnProperty(colName)) {
						t.push(convertValue(row[colName]));
					} else {
						log('No value for column ' + colName
								+ '. cell value set to empty string');
					}
				}
				t.push(parts[idx]);
				idx++;

			}
		}
		parentEle.innerHTML = t.join('');
		ele.style.display = 'none';
		parentEle.style.display = '';
	};

	/**
	 * set data from datarows to the node-tree with tableEle
	 * 
	 * @param {DOMElement}
	 *            tableEle root dom-element for thid table
	 * @param {Array}
	 *            dataRows
	 * @param {string}
	 *            name of table. this matches the attribute name
	 */
	var setHierarchicalDataToTable = function(tableEle, dataRows, tableName) {
		var rowEle = getRowEle(tableEle);
		var parentEle = rowEle.parentNode;
		/*
		 * handle no-data situation first..
		 */
		if (!dataRows || !dataRows.length) {
			if (tableEle.hasAttribute(POCOL.HIDE_IF_NO_DATA)) {
				log('No data in table ' + tableName
						+ ' and hence we are hiding the table element');
				tableEle.style.display = 'none';
			} else {
				log('No data in table ' + tableName);
				rowEle.style.display = 'none';
				parentEle.innerHTML = rowEle.outerHTML;
			}
			return;
		}

		var parts = getPartsForTable(tableEle, tableName, false);
		var html = [];
		addHtmlForParts(parts, {}, dataRows, html, '');
		parentEle.innerHTML = html.join('');
		/*
		 * render parentEle if required
		 */
		if (tableEle.hasAttribute(POCOL.HIDE_IF_NO_DATA)) {
			tableEle.style.display = '';
		}
		return;
	};

	/**
	 * get descendant ele marked as data-row
	 * 
	 * @param {DOMElement}
	 *            tableEle
	 * @return {DOMElement} that is marked as data-row, or null if no such
	 *         element
	 */
	var getRowEle = function(tableEle) {
		var child = tableEle.firstChild;
		while (child) {
			/*
			 * we end-up iterating thru non-elements but we avoided using
			 * element-based methods for sake of compatibility
			 */
			if (child.hasAttribute) {
				if (child.hasAttribute(POCOL.DATA_ROW)) {
					return child;
				}
				/*
				 * this is an ele. one of its child may be a row ele.
				 */
				var rowEle = getRowEle(child);
				if (rowEle) {
					return rowEle;
				}
			}
			child = child.nextSibling;
		}
		return null;
	};
	var MARKER_FOR_SPLITTING = '___';
	/**
	 * @method {getPartsForTable} get an array of part objcts that help us in
	 *         building innerHTMl to bind data to a table
	 * @param {DOMElement}
	 *            tableEle root dom element that is marked as data-table
	 * @param {string}
	 *            tableName name of this table
	 * @param {boolean}
	 *            isChildTable true if this is a childTable
	 */
	var getPartsForTable = function(tableEle, tableName, isChildTable) {
		var rowEle = getRowEle(tableEle);
		if (!rowEle) {
			error("Element "
					+ tableEle.id
					+ " is marked as data-table, but it does not have a child ele marked as data-row");
			return null;
		}
		var parts = null;
		parts = [];
		var endTag = null;
		if (isChildTable) {
			var parentEle = rowEle.parentNode;
			parentEle.insertBefore(document
					.createTextNode(MARKER_FOR_SPLITTING), rowEle);
			parentEle.removeChild(rowEle);
			var t = tableEle.outerHTML.split(MARKER_FOR_SPLITTING);
			if (t.length != 2) {
				error('we assume that you have no need to use three underscores together anywhere, but found one such instance in your html');
				return null;
			}
			addPartsFromHtml(parts, t[0]);
			endTag = t[1];
		}
		rowEle.style.display = 'none';
		parts.push({
			html : rowEle.outerHTML
		});
		rowEle.style.display = '';
		parts.startAt = parts.length;
		if (tableEle.hasAttribute(POCOL.HAS_CHILDREN)) {
			addPartsForComplexRow(parts, rowEle);
		} else {
			addPartsFromHtml(parts, rowEle.outerHTML);
		}
		parts.endBefore = parts.length;
		if (endTag) {
			addPartsFromHtml(parts, endTag);
		}
		return parts;
	};

	/**
	 * split html into parts that require data
	 * 
	 * @param {Array}
	 *            parts to whch we have to add new parts
	 * @param {string}
	 *            html that is to be converted to parts
	 */
	var addPartsFromHtml = function(parts, html) {
		var t = html.split(POCOL.COL_MARKER);
		var n = t.length;
		if (n % 2 !== 1) {
			error("We assume that you use double underscore as delimiter for marking tabl elements, like __colName__, Since this is ot true, we are unabel to set data to table.");
			return;
		}
		parts.push({
			html : t[0]
		});
		for (var i = 1; i < n; i++) {
			/*
			 * push a pair of col and html.
			 */
			var col = t[i];
			if (col === POCOL.IDX_MARKER) {
				parts.push({
					id : true
				});
			} else {
				parts.push({
					col : col
				});
			}
			i++;
			parts.push({
				html : t[i]
			});
		}
		return;
	};

	/**
	 * add parts for a row that has one or more child-tables (hierarchical data)
	 * 
	 * @param {Array}
	 *            parts
	 * @param {DOMElement}
	 *            rowEle
	 */
	var addPartsForComplexRow = function(parts, rowEle) {
		/*
		 * we need the begin-tag and end-tag for rowEle. Since there are no
		 * methods, we do some wizardry
		 */
		var ele = rowEle.cloneNode();
		ele.innerHTML = MARKER_FOR_SPLITTING;
		var tagParts = ele.outerHTML.split(MARKER_FOR_SPLITTING);
		if (tagParts.length != 2) {
			error("Our algorithm for setting data to table assumes that your html has no triple underscores. Please remove them from your html");
			return;
		}
		/*
		 * we accumulate outerHTML up to the child table into an array. Start
		 * with the openeing tag of rowEle
		 */
		var t = [ tagParts[0] ];
		/*
		 * we have to get parts for html upto, but excluding the first
		 * child-table. We go y each child element.
		 */
		ele = rowEle.firstChild;
		while (true) {
			if (!ele) {
				/*
				 * that was the last child. we are done.
				 */
				if (t.length) {
					addPartsFromHtml(parts, t.join(''));
				}
				break;
			}
			/*
			 * watch out for non-elements like text elements
			 */
			if (!ele.getAttribute) {
				t.push(ele.nodeValue);
			} else {
				var tableName = ele.getAttribute(POCOL.DATA_TABLE);
				if (!tableName) {
					t.push(ele.outerHTML);
				} else {
					/*
					 * this is a child table. Push accumulated htmls first.
					 */
					if (t.length) {
						addPartsFromHtml(parts, t.join(''));
						t = [];
					}

					var childParts = getPartsForTable(ele, tableName, true);
					parts.push({
						child : tableName,
						parts : childParts
					});
				}
			}
			ele = ele.nextSibling;
		}
		parts.push({
			html : tagParts[1]
		});
		return;
	};

	/**
	 * push html based on parts and data
	 * 
	 * @param {Array}
	 *            parts
	 * @param {Array}
	 *            dataRows
	 * @param {Array}
	 *            html to which htmls to be pushed
	 * @param {string}
	 *            iSuffix to be pushed for part that want id
	 */
	var addHtmlForParts = function(parts, parentData, dataRows, html, idSuffix) {
		/*
		 * push htmls that do not repeat
		 */
		var endAt = parts.startAt;
		for (var i = 0; i < endAt; i++) {
			html.push(getHtmlForPart(parts[i], parentData, idSuffix));
		}
		var endAt = parts.endBefore;
		var nbrRows = !dataRows ? 0 : dataRows.length;
		for (var j = 0; j < nbrRows; j++) {
			var row = dataRows[j];
			var newSuffix = idSuffix + '_' + j;
			for (var i = parts.startAt; i < endAt; i++) {
				var part = parts[i];
				if (part.child) {
					addHtmlForParts(part.parts, row, row[part.child], html,
							newSuffix);
				} else {
					html.push(getHtmlForPart(part, row, newSuffix));
				}
			}
		}
		endAt = parts.length;
		for (var i = parts.endBefore; i < endAt; i++) {
			html.push(getHtmlForPart(parts[i], parentData, idSuffix));
		}
	};

	/**
	 * @method getHtmlForPart get an html for the part and data
	 * @param {Part}
	 *            part
	 * @param {Object}
	 *            data
	 * @param {string}
	 *            suffix to be used for id
	 * @return {string} html to be appended
	 */
	var getHtmlForPart = function(part, data, idSuffix) {
		var txt = part.html;
		if (txt) {
			return txt;
		}
		if (part.id) {
			return idSuffix;
		}
		txt = part.col;
		if (data && data.hasOwnProperty(txt)) {
			return convertValue(data[txt]);
		}
		return '';
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
		} else {
			val = htmlEscape(val);
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
			var att = ele.getAttribute(POCOL.DATA_TABLE);
			if (att) {
				var list = sources[att];
				if (!list) {
					list = sources[att] = [];
				}
				list.push(ele);
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
		if (fieldValue == null) {
			fieldVaue = '';
		}
		var tag = ele.tagName.toLowerCase();
		/*
		 * most common - input field
		 */
		if (tag === 'input') {
			if (ele.type.toLowerCase() === 'checkbox') {
				ele.checked = ele.value ? true : false;
			} else {
				ele.value = fieldValue;
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
		 * when all else fails, we have textContent..
		 */
		ele.textContent = fieldValue;

	};

	/**
	 * agent who knows how to contact server
	 */
	var METHOD = "POST";
	var URL = 'a/a._s';
	var TIMEOUT = 12000;

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
	 *            successFn function is called with jsonObject (not json string)
	 *            that is returned from server. if this is not specified,
	 *            Simplity.pushDataToPage() is used.
	 * @param {Function}
	 *            failureFn optional. function that is invoked in case of any
	 *            error. it is called with an array of message objects.
	 *            Simplity.showMessages() is used as default.
	 */
	var getResponse = function(serviceName, data, successFn, failureFn) {
		successFn = successFn || pushDataToPage;
		failureFn = failureFn || showMessages;
		if (!serviceName) {
			log('No service');
			failureFn(createMessageArray('No serviceName specified'));
			return;
		}
		log('Service ' + serviceName + ' invoked');
		var xhr = new XMLHttpRequest();
		xhr.onreadystatechange = function() {
			if (this.readyState != '4') {
				return;
			}
			var json = {};
			if (xhr.responseText) {
				try {
					json = JSON.parse(xhr.responseText);
				} catch (e) {
					log('Response is not json. response text is returned instead of js object....');
					/*
					 * utility services may use non-jsons
					 */
					json = xhr.responseText;
				}
			}
			/*
			 * any issue with our web agent?
			 */
			if (xhr.status && xhr.status != 200) {
				log('HTTP error from server (non-200)\n' + xhr.responseText);
				failureFn(createMessageArray('Server or the communication infrastructure has failed to respond.'));
				return;
			}
			var st = json[POCOL.REQUEST_STATUS] || POCOL.STATUS_OK;
			if (st == POCOL.STATUS_OK) {
				window[POCOL.LAST_JSON] = json;
				log('Json saved as ' + POCOL.LAST_JSON);
				successFn(json);
				return;
			}
			if (st == POCOL.STATUS_NO_LOGIN) {
				if (reloginFunction) {
					log('Login required. invoking relogin');
					reloginFunction(serviceName, data, successFn, failureFn);
					return;
				}
				failureFn(createMessageArray('This service requires a valid login. Please login and try again.'));
				return;
			}
			var msgs = json[POCOL.MESSAGES];
			if (!msgs) {
				msgs = createMessageArray('Server reported a failure, but did not specify any error text.');
			}
			failureFn(msgs);
		};
		xhr.ontimeout = function() {
			log('time out');
			failureFn(createMessageArray('Sorry, there seem to be some red-tapism on the server. giving-up'));
		};
		try {
			xhr.open(METHOD, URL, true);
			xhr.timeout = TIMEOUT;
			xhr.setRequestHeader("Content-Type", "application/json; charset=utf-8");
			xhr.setRequestHeader(POCOL.SERVICE_NAME, serviceName);
			xhr.send(data);
		} catch (e) {
			log("error during xhr : " + e.message);
			failureFn(createMessageArray('Unable to connect to server. Error : '
					+ e.message));
		}
	};

	/**
	 * create an array with a message object with the supplied message text
	 */
	var createMessageArray = function(messageText) {
		return [ {
			messageType : 'error',
			text : messageText
		} ];
	};
	/**
	 * create a response with an error message
	 */
	var createErrorResponse = function(messageText) {
		var resp = {};
		resp[POCOL.REQUEST_STATUS] = POCOL.STATUS_ERROR;
		resp[POCOL.MESSAGES] = createMessageArray(messageText);
		return resp;
	};

	/**
	 * create a response with an error message
	 */
	var createSuccessResponse = function() {
		var resp = {};
		resp[POCOL.REQUEST_STATUS] = POCOL.STATUS_OK;
		resp[POCOL.MESSAGES] = [];
		return resp;
	};

	/**
	 * dummy server functionality. Mimics server with local data/function
	 */
	var getResponseLocal = function(serviceName, data, successFn, failureFn) {
		successFn = successFn || pushDataToPage;
		failureFn = failureFn || showMessages;
		if (!serviceName) {
			log('No service');
			FailureFn(createMessageArray('No serviceName specified'));
			return;
		}
		if (data && data.length) {
			try {
				data = JSON.parse(data);
			} catch (e) {
				log('Invaid input json. Assuming that the service is a special one that expects plain text..');
			}
		} else {
			data = {};
		}
		log('Service ' + serviceName + ' invoked locally');
		var response = localServer.getResponse(serviceName, data);
		log('local response for service ' + serviceName + ' :');
		log(response);
		try {
			response = JSON.parse(response);
		} catch (e) {
			log('Response is not a valid JSON. Assumed to be text and returning text instead of JSON.');
		}
		var st = response[POCOL.REQUEST_STATUS];
		if (!st || st == POCOL.STATUS_OK) {
			/*
			 * save json to a window variable
			 */
			window[POCOL.LAST_JSON] = response;
			successFn(response);
			return;
		}
		var msgs = response[POCOL.MESSAGES];
		if (!msgs) {
			msgs = createMessageArray('Server indicated failure of service, but offered no explanation!');
		}
		failureFn(msgs);
	};

	/*
	 * deal with local service during prototyping/testing
	 */
	var localServer = (function() {
		var localData = {
			tables : {},
			views : {},
			responses : {}
		};

		/**
		 * function to be called before making any service calls
		 */
		var initialize = function(data) {
			localData = data;
		};

		/**
		 * copy attributes from one item to the other. You may also use this to
		 * clone an item, but remember we do it only at one level. That is, we
		 * expect the attributes to be primitive. We do not handle objects
		 * containing other objects.
		 * 
		 * @param {Object}
		 *            fromObject from which to copy attributes.
		 * @param {Object}
		 *            toObject. If not passed, we create a new one.
		 * @param {Array}
		 *            attNames in case you have a list of possible attributes to
		 *            be copied
		 * @returns {Object} toObject. (received as parameter, or new one)
		 */
		var copyAtts = function(fromObject, toObject, attNames) {
			if (!toObject) {
				toObject = {};
			}
			if (attNames) {
				for (var i = 0; i < attNames.length; i++) {
					var attName = attNames[i];
					if (fromObject.hasOwnProperty(attName)) {
						toObject[attName] = fromObject[attName];
					}
				}
			} else {
				for ( var a in fromObject) {
					toObject[a] = fromObject[a];
				}
			}
			return toObject;
		};

		var copyReferredAtts = function(fromObject, toObject, refObject) {
			if (!toObject) {
				toObject = {};
			}
			for ( var a in refObject) {
				if (fromObject.hasOwnProperty(a)) {
					toObject[a] = fromObject[a];
				}
			}
			return toObject;
		};

		var copyItems = function(items) {
			var n = (items && items.length) || 0;
			var result = [];
			for (var i = 0; i < n; i++) {
				result.push(copyAtts(items[i]));
			}
			return result;
		};

		/**
		 * filter an array of items for matching value of an attribute
		 */
		var filterWithKey = function(items, attName, valueToMatch) {
			var result = [];
			var n = items && items.length || 0;
			for (var i = 0; i < n; i++) {
				var item = items[i];
				if (item[attName] == valueToMatch) {
					result.push(item);
				}
			}
			return result;
		};
		/**
		 * extract set of filters from data received from client.
		 * 
		 * @param {Object}
		 *            sampleItem an item that has all the attributes that can be
		 *            used for filtering
		 * @param {Object}
		 *            json contains name-value pairs. nameOperator and nameTo
		 *            are relatedFields
		 * @returns {Array} filtered items that satisfy filtering criterion
		 */
		var getFilters = function(item, json) {
			var filters = [];
			for (fieldName in item) {
				if (!json.hasOwnProperty(fieldName)) {
					continue;
				}
				/*
				 * create a default filter
				 */
				var filter = {
					name : fieldName,
					value : json[fieldName],
					op : '='
				};
				filters.push(filter);
				var val = json[fieldName + 'Operator'];
				if (!val) {
					continue;
				}
				/*
				 * operator is specified
				 */
				filter.op = val;
				if (val != '><') {
					continue;
				}

				// to operator
				val = json[fieldName + 'To'];
				if (!val) {
					throw 'we expected a value for ' + fieldName
							+ 'To because the operator is between (><)';
				}
				filter.to = val;
			}
			return filters;
		};

		/**
		 * apply filter criterion on an item
		 * 
		 * @param {Object}
		 *            item
		 * @param {Object}
		 *            filter
		 * @returns true if this items clears filtering criterion (it should be
		 *          selected). False otherwise
		 */
		var filterItem = function(item, filter) {
			var val = item[filter.name];
			switch (filter.op) {
			case '=':
				return filter.value == val;
			case '!=':
				return filter.value != val;
			case '<':
				return val < filter.value;
			case '<=':
				return filter.value <= val;
			case '>':
				return val > filter.value;
			case '>=':
				return filter.value >= val;
			case '><':
				return val > filter.value && val < filter.to;
			case '^':
				return val.toUpperCase().indexOf(filter.value.toUpperCase()) == 0;
			case '~':
				return val.toUpperCase().indexOf(filter.value.toUpperCase()) != -1;
			case '@':
				var list = filter.value && filter.value.split(',');
				var n = list.length || 0;
				for (var i = 0; i < n; i++) {
					if (val == list[i]) {
						return true;
					}
				}
				return false;
			}
		};
		/**
		 * copy attributes and items from related table/view based on list of
		 * filters
		 * 
		 * @param {Array}
		 *            items to be filtered
		 * @param {Array}
		 *            filters
		 * @returns filtered items
		 */
		var filterData = function(items, filters) {
			var result = [];
			var n = items.length || 0;
			var m = filters.length || 0;
			for (var i = 0; i < n; i++) {
				var item = items[i];
				var selected = true;
				for (var j = 0; j < m; j++) {
					if (!filterItem(item, filters[j])) {
						selected = false;
						break;
					}
				}
				if (selected) {
					result.push(item);
				}
			}
			return result;
		};

		/**
		 * add attributes/children to items from related items from join
		 * specification
		 * 
		 * @param {Array}
		 *            joins - array of joins from a view specification
		 * @param {Array}
		 *            items - base list of items to which we join others
		 * @returns array of freshly created items to which other attributes are
		 *          copied
		 */
		var doJoin = function(joins, items) {
			var nbrItems = items.length || 0;
			var nbrJoins = joins.length || 0;
			if (!nbrItems || !nbrJoins) {
				return items;
			}

			for (var i = 0; i < nbrItems; i++) {
				var item = items[i];
				for (var j = 0; j < nbrJoins; j++) {
					var join = joins[j];
					var attName = join.baseField;
					if (!item.hasOwnProperty(attName)) {
						throw 'base table does not have the attribute '
								+ attName + '. Invalid join specification.';
					}
					/*
					 * get children with matching values of key field
					 */
					var children = getData(join.joinedTable, null,
							join.joinedField, item[attName], false);
					if (children.length > 1) {
						throw 'We got '
								+ children.length
								+ ' items as children from '
								+ join.joinedTable
								+ ' with its parent key field '
								+ join.joinedFeild
								+ ' = '
								+ item[join.fieldName]
								+ ' but the join definition has not specified childrenName indicating that we do not expect more than one child items.';
					}
					if (children.length) {
						copyAtts(children[0], item);
					} else {
						log('No row from joining table ' + join.joinedTable
								+ ' with  ' + join.joinedField + ' = '
								+ item[attName]);
					}
				}
			}
		};

		/**
		 * get all items from a table/view.
		 * 
		 * @param {string}
		 *            tableName table/view name
		 * @param {Object}
		 *            forKey as an attribute-value for key matching. null if all
		 *            rows
		 * @returns {Array} rows from this table/view. all or for the supplied
		 *          key
		 */
		var getData = function(tableName, forKey, fieldName, fieldValue,
				getChildrenAswell) {
			var topTable = localData.tables[tableName];
			if (!topTable) {
				throw tableName
						+ ' is not defined as a table/view under _localData';
			}
			var children = getChildrenAswell && topTable.children;
			log('getting data for table ' + tableName + ' with children = '
					+ children);
			var check = {};
			var views = [];
			var table = null;
			/*
			 * we keep going down the tbales till we get a table with data. Then
			 * come up the chain of views.
			 */
			while (true) {
				table = localData.tables[tableName];
				if (!table) {
					throw tableName
							+ ' is not defined as a table/view under _localData';
				}
				if (table.data) {
					break;
				}
				/*
				 * keep track of tables in the set to avoid cycling in an
				 * infinite loop
				 */
				check[tableName] = true;
				views.push(table);
				tableName = table.baseTable;
				if (check[tableName]) {
					throw 'We found cyclic dependence of table ' + tableName;
				}
			}

			var items = table.data;
			if (forKey) {
				items = filterWithKey(items, table.key, forKey[table.key]);
			} else if (fieldName) {
				items = filterWithKey(items, fieldName, fieldValue);
			}
			items = copyItems(items);
			if (!items || !items.length) {
				return items;
			}
			/*
			 * we found the base data. We keep coming up the chain and join at
			 * each level
			 */
			for (var i = views.length - 1; i >= 0; i--) {
				doJoin(views[i].joins, items);
			}
			if (!children) {
				return items;
			}
			log('going to add rows from children ' + children);
			var n = items.length;
			var keyName = topTable.key;
			var filter = {
				name : keyName,
				op : '='
			};
			var filters = [ filter ];
			for (var i = 0; i < children.length; i++) {
				var childName = children[i];
				var childItems = getData(childName, null, null, null, true);
				for (var j = 0; j < n; j++) {
					var item = items[j];
					filter.value = item[keyName];
					item[childName] = filterData(childItems, filters);
				}

			}

			return items;
		};

		var getItemForKey = function(items, keyName, keyValue) {
			var n = items.length;
			for (var i = 0; i < n; i++) {
				var item = items[i];
				if (item[keyName] == keyValue) {
					return item;
				}
			}
			return null;
		};

		var saveData = function(tableName, json) {
			var table = localData.tables[tableName];
			if (!table) {
				throw tableName
						+ ' is not a table and hence data can not be saved using an express service';
			}
			var keyName = table.key;
			var keyValue = json[keyName];
			var items = table.data;
			var item = null;
			if (keyValue) {
				item = getItemForKey(items, keyName, keyValue);
				if (!item) {
					throw 'Item not found for the supplied key. Data can not be saved.';
				}
			}
			var result = {};
			item = copyReferredAtts(json, item, items[0]);
			if (!keyValue) {
				keyValue = items[items.length - 1][keyName];
				keyValue++;
				item[keyName] = keyValue;
				items.push(item);
				result[keyName] = keyValue;
			}
			return result;
		};

		var getList = function(tableName, json) {
			var table = localData.tables[tableName];
			if (!table) {
				throw tableName
						+ ' is not a table and hence we are unable to get a list of vname-value pairs';
			}
			var key = table.key;
			var value = table.valueFieldName;
			if (!key || !value) {
				throw tableName
						+ ' need to have attributes key and valueFieldName for us to create a list of name-value pairs';
			}
			var result = [];
			var items = table.data;
			var n = items.length;
			for (var i = 0; i < n; i++) {
				var item = items[i];
				result.push({
					key : item[key],
					value : item[value]
				});
			}
			return result;
		};

		var expressServices = {
			get : function(tableName, json) {
				var data = getData(tableName, json, null, null, true);
				var resp = createSuccessResponse();
				data = data && data[0];
				if (data) {
					copyAtts(data, resp);
				}
				return resp;
			},

			filter : function(tableName, json) {
				var data = getData(tableName, null, null, null, true);
				if (data.length && json) {
					var filters = getFilters(data[0], json);
					if (filters.length) {
						data = filterData(data, filters);
					}
				}
				var result = createSuccessResponse();
				result[tableName] = data;
				return result;
			},

			save : function(tableName, json) {
				return saveData(tableName, json);

			},

			'delete' : function(tableName, json) {
				var table = localData.tables[tableName];
				if (!table) {
					throw (tableName + ' is not a valid table name');
				}
				return deleteData(table, json);
			},
			list : function(tableName, json) {
				var data = getList(tableName, json);
				var result = createSuccessResponse();
				result[tableName] = data;
				return result;
			},
			suggest : function(tableName, json) {
				return createErrorResponse('local functionaity for suggest is not yet built');
			}
		};

		var serve = function(serviceName, json) {
			/*
			 * has the page included any ready response?
			 */
			var resp = window[POCOL.LOCAL_RESPONSES];
			resp = resp && resp[serviceName];
			if (resp) {
				log('response located in page-specific response');
				return resp;
			}
			/*
			 * is there a page specific fn for this?
			 */
			resp = window[POCOL.LOCAL_SERVICES];
			resp = resp && resp[serviceName];
			if (resp) {
				log('service function found in page-specific script');
				if (typeof (resp) !== 'function') {
					throw resp
							+ ' is expected to be a function that returns a valid response to a request for service '
							+ serviceName;
				}
				return resp(json);
			}
			/*
			 * no page specific option. Let us do it ourselves
			 */
			resp = localData.responses[serviceName];
			if (resp) {
				log('response located in local ready-responses');
				return resp;
			}
			/*
			 * express service?
			 */
			var parts = serviceName.split('_');
			if (parts.length > 1) {
				var prefix = parts[0];
				var fn = expressServices[prefix];
				if (fn) {
					var rec = serviceName.substring(prefix.length + 1);
					log('trying ' + prefix + ' for table ' + rec);
					return fn(rec, json);
				}
			}
			throw serviceName + ' is not served by the local server.';
		};

		var getResponse = function(serviceName, json) {
			var resp = null;
			try {
				resp = serve(serviceName, json);
			} catch (e) {
				resp = createErrorResponse(e.message ? e.message : e);
			}
			return JSON.stringify(resp);
		};
		return {
			initialize : initialize,
			getResponse : getResponse
		};

	})();

	var savedLocalData = null;
	/*
	 * patch for demo/testing/client-only development
	 */
	if (window.location.protocol === 'file:') {
		/*
		 * we are operating in local mode with no server
		 */
		if(!window.sessionStorage){
			alert("Sorry, your browser setting does not allow certain features when html is opened from file-system. Use a different browser, or see if you can enable sessionStorage feature using any setting options.")
			return;
		}
		var text = sessionStorage[POCOL.LOCAL_STORAGE_NAME];
		if (text) {
			log('Session storage found in session');
			savedLocalData = JSON.parse(text);
		} else {
			savedLocalData = window[POCOL.LOCAL_STORAGE_NAME];
			if (savedLocalData) {
				log('local storage found as windows variable..');
				sessionStorage[POCOL.LOCAL_STORAGE_NAME] = JSON
						.stringify(savedLocalData);
			}
		}
		if (savedLocalData) {
			/*
			 * set this to our local server
			 */
			localServer.initialize(savedLocalData);
			/*
			 * save it in session for next page to load before we die..
			 */
			window.addEventListener('beforeunload', function() {
				sessionStorage[POCOL.LOCAL_STORAGE_NAME] = JSON
						.stringify(savedLocalData);
			});
		} else {
			log('local storage NOT found');
		}
		getResponse = getResponseLocal;
	}
	/*
	 * what we want to expose as API
	 */
	return {
		log : log,
		error : error,
		warn : warn,
		pipeLogging : pipeLogging,
		showMessage : showMessage,
		showMessages : showMessages,
		overrideShowMessage : overrideShowMessage,
		overrideShowMessages : overrideShowMessages,
		getResponse : getResponse,
		pushDataToPage : pushDataToPage,
		htmlEscape : htmlEscape
	};
})();
