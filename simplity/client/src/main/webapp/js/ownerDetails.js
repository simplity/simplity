/*
 * dummy response for demo/tutorial/development/test
 */
var DUMMY_RESPONSE = {
	_requestStatus : 'ok',
	ownerId : 3,
	firstName : 'Eduardo',
	lastName : 'Rodriquez',
	address : '2693 Commerce St.',
	city : 'McFarland',
	telephone : '6085558763',
	pets : [ {
		petId : 3,
		petName : 'Rosy',
		petDob : '2011-04-17',
		petTypeId : 2,
		petType : 'dog',
		visits : [ {
			visitDate : '2011-12-23',
			visitDescription : 'general check up'
		}, {
			visitDate : '2012-01-04',
			visitDescription : 'special check up'
		} ]
	}, {
		petId : 4,
		petName : 'Jewel',
		petDob : '2010-03-07',
		petTypeId : 2,
		petType : 'dog',
		visits : []
	} ]
};

/*
 * set this global variable to use this as response.
 * 
 * comment this out to disable this option
 */
// window[POCOL.LOCAL_RESPONSES] = {
// get_ownerDetails : DUMMY_RESPONSE
// };
/*
 * comment this to hand over to Simplitty's local service
 */
window[POCOL.LOCAL_SERVICES] = {
	get_ownerDetailsssssssss : function(json) {
		var resp = DUMMY_RESPONSE;
		resp.pets[0].visits[0].visitDescription = 'this description is coming from a local script';
		return resp;
	}
};
/*
 * this is called from loaded() in common
 */
var pageLoaded = function() {
	a = document.getElementById('ownerTab');
	a.parentNode.className = 'active';
	/*
	 * do we have a ownerId?
	 */
	var ownerId = pageParams.param || pageParams.ownerId;
	if (!ownerId) {
		Simplity
				.error('ownerId is not passed by the caller. This page cannot function.');
		window.location.href = PAGES.welcome;
	}
	Simplity.getResponse(SERVICES.getOwner, '{"ownerId":"' + ownerId + '"}');
};
var addPet = function() {
	window.location.href = PAGES.editPet + '?ownerId='
			+ document.getElementById('ownerId').value;
};
/**
 * navigate to editIwner with ownerId as parameter
 */
var editOwner = function() {
	window.location.href = PAGES.editOwner + '?'
			+ document.getElementById('ownerId').value;
};
