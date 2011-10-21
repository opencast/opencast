/**
 *  Copyright 2009-2011 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
 
var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace Annotation_Chapter
 */
Opencast.Annotation_Chapter = (function ()
{
    var mediaPackageId, duration;
    var annotationChapterDisplayed = false;
    var ANNOTATION_CHAPTER = "Annotation",
        ANNOTATION_CHAPTERHIDE = "Annotation off";
    var annotationType = "chapter";
    
    /**
     * @memberOf Opencast.Annotation_Chapter
     * @description Initializes Annotation Chapter
     *              Checks whether Data are available. If not: Hide Annotations
     */
    function initialize()
    {
        // Request JSONP data
        $.ajax(
        {
            url: Opencast.Watch.getAnnotationURL(),
            data: 'episode=' + mediaPackageId + '&type=' + annotationType,
            dataType: 'json',
            jsonp: 'jsonp',
            success: function (data)
            {
                $.log("Annotation AJAX call: Requesting data succeeded");
                if ((data !== undefined) && (data['annotations'] !== undefined) && (data['annotations'].annotation !== undefined))
                {
                    $.log("Annotation AJAX call: Data available");
                    // Display the controls
                    $('#oc_checkbox-annotations').show();
                    $('#oc_label-annotations').show();
                    $('#oc_video-view').show();
                    Opencast.Analytics.initialize();
                }
                else
                {
                    $.log("Annotation AJAX call: Data not available");
                    displayNoAnnotationsAvailable("No data available");
                }
            },
            // If no data comes back
            error: function (xhr, ajaxOptions, thrownError)
            {
                $.log("Annotation Ajax call: Requesting data failed");
                Opencast.Player.addEvent(Opencast.logging.ANNOTATION_INIT_AJAX_FAILED);
                displayNoAnnotationsAvailable("No data available");
            }
        });
    }
    
    /**
     * @memberOf Opencast.Annotation_Chapter
     * @description Show Annotation_Chapter
     */
    function showAnnotation_Chapter()
    {
        Opencast.Player.addEvent(Opencast.logging.SHOW_ANNOTATIONS);
        // Request JSONP data
        $.ajax(
        {
            url: Opencast.Watch.getAnnotationURL(),
            data: 'id=' + mediaPackageId,
            dataType: 'json',
            jsonp: 'jsonp',
            success: function (data)
            {
                $.log("Annotation AJAX call: Requesting data succeeded");
                if ((data === undefined) || (data['annotations'] === undefined) || (data['annotations'].annotation === undefined))
                {
                    $.log("Annotation AJAX call: Data not available");
                    displayNoAnnotationsAvailable("No data defined");
                }
                else
                {
                    $.log("Annotation AJAX call: Data available");
                    data['annotations'].duration = duration * 1000; // duration is in seconds
                    data['annotations'].nrOfSegments = Opencast.segments.getNumberOfSegments();
                    var annoIndex = 0;
                    $(data['annotations'].annotation).each(function (i)
                    {
                        data['annotations'].annotation[i].index = annoIndex++;
                    });
                    // Create Trimpath Template
                    var annotSet = Opencast.Annotation_ChapterPlugin.addAsPlugin($('#annotation'), data['annotations']);
                    if (!annotSet)
                    {
                        displayNoAnnotationsAvailable("No template available");
                    }
                    else
                    {
                        annotationChapterDisplayed = true;
                        var analyticsVisible = Opencast.Analytics.isVisible();
                        // If Analytics is visible: Hide it before changing
                        if (analyticsVisible)
                        {
                            Opencast.Analytics.hideAnalytics();
                        }
                        $('#segmentstable').css('segment-holder-empty', 'none');
                        $("#annotation").show();
                        $('#segmentstable1').hide();
                        $('#segmentstable2').hide();
                        // If Analytics was visible: Display it again
                        if (analyticsVisible)
                        {
                            Opencast.Analytics.showAnalytics();
                        }
                    }
                }
            },
            // If no data comes back
            error: function (xhr, ajaxOptions, thrownError)
            {
                $.log("Annotation Ajax call: Requesting data failed");
                Opencast.Player.addEvent(Opencast.logging.ANNOTATION_CHAPTER_AJAX_FAILED);
                displayNoAnnotationsAvailable("No data available");
            }
        });
    }
    
    /**
     * @memberOf Opencast.Annotation_Chapter
     * @description Displays that no Annotation is available and hides Annotations
     * @param errorDesc Error Description (optional)
     */
    function displayNoAnnotationsAvailable(errorDesc)
    {
        errorDesc = errorDesc || '';
        var optError = (errorDesc != '') ? (": " + errorDesc) : '';
        $("#annotation").html("No Annotations available" + optError);
        $('#oc_checkbox-annotations').removeAttr("checked");
        $('#oc_checkbox-annotations').attr('disabled', true);
        $('#oc_checkbox-annotations').hide();
        $('#oc_label-annotations').hide();
        hideAnnotation_Chapter();
    }
    
    /**
     * @memberOf Opencast.Annotation_Chapter
     * @description Hide the Annotation
     */
    function hideAnnotation_Chapter()
    {
        $("#annotation").hide();
        $('#segmentstable1').show();
        $('#segmentstable2').show();
        annotationChapterDisplayed = false;
    }
    
    /**
     * @memberOf Opencast.Annotation_Chapter
     * @description Toggle Analytics
     */
    function doToggleAnnotation_Chapter()
    {
        if (!annotationChapterDisplayed)
        {
            showAnnotation_Chapter();
        }
        else
        {
            hideAnnotation_Chapter();
        }
        return true;
    }
    
    /**
     * @memberOf Opencast.Annotation_Chapter
     * @description Set the mediaPackageId
     * @param String mediaPackageId
     */
    function setMediaPackageId(id)
    {
        mediaPackageId = id;
    }
    
    /**
     * @memberOf Opencast.Annotation_Chapter
     * @description Set the duration
     * @param int duration
     */
    function setDuration(val)
    {
        duration = val;
    }
    
    return {
        initialize: initialize,
        hideAnnotation_Chapter: hideAnnotation_Chapter,
        showAnnotation_Chapter: showAnnotation_Chapter,
        setDuration: setDuration,
        setMediaPackageId: setMediaPackageId,
        doToggleAnnotation_Chapter: doToggleAnnotation_Chapter
    };
}());
