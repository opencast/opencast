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
 * @namespace the global Opencast namespace Slide_CommentPlugin
 */
Opencast.Slide_CommentPlugin = (function ()
{
    //place to render the data in the html
    var template_slide =
                     '{for a in comment}' +
                       '<canvas id="slideComment${a.getID()}" style="z-index:10;width:18px;height:18px;top:${a.getY()}%;position:absolute;left:${a.getX()}%;" '+
                       'onmouseover="Opencast.Annotation_Comment.hoverSlideComment(\'${a.getID()}\',\'${a.getText()}\',\'${a.getCreator()}\',\'${a.getSlideNr()}\')" ' +
                       'onmouseout="Opencast.Annotation_Comment.hoverOutSlideComment()" ' +
                       '>' +
                       '</canvas>' +
                       
                     '{/for}';
        

                    
    // The Element to put the div into
    var element;
    // Data to process
    var annotation_CommentData;
    // Processed Data
    var processedTemplateData;
    
    /**
     * @memberOf Opencast.Slide_CommentPlugin
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
     * @memberOf Opencast.Slide_CommentPlugin
     * @description Resize Plug-in
     * @return true if successfully processed, false else
     */
    function resizePlugin()
    {
        return drawAnnotation_Comment();
    }
    
    /**
     * @memberOf Opencast.Slide_CommentPlugin
     * @description Add annotations into template element
     * processing the template with service data
     * @return true if successfully processed, false else
     */
    function drawAnnotation_Comment()
    {
        if ((element !== undefined) &&
            (annotation_CommentData.comment !== undefined) &&
            (annotation_CommentData.comment.length > 0) &&
            (annotation_CommentData.type === "slide"))
        {
            $.log("Slide Comment Plugin: Data available, processing template");
            processedTemplateData = template_slide.process(annotation_CommentData);
            //$.log("processedTemplateData: "+processedTemplateData);
            element.html(processedTemplateData);
            //draw balloons with html5
            $(annotation_CommentData.comment).each(function (i)
            {
                var id = annotation_CommentData.comment[i].getID();
                var c_canvas = $("#slideComment"+id)[0];
                
                drawBalloon(c_canvas);
            });
            
            return true;
        }
        else
        {
            $.log("Annotation Plugin: No data available");
            return false;
        }
    }
    
     /**
     * @memberOf Opencast.Slide_CommentPlugin
     * @description draw the comment icon with the canvas element
     * @param canvas DOM canvas element
     */   
    function drawBalloon(canvas){
        var ctx = canvas.getContext('2d');
        
        ctx.save();
        ctx.fillStyle = "rgba(167,33,35,0.9)";
        ctx.lineWidth   = 8;
        
        ctx.shadowOffsetX = 5;
        ctx.shadowOffsetY = 2;
        ctx.shadowBlur = 10;
        ctx.shadowColor = "rgba(0, 0, 0, 0.8)";
    
        ctx.beginPath();
        ctx.moveTo(70,0);
        ctx.quadraticCurveTo(10,0,10,45);
        ctx.lineTo(10,70);
        ctx.quadraticCurveTo(10,110,60,110);
        ctx.lineTo(150,110);
        ctx.lineTo(130,145);
        ctx.lineTo(200,110);
        ctx.lineTo(230,110);
        ctx.quadraticCurveTo(280,110,280,75);
        ctx.lineTo(280,40);
        ctx.quadraticCurveTo(280,0,220,0);
        ctx.lineTo(70,0);
        ctx.fill();
        ctx.stroke();
        ctx.restore();
    }
    
    return {
        addAsPlugin: addAsPlugin,
        resizePlugin: resizePlugin
    };
}());
