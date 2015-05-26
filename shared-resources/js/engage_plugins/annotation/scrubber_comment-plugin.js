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
 * @namespace the global Opencast namespace Scrubber_CommentPlugin
 */
Opencast.Scrubber_CommentPlugin = (function ()
{
    //place to render the data in the html ${(parseInt(a.inpoint) / parseInt(duration)) * 100} ;float:left 
    var template_scrubber = '<div ' +
                      'id="annotation_comment_holder" ' +
                      'style="width:100%;" >' +
                      
                     '{for a in comment}' +
                       '<div id="scComment${a.getID()}" class="oc-comment-scrubber-baloon" inpoint="${a.getInpoint()}" style="left:${(parseInt(a.getInpoint()) / parseInt(duration)) * 100}%;" '+
                       'onmouseover="Opencast.Annotation_Comment.hoverComment(\'${a.getID()}\', \'${a.getText()}\',\'${a.getInpoint()}\',\'${a.getCreator()}\',\'${a.isPrivate()}\')" ' +
                       'onclick="Opencast.Annotation_Comment.clickComment(\'${a.getInpoint()}\',\'${a.getID()}\')" ' +
                       'onmouseout="Opencast.Annotation_Comment.hoverOutComment()" ' +
                       '>' +
                       '</div>' +
                       
                     '{/for}' +

                    '</div>';
        

                    
    // The Element to put the div into
    var element;
    // Data to process
    var annotation_CommentData;
    // Processed Data
    var processedTemplateData;
    
    /**
     * @memberOf Opencast.Scrubber_CommentPlugin
     * @description Add As Plug-in
     * @param elem Element to put the Data into
     * @param data The Data to process
     * @return true if successfully processed, false else
     */
    function addAsPlugin(elem, data)
    {
        element = elem;
        annotation_CommentData = data;
        return drawAnnotation_Comment();
    }
    
    /**
     * @memberOf Opencast.Scrubber_CommentPlugin
     * @description Resize Plug-in
     * @return true if successfully processed, false else
     */
    function resizePlugin()
    {
        return drawAnnotation_Comment();
    }
    
    /**
     * @memberOf Opencast.Scrubber_CommentPlugin
     * @description Add annotations into template element
     * processing the template with service data
     * @return true if successfully processed, false else
     */
    function drawAnnotation_Comment()
    {
        if ((element !== undefined) &&
            (annotation_CommentData.comment !== undefined) &&
            (annotation_CommentData.comment.length > 0) &&
            (annotation_CommentData.duration > 0) &&
            (annotation_CommentData.type === "scrubber"))
        {
            $.log("Scrubber Comment Plugin: Data available, processing template");
            processedTemplateData = template_scrubber.process(annotation_CommentData);
            $.log("processedTemplateData: "+processedTemplateData);
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
