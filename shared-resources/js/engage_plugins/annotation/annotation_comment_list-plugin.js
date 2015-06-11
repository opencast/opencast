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
Opencast.Annotation_Comment_List_Plugin = (function ()
{
    // The Template to process
    var template =  '<table cellspacing="5" onmouseout="Opencast.Annotation_Comment_List.hoverOutCommentList()" cellpadding="0" width="100%">' +					
                        '{for c in comment}' +
                                '<tr class="oc-comment-list-row" id="comment-row-${c.id}" >' +
                                    '<td class="oc-comment-list-border" style="cursor:pointer;cursor:hand;">' +
                                    	'{if c.type == "reply"}' +
                                    		'<p style="width:65px;float: left;"></p> ' +
                                    	'{else}'+ 
                                    		'<p style="width:10px;float: left;"></p> ' +
                                    	'{/if}' +                                 
	                                    '<div class="oc-comment-list-left-row" align="left" style="cursor:pointer;cursor:hand;">' +
	                                        '<div id ="oc-comment-list-user-icon-${c.id}" class="oc-comment-list-user-icon"></div>' +
	                                    '</div>' +
	                                    '<div class="oc-comment-list-middle-row" align="left" style="cursor:pointer;cursor:hand;">' +
	                                        '<div class="oc-comment-list-user-text">${c.user}</div>' +
	                                        '<div class="oc-comment-list-textspace"></div>' +                                   
	                                        '{if c.type == "scrubber"}' +
	                                            '<div class="oc-comment-list-type-text">at ${$.formatSeconds(c.inpoint)}</div>' +
	                                        '{elseif c.type == "slide"}'+
	                                            '<div class="oc-comment-list-type-text">at slide ${parseInt(c.slide) + 1}</div>' +
	                                        '{else}'+
	                                            '<div class="oc-comment-list-type-text"></div>' +
	                                        '{/if}' +
	                                        '<div class="oc-comment-list-textspace"></div>' + 
	                                        '<div style="float:left">${c.created}</div>' +

	                                        '<p class="oc-comment-list-value-text">${c.text}</p>' +
	                                    '</div>' +
	                                    '<div class="oc-comment-list-right-row">' +
	                                        '<a style="float:right; color:blue" href="javascript:Opencast.Annotation_Comment_List.deleteComment(\'${c.id}\')" >Remove</a>' +
	                                        '<div class="oc-comment-list-textspace"></div>' +
	                                        '<a style="float:right; color:blue" href="javascript:Opencast.Annotation_Comment_List.editComment(\'${c.id}\')" >Change</a>' +
	                                        '<div class="oc-comment-list-textspace"></div>' +
	                                        '{if c.type == "scrubber"}' +
	                                            '<a style="float:right; color:blue" href="javascript:Opencast.Annotation_Comment_List.clickCommentList(\'${c.id}\',\'${c.text}\',\'${c.inpoint}\',\'${c.slide}\',\'${c.user}\',\'${c.type}\')" >Jump To Comment</a>' +
	                                        '{/if}' +
	                                        '{if c.type == "slide"}' +
	                                            '<a style="float:right; color:blue" href="javascript:Opencast.Annotation_Comment_List.clickCommentList(\'${c.id}\',\'${c.text}\',\'${c.inpoint}\',\'${c.slide}\',\'${c.user}\',\'${c.type}\')" >Jump To Slide</a>' +
	                                        '{/if}' +
	                                        '<div class="oc-comment-list-textspace"></div>' +
	                                        '{if c.type != "reply" && c.isPrivate != true}' +
	                                        	'<a style="float:right; color:blue" href="javascript:Opencast.Annotation_Comment_List.replyComment(\'${c.id}\')" >Reply</a>' +
	                                        '{/if}' +	                                    
	                                    '</div>' +
                                    '</td>' +
                                '</tr>' +
                                '{for rc in replys}' +
                                	'{if rc.replyID == c.id}' +
										'<tr class="oc-comment-list-row" id="comment-row-${rc.id}" >' +
											'<td class="oc-comment-list-border" style="cursor:pointer;cursor:hand;">' +
												'<p style="width:65px;float: left;"></p> ' +
											    '<div class="oc-comment-list-left-row" align="left" style="cursor:pointer;cursor:hand;">' +
											        '<div id="oc-comment-list-user-icon-${rc.id}" class="oc-comment-list-user-icon"></div>' +
											    '</div>' +
											    '<div class="oc-comment-list-middle-row" align="left" style="cursor:pointer;cursor:hand;">' +
											        '<div class="oc-comment-list-user-text">${rc.user}</div>' +
											        '<div class="oc-comment-list-textspace"></div>' + 
											        '<div style="float:left">${rc.created}</div>' +
											        '<p class="oc-comment-list-value-text">${rc.text}</p>' +
											    '</div>' +
											    '<div class="oc-comment-list-right-row">' +
											        '<a style="float:right; color:blue" href="javascript:Opencast.Annotation_Comment_List.deleteComment(\'${rc.id}\')" >Remove</a>' +
											        '<a style="float:right; color:blue" href="javascript:Opencast.Annotation_Comment_List.editComment(\'${rc.id}\')" >Change</a>' +
											   '</div>' +
											'</td>' +
										'</tr>' +
									'{/if}' +                                
                                '{/for}' +
                        '{forelse}' +
                            'No Comments available' +
                        '{/for}' +
                        
                    '</table>';
    
    // The Element to put the div into
    var element;
    // Data to process
    var annotation_CommentData
    // Precessed Data
    var processedTemplateData = false;
    
    /**
     * @memberOf Opencast.Annotation_Comment_List_Plugin
     * @description Add As Plug-in
     * @param elem Element to fill with the Data (e.g. a div)
     * @param data Data to fill the Element with
     */
    function addAsPlugin(elem, data)
    {
        element = elem;
        annotation_CommentData = data;
        return drawAnnotation_Comment();
    }
    
     /**
     * @memberOf Opencast.Annotation_Comment_List_Plugin
     * @description draw identicon icon in given DOM element with given username
     * @param elem Element to fill
     * @param user Username
     */
    function drawIdenticon(elemID,user)
    {
        pwEncrypt = $().crypt( {method: 'md5',source: user});
        $("#"+elemID).html(pwEncrypt);
        $("#"+elemID).identicon5({rotate:true, size:50});
    }
    
    /**
     * @memberOf Opencast.Annotation_Comment_List_Plugin
     * @description Resize Plug-in
     * @return true if successfully processed, false else
     */
    function resizePlugin()
    {
        return drawAnnotation_Comment();
    }
    
    /**
     * @memberOf Opencast.Annotation_Comment_List_Plugin
     * @description Add annotations into template element
     * processing the template with service data
     * @return true if successfully processed, false else
     */
    function drawAnnotation_Comment()
    {
        if ((element !== undefined) &&
            (annotation_CommentData.comment !== undefined) &&
            (annotation_CommentData.comment.length > 0))
        {
            $.log("Annotation_Comment_List_Plugin: Data available, processing template");
            processedTemplateData = template.process(annotation_CommentData);
            //$.log("Annotation_Comment_List_Plugin: processed template: "+processedTemplateData);
            element.html(processedTemplateData);
            //draw identicon icons
            $(annotation_CommentData.comment).each(function(i){
                drawIdenticon("oc-comment-list-user-icon-"+annotation_CommentData.comment[i].id,annotation_CommentData.comment[i].user);
            });
            $(annotation_CommentData.replys).each(function(i){
                drawIdenticon("oc-comment-list-user-icon-"+annotation_CommentData.replys[i].id,annotation_CommentData.replys[i].user);
            });
          
            return true;
        }
        else
        {
            $.log("Annotation_Comment_List_Plugin: No data available");
            return false;
        }
    }
    
    return {
        addAsPlugin: addAsPlugin,
        resizePlugin: resizePlugin
    };
}());
