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
 * @namespace the global Opencast namespace Description
 */
Opencast.Description = (function ()
{
    var mediaPackageId, duration;
    var DESCRIPTION = "Description",
        DESCRIPTION_HIDE = "Hide Description";
    var defaultChar = '-';
    
    /**
     * @memberOf Opencast.Description
     * @description Displays the Description Tab
     */
    function showDescription()
    {
        Opencast.Player.addEvent("SHOW-DESCRIPTION");
        // Hide other Tabs
        Opencast.segments.hideSegments();
        Opencast.segments_text.hideSegmentsText();
        Opencast.search.hideSearch();
        // Change Tab Caption
        $('#oc_btn-description').attr(
        {
            title: DESCRIPTION_HIDE
        });
        $('#oc_btn-description').html(DESCRIPTION_HIDE);
        $("#oc_btn-description").attr('aria-pressed', 'true');
        // Show a loading Image
        $('#oc_description').show();
        $('#description-loading').show();
        $('#oc-description').hide();
        // If cashed data are available
        if (Opencast.Description_Plugin.createDescriptionFromCashe())
        {
            $.log("Cashing description plugin: yes");
            // Make visible
            $('#description-loading').hide();
            $('#oc-description').show();
        }
        else
        {
            $.log("Cashing description plugin: no");
            // Request JSONP data
            $.ajax(
            {
                url: Opencast.Watch.getDescriptionEpisodeURL(),
                data: 'id=' + mediaPackageId,
                dataType: 'jsonp',
                jsonp: 'jsonp',
                success: function (data)
                {
                    $.log("Description AJAX call #1: Requesting data succeeded");
                    if ((data === undefined) || (data['search-results'] === undefined) || (data['search-results'].result === undefined))
                    {
                        displayNoDescriptionAvailable("No data defined");
                        return;
                    }
                    // Process data
                    // Trimpath throws (no) errors if a variable is not defined => assign default value
                    data['search-results'].defaultChar = defaultChar;
                    data['search-results'].result.dcSeriesTitle = checkForNullUndef(data['search-results'].result.mediapackage.seriestitle, defaultChar);
                    data['search-results'].result.dcContributor = checkForNullUndef(data['search-results'].result.dcContributor, defaultChar);
                    data['search-results'].result.dcLanguage = checkForNullUndef(data['search-results'].result.dcLanguage, defaultChar);
                    data['search-results'].result.dcCreator = checkForNullUndef(data['search-results'].result.dcCreator, defaultChar);
                    data['search-results'].result.dcViews = checkForNullUndef(data['search-results'].result.dcViews, defaultChar);
                    data['search-results'].result.dcCreated = checkForNullUndef(data['search-results'].result.dcCreated, defaultChar);
                    // format date if date is available
                    if (data['search-results'].result.dcCreated != defaultChar)
                    {
                        var sd = $.dateStringToDate(data['search-results'].result.dcCreated);
                        data['search-results'].result.dcCreated = $.getDateString(sd) + ' - ' + $.getTimeString(sd);
                    }
                    // Request JSONP data (Stats)
                    $.ajax(
                    {
                        url: Opencast.Watch.getDescriptionStatsURL(),
                        data: 'id=' + mediaPackageId,
                        dataType: 'jsonp',
                        jsonp: 'jsonp',
                        success: function (result)
                        {
                            $.log("Description AJAX call #2: Requesting data succeeded");
                            var views = checkForNullUndef(result.stats.views);
                            if ((result.stats.views == 0) || (views != defaultChar))
                            {
                                data['search-results'].result.dcViews = result.stats.views;
                            }
                            // Create Trimpath Template
                            var descriptionSet = Opencast.Description_Plugin.addAsPlugin($('#oc-description'), data['search-results']);
                            if (!descriptionSet)
                            {
                                displayNoDescriptionAvailable("No template available");
                            }
                            else
                            {
                                // Make visible
                                $('#description-loading').hide();
                                $('#oc-description').show();
                            }
                        },
                        // If no data comes back (JSONP-Call #2)
                        error: function (xhr, ajaxOptions, thrownError)
                        {
                            $.log("Description Ajax call #1: Requesting data failed");
                            Opencast.Player.addEvent("DESCRIPTION-AJAX-1-FAILED");
                            displayNoDescriptionAvailable("No data available");
                        }
                    });
                },
                // If no data comes back (JSONP-Call #1)
                error: function (xhr, ajaxOptions, thrownError)
                {
                    $.log("Description Ajax call #2: Requesting data failed");
                    Opencast.Player.addEvent("DESCRIPTION-AJAX-2-FAILED");
                    displayNoDescriptionAvailable("No data available");
                }
            });
        }
    }
    
    /**
     * @memberOf Opencast.Description
     * @description Checks an Object for null and undefined
     * @param toCheck Object to check
     * @param char default character to return if toCheck is null or undefined
     * @return char if object if null or undefined, toCheck else
     */
    function checkForNullUndef(toCheck, defaultChar)
    {
        if (!toCheck || (toCheck === null) || (toCheck === undefined))
        {
            return defaultChar;
        }
        return toCheck;
    }
    
    /**
     * @memberOf Opencast.Description
     * @description Displays that no Description is available
     * @param errorDesc Error Description (optional)
     */
    function displayNoDescriptionAvailable(errorDesc)
    {
        errorDesc = errorDesc || '';
        $('#description-loading').hide();
        var optError = (errorDesc != '') ? (": " + errorDesc) : '';
        $('#oc-description').html('No Description available' + optError);
        $('#oc-description').show();
        $('#scrollcontainer').hide();
    }
    
    /**
     * @memberOf Opencast.Description
     * @description Hides the Description Tab
     */
    function hideDescription()
    {
        // Change Tab Caption
        $('#oc_btn-description').attr(
        {
            title: DESCRIPTION
        });
        $('#oc_btn-description').html(DESCRIPTION);
        $("#oc_btn-description").attr('aria-pressed', 'false');
        $('#oc_description').hide();
    }
    
    /**
     * @memberOf Opencast.Description
     * @description Toggles the Description Tab
     */
    function doToggleDescription()
    {
        if ($('#oc_btn-description').attr("title") === DESCRIPTION)
        {
            Opencast.segments.hideSegments();
            Opencast.segments_text.hideSegmentsText();
            Opencast.search.hideSearch();
            showDescription();
        }
        else
        {
            hideDescription();
        }
    }
    
    /**
     * @memberOf Opencast.Description
     * @description Set the mediaPackageId
     * @param String mediaPackageId
     */
    function setMediaPackageId(id)
    {
        mediaPackageId = id;
    }
    
    return {
        showDescription: showDescription,
        hideDescription: hideDescription,
        setMediaPackageId: setMediaPackageId,
        doToggleDescription: doToggleDescription
    };
}());
