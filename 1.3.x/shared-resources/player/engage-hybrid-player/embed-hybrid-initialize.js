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
 * @namespace the global Opencast namespace Initialize
 */
Opencast.Initialize = (function ()
{
    var VOLUME = 'volume',
        VIDEOSIZE = 'videosize',
        divId = '',
        timeout = 200,  // http://javascript-array.com/scripts/jquery_simple_drop_down_menu/
        closetimer = 0,
        ddmenuitem = 0,
        keysSet = false,
        KEY_0,
        KEY_1,
        KEY_2,
        KEY_3,
        KEY_4,
        KEY_5,
        KEY_6,
        KEY_7,
        KEY_8,
        KEY_9,
        KEY_C,
        KEY_D,
        KEY_F,
        KEY_I,
        KEY_M,
        KEY_P,
        KEY_R,
        KEY_S,
        KEY_T,
        KEY_U,
        KEY_c,
        KEY_d,
        KEY_f,
        KEY_i,
        KEY_m,
        KEY_p,
        KEY_r,
        KEY_s,
        KEY_t,
        KEY_u,
        VIDEOSIZEONLYRIGHT = "videoSizeOnlyRight",
        VIDEOSIZEONLYLEFT = "videoSizeOnlyLeft",
        SINGLEPLAYER = "Singleplayer",
        intvalOnPlayerReady = "",
        locked = false,
        start = false,
        playerReady = false,
        formatOne = 0,
        formatTwo = 0,
        formatSingle = 0,
        mediaOneWidth = 0,
        mediaOneHeight = 0,
        mediaTwoWidth = 0,
        mediaTwoHeight = 0,
        mediaSingleWidth = 0,
        mediaSingleHeight = 0,
        iFrameHeight = 0,
        otherDivHeight = 0,
        flashHeight = 0,
        flashWidth = 0,
        embedUrl = "",
        advancedUrl = "",
        size = "",
        segmentForwardDelay = 200,
        segmentBackwardDelay = 200,
        segmentForwardClickedCounter = 0,
        segmentBackwardClickedCounter = 0,
        segmentTimeoutForward, segmentTimeoutBackward;
        
    /**
     @memberOf Opencast.Initialize
     @description set the id of the div.
     */
    function setDivId(id)
    {
        divId = id;
    }
    
    /**
     @memberOf Opencast.Initialize
     @description get the id of the div.
     */
    function getDivId()
    {
        return divId;
    }
    
    /**
     @memberOf Opencast.Player
     @description Set the playerReady.
     */
    function setPlayerReady(playerReadyBool)
    {
        playerReady = playerReadyBool;
    }
    
    /**
     @memberOf Opencast.Player
     @description Get the playerReady.
     @return Boolean playerReady
     */
    function getPlayerReady()
    {
        return playerReady;
    }
    
    /**
     @memberOf Opencast.Initialize
     @description sets the keys to its ascii characters
     */
    function setKeys()
    {
        if(!keysSet)
        {
            var asciiAlphabet = $.getAsciiAlphabet();
            KEY_0 = asciiAlphabet['0'],
            KEY_1 = asciiAlphabet['1'],
            KEY_2 = asciiAlphabet['2'],
            KEY_3 = asciiAlphabet['3'],
            KEY_4 = asciiAlphabet['4'],
            KEY_5 = asciiAlphabet['5'],
            KEY_6 = asciiAlphabet['6'],
            KEY_7 = asciiAlphabet['7'],
            KEY_8 = asciiAlphabet['8'],
            KEY_9 = asciiAlphabet['9'],
            KEY_C = asciiAlphabet['C'],
            KEY_D = asciiAlphabet['D'],
            KEY_F = asciiAlphabet['F'],
            KEY_I = asciiAlphabet['I'],
            KEY_M = asciiAlphabet['M'],
            KEY_P = asciiAlphabet['P'],
            KEY_R = asciiAlphabet['R'],
            KEY_S = asciiAlphabet['S'],
            KEY_T = asciiAlphabet['T'],
            KEY_U = asciiAlphabet['U'],
            KEY_c = asciiAlphabet['c'],
            KEY_d = asciiAlphabet['d'],
            KEY_f = asciiAlphabet['f'],
            KEY_i = asciiAlphabet['i'],
            KEY_m = asciiAlphabet['m'],
            KEY_p = asciiAlphabet['p'],
            KEY_r = asciiAlphabet['r'],
            KEY_s = asciiAlphabet['s'],
            KEY_t = asciiAlphabet['t'],
            KEY_u = asciiAlphabet['u'];
            keysSet = true;
        }
    }
    
    /**
     @memberOf Opencast.Initialize
     @description Keylistener.
     */    
    function keyboardListener()
    {
        setKeys();
        $(document).keyup(function (event)
        {
            if (event.altKey === true && event.ctrlKey === true)
            {
                if (event.which === KEY_M ||
                    event.which === KEY_m)
                {
                    Opencast.Player.doToggleMute();
                }
                if (event.which === KEY_0 ||
                    event.which === KEY_0 ||
                    event.which === KEY_2 ||
                    event.which === KEY_3 ||
                    event.which === KEY_4 ||
                    event.which === KEY_5 ||
                    event.which === KEY_6 ||
                    event.which === KEY_7 ||
                    event.which === KEY_8 ||
                    event.which === KEY_9 ||
                    event.which === KEY_C ||
                    event.which === KEY_D ||
                    event.which === KEY_F ||
                    event.which === KEY_I ||
                    event.which === KEY_P ||
                    event.which === KEY_R ||
                    event.which === KEY_S ||
                    event.which === KEY_T ||
                    event.which === KEY_U ||
                    event.which === KEY_c ||
                    event.which === KEY_d ||
                    event.which === KEY_f ||
                    event.which === KEY_i ||
                    event.which === KEY_p ||
                    event.which === KEY_r ||
                    event.which === KEY_s ||
                    event.which === KEY_t ||
                    event.which === KEY_u)
                {
                    Videodisplay.passCharCode(event.which);
                }
                event.preventDefault();
            }
        });
    }
    
    /**
     @memberOf Opencast.Initialize
     @description close the drop dowan menue.
     */
    function dropdown_close()
    {
        if (ddmenuitem)
        {
            ddmenuitem.css('visibility', 'hidden');
        }
    }
    
    /**
     @memberOf Opencast.Initialize
     @description new timer.
     */
    function dropdown_timer()
    {
        closetimer = window.setTimeout(dropdown_close, timeout);
    }
    
    /**
     @memberOf Opencast.Initialize
     @description cancel the timer.
     */
    function dropdown_canceltimer()
    {
        if (closetimer)
        {
            window.clearTimeout(closetimer);
            closetimer = null;
        }
    }
    
    /**
     @memberOf Opencast.Initialize
     @description open the drop down menue.
     */
    function dropdown_open()
    {
        if (getDivId() === VIDEOSIZE)
        {
            $('#oc_player_video-dropdown').position(
            {
                of: $( "#oc_btn-dropdown" ),
                my: "left bottom",
                at: "left top"
            });
            $('#oc_player_video-dropdown').css('visibility', 'visible');
            $('#oc_volume-menue').css('visibility', 'hidden');
            ddmenuitem = $('#oc_player_video-dropdown');
        }
        else
        {
            $('#oc_volume-menue').css('visibility', 'visible');
            $('#oc_player_video-dropdown').css('visibility', 'hidden');
            ddmenuitem = $('#oc_volume-menue');
        }
        dropdown_canceltimer();
        setDivId('');
    }
    
    /**
     @memberOf Opencast.Initialize
     @description open the drop down menue video.
     */
    function dropdownVideo_open()
    {
        setDivId(VIDEOSIZE);
        dropdown_open();
    }

    function onPlayerReadyListener()
    {
        if (intvalOnPlayerReady === "")
        {
            intvalOnPlayerReady = window.setInterval("Opencast.Initialize.onPlayerReady()", 100);
        }
    }

    function onPlayerReady()
    {
        if (getPlayerReady() === true)
        {
            Opencast.Watch.onPlayerReady();
            window.clearInterval(intvalOnPlayerReady);
            intvalOnPlayerReady = "";
            $('#oc_image').hide();
            $('#oc_video-player-controls, #oc_draggable-embed, #oc_segments-embed').show();
            start = true;
            $('#oc_controlbar-embed').hide();
        }
    }
    
    /**
     @memberOf Opencast.Initialize
     @description binds the Video Control Button
     */
    function bindVidSize()
    {
        $('#oc_video-size-controls').bind('mouseover', dropdownVideo_open);
        $('#oc_video-size-controls').bind('mouseout', dropdown_timer);
    }
    
    $(document).ready(function ()
    {
        keyboardListener();
        // set the video size list
        Opencast.Player.setVideoSizeList(SINGLEPLAYER);
        //
        $('#wysiwyg').wysiwyg(
        {
            controls: {
                strikeThrough: {
                    visible: true
                },
                underline: {
                    visible: true
                },
                separator00: {
                    visible: true
                },
                justifyLeft: {
                    visible: true
                },
                justifyCenter: {
                    visible: true
                },
                justifyRight: {
                    visible: true
                },
                justifyFull: {
                    visible: true
                },
                separator01: {
                    visible: true
                },
                indent: {
                    visible: true
                },
                outdent: {
                    visible: true
                },
                separator02: {
                    visible: true
                },
                subscript: {
                    visible: true
                },
                superscript: {
                    visible: true
                },
                separator03: {
                    visible: true
                },
                undo: {
                    visible: true
                },
                redo: {
                    visible: true
                },
                separator04: {
                    visible: true
                },
                insertOrderedList: {
                    visible: true
                },
                insertUnorderedList: {
                    visible: true
                },
                insertHorizontalRule: {
                    visible: true
                },
                separator07: {
                    visible: true
                },
                cut: {
                    visible: true
                },
                copy: {
                    visible: true
                },
                paste: {
                    visible: true
                }
            }
        });
        $('#oc_video-player-controls-embed').show();
        
        $('#oc_player_video-dropdown').bind('mouseover', dropdownVideo_open);
        $('#oc_player_video-dropdown').bind('mouseout', dropdown_timer);
        
        // Handler focus
        $('#oc_btn-dropdown').focus(function ()
        {
            setDivId(VIDEOSIZE);
            dropdown_open();
        });
         $('#oc_btn-dropdown').click(function ()
        {
            setDivId(VIDEOSIZE);
            dropdown_open();
        });
        // Handler blur
        $('#oc_btn-dropdown').blur(function ()
        {
            dropdown_timer();
        });
        $('#oc_sound').bind('mouseover', dropdown_open);
        $('#oc_sound').bind('mouseout', dropdown_timer);
        // Handler focus
        $('#oc_btn-volume').focus(function ()
        {
            setDivId(VOLUME);
            dropdown_open();
        });
        $('#slider_volume_Thumb').focus(function ()
        {
            setDivId(VOLUME);
            dropdown_open();
        });
        // Handler blur
        $('#oc_btn-volume').blur(function ()
        {
            dropdown_timer();
        });
        $('#slider_volume_Thumb').blur(function ()
        {
            dropdown_timer();
        });
        // aria roles
        $("#editorContainer").attr("className", "oc_editTime");
        $("#editField").attr("className", "oc_editTime");
        $("#oc_btn-cc").attr('role', 'button');
        $("#oc_btn-cc").attr('aria-pressed', 'false');
        $("#oc_btn-volume").attr('role', 'button');
        $("#oc_btn-volume").attr('aria-pressed', 'false');
        $("#oc_btn-play-pause").attr('role', 'button');
        $("#oc_btn-play-pause").attr('aria-pressed', 'false');
        $("#oc_btn-skip-backward").attr('role', 'button');
        $("#oc_btn-skip-backward").attr('aria-labelledby', 'Skip Backward');
        $("#oc_btn-rewind").attr('role', 'button');
        $("#oc_btn-rewind").attr('aria-labelledby', 'Rewind: Control + Alt + R');
        $("#oc_btn-fast-forward").attr('role', 'button');
        $("#oc_btn-fast-forward").attr('aria-labelledby', 'Fast Forward: Control + Alt + F');
        $("#oc_btn-skip-forward").attr('role', 'button');
        $("#oc_btn-skip-forward").attr('aria-labelledby', 'Skip Forward');
        $("#oc_current-time").attr('role', 'timer');
        $("#oc_edit-time").attr('role', 'timer');
        $("#oc_btn-slides").attr('role', 'button');
        $("#oc_btn-slides").attr('aria-pressed', 'false');
        $("#oc_myBookmarks-checkbox").attr('role', 'checkbox');
        $("#oc_myBookmarks-checkbox").attr('aria-checked', 'true');
        $("#oc_myBookmarks-checkbox").attr('aria-describedby', 'My Bookmarks');
        $("#oc_publicBookmarks-checkbox").attr('role', 'checkbox');
        $("#oc_publicBookmarks-checkbox").attr('aria-checked', 'true');
        $("#oc_publicBookmarks-checkbox").attr('aria-describedby', 'Public Bookmarks');
        // Handler for .click()
        $('#oc_btn-skip-backward').click(function ()
        {
            // Delete forward Timeout and Clicks
            if(segmentTimeoutForward !== undefined)
            {
                clearTimeout(segmentTimeoutForward);
            }
            segmentForwardClickedCounter = 0;

            // Handle backward Timeout and Clicks
            if(segmentTimeoutBackward !== undefined)
            {
                clearTimeout(segmentTimeoutBackward);
            }
            ++segmentBackwardClickedCounter;
            segmentTimeoutBackward = setTimeout(function()
                {
                    var currentSlideId = Opencast.segments.getCurrentSlideId();
                    var sec = Opencast.segments.getSegmentSeconds(currentSlideId - segmentBackwardClickedCounter);
                    if(sec < 0)
                    {
                        sec = 0;
                    }
                    segmentBackwardClickedCounter = 0;
                    Opencast.Watch.seekSegment(sec);
                }, segmentForwardDelay);
        });
        $('#oc_btn-skip-forward').click(function ()
        {
            // Delete backward Timeout and Clicks
            if(segmentTimeoutBackward !== undefined)
            {
                clearTimeout(segmentTimeoutBackward);
            }
            segmentBackwardClickedCounter = 0;

            // Handle forward Timeout and Clicks
            if(segmentTimeoutForward !== undefined)
            {
                clearTimeout(segmentTimeoutForward);
            }
            ++segmentForwardClickedCounter;
            segmentTimeoutForward = setTimeout(function()
                {
                    var currentSlideId = Opencast.segments.getCurrentSlideId();
                    var sec = Opencast.segments.getSegmentSeconds(currentSlideId + segmentForwardClickedCounter);
                    var secOfLastSeg = Opencast.segments.getSegmentSeconds(Opencast.segments.getNumberOfSegments() - 1);
                    if((sec == 0) || (sec > secOfLastSeg))
                    {
                        sec = secOfLastSeg;
                    }
                    segmentForwardClickedCounter = 0;
                    Opencast.Watch.seekSegment(sec);
                }, segmentForwardDelay);
        });
        $('#oc_btn-play-pause').click(function ()
        {
            Opencast.Player.doTogglePlayPause();
        });
        $('#oc_btn-play-pause-embed').click(function ()
        {
            $('#oc_image').hide();
            $('#oc_controlbar-embed').hide();
            $('#oc_video-player-controls').show();
            //$('#oc_flash-player').html('<script type="text/javascript" src="engage-hybrid-player/player-multi-hybrid-flash.js"></script>');
            window.location = window.location + "&play=true";
        });
        $('#oc_btn-volume').click(function ()
        {
            Opencast.Player.doToggleMute();
        });
        $('#oc_btn-cc').click(function ()
        {
            Opencast.Player.doToogleClosedCaptions();
        });
        $('#oc_current-time').click(function ()
        {
            Opencast.Player.showEditTime();
        });
        $('#oc_image').click(function ()
        {
            $('#oc_image').hide();
            $('#oc_controlbar-embed').hide();
            $('#oc_video-player-controls').show();
            //$('#oc_flash-player').html('<script type="text/javascript" src="engage-hybrid-player/player-multi-hybrid-flash.js"></script>');
            window.location = window.location + "&play=true";
        });
        $('#oc-link-advanced-player').click(function ()
        {
            Opencast.Player.doPause();
        });
        // Handler for .mouseover()
        $('#oc_btn-skip-backward').mouseover(function ()
        {
            this.className = 'oc_btn-skip-backward-over';
        });
        $('#oc_btn-rewind').mouseover(function ()
        {
            this.className = 'oc_btn-rewind-over';
        });
        $('#oc_btn-play-pause').mouseover(function ()
        {
            if(Opencast.Player.isPlaying())
            {
                $("#oc_btn-play-pause").attr("className", "oc_btn-pause-over");
            } else
            {
                $("#oc_btn-play-pause").attr("className", "oc_btn-play-over");
            }
            Opencast.Player.PlayPauseMouseOver();
        });
        $('#oc_btn-play-pause-embed').mouseover(function ()
        {
            $("#oc_btn-play-pause-embed").attr("className", "oc_btn-play-over");
        });
        $('#oc_btn-fast-forward').mouseover(function ()
        {
            this.className = 'oc_btn-fast-forward-over';
        });
        $('#oc_btn-skip-forward').mouseover(function ()
        {
            this.className = 'oc_btn-skip-forward-over';
        });
        $('#oc_btn-cc').mouseover(function ()
        {
            if (Opencast.Player.getCaptionsBool() === false)
            {
                this.className = 'oc_btn-cc-over';
            }
        });
        // Handler for .mouseout()
        $('#oc_btn-skip-backward').mouseout(function ()
        {
            this.className = 'oc_btn-skip-backward';
        });
        $('#oc_btn-rewind').mouseout(function ()
        {
            this.className = 'oc_btn-rewind';
        });
        $('#oc_btn-play-pause').mouseout(function ()
        {
            $("#oc_btn-play-pause").attr("className", "oc_btn-play");
            Opencast.Player.PlayPauseMouseOut();
        });
        $('#oc_btn-play-pause-embed').mouseout(function ()
        {
            $("#oc_btn-play-pause-embed").attr("className", "oc_btn-play");
        });
        $('#oc_btn-fast-forward').mouseout(function ()
        {
            this.className = 'oc_btn-fast-forward';
        });
        $('#oc_btn-skip-forward').mouseout(function ()
        {
            this.className = 'oc_btn-skip-forward';
        });
        $('#oc_btn-cc').mouseout(function ()
        {
            if (Opencast.Player.getCaptionsBool() === false)
            {
                this.className = 'oc_btn-cc-off';
            }
        });
        // Handler for .mousedown()
        $('#oc_btn-skip-backward').mousedown(function ()
        {
            this.className = 'oc_btn-skip-backward-clicked';
        });
        $('#oc_btn-rewind').mousedown(function ()
        {
            this.className = 'oc_btn-rewind-clicked';
            if (!locked)
            {
                locked = true;
                setTimeout(function ()
                {
                    locked = false;
                }, 400);
                Opencast.Player.doRewind();
            }
        });
        $('#oc_btn-play-pause').mousedown(function ()
        {
            Opencast.Player.PlayPauseMouseDown();
        });
        $('#oc_btn-play-pause-embed').mousedown(function ()
        {
            $("#oc_btn-play-pause-embed").attr("className", "oc_btn-play-clicked");
        });
        $('#oc_btn-fast-forward').mousedown(function ()
        {
            this.className = 'oc_btn-fast-forward-clicked';
            if (!locked)
            {
                locked = true;
                setTimeout(function ()
                {
                    locked = false;
                }, 400);
                Opencast.Player.doFastForward();
            }
        });
        $('#oc_btn-skip-forward').mousedown(function ()
        {
            this.className = 'oc_btn-skip-forward-clicked';
        });
        // Handler for .mouseup()
        $('#oc_btn-skip-backward').mouseup(function ()
        {
            this.className = 'oc_btn-skip-backward-over';
        });
        $('#oc_btn-rewind').mouseup(function ()
        {
            this.className = 'oc_btn-rewind-over';
            Opencast.Player.stopRewind();
        });
        $('#oc_btn-play-pause').mouseup(function ()
        {
            Opencast.Player.PlayPauseMouseOver();
        });
        $('#oc_btn-fast-forward').mouseup(function ()
        {
            this.className = 'oc_btn-fast-forward-over';
            Opencast.Player.stopFastForward();
        });
        $('#oc_btn-skip-forward').mouseup(function ()
        {
            this.className = 'oc_btn-skip-forward-over';
        });
        // Handler onBlur
        $('#oc_edit-time').blur(function ()
        {
            Opencast.Player.hideEditTime();
        });
        // Handler keypress
        $('#oc_current-time').keypress(function (event)
        {
            if (event.keyCode === 13)
            {
                Opencast.Player.showEditTime();
            }
        });
	$('#oc_current-time').focus(function (event)
        {
	    Opencast.Player.showEditTime();
        });
        $('#oc_edit-time').keypress(function (event)
        {
            if (event.keyCode === 13)
            {
                Opencast.Player.editTime();
            }
        });
        // Handler keydown
        $('#oc_btn-rewind').keydown(function (event)
        {
            if (event.keyCode === 13 || event.keyCode === 32)
            {
                this.className = 'oc_btn-rewind-clicked';
                Opencast.Player.doRewind();
            }
            else if (event.keyCode === 9)
            {
                this.className = 'oc_btn-rewind-over';
                Opencast.Player.stopRewind();
            }
        });
        $('#oc_btn-fast-forward').keydown(function (event)
        {
            if (event.keyCode === 13 || event.keyCode === 32)
            {
                this.className = 'oc_btn-fast-forward-clicked';
                Opencast.Player.doFastForward();
            }
            else if (event.keyCode === 9)
            {
                this.className = 'oc_btn-fast-forward-over';
                Opencast.Player.stopFastForward();
            }
        });
        $('#oc_current-time').keydown(function (event)
        {
            if (event.keyCode === 37)
            {
                Opencast.Player.doRewind();
            }
            else if (event.keyCode === 39)
            {
                Opencast.Player.doFastForward();
            }
        });
        // Handler keyup
        $('#oc_btn-rewind').keyup(function (event)
        {
            if (event.keyCode === 13 || event.keyCode === 32)
            {
                this.className = 'oc_btn-rewind-over';
                Opencast.Player.stopRewind();
            }
        });
        $('#oc_btn-fast-forward').keyup(function (event)
        {
            if (event.keyCode === 13 || event.keyCode === 32)
            {
                this.className = 'oc_btn-fast-forward-over';
                Opencast.Player.stopFastForward();
            }
        });
        $('#oc_current-time').keyup(function (event)
        {
            if (event.keyCode === 37)
            {
                Opencast.Player.stopRewind();
            }
            else if (event.keyCode === 39)
            {
                Opencast.Player.stopFastForward();
            }
        });
        //hide player controll
        $('#oc_video-player-controls, #oc_draggable-embed, #oc_segments-embed').hide();
        //initalize close button
        $('#oc_btn-leave-session-time').button(
        {
            icons: {
                primary: 'ui-icon-close'
            },
            text: false
        });
        //bind click functions
        $('#oc_time-chooser,  #oc_btn-leave-session-time').click(function ()
        {
            Opencast.Player.doToggleTimeLayer();
        });
        onPlayerReadyListener();
        // to calculate the embed flash height
        iFrameHeight = document.documentElement.clientHeight;
        otherDivHeight = 70; //needs to be changed in player-multi-hybrid-initialize.js:25 too
        flashHeight = iFrameHeight;
        flashWidth = document.documentElement.clientWidth;
        $("#oc_flash-player").css('height', flashHeight + 'px');
        // create watch.html link
        embedUrl = window.location.href;
        advancedUrl = embedUrl.replace(/embed.html/g, "watch.html");
        $("a[href='#']").attr('href', '' + advancedUrl + '');
        // set preview image size
        var previewImageHeight = 480;
        var previewImageWidth = 640;
        var previewImageFormat = previewImageWidth / previewImageHeight;
        var newPreviewImageHeight = previewImageHeight;
        var newPreviewImageWidth = previewImageWidth;
        var previewFlashWidth = flashWidth - 4;
        var previewFlashHeigth = flashHeight - 3;
        var flashFormat = previewFlashWidth / previewFlashHeigth;
        var previewImageMarginTop = 0;
        if (previewFlashHeigth < previewImageHeight || previewFlashWidth < previewImageWidth)
        {
            if (flashFormat > previewImageFormat)
            {
                newPreviewImageHeight = previewFlashHeigth;
                newPreviewImageWidth = Math.round(newPreviewImageHeight * previewImageFormat);
            }
            else if (flashFormat < previewImageFormat)
            {
                newPreviewImageWidth = previewFlashWidth;
                newPreviewImageHeight = Math.round(newPreviewImageWidth / previewImageFormat);
            }
            else if (flashFormat === previewImageFormat)
            {
                newPreviewImageWidth = previewFlashWidth;
                newPreviewImageHeight = previewFlashHeigth;
            }
        }
        // to calculate the new previewImageMarginTop
        previewImageMarginTop = Math.round((previewFlashHeigth - newPreviewImageHeight) / 2);
        // set the new css style
        $("#oc_image").css('height', newPreviewImageHeight + 'px');
        $("#oc_image").css('width', newPreviewImageWidth + 'px');
        $("#oc_image").css('margin-top', previewImageMarginTop + 'px');
        var coverUrl = $.getURLParameter('coverUrl');
        if (coverUrl === null)
        {
            var coverType;
            coverUrl = 'engage-hybrid-player/img/MatterhornEmbedLogo.png';
            $.ajax(
            {
                type: 'GET',
                contentType: 'text/xml',
                url: "../../search/episode.xml",
                data: "id=" + $.getURLParameter('id'),
                dataType: 'xml',
                success: function (xml)
                {
                    $(xml).find('attachment').each(function ()
                    {
                        coverType = $(this).attr('type');
                        if (coverType.search(/player/) !== -1)
                        {
                            coverUrl = $(this).find('url').text();
                        }
                    }); //close each(
                    $('#oc_image').attr("src", coverUrl);
                },
                error: function (a, b, c)
                {
                    Opencast.Player.addEvent(Opencast.logging.EMBED_SEARCH_AJAX_FAILED);
                    // Some error while trying to get the search result
                }
            }); //close ajax
        } // close if
        //control bar hide effect
        Opencast.embedControlHide.initialize();
        $.ajax(
        {
            type: 'GET',
            url: "../../usertracking/detailenabled",
            dataType: 'text',
            success: function (text)
            {
                if (text === 'true') {
                  Opencast.Player.detailedLogging = true;
                  //This is done here because otherwise it doesn't fire (due to async threads I'm guessing)
                  Opencast.Player.addEvent(Opencast.logging.EMBED_STARTUP);
                } else {
                  Opencast.Player.detailedLogging = false;
                }
            },
            error: function (a, b, c)
            {
                Opencast.Player.detailedLogging = false;
            }
        });
    });
    
    /**
     @memberOf Opencast.Player
     @description Set new media resuliton to the videodisplay
     @param Number format
     */
    function setNewResolution(format)
    {
        var multiMediaContainerLeft = 0;
        var flashContainerWidth = document.documentElement.clientWidth - 10;
        var newHeight = flashContainerWidth / format;
        var newWidth = newHeight * format;
        if (newHeight > $('#oc_flash-player').height() - 10)
        {
            newHeight = $('#oc_flash-player').height() - 10;
            newWidth = ($('#oc_flash-player').height() - 10) * format;
        }
        multiMediaContainerLeft = (flashContainerWidth - newWidth) / 2;
        Videodisplay.setMediaResolution(mediaOneWidth, mediaOneHeight, mediaTwoWidth, mediaTwoHeight, multiMediaContainerLeft);
    }
    
    /**
     @memberOf Opencast.Player
     @description Do resize the Embed Player
     */
    function doResize()
    {
        size = Opencast.Player.getCurrentVideoSize();
        switch (size)
        {
        case VIDEOSIZEONLYRIGHT:
            setNewResolution(formatTwo);
            break;
        case VIDEOSIZEONLYLEFT:
            setNewResolution(formatOne);
            break;
        default:
            setNewResolution(formatOne);
            break;
        }
    }
    
    /**
     @memberOf Opencast.Player
     @description Set media resuliton of the videos
     @param Number mediaResolutionOne, Number mediaResolutionTwo
     */
    function setMediaResolution(mediaResolutionOne, mediaResolutionTwo)
    {
        if (mediaResolutionOne !== '')
        {
            var mediaResolutionOneString = mediaResolutionOne;
            var mediaResolutionTwoString = mediaResolutionTwo;
            var mediaResolutionOneArray = mediaResolutionOneString.split('x');
            if (mediaResolutionTwoString !== '')
            {
                var mediaResolutionTwoArray = mediaResolutionTwoString.split('x');
                mediaOneWidth = parseInt(mediaResolutionOneArray[0], 10);
                mediaOneHeight = parseInt(mediaResolutionOneArray[1], 10);
                mediaTwoWidth = parseInt(mediaResolutionTwoArray[0], 10);
                mediaTwoHeight = parseInt(mediaResolutionTwoArray[1], 10);
                formatOne = mediaOneWidth / mediaOneHeight;
                formatTwo = mediaTwoWidth / mediaTwoHeight;
            }
            else
            {
                mediaSingleWidth = parseInt(mediaResolutionOneArray[0], 10);
                mediaSingleHeight = parseInt(mediaResolutionOneArray[1], 10);
                formatSingle = mediaSingleWidth / mediaSingleHeight;
            }
        }
        else
        {
            Videodisplay.setMediaResolution(0, 0, 0, 0, 0);
        }
    }
    
    return {
        bindVidSize: bindVidSize,
        dropdownVideo_open: dropdownVideo_open,
        dropdown_timer: dropdown_timer,
        dropdown_close: dropdown_close,
        doResize: doResize,
        setPlayerReady: setPlayerReady,
        onPlayerReady: onPlayerReady,
        setNewResolution: setNewResolution,
        setMediaResolution: setMediaResolution
    };
}());
