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
	Simplity.getResponse(SERVICES.getPetTypes);
	var petId = pageParams.param || pageParams.petId;
	if (petId) {
		Simplity.getResponse(SERVICES.getPet, '{"petId" : "' + petId + '"}');
	} else if (pageParams.ownerId) {
		document.getElementById('hdr').textContent = 'Add Pet';
		Simplity.getResponse(SERVICES.getOwner, '{"ownerId" : "'
				+ pageParams.ownerId + '"}');
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
	var data = {};
	for (a in fields) {
		var val = fields[a].ele.value;
		if (val) {
			data[a] = val;
		}
	}
	Simplity.getResponse(SERVICES.savePet, JSON.stringify(data), saved,
			saveFailed);
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
	window.location.href = PAGES.showOwner + '?' + encodeURI(key);
};
