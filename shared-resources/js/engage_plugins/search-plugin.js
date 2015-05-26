/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
 
var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace search_Plugin
 */
Opencast.search_Plugin = (function ()
{
    // The Template to process
    var template =  '<table cellspacing="5" cellpadding="0" style="table-layout:fixed; empty-cells:hide">' +
                    '{for s in segment}' +
                        '{if s.display}' +
                            '<tr style="background-color:${s.backgroundColor};cursor:pointer;cursor:hand;">' +
                                '<td style="width:115px" class="oc-segments-preview">' +
                                    '<a onclick="Opencast.Watch.seekSegment(${Math.floor(parseInt(s.time) / 1000)})"><img width="111" alt="Slide ${parseInt(s.index) + 1} of ${segment.length}" src="${s.previews.preview.$}"></a>' +
                                '</td>' +
                                '<td style="width:90px; text-align:center;" onclick="Opencast.Watch.seekSegment(${Math.floor(parseInt(s.time) / 1000)})">' +
                                    '<a class="segments-time"' +
                                        'onclick="Opencast.Watch.seekSegment(${Math.floor(parseInt(s.time) / 1000)})">' +
                                        '${$.formatSeconds(Math.floor(parseInt(s.time) / 1000))}' +
                                    '</a>' +
                                '</td>' +
                                '<td style="text-align:left;" onclick="Opencast.Watch.seekSegment(${Math.floor(parseInt(s.time) / 1000)})">' +
                                    '<a onclick="Opencast.Watch.seekSegment(${Math.floor(parseInt(s.time) / 1000)})">${s.text}</a>' +
                                '</td>' +
                            '</tr>' +
                        '{/if}' +
                    '{forelse}' +
                        'No Segment Text available' +
                    '{/for}' +
                    '</table>';
    
    /**
     * @memberOf Opencast.search_Plugin
     * @description Returns the header
     * @param searchValue The search value
     * @return the header as a string
     */
    function getHeader(searchValue)
    {
        var ret = '<div id="searchValueDisplay" style="float:left">' +
                      'Results for &quot;' + unescape(searchValue) + '&quot;' +
                  '</div>';
        ret +=  '<div id="relevance-overview">' +
                    '<div style="text-align: center; margin-left: 1px; margin-right: 1px; border: 0px solid black;width: 60px; height: 15px; background-color: ' + Opencast.search.getThirdColor() + '; float: right">' +
                        '&gt; 70&#37;' +
                    '</div>' +
                    '<div style="text-align: center; margin-left: 1px; margin-right: 1px; border: 0px solid black;width: 60px; height: 15px; background-color: ' + Opencast.search.getSecondColor() + '; float: right">' +
                        '&lt; 70&#37;' +
                    '</div>' +
                    '<div style="text-align: center; margin-left: 1px; margin-right: 1px; border: 0px solid black;width: 60px; height: 15px; background-color: ' + Opencast.search.getFirstColor() + '; float: right">' +
                        '&lt; 30&#37;' +
                    '</div>' +
                    '<div style="float:right">' +
                        'Search Relevance:&nbsp;' +
                    '</div>' +
                '</div>';
        return ret;
    }

    // The Element to put the div into
    var element;
    // Data to process
    var search_data;
    // Precessed Data
    var processedTemplate = '';
    // Search Value
    var search_value = '';

    /**
     * @memberOf Opencast.search_Plugin
     * @description Add As Plug-in
     * @param elem Element to fill with the Data (e.g. a div)
     * @param data Data to fill the Element with
     * @return true if successfully processed, false else
     */
    function addAsPlugin(elem, data, value)
    {
        element = elem;
        search_data = data;
        
        search_value = value;
        return createSearch();
    }

    /**
     * @memberOf Opencast.search_Plugin
     * @description Processes the Data and puts it into the Element
     * @return true if successfully processed, false else
     */
    function createSearch()
    {
        if (element !== undefined)
        {
            $.log("Search Plugin: Data available, processing template");
            if (search_value !== '')
            {
                var newTemplate = getHeader(search_value) + '<br />' + template;
                processedTemplate = newTemplate.process(search_data);
            }
            else
            {
                processedTemplate = template.process(search_data);
            }
            element.html(processedTemplate);
            return true;
        } else
        {
            $.log("Search Plugin: No data available");
            return false;
        }
    }

    return {
        addAsPlugin: addAsPlugin
    };
}());
