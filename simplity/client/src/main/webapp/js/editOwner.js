var _localResponsesssssssssssss = {
	'save_owners' : {
		_requestStatus : 'error',
		_messages : [ {
			messageType : 'error',
			fieldName : 'firstName',
			text : 'this field is in error'
		} ]
	}
};
/*
 * fields that we deal with
 */
var FIELDS = [ 'ownerId', 'firstName', 'lastName', 'address', 'city',
		'telephone' ];
/**
 * called once page got laded. getReady
 */
var pageLoaded = function() {
	document.getElementById('ownerTab').className = 'active';
	/*
	 * are we to edit or create new?
	 */
	var ownerId = pageParams.param || pageParams.ownerId;
	if (ownerId) {
		Simplity
				.getResponse(SERVICES.getOwner, '{"ownerId":"' + ownerId + '"}');
	}
};

/**
 * user is submitting the form
 */
var submitted = function() {
	document.getElementById('submit').setAttribute('disabled', 'disabled');
	var data = {};
	for ( var a in fields) {
		var val = fields[a].ele.value;
		if (val) {
			data[a] = val;
		}
	}
	Simplity.getResponse(SERVICES.saveOwner, JSON.stringify(data), saved,
			saveFailed);
};

/**
 * save successful. We go to
 */
var saved = function(json) {
	var key = json && json.ownerId;
	if (!key) {
		key = fields.ownerId.ele.value;
	}
	window.location.href = PAGES.showOwner + '?' + encodeURI(key);
};
