var FIELDS = [ 'ownerId', 'petId', 'visitDescription', 'visitDate' ];
/**
 * called once page got laded. getReady
 */
var pageLoaded = function() {
	document.getElementById('ownerTab').className = 'active';
	/*
	 * are we to edit or create new?
	 */

	var petId = pageParams.param || pageParams.petId;
	if (!petId) {
		pageError();
		return;
	}
	fields.petId.ele.value = petId;
	Simplity.getResponse(SERVICES.getPet, '{"petId" : "' + petId + '"}');
};

/**
 * user is submitting the form
 */
var submitted = function() {
	document.getElementById('submit').setAttribute('disabled', 'disabled');
	var data = {};
	for (a in fields) {
		var val = fields[a].ele.value;
		if (val) {
			data[a] = val;
		}
	}
	Simplity.getResponse(SERVICES.saveVisit, JSON.stringify(data), saved,
			saveFailed);
};

var pageError = function() {
	alert('This page is to be invoked as .html?12 where 12 is the petId for which visit is to be added');
	window.location.href = PAGES.home;
};
/**
 * save successful. We go to
 */
var saved = function(json) {
	window.location.href = 'ownerDetails.html?' + fields.ownerId.ele.value;
};
