
/* Actions */
document.querySelectorAll(".notification-action").forEach((a) => {
	a.addEventListener("click", () => {

		fetch(a.dataset.actionUri, {
			//	
		}).then(function(res) {
			return res.json();
		}).then(function(res) {
			if(res.redirect) {
				window.location.href = res.location;
			}
			if (res.success) {
				var popup = a.closest(".popup");
				var alertType = popup.dataset.popupType;
				var parent = popup.parentElement;
				popup.remove();
				var others = document.querySelectorAll("[data-popup-type=" + alertType + "]");
				if(others.length == 0) {
					parent.remove();
					document.querySelector("[data-popup-trigger=popup-" + alertType + "]").remove();
				}
			}
			else {
				alert(res.message);
			}
		}).catch(function(e) {
			alert(e);
		}).finally(function() {
			// TDO
		});
	});
	

});