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
var editor = editor || {};

var ME_JSON = "/info/me.json";
var WORKFLOW_INSTANCE_PATH = "/workflow/instance/"; // {id}.json/.xml
var SMIL_PATH = "/smil";
var SMIL_CREATE_PATH = "/create";
var SMIL_ADDPAR_PATH = "/addPar";
var SMIL_ADDCLIP_PATH = "/addClip";
var FILE_PATH = "/files";
var FILE_MEDIAPACKAGE_PATH = "/mediapackage";

var WORKFLOW_INSTANCE_SUFFIX_JSON = ".json";
var WORKFLOW_INSTANCE_SUFFIX_XML = ".xml";
var SMIL_FLAVOR = "smil/smil";
var WAVEFORM_FLAVOR = "image/waveform";

var PREVIOUS_FRAME = "trim.previous_frame";
var NEXT_FRAME = "trim.next_frame";
var SPLIT_AT_CURRENT_TIME = "trim.split_at_current_time";
var PLAY_CURRENT_SEGMENT = "trim.play_current_segment";
var PLAY_CURRENT_PRE_POST = "trim.play_current_pre_post";
var SET_CURRENT_TIME_AS_INPOINT = "trim.set_current_time_as_inpoint";
var SET_CURRENT_TIME_AS_OUTPOINT = "trim.set_current_time_as_outpoint";
var PLAY_PAUSE = "trim.play_pause";
var SELECT_ITEM_AT_CURRENT_TIME = "trim.select_item_at_current_time";
var DELETE_SELECTED_ITEM = "trim.delete_selected_segment";
var NEXT_MARKER = "trim.next_marker";
var PREVIOUS_MARKER = "trim.previous_marker";
var PLAY_ENDING_OF_CURRENT_SEGMENT = "trim.play_ending_of_current_segment";

// key codes
var KEY_ENTER = 13;
var KEY_SPACE = 32;
var KEY_LEFT = 37;
var KEY_UP = 38;
var KEY_RIGHT = 39;
var KEY_DOWN = 40;

var default_config = {};
default_config[PREVIOUS_FRAME] = "left";
default_config[NEXT_FRAME] = "right";
default_config[SPLIT_AT_CURRENT_TIME] = "v";
default_config[PLAY_CURRENT_SEGMENT] = "c";
default_config[PLAY_CURRENT_PRE_POST] = "Shift+c";
default_config[SET_CURRENT_TIME_AS_INPOINT] = "i";
default_config[SET_CURRENT_TIME_AS_OUTPOINT] = "o";
default_config[PLAY_PAUSE] = "space";
default_config[SELECT_ITEM_AT_CURRENT_TIME] = "y";
default_config[DELETE_SELECTED_ITEM] = "Delete";
default_config[PREVIOUS_MARKER] = "Up";
default_config[NEXT_MARKER] = "Down";
default_config[PLAY_ENDING_OF_CURRENT_SEGMENT] = "n";

editor.error = false;
editor.workflowID = undefined;
editor.splitData = {};
editor.splitData.splits = [];
editor.selectedSplit = null;
editor.player = null;
editor.canvas = null;
editor.ready = false;
editor.smil = null;
editor.parsedSmil = null;
editor.workflowParser = null;
editor.mediapackageParser = null;
editor.smilParser = null;
editor.smilResponseParser = null;

var windowResizeMS = 500;
var initMS = 150;
var lastTimeSplitItemClick = 0;
var endTime = 0;
var now = 100;
var isSeeking = false;
var timeoutUsed = false;
var currSplitItemClickedViaJQ = false;
var timeout1 = null;
var timeout2 = null;
var timeout3 = null;
var timeout4 = null;
var currEvt = null;
var jumpBackTime = null;
var currSplitItem = null;

var waveformImageLoadDone = false;
var initialWaveformWidth = 0;
var currentWaveformWidth = 0;
var currWaveformZoom = 1;
var waveformZoomFactor = 20;
var maxWaveformZoomSlider = 400;

/******************************************************************************/
// editor
/******************************************************************************/

/**
 * get the workflow ID
 *
 * @return true if everything went fine, false else
 */
editor.getWorkflowID = function () {
    ocUtils.log("Getting workflow ID...");
    var postData = {
        'id': parent.document.getElementById("holdWorkflowId").value
    };
    editor.workflowID = postData.id;
    if (!editor.workflowID) {
        ocUtils.log("Error: Could not retrieve workflow instance ID...");
        editor.error = true;
        editor.workflowID = -1;
        displayError("Could not retrieve workflow instance ID. "+
		     "Without it the editor won't work. "+
		     "This should not happen, please ask your Matterhorn administrator.", "Error");
    } else {
        ocUtils.log("Done");
    }
    return !editor.error;
}

/**
 * parses the workflow
 *
 * @param jsonData json data
 * @return true if everything went fine, false else
 */
editor.parseWorkflow = function (jsonData) {
    if (!editor.error) {
        ocUtils.log("Parsing workflow instance...");
        try {
            editor.workflowParser = new $.workflowParser(jsonData);
            editor.workflowParser;
        } catch (e) {
            ocUtils.log("Error: Could not parse workflow instance...");
            editor.error = true;
            displayError("Could not parse workflow instance.", "Error");
        }
        ocUtils.log("Done");
    }
    return !editor.error;
}

/**
 * parses the mediapackage
 *
 * @param jsonData json data
 * @return true if everything went fine, false else
 */
editor.parseMediapackage = function (jsonData) {
    if (!editor.error) {
        ocUtils.log("Parsing mediapackage...");
        try {
            editor.mediapackageParser = new $.mediapackageParser(jsonData);
            editor.mediapackageParser;
        } catch (e) {
            ocUtils.log("Error: Could not parse mediapackage...");
            editor.error = true;
            displayError("Could not parse mediapackage.", "Error");
        }
        ocUtils.log("Done");
    }
    return !editor.error;
}

/**
 * parses the smil
 *
 * @param xmlData xml data
 * @return true if everything went fine, false else
 */
editor.parseSmil = function (xmlData) {
    if (!editor.error) {
        ocUtils.log("Parsing smil...");
        try {
            editor.smilParser = new $.smilParser(xmlData);
            editor.smil = editor.smilParser.smil;
            editor.parsedSmil = editor.smilParser.parsedSmil;
        } catch (e) {
            ocUtils.log("Error: Could not parse smil...");
            // editor.error = true;
            displayError("Could not parse smil.", "Error");
        }
        ocUtils.log("Done");
    }
    return !editor.error;
}

/**
 * downloads smil
 *
 * @param func function to execute after smil has been downloaded
 */
editor.downloadSmil = function (func) {
    if (!editor.error) {
        ocUtils.log("Downloading smil...");
        $.ajax({
            dataType: "text", // get it as "text", don't use jQuery smart guess!
            type: "GET",
            url: editor.mediapackageParser.smil_url
        }).done(function (data) {
            ocUtils.log("Done");
            editor.parseSmil(data);
            func();
        }).fail(function (error) {
            ocUtils.log("Error: Could not retrieve workflow instance");
            displayError("Could not retrieve workflow instance.", "Error");
        });
    }
}

/**
 * get the smil
 * 1. get the workflow GET /instance/{id}.json
 * 2. parse the workflow
 * 3. parse the mediapackage
 * 4. get the smil GET smil.url
 * 5. parse the smil
 * @param func function to execute after smil has been parsed
 **/
editor.getSmil = function (func) {
    if (!editor.error) {
        ocUtils.log("Downloading workflow instance (json)...");
        $.ajax({
            type: "GET",
            dataType: "json",
            url: WORKFLOW_INSTANCE_PATH + editor.workflowID + WORKFLOW_INSTANCE_SUFFIX_JSON
        }).done(function (data) {
            ocUtils.log("Done");
            editor.parseWorkflow(data);
            if (!editor.error) {
                editor.parseMediapackage(editor.workflowParser.mediapackage);
                if (!editor.error) {
                    editor.downloadSmil(func);
                }
            }
        }).fail(function (error) {
            ocUtils.log("Error: Could not retrieve smil");
            displayError("Could not retrieve smil.", "Error");
        });
    }
}

/**
 * add a par element to the smil
 *
 * @param xml response data
 * @return true if everything went fine, false else
 */
editor.parseSmilResponse = function (xml_response_data) {
    if (!editor.error) {
        ocUtils.log("Parsing smil response...");
        editor.smilResponseParser = new $.smilResponseParser(xml_response_data);
        ocUtils.log("Done");
        if (editor.smilResponseParser.smil) {
            editor.parseSmil(editor.smilResponseParser.smil);
        }
    }
    return !editor.error;
}

/**
 * create a new smil
 *
 * @param func function to execute after smil has been parsed
 */
editor.createNewSmil = function (func) {
    if (!editor.error) {
	ocUtils.log("Creating new smil...");
	$.ajax({
	    type: "POST",
	    dataType: "text", // get it as "text", don't use jQuery smart guess!
	    url: SMIL_PATH + SMIL_CREATE_PATH,
	    data: {
		mediaPackage: editor.mediapackageParser.mediapackage
	    }
	}).done(function (xml_response_data) {
	    ocUtils.log("Done");
	    editor.parseSmilResponse(xml_response_data);
	    if (editor.smilResponseParser.smil) {
		editor.parseSmil(editor.smilResponseParser.smil);
	    }
	    func();
	}).fail(function (e) {
	    ocUtils.log("Error: Error creating smil: ");
	    ocUtils.log(e);
	    displayError("Error creating smil.", "Error");
	});
    }
    return !editor.error;
}

/**
 * add a par element to the smil
 *
 * @param index index of currently added par
 * @param func function to execute after smil response has been parsed
 * @return false if an error has been thrown in another editor function, true if operating (else)
 */
editor.addPar = function (currParIndex) {
    if (!editor.error) {
        ocUtils.log("Adding par...");
        $.ajax({
            type: "POST",
            dataType: "text", // get it as "text", don't use jQuery smart guess!
            url: SMIL_PATH + SMIL_ADDPAR_PATH,
            data: {
                smil: editor.smil
            }
        }).done(function (data) {
            ocUtils.log("Done");
            editor.parseSmilResponse(data);
            var par = {
                "parID": editor.smilResponseParser.parID,
                "xmlEntity": editor.smilResponseParser.parXMLEntity
            }
            ocUtils.log("Next parID: " + par.parID);

            ocUtils.log("Downloading workflow instance (xml)...");
            $.ajax({
                type: "GET",
                dataType: "text", // get it as "text", don't use jQuery smart guess!
                url: WORKFLOW_INSTANCE_PATH + editor.workflowID + WORKFLOW_INSTANCE_SUFFIX_XML
            }).done(function (wfXML) {
                ocUtils.log("Done");
                var strs = getAllStringsOf(wfXML, "<ns3:track", "</ns3:track>");
                var error = false;
                for (var i = 0; i < strs.length && !error; ++i) {
		    var start = parseInt(editor.splitData.splits[currParIndex].clipBegin * 1000);
		    var duration = parseInt((editor.splitData.splits[currParIndex].clipEnd - editor.splitData.splits[currParIndex].clipBegin) * 1000);

                    ocUtils.log("Adding track no " + (i + 1) + " / " + strs.length);
                    $.ajax({
                        type: "POST",
                        async: false,
                        dataType: "text", // get it as "text", don't use jQuery smart guess!
                        url: SMIL_PATH + SMIL_ADDCLIP_PATH,
                        data: {
                            parentId: par.parID,
                            smil: editor.smil,
                            track: strs[i],
                            start: start,
                            duration: duration
                        }
                    }).done(function (xml_response_data) {
                        editor.parseSmilResponse(xml_response_data);
                        if (editor.smilResponseParser.smil) {
                            editor.parseSmil(editor.smilResponseParser.smil);
                        }
                    }).fail(function (e) {
                        ocUtils.log("Error: Could not add clip");
                        ocUtils.log(e);
                        error = true;
			displayError("Could not add clip. Aborting operation.", "Error");
                    });
                }
                if (!error) {
                    ocUtils.log("Continuing with next par element...");
                    editor.saveSplitListHelper(currParIndex + 1);
                }
            }).fail(function (e) {
                ocUtils.log("Error: Could not get workflow instance");
                ocUtils.log(e);
		displayError("Could not get workflow instance.", "Error");
            });
        }).fail(function (e) {
            ocUtils.log("Error: Could not add par element");
            ocUtils.log(e);
	    displayError("Could not add par element.", "Error");
        });
    }
    return !editor.error;
}

/**
 * save split list helper
 */
editor.saveSplitListHelper = function (startAtIndex) {
    if (!editor.error) {
	startAtIndex = (startAtIndex && (startAtIndex >= 0)) ? startAtIndex : 0;
	if (startAtIndex == 0) {
	    ocUtils.log("Potential segments to save (if enabled): " + editor.splitData.splits.length);
	}
	if (editor.splitData.splits.length > startAtIndex) {
	    if (editor.splitData.splits[startAtIndex].enabled) {
		ocUtils.log("Waiting for par to be added. Next start index to check: " + startAtIndex);
		editor.addPar(startAtIndex);
	    } else {
		ocUtils.log("Not adding par: Par not enabled. Next start index to check: " + startAtIndex);
		editor.saveSplitListHelper(startAtIndex + 1);
	    }
	} else if (editor.splitData.splits.length > 0) {
	    ocUtils.log("Done");
	    if(!editor.error) {
		ocUtils.log("Sending smil...");

		// Continue processing
		// POST files/mediapackage/{mediaPackageID}/{mediaPackageElementID}
		// Path params:
		//   mediaPackageID: the mediapackage identifier 
		//   mediaPackageElementID: the mediapackage element identifier
		// Form params:
		//   file: the file

		// generate a random mediapackage element ID
		var mpElementID = editor.mediapackageParser.smil_id; // Math.floor((Math.random()*1000)+1);

		// define a boundary -- stole this from Chrome
		var boundary = "----WebKitFormBoundaryvasZVBiO9iHRlTvY";
		// define the request payload
		var body = '--' + boundary + '\r\n'
		    + 'Content-Disposition: form-data; name="mediaPackageID"\r\n'
		    + '\r\n'
		    + editor.mediapackageParser.id + '\r\n'
		    + boundary + '\r\n';
		body += 
		'Content-Disposition: form-data; name="mediaPackageElementID"\r\n'
		    + '\r\n'
		    + mpElementID + '\r\n'
		    + '--' + boundary + '\r\n';
		body += 
		// parameter name "file", local filename "smil.smil"
		'Content-Disposition: form-data; name="file"; filename="smil.smil"\r\n'
		    + 'Content-Type: application/smil\r\n'
		    + '\r\n'
		    + editor.smil + '\r\n'
		    + '--' + boundary + '--' + '\r\n';

		$.ajax({
		    type: "POST",
		    contentType: "multipart/form-data; boundary="+boundary,
		    data: body,
		    url: FILE_PATH +
			FILE_MEDIAPACKAGE_PATH +
			"/" + editor.mediapackageParser.id +
			"/" + mpElementID
		}).done(function(data) {
		    ocUtils.log("Done");
		    ocUtils.log("Continuing workflow...");
		    editor.continueWorkflowFunction();
		}).fail(function (e) {
		    ocUtils.log("Error: Error submitting smil file: ");
		    ocUtils.log(e);
		    displayError("Error submitting smil file.", "Error");
		});
	    }
	}
    }
    return !editor.error;
}

/**
 * save smil
 */
editor.saveSplitList = function (func) {
    editor.continueWorkflowFunction = func;
    if (!editor.error) {
	$('#continueButton').attr("disabled", "disabled");
	editor.createNewSmil(function () {
	    editor.saveSplitListHelper(0);
	});
    }
    return !editor.error;
}

/**
 * update UI split list
 */
editor.updateSplitList = function (dontClickCancel) {
    if (!editor.error) {
	if (!dontClickCancel) {
	    cancelButtonClick();
	}

	var tmpTime = 0;
	$.each(editor.splitData.splits, function(index, value) {
	    if(value.enabled) {
		tmpTime += value.clipEnd - value.clipBegin;
	    }
	});
	tmpTime = (tmpTime >= 0) ? tmpTime : 0;
	$('#newTime').html("Duration after trimming: " + formatTime(tmpTime.toFixed(4)));

	$('#leftBox').html($('#splitElements').jqote(editor.splitData));
	$('#splitSegments').html($('#splitSegmentTemplate').jqote(editor.splitData));
	$('.splitItemDiv').click(splitItemClick);
	$('.splitSegmentItem').click(splitItemClick);
	// $('.splitRemover').click(splitRemoverClick);

	$('.splitRemoverLink').button({
	    text: false,
	    icons: {
		primary: "ui-icon-trash"
	    }
	});

	$('.splitAdderLink').button({
	    text: false,
	    icons: {
		primary: "ui-icon-arrowreturnthick-1-w"
	    }
	});

	$('.splitRemoverLink').click(splitRemoverClick);
	$('.splitAdderLink').click(splitRemoverClick);

	$('.splitSegmentItem').dblclick(jumpToSegment);
	$('.splitItemDiv').dblclick(jumpToSegment);

	$('.splitSegmentItem').hover(splitHoverIn, splitHoverOut);
	$('.splitItemDiv').hover(splitHoverIn, splitHoverOut);
    }
    return !editor.error;
}

/******************************************************************************/
// getter
/******************************************************************************/

/**
 * get the current player time
 *
 * @return current player time
 */
function getCurrentTime() {
    var currentTime = editor.player.prop("currentTime");
    currentTime = isNaN(currentTime) ? 0 : currentTime;
    return currentTime;
}

/**
 * get the media duration
 *
 * @return media duration
 */
function getDuration() {
    var duration = editor.player.prop("duration");
    duration = isNaN(duration) ? 0 : duration;
    return duration;
}

/**
 * get whether the player has been paused or not
 *
 * @return true if the player is currently paused, false else
 */
function getPlayerPaused() {
    var paused = editor.player.prop("paused");
    return paused;
}

/**
 * get the current split item by time
 * 
 * @return the current split item
 */
function getCurrentSplitItem() {
    var currentTime = getCurrentTime();
    for (var i = 0; i < editor.splitData.splits.length; ++i) {
        var splitItem = editor.splitData.splits[i];
        if ((splitItem.clipBegin <= (currentTime + 0.1)) && (currentTime < (splitItem.clipEnd - 1))) {
            splitItem.id = i;
            return splitItem;
        }
    }
    return currSplitItem;
}

/**
 * get the timefield begin time
 * 
 * @return the time in the timefield begin
 */
function getTimefieldTimeBegin() {
    return $('#clipBegin').timefield('option', 'value');
}

/**
 * get the timefield end time
 * 
 * @return the time in the timefield end
 */
function getTimefieldTimeEnd() {
    return $('#clipEnd').timefield('option', 'value');
}

/******************************************************************************/
// setter
/******************************************************************************/

/**
 * enable/disable a split item
 * 
 * @param uuid
 *          the id of the splitItem
 * @param enabled
 *          whether enabled or not
 */
function setEnabled(uuid, enabled) {
    editor.splitData.splits[uuid].enabled = enabled;
    editor.updateSplitList();
}

/**
 * set current time as the new inpoint of selected item
 */
function setCurrentTimeAsNewInpoint() {
    if (editor.selectedSplit != null) {
        setTimefieldTimeBegin(getCurrentTime());
    }
}

/**
 * set current time as the new outpoint of selected item
 */
function setCurrentTimeAsNewOutpoint() {
    if (editor.selectedSplit != null) {
        setTimefieldTimeEnd(getCurrentTime());
    }
}

/**
 * set current time
 *
 * @param time time to set
 */
function setCurrentTime(time) {
    time = isNaN(time) ? 0 : time;
    var duration = getDuration();
    time = (time > duration) ? duration : time;
    editor.player.prop("currentTime", time);
}

/**
 * set timefield begin time
 *
 * @param time time to set
 */
function setTimefieldTimeBegin(time) {
    $('#clipBegin').timefield('option', 'value', time);
}

/**
 * set timefield end time
 *
 * @param time time to set
 */
function setTimefieldTimeEnd(time) {
    $('#clipEnd').timefield('option', 'value', time);
}

/******************************************************************************/
// helper
/******************************************************************************/

/**
 * return all strings in str starting with startStr and ending with endStr
 *
 * @param str string to check
 * @param startStr start sequence
 * @param endStr end sequence
 * @return all strings in str starting with startStr and ending with endStr
 */
function getAllStringsOf(str, startStr, endStr) {
    var result = new Array();
    var strCpy = str;
    while ((strCpy.indexOf(startStr) != -1) && (strCpy.indexOf(endStr) != -1)) {
        var tmp = strCpy.substring(strCpy.indexOf(startStr), strCpy.indexOf(endStr) + endStr.length);
	if(tmp && (tmp != "")) {
	    result[result.length] = tmp;
	}
        strCpy = strCpy.substring(str.indexOf(endStr) + endStr.length, strCpy.length);
    }
    return result;
}

/**
 * formatting a time string to hh:MM:ss.mm
 * 
 * @param seconds
 *          the timeString in seconds
 * @returns the formated time string
 */
function formatTime(seconds) {
    if (typeof seconds == "string") {
        seconds = parseFloat(seconds);
    }

    var h = "00";
    var m = "00";
    var s = "00";
    if (!isNaN(seconds) && (seconds >= 0)) {
        var tmpH = Math.floor(seconds / 3600);
        var tmpM = Math.floor((seconds - (tmpH * 3600)) / 60);
        var tmpS = Math.floor(seconds - (tmpH * 3600) - (tmpM * 60));
        var tmpMS = seconds - tmpS;
        h = (tmpH < 10) ? "0" + tmpH : (Math.floor(seconds / 3600) + "");
        m = (tmpM < 10) ? "0" + tmpM : (tmpM + "");
        s = (tmpS < 10) ? "0" + tmpS : (tmpS + "");
        ms = tmpMS + "";
        var indexOfSDot = ms.indexOf(".");
        if (indexOfSDot != -1) {
            ms = ms.substr(indexOfSDot + 1, ms.length);
        }
        ms = ms.substr(0, 4);
        while (ms.length < 4) {
            ms += "0";
        }
    }
    return h + ":" + m + ":" + s + "." + ms;
}

/******************************************************************************/
// checks
/******************************************************************************/

/**
 * check whether toCheck is in Interval [lower, upper]
 *
 * @param toCheck variable to check
 * @param lower lower limit
 * @param upper upper limit
 * @return true when toCheck is in Interval [lower, upper], false else
 */
function isInInterval(toCheck, lower, upper) {
    return (toCheck >= lower) && (toCheck <= upper);
}

/**
 * check whether clipBegin is in the right format
 */
function checkClipBegin() {
    var clipBegin = getTimefieldTimeBegin();
    if (isNaN(clipBegin) || (clipBegin < 0)) {
        displayError("The inpoint is too low or the format is not correct. Correct format: hh:MM:ss.mmmm. Please check.",
		     "Check inpoint");
        return false;
    }
    return true;
}

/**
 * check whether clipEnd is in the right format
 */
function checkClipEnd() {
    var clipEnd = getTimefieldTimeEnd();
    var duration = getDuration();
    if (isNaN(clipEnd) || (clipEnd > duration)) {
        displayError("The outpoint is too high or the format is not correct. Correct format: hh:MM:ss.mmmm. Please check.",
		     "Check outpoint");
        return false;
    }
    return true;
}

/**
 * checks previous and next segments
 */
function checkPrevAndNext(id) {
    var duration = getDuration();
    // new first item
    if (id == 0) {
        if (editor.splitData.splits.length > 1) {
            var next = editor.splitData.splits[id + 1];
            next.clipBegin = splitItem.clipEnd;
        }
        if (getTimefieldTimeBegin() != 0) {
            var newSplitItem = {
                description: "",
                clipBegin: 0,
                clipEnd: splitItem.clipBegin,
                enabled: true
            };

            // add new item to front
            editor.splitData.splits.splice(0, 0, newSplitItem);
        }
        // new last item
    } else if (id == editor.splitData.splits.length - 1) {
        var duration = getDuration();
        if (getTimefieldTimeEnd() != duration) {
            var newLastItem = {
                description: "",
                clipBegin: editor.splitData.splits[id].clipEnd,
                clipEnd: duration,
                enabled: true
            };

            // add the new item to the end
            editor.splitData.splits.push(newLastItem);
        }
        var prev = editor.splitData.splits[id - 1];
        prev.clipEnd = editor.splitData.splits[id].clipBegin;
        // in the middle
    } else {
        var prev = editor.splitData.splits[id - 1];
        var next = editor.splitData.splits[id + 1];

        if (getTimefieldTimeBegin() <= prev.clipBegin) {
            displayError("The inpoint is lower than the begin of the last segment. Please check.",
			 "Check inpoint");
            return false;
        }
        if (getTimefieldTimeEnd() >= next.clipEnd) {
            displayError("The outpoint is bigger than the end of the next segment. Please check.",
			 "Check outpoint");
            return false;
        }

        prev.clipEnd = splitItem.clipBegin;
        next.clipBegin = splitItem.clipEnd;
    }
    return true;
}

/******************************************************************************/
// click
/******************************************************************************/

/**
 * click handler for saving data in editing box
 */
function okButtonClick() {
    if (checkClipBegin() && checkClipEnd()) {
        id = $('#splitUUID').val();
        if (id != "") {
            id = parseInt(id);
            if (getTimefieldTimeBegin() > getTimefieldTimeEnd()) {
                displayError("The inpoint is bigger than the outpoint. Please check and correct it.",
			     "Check and correct inpoint and outpoint");
                selectSegmentListElement(id);
                return;
            }

            var tmpBegin = splitItem.clipBegin;
            var tmpEnd = splitItem.clipEnd;
            var tmpDescription = splitItem.description;

            splitItem = editor.splitData.splits[id];
            splitItem.clipBegin = getTimefieldTimeBegin();
            splitItem.clipEnd = getTimefieldTimeEnd();
            splitItem.description = $('#splitDescription').val();
            if (checkPrevAndNext(id)) {
                editor.updateSplitList(true);
                $('#videoPlayer').focus();
                selectSegmentListElement(id);
            } else {
                splitItem = editor.splitData.splits[id];
                splitItem.clipBegin = tmpBegin;
                splitItem.clipEnd = tmpEnd;
                splitItem.description = tmpDescription;
                selectSegmentListElement(id);
            }
        }
    } else {
        selectCurrentSplitItem();
    }
}

/**
 * click handler for canceling editing
 */
function cancelButtonClick() {
    $('#splitDescription').html('');
    $('#splitUUID').val('');
    $('#splitDescription').val("");
    setTimefieldTimeBegin(0);
    setTimefieldTimeEnd(0);
    $('#splitIndex').html('#');
    $('.splitItem').removeClass('splitItemSelected');
    $('.splitSegmentItem').removeClass('splitSegmentItemSelected');
    editor.selectedSplit = null;
    enableInspectorBox(false);
}

/**
 * click/shortcut handler for removing current split item
 */
function splitRemoverClick() {
    item = $(this);
    var id = item.prop('id');
    if (id != undefined) {
        id = id.replace("splitItem-", "");
        id = id.replace("splitRemover-", "");
        id = id.replace("splitAdder-", "");
    } else {
        id = "";
    }
    if (id == "" || id == "deleteButton") {
        id = $('#splitUUID').val();
    }
    id = parseInt(id);
    if(editor.splitData.splits[id]) {
	if (editor.splitData.splits[id].enabled) {
	    /*
	      $('#deleteDialog').dialog({
	      buttons: {
	      "Yes": function () {
	      $('#splitItemDiv-' + id).addClass('disabled');
	      $('#splitRemover-' + id).hide();
	      $('#splitAdder-' + id).show();
	      $('.splitItem').removeClass('splitItemSelected');
	      $(this).dialog('close');
	      setEnabled(id, false);
	      },
	      "No": function () {
	      $(this).dialog('close')
	      }
	      },
	      title: "Remove Item?"
	      });
	    */
	    $('#splitItemDiv-' + id).addClass('disabled');
	    $('#splitRemover-' + id).hide();
	    $('#splitAdder-' + id).show();
	    $('.splitItem').removeClass('splitItemSelected');
	    setEnabled(id, false);
	    if(getCurrentSplitItem().id == id) {
		// if current split item is being deleted:
		// try to select the next enabled segment, if that fails try to select the previous enabled item
		var sthSelected = false;
		for(var i = id; i < editor.splitData.splits.length; ++i) {
		    if(editor.splitData.splits[i].enabled) {
			sthSelected = true;
			selectSegmentListElement(i, true);
			break;
		    }
		}
		if(!sthSelected) {
		    for(var i = id; i >= 0; --i) {
			if(editor.splitData.splits[i].enabled) {
			    sthSelected = true;
			    selectSegmentListElement(i, true);
			    break;
			}
		    }
		}
	    }
	    selectCurrentSplitItem();
	} else {
	    $('#splitItemDiv-' + id).removeClass('disabled');
	    $('#splitRemover-' + id).show();
	    $('#splitAdder-' + id).hide();
	    setEnabled(id, true);
	}
    }
    cancelButtonClick();
}

/**
 * click handler for selecting a split item in segment bar or list
 */
function splitItemClick() {
    if (!isSeeking || (isSeeking && ($(this).prop('id').indexOf('Div-') == -1))) {
        now = new Date();
    }

    if ((now - lastTimeSplitItemClick) > 80) {
        lastTimeSplitItemClick = now;

        // if not disabled and not seeking
        if (!$(this).hasClass('disabled') && ((isSeeking && ($(this).prop('id').indexOf('Div-') == -1)) || !isSeeking)) {
            // remove all selected classes
            $('.splitSegmentItem').removeClass('splitSegmentItemSelected');
            $('.splitItem').removeClass('splitItemSelected');

            // get the id of the split item
            id = $(this).prop('id');
            id = id.replace('splitItem-', '');
            id = id.replace('splitItemDiv-', '');
            id = id.replace('splitSegmentItem-', '');

            // add the selected class to the corresponding items
            $('#splitItem-' + id).addClass('splitItemSelected');
            $('#splitSegmentItem-' + id).addClass('splitSegmentItemSelected');

            $('#splitSegmentItem-' + id).removeClass('hover');

            // load data into right box
            splitItem = editor.splitData.splits[id];
            editor.selectedSplit = splitItem;
            editor.selectedSplit.id = parseInt(id);
            $('#splitDescription').val(splitItem.description);
            $('#splitUUID').val(id);
            setTimefieldTimeBegin(splitItem.clipBegin);
            setTimefieldTimeEnd(splitItem.clipEnd);
            $('#splitIndex').html(parseInt(id) + 1);

            currSplitItem = splitItem;

            if (!timeoutUsed) {
                if (!currSplitItemClickedViaJQ) {
                    setCurrentTime(splitItem.clipBegin);
                }
                // update the current time of the player
                $('.video-timer').html(formatTime(getCurrentTime()) + "/" + formatTime(getDuration()));
            }

            enableInspectorBox(true);
        }
    }
}

/**
 * click/shortcut handler for adding a split item at current time
 */
function splitButtonClick() {
    var currentTime = getCurrentTime();
    for (var i = 0; i < editor.splitData.splits.length; ++i) {
        var splitItem = editor.splitData.splits[i];
        if ((splitItem.clipBegin < currentTime) && (currentTime < splitItem.clipEnd)) {
            newEnd = 0;
            if (editor.splitData.splits.length == (i + 1)) {
                newEnd = splitItem.clipEnd;
            } else {
                newEnd = editor.splitData.splits[i + 1].clipBegin;
            }
            var newItem = {
                clipBegin: currentTime,
                clipEnd: newEnd,
                enabled: true,
                description: ""
            }
            splitItem.clipEnd = currentTime;
            editor.splitData.splits.splice(i + 1, 0, newItem);
            editor.updateSplitList();
            selectSegmentListElement(i + 1);
            return;
        }
    }
    selectCurrentSplitItem();
}

/******************************************************************************/
// select
/******************************************************************************/

/**
 * select the split segment at the current time
 */
function selectCurrentSplitItem() {
    var splitItem = getCurrentSplitItem();
    if (splitItem != null) {
	var idFound = false;
	var id = -1;
	if(splitItem.enabled) {
	    id = splitItem.id;
	    idFound = true;
	} else {
	    for(var i = splitItem.id; i < editor.splitData.splits.length; ++i) {
		if(editor.splitData.splits[i].enabled) {
		    idFound = true;
		    id = i;
		    break;
		}
	    }
	    if(!idFound) {
		for(var i = splitItem.id; i >= 0; --i) {
		    if(editor.splitData.splits[i].enabled) {
			idFound = true;
			id = i;
			break;
		    }
		}
	    }
	}
	if(idFound) {
	    currSplitItemClickedViaJQ = true;
	    $('#splitSegmentItem-' + id).click();
	    $('#descriptionCurrentTime').html(formatTime(getCurrentTime()));
	} else {
	    ocUtils.log("Could not find an enabled ID");
	}
    }
}

/**
 * selects the split segment with the number number
 */
function selectSegmentListElement(number, dblClick) {
    dblClick = dblClick ? dblClick : false;
    var spltItem = editor.splitData.splits[number];
    if(spltItem) {
	setCurrentTime(spltItem.clipBegin);
	currSplitItemClickedViaJQ = true;
	if ($('#splitItemDiv-' + number)) {
	    if(dblClick) {
		$('#splitItemDiv-' + number).dblclick();
	    } else {
		$('#splitItemDiv-' + number).click();
	    }
	}
    }
}

/******************************************************************************/
// visual
/******************************************************************************/

/**
 * displays a graphical error
 */
function displayError(errorMsg, errorTitle) {
    $('<div />').html(errorMsg).dialog({
        title: errorTitle,
        resizable: false,
        buttons: {
            OK: function () {
                $(this).dialog("close");
            }
        }
    });
}

/**
 * updates the currentTime div
 */
function updateCurrentTime() {
    $('#current_time').html(formatTime(getCurrentTime()));
}

/**
 * enable/disable the right editing box
 * 
 * @param enabled
 *          whether enabled or not
 */
function enableInspectorBox(enabled) {
    if (enabled) {
        $('#midBox :input').removeProp('disabled');
        $('.frameButton').button("enable");
        $('#descriptionCurrentTimeDiv').show();
    } else {
        $('#midBox :input').prop('disabled', 'disabled');
        $('.frameButton').button("disable");
        $('#descriptionCurrentTimeDiv').hide();
    }
}

/******************************************************************************/
// split
/******************************************************************************/

/**
 * handler for hover in events on split segements and -list
 * 
 * @param evt the corresponding event
 */
function splitHoverIn(evt) {
    id = $(this).prop('id');
    id = id.replace('splitItem-', '');
    id = id.replace('splitItemDiv-', '');
    id = id.replace('splitSegmentItem-', '');

    $('#splitItem-' + id).addClass('hover');
    $('#splitSegmentItem-' + id).addClass('hover');
}

/**
 * handler for hover out events on split segements and -list
 * 
 * @param evt the corresponding event
 */
function splitHoverOut(evt) {
    id = $(this).prop('id');
    id = id.replace('splitItem-', '');
    id = id.replace('splitItemDiv-', '');
    id = id.replace('splitSegmentItem-', '');

    $('#splitItem-' + id).removeClass('hover');
    $('#splitSegmentItem-' + id).removeClass('hover');
}

/******************************************************************************/
// events
/******************************************************************************/

/**
 * event handler for isSeeking
 */
function isSeeking() {}

/**
 * event handler for hasSeeked
 */

function hasSeeked() {}

/**
 * clearing events
 */
function clearEvents() {
    if (timeout1 != null) {
        window.clearTimeout(timeout1);
        timeout1 = null;
    }
    if (timeout2 != null) {
        window.clearTimeout(timeout2);
        timeout2 = null;
    }
    timeoutUsed = false;
}

/**
 * clearing events2
 */

function clearEvents2() {
    if (timeout3 != null) {
        window.clearTimeout(timeout3);
        timeout3 = null;
    }
    if (timeout4 != null) {
        window.clearTimeout(timeout4);
        timeout4 = null;
    }
    editor.player.on("play", {
        duration: 0,
        endTime: getDuration()
    }, onPlay);
    clearEvents();
}

/**
 * function executed when play event was thrown
 * 
 * @param evt
 *          the event
 */
function onPlay(evt) {
    if (timeout1 == null) {
        currEvt = evt;
        timeout1 = window.setTimeout(onTimeout, evt.data.duration);
    }
}

/**
 * the timeout function pausing the video again
 */
function onTimeout() {
    if (!timeoutUsed) {
        pauseVideo();
        var check = function () {
            endTime = currEvt.data.endTime;
            if (endTime > getCurrentTime()) {
                playVideo();
                timeout2 = window.setTimeout(check, 10);
                timeoutUsed = true;
            } else {
                clearEvents();
                pauseVideo();
                if ((timeout3 == null) && (timeout4 == null)) {
                    editor.player.on("play", {
                        duration: 0,
                        endTime: getDuration()
                    }, onPlay);
                }

                jumpBackTime = currEvt.data.jumpBackTime;
                jumpBackTime = ((jumpBackTime == null) || (jumpBackTime == undefined)) ? null : jumpBackTime;
                if (jumpBackTime != null) {
                    setCurrentTime(jumpBackTime);
                    jumpBackTime = null;
                }
            }
        }
        check();
    }
}

/******************************************************************************/
// play/pause
/******************************************************************************/

/**
 * play the video
 */
function playVideo() {
    editor.player[0].play();
}

/**
 * pause the video
 */
function pauseVideo() {
    if (!getPlayerPaused()) {
        editor.player[0].pause();
    }
}

/**
 * plays the current split item from it's beginning
 */
function playCurrentSplitItem() {
    var splitItem = getCurrentSplitItem();
    if (splitItem != null) {
        pauseVideo();
        var duration = (splitItem.clipEnd - splitItem.clipBegin) * 1000;
        setCurrentTime(splitItem.clipBegin);

        clearEvents();
        editor.player.on("play", {
            duration: duration,
            endTime: splitItem.clipEnd
        }, onPlay);
        playVideo();
    }
}

/**
 * play last 2 seconds of the current segment
 */
function playEnding() {
    var splitItem = getCurrentSplitItem();
    if (splitItem != null) {
        pauseVideo();
        var clipEnd = splitItem.clipEnd;
        setCurrentTime(clipEnd - 2);

        clearEvents();
        editor.player.on("play", {
            duration: 2000,
            endTime: clipEnd
        }, onPlay);
        playVideo();
    }
}

/**
 * play current segment -2s exclude the removed segments
 */
function playWithoutDeleted() {
    var splitItem = getCurrentSplitItem();

    if (splitItem != null) {
        pauseVideo();

        var clipStartFrom = -1;
        var clipStartTo = -1;
        var segmentStart = splitItem.clipBegin;
        var segmentEnd = splitItem.clipEnd;
        var clipEndFrom = -1;
        var clipEndTo = -1;
        var hasPrevElem = true;
        var hasNextElem = true;

        if ((splitItem.id - 1) >= 0) {
            hasPrevElem = true;
            var prevSplitItem = editor.splitData.splits[splitItem.id - 1];
            while (!prevSplitItem.enabled) {
                if ((prevSplitItem.id - 1) < 0) {
                    hasPrevElem = false;
                    break;
                } else {
                    prevSplitItem = editor.splitData.splits[prevSplitItem.id - 1];
                }
            }
            if (hasPrevElem) {
                clipStartTo = prevSplitItem.clipEnd;
                clipStartFrom = clipStartTo - 2;
            }
        }
        if (hasPrevElem) {
            clipStartFrom = (clipStartFrom < 0) ? 0 : clipStartFrom;
        }

        if ((splitItem.id + 1) < editor.splitData.splits.length) {
            hasNextElem = true;
            var nextSplitItem = editor.splitData.splits[splitItem.id + 1];
            while (!nextSplitItem.enabled) {
                if ((nextSplitItem.id + 1) >= editor.splitData.splits.length) {
                    hasNextElem = false;
                    break;
                } else {
                    nextSplitItem = editor.splitData.splits[nextSplitItem.id + 1];
                }
            }
            if (hasNextElem) {
                clipEndFrom = nextSplitItem.clipBegin;
                clipEndTo = clipEndFrom + 2;
            }
        }
        if (hasNextElem) {
            var duration = getDuration();
            clipEndTo = (clipEndTo > duration) ? duration : clipEndTo;
        }

        ocUtils.log("Play Times: " +
		    clipStartFrom + " - " +
		    clipStartTo + " | " +
		    segmentStart + " - " +
		    segmentEnd + " | " +
		    clipEndFrom + " - " +
		    clipEndTo);

        if (hasPrevElem && hasNextElem) {
	    currSplitItemClickedViaJQ = true;
            setCurrentTime(clipStartFrom);
            clearEvents();
            editor.player.on("play", {
                duration: (clipStartTo - clipStartFrom) * 1000,
                endTime: clipStartTo
            }, onPlay);

            playVideo();

            timeout3 = window.setTimeout(function () {
                pauseVideo();
                currSplitItemClickedViaJQ = true;
                setCurrentTime(segmentStart);
                clearEvents();
                editor.player.on("play", {
                    duration: (segmentEnd - segmentStart) * 1000,
                    endTime: segmentEnd
                }, onPlay);
                playVideo();
            }, (clipStartTo - clipStartFrom) * 1000);

            timeout4 = window.setTimeout(function () {
                pauseVideo();
                if (timeout3 != null) {
                    window.clearTimeout(timeout3);
                    timeout3 = null;
                }
                currSplitItemClickedViaJQ = true;
                setCurrentTime(clipEndFrom);
                clearEvents();
                editor.player.on("play", {
                    duration: (clipEndTo - clipEndFrom) * 1000,
                    endTime: clipEndTo
                }, onPlay);
                playVideo();
                if (timeout4 != null) {
                    window.clearTimeout(timeout4);
                    timeout4 = null;
                }
            }, ((clipStartTo - clipStartFrom) * 1000) + ((segmentEnd - segmentStart) * 1000));
        } else if (!hasPrevElem && hasNextElem) {
	    currSplitItemClickedViaJQ = true;
            setCurrentTime(segmentStart);
            clearEvents();
            editor.player.on("play", {
                duration: (segmentEnd - segmentStart) * 1000,
                endTime: segmentEnd
            }, onPlay);

            playVideo();

            timeout3 = window.setTimeout(function () {
                pauseVideo();
                currSplitItemClickedViaJQ = true;
                setCurrentTime(clipEndFrom);
                clearEvents();
                editor.player.on("play", {
                    duration: (clipEndTo - clipEndFrom) * 1000,
                    endTime: clipEndTo
                }, onPlay);
                playVideo();
                if (timeout3 != null) {
                    window.clearTimeout(timeout3);
                    timeout3 = null;
                }
            }, ((segmentEnd - segmentStart) * 1000));
        } else if (hasPrevElem && !hasNextElem) {
            currSplitItemClickedViaJQ = true;
            setCurrentTime(clipStartFrom);
            clearEvents();
            editor.player.on("play", {
                duration: (clipStartTo - clipStartFrom) * 1000,
                endTime: clipStartTo
            }, onPlay);

            playVideo();

            timeout3 = window.setTimeout(function () {
                pauseVideo();
                currSplitItemClickedViaJQ = true;
                setCurrentTime(segmentStart);
                clearEvents();
                editor.player.on("play", {
                    duration: (segmentEnd - segmentStart) * 1000,
                    endTime: segmentEnd
                }, onPlay);
                playVideo();
                if (timeout3 != null) {
                    window.clearTimeout(timeout3);
                    timeout3 = null;
                }
            }, (clipStartTo - clipStartFrom) * 1000);
        } else if (!hasPrevElem && !hasNextElem) {
            clearEvents();
            editor.player.on("play", {
                duration: (segmentEnd - segmentStart) * 1000,
                endTime: segmentEnd
            }, onPlay);

            playVideo();
        }
    }
}

/******************************************************************************/
// jumps
/******************************************************************************/

/**
 * jump to beginning of current split item
 */
function jumpToSegment() {
    id = $(this).prop('id');
    id = id.replace('splitItem-', '');
    id = id.replace('splitItemDiv-', '');
    id = id.replace('splitSegmentItem-', '');

    setCurrentTime(editor.splitData.splits[id].clipBegin);
}

/**
 * jump to next segment
 */
function nextSegment() {
    var playerPaused = getPlayerPaused();
    if (!playerPaused) {
        pauseVideo();
    }

    var currSplitItem = getCurrentSplitItem();
    var new_id = currSplitItem.id + 1;

    new_id = (new_id >= editor.splitData.splits.length) ? 0 : new_id;

    var idFound = true;
    if ((new_id < 0) || (new_id >= editor.splitData.splits.length)) {
	idFound = false;
    } else if(!editor.splitData.splits[new_id].enabled) {
	idFound = false;
	new_id = (new_id >= (editor.splitData.splits.length - 1)) ? 0 : new_id;

	for(var i = new_id + 1; i < editor.splitData.splits.length; ++i) {
	    if(editor.splitData.splits[i].enabled) {
		new_id = i;
		idFound = true;
		break;
	    }
	}
    }
    if(!idFound) {
	for(var i = 0; i < new_id; ++i) {
	    if(editor.splitData.splits[i].enabled) {
		new_id = i;
		idFound = true;
		break;
	    }
	}   
    }

    if(idFound) {
	selectSegmentListElement(new_id, !playerPaused);
    }
    if (!playerPaused) {
	playVideo();
    }
}

/**
 * jump to previous segment
 */
function previousSegment() {
    var playerPaused = getPlayerPaused();
    if (!playerPaused) {
        pauseVideo();
    }
    var currSplitItem = getCurrentSplitItem();
    var new_id = currSplitItem.id - 1;
    new_id = (new_id < 0) ? (editor.splitData.splits.length - 1) : new_id;

    var idFound = true;
    if((new_id < 0) || (new_id >= editor.splitData.splits.length)) {
	idFound = false;
    } else if(!editor.splitData.splits[new_id].enabled) {
	idFound = false;
	new_id = (new_id <= 0) ? editor.splitData.splits.length : new_id;
	for(var i = new_id - 1; i >= 0; --i) {

	    if(editor.splitData.splits[i].enabled) {
		new_id = i;
		idFound = true;
		break;
	    }
	}
    }
    if(!idFound) {
	for(var i = editor.splitData.splits.length - 1; i >= 0; --i) {
	    if(editor.splitData.splits[i].enabled) {
		new_id = i;
		idFound = true;
		break;
	    }
	}
    }

    if(idFound) {
	selectSegmentListElement(new_id, !playerPaused);
    }
    if (!playerPaused) {
	playVideo();
    }
}

/******************************************************************************/
// other
/******************************************************************************/

/**
 * add all shortcuts
 */
function addShortcuts() {
    $.ajax({
        url: ME_JSON,
        dataType: "json",
        async: false,
        success: function (data) {
            $.each(data.org.properties, function (key, value) {
                default_config[key] = value;
                $('#' + key.replace(".", "_")).html(value);
            });
        }
    });

    $.each(default_config, function (key, value) {
        $('#' + key.replace(".", "_")).html(value);
    });

    // add shortcuts for easier editing
    shortcut.add(default_config[SPLIT_AT_CURRENT_TIME], splitButtonClick, {
        disable_in_input: true,
    });
    shortcut.add(default_config[PREVIOUS_FRAME], function () {
        pauseVideo();
        $('.video-prev-frame').click();
    }, {
        disable_in_input: true,
    });
    shortcut.add(default_config[NEXT_FRAME], function () {
        pauseVideo();
        $('.video-next-frame').click();
    }, {
        disable_in_input: true,
    });
    shortcut.add(default_config[PLAY_PAUSE], function () {
        if (getPlayerPaused()) {
            playVideo();
        } else {
            pauseVideo();
        }
    }, {
        disable_in_input: true,
    });
    shortcut.add(default_config[PLAY_CURRENT_SEGMENT], playCurrentSplitItem, {
        disable_in_input: true,
    });
    shortcut.add(default_config[DELETE_SELECTED_ITEM], splitRemoverClick, {
        disable_in_input: true,
    });
    shortcut.add(default_config[SELECT_ITEM_AT_CURRENT_TIME], selectCurrentSplitItem, {
        disable_in_input: true,
    });
    shortcut.add(default_config[SET_CURRENT_TIME_AS_INPOINT], setCurrentTimeAsNewInpoint, {
        disable_in_input: true,
    });
    shortcut.add(default_config[SET_CURRENT_TIME_AS_OUTPOINT], setCurrentTimeAsNewOutpoint, {
        disable_in_input: true,
    });
    shortcut.add(default_config[NEXT_MARKER], nextSegment, {
        disable_in_input: true,
    });
    shortcut.add(default_config[PREVIOUS_MARKER], previousSegment, {
        disable_in_input: true,
    });
    shortcut.add(default_config[PLAY_ENDING_OF_CURRENT_SEGMENT], playEnding, {
        disable_in_input: true,
    });
    shortcut.add(default_config[PLAY_CURRENT_PRE_POST], playWithoutDeleted, {
        disable_in_input: true
    });
}

/**
 * init the playbuttons in the editing box
 */
function initPlayButtons() {
    $('#clipBeginSet, #clipEndSet').button({
        text: false,
        icons: {
            primary: "ui-icon-arrowthickstop-1-s"
        }
    });

    $('#clipBeginSet').click(setCurrentTimeAsNewInpoint);
    $('#clipEndSet').click(setCurrentTimeAsNewOutpoint);

    $('#shortcuts').button();
    $('#shortcuts').click(function () {
	$("#rightBoxDescription").toggle();
	$("#rightBox").toggle();
    });

    $('#clearList').button();

    $('#clearList').click(function () {
        editor.splitData.splits = [];
        // create standard split point
        editor.splitData.splits.push({
            clipBegin: 0,
            clipEnd: workflowInstance.mediapackage.duration / 1000,
            enabled: true,
            description: ""
        });
        editor.updateSplitList();
        selectSegmentListElement(0);
    });
}

function setWaveformWidth(value) {
    if(value == 1) {
	currentWaveformWidth = initialWaveformWidth;
    } else if(value > currWaveformZoom) {
	currentWaveformWidth = initialWaveformWidth + value * waveformZoomFactor;
    } else if(value < currWaveformZoom) {
	var maxWidth = initialWaveformWidth + maxWaveformZoomSlider * waveformZoomFactor;
	var newWidth = maxWidth - (maxWaveformZoomSlider - value) * waveformZoomFactor;
	newWidth = (newWidth >= initialWaveformWidth) ? newWidth : initialWaveformWidth;
	currentWaveformWidth = newWidth;
    }
    $('#segmentsWaveform').width(currentWaveformWidth);
    $('#waveformImage').width(currentWaveformWidth);
    positionWaveformAndTimeIndicator();
    currWaveformZoom = value;
}

/**
 * prepares the UI
 */
function prepareUI() {
    // update split list and enable the editor
    editor.updateSplitList();
    $('#editor').removeClass('disabled');

    // try to load waveform image
    if (editor.mediapackageParser.mediapackage.attachments) {
        ocUtils.log("Found waveform");
        $.each(ocUtils.ensureArray(editor.mediapackageParser.mediapackage.attachments.attachment), function (key, value) {
            if (value.type == WAVEFORM_FLAVOR) {
                $('#waveformImage').prop("src", value.url);
                $('#waveformImage').load(function () {
                    $('#segmentsWaveform').height($('#waveformImage').height());
                    $('#segmentsWaveform').width($('#videoHolder').width());
		    initialWaveformWidth = $('#segmentsWaveform').width();
		    currentWaveformWidth = initialWaveformWidth;
		    currWaveformZoom = 1;
		    waveformImageLoadDone = true;
		    $("#slider-waveform-zoom").slider({
			range: "min",
			value: 1,
			min: 1,
			max: maxWaveformZoomSlider,
			slide: function(event, ui) {
			    setWaveformWidth(ui.value);
			}
		    });
		    $("#waveformControls").show();
                });
                $(window).resize(function (evt) {
		    if(waveformImageLoadDone) {
			$('#segmentsWaveform').height($('#waveformImage').height());
			$('#segmentsWaveform').width($('#videoHolder').width());
			initialWaveformWidth = $('#segmentsWaveform').width();
			currentWaveformWidth = initialWaveformWidth;
			currWaveformZoom = 1;
			$("#slider-waveform-zoom").slider("option", "value", 1);
			$('.holdStateUI').height($('#segmentsWaveform').height() + $('#videoPlayer').height() + 70);
			setWaveformWidth(currWaveformZoom);
		    }
                });
            }
        });
    } else {
        ocUtils.log("Did not find waveform");
        $('#waveformImage').hide();
        $('#slider-waveform-zoom').hide();
    }

    // adjust size of the holdState UI
    var height = parseInt($('.holdStateUI').css('height').replace("px", ""));
    var heightVideo = parseInt($('#videoHolder').css('height').replace("px", ""));
    $('#videoHolder').css('width', '98%');
    $('.holdStateUI').css('height', (height + heightVideo) + "px");
    parent.ocRecordings.adjustHoldActionPanelHeight();

    // grab the focus in the Iframe so one can use the keyboard shortcuts
    $(window).focus();

    // add click handler for btns in control bar
    $('.video-previous-marker').click(previousSegment);
    $('.video-next-marker').click(nextSegment);
    $('.video-play-pre-post').click(playWithoutDeleted);

    // add timelistener for current time in description div
    editor.player.on("timeupdate", function () {
        selectCurrentSplitItem();
    });

    // add evtl handler for enter in editing fields
    $('#clipBegin input').keyup(function (e) {
        var keyCode = e.keyCode || e.which();
        if (keyCode == KEY_ENTER) {
            okButtonClick();
        }
    });
    $('#clipEnd input').keyup(function (e) {
        var keyCode = e.keyCode || e.which();
        if (keyCode == KEY_ENTER) {
            okButtonClick();
        }
    });

    selectCurrentSplitItem();
}

/**
 * parses the initial smil file and adds segments if already available
 */
function parseInitialSMIL() {
    if (editor.parsedSmil) {
        ocUtils.log("smil found. Parsing...");
	var insertedSplitItem = false;
        var newStart = false;
        // check whether SMIL has already cutting points
        if (editor.parsedSmil.par) {
            editor.splitData.splits = [];
            editor.parsedSmil.par = ocUtils.ensureArray(editor.parsedSmil.par);
            $.each(editor.parsedSmil.par, function (key, value) {
                value.video = ocUtils.ensureArray(value.video);
                var clipBegin = parseFloat(value.video[0].clipBegin) / 1000;
                var clipEnd = parseFloat(value.video[0].clipEnd) / 1000;
                ocUtils.log("Found a split element (" + clipBegin + " - " + clipEnd + ")");
		// check whether split element is big enough
		if((clipEnd - clipBegin) > 0.02) {
                    if ((editor.splitData.splits.length == 0)
			&& (clipBegin > 0.02)) {
			newStart = true;
			editor.splitData.splits.push({
                            clipBegin: 0,
                            clipEnd: clipBegin,
                            enabled: false,
                            description: ""
			});
                    }
                    editor.splitData.splits.push({
			clipBegin: clipBegin,
			clipEnd: clipEnd,
			enabled: true,
			description: value.video[0].description ? value.video[0].description : ""
                    });

		    checkPrevAndNext(editor.splitData.splits.length - 1);

		    if(clipEnd < (getDuration() - 0.1)) {
			editor.splitData.splits[editor.splitData.splits.length - 1].enabled = false;
		    }
		    /*
                      if (clipEnd < editor.parsedSmil.trackDuration / 1000 / 60) {
                      editor.splitData.splits.push({
                      clipBegin: clipEnd,
                      clipEnd: editor.parsedSmil.trackDuration,
                      enabled: false,
                      description: ""
                      });
                      }
		    */
		    insertedSplitItem = true;
		} else {
                    ocUtils.log("Split element not inserted due to a too short duration");
		}
            });
        }
        ocUtils.log("Done");
    } else {
        ocUtils.log("No smil found.");
    }
    ocUtils.log("Inserting new split segment...");
    if(!insertedSplitItem) {
	editor.splitData.splits.push({
	    clipBegin: 0,
	    clipEnd: editor.mediapackageParser.duration / 1000,
	    enabled: true,
	    description: ""
	});
    }
    window.setTimeout(function () {
	if (!newStart) {
	    $('#splitSegmentItem-0').click();
	    if (editor.splitData.splits.length == 1) {
		$('#splitSegmentItem-0').css('width', '100%');
	    }
	} else {
	    $('#splitSegmentItem-1').click();
	}
    }, 200);
    prepareUI();
}

function positionWaveformAndTimeIndicator() {
    var duration = workflowInstance.mediapackage.duration / 1000;
    var perc = getCurrentTime() / duration * 100;
    var imgDivWidth = $('#videoHolder').width();
    var imgWidth = $('#waveformImage').width();

    $('#currentTimeDiv').css("left", perc + "%");

    var c = (perc / 100) * (imgWidth - imgDivWidth);
    $("#segmentsWaveform").css('left', -c);
}

/**
 * when player is ready
 */
function playerReady() {
    if (!editor.ready) {
        editor.ready = true;

        // create additional data output
        $('#videoHolder').append('<div id="segmentsWaveform"></div>');
        $('#segmentsWaveform').append('<div id="splitSegments"></div>');
        $('#segmentsWaveform').append('<div id="imageDiv"><img id="waveformImage" alt="waveform"/></div>');
        $('#segmentsWaveform').append('<div id="currentTimeDiv"></div>');

        editor.player.bind("timeupdate", function () {
            positionWaveformAndTimeIndicator();
        });

        // create standard split point
        editor.splitData.splits.push({
            clipBegin: 0,
            clipEnd: workflowInstance.mediapackage.duration / 1000,
            enabled: true,
            description: ""
        });

        editor.smil = null;
        editor.parsedSmil = null;
        $.each(workflowInstance.mediapackage.metadata.catalog, function (key, value) {
            // load smil if there is already one
            if (value.type == SMIL_FLAVOR) {
                // download smil
                // mediapackage: smil/smil
                editor.getSmil(function () {
                    parseInitialSMIL();
                });
            }
        });
    }
}

/**
 * document fully loaded
 */
$(document).ready(function () {
    if(!editor.getWorkflowID()) {
	$("#editor").remove();
	$("#editorMetaData").remove();
	$("#videoContainer").remove();
	$("#mhVideoPlayer").remove();
	$("#boxes").remove();
	$("#continueButton").remove();
	$("#holdActionUI").remove();
	$("#cancelButton").attr('title', 'Back').html('Back');
	return false;
    }

    editor.player = $('#videoPlayer');
    editor.player.on("canplay", playerReady);
    editor.player.on("seeking", isSeeking);
    editor.player.on("seeked", hasSeeked);
    $('.clipItem').timefield();
    $('.video-split-button').click(splitButtonClick);
    $('#okButton').click(okButtonClick);
    $('#cancelButton').click(cancelButtonClick);
    $('#deleteButton').click(splitRemoverClick);

    $('#deleteButton').button();
    $('#cancelButton').button();
    $('#okButton').button();
    
    enableInspectorBox(false);
    initPlayButtons();
    addShortcuts();

    checkClipBegin();
    checkClipEnd();

    $(document).keydown(function (e) {
        var keyCode = e.keyCode || e.which();
        if (keyCode == KEY_SPACE) {
            clearEvents2();
        }
        if (!$('#clipBegin').is(":focus") && !$('#clipEnd').is(":focus") && ((keyCode == KEY_LEFT) || (keyCode == KEY_UP) || (keyCode == KEY_RIGHT) || (keyCode == KEY_DOWN))) {
            isSeeking = true;
            return false;
        }
    }).keyup(function (e) {
        var keyCode = e.keyCode || e.which();
        if ((keyCode == KEY_LEFT) || (keyCode == KEY_UP) || (keyCode == KEY_RIGHT) || (keyCode == KEY_DOWN)) {
            isSeeking = false;
            lastTimeSplitItemClick = new Date();
            return false;
        }
    });

    $(document).click(function () {
        if (!currSplitItemClickedViaJQ) {
            clearEvents2();
        }
        currSplitItemClickedViaJQ = false;
    });

    $(window).resize(function () {
        if (this.resizeTO) {
            clearTimeout(this.resizeTO);
        }
        this.resizeTO = setTimeout(function () {
            $(this).trigger('resizeEnd');
        }, windowResizeMS);
    });

    $(window).bind('resizeEnd', function () {
        // window has not been resized in windowResizeMS ms
        editor.updateSplitList();
        selectCurrentSplitItem();
    });

    window.setTimeout(function () {
        selectCurrentSplitItem();

        if (!$.browser.webkit && !$.browser.mozilla) {
            playVideo();
            pauseVideo();
            setCurrentTime(0);
        }
    }, initMS);
});
