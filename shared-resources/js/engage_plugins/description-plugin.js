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
 * @namespace the global Opencast namespace Description_Plugin
 */
Opencast.Description_Plugin = (function ()
{
    // The Template to process
    var template =  '<div>' +
                        'Date:&nbsp;<span style="color:grey;">${result.dcCreated}</span><br />' +
                        'Contributor:&nbsp;<span style="color:grey;">${result.dcContributor}</span><br />' +
                        'Language:&nbsp;<span style="color:grey;">${result.dcLanguage}</span><br />' +
                        'Views:&nbsp;<span style="color:grey;">${result.dcViews}</span><br />' +
                        // 'See related Videos: <span style="color:grey;"></span><br />' +
                        'Series:&nbsp;<span style="color:grey;">${result.dcSeriesTitle}</span><br />' +
                        'Presenter:&nbsp;' +
                        '{if result.dcCreator != defaultChar}' +
                            '<a href="../../engage/ui/index.html?q=${result.dcCreator}">${result.dcCreator}</a><br />' +
                        '{else}' +
                            '<span style="color:grey;">${result.dcCreator}</span><br />' +
                        '{/if}' +
                        'Description:&nbsp;<span style="color:grey;">${result.dcDescription}</span><br />' +
                        '{if result.dcLicense != defaultChar}' +
                            'License:&nbsp;<span style="color:grey;">${result.dcLicense}</span><br />' +
                        '{/if}' +
                    '</div>' +
                    '<div style="clear: both">' + 
                    '</div>';
                    
    // The Element to put the div into
    var element;
    // Data to process
    var description_data;
    // Precessed Data
    var processedTemplateData = false;
    
    /**
     * @memberOf Opencast.Description_Plugin
     * @description Add As Plug-in
     * @param elem Element to fill with the Data (e.g. a div)
     * @param data Data to fill the Element with
     * @return true if successfully processed, false else
     */
    function addAsPlugin(elem, data)
    {
        element = elem;
        description_data = data;
        return createDescription();
    }
    
    /**
     * @memberOf Opencast.Description_Plugin
     * @description Tries to work with the cashed data
     * @return true if successfully processed, false else
     */
    function createDescriptionFromCashe()
    {
        if ((processedTemplateData !== false) && (element !== undefined) && (description_data !== undefined))
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
     * @memberOf Opencast.Description_Plugin
     * @description Processes the Data and puts it into the Element
     * @return true if successfully processed, false else
     */
    function createDescription()
    {
        if ((element !== undefined) && (description_data !== undefined))
        {
            $.log("Description Plugin: Data available, processing template");
            if((description_data != undefined) &&
               (description_data.result != undefined))
               {
                   description_data.defaultChar = description_data.defaultChar || "-";
                   description_data.result.dcCreated = description_data.result.dcCreated || description_data.defaultChar;
                   description_data.result.dcContributor = description_data.result.dcContributor || description_data.defaultChar;
                   description_data.result.dcLanguage = description_data.result.dcLanguage || description_data.defaultChar;
                   description_data.result.dcViews = description_data.result.dcViews || description_data.defaultChar;
                   description_data.result.dcSeriesTitle = description_data.result.dcSeriesTitle || description_data.defaultChar;
                   description_data.result.dcDescription = description_data.result.dcDescription || description_data.defaultChar;
                   description_data.result.dcLicense = description_data.result.dcLicense || description_data.defaultChar;
               }
            processedTemplateData = template.process(description_data);
            element.html(processedTemplateData);
            return true;
        }
        else
        {
            $.log("Description Plugin: No data availablee");
            return false;
        }
    }
    
    return {
        createDescriptionFromCashe: createDescriptionFromCashe,
        addAsPlugin: addAsPlugin
    };
}());
