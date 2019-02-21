/*
 * fields that we deal with
 */
var FIELDS = [ 'ownerId', 'petId', 'petName', 'petDob', 'petTypeId' ];
/**
 * called once page got laded. getReady
 */
var pageLoaded = function() {
	document.getElementById('ownerTab').parentNode.className = 'active';
	/*
	 * are we to edit or create new?
	 */
	server.getResponse(SERVICES.getPetTypes);
	var petId = pageParams.param || pageParams.petId;
	var payload = {};
	if (petId) {
		payload.petId = petId;
		server.getResponse(SERVICES.getPet, payload);
	} else if (pageParams.ownerId) {
		document.getElementById('hdr').textContent = 'Add Pet';
		payload.ownerId = pageParams.ownerId;
		server.getResponse(SERVICES.getOwner, payload);
	} else {
		pageError();
		return;
	}
};

/**
 * user is submitting the form
 */
var submitted = function() {
	document.getElementById('submit').setAttribute('disabled', 'disabled');
	var payload = {};
	for (a in fields) {
		var val = fields[a].ele.value;
		if (val) {
			payload[a] = val;
		}
	}
	server.getResponse(SERVICES.savePet, payload, saved, saveFailed);
};

var pageError = function() {
	alert('This page is to be invoked as .html?ownerId=1 to add a pet to owner with id 1, or .html?petId=1 to edit pet with id of 1');
	window.location.href = PAGES.home;
};
/**
 * save successful. We go to
 */
var saved = function(json) {
	var key = fields.ownerId.ele.value;
	window.location.href = PAGES.showOwner + '?' + encodeURIComponent(key);
};
