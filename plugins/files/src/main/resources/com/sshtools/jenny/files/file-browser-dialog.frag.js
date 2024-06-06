class FileBrowserDialog {

	constructor(br) {
		this.dialogElement = br;
		this.dialogElement.addEventListener('click', this._setupBrowser.bind(this));
	}
	
	_setupBrowser(e) {
		let opts = this.dialogElement.dataset.fileBrowserOptions;
		let path = this.dialogElement.dataset.fileBrowserPath;	
			
		let uri = '/browse-files';
		if(path.length > 0) {
			uri += path;
		}
		if (opts)
			uri += '?options=' + encodeURIComponent(this.dialogElement.dataset.fileBrowserOptions);

		this.dialog = bootbox.dialog(
			{
				message: '<iframe style="width: 100%; height: 100%" src="' + uri + '" title="' + i18n.format('browser.title') + '"></iframe>',
				size: 'extra-large',
				className: 'file-browser-dialog',
				closeButton: true
			});
	}

}

class FileChooserDialog {

	constructor(br) {
		this.dialogElement = br;
		this.dialogElement.addEventListener('click', this._setupBrowser.bind(this));
	}
	
	_setupBrowser(e) {

		this.selections = [];
		let opts = this.dialogElement.dataset.fileBrowserOptions;
		let fieldName = this.dialogElement.dataset.fileBrowserFieldName;

		this.field = document.querySelector('[name=' + fieldName + ']');
		if (!this.field) {
			alert('Target form field named ' + fieldName + ' does not exist.');
			return;
		}

		let uri = '/browse-files';
		if(this.field.value.length > 0) {
			uri += this.field.value;
			this.selections.push(this.field.value);
		}
		if (opts)
			uri += '?options=' + encodeURIComponent(this.dialogElement.dataset.fileBrowserOptions);

		this.dialog = bootbox.dialog(
			{
				message: '<iframe style="width: 100%; height: 100%" src="' + uri + '" title="' + i18n.format('browser.title') + '"></iframe>',
				title: '<button id="fileChooserSelect" role="file-chooser-select" class="btn btn-primary">' + i18n.format('browser.select') + '</button>',
				size: 'extra-large',
				className: 'file-browser-dialog',
				closeButton: true
			});

		this.chooserSelect = document.getElementById('fileChooserSelect');
		this.chooserSelect.disabled = this.field.value.length == 0;
		this.chooserSelect.addEventListener('click', this._onSelect.bind(this));
		
		window.onmessage = this._onIframeMessage.bind(this);

		e.preventDefault();
		return true;
	}

	_onSelect(e) {
		if(this.selections.length > 0)
			this.field.value = this.selections[0];
		this.dialog.modal('hide');
	}

	_onIframeMessage(e) {
		if (e.data.startsWith('filebrowser:')) {
			var fmsg = JSON.parse(e.data.substring(12));
			if (fmsg.op == 'selection') {
				this.selections = fmsg.selections;
				this.chooserSelect.disabled = fmsg.selectionCount == 0;
			}
		}
	}
}

class FileDialogs {

	constructor() {
		let browseable = document.querySelectorAll('[data-file-browser-field-name]');
		browseable.forEach((br) => {
			new FileChooserDialog(br);
		});
		
		browseable = document.querySelectorAll('[data-file-browser-path]');
		browseable.forEach((br) => {
			new FileBrowserDialog(br);
		});

		return true;
	}
}

new FileDialogs();