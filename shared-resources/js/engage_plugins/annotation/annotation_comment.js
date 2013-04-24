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
 * Global object constructors
 */

/**
 * @memberOf .
 * @description Scrubber Comment Object 
 */
function ScrubberComment(username, id, text, inpoint){
	var creator = username;
	var commentId = id;
	var commentText = text;
	var commentInpoint = inpoint;
	var self = this;
	
	this.getCreator = (function(){
		return creator;
	});
	this.getID = (function(){
		return commentId;
	});
	this.getText = (function(){
		return commentText;
	});
	this.getInpoint = (function(){
		return commentInpoint;
	});
}
/**
 * @memberOf .
 * @description Slide Comment Object 
 */
function SlideComment(username, id, text, slide, relPos){
	var creator = username;
	var commentId = id;
	var commentText = text;
	var commentOnSlide = slide;
	var slidePosition = relPos;
	var self = this;
	
	this.getCreator = (function(){
		return creator;
	});
	this.getID = (function(){
		return commentId;
	});
	this.getText = (function(){
		return commentText;
	});
	this.getSlideNr = (function(){
		return commentOnSlide;
	});
	this.getX = (function(){
		return slidePosition.x;
	});
	this.getY = (function(){
		return slidePosition.y;
	});
}
/**
 * @memberOf .
 * @description Reply Comment Object 
 */
function ReplyComment(username, id, text, replyId){
	var creator = username;
	var commentId = id;
	var commentText = text;
	var commentReplyId = replyId;
	var self = this;
	
	this.getCreator = (function(){
		return creator;
	});
	this.getID = (function(){
		return commentId;
	});
	this.getText = (function(){
		return commentText;
	});
	this.getResponseTo = (function(){
		return commentReplyId;
	});
}

function ReplyMap(){
    var reply_map = new Object();
    
    //Adds a reply to the map
    this.addReplyToComment = (function(reply){
        if(reply_map[reply.getResponseTo()] !== undefined){
            var a_end = reply_map[reply.getResponseTo()].length;
            reply_map[reply.getResponseTo()][a_end] = reply;
        }else{
            reply_map[reply.getResponseTo()] = new Array();
            reply_map[reply.getResponseTo()][0] = reply;
        }
    });
    //Returns array with comments
    this.getReplysToComment = (function(cId){
       return reply_map[cId]; 
    });
    //Removes a reply by given reply ID, returns true if found and removed
    this.removeReplyByID = (function(rId){
    	var found = false;
    	//for each comments
        for(i in reply_map){
        	if(found===true)
        		break;
        	//for each replys to this comment
        	$(reply_map[i]).each(function(j){
        		if(found===true)
        			return;
        		if(reply_map[i][j].getID() === rId){
        			//remove found item
        			reply_map[i].splice(j,1);
        			found = true;
        			return;
        		}
        	});
        }
        return found;
    });
    //Removes all replys to a given comment ID, returns true if found and removed
    this.removeReplysByCID = (function(cId){
        if(reply_map[cId] !== undefined){
        	delete reply_map[cId];
        	return true;
        }else{
        	return false;
        }
    });    
    
}

/**
 * @namespace the global Opencast namespace Annotation_Comment
 */
Opencast.Annotation_Comment = (function ()
{
    var mediaPackageId, duration, relativeSlideCommentPosition, commentAtInSeconds, cm_username, comments_cache, reply_map;
    var isOpening = false,
    isOpen = false,
    addingAcomment = false,
    clickedOnHoverBar = false,
    clickedOnComment = false,
    hoverInfoBox = false,
    ANNOTATION_COMMENT = "Annotation",
    ANNOTATION_COMMENTHIDE = "Annotation off",
    annotationType = "comment",
    oldSlideId = 0,
    infoTime = "",
    cookieName = "oc_comment_username",
    default_name = "Your Name!",
    defaul_comment_text = "Type Your Comment Here!",
    time_offset = 3,
    modus = "private";
    initialized = false;
    
    /**
     * @memberOf Opencast.Annotation_Comment
     * @description Initializes Annotation Comment
     */
    function initialize()
    {
	// no support for IE < version 8
	if(!($.browser.msie && (parseInt($.browser.version, 10) < 9)))
	{
	    initialized = true;
	}

	if(!initialized)
	{
	    return;
	}
    	
	if(modus === "public")
	{
            //Read Cookie for default Name
    	    cm_username = default_name;
    	    var nameEQ = cookieName + "=";
    	    var ca = document.cookie.split(';');
    	    for(var i = 0; i < ca.length; i++) {
    		var c = ca[i];
    		while(c.charAt(0) == ' ')
		{
    		    c = c.substring(1, c.length);
		}
    		if(c.indexOf(nameEQ) == 0)
		{
    		    cm_username = c.substring(nameEQ.length, c.length);
		}
    	    }
	}
	else if(modus === "private")
	{
	    //set username
	    loggedUser();
	    //disable username input
	}
	else
	{
	    //TODO: error deactivate plugin
	}
	//if user logged in use his username	
	$.log("Comment Plugin set username to: "+cm_username);
    	
       	// Handler keypress ALT+CTRL+a
        $(document).keyup(function (event)
        {
            if (event.altKey === true && event.ctrlKey === true)
            {
                if (event.which === 65)
                {
                    if(isOpen == true)
		    {
			$("#oc_btn-add-comment").click();            	
                    }

                }

            }
        });
        
        //// UI ////
        //add scrubber comment handler
        $("#oc_btn-add-comment").click(function()
				       {
        				   //pause player
        				   Opencast.Player.doPause();
        				   
        				   //exit shown infos
        				   $(".oc-comment-exit").click();
        				   
        				   clickedOnHoverBar = true;
        				   addingAcomment = true;
					   
    					   //hide other slide comments
        				   $('div[id^="scComment"]').hide();
        				   $('canvas[id^="slideComment"]').hide();
					   
					   //process position and set comment info box
					   var left = $("#draggable").offset().left + ($("#draggable").width() / 2) ;
					   var top = $("#data").offset().top - 136;
					   $("#comment-Info").css("left", left+"px");
					   $("#comment-Info").css("top", top+"px");
					   //show info
					   $("#comment-Info").show();
					   
					   //process current time
					   var curTime;
					   if(parseInt(Opencast.Player.getCurrentPosition()) > time_offset)
					   {
            				       curTime = $.formatSeconds((parseInt(Opencast.Player.getCurrentPosition()) - time_offset));
					   }
					   else
					   {
            				       curTime = Opencast.Player.getCurrentTime();
					   }
					   
					   //set top header info
					   $("#oc-comment-info-header-text").html("Comments at " + curTime);
					   
					   //process comment input form
					   if(modus === "private")
					   {
					       $("#oc-comment-info-value-wrapper").html(
						   '<div id="oc-comment-info-header-1" class="oc-comment-info-cm-header">'+
						       '<input id="oc-comment-add-submit" class="oc-comment-submit" type="image" src="/engage/ui/img/misc/space.png" name="Add" alt="Add" title="Add" value="Add">'+         
						       '<input id="oc-comment-add-namebox" class="oc-comment-namebox" type="text" value="'+cm_username+'" disabled="disabled">'+
						       '<div id="oc-comment-info-header-text-1" class="oc-comment-info-header-text"> at '+curTime+'</div>'+
						       '</div>'+
						       '<textarea id="oc-comment-add-textbox" class="oc-comment-textbox">Type Your Comment Here</textarea>'            
					       );
					       $("#oc-comment-add-textbox").focus();
            				       $("#oc-comment-add-textbox").select();		    
					   }else if(modus === "public"){
					       $("#oc-comment-info-value-wrapper").html(
						   '<div id="oc-comment-info-header-1" class="oc-comment-info-cm-header">'+
						       '<input id="oc-comment-add-submit" class="oc-comment-submit" type="image" src="/engage/ui/img/misc/space.png" name="Add" alt="Add" title="Add" value="Add">'+           
						       '<input id="oc-comment-add-namebox" class="oc-comment-namebox" type="text" value="'+cm_username+'">'+
						       '<div id="oc-comment-info-header-text-1" class="oc-comment-info-header-text"> at '+curTime+'</div>'+
						       '</div>'+
						       '<textarea id="oc-comment-add-textbox" class="oc-comment-textbox">Type Your Comment Here</textarea>'            
					       );
					       $("#oc-comment-add-namebox").focus();
            				       $("#oc-comment-add-namebox").select();			    
					   }
					   
					   //submit comment btn click handler
					   $("#oc-comment-add-submit").click(function(){
					       submitCommentHandler("scrubber");
					   });
					   
					   // Handler keypress CTRL+enter to submit comment
        				   $("#oc-comment-add-textbox").keyup(function (event){
					       if (event.ctrlKey === true){
						   if (event.keyCode == 13){
						       submitCommentHandler("scrubber");
						   }
					       }
        				   });
				       });
	
        //double click handler on slide comment box
        $("#oc_slide-comments").dblclick(function(event){
            
        	//exit shown infos
        	$(".oc-comment-exit").click();
        	
        	addingAcomment = true;
        	
        	//hide doubleclick info
        	$("#oc_dbclick-info").hide();
        	
        	//pause player
    		Opencast.Player.doPause();
        	
        	//hide other comments
        	$('canvas[id^="slideComment"]').hide();
        	$('div[id^="scComment"]').hide();
           
            var mPos = new Object();
            mPos.x = event.pageX - $('#oc_slide-comments').offset().left - 10;
            mPos.y = event.pageY - $('#oc_slide-comments').offset().top - 18;
            
            var relPos = new Object();
            if($('#oc_slide-comments').width() > 0){
                relPos.x = ( mPos.x / $('#oc_slide-comments').width() ) * 100;
            }else{
                relPos.x = 0;
            }
            if($('#oc_slide-comments').height() > 0){
                relPos.y = ( mPos.y / $('#oc_slide-comments').height() ) * 100;
            }else{
                relPos.y = 0;
            }
	    
            // set global variable
            relativeSlideCommentPosition = relPos;
            var ciLeft = event.pageX;
            var ciTop = event.pageY-137;

            $("#comment-Info").css("left", ciLeft+"px");
            $("#comment-Info").css("top", ciTop+"px");
            $("#comment-Info").show();       
                     
            //header info text
            var curSlide = Opencast.segments.getCurrentSlideId() + 1;
            var allSlides = Opencast.segments.getNumberOfSegments();
            var infoText = "Slide " + curSlide + " of " + allSlides;    
            $("#oc-comment-info-header-text").html(infoText);
            
            //process comment input form
	    if(modus === "private"){
		$("#oc-comment-info-value-wrapper").html(
		    '<div id="oc-comment-info-header-1" class="oc-comment-info-cm-header">'+
		        '<input id="oc-comment-add-submit" class="oc-comment-submit" type="image" src="/engage/ui/img/misc/space.png" name="Add" alt="Add" title="Add" value="Add">'+      	
		        '<input id="oc-comment-add-namebox" class="oc-comment-namebox" type="text" disabled="disabled" value="'+cm_username+'">'+
		        '<div id="oc-comment-info-header-text-1" class="oc-comment-info-header-text"> at Slide '+curSlide+'</div>'+
		        '</div>'+
	            	'<textarea id="oc-comment-add-textbox" class="oc-comment-textbox">Type Your Comment Here</textarea>'			
		);
		$("#oc-comment-add-textbox").focus();
            	$("#oc-comment-add-textbox").select();
	    }
	    else if(modus === "public")
	    {
		$("#oc-comment-info-value-wrapper").html(
		    '<div id="oc-comment-info-header-1" class="oc-comment-info-cm-header">'+
		        '<input id="oc-comment-add-submit" class="oc-comment-submit" type="image" src="/engage/ui/img/misc/space.png" name="Add" alt="Add" title="Add" value="Add">'+      	
		        '<input id="oc-comment-add-namebox" class="oc-comment-namebox" type="text" value="'+cm_username+'">'+
		        '<div id="oc-comment-info-header-text-1" class="oc-comment-info-header-text"> at Slide '+curSlide+'</div>'+
		        '</div>'+
	            	'<textarea id="oc-comment-add-textbox" class="oc-comment-textbox">Type Your Comment Here</textarea>'			
		);
		$("#oc-comment-add-namebox").focus();
            	$("#oc-comment-add-namebox").select();  				
	    }
	    
	    //submit comment btn click handler
	    $("#oc-comment-add-submit").click(function(){
		submitCommentHandler("slide");
	    });
	    
	    // Handler keypress CTRL+enter to submit comment
            $("#oc-comment-add-textbox").keyup(function (event){
		if (event.ctrlKey === true){
		    if (event.keyCode == 13){
		        submitCommentHandler("slide");
		    }
		}
            });
	});
        
        // resize handler
        $('#oc_flash-player').bind('doResize', function(e) {
	    //positioning of the slide comment box
	    var flashWidth = $('#oc_flash-player').width() / 2;
	    var flashHeight = $('#oc_flash-player').height()-10;
	    var flashTop = $('#oc_flash-player').offset().top;
	    var flashLeft = $('#oc_flash-player').offset().left;
	    
	    var scHeight = 0;
	    var scWidth = 0;
	    var scLeft = 0;
	    var scTop = 0;
	    
	    if(((flashWidth - 5) / flashHeight) < (4/3) ){
		scHeight = (flashWidth - 5) / (4/3);
		scWidth = (4/3)*scHeight;
		scLeft = flashWidth;
		scTop = flashHeight - scHeight + 4;
	    }else{
		scWidth = (4/3) * flashHeight;
		scHeight = scWidth / (4/3);
		scLeft = flashWidth;
		scTop = 5;
	    }
	    
	    $("#oc_slide-comments").css("position","absolute");
	    $("#oc_slide-comments").css("height",scHeight+"px");
	    $("#oc_slide-comments").css("width",scWidth+"px");
	    $("#oc_slide-comments").css("left",scLeft+"px");
	    $("#oc_slide-comments").css("top",scTop+"px");
	    /*
	      if(isOpen){
	      //Workaround: 500ms after resize repaint comments and marks
	      window.setTimeout(function() {
	      $.log("after resize and 500ms show annotations");  
	      Opencast.Annotation_Comment.show();
	      }, 500);            	
	      }*/
	    //Opencast.Annotation_Comment.show();
        });
        
        //listen to change video size event
        $(document).bind('changeVideoSize', function(e) {
        	$.log("CHANGE_VIDEO_SIZE_TO: "+Opencast.Player.getCurrentVideoSize());
        	if(Opencast.Player.getCurrentVideoSize() !== "videoSizeMulti"){
        		//deactivate slide comments
        		$("#oc_slide-comments").hide();
        	}else{
        		//avtivate slide comments
        		$("#oc_slide-comments").show();
        	}
        });
        
        //listen to change username event     
        $(document).bind('changeCmUsername', function(e,uname) {
        	$.log("CHANGE_CM_USERNAME_TO: "+uname);
			cm_username = uname;
        });
                
        $(".oc-comment-exit").click(function(){
            // hide info box
            $("#comment-Info").hide();
            clickedOnHoverBar = false;
            clickedOnComment = false;
            addingAcomment = false;
	    
	    //show other slide comments
	    $('canvas[id^="slideComment"]').show();
	    $('div[id^="scComment"]').show();
	    $('#oc-comment-info-header').attr(
		{
                    title: ""
		});
        });
        
        // Handler keypress Enter on textbox
        $("#oc-comment-add-textbox").keyup(function (event)
        {
            if (event.which === 13)
            {
                $("#oc-comment-add-submit").click();
            }
        });
        // Handler keypress Enter on namebox
        $("#oc-comment-add-namebox").keyup(function (event)
        {
            if (event.which === 13)
            {
                $("#oc-comment-add-submit-name").click();
            }
        });
        //show slide comment box in dependency of the video size
        if(Opencast.Player.getCurrentVideoSize() !== "videoSizeMulti"){
            //deactivate slide comments
            $("#oc_slide-comments").hide();
        }else{
            //avtivate slide comments
            $("#oc_slide-comments").show();
        }
        
        //// UI END ////
	
	// change scrubber position handler
        $('#scrubber').bind('changePosition', function(e) {
            //Check whether comments are on the current slide 
            if(Opencast.segments.getCurrentSlideId() !== oldSlideId){
                if(isOpen === true){
                    isOpen = false;
                    isOpening = false;
                    show();
                    //exit shown infos
        	    $(".oc-comment-exit").click();
                }                   
                oldSlideId = Opencast.segments.getCurrentSlideId();
            }
            //Check weather comments are on the current time
            $('div[id^="scComment"]').each(function(i){
            	if((parseInt(Opencast.Player.getCurrentPosition())+1) === parseInt($(this).attr("inpoint"))){
            	    if(clickedOnComment === false && isOpen === true){
	            	//show comment info for 3 seconds
	            	$(this).mouseover();
	            	window.setTimeout(function() {  
	    		    Opencast.Annotation_Comment.hoverOutComment();
			}, 2000); 
            	    }
            	}
            });
        });
        $('#draggable').bind('dragstop', function (event, ui){
            //Check wether comments on the current slide 
            if(Opencast.segments.getCurrentSlideId() !== oldSlideId){
                if(isOpen === true){
                    isOpen = false;
                    isOpening = false;
                    show();
                    //exit shown infos
        	    $(".oc-comment-exit").click();                    
                }                   
                oldSlideId = Opencast.segments.getCurrentSlideId();
            }                
        });
	
	$("#oc_slide-comments").mouseenter(function(){
       	    var sl_left = $('#oc_slide-comments').offset().left + $('#oc_slide-comments').width() + 2;
       	    var sl_top = $('#oc_slide-comments').offset().top + $('#oc_slide-comments').height() - 40;
       	    
            $("#oc_dbclick-info").css("left", sl_left+"px");
            $("#oc_dbclick-info").css("top", sl_top+"px");
            
            $("#oc_dbclick-info").show();     	
	});
	
	$("#oc_slide-comments").mouseleave(function(){
       	    $("#oc_dbclick-info").hide();
	});
        
        // Display the controls
        $('#oc_checkbox-annotation-comment').show();    // checkbox
        $('#oc_label-annotation-comment').show();       // 
        $('#oc_video-view').show();                     // slide comments
        //$("#oc_ui_tabs").tabs('enable', 3);             // comment tab
    }

    /**
     * @memberOf Opencast.Annotation_Comment
     * @description set username from matterhorn system
     */
     function loggedUser(){
        $.ajax(
        {
            url: "../../info/me.json",
            data: "",
            dataType: 'json',
            jsonp: 'jsonp',
            success: function (data)
            {
                if ((data !== undefined) || (data['username'] !== undefined))
                {   
                     if(data.username === "anonymous"){
                        setModus("public");
                        //set default name and read cookie
                        cm_username = default_name;
                        var nameEQ = cookieName + "=";
                        var ca = document.cookie.split(';');
                        for(var i = 0; i < ca.length; i++) {
                          var c = ca[i];
                          while(c.charAt(0) == ' ')
                          {
                            c = c.substring(1, c.length);
                          }
                          if(c.indexOf(nameEQ) == 0)
                          {
                            cm_username = c.substring(nameEQ.length, c.length);
                          }
                        }
                     }else{
                         cm_username = data.username;
                     }
                }  
            }
        });        
     }
    /**
     * @memberOf Opencast.Annotation_Comment
     * @description handler for submit btn
     * @parameters comment type, replyID(if reply this is the commentID to reply)
     */
    function submitCommentHandler(type, replyID){
        if($("#oc-comment-add-textbox").val() !== defaul_comment_text || $("#oc-comment-add-namebox").val() !== default_name){
	    // hide comment info box
	    $("#comment-Info").hide();
	    clickedOnHoverBar = false;
	    var commentValue = "";
	    if(type === "reply"){
		commentValue = $("#oc-comment-add-reply-textbox").val();
	    }
	    else if(type === "scrubber" || type === "slide"){
		commentValue = $("#oc-comment-add-textbox").val();
	    }
	    commentValue = commentValue.replace(/<>/g,"");
	    commentValue = commentValue.replace(/'/g,"`");
	    commentValue = commentValue.replace(/"/g,"`");
	    commentValue = commentValue.replace(/\n/,"");
	    var nameValue = "";
	    if(modus === "private"){
		nameValue = cm_username;
	    }else if(modus === "public"){
		nameValue = $("#oc-comment-add-namebox").val();
	    }	        
	    nameValue = nameValue.replace(/<>/g,"");       
	    nameValue = nameValue.replace(/'/g,"`"); 
	    nameValue = nameValue.replace(/"/g,"`");  
	    //show other comments
	    $('canvas[id^="slideComment"]').show();
	    $('div[id^="scComment"]').show(); 
	    if(type === "scrubber"){
		var curTime;
		if(parseInt(Opencast.Player.getCurrentPosition()) > time_offset)
		{
		    curTime = parseInt(Opencast.Player.getCurrentPosition()) - time_offset;
		}
		else
		{
		    curTime = parseInt(Opencast.Player.getCurrentPosition());
		}
		//add scrubber comment
		addComment(nameValue,curTime,commentValue,"scrubber");                
	    }else if(type === "slide"){
		//add slide comment
		addComment(nameValue,
			   parseInt(Opencast.Player.getCurrentPosition()),
			   commentValue,
			   "slide",
			   relativeSlideCommentPosition.x,
			   relativeSlideCommentPosition.y,
			   Opencast.segments.getCurrentSlideId()
		          );                              
	    }else if(type === "reply"){
		//add reply to comment
		addComment(nameValue,replyID,commentValue,"reply")
	    }
	    addingAcomment = false;
	    setUsername(nameValue);
	}
    }

    /**
     * @memberOf Opencast.Annotation_Comment
     * @description Set username
     * @param String username
     */
    function setUsername(user)
    {
    	if(modus === "public"){
	    	//Create cookie with username
	        document.cookie = cookieName+"="+user+"; path=/engage/ui/";
	    	cm_username = user;
	    }
	  	//trigger change username event
		$(document).trigger("changeCmUsername",cm_username);
    }
    
    /**
     * @memberOf Opencast.Annotation_Comment
     * @description Get username
     */
    function getUsername()
    {
    	return cm_username;
    }
    
    /**
     * @memberOf Opencast.Annotation_Comment
     * @description Add a comment
     * @param user,curPosition/replyID,value,type,xPos,yPos,segId
     */
    function addComment(user,curPosition,value,type,xPos,yPos,segId)
    {
        //var user = "Anonymous";
        //if(Opencast.Player.getUserId() !== null){
        //    user = Opencast.Player.getUserId();
        //}
        
        //Set username if public
        if(modus === "public")
	{
            setUsername(user);
	}
               
        var data = "";

       	var timePos = curPosition;
       	var replyID = curPosition;
        if(type === "reply"){
            //comment data [user]<>[text]<>[type]<>[replyID]
            if(replyID !== undefined){
        	data = user+"<>"+value+"<>"+type+"<>"+replyID;
        	timePos = 0;
            }else{
        	$.log("Opencast.Annotation_Comment: illegal add comment parameters");
        	return;
            }
        }else{
            //comment data [user]<>[text]<>[type]<>[xPos]<>[yPos]<>[segId]
	    if(xPos !== undefined && yPos !== undefined){
	        data = user+"<>"+value+"<>"+type+"<>"+xPos+"<>"+yPos+"<>"+segId;
	        //var markdiv = "<div style='height:100%; width:5px; background-color: #A72123; float: right;'> </div>";
	        //$("#segment"+segId).html(markdiv);
	    }else{
	        data = user+"<>"+value+"<>"+type;        
	    }      
        }        
        $.ajax(
            {
		type: 'PUT',
		url: "../../annotation/",
		data: "episode="+mediaPackageId+"&type="+annotationType+"&in="+timePos+"&value="+data+"&out="+curPosition,
		dataType: 'xml',
		success: function (xml)
		{
                    $.log("add comment success");
                    //erase cache
                    comments_cache = undefined;
                    //show new comments
                    isOpen = false;
                    isOpening = false;
                    show();
                    //check checkbox
                    $('#oc_checkbox-annotation-comment').attr('checked', true);
                    
                    var comment_list_show = $('#oc_btn-comments').attr("title");
                    if(comment_list_show == "Hide Comments"){
                        Opencast.Annotation_Comment_List.show();
                    }                    
		},
		error: function (jqXHR, textStatus, errorThrown)
		{
                    $.log("Add_Comment error: "+textStatus);
		}
            });
    }
    
    /**
     * @memberOf Opencast.Annotation_Comment
     * @description Show Annotation_Comment
     */
    function show()
    {
	if(!initialized)
	{
	    return;
	}

	if(!isOpen && !isOpening)
	{
            isOpening = true;

            // Request JSONP data
            $.ajax(
		{
		    url: Opencast.Watch.getAnnotationURL(),
		    data: "episode=" + mediaPackageId+"&type="+annotationType+"&limit=1000",
		    dataType: 'json',
		    jsonp: 'jsonp',
		    success: function (data)
		    {
			$.log("Annotation AJAX call: Requesting data succeeded");
			
			//demark segements              
			for(var slidesNr = Opencast.segments.getNumberOfSegments()-1 ; slidesNr >= 0 ; slidesNr--){
			    $("#segment"+slidesNr).html("");
			}
			
			if ((data === undefined) || (data['annotations'] === undefined) || (data['annotations'].annotation === undefined))
			{
			    $.log("Annotation AJAX call: Data not available");
			    //show nothing
			    $('#oc-comment-scrubber-box').html("");
			    $('#oc_slide-comments').html("");
			    isOpening = false;
			}
			else
			{
			    $.log("Annotation AJAX call: Data available");
			    data['annotations'].duration = duration; // duration is in seconds
			    data['annotations'].nrOfSegments = Opencast.segments.getNumberOfSegments();
			    
			    var scrubberData = new Object();
			    var slideData = new Object();
			    
			    var scrubberArray = new Array();
			    var slideArray = new Array();
			    var replyArray = new Array();
			    var toMarkSlidesArray = new Array();
			    
			    scrubberData.duration = duration;
			    scrubberData.type = "scrubber";
			    slideData.type = "slide";
			    
			    reply_map = new ReplyMap();
			    
			    if(data['annotations'].total > 1){
				var scCount = 0;
				var slCount = 0;
				var replyCount = 0;
				$(data['annotations'].annotation).each(function (i)
                   {
                   //split data by <> [user]<>[text]<>[type]<>[xPos]<>[yPos]<>[segId]
                   //OR split data by <> [user]<>[text]<>[type]<>[replyID]
                   var dataArray = data['annotations'].annotation[i].value.split("<>");
                   //found scrubber comment
                   if(dataArray[2] === "scrubber"){
                                       comment = new ScrubberComment(
                                       dataArray[0], //username
                                       data['annotations'].annotation[i].annotationId, //ID
                                       dataArray[1], //text
                                       data['annotations'].annotation[i].inpoint //inpoint
                                       );
                       scrubberArray[scCount] = comment;
                       scCount++;
                   //found slide comment on current slide
                   }else if(dataArray[2] === "slide" && dataArray[5] == Opencast.segments.getCurrentSlideId()){
                                       var relPos = {x:dataArray[3],y:dataArray[4]};
                                       comment = new SlideComment(
                                       dataArray[0], //username
                                       data['annotations'].annotation[i].annotationId, //ID
                                       dataArray[1], //text
                                       dataArray[5], //slide nr
                                       relPos //relative position on the slide
                                       );
                       slideArray[slCount] = comment;
                       slCount++;
                       var slideFound = false;
                       for (i in toMarkSlidesArray) {
                           if (toMarkSlidesArray[i] === dataArray[5]) {
                               slideFound = true;
                           }
                       }
                       if(slideFound === false){
                       toMarkSlidesArray[toMarkSlidesArray.length] = dataArray[5];
                       }
                       //found slide comment
                   }else if(dataArray[2] === "slide"){
                       var slideFound = false;
                       for (i in toMarkSlidesArray) {
                           if (toMarkSlidesArray[i] === dataArray[5]) {
                               slideFound = true;
                           }
                       }
                       if(slideFound === false){
                       toMarkSlidesArray[toMarkSlidesArray.length] = dataArray[5];
                       }
                   }else if(dataArray[2] === "reply"){
                                       comment = new ReplyComment(
                                       dataArray[0], //username
                                       data['annotations'].annotation[i].annotationId, //ID
                                       dataArray[1], //text
                                       dataArray[3] //Reply ID
                                       );
                                       reply_map.addReplyToComment(comment);
                   }
                   });
			    }
			    else if(data['annotations'].total != 0)
			    {
					var scCount = 0;
					var slCount = 0;
					var replyCount = 0;
					   //split data by <> [user]<>[text]<>[type]<>[xPos]<>[yPos]<>[segId]
					   //OR split data by <> [user]<>[text]<>[type]<>[replyID]
					   var dataArray = data['annotations'].annotation.value.split("<>");
					   //found scrubber comment
					   if(dataArray[2] === "scrubber"){
						               comment = new ScrubberComment(
						               dataArray[0], //username
						               data['annotations'].annotation.annotationId, //ID
						               dataArray[1], //text
						               data['annotations'].annotation.inpoint //inpoint
						               );                                                                                   
					       scrubberArray[scCount] = comment;
					       scCount++;
					       //found slide comment on current slide
					   }else if(dataArray[2] === "slide" && dataArray[5] == Opencast.segments.getCurrentSlideId()){
						               var relPos = {x:dataArray[3],y:dataArray[4]};
						               comment = new SlideComment(
						               dataArray[0], //username
						               data['annotations'].annotation.annotationId, //ID
						               dataArray[1], //text
						               dataArray[5], //slide nr
						               relPos //relative position on the slide
						               );              
					       slideArray[slCount] = comment;
					       slCount++;
					       var slideFound = false;
					       for (i in toMarkSlidesArray) {
						   if (toMarkSlidesArray[i] === dataArray[5]) {
						       slideFound = true;
						   }
					       }
					       if(slideFound === false){
					       toMarkSlidesArray[toMarkSlidesArray.length] = dataArray[5];
					       }
					       //found slide comment                               
					   }else if(dataArray[2] === "slide"){
					       var slideFound = false;
					       for (i in toMarkSlidesArray) {
						   if (toMarkSlidesArray[i] === dataArray[5]) {
						       slideFound = true;
						   }
					       }
					       if(slideFound === false){
					       toMarkSlidesArray[toMarkSlidesArray.length] = dataArray[5];
					       }                                
					   }
			    }
			    
			    scrubberData.comment = scrubberArray;
			    slideData.comment = slideArray;
			    
			    // Create Trimpath Template
			    var scrubberCommentSet = Opencast.Scrubber_CommentPlugin.addAsPlugin($('#oc-comment-scrubber-box'), scrubberData);
			    var slideCommentSet = Opencast.Slide_CommentPlugin.addAsPlugin($('#oc_slide-comments'), slideData);
			    if (!scrubberCommentSet)
			    {
				$.log("No scrubberComment template processed");
				//$("#oc-comment-scrubber-box").html("");
			    }
			    else
			    {                                                
				//$("#oc-comment-scrubber-box").show();
			    }
			    
			    if (!slideCommentSet)
			    {
				$.log("No slideComment template processed");
				$("#oc_slide-comments").html("");
			    }
			    else
			    {                        
				//$("#oc_slide-comments").show();
			    }
			    
			    //mark segments
			    if(toMarkSlidesArray.length > 0){
				$.log("Slide Comments available");
				$(toMarkSlidesArray).each(function (i){
                        	    $.log("Mark Slide: "+toMarkSlidesArray[i]);
				    var markdiv = "<div id='oc-comment-segmark_"+ toMarkSlidesArray[i] +"' style='width:6px; float: left;'> </div>";
				    $("#segment"+toMarkSlidesArray[i]).html(markdiv);
				    $("#oc-comment-segmark_"+ toMarkSlidesArray[i]).corner("cc:#000000 bevel bl 6px");
				});
			    }
			}
			$("#oc_slide-comments").show();
			$("#oc-comment-scrubber-box").show();
			isOpening = false;
			isOpen = true;
		    },
		    // If no data comes back
		    error: function (xhr, ajaxOptions, thrownError)
		    {
			$.log("Comment Ajax call: Requesting data failed "+xhr+" "+ ajaxOptions+" "+ thrownError);
			isOpening = false;
		    }
		});
	}
    }
    
    /**
     * @memberOf Opencast.annotation_comment
     * @description shows given scrubber comment on timeline
     * @param commentId, commentValue, commentTime, userId
     */
    function showScrubberComment(commentId, commentValue, commentTime, userId)
    {
    	//process position and set comment info box
        var left = $("#scComment" + commentId).offset().left + 3;
        var top = $("#data").offset().top - 136;
        $("#comment-Info").css("left", left+"px");
        $("#comment-Info").css("top", top+"px");
        //show info, hide input forms
        $("#comment-Info").show();
        //set top header info
        $("#oc-comment-info-header-text").html("Comments at "+$.formatSeconds(commentTime));
        //process html for comments
        var editCMBtn = "";
        var deleteCMBtn = "";
        if(userId === cm_username){
            editCMBtn = "<input onclick='Opencast.Annotation_Comment.editComment("+commentId+",\"slide\")' class='oc-comment-info-cm-btn oc-comment-info-cm-editbtn' type='image' src='/engage/ui/img/misc/space.png' name='Edit' alt='Edit' title='Edit' value='Edit'>";
            deleteCMBtn = "<input onclick='Opencast.Annotation_Comment.deleteComment("+commentId+",\"slide\")' class='oc-comment-info-cm-btn oc-comment-info-cm-delbtn' type='image' src='/engage/ui/img/misc/space.png' name='Delete' alt='Delete' title='Delete' value='Delete'>";
        }
        $("#oc-comment-info-value-wrapper").html(
            "<div id='oc-comment-info-comment"+commentId+"'>"+
		"<div class='oc-comment-info-cm-header'>"+
		deleteCMBtn+
		editCMBtn+
		"<input onclick='Opencast.Annotation_Comment.replyComment("+commentId+")' class='oc-comment-info-cm-btn oc-comment-info-cm-repbtn' type='image' src='/engage/ui/img/misc/space.png' name='Reply' alt='Reply' title='Reply' value='Reply'>"+
		"<input onclick='Opencast.Annotation_Comment.clickComment("+commentTime+")' class='oc-comment-info-cm-btn oc-comment-info-cm-gotobtn' type='image' src='/engage/ui/img/misc/space.png' name='Go To' alt='Go To' title='Go To' value='Go To'>"+
		"<div class='oc-comment-info-header-text'>"+userId+" at "+$.formatSeconds(commentTime)+"</div>"+
		"</div>"+
		"<p id='oc-comment-cm-textbox-"+commentId+"' class='oc-comment-cm-textbox'>"+commentValue+"</p>"
        );
        //process html for replys
        $(reply_map.getReplysToComment(commentId)).each(function(i){
        	var editReBtn = "";
        	var deleteReBtn = "";
		    if(reply_map.getReplysToComment(commentId)[i].getCreator() === cm_username){
		        editReBtn = "<input onclick='Opencast.Annotation_Comment.editComment("+reply_map.getReplysToComment(commentId)[i].getID()+",\"reply\")' class='oc-comment-info-cm-btn oc-comment-info-cm-editbtn' type='image' src='/engage/ui/img/misc/space.png' name='Edit' alt='Edit' title='Edit' value='Edit'>";
		        deleteReBtn = "<input onclick='Opencast.Annotation_Comment.deleteComment("+reply_map.getReplysToComment(commentId)[i].getID()+",\"reply\")' class='oc-comment-info-cm-btn oc-comment-info-cm-delbtn' type='image' src='/engage/ui/img/misc/space.png' name='Delete' alt='Delete' title='Delete' value='Delete'>";
		    }
	            $("#oc-comment-info-value-wrapper").append(
        	"<div id='oc-comment-info-comment"+reply_map.getReplysToComment(commentId)[i].getID()+"'>"+
	    	    "<div class='oc-comment-info-reply-header'>"+
	            deleteReBtn+
	            editReBtn+
	            "<div class='oc-comment-info-header-text'>"+reply_map.getReplysToComment(commentId)[i].getCreator()+"</div>"+		            	
		    "</div>"+
	            "<p class='oc-comment-reply-textbox'>"+reply_map.getReplysToComment(commentId)[i].getText()+"</p>"+
	            "</div>"
            );
        });
        //close first comment tag
        $("#oc-comment-info-value-wrapper").append("</div>");    	
    }    
    
    /**
     * @memberOf Opencast.annotation_comment
     * @description shows given slide comment on slide
     * @param commentId, commentValue, slideNr, userId
     */
    function showSlideComment(commentId, commentValue, slideNr, userId)
    {
    	//process position and set comment info box
	var left = $("#slideComment" + commentId).offset().left + 8;
        var top = $("#slideComment" + commentId).offset().top - 137;
        $("#comment-Info").css("left", left+"px");
        $("#comment-Info").css("top", top+"px");
        clickedOnHoverBar = true;
        $("#comment-Info").show();
        var slNr = parseInt(slideNr) + 1;
        
        //set top header info
        $("#oc-comment-info-header-text").html("Comments at slide "+slNr);
        //process html for comments
        var editCMBtn = "";
        var deleteCMBtn = "";
        if(userId === cm_username){
            editCMBtn = "<input onclick='Opencast.Annotation_Comment.editComment("+commentId+",\"slide\")' class='oc-comment-info-cm-btn oc-comment-info-cm-editbtn' type='image' src='/engage/ui/img/misc/space.png' name='Edit' alt='Edit' title='Edit' value='Edit'>";
            deleteCMBtn = "<input onclick='Opencast.Annotation_Comment.deleteComment("+commentId+",\"slide\")' class='oc-comment-info-cm-btn oc-comment-info-cm-delbtn' type='image' src='/engage/ui/img/misc/space.png' name='Delete' alt='Delete' title='Delete' value='Delete'>";
        }
        $("#oc-comment-info-value-wrapper").html(
            "<div id='oc-comment-info-comment"+commentId+"'>"+
		"<div class='oc-comment-info-cm-header'>"+
		deleteCMBtn+
		editCMBtn+
		"<input onclick='Opencast.Annotation_Comment.replyComment("+commentId+")' class='oc-comment-info-cm-btn oc-comment-info-cm-repbtn' type='image' src='/engage/ui/img/misc/space.png' name='Reply' alt='Reply' title='Reply' value='Reply'>"+
		"<div class='oc-comment-info-header-text'>"+userId+" at slide "+slNr+"</div>"+
		"</div>"+
		"<p id='oc-comment-cm-textbox-"+commentId+"' class='oc-comment-cm-textbox'>"+commentValue+"</p>"
	);
        //process html for replys
        $(reply_map.getReplysToComment(commentId)).each(function(i){
        	var editReBtn = "";
            var deleteReBtn = "";
		    if(reply_map.getReplysToComment(commentId)[i].getCreator() === cm_username){
		        editReBtn = "<input onclick='Opencast.Annotation_Comment.editComment("+reply_map.getReplysToComment(commentId)[i].getID()+",\"reply\")' class='oc-comment-info-cm-btn oc-comment-info-cm-editbtn' type='image' src='/engage/ui/img/misc/space.png' name='Edit' alt='Edit' title='Edit' value='Edit'>";
		        deleteReBtn = "<input onclick='Opencast.Annotation_Comment.deleteComment("+reply_map.getReplysToComment(commentId)[i].getID()+",\"reply\")' class='oc-comment-info-cm-btn oc-comment-info-cm-delbtn' type='image' src='/engage/ui/img/misc/space.png' name='Delete' alt='Delete' title='Delete' value='Delete'>";
		    }
            $("#oc-comment-info-value-wrapper").append(
        	"<div id='oc-comment-info-comment"+reply_map.getReplysToComment(commentId)[i].getID()+"'>"+
	    	    "<div class='oc-comment-info-reply-header'>"+
	            deleteReBtn+
	            editReBtn+
	            "<div class='oc-comment-info-header-text'>"+reply_map.getReplysToComment(commentId)[i].getCreator()+"</div>"+		            	
		    "</div>"+
	            "<p class='oc-comment-reply-textbox'>"+reply_map.getReplysToComment(commentId)[i].getText()+"</p>"+
	            "</div>"
            );
        });
        //close first comment tag
        $("#oc-comment-info-value-wrapper").append("</div>");    	
    } 
    
    /**
     * @memberOf Opencast.Annotation_Comment
     * @description changes a comment replacing the text
     */
    
    function changeComment(commentID, text)
    {  	
	    $.ajax(
    			{
    				type: 'GET',
    				url: "/annotation/"+commentID+".json",
    				dataType: 'json',
    				jsonp: 'jsonp',
    				success: function (data)
    				{
    					$.log("Annotation AJAX call: Requesting data succeeded");

    					if ((data === undefined) || (data['annotation'] === undefined))
    					{
    						$.log("Annotation AJAX call: Data not available");
    						//show nothing
    						$('#oc-comments-list').html("");
    						//displayNoAnnotationsAvailable("No data defined");
    						isOpening = false;
    					}
    					else
    					{
    						$.log("Annotation AJAX call: Data available");

    						var commentData = new Object();                  
    						var commentArray = new Array();
    						var replyArray = new Array();

    						if(data['annotation'].total != 0){
    							//split data by <> [user]<>[text]<>[type]<>[xPos]<>[yPos]<>[segId]
    							//OR split data by <> [user]<>[text]<>[type]<>[replyID]
    							var dataArray = data['annotation'].value.split("<>");
    							dataArray[1] = text;
    							var annText = dataArray.join("<>");      				
    							// ajax CHANGE Request
    							$.ajax(
    									{
    										type: 'PUT',
    										url: "/annotation/"+commentID,
    										data: "value="+annText,
    										dataType: 'xml',
    										success: function (xml)
    										{
    											$.log("change comment success");
    										},
    										error: function (jqXHR, textStatus, errorThrown)
    										{
    											$.log("Add_Comment error: "+textStatus);
    										}
    									}); 
    						}
    					}
    				}
    			});
    }    

    function editComment(commentId){
    	//pause player
    	Opencast.Player.doPause();
    	addingAcomment = true;

    	var oldEditSpace = $("#oc-comment-info-comment"+commentId).html()
    	var editValue = $("#oc-comment-cm-textbox-"+commentId).text();
    	var reply = false;
    	var editSpace = oldEditSpace;
    	if (editValue === ""){
    		editValue = $(".oc-comment-reply-textbox").text();
        	editSpace = editSpace.replace(/<p.*class=\"oc-comment-reply-textbox\">.*<\/p>/g, 
        			'<div id="oc-comment-preedit-value" style="display:none"></div>' +
        	    	'<div id="oc-comment-editbox">' +
        	    	'<textarea id="oc-reply-edit-textbox" >' + editValue + '</textarea>' +
        	    	'<input id="oc-comment-edit-cancel" class="oc-comment-cancel" type="image" src="/engage/ui/img/misc/space.png" name="Cancel" alt="cancel" title="cancel" value="cancel">' +
        	    	'<input id="oc-comment-edit-submit" class="oc-comment-change" type="image" src="/engage/ui/img/misc/space.png" name="Change" alt="Change" title="Change" value="Change">' +
        	    	'</div>');
    		reply =  true;
    	} else {
    		editSpace = oldEditSpace.replace(/<p.*class=\"oc-comment-cm-textbox\">.*<\/p>/g,
    				'<div id="oc-comment-preedit-value" style="display:none"></div>' +
        	    	'<div id="oc-comment-editbox">' +
        	    	'<textarea id="oc-comment-edit-textbox" >' + editValue + '</textarea>' +
        	    	'<input id="oc-comment-edit-cancel" class="oc-comment-cancel" type="image" src="/engage/ui/img/misc/space.png" name="Cancel" alt="cancel" title="cancel" value="cancel">' +
        	    	'<input id="oc-comment-edit-submit" class="oc-comment-change" type="image" src="/engage/ui/img/misc/space.png" name="Change" alt="Change" title="Change" value="Change">' +
        	    	'</div>');
    	}
    	$("#oc-comment-info-comment"+commentId).html(editSpace);
    	// submit comment btn click handler
		if (reply){
        	$("#oc-reply-edit-textbox").focus();
        	$("#oc-reply-edit-textbox").select();		    
		} else {
        	$("#oc-comment-edit-textbox").focus();
        	$("#oc-comment-edit-textbox").select();		    
		}
    	$("#oc-comment-edit-submit").click(function(){
    		var commentValue = "";
    		if (reply){
    			commentValue = $("#oc-reply-edit-textbox").val();
    		} else {
    			commentValue = $("#oc-comment-edit-textbox").val();
    		}
    		commentValue = commentValue.replace(/<>/g,"");
    		commentValue = commentValue.replace(/'/g,"`");
    		commentValue = commentValue.replace(/"/g,"`");
    		commentValue = commentValue.replace(/\n/,"");
    		changeComment(commentId, commentValue);
    		$("#oc-comment-info-comment"+commentId).html(oldEditSpace);
    		if (reply){
        		$(".oc-comment-reply-textbox").text(commentValue);
    		} else {
    			$("#oc-comment-cm-textbox-"+commentId).text(commentValue);
    		}
    	});

    	// cancel comment btn click handler
    	$("#oc-comment-edit-cancel").click(function(){
    		$("#oc-comment-info-comment"+commentId).html(oldEditSpace);
    	});

		// Handler keypress CTRL+enter to submit comment
    	$("#oc-comment-edit-textbox").keyup(function (event){
    		if (event.ctrlKey === true){
    			if (event.keyCode == 13){
    	    		var commentValue = "";
    	    		if (reply){
    	    			commentValue = $("#oc-reply-edit-textbox").val();
    	    		} else {
    	    			commentValue = $("#oc-comment-edit-textbox").val();
    	    		}
    	    		commentValue = commentValue.replace(/<>/g,"");
    	    		commentValue = commentValue.replace(/'/g,"`");
    	    		commentValue = commentValue.replace(/"/g,"`");
    	    		commentValue = commentValue.replace(/\n/,"");
    	    		changeComment(commentId, commentValue);
    	    		$("#oc-comment-info-comment"+commentId).html(oldEditSpace);
    	    		if (reply){
    	        		$(".oc-comment-reply-textbox").text(commentValue);
    	    		} else {
    	    			$("#oc-comment-cm-textbox-"+commentId).text(commentValue);
    	    		}
    			}
    		}
    	});    	
    	addingAcomment = false;
    }
    
    /**
     * @memberOf Opencast.annotation_comment
     * @description open reply form and give possibilty to reply to given comment id
     * @param commentId
     */
    function replyComment(commentId)
    {
    	//pause player
    	Opencast.Player.doPause();
    	addingAcomment = true;
	//process comment input form
	if(modus === "private"){
            $("#oc-comment-cm-textbox-"+commentId).after(
                '<div id="oc-comment-reply-form" style="display:none;">'+
                    '<div id="oc-comment-info-header-reply" class="oc-comment-info-reply-header">'+
                    '<input id="oc-comment-add-cancel" class="oc-comment-cancel" type="image" src="/engage/ui/img/misc/space.png" name="cancel" alt="cancel" title="cancel" value="cancel">'+
                    '<input id="oc-comment-add-submit" class="oc-comment-submit" type="image" src="/engage/ui/img/misc/space.png" name="Add" alt="Add" title="Add" value="Add">'+
                    '<input id="oc-comment-add-namebox" class="oc-comment-namebox" type="text" value="'+cm_username+'" disabled="disabled">'+
                    '</div>'+
                    '<textarea id="oc-comment-add-reply-textbox" class="oc-comment-textbox">Type Your Comment Here</textarea>'+
                    '</div>'            
            );
            $("#oc-comment-add-reply-textbox").focus();
            $("#oc-comment-add-reply-textbox").select();		    
	}else if(modus === "public"){
            $("#oc-comment-cm-textbox-"+commentId).after(
                '<div id="oc-comment-reply-form" style="display:none;">'+
                    '<div id="oc-comment-info-header-reply" class="oc-comment-info-reply-header">'+
                    '<input id="oc-comment-add-cancel" class="oc-comment-cancel" type="image" src="/engage/ui/img/misc/space.png" name="cancel" alt="cancel" title="cancel" value="cancel">'+
                    '<input id="oc-comment-add-submit" class="oc-comment-submit" type="image" src="/engage/ui/img/misc/space.png" name="Add" alt="Add" title="Add" value="Add">'+        
                    '<input id="oc-comment-add-namebox" class="oc-comment-namebox" type="text" value="'+cm_username+'">'+
                    '</div>'+
                    '<textarea id="oc-comment-add-reply-textbox" class="oc-comment-textbox">Type Your Comment Here</textarea>'+
                    '</div>'            
            );
            $("#oc-comment-add-namebox").focus();
            $("#oc-comment-add-namebox").select(); 		    
	}
	
	$("#oc-comment-reply-form").slideDown(500);
	
	//submit comment btn click handler
	$("#oc-comment-add-submit").click(function(){
	    submitCommentHandler("reply", commentId);
	});
	
	//cancel comment btn click handler
	$("#oc-comment-add-cancel").click(function(){
	    $("#oc-comment-reply-form").slideUp(300,function(){
		$("#oc-comment-reply-form").remove();
		addingAcomment = false;
	    });
	});
	
	// Handler keypress CTRL+enter to submit comment
    	$("#oc-comment-add-reply-textbox").keyup(function (event){
	    if (event.ctrlKey === true){
	        if (event.keyCode == 13){
	            submitCommentHandler("reply", commentId);
	        }
	    }
    	});    	
    }
    
    /**
     * @memberOf Opencast.Annotation_Comment
     * @description deletes comment
     */
    function deleteComment(commentID, type)
    {
    	var del_local = function(cID, t){
	    if(type === "reply"){
		//Remove from local reply map
		reply_map.removeReplyByID(commentID);
		//Hide Comment and remove it from the DOM
            	$("#oc-comment-info-comment"+commentID).slideUp(500,function(){
            	    $("#oc-comment-info-comment"+commentID).remove();
            	});
	    }else if(type === "scrubber"){
		//Remove from  local reply map
		reply_map.removeReplysByCID(commentID);
		//TODO Check weather comment is the last in this balloon
		//Remove comment info from DOM, hide Comment balloon, remove comment point from scrubber
            	$("#oc-comment-info-comment"+commentID).remove();
            	$("#scComment"+commentID).remove();
            	$(".oc-comment-exit").click();
            }else if(type === "slide"){
		//Remove from  local reply map
		reply_map.removeReplysByCID(commentID);
		//TODO Check weather comment is the last in this balloon
		//Remove comment info from DOM, hide Comment balloon, remove comment point from scrubber
            	$("#oc-comment-info-comment"+commentID).remove();
            	$("#slideComment"+commentID).remove();
            	$(".oc-comment-exit").click();
            }   		
    	}
    	
        // ajax DELETE Request
        $.ajax(
            {
		type: 'DELETE',
		url: "../../annotation/"+commentID,
		complete: function ()
		{
            	    $.log("Comment DELETE Ajax call: Request success");
		    del_local(commentID,type);
		    var comment_list_show = $('#oc_btn-comments').attr("title");
                    if(comment_list_show == "Hide Comments"){
                    	Opencast.Annotation_Comment_List.showComments();
                    }
		},
		statusCode: {
                    200: function() {
			//$.log("Comment DELETE Ajax call: Request 200 success");
			//del_local(commentID,type);
   		    }
		},
		statusCode: {
                    404: function() {
			//$.log("Comment DELETE Ajax call: Request success but Comment not found");
			//del_local(commentID,type);
                    }
		}  
            });
    }    
    
    /**
     * @memberOf Opencast.annotation_comment
     * @description click event comment
     * @param commentId id of the comment
     * @param commentValue comment value
     */
    function clickComment(commentTime)
    {
    	clickedOnComment = true;
	//show comment on timeline
	//showScrubberComment(commentId,commentValue,commentTime,userId);
        //seek player to comment
        Opencast.Watch.seekSegment(parseInt(commentTime));   
    }
    
    /**
     * @memberOf Opencast.annotation_comment
     * @description hoverComment
     * @param commentId id of the comment
     * @param commentValue comment value
     */
    function hoverComment(commentId, commentValue, commentTime, userId)
    {
    	if(addingAcomment === true){
    	    
    	}else if(clickedOnHoverBar === false & clickedOnComment === false){
            clickedOnHoverBar = true;
            clickedOnComment = true;
    	    $("#cm-info-box").hover(function(){
	    	//enter info box
	    	hoverInfoBox = true;
	    },function(){
	    	//leave info box
	    	if(addingAcomment === false){
		    hoverInfoBox = false;
		    clickedOnHoverBar = false;
	            clickedOnComment = false;
		    $("#comment-Info").hide();
	    	}
	    });
	    //show comment on timeline
	    showScrubberComment(commentId,commentValue,commentTime,userId);
	}
    }
    
    /**
     * @memberOf Opencast.annotation_comment
     * @description hoverSlideComment
     * @param commentId id of the comment
     * @param commentValue comment value
     */
    function hoverSlideComment(commentId, commentValue, userId, slideNr)
    {              
        if(addingAcomment === true){
    	    
    	}else if(clickedOnHoverBar === false & clickedOnComment === false){
            clickedOnHoverBar = true;
            clickedOnComment = true;
    	    $("#cm-info-box").hover(function(){
	    	//enter info box
	    	hoverInfoBox = true;
	    },function(){
	    	//leave info box
	    	if(addingAcomment === false){
		    hoverInfoBox = false;
		    clickedOnHoverBar = false;
	            clickedOnComment = false;
		    $("#comment-Info").hide();
	    	}
	    	
	    });
	    
	    //show comment on slide
	    showSlideComment(commentId, commentValue, slideNr, userId);
	    $("#oc_dbclick-info").hide(); //hide double click info 
	}        
    }
    
    /**
     * @memberOf Opencast.annotation_comment
     * @description hoverOutSlideComment
     * @param commentId the id of the comment
     */
    function hoverOutSlideComment()
    {
		//start show timer 1sec
	    window.setTimeout(function() {
	    	if(hoverInfoBox === false && addingAcomment === false){
	    		clickedOnComment = false;
	    		clickedOnHoverBar = false;
	    		$("#comment-Info").hide();		
	    	}
		}, 600);
    }
    
    /**
     * @memberOf Opencast.annotation_comment
     * @description hoverOutComment
     * @param commentId the id of the comment
     */
    function hoverOutComment()
    {		
		//start show timer 1sec
	    window.setTimeout(function() {
	    	if(hoverInfoBox === false && addingAcomment === false){
	    		clickedOnComment = false;
	    		clickedOnHoverBar = false;
	    		$("#comment-Info").hide();	
	    	}
		}, 600); 
    }
    
    /**
     * @memberOf Opencast.Annotation_Comment
     * @description Hide the Annotation
     */
    function hide()
    {
	if(!initialized)
	{
	    return;
	}

	if(isOpen)
	{
    	    //remove segment marks
    	    $('div[id^="oc-comment-segmark_"]').remove();
            $("#oc-comment-scrubber-box").hide();
            $('canvas[id^="slideComment"]').hide();
            isOpen = false;
	}
    }

    /**
     * @memberOf Opencast.Annotation_Comment
     * @description Toggle Analytics
     */
    function doToggle()
    {
	if(!initialized)
	{
	    return;
	}

        if (!isOpen)
        {
            show();
        }
        else
        {
            hide();
        }
        return true;
    }
    
    /**
     * @memberOf Opencast.Annotation_Comment
     * @description Set the mediaPackageId
     * @param String mediaPackageId
     */
    function setMediaPackageId(id)
    {
        mediaPackageId = id;
    }
    
    /**
     * @memberOf Opencast.Annotation_Comment
     * @description Set the duration
     * @param int duration
     */
    function setDuration(val)
    {
        duration = val;
    }
    
    /**
     * @memberOf Opencast.Annotation_Comment
     * @description Gets status of annotations are shown
     */
    function getAnnotationCommentDisplayed()
    {
        return isOpen;
    }
    
    /**
     * @memberOf Opencast.Annotation_Comment
     * @description get modus of comment feature
     */
    function getModus()
    {
        return modus;
    }
   
    /**
     * @memberOf Opencast.Annotation_Comment
     * @description set modus of comment feature
     */
    function setModus(m)
    {
        modus = m;
    }
    
    return {
        initialize: initialize,
        hide: hide,
        show: show,
        getAnnotationCommentDisplayed: getAnnotationCommentDisplayed,
        setUsername: setUsername,
        getUsername: getUsername,
        getModus: getModus,
        setModus: setModus,
        setDuration: setDuration,
        setMediaPackageId: setMediaPackageId,
        clickComment: clickComment,
        replyComment: replyComment,
        editComment: editComment,
        deleteComment: deleteComment,
        hoverComment: hoverComment,
        hoverOutComment: hoverOutComment,
        hoverSlideComment: hoverSlideComment,
        hoverOutSlideComment: hoverOutSlideComment,
        doToggle: doToggle
    };
}());
