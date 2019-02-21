var SERVER_IS_REST = true;
var REST_ROOT = "rest/"
/*
 * All services used by this app. We define them in one place to get a ready
 * list of what the client is expecting from the server
 */
var SERVICES = {
	findOwners0 : {
		name : 'pet.owner.filter',
		url : 'owner/0',
		method : 'get'
	},
	findOwners1 : {
		name : 'pet.filterOwners',
		url : 'owner/1',
		method : 'get'
	},
	findOwners2 : {
		name : 'pet.filterWithJavaAction',
		url : 'owner/2',
		method : 'get'
	},
	findOwners3 : {
		name : 'pet.filterWithJavaService',
		url : 'owner/3',
		method : 'get'
	},
	findOwners4 : {
		name : 'clientSpecificServiceName',
		url : 'owner/4',
		method : 'get'
	},
	getOwner : {
		name : 'pet.owner.get',
		url : 'owner/id/',
		method : 'get',
		id : 'ownerId'
	},
	saveOwner : {
		name : 'pet.owner.save',
		url : 'owner',
		method : 'post'
	},
	getVets : {
		name : 'pet.vet.filter',
		url : 'vet',
		method : 'get'
	},
	saveVisit : {
		name : 'pet.visit.save',
		url : 'visit',
		method : 'post'
	},
	getPet : {
		name : 'pet.petDetail.get',
		url : 'pet/id/',
		method : 'get',
		id : 'petId'
	},
	savePet : {
		name : 'pet.pet.save',
		url : 'pet',
		method : 'post'
	},
	getPetTypes : {
		name : 'pet.petType.list',
		url : 'petType',
		method : 'get'
	}
};
/*
 * all page URLs used for navigation
 */
var PAGES = {
	home : 'welcome.html',
	showOwner : 'ownerDetails.html',
	editOwner : 'editOwner.html',
	editPet : 'editPet.html',
	editVisit : 'editVisit.html'
};
var RELATED_ELES = {
	group : 'Group',
	icon : 'Icon',
	msg : 'Msg'
};
var STYLES = {
	ok : {
		group : 'form-group',
		icon : 'glyphicon glyphicon-ok form-control-feedback'
	},
	err : {
		group : 'form-group has-error',
		icon : 'glyphicon glyphicon-remove form-control-feedback'
	}
};

var loaded = function() {
	// return;
	/*
	 * in local mode, we have to ensure that we start with welcome page always
	 */
	if (window.location.protocol === 'file:') {
		if (!sessionStorage['_localData']) {
			window.location.href = 'welcome.html';
		}
	}
	/*
	 * if page has listed fields that are used in script
	 */
	if (window.FIELDS) {
		var f = {};
		window.fields = f;
		var a;
		for (var i = 0; i < FIELDS.length; i++) {
			a = FIELDS[i];
			var ele = document.getElementById(a);
			if (ele) {
				var field = {
					ele : ele
				};
				f[a] = field;
				for (b in RELATED_ELES) {
					ele = document.getElementById(a + RELATED_ELES[b]);
					if (ele) {
						field[b] = ele;
					}
				}
			} else {
				Simplity.log('No dom element for id ' + a);
			}
		}
		/*
		 * just play it safe and refresh message areas
		 */
		setMessagesToFields({});
	}
	/*
	 * are there page parameters
	 */
	var p = {};
	window.pageParams = p;
	if (window.location.search) {
		var parts = window.location.search.substring(1).split('&');
		for (var i = parts.length - 1; i >= 0; i--) {
			var pair = parts[i].split('=');
			if (pair.length > 2) {
				Simplity.log('Invaid queryString part ' + parts[i]);
			} else if (pair.length == 1) {
				if (p.param) {
					Simplity
							.log('Invaid queryString ' + window.location.search);
				} else {
					p.param = pair[0];
				}

			} else {
				p[pair[0]] = pair[1];
			}
		}
	}

	/*
	 * trigger page load at page-level
	 */
	if (window.pageLoaded) {
		window.pageLoaded();
	}
};

/**
 * save has returned with errors.
 */
var saveFailed = function(msgs) {
	var n = msgs.length;
	var fieldMsgs = {};
	for (var i = 0; i < n; i++) {
		var msg = msgs[i];
		if (msg.messageType.toLowerCase() == 'error') {
			if (msg.fieldName) {
				fieldMsgs[msg.fieldName] = msg.text;
			} else {
				alert(msg.messageType.toUpperCase() + '\n' + msg.text);
			}
		}
	}
	setMessagesToFields(fieldMsgs);
	var btn = document.getElementById('submit');
	if (btn) {
		btn.removeAttribute('disabled');
	}
};

var setMessagesToFields = function(msgs) {
	if (!window.fields) {
		Simplity
				.log('This page has not defined fields. Messages are not going to be rendered..');
		return;
	}
	for (a in fields) {
		var field = fields[a];
		if (!field.msg) {
			continue;
		}
		var msg = msgs[a];
		var styles = STYLES.ok;
		if (msg) {
			styles = STYLES.err;
		} else {
			msg = '';
		}
		field.msg.textContent = msg;
		field.group.className = styles.group;
		field.icon.className = styles.icon;
	}
};

/**
 * client script uses this as the server to get whatever they want from it. for
 * example get a response for a service request
 */
var server = (function() {
	var getResponse = function(service, payload, callBack) {
		if (SERVER_IS_REST) {
			var url = REST_ROOT + service.url;
			var id = service.id;
			if(id){
				if(!payload || !payload[id]){
					alert("Client design error: value for field " + id + " is missing from payload");
					return;
				}
				url += encodeURIComponent(payload[id]);
				delete payload[id];
			}
			if(service.method == 'get'){
				var qry = [];
				var ch = '?'
				for(var a in  payload){
					if(!payload[id]){
						console.log(a + '=' + payload[a] + ' not sent to server');
						continue;
					}
					qry.push(ch);
					qry.push(a);
					qry.push('=');
					qry.push(encodeURIComponent(payload[a]));
					ch = '&';
				}
				if(qry.length){
					url += qry.join('');
				}
				payload = null;
			}
			console.log('url=' + url + '   method=' + service.method);
			Simplity.getResponse(null, payload, callBack, null, service.method, url );
		} else {
			Simplity.getResponse(service.name, payload, callBack, null);
		}
	};
	return {
		getResponse : getResponse
	};
})();
