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
 * @namespace the global Opencast namespace Player
 */
Opencast.Player = (function ()
{
    var PLAYING = "playing",
        PAUSING = "pausing",
        PLAY = "Play: Control + Alt + P",
        PAUSE = "Pause: Control + Alt + P",
        CCON = "Closed Caption On: Control + Alt + C",
        CCOFF = "Closed Caption Off: Control + Alt + C",
        UNMUTE = "Mute: Control + Alt + M",
        MUTE = "Unmute: Control + Alt + M",
        SLIDERVOLUME = "slider_volume_Thumb",
        NOTES = "Notes",
        NOTESHIDE = "Hide Notes",
        TRANSCRIPT = "Transcript",
        TRANSCRIPTHIDE = "Hide Transcript",
        SHORTCUTS = "Shortcuts",
        SHORTCUTSHIDE = "Hide Shortcuts",
        MULTIPLAYER = "Multiplayer",
        SINGLEPLAYER = "Singleplayer",
        SINGLEPLAYERWITHSLIDES = "SingleplayerWithSlides",
        AUDIOPLAYER = "Audioplayer",
        VIDEOSIZESINGLE = "vidoSizeSingle",
        VIDEOSIZEBIGRIGHT = "videoSizeBigRight",
        VIDEOSIZEBIGLEFT = "videoSizeBigLeft",
        VIDEOSIZEMULTI = "videoSizeMulti",
        VIDEOSIZEONLYRIGHT = "videoSizeOnlyRight",
        VIDEOSIZEONLYLEFT = "videoSizeOnlyLeft",
        VIDEOSIZEAUDIO = "videoSizeAudio",
        SHOWPRESENTERVIDEO = "Show presenter video",
        SHOWPRESENTATIONONLY = "Show presentation only",
        SHOWCAPTION = "Show Captions",
        HIDECAPTION = "Hide Captions",
        currentPlayPauseState = PAUSING,
        backupPlayPauseState = '',
        currentVideoSize = '',
        currentTimeString = '00:00:00',
        showSections = true,
        mouseOverBool = false,
        captionsBool = false,
        dragging = false,
        htmlBool = true,
        duration = 0,
        browserWidth = 0,
        durationText = "",
        FLASH_PLAYERTYPE = "",
        FLASH_PLAYERSTATE = "",
        FLASH_VIEWSTATE = "",
        FLASH_MUTE = "",
        intvalRewind = "",
        intvalFastForward = "",
        displayMode = "",
        optionClassName = "",
        seekState = PAUSING,
        displVidSizeControl = false,
        shareDisplayed = false,
        timeLayerDisplayed = false,
        shortcutTabReturnId = '',
        downloadTabReturnId = '',
        embedDialogDisplayed = false,
        shareTimeDialogDisplayed = false,
        mediaPackageId, userId, sessionId, inPosition = 0,
        outPosition = 0,
        curPosition = 0,
        INTERVAL_LENGTH = 5,
        detailedLogging = false,
        lastValidTime = "Initializing";
        
        
    /*************************************************************/
	/* getter
    /*************************************************************/
    
    /**
     * @memberOf Opencast.Player
     * @description Get the showSections.
     */
    function getShowSections()
    {
        return showSections;
    }

    /**
     * @memberOf Opencast.Player
     * @description Get the duration.
     * @return the duration if initialized, -1 else
     */
    function getDuration()
    {
        return (isNaN(duration) ? -1 : duration);
    }

    /**
     * @memberOf Opencast.Player
     * @description Get the captionsBool.
     */
    function getCaptionsBool()
    {
        return captionsBool;
    }

    /**
     * @memberOf Opencast.Player
     * @description Get the current position
     */
    function getCurrentPosition()
    {
        return curPosition;
    }

    /**
     * @memberOf Opencast.Player
     * @description Get the current sessionId
     */
    function getMediaPackageId()
    {
        return mediaPackageId;
    }

    /**
     * @memberOf Opencast.Player
     * @description Get the current sessionId
     */
    function getSessionId()
    {
        return sessionId;
    }

    /**
     * @memberOf Opencast.Player
     * @description Get the currentVideoSize.
     */
    function getCurrentVideoSize()
    {
        return currentVideoSize;
    }

    /**
     * @memberOf Opencast.Player
     * @description Get the View State.
     */
    function getViewState()
    {
        FLASH_VIEWSTATE = Videodisplay.getViewState();
    }

    /**
     * @memberOf Opencast.Player
     * @description Get the htmlBool.
     */
    function getHtmlBool()
    {
        return htmlBool;
    }

    /**
     * @memberOf Opencast.Player
     * @description Get the current time of the video.
     */
    function getCurrentTime()
    {
        return currentTimeString;
    }

    /*************************************************************/
	/* setter
    /*************************************************************/

    /**
     * @memberOf Opencast.Player
     * @description Set the dragging.
     * @param Boolean bool
     */
    function setDragging(bool)
    {
        dragging = bool;
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the media URL.
     * @param String mediaURL
     */
    function setMediaURL(coverURLOne, coverURLTwo, mediaURLOne, mediaURLTwo, mimetypeOne, mimetypeTwo, playerstyle, slideLength)
    {
        if (mediaURLOne[0] === 'h' || mediaURLOne[0] === 'H' && mediaURLOne[2] === 't' || mediaURLOne[2] === 'T' || mediaURLTwo[0] === 'h' || mediaURLTwo[0] === 'H' && mediaURLTwo[2] === 't' || mediaURLTwo[2] === 'T')
        {
            $("#oc-background-progress").attr('className', 'matterhorn-progress-bar-background');
            setHtmlBool(true);
        }
        else
        {
            setHtmlBool(false);
        }
        Videodisplay.setMediaURL(coverURLOne, coverURLTwo, mediaURLOne, mediaURLTwo, mimetypeOne, mimetypeTwo, playerstyle, slideLength);
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the captions URL.
     * @param String captionsURL
     */
    function setCaptionsURL(captionsURL)
    {
        if (captionsURL !== '')
        {
            $('#oc_video-cc').css('visibility', 'visible');
        }
        Videodisplay.setCaptionsURL(captionsURL);
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the browserWidth.
     * @param Number witdh
     */
    function setBrowserWidth(witdh)
    {
        browserWidth = witdh;
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the cc icon
     */
    function setCCIconOn()
    {
        $("#oc_btn-cc").attr(
        {
            title: CCON
        });
        $('#oc_video-cc').button('option', 'label', HIDECAPTION);
        // Because the above piece of code does not work:
        $('.ui-button-text').each(function (i, val)
        {
            if ($(val).html().indexOf(SHOWCAPTION) != -1)
            {
                $(val).html(HIDECAPTION);
            }
        });
        setCaptionsBool(true);
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the cc icon
     */
    function setCCIconOff()
    {
        $("#oc_btn-cc").attr(
        {
            title: CCOFF
        });
        $("#oc_video-cc").button('option', 'label', SHOWCAPTION);
        // Because the above piece of code does not work:
        $('.ui-button-text').each(function (i, val)
        {
            if ($(val).html().indexOf(HIDECAPTION) != -1)
            {
                $(val).html(SHOWCAPTION);
            }
        });
        setCaptionsBool(false);
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the volume slider
     * @param Number newVolume
     */
    function setPlayerVolume(newPlayerVolume)
    {
        Videodisplay.setVolumePlayer(newPlayerVolume);
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the play/pause state and change the css style of the play/pause button.
     * @param String state
     */
    function setPlayPauseState(state)
    {
        if (state === PLAYING)
        {
            $("#oc_btn-play-pause").attr(
            {
                alt: PLAY,
                title: PLAY
            });
            $("#oc_btn-play-pause").attr('className', 'oc_btn-play');
            $("#oc_btn-play-pause").removeAttr('class');
            $("#oc_btn-play-pause").attr('class', 'oc_btn-play');
            $("#oc_btn-play-pause").attr('aria-pressed', 'false');
            setCurrentPlayPauseState(PAUSING);
        }
        else
        {
            $("#oc_btn-play-pause").attr(
            {
                alt: PAUSE,
                title: PAUSE
            });
            $("#oc_btn-play-pause").attr("className", "oc_btn-pause");
            $("#oc_btn-play-pause").removeAttr('class');
            $("#oc_btn-play-pause").attr('class', 'oc_btn-pause');
            $("#oc_btn-play-pause").attr('aria-pressed', 'true');
            setCurrentPlayPauseState(PLAYING);
        }
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the current time of the video.
     * @param String text
     */
    function setCurrentTime(text)
    {
        if (text.indexOf('NaN') != -1)
        {
            text = '00:00:00';
        }
        if (getDragging() === false)
        {
            $("#oc_current-time").attr("value", text);
            $("#oc_current-time").attr("title", text);
            $("#oc_edit-time").attr("value", text);
            $("#slider_seek_Rail").attr("title", "Time " + text);
            $("#scrubber").attr("aria-valuenow", text);
	    setShareTimeLink();
        }
        currentTimeString = text;
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the total time of the video.
     * @param String text
     */
    function setTotalTime(text)
    {
        // Assume that no Video/Audio lasts 0 seconds
        if ((text.indexOf('NaN') != -1) || (text == '00:00:00') || (text == ''))
        {
            text = lastValidTime;
        }
        else
        {
            lastValidTime = text;
        }
        $("#oc_duration").text(text);
        $("#scrubber").attr("aria-valuemin", "00:00:00");
        setDurationText(text);
        // If a duration has been set
        if (text != 'Initializing')
        {
            $("#scrubber").attr("aria-valuemax", text);
        }
        else
        {
            $("#scrubber").attr("aria-valuemax", "00:00:00");
        }
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the mediaPackageId
     * @param String mediaPackageId
     */
    function setMediaPackageId(id)
    {
        mediaPackageId = id;
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the userId
     * @param String userId
     */
    function setUserId(id)
    {
        userId = id;
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the mediaPackageId
     * @param String mediaPackageId
     */
    function setSessionId(id)
    {
        sessionId = id;
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the optionClassName.
     * @param Sring className
     */
    function setOptionClassName(className)
    {
        optionClassName = className;
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the slider max time and set the duration.
     * @param Number time
     */
    function setDuration(time)
    {
        duration = time;
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the new position of the seek slider.
     * @param Number newPosition
     */
    function setPlayhead(newPosition)
    {
        curPosition = newPosition;
        var fullPosition = Math.round(newPosition);
        if (inPosition <= fullPosition && fullPosition <= inPosition + INTERVAL_LENGTH)
        {
            outPosition = fullPosition;
            if (inPosition + INTERVAL_LENGTH === outPosition)
            {
                addFootprint();
                inPosition = outPosition;
            }
        }
        else
        {
            addFootprint();
            inPosition = fullPosition;
	    addEvent(Opencast.logging.SEEK);
            outPosition = fullPosition;
        }
        if (getDragging() === false)
        {
            refreshScrubberPosition();
        }
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the with of the progress bar.
     * @param Number value
     */
    function setProgress(value)
    {
        Opencast.engage.setLoadProgressPercent(value);
        $('.load-progress').css("width", (Math.min(value, 100) + "%"));
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the volume slider
     * @param Number newVolume
     */
    function setVolumeSlider(newVolume)
    {
        Opencast.ariaSpinbutton.jumpToRange(newVolume);
	addEvent(Opencast.logging.SET_VOLUME + newVolume);
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the video size list
     * @param String displayMode
     */
    function setVideoSizeList(displayMode)
    {
        var content = '';
        var displayMonitorControls = true;
        if (displayMode === MULTIPLAYER)
        {
            content = content + '<input id="oc_btn-singleDisplay" class="oc_btn-singleDisplay-presenter" type="image" src="img/space.png" name="show_presenter_video" alt="Show presenter video" title="Show presenter video" value="<![CDATA[ ]]>" onclick="Opencast.Player.videoSizeControlMultiOnlyLeftDisplay();" onfocus="Opencast.Initialize.dropdownVideo_open();" onblur="Opencast.Initialize.dropdown_timer();"></input><br/>';
            content = content + '<input id="oc_btn-bigLeftDisplay" class="oc_btn-bigLeftDisplay" type="image" src="img/space.png" name="show_large_presente_small_presentation" alt="Show large presenter / Small presentation" title="Show large presenter / Small presentation" onclick="Opencast.Player.videoSizeControlMultiBigLeftDisplay();" onfocus="Opencast.Initialize.dropdownVideo_open();" onblur="Opencast.Initialize.dropdown_timer();" /><br/>';
            content = content + '<input id="oc_btn-centerDisplay" class="oc_btn-centerDisplay" type="image" src="img/space.png" name="show_presenter_and_presentation_equal" alt="Show presenter and presentation equal" title="Show presenter and presentation equal"  onclick="Opencast.Player.videoSizeControlMultiDisplay();" onfocus="Opencast.Initialize.dropdownVideo_open();" onblur="Opencast.Initialize.dropdown_timer();" /><br/>';
            content = content + '<input id="oc_btn-bigRightDisplay" class="oc_btn-bigRightDisplay" type="image" src="img/space.png" name="show_small_presenter_large_presentation" alt="Show small presenter / Large presentation" title="Show small presenter / Large presentation" onclick="Opencast.Player.videoSizeControlMultiBigRightDisplay();" onfocus="Opencast.Initialize.dropdownVideo_open();" onblur="Opencast.Initialize.dropdown_timer();" /><br/>';
            content = content + '<input id="oc_btn-singleDisplay" class="oc_btn-singleDisplay-presentation" type="image" src="img/space.png" name="show_presentation_only " alt="Show presentation only " title="Show presentation only " onclick="Opencast.Player.videoSizeControlMultiOnlyRightDisplay();" onfocus="Opencast.Initialize.dropdownVideo_open();" onblur="Opencast.Initialize.dropdown_timer();" /><br/>';
            //content = content + '<input id="oc_btn-audioDisplay" class="oc_btn-audioDisplay" type="submit" name="Audio" alt="Audio" title="Audio" value="" onclick="Opencast.Player.videoSizeControlAudioDisplay()" onfocus="Opencast.Initialize.dropdownVideo_open();" onblur="Opencast.Initialize.dropdown_timer()"></input>';
            $('#oc_player_video-dropdown').append(content);
            $("#oc_btn-dropdown").attr("className", "oc_btn-centerDisplay");
            displVidSizeControl = true;
            setDisplayMode(displayMode);
        }
        else if (displayMode === SINGLEPLAYER)
        {
            //~ content = '<span id="oc_video-size-dropdown-div">';
            content = '<input id="oc_btn-singleDisplay" class="oc_btn-singleDisplay-presenter" type="image" src="img/space.png" name="show_presenter_video" alt="Show presenter video" title="Show presenter video" value="<![CDATA[ ]]>" onclick="Opencast.Player.videoSizeControlMultiOnlyLeftDisplay();" /><br/>';
            content = content + '<input id="oc_btn-singleDisplay" class="oc_btn-singleDisplay-presentation" type="image" src="img/space.png" name="show_presentation_only " alt="Show presentation only " title="Show presentation only " onclick="Opencast.Player.videoSizeControlMultiOnlyRightDisplay();" onfocus="Opencast.Initialize.dropdownVideo_open();" onblur="Opencast.Initialize.dropdown_timer();" /><br/>';
            //content = content + '<input id="oc_btn-singleDisplay" class="oc_btn-singleDisplay" type="submit" name="Show presenter video " alt="Show presenter video " title="Show presenter video " value="" onclick="Opencast.Player.videoSizeControlSingleDisplay()" onfocus="Opencast.Initialize.dropdownVideo_open();" onblur="Opencast.Initialize.dropdown_timer()"></input>';
            //content = content + '<input id="oc_btn-audioDisplay" class="oc_btn-audioDisplay" type="submit" name="Audio" alt="Audio" title="Audio" value="" onclick="Opencast.Player.videoSizeControlAudioDisplay()" onfocus="Opencast.Initialize.dropdownVideo_open();" onblur="Opencast.Initialize.dropdown_timer()"></input>';
            //~ content = content + '</span>';
            $('#oc_player_video-dropdown').append(content);
            $("#oc_btn-dropdown").attr("className", "oc_btn-singleDisplay");
            $("#oc_btn-dropdown").css("display", 'none');
            $('#oc_video-size-dropdown-div').css("width", '0%');
            $('#oc_video-size-dropdown-div').css("display", 'none');
            $('#oc_video-size-dropdown-div').css("margin-left", '-22px');
            setDisplayMode(displayMode);
            setCurrentVideoSize(VIDEOSIZESINGLE);
            displVidSizeControl = false;
        }
        else if (displayMode === SINGLEPLAYERWITHSLIDES)
        {
            content = '<input id="oc_btn-singleDisplay" class="oc_btn-singleDisplay-presenter" type="image" src="img/space.png" name="show_presenter_video" alt="Show presenter video" title="Show presenter video" value="<![CDATA[ ]]>" onclick="Opencast.Player.videoSizeControlMultiOnlyLeftDisplay();" /><br/>';
            content = content + '<input id="oc_btn-singleDisplay" class="oc_btn-singleDisplay-presentation" type="image" src="img/space.png" name="show_presentation_only " alt="Show presentation only " title="Show presentation only " onclick="Opencast.Player.videoSizeControlMultiOnlyRightDisplay();" onfocus="Opencast.Initialize.dropdownVideo_open();" onblur="Opencast.Initialize.dropdown_timer();" />';
            //content = content + '<input style="margin-top:5px; id="oc_btn-singleDisplay" class="oc_btn-singleDisplay" type="submit" name="Show presentation only " alt="Show presentation only" title="Show presentation only" value="" onclick="Opencast.Player.videoSizeControlMultiOnlyRightDisplay()" onfocus="Opencast.Initialize.dropdownVideo_open();" onblur="Opencast.Initialize.dropdown_timer()"></input><br/>';
            //content = content + '<input id="oc_btn-audioDisplay" class="oc_btn-audioDisplay" type="submit" name="Audio" alt="Audio" title="Audio" value="" onclick="Opencast.Player.videoSizeControlAudioDisplay()" onfocus="Opencast.Initialize.dropdownVideo_open();" onblur="Opencast.Initialize.dropdown_timer()"></input>';
            $('#oc_player_video-dropdown').html(content);
            $("#oc_btn-dropdown").attr("className", "oc_btn-singleDisplay");
            $("#oc_btn-dropdown").css("display", 'block');
            setDisplayMode(displayMode);
            displVidSizeControl = false;
        }
        else if (displayMode === AUDIOPLAYER)
        {
            $('#oc_video-size-controls').css('display', 'none');
            //$("#oc_btn-dropdown").attr("className", "oc_btn-audioDisplay");
            setDisplayMode(displayMode);
            setCurrentVideoSize(VIDEOSIZEAUDIO);
            displVidSizeControl = false;
        }
        if (!displVidSizeControl)
        {
            $("#oc_video-size-controls").unbind('mouseover mouseout mouseenter mouseleave');
        } else
        {
            $("#oc_video-size-controls").show();
            $('#oc_video-size-dropdown').show();
            $('#oc_btn-dropdown').show();
            // Show Screen Settings
            $("#oc_btn-dropdown").css("display", 'block');
            // Bind the video size controls to its functions
            Opencast.Initialize.bindVidSize();
        }
    }

    /**
     * @memberOf Opencast.Player
     * @description Sets the share time link
     */
    function setShareTimeLink()
    {
        var advancedUrl = $.getCleanURL();
	var unfTime = $('#oc_current-time').val();
	var time = $.getURLTimeFormat(unfTime);
	if(time != 0)
	{
	    advancedUrl += "&t=" + time;
	}
	$('#oc_share-time-text').html('Video at current time (' + unfTime + '):').show();
	$('#oc_share-time-textfield').html(advancedUrl).show();
	$('#oc_share-time-textfield').focus(
	    function()
	    {
		$(this).select();
	    }
	);
	if(Opencast.Player.isPlaying())
	{
	    $('#oc_share-time-tip').show();
	} else
	{
	    $('#oc_share-time-tip').hide();
	}
    }
    
    /*************************************************************/
    /* do
    /*************************************************************/
    
    /**
     * @memberOf Opencast.Player
     * @description Toggle the Notes
     */
    function doToggleNotes()
    {
        if ($("#oc_btn-notes").attr("title") === NOTES)
        {
            showNotes();
            Opencast.Description.hideDescription();
            setShowSections(true);
	    addEvent(Opencast.logging.SHOW_NOTES);
        }
        else
        {
            hideNotes();
            setShowSections(false);
        }
    }

    /**
     * @memberOf Opencast.Player
     * @description Toggle the Transcript
     */
    function doToggleTranscript()
    {
        if ($("#oc_btn-transcript").attr("title") === TRANSCRIPT)
        {
            showTranscript();
            hideSlideText();
            setShowSections(true);
	    addEvent(Opencast.logging.SHOW_TRANSCRIPT);
        }
        else
        {
            hideTranscript();
            setShowSections(false);
        }
    }

    /**
     * @memberOf Opencast.Player
     * @description Toggle the embed
     */
    function doToggleEmbed()
    {
        if (embedDialogDisplayed)
        {
            hideEmbed();
        }
        else
        {
            showEmbed();
	    addEvent(Opencast.logging.SHOW_EMBED);
        }
        // Opencast.Initialize.doResize();
    }

    /**
     * @memberOf Opencast.Player
     * @description Toggle the share time
     */
    function doToggleShareTime()
    {
        if (shareTimeDialogDisplayed)
        {
            hideShareTime();
        }
        else
        {
            showShareTime();
        }
        // Opencast.Initialize.doResize();
    }
    
    /**
     * @memberOf Opencast.Player
     * @description Toggle the Share layer
     */
    function doToggleShare(e)
    {
        if (shareDisplayed)
        {
            hideShare();
            $('#oc_share-button').focus();
        }
        else
        {
            showShare();
            $('#oc_share-layer').focus();
	    addEvent(Opencast.logging.SHOW_SHARE);
        }
        if (e !== undefined)
        {
            e.preventDefault();
        }
    }

    /**
     * @memberOf Opencast.Player
     * @description Toggle the time layer
     */
    function doToggleTimeLayer()
    {
        if (timeLayerDisplayed)
        {
            hideTimeLayer();
        }
        else
        {
            showTimeLayer();
	    addEvent(Opencast.logging.SHOW_TIME_LAYER);
        }
    }

    /**
     * @memberOf Opencast.Player
     * @description Toggle the shortcuts
     */
    function doToggleShortcuts(e, who)
    {
        if ($('#oc_shortcuts').dialog('isOpen'))
        {
            hideShortcuts();
        }
        else
        {
            showShortcuts();
	    addEvent(Opencast.logging.SHOW_SHORTCUTS);
        }
        if (who !== undefined)
        {
            //set jump back point for tab navigation
            shortcutTabReturnId = who;
        }
        if (e !== undefined)
        {
            e.preventDefault();
        }
        return false;
    }

    /**
     * @memberOf Opencast.Player
     * @description Toggle the downloads
     */
    function doToggleDownloads(e, who)
    {
        if ($('#oc_downloads').dialog('isOpen'))
        {
            hideDownloads();
        }
        else
        {
            showDownloads();
            addEvent('SHOW-DOWNLOADS');
        }
        if (who !== undefined)
        {
            //set jump back point for tab navigation
            downloadTabReturnId = who;
        }
        if (e !== undefined)
        {
            e.preventDefault();
        }
        return false;
    }

    /**
     * @memberOf Opencast.Player
     * @description Toggle between play and pause the video.
     */
    function doTogglePlayPause()
    {
        // Checking if btn_play_pause is "play"
        if (getCurrentPlayPauseState() === PAUSING)
        {
            doPlay();
        }
        else
        {
            doPause();
        }
    }

    /**
     * @memberOf Opencast.Player
     * @description Toggle between mute and unmute
     */
    function doToggleMute()
    {
        FLASH_MUTE = Videodisplay.mute();
        if (!FLASH_MUTE) {
	    addEvent(Opencast.logging.MUTE);
            addEvent('MUTE');
        } else {
	    addEvent(Opencast.logging.UNMUTE);
        }
        Opencast.ariaSpinbutton.toggleMute();
    }

    /**
     * @memberOf Opencast.Player
     * @description Do play the video.
     * @return true if playing, false else
     */
    function doPlay()
    {
        if (!isPlaying())
        {
            var playing = Videodisplay.play();
            if (playing != false)
            {
                FLASH_PLAYERSTATE = playing;
                setCurrentPlayPauseState(PLAYING);
                addEvent(Opencast.logging.PLAY);
            }
            else
            {
                return false;
            }
        }
        return isPlaying();
    }

    /**
     * @memberOf Opencast.Player
     * @description Do pause the video.
     */
    function doPause()
    {
        if (!(getCurrentPlayPauseState() == PAUSING))
        {
            setCurrentPlayPauseState(PAUSING);
            FLASH_PLAYERSTATE = Videodisplay.pause();
	    setShareTimeLink();
            addEvent(Opencast.logging.PAUSE);
        }
    }
    
    /**
     * @memberOf Opencast.Player
     * @description Do fast forward the video.
     */
    function doFastForward()
    {
        if (intvalFastForward === "")
        {
            setSeekState(getCurrentPlayPauseState());
            Videodisplay.fastForward();
            intvalFastForward = window.setInterval("Videodisplay.fastForward()", 1000);
	    addEvent(Opencast.logging.FAST_FORWARD);
        }
    }

    /**
     * @memberOf Opencast.Player
     * @description Do skip backward in the video.
     */
    function doSkipBackward()
    {
        var sec = Opencast.segments.getSecondsBeforeSlide();
        Opencast.Watch.seekSegment(sec);
	addEvent(Opencast.logging.SKIP_BACKWARD);
    }

    /**
     * @memberOf Opencast.Player
     * @description Do rewind in the video.
     */
    function doRewind()
    {
        if (intvalRewind === "")
        {
            setSeekState(getCurrentPlayPauseState());
            Videodisplay.rewind();
            intvalRewind = window.setInterval("Videodisplay.rewind()", 1000);
	    addEvent(Opencast.logging.REWIND);
        }
    }

    /**
     * @memberOf Opencast.Player
     * @description Do skip forward in the vido.
     */
    function doSkipForward()
    {
        var sec = Opencast.segments.getSecondsNextSlide();
        Opencast.Watch.seekSegment(sec);
	addEvent(Opencast.logging.SKIP_FORWARD);
    }

    /**
     * @memberOf Opencast.Player
     * @description seeks
     * @param ms milliseconds
     */
    function doSeek(ms)
    {
        Videodisplay.seek(ms);
    }

    /**
     * @memberOf Opencast.Player
     * @description Toggle between closed captions on or off.
     * @param Boolean cc
     */
    function doToogleClosedCaptions()
    {
        Videodisplay.closedCaptions();
	addEvent(Opencast.logging.CLOSED_CAPTIONS);
    }

    /*************************************************************/
	/* show
    /*************************************************************/

    /**
     * @memberOf Opencast.Player
     * @description Shows the share overlay
     */
    function showShare()
    {
        // Currently a little 'hack' for webkit-browsers
        $("#oc_share-layer").position(
        {
            of: $("#oc_share-button"),
            my: "center top",
            at: "center bottom"
        });
        // first show, then position!
        $('#oc_share-layer').show();
        $('#oc_share-layer').attr(
        {
            'aria-hidden': 'false',
            'tabindex': '0'
        });
        $("#oc_share-layer").position(
        {
            of: $("#oc_share-button"),
            my: "center top",
            at: "center bottom"
        });
        shareDisplayed = true;
    }

    /**
     * @memberOf Opencast.Player
     * @description Show the edit time input and hide the current time span.
     */
    function showEditTime()
    {
        $("#oc_current-time").addClass("oc_current-time-hide").removeClass('oc_current-time');
        $("#oc_edit-time").removeClass("oc_edit-time-hide").addClass("oc_edit-time");
        backupPlayPauseState = getCurrentPlayPauseState();
        if (backupPlayPauseState === PLAYING)
        {
            Opencast.Player.doPause();
        }
        $("#oc_edit-time").focus();
    }

    /**
     * @memberOf Opencast.Player
     * @description Show the embed
     */
    function showEmbed()
    {
        hideShare();
        $('#oc_embed').dialog('open');
        $('#oc_embed').bind('dialogclose', function ()
        {
            Opencast.Player.hideEmbed();
        });
        embedDialogDisplayed = true;
    }
    
    /**
     * @memberOf Opencast.Player
     * @description Show the shared time dialog
     */
    function showShareTime()
    {
        hideShareTime();
	setShareTimeLink();
        $('#oc_share-time').dialog('open');
        $('#oc_share-time').bind('dialogclose', function ()
        {
            Opencast.Player.hideShareTime();
        });
        shareTimeDialogDisplayed = true;
    }

    /*************************************************************/
	/* hide
    /*************************************************************/

    /**
     * @memberOf Opencast.Player
     * @description Hides the shortcuts
     */
    function hideShortcuts()
    {
        if (shortcutTabReturnId !== undefined)
        {
            $('#' + shortcutTabReturnId).focus();
        }
        $('#oc_shortcuts').dialog('close');
    }

    /**
     * @memberOf Opencast.Player
     * @description Hides the downloads
     */
    function hideDownloads()
    {
        if (downloadTabReturnId !== undefined)
        {
            $('#' + downloadTabReturnId).focus();
        }
        $('#oc_downloads').dialog('close');
    }

    /**
     * @memberOf Opencast.Player
     * @description Hides the share overlay
     */
    function hideShare(e)
    {
        $('#oc_share-layer').hide();
        $('#oc_share-layer').attr(
        {
            'aria-hidden': 'true',
            'tabindex': '-1'
        });
        shareDisplayed = false;
    }

    /**
     * @memberOf Opencast.Player
     * @description Hide the embed
     */
    function hideEmbed()
    {
        $('#oc_share-button').focus();
        embedDialogDisplayed = false;
    }

    /**
     * @memberOf Opencast.Player
     * @description Hide the share time dialog
     */
    function hideShareTime()
    {
        $('#oc_share-button').focus();
        shareTimeDialogDisplayed = false;
    }

    /*************************************************************/
	/* video size control
    /*************************************************************/

    /**
     * @memberOf Opencast.Player
     * @description Show the single video display
     */
    function videoSizeControlSingleDisplay()
    {
        $("#oc_btn-dropdown").attr("className", "oc_btn-singleDisplay");
        setCurrentVideoSize(VIDEOSIZESINGLE);
	addEvent(Opencast.logging.VIDEOSIZE_SINGLE);
    }

    /**
     * @memberOf Opencast.Player
     * @description Show the audio display
     */
    function videoSizeControlAudioDisplay()
    {
        if (getDisplayMode() === MULTIPLAYER)
        {
            Videodisplay.videoSizeControl(100, 0);
        }
        $("#oc_btn-dropdown").attr("className", "oc_btn-audioDisplay");
        setCurrentVideoSize(VIDEOSIZEAUDIO);
        Opencast.Initialize.doResize();
	addEvent(Opencast.logging.VIDEOSIZE_AUDIO);
    }

    /**
     * @memberOf Opencast.Player
     * @description Show only the presenter video display
     */
    function videoSizeControlMultiOnlyLeftDisplay()
    {
        Videodisplay.videoSizeControl(100, 0);
        $("#oc_btn-dropdown").attr("className", "oc_btn-singleDisplay-presenter");
        setCurrentVideoSize(VIDEOSIZEONLYLEFT);
        Opencast.Initialize.doResize();
	addEvent(Opencast.logging.VIDEOSIZE_LEFT_ONLY);
    }

    /**
     * @memberOf Opencast.Player
     * @description Show only the presentation video display
     */
    function videoSizeControlMultiOnlyRightDisplay()
    {
        Videodisplay.videoSizeControl(0, 100);
        $("#oc_btn-dropdown").attr("className", "oc_btn-singleDisplay-presentation");
        setCurrentVideoSize(VIDEOSIZEONLYRIGHT);
        Opencast.Initialize.doResize();
	addEvent(Opencast.logging.VIDEOSIZE_RIGHT_ONLY);
    }

    /**
     * @memberOf Opencast.Player
     * @description Resize the presentation video
     */
    function videoSizeControlMultiBigRightDisplay()
    {
        Videodisplay.videoSizeControl(50, 100);
        $("#oc_btn-dropdown").attr("className", "oc_btn-bigRightDisplay");
        setCurrentVideoSize(VIDEOSIZEBIGRIGHT);
        Opencast.Initialize.doResize();
	addEvent(Opencast.logging.VIDEOSIZE_BIG_RIGHT);
    }

    /**
     * @memberOf Opencast.Player
     * @description Resize the presenter video
     */
    function videoSizeControlMultiBigLeftDisplay()
    {
        Videodisplay.videoSizeControl(100, 50);
        $("#oc_btn-dropdown").attr("className", "oc_btn-bigLeftDisplay");
        setCurrentVideoSize(VIDEOSIZEBIGLEFT);
        Opencast.Initialize.doResize();
	addEvent(Opencast.logging.VIDEOSIZE_BIG_LEFT);
    }

    /**
     * @memberOf Opencast.Player
     * @description Show presenter and presentation video
     */
    function videoSizeControlMultiDisplay()
    {
        Videodisplay.videoSizeControl(100, 100);
        $("#oc_btn-dropdown").attr("className", "oc_btn-centerDisplay");
        setCurrentVideoSize(VIDEOSIZEMULTI);
        Opencast.Initialize.doResize();
	addEvent(Opencast.logging.VIDEOSIZE_MULTI);
    }

    /**
     * @memberOf Opencast.Player
     * @description Show only the presenter video display
     */
    function videoSizeControlSinglePlayerWithSlides()
    {
        if ($(".oc_btn-singleDisplay").attr("title") === SHOWPRESENTERVIDEO)
        {
            $(".oc_btn-singleDisplay").attr(
            {
                alt: SHOWPRESENTATIONONLY,
                title: SHOWPRESENTATIONONLY,
                name: SHOWPRESENTATIONONLY
            });
            Videodisplay.videoSizeControl(100, 0);
            setCurrentVideoSize(VIDEOSIZEONLYLEFT);
        }
        else
        {
            $(".oc_btn-singleDisplay").attr(
            {
                alt: SHOWPRESENTERVIDEO,
                title: SHOWPRESENTERVIDEO,
                name: SHOWPRESENTERVIDEO
            });
            Videodisplay.videoSizeControl(0, 100);
            setCurrentVideoSize(VIDEOSIZEONLYRIGHT);
        }
        Opencast.Initialize.doResize();
    }

    /*************************************************************/
	/* alert
    /*************************************************************/

    /**
     * @memberOf Opencast.Player
     * @description Remove the old alert div and create an new div with the aria role alert.
     * @param String alertMessage
     */
    function addAlert(alertMessage)
    {
        removeOldAlert();
        var newAlert = document.createElement("div");
        newAlert.setAttribute("role", "alert");
        newAlert.setAttribute("id", "alert");
        newAlert.setAttribute("class", "oc_offScreen-hidden");
        var msg = document.createTextNode(alertMessage);
        newAlert.appendChild(msg);
        document.body.appendChild(newAlert);
    }

    /**
     * @memberOf Opencast.Player
     * @description Remove the alert div.
     */
    function removeOldAlert()
    {
        var oldAlert = $("#alert").get(0);
        if (oldAlert)
        {
            document.body.removeChild(oldAlert);
        }
    }

    /*************************************************************/
	/* sound
    /*************************************************************/

    /**
     * @memberOf Opencast.Player
     * @description Set the volume icon
     */
    function lowSound()
    {
        $("#oc_btn-volume").attr('className', 'oc_btn-volume-low');
        $("#oc_btn-volume").attr(
        {
            alt: UNMUTE,
            title: UNMUTE
        });
        $("#oc_btn-volume").attr('aria-pressed', 'false');
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the volume icon
     */
    function noneSound()
    {
        $("#oc_btn-volume").attr('className', 'oc_btn-volume-none');
        $("#oc_btn-volume").attr(
        {
            alt: UNMUTE,
            title: UNMUTE
        });
        $("#oc_btn-volume").attr('aria-pressed', 'false');
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the volume icon
     */
    function highSound()
    {
        $("#oc_btn-volume").attr('className', 'oc_btn-volume-high');
        $("#oc_btn-volume").attr(
        {
            alt: UNMUTE,
            title: UNMUTE
        });
        $("#oc_btn-volume").attr('aria-pressed', 'false');
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the volume icon
     */
    function muteSound()
    {
        $("#oc_btn-volume").attr('className', 'oc_btn-volume-mute');
        $("#oc_btn-volume").attr(
        {
            alt: MUTE,
            title: MUTE
        });
        $("#oc_btn-volume").attr('aria-pressed', 'true');
    }

    /*************************************************************/
	/* play/pause
    /*************************************************************/

    /**
     * @memberOf Opencast.Player
     * @description Mouse over effect, change the css style.
     */
    function PlayPauseMouseOver()
    {
        if (getCurrentPlayPauseState() === PLAYING)
        {
            setMouseOverBool(true);
        }
        else if (getCurrentPlayPauseState() === PAUSING)
        {
            setMouseOverBool(true);
        }
    }

    /**
     * @memberOf Opencast.Player
     * @description Mouse out effect, change the css style.
     */
    function PlayPauseMouseOut()
    {
        if (getCurrentPlayPauseState() === PLAYING)
        {
            $("#oc_btn-play-pause").attr("className", "oc_btn-pause");
            setMouseOverBool(false);
        }
        else if (getCurrentPlayPauseState() === PAUSING)
        {
            $("#oc_btn-play-pause").attr("className", "oc_btn-play");
            setMouseOverBool(false);
        }
    }

    /**
     * @memberOf Opencast.Player
     * @description Mouse down effect, change the css style.
     */
    function PlayPauseMouseDown()
    {
        return true;
    }

    /*************************************************************/
	/* misc
    /*************************************************************/

    /**
     * @memberOf Opencast.Player
     * @description Returns if playing
     * @return true if playing, false else
     */
    function isPlaying()
    {
        return getCurrentPlayPauseState() === PLAYING;
    }

    /**
     * @memberOf Opencast.Player
     * @description Returns true if Share Overlay is display, false else
     * @return true if Share Overlay is display, false else
     */
    function shareOverlayDisplayed()
    {
        return shareDisplayed;
    }

    /**
     * @memberOf Opencast.Player
     * @description Returns true if Share Overlay is display, false else
     * @return true if Share Overlay is display, false else
     */
    function shortcutOverlayDisplayed()
    {
        return shortcutDialogDisplayed;
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the scrubber postion
     */
    function refreshScrubberPosition()
    {
        var newPos = Math.round((curPosition / getDuration()) * $("#scubber-channel").width());
        if (!isFinite(newPos))
        {
            newPos = 0;
        }
        if (newPos <= $("#scubber-channel").width())
        {
            $("#draggable").css("left", newPos);
            $("#scrubber").css("left", newPos);
            $("#play-progress").css("width", newPos);
        }
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the embed Player.
     * @param String width, String height
     */
    function embedIFrame(width, height, dontLog)
    {
        var iFrameText = '';
        var advancedUrl = window.location.href;
        var embedUrl = advancedUrl.replace(/watch.html/g, "embed.html");
	if($('#oc_embed-costum-hide-controls-checkbox').is(':checked'))
	{
	    embedUrl += "&hideControls=false";
	}
        $('#oc_embed-costum-width-textinput').val(width);
        $('#oc_embed-costum-height-textinput').val(height);
        iFrameText = '<iframe src="' + embedUrl + '" style="border:0px #FFFFFF none;" name="Opencast Matterhorn - Media Player" scrolling="no" frameborder="0" marginheight="0px" marginwidth="0px" width="' + width + '" height="' + height + '"></iframe>';
        $('#oc_embed-textarea').val(iFrameText);
	if(!dontLog)
	{
	    addEvent(Opencast.logging.GENERATE_EMBED + width + 'x' + height);
	}
	var w = width;
	var h = height;
	$('#oc_embed-costum-hide-controls-checkbox').unbind('click');
	$('#oc_embed-costum-hide-controls-checkbox').bind('click', function() {
	    Opencast.Player.embedIFrame(w, h, true);
	});
    }

    /**
     * @memberOf Opencast.Player
     * @description Stop the rewind in the video.
     */
    function stopRewind()
    {
        if (intvalRewind !== "")
        {
            window.clearInterval(intvalRewind);
            intvalRewind = "";
        }
        Videodisplay.stopRewind();
    }

    /**
     * @memberOf Opencast.Player
     * @description Stop fast forward the video.
     */
    function stopFastForward()
    {
        if (intvalFastForward !== "")
        {
            window.clearInterval(intvalFastForward);
            intvalFastForward = "";
        }
        Videodisplay.stopFastForward();
    }

    /**
     * @memberOf Opencast.Player
     * @description Show the current time span and hide the edit time input.
     */
    function hideEditTime()
    {
        $("#oc_current-time").removeClass("oc_current-time-hide").addClass('oc_current-time');
        $("#oc_edit-time").addClass("oc_edit-time-hide").removeClass("oc_edit-time");
        if ($("#oc_edit-time").hasClass("oc_edit-time-error"))
        {
            $("#oc_edit-time").val($("#oc_current-time").val());
            $("#oc_edit-time").removeClass("oc_edit-time-error");
        }
        if (backupPlayPauseState === PLAYING && Opencast.engage.getLoadProgress() != -1)
        {
            Opencast.Player.doPlay();
        }
        backupPlayPauseState = '';
    }

    /**
     * @memberOf Opencast.Player
     * @description Check the new time and seek the video to the new time.
     */
    function editTime()
    {
        var playheadString = $("#oc_edit-time").attr("value");
        var durationString = $("#oc_duration").text();
        playheadString = playheadString.replace(/[-\/]/g, ':');
        playheadString = playheadString.replace(/[^0-9:]/g, '');
        playheadString = playheadString.replace(/ +/g, ' ');
        var playheadArray = playheadString.split(':');
        var durationArray = durationString.split(':');
        try
        {
            var playheadHour = parseInt(playheadArray[0], 10);
            var playheadMinutes = parseInt(playheadArray[1], 10);
            var playheadSeconds = parseInt(playheadArray[2], 10);
            var durationHour = parseInt(durationArray[0], 10);
            var durationMinutes = parseInt(durationArray[1], 10);
            var durationSeconds = parseInt(durationArray[2], 10);
            if (playheadHour > 99 || playheadMinutes > 59 || playheadSeconds > 59)
            {
                addAlert('Wrong time enter like this: HH:MM:SS');
                $("#oc_edit-time").addClass("oc_edit-time-error");
            }
            else
            {
                var newPlayhead = (playheadHour * 60 * 60) + (playheadMinutes * 60) + (playheadSeconds);
                durationSeconds = (durationHour * 60 * 60) + (durationMinutes * 60) + (durationSeconds);
                if (isNaN(newPlayhead) || newPlayhead > durationSeconds)
                {
                    addAlert('Wrong time enter like this: HH:MM:SS');
                    $("#oc_edit-time").addClass("oc_edit-time-error");
                    //addEvent('BAD-EDIT-TIME');
                }
                else
                {
                    Videodisplay.seek(newPlayhead);
                    hideEditTime();
		    addEvent(Opencast.logging.VALID_EDIT_TIME);
                }
            }
        }
        catch (exception)
        {
            addAlert('Wrong time enter like this: HH:MM:SS');
            $("#oc_edit-time").addClass("oc_edit-time-error");
            //addEvent('EDIT-TIME-EXCEPTION');
        }
    }

    /**
     * @memberOf Opencast.Player
     * @description addAlert in html code.
     * @param String alertMessage
     */
    function currentTime(alertMessage)
    {
        addAlert(alertMessage);
    }

    /**
     * @memberOf Opencast.Player
     * @description Returns communication values from the Flash Videodisplay
     */
    function flashVars()
    {
        return {
            'playerType': FLASH_PLAYERTYPE,
            'playerState': FLASH_PLAYERSTATE,
            'viewState': FLASH_VIEWSTATE,
            'mute': FLASH_MUTE
        };
    }

    /*************************************************************/
	/* non-public
    /*************************************************************/

    /**
     * @memberOf Opencast.Player
     * @return true if video size control is visible
     */
    function displayVideoSizeControl()
    {
        return displVidSizeControl;
    }

    /**
     * @memberOf Opencast.Player
     * @description Get the current play pause state.
     */
    function getCurrentPlayPauseState()
    {
        return currentPlayPauseState;
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the current play pause state.
     * @param String state
     */
    function setCurrentPlayPauseState(state)
    {
        currentPlayPauseState = state;
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the seek state.
     * @param String state
     */
    function setSeekState(state)
    {
        seekState = state;
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the showSections.
     * @param Boolean bool
     */
    function setShowSections(bool)
    {
        showSections = bool;
    }

    /**
     * @memberOf Opencast.Player
     * @description Get the mouseOverBool.
     */
    function getMouseOverBool()
    {
        return mouseOverBool;
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the mouseOverBool.
     * @param Boolean bool
     */
    function setMouseOverBool(bool)
    {
        mouseOverBool = bool;
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the captionsBool.
     * @param Boolean bool
     */
    function setCaptionsBool(bool)
    {
        captionsBool = bool;
    }

    /**
     * @memberOf Opencast.Player
     * @description Get the durationText.
     */
    function setDurationText(text)
    {
        durationText = text;
    }

    /**
     * @memberOf Opencast.Player
     * @description Get the durationText.
     */
    function getDurationText()
    {
        return durationText;
    }

    /**
     * @memberOf Opencast.Player
     * @description Get the dragging.
     * @param Boolean bool
     */
    function getDragging()
    {
        return dragging;
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the htmlBool.
     * @param Boolean bool
     */
    function setHtmlBool(bool)
    {
        htmlBool = bool;
    }

    /**
     * @memberOf Opencast.Player
     * @description Get the displayMode.
     */
    function getDisplayMode()
    {
        return displayMode;
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the displayMode.
     * @param Sring mode
     */
    function setDisplayMode(mode)
    {
        displayMode = mode;
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the displayMode.
     * @param Sring videoSize
     */
    function setCurrentVideoSize(videoSize)
    {
        currentVideoSize = videoSize;
    }

    /**
     * @memberOf Opencast.Player
     * @description Show the notes
     */
    function showNotes()
    {
        $("#oc_notes").attr("className", "oc_DisplayBlock");
        $("#oc_btn-notes").attr(
        {
            title: NOTESHIDE
        });
        $("#oc_btn-notes").html(NOTESHIDE);
        $("#oc_btn-notes").attr('aria-pressed', 'true');
	addEvent(Opencast.logging.SHOW_NOTES);
    }

    /**
     * @memberOf Opencast.Player
     * @description Hide the notes
     */
    function hideNotes()
    {
        $("#oc_notes").attr("className", "oc_DisplayNone");
        $("#oc_btn-notes").attr(
        {
            title: NOTES
        });
        $("#oc_btn-notes").html(NOTES);
        $("#oc_btn-notes").attr('aria-pressed', 'false');
        addEvent(Opencast.logging.HIDE_NOTES);
    }

    /**
     * @memberOf Opencast.Player
     * @description Show the transcript
     */
    function showTranscript()
    {
        $("#oc_transcript").attr("className", "oc_DisplayBlock-textleft");
        $("#oc_btn-transcript").attr(
        {
            alt: TRANSCRIPTHIDE,
            title: TRANSCRIPTHIDE,
            value: TRANSCRIPTHIDE
        });
        $("#oc_btn-transcript").attr('aria-pressed', 'true');
        addEvent(Opencast.logging.SHOW_TRANSCRIPT);
    }

    /**
     * @memberOf Opencast.Player
     * @description Hide the transcript
     */
    function hideTranscript()
    {
        $("#oc_transcript").attr("className", "oc_DisplayNone");
        $("#oc_btn-transcript").attr(
        {
            alt: TRANSCRIPT,
            title: TRANSCRIPT,
            value: TRANSCRIPT
        });
        $("#oc_btn-transcript").attr('aria-pressed', 'false');
        addEvent(Opencast.logging.HIDE_TRANSCRIPT);
    }

    /**
     * @memberOf Opencast.Player
     * @description Shows the time layer
     */
    function showTimeLayer()
    {
        $("#oc_time-chooser-layer").position(
        {
            of: $("#oc_time-chooser"),
            my: 'left top',
            at: 'lfet bottom'
        });
        $('#oc_time-chooser-layer').show();
        $('#oc_time-chooser-layer').attr(
        {
            'aria-hidden': 'false',
            'tabindex': '0'
        });
        $('#oc_time-chooser-layer').focus();
        timeLayerDisplayed = true;
        addEvent(Opencast.logging.SHOW_TIME_LAYER);
    }

    /**
     * @memberOf Opencast.Player
     * @description Hides the time layer
     */
    function hideTimeLayer()
    {
        $('#oc_time-chooser-layer').attr(
        {
            'aria-hidden': 'true',
            'tabindex': '-1'
        });
        $('#oc_time-chooser-layer').hide();
        $('#oc_time-chooser').focus();
        timeLayerDisplayed = false;
        addEvent(Opencast.logging.HIDE_TIME_LAYER);
    }

    /**
     * @memberOf Opencast.Player
     * @description Shows Shortcuts
     */
    function showShortcuts()
    {
        $('#oc_shortcuts').dialog('open');
    }

    /**
     * @memberOf Opencast.Player
     * @description Shows downloads
     */
    function showDownloads()
    {
        $('#oc_downloads').dialog('open');
    }

    /**
     * @memberOf Opencast.Player
     * @description Add a Footpring.
     * @description Add a Footprint.
     */
    function addFootprint()
    {
        addEvent(Opencast.logging.FOOTPRINT);
    }

    /**
     * @memberOf Opencast.Player
     * @description Add an event to the log.
     */
    function addEvent(eventType)
    {
        //If the detailed logging is turned off (false), and the log type is not a footprint then return.
	if (!Opencast.Player.detailedLogging && (eventType != Opencast.logging.FOOTPRINT || eventType.matches("/.*AJAX-FAILED/i"))) {
          return;
        }
        $.ajax(
        {
            type: 'GET',
            url: "../../usertracking/?_method=PUT",
            data: "id=" + mediaPackageId + "&in=" + inPosition + "&out=" + outPosition + "&type=" + eventType + "&playing=" + isPlaying(),
            dataType: 'xml',
            success: function (xml)
            {
                // Do nothing, the event has been saved
            },
            error: function (a, b, c)
            {
                // Some error while adding the event
            }
        });
    }

    return {
        // getter
        getShowSections: getShowSections,
        getDuration: getDuration,
        getCaptionsBool: getCaptionsBool,
        getCurrentPosition: getCurrentPosition,
        getMediaPackageId: getMediaPackageId,
        getSessionId: getSessionId,
        getCurrentVideoSize: getCurrentVideoSize,
        getViewState: getViewState,
        getHtmlBool: getHtmlBool,
        getCurrentTime: getCurrentTime,
        // setter
        setDragging: setDragging,
        setMediaURL: setMediaURL,
        setCaptionsURL: setCaptionsURL,
        setBrowserWidth: setBrowserWidth,
        setCCIconOn: setCCIconOn,
        setCCIconOff: setCCIconOff,
        setPlayerVolume: setPlayerVolume,
        setPlayPauseState: setPlayPauseState,
        setCurrentTime: setCurrentTime,
        setTotalTime: setTotalTime,
        setMediaPackageId: setMediaPackageId,
        setUserId: setUserId,
        setSessionId: setSessionId,
        setOptionClassName: setOptionClassName,
        setDuration: setDuration,
        setPlayhead: setPlayhead,
        setProgress: setProgress,
        setVolumeSlider: setVolumeSlider,
        setVideoSizeList: setVideoSizeList,
	setShareTimeLink: setShareTimeLink,
        // do
        doToggleNotes: doToggleNotes,
        doToggleTranscript: doToggleTranscript,
        doToggleEmbed: doToggleEmbed,
	doToggleShareTime: doToggleShareTime,
        doToggleShare: doToggleShare,
        doToggleTimeLayer: doToggleTimeLayer,
        doToggleShortcuts: doToggleShortcuts,
        doToggleDownloads: doToggleDownloads,
        doTogglePlayPause: doTogglePlayPause,
        doToggleMute: doToggleMute,
        doPlay: doPlay,
        doPause: doPause,
        doFastForward: doFastForward,
        doSkipBackward: doSkipBackward,
        doRewind: doRewind,
        doSkipForward: doSkipForward,
        doSeek: doSeek,
        doToogleClosedCaptions: doToogleClosedCaptions,
        // show
        showShare: showShare,
        showEditTime: showEditTime,
        showEmbed: showEmbed,
	showShareTime: showShareTime,
        // hide
        hideShortcuts: hideShortcuts,
        hideShare: hideShare,
        hideEmbed: hideEmbed,
	hideShareTime: hideShareTime,
        // video size control
        videoSizeControlSingleDisplay: videoSizeControlSingleDisplay,
        videoSizeControlAudioDisplay: videoSizeControlAudioDisplay,
        videoSizeControlMultiOnlyLeftDisplay: videoSizeControlMultiOnlyLeftDisplay,
        videoSizeControlMultiOnlyRightDisplay: videoSizeControlMultiOnlyRightDisplay,
        videoSizeControlMultiBigRightDisplay: videoSizeControlMultiBigRightDisplay,
        videoSizeControlMultiBigLeftDisplay: videoSizeControlMultiBigLeftDisplay,
        videoSizeControlMultiDisplay: videoSizeControlMultiDisplay,
        videoSizeControlSinglePlayerWithSlides: videoSizeControlSinglePlayerWithSlides,
        // alert
        addAlert: addAlert,
        removeOldAlert: removeOldAlert,
        // sound
        lowSound: lowSound,
        noneSound: noneSound,
        highSound: highSound,
        muteSound: muteSound,
        // play/pause
        PlayPauseMouseOver: PlayPauseMouseOver,
        PlayPauseMouseOut: PlayPauseMouseOut,
        PlayPauseMouseDown: PlayPauseMouseDown,
        // misc
        isPlaying: isPlaying,
        shareOverlayDisplayed: shareOverlayDisplayed,
        shortcutOverlayDisplayed: shortcutOverlayDisplayed,
        refreshScrubberPosition: refreshScrubberPosition,
        embedIFrame: embedIFrame,
        stopRewind: stopRewind,
        stopFastForward: stopFastForward,
        hideEditTime: hideEditTime,
        editTime: editTime,
        currentTime: currentTime,
        flashVars: flashVars,
        addEvent: addEvent,
        addFootprint: addFootprint
    };
}());
