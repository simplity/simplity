/*
 * elements we use
 */
var FIELDS = [ 'ownerTab', 'found', 'find', 'lastName', 'message' ];

/**
 * called once page got laded. getReady
 */
var pageLoaded = function() {
	fields.ownerTab.ele.parentNode.className = 'active';
	fields.found.ele.style.display = 'none';
};

/**
 * user is asking us to find owners. We look up to the server to help us on
 * this.
 */
var find = function() {
	var txt = fields.lastName.ele.value;
	if (txt) {
		/*
		 * we want owners whose last name starts with this value. we are
		 * re-using txt variable
		 */
		txt = {
			lastName : txt,
			lastNameComparator : '~'
		};
		txt = JSON.stringify(txt);
	}
	Simplity.getResponse(SERVICES.findOwners, txt, gotOwners);
};

/**
 * we got filter result from server. called back from Simplity.getRespone()
 */
var gotOwners = function(json) {
	var owners = json.owners;
	if (owners && owners.length) {
		fields.find.ele.style.display = 'none';
		fields.found.ele.style.display = '';
		Simplity.pushDataToPage(json);

	} else {
		fields.message.ele.innerHTML = 'has not been found';
	}
};
