var toastId = 0;
io.onReady(function(io) {
	io.subscribe('toast', function(msg) {
		toastId++;
		var toastElId = 'toast' + this._toastId;
		var actionHtml = '';
		if(msg.actions.length > 0) {
			actionHtml += '<div class="py-2 border-top"><div class="px-2">';
			for(i = 0 ; i < msg.actions.length; i++) {
				var act = msg.actions[i]; 
				actionHtml += '<a href="' + act.path + '" class="btn btn-' + act.style + '">';
				if(act.icon) {
					actionHtml += '<i class="bi bi-' + act.icon + '"></i>';
				}
				actionHtml += act.text + '</a>';
			}
			actionHtml += '</div>';
		}
		var html = '<div id="' + toastElId + '" class="toast" role="alert" aria-live="polite" aria-atomic="true" data-bs-delay="10000">' +
			'<div class="toast-header">' +
			'<strong class="me-auto text-' + msg.style + '">' + msg.title + '</strong>' + 
			'<small>' + msg.subtitle + '</small>' +
			'<button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>' +
			'</div>' +
			'<div class="toast-body">' +
			'<i style="font-size: 2rem;" class="' + msg.icon + ' align-middle"></i>&nbsp;' + msg.description +
			'</div>' +
			actionHtml 
			'</div>';
		var toastEl = $(html);
		$('#toastParent').append(toastEl);
		var toast = new bootstrap.Toast(toastEl);
		document.getElementById(toastElId).addEventListener('hidden.bs.toast', function() {
			$('#' + toastElId).remove();
		});
		toast.show();
		
	});
});