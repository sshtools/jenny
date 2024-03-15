function nameFormatter(value, row) {
    return '<a href="#"><i class="me-3 bi bi-' + ( row.icon.length > 0 ? row.icon : 'plugin' ) + '"></i>' + value + '</a>';
}

function formatSelection(selections) {
    if(!selections)    
        selections = $('#extensions').bootstrapTable('getSelections');
    var name = '';
    for(var i = 0 ; i < selections.length; i++) {
        if(name != '')
            name += '/';
        name += selections[i].id;
    }
    return name;
}

function updateButtonState() {    
    var selections = $('#extensions').bootstrapTable('getSelections');
    if(selections.length == 0) {
        $('#remove').attr('disabled', '');
    }
    else {        
        $('#remove').removeAttr('disabled');
    }
}

const srvs = $('#extensions');
srvs.on('page-change.bs.table', function (e, number, size) {
    $('input[name=pageNumber]').val(number);
    $('input[name=pageSize]').val(size);
})
srvs.on('check.bs.table check-all.bs.table uncheck.bs.table uncheck-all.bs.table', function (e, row) {
    updateButtonState();
}); 

$('#remove').on('click', function() {
    var selections = $('#extensions').bootstrapTable('getSelections');
    bootbox.confirm(i18n.format('confirm.remove', selections.length), function(result) {
        if(result) {
            $('[name=ids]').val(formatSelection(selections));
            $('[name=op]').val('remove');
            $('#extensionsForm').submit();
        }
    });
});