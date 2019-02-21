/**
 * called once page got laded. getReady
 */
var pageLoaded = function() {
	var vets = document.getElementById('vetsTab');
	vets.href = '#';
	vets.parentNode.className = 'active';
	/*
	 * invoke service with no input data, and default call-back action
	 */
	server.getResponse(SERVICES.getVets);
};

var showJson = function() {
	/*
	 * last json received is generally available in this special variable
	 * 
	 */
	var json = window[POCOL.LAST_JSON];
	if (!json) {
		Simplity.error('Oooops. I lost the json. How could I do that!!');
		return;
	}
	var win = window.open('json');
	if (!win) {
		Simplity
				.error('Sorry, unable to open a new window. JSON can not be shown.');
		return;
	}
	var doc = win.document;
	doc.open();
	doc.write('<pre>');
	doc.write(JSON.stringify(json, null, 2));
	doc.write('</pre>');
	doc.close();
};