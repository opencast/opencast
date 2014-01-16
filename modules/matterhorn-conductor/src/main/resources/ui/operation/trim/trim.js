var PLAYER_URL                = '/admin/embed.html',
    DEFAULT_SERIES_CATALOG_ID = 'seriesCatalog',
    WORKFLOW_RESTSERVICE      = '/workflow/instance/',
    DUBLIN_CORE_NS_URI        = 'http://purl.org/dc/terms/',
    postData = {
        'id': parent.document.getElementById("holdWorkflowId").value
    },

    catalogUrl       = '',
    mediapackage     = null,
    DCmetadata       = null,
    metadataChanged  = false,
    seriesChanged    = false,
    seriesServiceURL = false,

    inpoint  = 0,
    outpoint = 0,

    // Variables for the "In point"- and "Out point"- increase-/decrease-Buttons
    secondsForward  = 1,
    secondsBackward = 1,

    intervalTimer   = 0,
    // Timeout of the Intervall Timer
    timerTimeout    = 1500,
    timerSet        = false, // -> temporary solution. clearInterval(timer) does not work properly

    mpe; // Metadata editor


$(document).ready(function () {
    var id = postData.id,
        tracks = {
            tracks: []
        };

    if (id === "") {
        return;
    }

    // Set default Values for In point and Out point
    $('#player-container').load(function () {
        $('#inPoint').val("00:00:00");
        // Set a Timer for the while-Loop
        this.intervalTimer = window.setInterval(function(){
            // Ask if a default Out Point has been set
            if (setOutPointDefaultValue()) {
                // Clear the Intervall
                $('#continueBtn').button('enable');
                window.clearInterval(this.intervalTimer);
            }
        }, timerTimeout);
    });

    //load tracks
    $.ajax({
        url: WORKFLOW_RESTSERVICE + id + ".json",
        async:false,
        success: function(data) {
            data = data.workflow.mediapackage.media.track;
            for (i = 0; i < data.length; i++) {
                if (data[i].type.indexOf("work") !== -1) {
                    tracks.tracks.push(data[i]);
                }
            }
        }
    });

    $("#trackForm").append($("#template").jqote(tracks));

    $("input[id^='chk']").click(function(event) {
        if ($("input:checked").length === 0) {
            $("#trackError").show();
            $(event.currentTarget).prop("checked", true);
        } else {
            $("#trackError").hide();
        }
    });

    // loading tracks ready

    /**
     * Tries to set the default Out point value
     */
    function setOutPointDefaultValue () {
        // If Duration has been set
        if ($("#player-container")[0].contentWindow.Opencast != null &&
           ($("#player-container")[0].contentWindow.Opencast.Player.getDuration() != 0) &&
           !timerSet &&
           ($("#player-container")[0].contentWindow.Opencast.Player.getDuration() != -1) &&
           $("#player-container").contents().find("#oc_duration").text() != "Initializing") {
                $("#outPoint").val($("#player-container").contents().find("#oc_duration").text());
                $("#newLength").val($("#player-container").contents().find("#oc_duration").text());
                timerSet = true;
                return true;
        };
        return false;
    }

    //create Buttons
    $(".ui-button").button();
    //disable continue
    $("#continueBtn").button("disable");

    $("#trimming-hint").toggle();

    window.parent.$("#controlsTop").hide(0);
    window.parent.$("#searchBox").hide(0);
    window.parent.$("#tableContainer").hide(0);
    window.parent.ocRecordings.disableRefresh();
    window.parent.ocRecordings.stopStatisticsUpdate();
    window.parent.$("#controlsFoot").hide(0);

    // Event edit link clicked
    $("#edit-link").click(function () {
        //parent.Recordings.retryRecording(id);
        parent.location.href = "/admin/upload.html?retry=" + id;
        return false;
    });

    // Event set inpoint clicked
    $("#set-trimin").click(function () {
        $("#inPoint").val($("#player-container")[0].contentWindow.Opencast.Player.getCurrentTime());
        checkInOutPoint();
    });

    //Event set outpoint clicked
    $("#set-trimout").click(function () {
        $("#outPoint").val($("#player-container")[0].contentWindow.Opencast.Player.getCurrentTime());
        checkInOutPoint();
    });

    //Event forward one second (inpoint)
    $("#step-in-forward").click(function () {
        in_de_creaseObject($("#inPoint"), secondsForward);
        checkInOutPoint();
    });

    //Event backward one second (inpoint)
    $("#step-in-backward").click(function () {
        in_de_creaseObject($("#inPoint"), -secondsForward);
        checkInOutPoint();
    });

    //Event forward one second (outpoint)
    $("#step-out-forward").click(function () {
        in_de_creaseObject($("#outPoint"), secondsBackward);
        checkInOutPoint();
    });

    //Event backward one second (outpoint)
    $("#step-out-backward").click(function () {
        in_de_creaseObject($("#outPoint"), -secondsBackward);
        checkInOutPoint();
    });

    /**
    * Checks if In point is bigger than Out point and prints an Error Message if this is the case
    */
    function checkInOutPoint(){
        // Check if Out point is larger than the Video Duration
        if (getTimeInMilliseconds($("#outPoint").val()) > getTimeInMilliseconds($("#player-container").contents().find("#oc_duration").text())) {
            $("#outPoint").val($("#player-container").contents().find("#oc_duration").text());
        }
        // Check if In point is larger than the Video Duration
        if (getTimeInMilliseconds($("#inPoint").val()) > getTimeInMilliseconds($("#player-container").contents().find("#oc_duration").text())) {
            $("#inPoint").val($("#player-container").contents().find("#oc_duration").text());
        }
        if (getTimeInMilliseconds($("#inPoint").val()) >= getTimeInMilliseconds($("#outPoint").val())) {
            $("div#errorMessage").html("Out point must be later than In point");
            $("#trimming-hint").hide();
            $("div#errorMessage").show();
        } else {
            calculateNewLength();
            $("div#errorMessage").hide();
            $("#trimming-hint").show();
        }
    }

    //start playing from current in point
    $("#play-from-in").click(function () {
        var videodisplay = $("#player-container")[0].contentWindow.Videodisplay,
            seekTime = getTimeInMilliseconds($("#inPoint").val());

        videodisplay.pause();
        videodisplay.seek(seekTime / 1000);
        window.setTimeout(function () {
            videodisplay.play();
        }, 800);
    });

    //play last 10 seconds to out
    $("#play-to-out").click(function () {
        var videodisplay = $("#player-container")[0].contentWindow.Videodisplay,
            seekTime = getTimeInMilliseconds($("#outPoint").val()) / 1000,
            timeOut = 10000;

        videodisplay.pause();
        // If Out point < 10 Seconds
        if ((seekTime - 10) <= 0) {
            videodisplay.seek(0);
            timeOut = seekTime * 1000;
        } else {
            videodisplay.seek(seekTime - 10);
        }
        window.setTimeout(function () {
            videodisplay.play();
            window.setTimeout(function () {
                videodisplay.pause();
            }, timeOut);
        }, 800);
    });

    // load preview player and metadata
    $.ajax({
        url: "/workflow/instance/" + id + ".xml",
        dataType: "xml", // or XML..
        success: function (data) {
            var mediapackageXML,
                clone,
                seriesid;

            $.xmlns["wf"]  = "http://workflow.opencastproject.org";
            $.xmlns["sec"] = "http://org.opencastproject.security";
            $.xmlns["mp"]  = "http://mediapackage.opencastproject.org";

            mediapackageXML = $(data).find("mp|mediapackage");

            if (mediapackageXML.length > 0) {
                openMetadataEditor(mediapackageXML[0]);
            } else {
                console.warn("No valid mediapackage found for the metadata editor!")
            }

            // clone mediapackage for editing
            mediapackage = ocUtils.createDoc("mediapackage", $.xmlns["mp"]);
            clone = $(data.documentElement).find("mp|mediapackage").clone();
            $(clone).children().appendTo($(mediapackage.documentElement));
            $(mediapackage.documentElement).attr("id", $(clone).attr("id"));
            $(mediapackage.documentElement).attr("start", $(clone).attr("start"));
            $(mediapackage.documentElement).attr("duration", $(clone).attr("duration"));

            // populate series field if information present
            seriesid =  $(data.documentElement).find("mp|mediapackage > series").text();
            if (seriesid != "") {
                $("#isPartOf").val(seriesid);
                $("#seriesSelect").val($(data.documentElement).find("mp|mediapackage > seriestitle").text());
                $("#info-series")[0].innerHTML = $(data.documentElement).find("mp|mediapackage > seriestitle").text();
            }

            var previewFilesTypes = new Array();
            var previewFiles = new Array();

            $(data.documentElement).find("mp|mediapackage > mp|media > track").each(function(index, elm){
                if ($(elm).attr("type").split(/\//)[1] == "preview") {
                    previewFilesTypes.push($(elm).attr("type").split(/\//)[0]);
                    previewFiles.push($(elm).find("url").text());
                }
            });
            if (previewFiles.length > 0) {
                var url = PLAYER_URL + "?";
                for (var i = 0; i < previewFiles.length; i++) {
                    if ((previewFiles.length === 1) && (i == 0)) {
                        url += "videoUrl=";
                    } else if ((i == 0) && (previewFilesTypes[i] == "presenter")) {
                        url += "videoUrl=";
                    } else if ((i == 1) && (previewFilesTypes[i] == "presentation")) {
                        url += "&videoUrl2=";
                    } else if ((i == 0) && (previewFilesTypes[i] == "presentation")) {
                        url += "videoUrl2=";
                    } else if ((i == 1) && (previewFilesTypes[i] == "presenter")) {
                        url += "&videoUrl=";
                    } else {
                        url += "&videoUrl" + (i + 1) + "=";
                    }
                    url += previewFiles[i];
                }
                $("#player-container").attr("src", url + "&play=false&preview=true&hideControls=false&hideAPLogo=true");
            } else {
                $("#player-container").text("No preview media files found for this media package.");
            }

            // show links to source media
            var singleFile = true;
            $(data.documentElement).find("mp|mediapackage > mp|media > track").each(function (index, elm) {
                if ($(elm).attr("type").split(/\//)[1] == "source") {
                    var link = document.createElement("a");
                    var url = $(elm).find("url").text();
                    $(link).attr("href", url);
                    var filename = url.split(/\//);
                    $(link).text(filename[filename.length - 1]).attr("title", "Download " + filename[filename.length - 1] + " for editing");
                    if (singleFile) {
                        singleFile = false;
                    } else {
                        $("#files").append($(document.createElement("span")).text(", "));
                    }
                    $("#files").append(link);
                }
            });
        }
    });

    // Event: collapsable title clicked, de-/collapse collapsables
    $(".collapse-control2").click(function () {
        $(this).find(".ui-icon").toggleClass("ui-icon-triangle-1-e");
        $(this).find(".ui-icon").toggleClass("ui-icon-triangle-1-s");
        $(this).next(".collapsable").toggle();
        parent.ocRecordings.adjustHoldActionPanelHeight();
    });

    // try to obtain URL of series service endpoint from service registry
    // on success enable series input field / init autocomplete on it
    // TODO: use JSONP here so we can call services provieded by other hosts in a distributed deployment
    var thisHost = window.location.protocol + "//" + window.location.host;
    $.ajax({
        type    : "GET",
        url     : "/services/services.json",
        data    : { serviceType: "org.opencastproject.series",  host: thisHost},
        dataType: "json",
        success: function (data) {                      // we are asking for a series service on the host this site comes from
            if (data.services.service !== undefined) {  // so there should always be none or one instance returned (instead of  data.services beeing an array
                seriesServiceURL = data.services.service.host + data.services.service.path;
                $("#seriesSelect").removeAttr("disabled");
                ocUtils.log("Initializing autocomplete for series field")
            }
        }
  });

  parent.ocRecordings.adjustHoldActionPanelHeight();
});


/**
 * Returns the Input Time in Milliseconds
 * @param data Data in the Format ab:cd:ef
 * @return Time from the Data in Milliseconds
 */
function getTimeInMilliseconds (data) {
    var values = data.split(':'),
        val0,
        val1,
        val2;

    // If the Format is correct
    if (values.length == 3) {
        // Try to convert to Numbers
        val0 = values[0] * 1;
        val1 = values[1] * 1;
        val2 = values[2] * 1;
        // Check and parse the Seconds
        if (!isNaN(val0) && !isNaN(val1) && !isNaN(val2)) {
            // Convert Hours, Minutes and Seconds to Milliseconds
            val0 *= 60 * 60 * 1000; // 1 Hour = 60 Minutes = 60 * 60 Seconds = 60 * 60 * 1000 Milliseconds
            val1 *= 60 * 1000; // 1 Minute = 60 Seconds = 60 * 1000 Milliseconds
            val2 *= 1000; // 1 Second = 1000 Milliseconds
            // Add the Milliseconds and return it
            return val0 + val1 + val2;
        } else {
            return 0;
        }
    } else {
        return 0;
    }
}

/**
 * Increases or decreases the current In point by val
 * @param obj Object with Function .val()
 * @param val Value in Seconds to increase (val > 0) or decrease (val < 0), val < 20 Seconds
 */
function in_de_creaseObject (obj, val) {
  if ((val != 0) && (Math.abs(val < 20))) {
    // Get current In point data
    var data = obj.val();
    // If data contains something
    if (data != '') {
      var values = data.split(':');
      if (values.length == 3) {
        // Try to convert to Numbers
        var val0 = values[0] * 1;
        var val1 = values[1] * 1;
        var val2 = values[2] * 1;
        // Check and parse the Seconds
        if (!isNaN(val0) && !isNaN(val1) && !isNaN(val2)) {
          // Increase
          if ((val > 0) && ((val0 >= 0) || (val1 >= 0) || (val2 >= 0))) {
            // If >= 59 Seconds
            if ((val2 + val) > 59) {
              // If >= 59 Minutes
              if ((val1 + 1) > 59) {
                // Increase Hours and set Minutes and Seconds to 0
                obj.val(getTimeString(val0 + 1, 0, Math.abs(val - (60 - val2))));
              }
              else {
                // Increase Minutes and set Seconds to the Difference
                obj.val(getTimeString(val0, val1 + 1, Math.abs(val - (60 - val2))));
              }
            }
            else {
              // Increase Seconds
              obj.val(getTimeString(val0, val1, val2 + val));
            }
          }
          // Decrease
          else
            if ((val0 > 0) || (val1 > 0) || (val2 > 0)) {
              // If <= 0 Seconds
              if ((val2 + val) < 0) {
                // If <= 0 Minutes
                if ((val1 - 1) < 0) {
                  // Decrease Hours and set Minutes and Seconds to 0
                  obj.val(getTimeString(val0 - 1, 59, 60 - Math.abs(60 - Math.abs(val - (60 - val2)))));
                }
                else {
                  // Decrease Minutes and set Seconds to 0
                  obj.val(getTimeString(val0, val1 - 1, 60 - Math.abs(60 - Math.abs(val - (60 - val2)))));
                }
            }
            else {
              // Decrease Seconds
              obj.val(getTimeString(val0, val1, val2 + val));
            }
          }
          else {
            obj.val("00:00:00");
          }
        }
        else {
          obj.val("00:00:00");
        }
      }
      else {
        obj.val("00:00:00");
      }
    }
  }
}

/**
 *calculates the new length of the media and shows the result
 *in the according field
 */
function calculateNewLength () {
    inPoint = getTimeInMilliseconds($('#inPoint').val());
    outPoint = getTimeInMilliseconds($('#outPoint').val());
    newLength = (outPoint - inPoint) / 1000;
    $('#newLength').val(getTimeString(Math.floor(newLength / 3600), (Math.floor(newLength / 60)) % 60, newLength % 60));    
}

/**
 * Returns a correct formatted Time String in the Format ab:cd:ef
 * @param val0 Hours element (0, 99)
 * @param val0 Minutes element (0, 60)
 * @param val0 Seconds element (0, 60)
 * @return a correct formatted Time String in the Format ab:cd:ef
 */
function getTimeString(val0, val1, val2){
  if ((val0 >= 0) && (val0 < 100) && (val1 >= 0) && (val1 < 60) && (val2 >= 0) && (val2 < 60)) {
    if (val0 <= 9) {
      val0 = "0" + val0.toString();
    }
    if (val1 <= 9) {
      val1 = "0" + val1.toString();
    }
    if (val2 <= 9) {
      val2 = "0" + val2.toString();
    }
    return val0 + ":" + val1 + ":" + val2;
  }
  else {
    return "00:00:00";
  }
}

/**
 * Continues the Workflow
 */
function continueWorkflow () {
        // Get Video Duration
    var videoDurationData = $('#player-container').contents().find('#oc_duration').text(),
        // Format 'Trim From'
        trimFromData = $('#inPoint').val(),
        trimToData;


    trimFromData = ((trimFromData == '') || (trimFromData == null)) ? '00:00:00' : trimFromData;
    // Format 'Trim To'
    trimToData = $('#outPoint').val();
    trimToData = ((trimToData == '') || (trimToData == null)) ? videoDurationData : trimToData;

    // if metadata was changed update DC catalog and mediapackage instance

    $("#mpe-editor").bind('succeeded', function(ev, updatedMP) {
        var newduration,
            trackChanged,
            trackId,
            mp;

        // If Input < Output
        if (trimFromData < trimToData) {
            newduration = getTimeInMilliseconds(trimToData) - getTimeInMilliseconds(trimFromData);
            postData["trimin"] = getTimeInMilliseconds(trimFromData);
            //postData["trimout"] = getTimeInMilliseconds(trimToData);
            postData["newduration"] = newduration;
            trackChanged = $("input:checkbox:not(:checked)").length != $("input:checkbox").length;

            if (metadataChanged || seriesChanged || trackChanged) {
                mp = updatedMP;
                mp = mp.replace(/ xmlns="http:\/\/www\.w3\.org\/1999\/xhtml"/g,"");     // no luck with $(element).removeAttr("xmlns");
                $("input:checkbox:not(:checked)").each(function (key, value) {
                    trackId = $(value).prop("id");
                    trackId = trackId.split("/")[1];
                    if (typeof trackId !== "undefined") {
                        mp = ocMediapackage.removeTrack(mp, trackId);
                    }
                });
                parent.ocRecordings.Hold.changedMediaPackage = mp;
            }
            parent.ocRecordings.continueWorkflow(postData);
        } else {
        $("div#errorMessage").html("The In-Point must not be bigger than the Out-Point");
        }
    });

    mpe.submit();
}

function leave(){
    window.parent.location.href = "/admin";
}

/**
 * @return generated UUID for catalog
 */
function generateUUID() {
    var uuid = (function () {
        var i,
            c = "89ab",
            u = [];
        for (i = 0; i < 36; i += 1) {
              u[i] = (Math.random() * 16 | 0).toString(16);
        }
        u[8] = u[13] = u[18] = u[23] = "-";
        u[14] = "4";
        u[19] = c.charAt(Math.random() * 4 | 0);
        return u.join("");
    }) ();

    return {
        toString: function () {
            return uuid;
        },
        valueOf: function () {
            return uuid;
        }
    };
}

/** Open the metadata editor with mediapackage xml.
 *  @param mediapackageXml -- xml representation of the mediapackage
 */
function openMetadataEditor (mediapackageXml) {
    mpe = $("#mpe-editor").mediaPackageEditor({
            additionalDC: {
                enable  : true,
                required: false
            },
            // Catalogs available for the plugin
            catalogs: {
                youtube: {
                    flavor: "catalog/youtube"
                }
            },
            requirement: {
                title: true,
            },

            addCatalog: function (mp, catalog, catalogDCXML) {
                var doc      = $.parseXML(mp),
                    mpId     = $(doc).find("mp|mediapackage").attr("id"),
                    uuid     = generateUUID(),
                    response = false;

                $.ajax({
                    async: false,
                    url: "/files/mediapackage/" + mpId + "/" + uuid + "/dublincore.xml",
                    data: {
                        content : catalogDCXML
                    },
                    type: "post",
                    success: function (url) {
                        catalog.url = url;
                        catalog.id  = uuid.toString();
                        response    = true;
                    }
                });

                return response;
            },
            changeCatalog: function(mp, catalog, catalogDCXML) {
                var doc      = $.parseXML(mp),
                    mpId     = $(doc).find("mp|mediapackage").attr("id"),
                    response = false;

                $.ajax({
                    async: false,
                    url: "/files/mediapackage/" + mpId + "/" + catalog.id + "/dublincore.xml",
                    data: {
                        content : catalogDCXML
                    },
                    type: "post",
                    success: function (url) {
                        catalog.url = url;
                        response = true;
                    }
                });

                return response;
            },
            deleteCatalog: function (catalog) {
                var mp       = this.getMediaPackage(),
                    doc      = $.parseXML(mp),
                    mpId     = $(doc).find("mp|mediapackage").attr("id"),
                    response = false;

                $.ajax({
                    async: false,
                    url: "/files/mediapackage/" + mpId + "/" + catalog.id,
                    type: "delete",
                    success: function () {
                            response = true;
                    },
                    error: function (jqXHR) {
                            if (jqXHR.status === 404) {
                                response = true;
                            }
                    }
                });
                return response;
            }
          },
          mediapackageXml);
}
