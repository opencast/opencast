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
 * @namespace the global Opencast namespace Series
 */
Opencast.Series = (function ()
{
    var mediaPackageId;
    var series_id;
    var series_visible = false;
    var position_set = false;
    
    /**
     * @memberOf Opencast.Series
     * @description toogle series Dropdown
     */
    function doToggleSeriesDropdown()
    {
        if (series_visible)
        {
            hideSeriesDropdown();
        }
        else
        {
            showSeriesDropdown()
            $('#oc_series').focus();
        }
    }
    
    /**
     * @memberOf Opencast.Series
     * @description Show the Series Page
     * @param page Page
     */
    function showSeriesPage(page)
    {
        // Request JSONP data
        $.ajax(
        {
            url: Opencast.Watch.getSeriesSeriesURL(),
            data: 'id=' + series_id + '&episodes=true&limit=20&offset=' + ((page - 1) * 20),
            dataType: 'jsonp',
            jsonp: 'jsonp',
            success: function (data)
            {
                $.log("Series AJAX call: Requesting data succeeded");
                data = createDataForPlugin(data);
                data['search-results'].currentPage = page;
                //add as a plugin
                Opencast.Series_Plugin.addAsPlugin($('#oc_series'), data['search-results']);
            },
            // If no data comes back
            error: function (xhr, ajaxOptions, thrownError)
            {
                $.log("Series Ajax call: Requesting data failed");
                Opencast.Player.addEvent(Opencast.logging.SERIES_PAGE_AJAX_FAILED);
            }
        });
    }
    
    /**
     * @memberOf Opencast.Series
     * @description hide series Dropdown
     */
    function hideSeriesDropdown()
    {
        $('#oc_series').hide();
        $('#oc_series').attr(
        {
            'aria-hidden': 'true',
            'tabindex': '-1'
        });
        series_visible = false;
    }
    
    /**
     * @memberOf Opencast.Series
     * @description show series Dropdown
     */
    function showSeriesDropdown()
    {
        $.ajax(
        {
            url: Opencast.Watch.getSeriesSeriesURL(),
            data: 'id=' + series_id + '&episodes=true&limit=20&offset=0',
            dataType: 'jsonp',
            jsonp: 'jsonp',
            success: function (data)
            {
                $.log("Series AJAX call: Requesting data succeeded");
                // get rid of every '@' in the JSON data
                data = $.parseJSON(JSON.stringify(data).replace(/@/g, ''));
                data = createDataForPlugin(data);
                data['search-results'].currentPage = 1;
                //add as a plugin
                Opencast.Series_Plugin.addAsPlugin($('#oc_series'), data['search-results']);
                //set position of div and make it visible
                if (!position_set)
                {
                    $("#oc_series").position(
                    {
                        of: $("#oc_see-more-button"),
                        my: "left top",
                        at: "left bottom"
                    });
                    position_set = true;
                    $(window).resize(function ()
                    {
                        $("#oc_series").position(
                        {
                            of: $("#oc_see-more-button"),
                            my: "left top",
                            at: "left bottom"
                        });
                    });
                }
                $('#oc_series').show();
                $('#oc_series').attr(
                {
                    'aria-hidden': 'false',
                    'tabindex': '0'
                });
                series_visible = true;
            },
            error: function (data)
            {
              Opencast.Player.addEvent(Opencast.logging.SERIES_DROPDOWN_AJAX_FAILED);
            }
        });
    }
    
    /**
     * @memberOf Opencast.Series
     * @description Prepares the Data for the Plugin
     * @param data Data
     * @return Processed Data
     */
    function createDataForPlugin(data)
    {
        //set current mediapackageId for
        data['search-results'].currentMediaPackageId = mediaPackageId;
        //if there is only one episode in the series make it an array
        if (data['search-results'].result.length == undefined)
        {
            var tmp = new Array(data['search-results'].result);
            data['search-results'].result = tmp;
        }
        //reverse the array to get the results in chronical order
        data['search-results'].result.reverse();
        //change date format to MM/DD/YYYY
        //add number
        //cut title and add '...'
        for (var i = 0; i < data['search-results'].result.length; i++)
        {
            data['search-results'].result[i].dcCreated = $.getLocaleDate(data['search-results'].result[i].dcCreated);
            data['search-results'].result[i].dcNumber = i + 1;
            data['search-results'].result[i].dcTitleShort = data['search-results'].result[i].dcTitle.substr(0, 35) + "...";
        }
        //create pages
        data['search-results'].pages = [];
        for (var i = 1; i <= Math.ceil(data['search-results'].total / 20); i++)
        {
            data['search-results'].pages.push(i);
        }
        return data;
    }
    
    /**
     * @memberOf Opencast.Series
     * @description Set the mediaPackageId
     * @param String mediaPackageId
     */
    function setMediaPackageId(id)
    {
        mediaPackageId = id;
        $.ajax(
        {
            url: Opencast.Watch.getSeriesEpisodeURL(),
            data: 'id=' + mediaPackageId,
            dataType: 'jsonp',
            jsonp: 'jsonp',
            success: function (data)
            {
                $.log("Series AJAX call #1: Requesting data succeeded");
                if ((data !== undefined) && (data['search-results'] !== undefined) && (data['search-results'].result !== undefined))
                {
                    $.log("Series AJAX call: Data available");
                    series_id = data['search-results'].result.dcIsPartOf;
                    if (series_id != '')
                    {
                        $.ajax(
                        {
                            url: Opencast.Watch.getSeriesSeriesURL(),
                            data: 'id=' + series_id + '&episodes=true&limit=20&offset=0',
                            dataType: 'jsonp',
                            jsonp: 'jsonp',
                            success: function (data)
                            {
                                $.log("Series AJAX call #2: Requesting data succeeded");
                                if((data['search-results'] != null) && (data['search-results'].result != null))
                                {
                                    if (data['search-results'].result.length > 1)
                                    {
                                        $('#oc_player-head-see-more').show();
                                    }
                                }
                            },
                            // If no data comes back
                            error: function (xhr, ajaxOptions, thrownError)
                            {
                                $.log("Series Ajax call #2: Requesting data failed");
                                Opencast.Player.addEvent(Opencast.logging.SERIES_EPISODES_AJAX_FAILED);
                            }
                        });
                    }
                } else
                {
                    $.log("Series AJAX call: Data not available");
                }
            },
            // If no data comes back
            error: function (xhr, ajaxOptions, thrownError)
            {
                $.log("Series Ajax call #1: Requesting data failed");
                Opencast.Player.addEvent(Opencast.logging.SERIES_INFO_AJAX_FAILED);
            }
        });
    }
    
    return {
        showSeriesDropdown: showSeriesDropdown,
        hideSeriesDropdown: hideSeriesDropdown,
        setMediaPackageId: setMediaPackageId,
        showSeriesPage: showSeriesPage,
        doToggleSeriesDropdown: doToggleSeriesDropdown
    };
}());
