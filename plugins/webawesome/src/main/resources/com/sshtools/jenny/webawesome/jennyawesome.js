class BusyManager {
	constructor(options) {
		options = Object.assign({
			spinner: false
		}, options);
		
		this.options = options;		
		this.tasks = [];
		
		this._originalListener = false;
		this._disabled = [];
	}
	
	start() {
		var task = {};
		this.tasks.push(task);
		this._update();
		return task;
	}

	stop(task) {
		const index = this.tasks.indexOf(task);
		if (index > -1) {
		  	this.tasks.splice(index, 1);
		}
		console.log("Now " + this.tasks.length + " tasks");
		this._update();
	}
	
	_update() {
		if(this.tasks.length == 0) {
			if(this.options.spinner)
				this.options.spinner.classList.add("d-none");
			window.onbeforeunload = this._originalListener;
			this._originalListener = false;
			
			for(var t of this._disabled) {
				t.removeAttribute("disabled");
			}
			this._disabled = [];
		}
		else if(this.tasks.length == 1) {
			if(this.options.spinner) {
				this.options.spinner.classList.remove("d-none");
				this._disabled = document.querySelectorAll("[data-busy-action=disable]");
				for(var t of this._disabled) {
					t.setAttribute("disabled", true);
				}
			}

			this._originalListener = window.onbeforeunload;
			window.onbeforeunload =	() => {
				return "You have attempted to leave this page, whilst there are pending changes. Are you sure?";			
			};
		}
	}
}

class API {
	constructor() {
	}
	
	task(busyManager, i18nPrefix, uri, options) {
		options = Object.assign({
			method: false,
			data: false,
			callouts: false,
			success: {
				icon: "circle-check",
				iconVariant: "regular",
				variant: "success",
				callback: false,
				callouts: false
			},
			failure: {
				icon: "circle-exclamation",
				iconVariant: "regular",
				variant: "danger",
				callback: false,
				callouts: false
			}
		}, options);
		
		if(!options.success.callouts) {
			options.success.callouts = options.callouts ? options.callouts : document.getElementById("callouts");
		}
		if(!options.failure.callouts) {
			options.failure.callouts = options.callouts ? options.callouts : document.getElementById("callouts");
		}
		
		if(options.data instanceof HTMLFormElement) {
			if(!options.method) {
				options.method = "POST";
			}
			var formElement = options.data;
			options.data = new URLSearchParams();
			for (const pair of new FormData(formElement)) {
			    options.data.append(pair[0], pair[1]);
			}
		}
		
		if(!options.method) {
			options.method = "GET";
		}
		
		var fetchOpts = {
			method: options.method
		};
		
		if(options.data) {
			fetchOpts.body = options.data;
		}
				
		var tsk = busyManager.start();
		fetch(uri, fetchOpts).then(function(res) {
			return res.json();
		}).then(function(res) {
			if (res.success) {
				new Callout(options.success.callouts, {
					icon: "circle-check",
					variant: "success",
					titleKey: i18nPrefix + ".title",
					textKey:  i18nPrefix + ".text",
					textArgs: username
				}).show();
				if(options.success.callback) {
					options.success.callback(res);
				}
			}
			else {
				new Callout(options.failure.callouts, {
					icon: options.failure.icon,
					variant: options.failure.variant,
					iconVariant: options.failure.iconVariant,
					titleKey: i18nPrefix + "Failed.title",
					text: res.message
				}).show();
				if(options.failure.callback) {
					options.failure.callback(res, false);
				}
			}
		}).catch(function(e) {
			new Callout(options.failure.callouts, {
				icon: options.failure.icon,
				iconVariant: options.failure.iconVariant,
				variant: options.failure.variant,
				titleKey: "requestFailed.title",
				text: e
			}).show();
			if(options.failure.callback) {
				options.failure.callback(false, e);
			}
		}).finally(function() {
			progress.stop(tsk);
		});		
	}
	
	
	/**
	 * sends a request to the specified url from a form. this will change the window location.
	 * @param {string} path the path to send the post request to
	 * @param {object} params the parameters to add to the url
	 * @param {string} [method=post] the method to use on the form
	 */

	post(path, params, method = 'post') {

		// The rest of this code assumes you are not using a library.
		// It can be made less verbose if you use one.
		const form = document.createElement('form');
		form.method = method;
		form.action = path;

		for (const key in params) {
			if (params.hasOwnProperty(key)) {
				const hiddenField = document.createElement('input');
				hiddenField.type = 'hidden';
				hiddenField.name = key;
				hiddenField.value = params[key];

				form.appendChild(hiddenField);
			}
		}

		document.body.appendChild(form);
		form.submit();
	}
}

class Callout {
    constructor(element, options) {
		options = Object.assign({
			variant: "brand",
			title: false,
			titleKey: false,
			text: false,
			textKey: false,
			textArgs: [],
			titleArgs: [],
			icon: false,
			iconVariant: false,
		}, options);
		
		var callout = document.createElement("wa-callout");
		callout.classList.add("d-none");
		callout.classList.add("mb-3");
		callout.setAttribute("variant", options.variant);
		
		var text = options.text;
		if(!text) {
			if(options.textKey) {
				text = i18n.format(options.textKey, options.textArgs);
			}
			else
				throw "Callout must have text or textKey.";
		}
		
		var title = options.title;
		if(!title && options.titleKey) {
			title = i18n.format(options.titleKey, options.titleArgs);
		}
		
		if(options.icon) {
			var icon = document.createElement("wa-icon");
			icon.classList.add("me-1");
			icon.setAttribute("slow", "icon");
			icon.setAttribute("name", options.icon);
			icon.setAttribute("variant", options.iconVariant ? "regular" : options.iconVariant);
			callout.appendChild(icon);
		}
		
		if(title) {
			var titleEl  = document.createElement("strong");
			titleEl.classList.add("d-inline-block");
			titleEl.classList.add("mb-3");
			titleEl.innerHTML = title;
			callout.appendChild(titleEl);
			callout.appendChild(document.createElement("br"));
		}
		
		var content = document.createElement("span");
		content.innerHTML = text;
		callout.appendChild(content);
		
		this.callout = callout;
		
		element.appendChild(callout);
    }
    
    show() {
		this.callout.classList.remove("d-none");
    }

    hide() {
		this.callout.classList.add("d-none");
    }
	
	destroy() {
		this.callout.remove();
	}
	
	static destroyAll(element) {
		if(!element)
			element = document.getElementById("callouts");
		element.innerHTML = "";
	}
}

class JennyAwesome {
	constructor() {
		/* Dynamic drawers */
		for(var drawerTrigger of document.querySelectorAll("[data-dynamic-drawer]")) {
			drawerTrigger.addEventListener("click", () => {
				
				const drawer = document.querySelector(drawerTrigger.dataset.dynamicDrawerSelector);
				if(!drawer) {
					alert("Framework Error: A Dynamic drawer was referenced (e.g. from a button), but the actual drawer HTML does not exist. The selector '" + 
						drawerTrigger.dataset.dynamicDrawerSelector + "' was used.")
					return;	
				}
				
				const iframe = document.createElement("iframe");
				iframe.classList.add("w-100");
				iframe.classList.add("h-100");
				
				/*iframe.style.display = "none";*/
				iframe.src = drawerTrigger.dataset.dynamicDrawer;
				const drawerContent = drawer.querySelector(".dynamic-draw-content");
				drawerContent.appendChild(iframe);
				
				drawer.open = true;
				
				drawer.addEventListener("wa-after-hide", () => {
					drawerContent.removeChild(iframe);
				});
			});
		}
		
		/* Form submitters */
		for(var button of document.querySelectorAll(".submitter")) {
			button.addEventListener("click", () => {
				this.findParent(button, "form").submit();
			});
		}

		/* Link menu items */
		for(var menu of document.querySelectorAll(".link-menu")) {
			menu.addEventListener("wa-select", (m) => {
				window.location.href = m.detail.item.dataset.href;
			});
		}
		
		/* Popup triggers */
		for(var trigger of document.querySelectorAll("[data-popup-trigger]")) {
			trigger.addEventListener("click", () => {
				document.getElementById(trigger.dataset.popupTrigger).active = !document.getElementById(trigger.dataset.popupTrigger).active;
			});
		}

		/* Iframes */
		for(var trigger of document.querySelectorAll("[data-iframe-event-source]")) {
			trigger.addEventListener("click", () => {
				var ifsrc = trigger.dataset.iframeEventSource;
				var nearform = trigger.closest("form");
				if(ifsrc === "create") {
					var formact = nearform.dataset.iframeCreateAction;
					if(!formact)
						formact = nearform.getAttribute("action");
					window.parent.postMessage(JSON.stringify({
						source : trigger.id,
						form: utilities.serializeForm(nearform),
						action: formact
					}));
				}
				else if(ifsrc === "edit") {
					var formact = nearform.dataset.iframeEditAction;
					if(!formact)
						formact = nearform.getAttribute("action");
					window.parent.postMessage(JSON.stringify({
						source : trigger.id,
						form: utilities.serializeForm(nearform),
						action: formact
					}));
				}
				else {
					window.parent.postMessage(JSON.stringify({
						source : trigger.id
					}));
				}
			});
		}
		
	}
	
	/* TODO replace with closest() */
	findParent(startElement, tagName) {
		let currentElm = startElement;
		while (currentElm != document.body) {
			if (currentElm.tagName.toLowerCase() == tagName.toLowerCase()) { return currentElm; }
			currentElm = currentElm.parentElement;
		}
		return false;
	}
}

jennyAwesome = new JennyAwesome();
api = new API();