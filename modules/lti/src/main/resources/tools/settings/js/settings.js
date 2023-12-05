const urlParams = new URLSearchParams(window.location.search);
const seriesID = urlParams.get('sid');

$(document).ready(function(){
    var seriesInfo = getSeries("/api/series/" + seriesID + "/metadata");
    var dublin = seriesInfo[0].fields;
    var ext = seriesInfo[1].fields;

    for (var x=0; x < dublin.length; x++) {
        if(dublin[x].id == "title") {
            $('#series_title').val(dublin[x].value);
        }
        if(dublin[x].id == "createdBy") {
            $('#series_creator').val(dublin[x].value);
        }
    }

    for (var j=0; j < ext.length; j++) {
        if(ext[j].id == "retention-cycle") {
            if(ext[j].value == "normal") {
                $('#series_retention  option[value=normal]').attr('selected','selected');
            } else if(ext[j].value == "long") {
                $('#series_retention  option[value=long]').attr('selected','selected');
            } else if(ext[j].value == "forever") {
                $('#series_retention  option[value=forever]').attr('selected','selected');
            }
        }
        if(ext[j].id == "caption-type") {
            if(ext[j].value == "none") {
                $('#series_captions  option[value=none]').attr('selected','selected');
            } else if(ext[j].value == "google") {
                $('#series_captions  option[value=google]').attr('selected','selected');
            } else if(ext[j].value == "") {
                $('#series_captions  option[value=no_selection]').attr('selected','selected');
            }
        }
        if(ext[j].id == "series-locked") {
            if(ext[j].value == false) {
                $('#locked_status').val("No");
            } else {
                $('#locked_status').val("Yes");
            }
        }
        if(ext[j].id == "series-expiry-date") {
            if(ext[j].value != '') {
                $('#retain_date').val(ext[j].value);
            } else {
                $('#retain_date').val('');
            }
        }
    }

    $("#series_captions").change(function(e){
      e.preventDefault();
      var captions = $('#series_captions').val();
      var fd;
      fd = new FormData();
      const newExt = ext.map(obj => obj.id === "caption-type" ? { ...obj, value: captions } : obj)
      const updatedMetadata = seriesInfo.map(obj => obj.flavor === "ext/series" ? { ...obj, fields: newExt} : obj)

      var metadata = JSON.stringify(updatedMetadata);
      fd.append("metadata",metadata);

      updateSeriesMetadata(fd);
    });


    $("#series_retention").change(function(e){
        e.preventDefault();
        var retention = $('#series_retention').val();
        var fd;
        fd = new FormData();
        const newExt = ext.map(obj => obj.id === "retention-cycle" ? { ...obj, value: retention } : obj)
        const updatedMetadata = seriesInfo.map(obj => obj.flavor === "ext/series" ? { ...obj, fields: newExt} : obj)

        var metadata = JSON.stringify(updatedMetadata);
        fd.append("metadata",metadata);

        updateSeriesMetadata(fd);
    });
});

function getSeries(url) {
    return JSON.parse($.ajax({
        type: 'GET',
        url: url,
        dataType: 'json',
        global: false,
        async: false,
        success: function (data) {
           return data;
        }
    }).responseText);
}

function updateSeriesMetadata(fd) {
    var url = "/api/series/" + seriesID;

    $.ajax({
        type: "PUT",
        url: url,
        processData: false,
        contentType: false,
        data: fd,
        dataType: "json"
    }).done(function (data) {
        return data;
    }).fail(function (error) {
        return error;
    });
}