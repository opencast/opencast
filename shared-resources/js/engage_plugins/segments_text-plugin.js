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
 * @namespace the global Opencast namespace segments_text_Plugin
 */
Opencast.segments_text_Plugin = (function ()
{
    // The Template to process
    var template =  '<table cellspacing="5" cellpadding="0" width="100%">' +
                        '{for s in segment}' +
                            // Accessibility Feature - Comment in if you want to display only the Segments after the current Slider-Position
                            // '{if s.durationIncludingSegment >= currentTime}' +
                                '<tr>' +
                                    '<td width="15%" class="oc-segments-preview" style="cursor:pointer;cursor:hand;">' +
                                        '<a onclick="Opencast.Watch.seekSegment(${Math.floor(parseInt(s.time) / 1000)})"><img width="111" alt="Slide ${parseInt(s.index) + 1} of ${segment.length}" src="${s.previews.preview.$}"></a>' +
                                    '</td>' +
                                    '<td width="85%" align="left" onclick="Opencast.Watch.seekSegment(${Math.floor(parseInt(s.time) / 1000)})" style="cursor:pointer;cursor:hand;">' +
                                        '&nbsp;<a class="segments-time"' +
                                            'onclick="Opencast.Watch.seekSegment(${Math.floor(parseInt(s.time) / 1000)})">' +
                                            '${$.formatSeconds(Math.floor(parseInt(s.time) / 1000))}' +
                                        '</a>' +
                                        '&nbsp;<a onclick="Opencast.Watch.seekSegment(${Math.floor(parseInt(s.time) / 1000)})">${s.text}</a>' +
                                    '</td>' +
                                '</tr>' +
                            // '{/if}' +
                        '{forelse}' +
                            'No Segment Text available' +
                        '{/for}' +
                    '</table>';
    
    // The Element to put the div into
    var element;
    // Data to process
    var segments_data;
    // Precessed Data
    var processedTemplateData = false;
    
    /**
     * @memberOf Opencast.segments_text_Plugin
     * @description Add As Plug-in
     * @param elem Element to fill with the Data (e.g. a div)
     * @param data Data to fill the Element with
     */
    function addAsPlugin(elem, data)
    {
        element = elem;
        segments_data = data;
        createSegments();
    }
    
    /**
     * @memberOf Opencast.segments_text_Plugin
     * @description Tries to work with the cashed data
     * @return true if successfully processed, false else
     */
    function createSegmentsTextFromCashe()
    {
        if ((processedTemplateData !== false) && (element !== undefined) && (segments_data.segment !== undefined) && (segments_data.segment.length > 0))
        {
            element.html(processedTemplateData);
            return true;
        }
        else
        {
            return false;
        }
    }
    
    /**
     * @memberOf Opencast.segments_text_Plugin
     * @description Processes the Data and puts it into the Element
     */
    function createSegments()
    {
        if ((element !== undefined) && (segments_data.segment !== undefined) && (segments_data.segment.length > 0))
        {
            $.log("Segments Text Plugin: Data available, processing template");
            processedTemplateData = template.process(segments_data);
            element.html(processedTemplateData);
        } else
        {
            $.log("Segments Text Plugin: No data available");
        }
    }
    
    return {
        createSegmentsTextFromCashe: createSegmentsTextFromCashe,
        addAsPlugin: addAsPlugin
    };
}());
