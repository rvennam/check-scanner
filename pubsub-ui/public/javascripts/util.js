var files = {};
function addFile(file) {
    files[file.name] = file;
    displayFiles();
}
function updateFile(updatedFile){
    files[updatedFile.name] = updatedFile;
    displayFiles();
}
function displayFiles() {
    $('#results').empty();

    for (var fileName in files) {
        var icon;
        var tag;
        var file = files[fileName];
        switch(file.status) {
            case 'processed':
                icon = 'check'
                tag = `<span class="tag is-info">${file.status} by ${file.workerID}</span>`
                break;
            case 'awaiting':
                icon = 'spinner'
                tag = `<span class="tag is-warning">${file.status}</span>`
                break;
        }
        $('#results')
            .append(
                $('<label>')
                    .addClass('panel-block')
                    .html(`${file.name}&nbsp;<span class="panel-icon"><i class="fas fa-${icon}"></i></span>${tag}`)

        );
      }
};

$(document).ready(function () {
    $('#btnSubmit')
        .click(function (event) {
            //stop submit the form, we will post it manually.
            event.preventDefault();
            // Get form
            var form = $('#fileUploadForm')[0];
            // Create an FormData object
            var data = new FormData(form);
            // disabled the submit button
            $('#btnSubmit').prop('disabled', true);

            $.ajax({
                type: 'POST',
                enctype: 'multipart/form-data',
                url: '/files',
                data: data,
                processData: false,
                contentType: false,
                cache: false,
                timeout: 60000,
                success: function (data) {
                    $('#result').text(data.name + ' uploaded to Object Storage');
                    addFile(data);
                    $('#btnSubmit').prop('disabled', false);
                },
                error: function (e) {
                    $('#result').text(e.responseText);
                    console.log('ERROR : ', e);
                    $('#btnSubmit').prop('disabled', false);
                }
            });

        });

});

$(function () {
    var socket = io();
    socket.on('file-status', function(msg){
        console.log('Got socket.io msg: ', msg);
        updateFile(msg);
    });
});
