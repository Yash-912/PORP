/* Non-blocking feedback: replaces alert() and confirms successful edits. */
(function () {
	function host() {
		var h = document.getElementById('toast-host');
		if (!h) {
			h = document.createElement('div');
			h.id = 'toast-host';
			document.body.appendChild(h);
		}
		return h;
	}

	function toast(message, type) {
		if (!document.body) { return; }
		var existing = host().querySelectorAll('.toast:not(.leaving)');
		for (var i = 0; i < existing.length; i++) {
			if (existing[i].textContent === String(message)) { return; }
		}
		var t = document.createElement('div');
		t.className = 'toast' + (type ? ' ' + type : '');
		t.setAttribute('role', 'status');
		t.textContent = message;
		host().appendChild(t);
		setTimeout(function () {
			t.classList.add('leaving');
			setTimeout(function () { if (t.parentNode) { t.parentNode.removeChild(t); } }, 350);
		}, 2600);
	}

	window.quizToast = toast;

	window.alert = function (message) {
		if (message && typeof message === 'object') {
			message = message.message || message.error || 'Something went wrong.';
		}
		toast(String(message), 'error');
	};

	var skip = /submitAnswers|\/login|\/logout|registration|forgotPassword|resetPassword/;
	var open = XMLHttpRequest.prototype.open;
	XMLHttpRequest.prototype.open = function (method, url) {
		var verb = (method || 'GET').toUpperCase();
		var target = String(url);
		if (verb !== 'GET' && !skip.test(target)) {
			this.addEventListener('load', function () {
				if (this.status >= 200 && this.status < 300) {
					toast(verb === 'DELETE' ? 'Deleted' : /\/publish$/.test(target) ? 'Published' : 'Saved');
				}
			});
		}
		return open.apply(this, arguments);
	};
}());
