/**
 * invoked on page load
 */
var test = function() {
	var serviceName = document.getElementById('service').value;
	var data = document.getElementById('data').value;
	if (!serviceName) {
		Simplity.showMessage('Please enter the service name to invoke');
		return;
	}
	Simplity.log('going to invoke ' + serviceName + " with data=" + data);
	/*
	 * invoke service with no input data
	 */
	Simplity.getResponse(serviceName, data, function(response) {
		document.getElementById('response').innerHTML = JSON
				.stringify(response);
	}, function(msg) {
		Simplity.showMessages(msg);
	});
};
