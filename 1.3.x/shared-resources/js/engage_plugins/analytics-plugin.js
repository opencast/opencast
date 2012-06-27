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
 * @namespace the global Opencast namespace AnalyticsPlugin
 */
Opencast.AnalyticsPlugin = (function ()
{
    // The Element to put the div into
    var element;
    // Data to process
    var footprintData;
    
    /**
     * @memberOf Opencast.AnalyticsPlugin
     * @description Add As Plug-in
     * @param elem Element to put the Data in
     * @param data The Data to Process
     * @return true if successfully processed, false else
     */
    function addAsPlugin(elem, data)
    {
        element = elem;
        footprintData = data;
        return drawFootprints();
    }
    
    /**
     * @memberOf Opencast.AnalyticsPlugin
     * @description Resize Plug-in
     * @return true if successfully processed, false else
     */
    function resizePlugin()
    {
        return drawFootprints();
    }
    
    /**
     * @memberOf Opencast.AnalyticsPlugin
     * @description Draw footprintData into the element
     * @return true if successfully processed, false else
     */
    function drawFootprints()
    {
        if (element !== undefined)
        {
            $.log("Analytics Plugin: Data available, drawing foot prints");
            element.sparkline(footprintData, {
                type: 'line',
                spotRadius: '0',
                width: '100%',
                height: '25px'
            });
            return true;
        }
        else
        {
            $.log("Annotation Plugin: No data available");
            return false;
        }
    }
    
    return {
        addAsPlugin: addAsPlugin,
        resizePlugin: resizePlugin
    };
}());
