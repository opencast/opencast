/**
 * Copyright 2009-2013 The Regents of the University of California Licensed
 * under the Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
var PLAYER_URL = '/admin/embed.html';
var DEFAULT_SERIES_CATALOG_ID = 'seriesCatalog';
var WORKFLOW_RESTSERVICE = '/workflow/instance/';
var DUBLIN_CORE_NS_URI = 'http://purl.org/dc/terms/';

var postData = {
    'id': ''
};

var catalogUrl = '';
var mediapackage = null;
var DCmetadata = null;
var metadataChanged = false;
var seriesChanged = false;
var seriesServiceURL = false;
var workflowInstance = null;
var player = null;
var tracks = {};
var previewTracks = [];
var recordDate = null;

var intervalTimer = 0;
var timerTimeout = 1500;
var timerSet = false; // -> temporary solution. clearInterval(timer) does not work properly

function zeroFill(number, width) {
    width -= number.toString().length;
    if (width > 0) {
        return new Array(width + (/\./.test(number) ? 2 : 1)).join('0') + number;
    }
    return number + ""; // always return a string
}

/**
 * Returns the Input Time in Milliseconds
 *
 * @param data
 *          Data in the Format ab:cd:ef
 * @return Time from the Data in Milliseconds
 */
function getTimeInMilliseconds(data) {
    var values = data.split(':');

    // If the Format is correct
    if (values.length == 3) {
        // Try to convert to Numbers
        var val0 = values[0] * 1;
        var val1 = values[1] * 1;
        var val2 = values[2] * 1;
        // Check and parse the Seconds
        if (!isNaN(val0) && !isNaN(val1) && !isNaN(val2)) {
            // Convert Hours, Minutes and Seconds to Milliseconds
            val0 *= 60 * 60 * 1000; // 1 Hour = 60 Minutes = 60 * 60 Seconds =
            // 60 * 60 * 1000 Milliseconds
            val1 *= 60 * 1000; // 1 Minute = 60 Seconds = 60 * 1000
            // Milliseconds
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
 *
 * @param obj
 *          Object with Function .val()
 * @param val
 *          Value in Seconds to increase (val > 0) or decrease (val < 0), val <
 *          20 Seconds
 */
function in_de_creaseObject(obj, val) {
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
                                // Increase Hours and set Minutes and Seconds to
                                // 0
                                obj.val(getTimeString(val0 + 1, 0, Math.abs(val - (60 - val2))));
                            } else {
                                // Increase Minutes and set Seconds to the
                                // Difference
                                obj.val(getTimeString(val0, val1 + 1, Math.abs(val - (60 - val2))));
                            }
                        } else {
                            // Increase Seconds
                            obj.val(getTimeString(val0, val1, val2 + val));
                        }
                    }
                    // Decrease
                    else if ((val0 > 0) || (val1 > 0) || (val2 > 0)) {
                        // If <= 0 Seconds
                        if ((val2 + val) < 0) {
                            // If <= 0 Minutes
                            if ((val1 - 1) < 0) {
                                // Decrease Hours and set Minutes and Seconds to
                                // 0
                                obj.val(getTimeString(val0 - 1, 59, 60 - Math.abs(60 - Math.abs(val - (60 - val2)))));
                            } else {
                                // Decrease Minutes and set Seconds to 0
                                obj.val(getTimeString(val0, val1 - 1, 60 - Math.abs(60 - Math.abs(val - (60 - val2)))));
                            }
                        } else {
                            // Decrease Seconds
                            obj.val(getTimeString(val0, val1, val2 + val));
                        }
                    } else {
                        obj.val("00:00:00");
                    }
                } else {
                    obj.val("00:00:00");
                }
            } else {
                obj.val("00:00:00");
            }
        }
    }
}

/**
 * Returns a correct formatted Time String in the Format ab:cd:ef
 *
 * @param val0
 *          Hours element (0, 99)
 * @param val0
 *          Minutes element (0, 60)
 * @param val0
 *          Seconds element (0, 60)
 * @return a correct formatted Time String in the Format ab:cd:ef
 */
function getTimeString(val0, val1, val2) {
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
    } else {
        return "00:00:00";
    }
}

function continueWorkflowHelper() {
    // if metadata was changed update DC catalog and mediapackage instance
    if (metadataChanged) {
        if ($('#meta-title').val()) {
            updateDCMetadata();
            updateMediapackageMetadata();
            saveDCMetadata();
        } else {
            alert("Field 'Title' must not be empty!");
            return;
        }
    }

    if (seriesChanged) {
        if ($('#series').val() == '') {
            // remove series from mediapackage
            ocUtils.log('Removing Series');
            $(mediapackage.documentElement).find('series').remove();
            $(mediapackage.documentElement).find('seriestitle').remove();
            var $seriesCatalogRef = $(mediapackage.documentElement).find("metadata > catalog[type='dublincore/series']");
            if ($seriesCatalogRef.length > 0) {
                // delete series DC xml from working file repo
                var seriesCatalogUrl = $seriesCatalogRef.find('url').text();
                $.ajax({
                    url: seriesCatalogUrl,
                    type: 'delete',
                    error: function() {
                        ocUtils.log('Failed to removed series');
                    },
                    success: function() {
                        ocUtils.log('Removed series');
                    }
                });
                $seriesCatalogRef.remove();
            }
        } else {
            // update/add series data to mediapackage
            var seriesDcXml = '';
            if ($('#ispartof').val() == '') {
                // create series
                ocUtils.log('Creating series ' + $('#series').val());
                seriesXml = '<series><additionalMetadata><metadata><key>title</key><value>' + $('#series').val() + '</value></metadata></additionalMetadata></series>';
                $.ajax({
                    async: false,
                    type: 'PUT',
                    url: seriesServiceURL,
                    data: {
                        series: seriesXml
                    },
                    dataType: 'json',
                    success: function(data) {
                        $('#ispartof').val(data.series.id);
                    }
                });
            }
            // get seriesDcXml
            ocUtils.log('Getting DC catalog for series ' + $('#ispartof').val());
            $.ajax({
                async: false,
                url: seriesServiceURL + '/' + $('#ispartof').val() + '.xml',
                type: 'GET',
                dataType: 'xml',
                error: function() {
                    ocUtils.log('Could not retrieve series DC catalog for series ' + $('#ispartof').val());
                },
                success: function(data) {
                    seriesDcXml = ocUtils.xmlToString(data);

                    // find series dc ref in mediapackage
                    var seriesDcElm = $(mediapackage.documentElement).find("metadata > catalog[type='dublincore/series']");
                    var seriesDcFileId = ''; // MediaPackageElementId of series DC catalog
                    if (seriesDcElm.length == 0) {
                        var seriesDcElm = $('<catalog></catalog>', mediapackage.documentElement).attr('id', DEFAULT_SERIES_CATALOG_ID).attr('type', 'dublincore/series');
                        $('<mimetype></mimetype>', mediapackage.documentElement).text('text/xml').appendTo(seriesDcElm);
                        $('<url></url>', mediapackage.documentElement).appendTo(seriesDcElm);
                        $(mediapackage.documentElement).find('metadata').append(seriesDcElm);
                    }
                    // upload series dc xml
                    seriesDcFileId = seriesDcElm.attr('id');
                    var mediapackageId = $(mediapackage.documentElement).attr('id');
                    var url = '/files/mediapackage/' + mediapackageId + '/' + seriesDcFileId + '/dublincore.xml';
                    ocUtils.log('Saving Series DC Catalog to ' + url);
                    $.ajax({
                        async: false,
                        url: "/ingest/addCatalog",
                        type: "POST",
                        data: {
                            url: seriesServiceURL + '/' + $('#ispartof').val() + '.xml',
                            flavor: "dublincore/series",
                            mediaPackage: seriesDcXml
                        },
                        error: function() {
                            ocUtils.log('Failed to save DC metadata to ' + url);
                        },
                        success: function(data) {
                            ocUtils.log('Save DC metadata to ' + url);
                            seriesDcElm.find('url').text(data);
                            // finally update series in mediapackage instance
                            updateMPElement('seriestitle', $('#series').val());
                            updateMPElement('series', $('#ispartof').val());
                        }
                    });
                }
            });
        }
    }

    var trackChanged = $('input:checkbox:not(:checked)').length != $('input:checkbox').length;
    if (metadataChanged || seriesChanged || trackChanged) {
        var mp = ocUtils.xmlToString(mediapackage);
        mp = mp.replace(/ xmlns="http:\/\/www\.w3\.org\/1999\/xhtml"/g, ''); // no luck with $(element).removeAttr('xmlns');
        $.each($('input:checkbox:not(:checked)'), function(key, value) {
            var trackId = $(value).prop("id");
            trackId = trackId.split('/')[1];
            mp = ocMediapackage.removeTrack(mp, trackId);
        });
        parent.ocRecordings.Hold.changedMediaPackage = mp;
    }
    parent.ocRecordings.continueWorkflow(postData);
}

/**
 * continues the workflow
 */
function continueWorkflow() {
    editor.saveSplitList(continueWorkflowHelper);
}

/**
 * cancels the operation
 */
function cancel() {
    window.parent.location.href = "/admin";
}

function updateDCMetadata() {
    ocUtils.log("Updating DC metadata");

    $('.dcMetaField').each(function() {
        var $field = $(this);
        var fieldname = $field.attr('name');
        if (fieldname == 'created') {
            if ($('#recordDate').val() != "") {
                // get date in milliseconds, convert to seconds
                recordDate = ($('#recordDate').datepicker('getDate') && $('#recordDate').datepicker('getDate').getTime()) ? ($('#recordDate').datepicker('getDate').getTime() / 1000) : 0;
                recordDate += $('#startTimeHour').val() * 3600;
                // hour to seconds, add to date
                recordDate += $('#startTimeMin').val() * 60;
                // minutes to seconds, add to date
                recordDate = recordDate * 1000; // back to milliseconds
                recordDate = ocUtils.toISODate(recordDate);
            }
        }
        if ($field.val() != '') {
            var $dcelm = false;
            $(DCmetadata.documentElement).children().each(function(index, elm) {
                if (elm.tagName == 'dcterms:' + fieldname) {
                    $dcelm = $(elm);
                }
            });
            if ($dcelm !== false) {
                ocUtils.log("updating " + $field.attr('name') + " to: " + $field.val());
                $field.attr('name') === "created" ? $dcelm.text(recordDate) : $dcelm.text($field.val());
            } else {
                ocUtils.log("creating " + $field.attr('name') + " with value: " + $field.val());
                $field.attr('name') === "created" ? $('<dcterms:' + $field.attr('name') + '>').text(recordDate).appendTo(
                    DCmetadata.documentElement) : $('<dcterms:' + $field.attr('name') + '>').text($field.val()).appendTo(
                    DCmetadata.documentElement);
            }
        }
    });
}

// Update the MediaPackage instances metadata fields
// See org.opencastproject.mediapackage.MediaPackageImpl
function updateMediapackageMetadata() {
    updateMetadataField('title', 'title');
    updateMetadataField('created', 'start');
    updateMetadataField('language', 'language');
    updateMetadataField('keywords', 'keywords');
    updateMetadataField('rightsholder', 'rightsholder');
    updateMetadataField('license', 'license');
    updateMetadataField('subject', 'subject');
    updateMetadataGroupField('creator', 'creators', 'creator');
    updateMetadataGroupField('contributor', 'contributors', 'contributor');

    // update series
    if ($('#series').val() != '') {
        if ($('#ispartof').val() == '') {
            createSeriesFromText();
        }
        updateMPElement('seriestitle', $('#series').val());
        updateMPElement('series', $('#ispartof').val());
    }
}

function createSeriesFromText() {
    var dcDoc = '<dublincore xmlns="http://www.opencastproject.org/xsd/1.0/dublincore/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:oc="http://www.opencastproject.org/matterhorn"><dcterms:title xmlns="">' + $('#series').val() + '</dcterms:title><dcterms:creator xmlns=""></dcterms:creator><dcterms:contributor xmlns=""></dcterms:contributor><dcterms:subject xmlns=""></dcterms:subject><dcterms:language xmlns=""></dcterms:language><dcterms:license xmlns=""></dcterms:license><dcterms:description xmlns=""></dcterms:description></dublincore>';
    var acl = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><ns2:acl xmlns:ns2="org.opencastproject.security"></ns2:acl>';
    $.ajax({
        type: 'POST',
        url: '/series/',
        async: false,
        dataType: 'xml',
        data: {
            series: dcDoc,
            acl: acl
        },
        success: function(data) {
            id = data.getElementsByTagName('dcterms:identifier')[0].textContent;
            $('#ispartof').val(id);
        }
    });
}

function updateMPElement(MPname, value) {
    var $mpelm = $(mediapackage.documentElement).find(MPname);
    if ($mpelm.length != 0) {
        ocUtils.log('Updating ' + MPname + ' in MediaPackage to ' + value);
        MPname === "start" ? $mpelm.text(recordDate) : $mpelm.text(value);
    } else {
        ocUtils.log('Creating ' + MPname + ' in MediaPackage with value ' + value);
        $newelm = MPname === "start" ? $('<' + MPname + '/>').text(recordDate) : $('<' + MPname + '/>').text(value);
        $newelm.appendTo(mediapackage.documentElement);
    }
}

function updateMetadataField(DCname, MPname) {
    var $dcelm = $(DCmetadata.documentElement).find('dcterms\\:' + DCname);
    // dcterms:created ->
    if ($dcelm.length != 0) {
        updateMPElement(MPname, $dcelm.text());
    }
}

function updateMetadataGroupField(DCname, MPgroupname, MPname) {
    var $dcelms = $(DCmetadata.documentElement).find('dcterms\\:' + DCname);
    if ($dcelms.length != 0) {
        $parent = $(mediapackage.documentElement).find(MPgroupname);
        if ($parent.length > 0) {
            $parent.empty();
        } else {
            $parent = $('<' + MPgroupname + '/>');
            $(mediapackage.documentElement).append($parent);
        }
        $dcelms.each(function() {
            $('<' + MPname + '/>').text($(this).text()).appendTo($parent);
        });
    }
}

function saveDCMetadata() {
    $.ajax({
        url: catalogUrl,
        async: false,
        type: 'post',
        data: {
            content: ocUtils.xmlToString(DCmetadata)
        },
        error: function() {
            ocUtils.log('Failed to save DC metadata to ' + catalogUrl);
        },
        success: function() {
            ocUtils.log('Save DC metadata to ' + catalogUrl);
        }
    });
}


/***********************************************************************/
/***********************************************************************/

function loadTracks() {
    ocUtils.log("Loading tracks");

    tracks.tracks = [];
    ocUtils.log("Loading workflow instance data from '" + WORKFLOW_RESTSERVICE + postData.id + ".json" + "'");
    $.ajax({
        url: WORKFLOW_RESTSERVICE + postData.id + ".json",
        async: false,
        error: function(XMLHttpRequest, textStatus, errorThrown) {
            ocUtils.log("Done: Loading workflow instance, ERROR: " + textStatus + ", " + errorThrown);
            $('div#errorMessage').html('error: ' + textStatus);
        },
        success: function(data) {
            ocUtils.log("Done: Loading workflow instance data");
            var previewFlavor = getPreviewFlavorsFromWorkflow(data);
            var sourceFlavor = getSourceFlavorsFromWorkflow(data);
            
            // extract tracks
            workflowInstance = data.workflow;
            data = data.workflow.mediapackage.media.track;
            var singleFile = true;
            for (i = 0; i < data.length; i++) {
                if (data[i].type.indexOf(sourceFlavor) != -1) {
                    tracks.tracks.push(data[i]);
                } else if (data[i].type.indexOf(previewFlavor) != -1) {
                    previewTracks.push(data[i]);
                }
            }

            // populate series field if information
            var seriesid = workflowInstance.mediapackage.series;
            if (seriesid) {
                $('#ispartof').val(seriesid);
                $('#series').val(workflowInstance.mediapackage.seriestitle);
                $('#info-series')[0].innerHTML = workflowInstance.mediapackage.seriestitle;
            }

            // load metadata from DC xml for editing
            $.each(ocUtils.ensureArray(workflowInstance.mediapackage.metadata.catalog), function(key, value) {
                if (value.type == "dublincore/episode") {
                    catalogUrl = window.location.protocol + value.url.substring(value.url.indexOf('/'));
                }
            });

            ocUtils.log("Loading catalog data from '" + catalogUrl + "'");
            $.ajax({
                url: catalogUrl,
                dataType: 'xml',
                async: false,
                error: function(XMLHttpRequest, textStatus, errorThrown) {
                    ocUtils.log("Done: Loading catalog data, ERROR: " + textStatus + ", " + errorThrown);
                    $('div#errorMessage').html('error: ' + textStatus);
                },
                success: function(data) {
                    ocUtils.log("Done: Loading catalog data");

                    DCmetadata = data;
                    $(data.documentElement).children().each(function(index, elm) {
                        var tagName = elm.tagName.split(/:/)[1];
                        if ($(elm).text() != '') {
                            $('#meta-' + tagName).val($(elm).text());
                            if ($('#info-' + tagName).length > 0)
                                $('#info-' + tagName)[0].innerHTML = $(elm).text();
                            if (tagName === "category") {
                                value = $(elm).text();
                                $('#categorySelector').val(value.substr(0, 3));
                                changedCategory();
                                if (value.length > 3) {
                                    $('#category').val(value);
                                    changedSubCategory();
                                }
                            }
                            if (tagName === "created") {
                                $('#recordDate').datepicker('setDate', new Date($(elm).text()));
                                $('#startTimeHour').val((new Date($(elm).text())).getHours());
                                $('#startTimeMin').val((new Date($(elm).text())).getMinutes());
                            }
                        }
                    });
                }
            });
        }
    });

    ocUtils.log("Done: Loading tracks");
}

function createPlayer() {
    ocUtils.log("Creating player");
    ocUtils.log("#Preview tracks: " + previewTracks.length);
    ocUtils.log("Preview track #1: URL: '" + previewTracks[0].url + "', type='" + previewTracks[0].mimetype + "'");

    $('#videoPlayer').prepend('<source src="' + previewTracks[0].url + '" type="' + previewTracks[0].mimetype + '"/>');

    var fps = (previewTracks[0] && previewTracks[0].video && previewTracks[0].video.framerate) ? previewTracks[0].video.framerate : 0;
    var duration = previewTracks[0].duration / 1000;
    ocUtils.log("FPS: '" + fps + "',  duration: '" + duration + "'");

    player = $('#videoPlayer').mhPlayer({
        fps: fps,
        duration: duration
    });

    if (previewTracks.length >= 2) {
        ocUtils.log("Preview track #1: URL: '" + previewTracks[1].url + "', type='" + previewTracks[1].mimetype + "'");

        var videoSlave = '<video id="videoPlayerSlave">Your browser does not support HTML5 video.</video>';
        videoSlave = $(videoSlave).prepend('<source src="' + previewTracks[1].url + '" type="' + previewTracks[1].mimetype + '"/>')
        $('#videoPlayer').after(videoSlave);
        $('#videoPlayer').after('<div id="video_overlay_msg"></div>');

        $('#videoPlayerSlave').show();

        $("#video_overlay_msg").html("Loading videos...").show();
        $.synchronizeVideos(0, "videoPlayer", "videoPlayerSlave");
        $(document).on("sjs:buffering", function(event) {
            // $("#video_overlay_msg").html("The videos are currently buffering...").show(); // TODO: Comment in
        });
        $(document).on("sjs:allPlayersReady", function(event) {
            $("#video_overlay_msg").html("").hide();
        });
        $(document).on("sjs:bufferedAndAutoplaying", function(event) {
            $("#video_overlay_msg").html("").hide();
        });
        $(document).on("sjs:bufferedButNotAutoplaying", function(event) {
            $("#video_overlay_msg").html("").hide();
        });
    } else {
        $('#videoPlayer').css("width", "100%");
    }

    ocUtils.log("Done: Creating player");
}

function changedCategory() {
    var categoryId = $('#categorySelector').val();
    var options = '';

    var category = iTunesCategories[categoryId];
    options += '<option value="' + categoryId + '">-- Choose a Subcategory --</option>';
    for (var i = 0; i < category['subCategories'].length; i++) {
        var sub = category['subCategories'][i];
        options += '<option value="' + sub.value + '">' + sub.name + '</option>';
    }
    $("#category").html(options);
    changedSubCategory();
}

function changedSubCategory() {
    var subject = $('#category option:selected').index() == 0 ? $('#categorySelector option:selected').text() : $('#category option:selected').text();
    $("#meta-subject").val(subject);
}

function setSelectValues(selectId, from, to) {
    for (var i = from; i <= to; i++) {
        var option = $('<option/>');
        option.attr({
            'value': i
        });
        option.html(zeroFill(i, 2));
        $('#' + selectId).append(option);
    }
}

function initUI() {
    ocUtils.log("Initializing UI");

    setSelectValues('startTimeHour', 0, 23);
    setSelectValues('startTimeMin', 0, 59);

    $('#recordDate').datepicker({
        showOn: 'both',
        buttonImage: '/admin/img/icons/calendar.gif',
        buttonImageOnly: true,
        dateFormat: 'yy-mm-dd'
    });

    // load categories
    var options = '';
    for (var key in iTunesCategories) {
        options += '<option value="' + key + '">' + iTunesCategories[key]['name']
    }
    $('#categorySelector').html(options);

    $('#trackForm').append($('#template').jqote(tracks));

    // event: collapsable title clicked, de-/collapse collapsables
    $('.collapse-control2').click(function() {
        $('#ui-icon').toggleClass('ui-icon-triangle-1-e');
        $('#ui-icon').toggleClass('ui-icon-triangle-1-s');
        $(this).next('.collapsable').toggle();
        parent.ocRecordings.adjustHoldActionPanelHeight();
    });

    // create buttons
    $('.ui-button').button();

    // hide some stuff we don't want to see
    $("#seriesLabel, #series").hide(); // TODO: Correct series code and comment in
    window.parent.ocRecordings.disableRefresh();
    window.parent.ocRecordings.stopStatisticsUpdate();
    window.parent.$('#uploadContainer, #controlsTop, #searchBox, #tableContainer, #controlsFoot').hide();
    $('#trimming-hint').toggle();

    $('input[id^="chk"]').click(function(event) {
        if ($("input:checked").length == 0) {
            $('#trackError').show();
            $(event.currentTarget).prop("checked", true);
        } else {
            $('#trackError').hide();
        }
    });
    $('#categorySelector').change(function() {
        changedCategory();
    });
    $('#category').change(function() {
        changedSubCategory();
    });
    $('.oc-ui-form-field').change(function() {
        metadataChanged = true;
    });
    $('.dcMetaField').change(function() {
        metadataChanged = true;
    });
    $('.ocMetaField').change(function() {
        metadataChanged = true;
    });
    $('#series').change(function() {
        seriesChanged = true;
    });

    parent.ocRecordings.adjustHoldActionPanelHeight();

    ocUtils.log("Done: Initializing UI");
}

function initSeriesAutocomplete() {
    ocUtils.log("Initializing series autocomplete field");

    $('#series').autocomplete({
        source: function(request, response) {
            $.ajax({
                url: '/series/series.json?q=' + request.term,
                dataType: 'json',
                type: 'GET',
                success: function(data) {
                    var series_list = [];
                    $.each(data.catalogs, function() {
                        series_list.push({
                            value: this[DUBLIN_CORE_NS_URI]['title'][0].value,
                            id: this[DUBLIN_CORE_NS_URI]['identifier'][0].value
                        });
                    });
                    response(series_list);
                    $('#series').removeAttr('disabled');
                },
                error: function() {
                    ocUtils.log('Could not retreive series data');
                }
            });
        },
        select: function(event, ui) {
            $('#ispartof').val(ui.item.id);
        },
        change: function(event, ui) {
            if ($('#ispartof').val() === '' && $('#series').val() !== '') {
                ocUtils.log("Searching for series in series endpoint");
                $.ajax({
                    url: seriesServiceURL + '/series.json?seriesTitle=' + $('#series').val(),
                    type: 'get',
                    dataType: 'json',
                    success: function(data) {
                        var series_input = $('#series').val(),
                            series_list = data["catalogs"],
                            series_title, series_id;

                        if (series_list.length != 0) {
                            series_title = series_list[0][DUBLIN_CORE_NS_URI]["title"] ? series_list[0][DUBLIN_CORE_NS_URI]["title"][0].value : "";
                            series_id = series_list[0][DUBLIN_CORE_NS_URI]["identifier"] ? series_list[0][DUBLIN_CORE_NS_URI]["identifier"][0].value : "";
                            $('#ispartof').val(series_id);
                        }
                    }
                });
            } else if (($('#ispartof').val() == '') && ($('#series').val() == '')) {
                $('#ispartof').val('');
            }
        },
        search: function() {
            $('#ispartof').val('');
        }
    });

    ocUtils.log("Done: Initializing series autocomplete field");
}

function getWorkflowInstanceData() {
    ocUtils.log("Getting workflow instance data");

    $.ajax({
        url: '/workflow/instance/' + postData.id + '.xml',
        dataType: 'xml',
        async: false,
        success: function(data) {
            // clone mediapackage for editing
            mediapackage = ocUtils.createDoc('mediapackage', '');
            $.xmlns["mp"] = "http://mediapackage.opencastproject.org";
            $(data).find('mediapackage').clone();
            var clone = $(data).find('mediapackage').clone();
            $(clone).children().appendTo($(mediapackage.documentElement));
            $(mediapackage.documentElement).attr('id', $(clone).attr('id'));
            $(mediapackage.documentElement).attr('start', $(clone).attr('start'));
            $(mediapackage.documentElement).attr('duration', $(clone).attr('duration'));
            ocUtils.log("Workflow instance data: 'id' = '" + $(clone).attr('id') + "', 'start' = '" + $(clone).attr('start') + "', 'duration' = '" + $(clone).attr('duration') + "'");
        }
    });

    ocUtils.log("Done: Getting workflow instance data");
}

function getPostdataId() {
    ocUtils.log("Getting post data ID");

    postData.id = parent.document.getElementById("holdWorkflowId").value;

    ocUtils.log("Post data ID = '" + postData.id + "'");
    ocUtils.log("Done: Getting post data ID");

    return (postData.id != "");
}

function getPreviewFlavorsFromWorkflow(workflowReply) {
    if (workflowReply.workflow.operations == undefined || workflowReply.workflow.operations.operation == undefined) {
        return "preview";
    }
    var operations = workflowReply.workflow.operations.operation;
    for (var i = 0; i < operations.length; i++) {
        if (operations[i].id == "editor"){
            if (operations[i].configurations != undefined && operations[i].configurations.configuration != undefined) {
                for (var j = 0; j < operations[i].configurations.configuration.length; j++) {
                    if (operations[i].configurations.configuration[j].key == "preview-flavors") {
                        return operations[i].configurations.configuration[j].$.split("/")[1];
                    } 
                }
            }
            return "preview";
        }
    }
    return "preview";
}

function getSourceFlavorsFromWorkflow(workflowReply) {
    if (workflowReply.workflow.operations == undefined || workflowReply.workflow.operations.operation == undefined) {
        return "work";
    }
    var operations = workflowReply.workflow.operations.operation;
    for (var i = 0; i < operations.length; i++) {
        if (operations[i].id == "editor"){
            if (operations[i].configurations != undefined && operations[i].configurations.configuration != undefined) {
                for (var j = 0; j < operations[i].configurations.configuration.length; j++) {
                    if (operations[i].configurations.configuration[j].key == "source-flavors") {
                        return operations[i].configurations.configuration[j].$.split("/")[1];
                    } 
                }
            }
            return "work";
        }
    }
    return "work";
}

$(document).ready(function() {
    if (getPostdataId()) {
        loadTracks();
        initUI();
        getWorkflowInstanceData();
        initSeriesAutocomplete();
        createPlayer();
    }
});
