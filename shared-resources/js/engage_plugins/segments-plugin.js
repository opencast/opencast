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
 * @namespace the global Opencast namespace segments_Plugin
 */
Opencast.segments_Plugin = (function ()
{
    // The Template to process
    var template = '{for s in segment}' +
                        '<div id="panel_${s.index}" class="panel" style="float: left; position: relative;">' +
                            '<div role="button" class="inside" ' +
                                'onmouseover="Opencast.segments_ui.hoverSegment(${parseInt(s.hoverSegmentIndex)}, ${parseInt(s.index)})" ' +
                                'onmouseout="Opencast.segments_ui.hoverOutSegment(${parseInt(s.index)})">' +
                                    '<a href="javascript:Opencast.Watch.seekSegment(${parseInt(s.time) / 1000})">' +
                                        '<img alt="Slide ${parseInt(s.index) + 1} of ${segment.length}" ' +
                                            'src="${s.previews.preview.$}">' +
                                    '</a>' +
                            '</div>' +
                        '</div>' +
                    '{forelse}' +
                        'No Segments available' +
                    '{/for}';

    // The Element to put the div into
    var element;
    // Data to process
    var segments_data;
    // Processed Data
    var processedTemplateData = false;
    
    /**
     * @memberOf Opencast.segments_Plugin
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
     * @memberOf Opencast.segments_Plugin
     * @description Tries to work with the cashed data
     * @return true if successfully processed, false else
     */
    function createSegmentsFromCashe()
    {
        if ((processedTemplateData !== false) && (element !== undefined) && (segments_data.segment !== undefined) && (segments_data.segment.length > 0))
        {
            $.log("Series Plugin: Data available, processing template");
            element.html(processedTemplateData);
            return true;
        }
        else
        {
            $.log("Series Plugin: No data available");
            return false;
        }
    }
    
    /**
     * @memberOf Opencast.segments_Plugin
     * @description Processes the Data and puts it into the Element
     */
    function createSegments()
    {
        if ((element !== undefined) && (segments_data.segment !== undefined) && (segments_data.segment.length > 0))
        {
            processedTemplateData = template.process(segments_data);
            element.html(processedTemplateData);
        }
    }
    
    return {
        createSegmentsFromCashe: createSegmentsFromCashe,
        addAsPlugin: addAsPlugin
    };
}());
