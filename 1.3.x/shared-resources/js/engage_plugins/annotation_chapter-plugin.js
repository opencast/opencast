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
 * @namespace the global Opencast namespace Annotation_ChapterPlugin
 */
Opencast.Annotation_ChapterPlugin = (function ()
{
    //place to render the data in the html
    var template = '<table ' +
                      'id="annotation_holder" ' +
                      'cellspacing="0" ' +
                      'cellpadding="0" ' +
                      'style="float:left;opacity: 0.65;" ' +
                      'class="segments">' +
                      '<tbody>' +
                           '<tr>' +
                               '{for a in annotation}' +
                                   '<td ' +
                                     'id="segment${a.annotationId}" ' +
                                     'onclick="Opencast.Watch.seekSegment(${parseInt(a.inpoint) / 1000})" ' +
                                     'alt="Slide ${parseInt(a.index) + 1} of ${annotation.length}" ' +
                                     'onmouseover="Opencast.segments_ui.hoverDescription(\'segment${a.annotationId}\', \'${a.value}\')" ' +
                                     'onmouseout="Opencast.segments_ui.hoverOutDescription(\'segment${a.annotationId}\', \'${a.value}\')" ' +
                                     'style="width: ${parseInt(a.length) / parseInt(duration) * 100}%;" ' +
                                     'class="segment-holder-over ui-widget ui-widget-content">' +
                                         // '${a.value}' + 
                                   '</td>' +
                                 '{/for}' +
                            '</tr>' +
                        '</tbody>' +
                    '</table>';
                    
    // The Element to put the div into
    var element;
    // Data to process
    var annotation_chapterData;
    // Processed Data
    var processedTemplateData;
    
    /**
     * @memberOf Opencast.Annotation_ChapterPlugin
     * @description Add As Plug-in
     * @param elem Element to put the Data into
     * @param data The Data to process
     * @return true if successfully processed, false else
     */
    function addAsPlugin(elem, data)
    {
        element = elem;
        annotation_chapterData = data;
        return drawAnnotation_Chapter();
    }
    
    /**
     * @memberOf Opencast.Annotation_ChapterPlugin
     * @description Resize Plug-in
     * @return true if successfully processed, false else
     */
    function resizePlugin()
    {
        return drawAnnotation_Chapter();
    }
    
    /**
     * @memberOf Opencast.Annotation_ChapterPlugin
     * @description Add annotations into template element
     * processing the template with service data
     * @return true if successfully processed, false else
     */
    function drawAnnotation_Chapter()
    {
        if ((element !== undefined) &&
            (annotation_chapterData.annotation !== undefined) &&
            (annotation_chapterData.annotation.length > 0) &&
            (annotation_chapterData.duration > 0))
        {
            $.log("Annotation Plugin: Data available, processing template");
            processedTemplateData = template.process(annotation_chapterData);
            element.html(processedTemplateData);
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
