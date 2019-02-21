/**
 * invoked on page load
 */

var login = function() {
	document.getElementById('json').value = JSON.stringify(TEST_JSON[1], null,
			2);
	document.getElementById('service').value = 'save_test.parentTable';
	Simplity.login('222', 'abcd');
};
/**
 * we just want to validate service name as of now, but we have plans to do some
 * more meaningful stuff, like getting input data specifications
 */
var serviceChanged = function(ele) {
	// disabled this functionality
	return;
	var serviceName = ele.value;
	if (serviceName) {
		var json = {
			serviceName : serviceName
		};
		Simplity.getResponse('test.getService', json);
	}
};
var TEST_JSON = [ {
	txt : 'this is a text field',
	nbr : 12,
	amt : 23.45,
	bool : true,
	dt : new Date(),
	txtarr : [ {
		txtarr : 't1'
	}, {
		txtarr : 't2'
	}, {
		txtarr : 't3'
	}, {
		txtarr : 't4'
	}, {
		txtarr : 't5'
	} ],
	nbrarr : [ {
		nbrarr : 1
	}, {
		nbrarr : 2
	}, {
		nbrarr : 3
	}, {
		nbrarr : 4
	}, {
		nbrarr : 5
	}, {
		nbrarr : 6
	} ],
	amtarr : [ {
		amtarr : 1.1
	}, {
		amtarr : 2.2
	}, {
		amtarr : 3.3
	}, {
		amtarr : 4.4
	} ],
	boolarr : [ {
		boolarr : true
	}, {
		boolarr : false
	} ],
	datarr : [ {
		datarr : new Date(2016, 1, 12)
	}, {
		datarr : new Date(2014, 4, 6)
	} ],
	ds : [ {
		nbr : 123,
		txt : 'ds text'
	} ],
	dsarr : [ {
		nbr : 234,
		txt : 'ds arr text1'
	}, {
		nbr : 345678,
		txt : 'ds arr text 2'
	} ],
	cds : {
		nbr : 765,
		txtarr : [ 'sd1', 'sd2', 'sd3' ],
		ds : {
			nbr : 99,
			txt : 'tteexxtt'
		},
		dsarr : [ {
			nbr : 22,
			txt : 'text22'
		}, {
			nbr : 33,
			txt : 'text33'
		}, {
			nbr : 44,
			txt : 'text44'
		} ]
	},
	cdsarr : [ {
		nbr : 765,
		txtarr : [ 'sd1', 'sd2', 'sd3' ],
		ds : {
			nbr : 99,
			txt : 'tteexxtt'
		},
		dsarr : [ {
			nbr : 22,
			txt : 'text22'
		}, {
			nbr : 33,
			txt : 'text33'
		}, {
			nbr : 44,
			txt : 'text44'
		} ]
	}, {
		nbr : 765,
		txtarr : [ 'sd1', 'sd2', 'sd3' ],
		ds : {
			nbr : 99,
			txt : 'tteexxtt'
		},
		dsarr : [ {
			nbr : 22,
			txt : 'text22'
		}, {
			nbr : 33,
			txt : 'text33'
		}, {
			nbr : 44,
			txt : 'text44'
		} ]
	} ]
}, {
	"parentName" : "par1Name",
	"_saveAction" : "add",
	"childTable" : [ {
		"childName" : "c1OfPar1"
	}, {
		"childName" : "c2OfPar1"
	} ]
} ];

var test = function() {
	var serviceName = document.getElementById('service').value;
	var data = document.getElementById('json').value;
	if (!serviceName) {
		alert.showMessage('Please enter the service name to test');
		return;
	}
	var txt = null;
	var json = null;
	try {
		json = JSON.parse(data);
		txt = JSON.stringify(json, null, 2);
	} catch (e) {
		alert("Your json has sytax error. \n\n "+ e);
		return;
	}
	if (txt) {
		document.getElementById('json').value = txt;
	}
	Simplity.getResponse(serviceName, json, showResponse);
};
var showResponse = function(data) {
	document.getElementById('response').innerHTML = JSON.stringify(data, null,
			2);
};
