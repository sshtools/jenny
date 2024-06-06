var fileIndex = 0;

function dropHandler(event) {
    event.preventDefault();
    $('#file' + fileIndex)[0].files = event.dataTransfer.files;
    addFileRow();
    dragLeaveHandler(event);
}

function dragOverHandler(event) {
    $('#dropZone').addClass('border border-2 p-3');
    event.preventDefault();
}

function dragLeaveHandler(event) {
    $('#dropZone').removeClass('border border-2 p-3');
}

function addFileRow() {
    fileIndex++;
    var files = $('#files');
    var templ = $('#fileTemplate');
    var newRow = $(templ.html());
    newRow.children('#file1').eq(0).attr('id', 'file' + fileIndex).attr('name', 'file' + fileIndex);
    var file1Remove = newRow.children('#file1Remove').eq(0);
    file1Remove.attr('id', 'file' + fileIndex + 'Remove');
    files.append(newRow);
    file1Remove.children('a').eq(0).on('click', function(e) {
        e.preventDefault();
        if($('#files').children().length > 2)
            newRow.remove();
    });
    var input = newRow.children('input').eq(0); 
    input.change(function () {
       addFileRow();
       file1Remove.removeClass('d-none');
     });
    input.focus();
}

addFileRow();