const apiPath = $('*[data-files-api-path]').data('filesApiPath');
const uiPath = $('*[data-files-ui-path]').data('filesUiPath');
const options = $('*[data-files-options]').data('filesOptions');
const path = $('*[data-path]').data('path');
if(!apiPath || !uiPath)
	alert('API Path or UI path is not known, file operations will fail.');

function actionsFormatter(value, row) {
    return '<a class="hover-show" href="file' + formatters.quoteattr(row.path) + '"><i class="bi bi-three-dots"></i></a>';
}

function nameFormatter(value, row) {
    if(row.type === 'folder') {        
       return '<a href="' + uiPath + formatters.quoteattr(row.path) + '?options=' + encodeURIComponent(options) + '"><i class="me-3 bi bi-' + row.type + '-fill"></i>' + value + '</a>';
    }
    else {
	   return '<a href="' + uiPath + formatters.quoteattr(row.path) + '?options=' + encodeURIComponent(options) + '"><i class="me-3 bi bi-' + row.type + '"></i>' + value + '</a>';
	}
}

function dateFormatter(value, row) {
    return moment(value).calendar();
}

function sizeFormatter(value, row) {
    return formatters.humanReadableByteCount(value);
}

function updateButtonState(noLocationUpdated) {    
    var selections = $('#files').bootstrapTable('getSelections');
    if(selections.length == 0) {
        $('#delete,#cut,#copy,#move,#download').attr('disabled', '');
    }
    else {        
        $('#delete,#cut,#copy,#move,#download').removeAttr('disabled');
    }
	var sd = calcSelectionData();
	var location = $('#location');
	if(location) {
		location.val(sd.selectionText);
	}
	postSelectionMessage(sd);
}

function postSelectionMessage(sd) {
	window.top.postMessage('filebrowser:' + JSON.stringify(sd, '*'));
}

function calcSelectionData() {
	var selections = $('#files').bootstrapTable('getSelections');
	var selectionString = '';
	var selectionPaths = [];
	for(var i = 0 ; i < selections.length; i++) {
		if(!selections[i].name) {
			continue;
		}
	    if(selectionString != '')
	        selectionString += ', ';
		var pname = ( path == '/' ? '' : path) + '/' + selections[i].name;
		selectionPaths.push(pname);
		selectionString += quoteAndEscapePath(pname);
	}
	if(selectionPaths.length == 0) {
		selectionPaths.push(path);
		selectionString = quoteAndEscapePath(path);		
	}
	return 	{
			op: 'selection',
			selections: selectionPaths,		
			selectionText: selectionString,
			selectionNames: selections,
			selectionCount: selections.length
	};
}

function quoteAndEscapePath(pname) {
	/* TODO escaping quotes and commas */
	if(pname.indexOf(' ') != -1) {
		pname = '"' + pname + '"';	
	}
	return pname;
}

function formatSelection(selections) {
    if(!selections)    
        selections = $('#files').bootstrapTable('getSelections');
    var name = '';
    for(var i = 0 ; i < selections.length; i++) {
        if(name != '')
            name += '/';
        name += selections[i].name;
    }
    return name;
}

function dropHandler(event) {
    console.log("File(s) dropped");
    event.preventDefault();
    $('[name=op]').val('upload');
    $('#upload')[0].files = event.dataTransfer.files;
    $('#filesForm').submit();
}

function dragOverHandler(event) {
    $('#dropZone').addClass('border border-2 p-3');
    event.preventDefault();
}

function dragLeaveHandler(event) {
    $('#dropZone').removeClass('border border-2 p-3');
}

const srvs = $('#files');
srvs.on('page-change.bs.table', function (e, number, size) {
	$('input[name=pageNumber]').val(number);
	$('input[name=pageSize]').val(size);
})
srvs.on('check.bs.table check-all.bs.table uncheck.bs.table uncheck-all.bs.table', function (e, row) {
    updateButtonState();
}); 

$('#hiddenFiles').on('change', function() {
    $('#filesForm').submit();
});

$('#mkdir').on('click', function() {
  bootbox.prompt('Create New Folder',
  function(result) {
      if(result) {
          $('[name=op]').val('mkdir');
          $('[name=name]').val(result);
          $('#filesForm').submit();
      }
  });
});

$('#cd').on('click', function(e) {
  bootbox.prompt('Change To Folder',
  function(result) {
      if(result) {
          $('[name=op]').val('cd');
          $('[name=name]').val(result);
          $('#filesForm').submit();
      }
  });
  return false;
});

$('#cut,#copy,#download').on('click', function() {
    $('[name=name]').val(formatSelection());
    $('[name=op]').val($(this).attr('id'));
    $('#filesForm').submit();
});

$('#paste').on('click', function() {
    $('[name=op]').val($(this).attr('id'));
    $('#filesForm').submit();
});

$('#delete').on('click', function() {
    var selections = $('#files').bootstrapTable('getSelections');
    bootbox.confirm(i18n.format('confirm.remove', selections.length), function(result) {
        if(result) {
            $('[name=name]').val(formatSelection(selections));
            $('[name=op]').val('delete');
            $('#filesForm').submit();
        }
    });
});

postSelectionMessage(calcSelectionData());