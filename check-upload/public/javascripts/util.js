var files = {};

function getFiles() {
    console.log("getFiles()");
    $.ajax({
        type: 'GET',
        url: '/files',
        success: function (data) {
            files = {}

            if(data.message){
                $('#result').text(data.message);
            }
            data.forEach((file) => {
                file.status = "awaiting";
                if(file.Key.includes("::")){
                    file.routing = file.Key.split(":")[0];
                    file.account = file.Key.split(":")[1];
                    file.name = file.Key.split("::")[1];
                    file.status = "processed";
                }
                else {
                    file.name = file.Key
                }
                files[file.name] = file;
            }
            );
            displayFiles();
        },
        error: function (e) {
            $('#result').text(e.responseText);
            console.log('ERROR : ', e);
        }
    });
}

function deleteFiles() {
    console.log("deleteFiles()");
    $.ajax({
        type: 'DELETE',
        url: '/files',
        success: function (data) {
            setTimeout(getFiles, 2000);
        },
        error: function (e) {
            $('#result').text(e.responseText);
            console.log('ERROR : ', e);
        }
    });
}
function displayFiles() {
    $('#results').empty();

    for (var fileName in files) {
        var icon;
        var tag;
        var file = files[fileName];
        switch (file.status) {
            case 'processed':
                icon = 'check'
                tag = `<span class="tag is-info">Routing: ${file.routing}, Account: ${file.routing} </span>`
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
    $('#uploadedFile').change(function () {
        $('#fileLabel').text(this.files[0].name);
    });
    $('#clearBucket').click((event)=>deleteFiles());
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
                    setTimeout(getFiles, 2000);
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


getFiles();
var timesRun = 0;
var interval = setInterval(function(){
    timesRun += 1;
    if(timesRun === 100){
        clearInterval(interval);
    }
    getFiles();
}, 3000); 