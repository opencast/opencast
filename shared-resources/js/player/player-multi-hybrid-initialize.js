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
 * @namespace the global Opencast namespace Initialize
 */
Opencast.Initialize = (function ()
{
    var myWidth = 0,
        myHeight = 0,
        OTHERDIVHEIGHT = 70, //needs to be changed in embed-hybrid-initialize.js:716 too
        MINWIDTH = 300,
        VOLUME = 'volume',
        VIDEOSIZE = 'videosize',
        divId = '',
        VIDEOSIZESINGLE = "vidoSizeSingle",
        VIDEOSIZEBIGRIGHT = "videoSizeBigRight",
        VIDEOSIZEBIGLEFT = "videoSizeBigLeft",
        VIDEOSIZEMULTI = "videoSizeMulti",
        VIDEOSIZEONLYRIGHT = "videoSizeOnlyRight",
        VIDEOSIZEONLYLEFT = "videoSizeOnlyLeft",
        VIDEOSIZEAUDIO = "videoSizeAudio",
        intvalOnPlayerReady = "",
        clickMatterhornSearchField = false,
        clickLecturerSearchField = false,
        playerReady = false,
        locked = false,
        formatOne = 0,
        formatTwo = 0,
        formatSingle = 0,
        maxFormat = 0,
        customHeight = '',
        customWidth = '',
        size = "",
        creatorPostfix = "",
        newHeight = 0,
        creator = "",
        segmentForwardDelay = 200,
        segmentBackwardDelay = 200,
        segmentTimeoutForward, segmentTimeoutBackward, segmentForwardClickedCounter = 0,
        segmentBackwardClickedCounter = 0,
        timeout = 800,
        closetimer = 0,
        ddmenuitem = 0,
        dropdownActive = false,
        keysSet = false,
        originalWidthMedia1 = 0,
        originalHeightMedia1 = 0,
        originalWidthMedia2 = 0,
        originalHeightMedia2 = 0,
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
        KEY_u;

    /**
     * @memberOf Opencast.Initialize
     * @description set the id of the div.
     */
    function setDivId(id)
    {
        divId = id;
    }

    /**
     * @memberOf Opencast.Initialize
     * @description get the id of the div.
     */
    function getDivId()
    {
        return divId;
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the maxFormat.
     * @param Sring format
     */
    function setMaxFormat(format)
    {
        maxFormat = format;
    }

    /**
     * @memberOf Opencast.Player
     * @description Get the maxFormat.
     * @return Sring maxFormat
     */
    function getMaxFormat()
    {
        return maxFormat;
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the customHeight.
     * @param Sring height
     */
    function setCustomHeight(height)
    {
        customHeight = height;
    }

    /**
     * @memberOf Opencast.Player
     * @description Get the customHeight.
     * @return Sring customHeight
     */
    function getCustomHeight()
    {
        return customHeight;
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the customWidth.
     * @param Sring height
     */
    function setCustomWidth(width)
    {
        customWidth = width;
    }

    /**
     * @memberOf Opencast.Player
     * @description Get the customWidth.
     * @return Sring customWidth
     */
    function getCustomWidth()
    {
        return customWidth;
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the playerReady.
     */
    function setPlayerReady(playerReadyBool)
    {
        playerReady = playerReadyBool;
    }

    /**
     * @memberOf Opencast.Player
     * @description Get the playerReady.
     * @return Boolean playerReady
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
                    event.which === KEY_1 ||
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
     * @memberOf Opencast.Initialize
     * @description close the drop dowan menue.
     */
    function dropdown_close()
    {
        if (ddmenuitem)
        {
            ddmenuitem.css('visibility', 'hidden');
        }
    }

    /**
     * @memberOf Opencast.Initialize
     * @description new timer.
     */
    function dropdown_timer()
    {
        closetimer = window.setTimeout(dropdown_close, timeout);
    }

    /**
     * @memberOf Opencast.Initialize
     * @description cancel the timer.
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
     * @memberOf Opencast.Initialize
     * @description open the drop down menue.
     */
    function dropdown_open()
    {
        if (getDivId() === VIDEOSIZE)
        {
            $('#oc_video-size-dropdown-div').css('width', '20%');
            $('#oc_player_video-dropdown').css('left', $('#oc_video-size-dropdown').offset().left - $('#oc_body').offset().left);
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
     * @memberOf Opencast.Initialize
     * @description open the drop down menu video.
     */
    function dropdownVideo_open()
    {
        setDivId(VIDEOSIZE);
        dropdown_open();
    }

    /**
     * @memberOf Opencast.Initialize
     * @description on player ready listener
     */
    function onPlayerReadyListener()
    {
        if (intvalOnPlayerReady === "")
        {
            intvalOnPlayerReady = window.setInterval("Opencast.Initialize.onPlayerReady()", 100);
        }
    }

    /**
     * @memberOf Opencast.Initialize
     * @description on player ready
     */
    function onPlayerReady()
    {
        if (getPlayerReady() === true)
        {
            Opencast.Watch.onPlayerReady();
            window.clearInterval(intvalOnPlayerReady);
            intvalOnPlayerReady = "";
        }
    }

    /**
     * @memberOf Opencast.Initialize
     * @description binds the Video Control Button
     */
    function bindVidSize()
    {
        $('#oc_video-size-controls').bind('mouseover', dropdownVideo_open);
        $('#oc_video-size-controls').bind('mouseout', dropdown_timer);
    }

    $(document).ready(function ()
    {
        keyboardListener();
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
        $('#oc_player_video-dropdown').bind('mouseover', dropdownVideo_open);
        $('#oc_player_video-dropdown').bind('mouseout', dropdown_timer);
        // Handler focus
        $('#oc_btn-dropdown').focus(function ()
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
            if (segmentTimeoutForward !== undefined)
            {
                clearTimeout(segmentTimeoutForward);
            }
            segmentForwardClickedCounter = 0;
            // Handle backward Timeout and Clicks
            if (segmentTimeoutBackward !== undefined)
            {
                clearTimeout(segmentTimeoutBackward);
            }
	    ++segmentBackwardClickedCounter;
            segmentTimeoutBackward = setTimeout(function ()
            {
                var currentSlideId = Opencast.segments.getCurrentSlideId();
                var sec = Opencast.segments.getSegmentSeconds(currentSlideId - segmentBackwardClickedCounter);
                if (sec < 0)
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
            if (segmentTimeoutBackward !== undefined)
            {
                clearTimeout(segmentTimeoutBackward);
            }
            segmentBackwardClickedCounter = 0;
            // Handle forward Timeout and Clicks
            if (segmentTimeoutForward !== undefined)
            {
                clearTimeout(segmentTimeoutForward);
            }++segmentForwardClickedCounter;
            segmentTimeoutForward = setTimeout(function ()
            {
                var currentSlideId = Opencast.segments.getCurrentSlideId();
                var sec = Opencast.segments.getSegmentSeconds(currentSlideId + segmentForwardClickedCounter);
                var secOfLastSeg = Opencast.segments.getSegmentSeconds(Opencast.segments.getNumberOfSegments() - 1);
                if ((sec == 0) || (sec > secOfLastSeg))
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
        $('#oc_searchField').click(function ()
        {
            if (clickMatterhornSearchField === false)
            {
                $("#oc_searchField").attr('value', '');
                clickMatterhornSearchField = true;
            }
        });
        $('#oc_lecturer-search-field').click(function ()
        {
            if (clickLecturerSearchField === false)
            {
                $("#oc_lecturer-search-field").attr('value', '');
                clickLecturerSearchField = true;
            }
            // Deselect any selected Tab
            $('#oc_ui_tabs').tabs('selected', -1);
            $(".ui-tabs-selected").removeClass("ui-state-active").removeClass("ui-tabs-selected");
        });
        $('#oc_btn-rewind').mousedown(function ()
        {
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
        $('#oc_btn-fast-forward').mousedown(function ()
        {
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
        $('#oc_btn-rewind').mouseup(function ()
        {
            Opencast.Player.stopRewind();
        });
        $('#oc_btn-play-pause').mouseup(function ()
        {
            Opencast.Player.PlayPauseMouseOver();
        });
        $('#oc_btn-fast-forward').mouseup(function ()
        {
            Opencast.Player.stopFastForward();
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
                Opencast.Player.doRewind();
            }
            else if (event.keyCode === 9)
            {
                Opencast.Player.stopRewind();
            }
        });
        $('#oc_btn-fast-forward').keydown(function (event)
        {
            if (event.keyCode === 13 || event.keyCode === 32)
            {
                Opencast.Player.doFastForward();
            }
            else if (event.keyCode === 9)
            {
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
                Opencast.Player.stopRewind();
            }
        });
        $('#oc_btn-fast-forward').keyup(function (event)
        {
            if (event.keyCode === 13 || event.keyCode === 32)
            {
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
        $('#oc_embed-costum-width-textinput').keyup(function (event)
        {
            if ((event.keyCode >= 48 && event.keyCode <= 57) || event.keyCode === 8 || event.keyCode === 9 || (event.keyCode >= 96 && event.keyCode <= 105))
            {
                setCustomWidth($('#oc_embed-costum-width-textinput').val());
                setCostumEmbedHeight();
            }
            else
            {
                $('#oc_embed-costum-width-textinput').attr('value', getCustomWidth());
            }
            $('#oc_embed-costum-width-textinput').css('background-color', '#ffffff');
        });
        $('#oc_embed-costum-height-textinput').keyup(function (event)
        {
            if ((event.keyCode >= 48 && event.keyCode <= 57) || event.keyCode === 8 || event.keyCode === 9 || (event.keyCode >= 96 && event.keyCode <= 105))
            {
                setCustomHeight($('#oc_embed-costum-height-textinput').val());
                setCostumEmbedWidth();
            }
            else
            {
                $('#oc_embed-costum-height-textinput').attr('value', getCustomHeight());
            }
            $('#oc_embed-costum-height-textinput').css('background-color', '#ffffff');
        }); /* initalise embed buttons */
        $("#oc_embed-icon-one, #oc_embed-icon-two, #oc_embed-icon-three, #oc_embed-icon-four, #oc_embed-icon-five", "#oc_embed-left").button();
	/* initalise search button */
        $("#oc_btn-search", "#oc_search").button();
        $("#oc_btn-cc", "#oc_video-time").button();
        $('#oc_btn-leave-share, .oc_btn-leave-session-time').button(
        {
            icons: {
                primary: 'ui-icon-close'
            },
            text: false
        });
        $('#oc_btn-leave-share, .oc_btn-leave-session-time').click(function ()
        {
            Opencast.Player.doToggleShare();
        }); /* initalise closed tabs */
        $("#oc_ui_tabs").tabs(
        {
            selected: -1
        });
        $("#oc_ui_tabs").tabs("option", "collapsible", true);
	/* handle select event for each tab */
        $("#oc_ui_tabs").tabs(
        {
            select: function (event, ui)
            {
                switch (ui.index)
                {
                case 0:
                    Opencast.Description.doToggle();
                    break;
                case 1:
                    Opencast.segments.doToggle();
                    break;
                case 2:
                    Opencast.segments_text.doToggle();
                    break;
                case 3:
                    Opencast.Annotation_Comment_List.doToggle();
                    break;
                case 4:
                    // Have a look at the - (engage-ui) watch.html - search trigger-function
                    break;
                }
            }
        });
        $("#oc_ui_tabs .ui-tabs-nav li").last().css('float', 'right');
        $(window).resize(function (e)
        {
            if (Opencast.Player.shareOverlayDisplayed())
            {
                Opencast.Player.showShare();
            }
	    Opencast.Player.addEvent(Opencast.logging.RESIZE_TO + $(window).width() + 'x' + $(window).height());
        });
        //bind click functions
        $('#oc_share-button').click(function (e)
        {
	    setEmbedButtons();
	    setEmbed();
            Opencast.Player.doToggleShare(e);
        });
        $('#oc_btn-email').click(function ()
        {
	    Opencast.Player.addEvent(Opencast.logging.EMAIL);
            Opencast.Player.doToggleShare();
        });
        $('#oc_time-chooser').click(function ()
        {
            Opencast.Player.doToggleTimeLayer();
        });
        $('#oc_checkbox-statistics').click(function ()
        {
            Opencast.Analytics.doToggle();
        });
        $('#oc_checkbox-annotations').click(function ()
        {
            Opencast.Annotation_Chapter.doToggle();
        });
        $('#oc_checkbox-annotation-comment').click(function ()
        {
            Opencast.Annotation_Comment.doToggle();
        });
        //bind click events to show dialog
        $('#oc_shortcuts').dialog(
        {
            autoOpen: false,
            width: 600
        });
        $('#oc_shortcut-button').click(function (e)
        {
            Opencast.Player.doToggleShortcuts(e, 'oc_shortcut-button');
        });

        $('#oc_downloads').dialog(
        {
            autoOpen: false,
            width: 600,
            resizable: false
        });
        $('#oc_download-button').click(function (e)
        {
            Opencast.Player.doToggleDownloads(e, 'oc_download-button');
        });

        $('#oc_embed').dialog(
        {
            autoOpen: false,
            width: 800
        });
        $('#oc_share-time').dialog(
        {
            autoOpen: false,
            width: 800
        });
        $('#oc_btn-embed').click(function ()
        {
            Opencast.Player.doToggleEmbed();
        });
        $('#oc_btn-share-time').click(function ()
        {
            Opencast.Player.doToggleShareTime();
        });
        $('#oc_series').hide();
        $('#oc_see-more-button').click(function (e)
        {
            Opencast.Series.doToggleSeriesDropdown()
        });
        $('#oc_video-player-controls').hide();

        // on change
        $('#oc_video-quality-options').change(function()
        {
            var videoQuality = $('#oc_video-quality-options').val();
            $.log("Request to set video quality to " + videoQuality + ", changing the URL...");
            var loc = window.location;
            var newLoc = $.getCleanedURLAdvanced(false, true, videoQuality, true);
            // change URL if new parameter
            if (loc != newLoc)
            {
                window.location = newLoc;
            }
        });

        onPlayerReadyListener();
        var mediaPackageId = $.getURLParameter('id');
        $.ajax(
        {
            url: '../../search/episode.json',
            data: 'id=' + mediaPackageId,
            dataType: 'jsonp',
            jsonp: 'jsonp',
            success: function (data)
            {
                if ((data !== undefined) && (data['search-results'] !== undefined) && (data['search-results'].result !== undefined))
                {
                    var result_data = data['search-results'].result;
                    var input_string = '';
                    if (result_data.dcSeriesTitle)
                    {
                        input_string += '<div id="oc_title-1" style="border-right: 2px solid black;">' + result_data.dcSeriesTitle + '</div><h2 id="oc_title-2">' + result_data.dcTitle + '</h2>';
                    }
                    else
                    {
                        input_string += '<h2 id="oc_title-1">' + result_data.dcTitle + '</h2>';
                    }
                    if (result_data.dcCreator)
                    {
                        input_string += '<div id="oc_title-creator">' + result_data.dcCreator + '</h2>';
                    }
                    $('#oc_title').html(input_string);
                }
                else
                {
                    $('#oc_title').html('');
                }
            },
            error: function ()
            {
		Opencast.Player.addEvent(Opencast.logging.NORMAL_SEARCH_AJAX_FAILED);
            }
        });
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
		  Opencast.Player.addEvent(Opencast.logging.NORMAL_STARTUP);
		  window.setInterval(function event() {
		      Opencast.Player.addEvent(Opencast.logging.HEARTBEAT);
		  }, 30 * 1000); //30 seconds
                } else {
                  Opencast.Player.detailedLogging = false;
                }
            },
            error: function (a, b, c)
            {
                Opencast.Player.detailedLogging = false;
		Opencast.Player.addEvent(Opencast.logging.NORMAL_DETAILED_LOGGING_AJAX_FAILED);
            }
        });
    });

    /**
     * http://www.roytanck.com
     * Roy Tanck
     * http://www.this-play.nl/tools/resizer.html
     */
    function reportSize()
    {
        myWidth = 0;
        myHeight = 0;
        if (typeof (window.innerWidth) === 'number')
        {
            //Non-IE
            myWidth = window.innerWidth;
            myHeight = window.innerHeight;
        }
        else
        {
            if (document.documentElement && (document.documentElement.clientWidth || document.documentElement.clientHeight))
            {
                //IE 6+ in 'standards compliant mode'
                myWidth = document.documentElement.clientWidth;
                myHeight = document.documentElement.clientHeight;
            }
            else
            {
                if (document.body && (document.body.clientWidth || document.body.clientHeight))
                {
                    //IE 4 compatible
                    myWidth = document.body.clientWidth;
                    myHeight = document.body.clientHeight;
                }
            }
        }
        Opencast.Player.setBrowserWidth(myWidth);
        Opencast.Player.refreshScrubberPosition();
    }

    /**
     * @memberOf Opencast.Player
     * @description Get the new height of the flash component.
     * @param Number mediaPercentOne, Number mediaPercentTwo
     */
    function getNewHeight(mediaPercentOne, mediaPercentTwo)
    {	
        var newHeight = 0;
        var flashContainerWidth = $('#oc_flash-player').width() - 10;
        var newHeightMediaOne = ((flashContainerWidth) * (mediaPercentOne / 100)) / formatOne;
        var newHeightMediaTwo = ((flashContainerWidth) * (mediaPercentTwo / 100)) / formatTwo;
        var newWidthMediaOne = newHeightMediaOne * formatOne;
        var newWidthMediaTwo = newHeightMediaTwo * formatTwo;
        if (newHeightMediaOne > newHeightMediaTwo)
        {
            newHeight = newHeightMediaOne;
        }
        else
        {
            newHeight = newHeightMediaTwo;
        }
        var otherContentHeight = 0;
        if (Opencast.Player.getShowSections() === true)
        {
            otherContentHeight = 310;
        }
        if (Opencast.Player.getShowSections() === false)
        {
            otherContentHeight = 200;
        }
        var contentHeight = newHeight + otherContentHeight;
        if (contentHeight > myHeight)
        {
            newHeight = newHeight - (contentHeight - myHeight);
            switch (size)
            {
            case VIDEOSIZEBIGRIGHT:
                newHeightMediaTwo = newHeight;
                newWidthMediaTwo = newHeight * formatTwo;
                break;
            case VIDEOSIZEBIGLEFT:
                newHeightMediaOne = newHeight;
                newWidthMediaOne = newHeight * formatOne;
                break;
            case VIDEOSIZEONLYRIGHT:
                newHeightMediaOne = 0;
                newWidthMediaOne = 0;
                newHeightMediaTwo = newHeight;
                newWidthMediaTwo = newHeight * formatTwo;
                break;
            case VIDEOSIZEONLYLEFT:
                newHeightMediaOne = newHeight;
                newWidthMediaOne = newHeight * formatOne;
                newHeightMediaTwo = 0;
                newWidthMediaTwo = 0;
                break;
            }
        }
        if (newHeight < MINWIDTH)
        {
            newHeight = MINWIDTH;
            switch (size)
            {
            case VIDEOSIZEBIGRIGHT:
                newHeightMediaTwo = newHeight;
                newWidthMediaTwo = newHeight * formatTwo;
                break;
            case VIDEOSIZEBIGLEFT:
                newHeightMediaOne = newHeight;
                newWidthMediaOne = newHeight * formatOne;
                break;
            case VIDEOSIZEONLYRIGHT:
                newHeightMediaOne = 0;
                newWidthMediaOne = 0;
                newHeightMediaTwo = newHeight;
                newWidthMediaTwo = newHeight * formatTwo;
                break;
            case VIDEOSIZEONLYLEFT:
                newHeightMediaOne = newHeight;
                newWidthMediaOne = newHeight * formatOne;
                newHeightMediaTwo = 0;
                newWidthMediaTwo = 0;
                break;
            }
        }

        var multiMediaContainerLeft = ((flashContainerWidth) - (newWidthMediaOne + newWidthMediaTwo)) / 2;
        Videodisplay.setMediaResolution(newWidthMediaOne, newHeightMediaOne, newWidthMediaTwo, newHeightMediaTwo, multiMediaContainerLeft);
        return Math.round(newHeight) + 10;
    }

    /**
     * @memberOf Opencast.Player
     * @description Get the new height of the flash component.
     * @param Number mediaPercentOne, Number mediaPercentTwo
     */
    function getNewHeightSingle()
    {
        var flashContainerWidth = $('#oc_flash-player').width() - 10;
        var newSingleHeight = flashContainerWidth / formatSingle;
        var otherContentHeight = 0;
        if (Opencast.Player.getShowSections() === true)
        {
            otherContentHeight = 310;
        }
        if (Opencast.Player.getShowSections() === false)
        {
            otherContentHeight = 200;
        }
        var contentHeight = newSingleHeight + otherContentHeight;
        if (contentHeight > myHeight)
        {
            newSingleHeight = newSingleHeight - (contentHeight - myHeight);
        }
        if (newSingleHeight < 300)
        {
            newSingleHeight = 300;
        }
        return Math.round(newSingleHeight) + 10;
    }

    function parseResolutionWidth(res) {
	return res.substring(0, res.lastIndexOf('x'));
    }

    function parseResolutionHeight(res) {
	return res.substring(res.lastIndexOf('x') + 1, res.length);
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the new height of the flash component
     */
    function doResize()
    {
        reportSize();
        size = Opencast.Player.getCurrentVideoSize();
        switch (size)
        {
        case VIDEOSIZEAUDIO:
            newHeight = 200;
            break;
        case VIDEOSIZESINGLE:
            newHeight = getNewHeightSingle();
            break;
        case VIDEOSIZEBIGRIGHT:
            newHeight = getNewHeight(33.333333333333, 66.666666666);
            break;
        case VIDEOSIZEBIGLEFT:
            newHeight = getNewHeight(66.666666666, 33.333333333333);
            break;
        case VIDEOSIZEONLYRIGHT:
            newHeight = getNewHeight(0, 100);
            break;
        case VIDEOSIZEONLYLEFT:
            newHeight = getNewHeight(100, 0);
            break;
        case VIDEOSIZEMULTI:
            newHeight = getNewHeight(50, 50);
            break;
        default:
            newHeight = getNewHeight(50, 50);
            break;
        }
        // set the new height
        if (newHeight > 0)
        {
            newHeight = Math.round(newHeight);
            $('#oc_flash-player').css("height", newHeight + "px");
            //Trigger Resize Event
            $('#oc_flash-player').trigger('doResize');
        }
    }

    /**
     * @memberOf Opencast.Player
     * @description init function
     */
    function init()
    {
        window.onresize = doResize;
        doResize();

	$.ajax(
	    {
		url: Opencast.Watch.getSearchURL(),
		data: 'id=' + mediaPackageId,
		dataType: 'jsonp',
		jsonp: 'jsonp',
		success: function (data)
		{
		    if ((data !== undefined) &&
			(data['search-results'] !== undefined) &&
			(data['search-results'].result !== undefined) &&
			(data['search-results'].result.mediapackage !== undefined)) {
			$(data['search-results'].result.mediapackage.media.track).each(
			    function (i)
			    {
				if(data['search-results'].result.mediapackage.media.track[i].video) {
				    $.log('Media resolution: ' + data['search-results'].result.mediapackage.media.track[i].video.resolution);
				    var pResWidth = parseResolutionWidth(data['search-results'].result.mediapackage.media.track[i].video.resolution);
				    var pResHeight = parseResolutionHeight(data['search-results'].result.mediapackage.media.track[i].video.resolution)
				    if(pResWidth > originalWidthMedia1) {
					originalWidthMedia1 = pResWidth;
					originalHeightMedia1 = pResHeight;
					originalWidthMedia2 = originalWidthMedia1;
					originalHeightMedia2 = originalHeightMedia1;
				    }
				}
			    }
			);
			setEmbedButtons();
			setEmbed();
		    } else {
			$.log("Player init Ajax call #3: Data undefined");
		    }
		},
		// If no data comes back (JSONP-Call #1)
		error: function (xhr, ajaxOptions, thrownError)
		{
		    $.log("Player init Ajax call #3: Requesting data failed");
		}
	    });

	$('#oc_embed-costum-hide-controls-controlsVisible').unbind('click');
	$('#oc_embed-costum-hide-controls-controlsNotVisible').unbind('click');
	$('#oc_embed-costum-hide-controls-controlsVisible').bind('click', hideControlsRadioButtonClicked);
	$('#oc_embed-costum-hide-controls-controlsNotVisible').bind('click', hideControlsRadioButtonClicked);
    }

    function hideControlsRadioButtonClicked() {
	Opencast.Initialize.setEmbedButtons();
	Opencast.Initialize.setEmbed();
        var embedWidth = $('#oc_embed-costum-width-textinput').val();
        var embedHeight = $('#oc_embed-costum-height-textinput').val();
        if ($.isNumber(embedWidth) && $.isNumber(embedHeight)) {
	    if($('#oc_embed-costum-hide-controls-controlsNotVisible').is(':checked')) {
		embedHeight = parseInt(embedHeight) + OTHERDIVHEIGHT;
	    } else {
		embedHeight = parseInt(embedHeight) - OTHERDIVHEIGHT;
	    }
	    Opencast.Player.embedIFrame(embedWidth, embedHeight, true);
	}
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the new custom height
     */
    function setCostumEmbedHeight()
    {
        var embedWidth = $('#oc_embed-costum-width-textinput').val();
        if ($.isNumber(embedWidth))
        {
	    var embedHeight = getAspectRatioHeight(embedWidth);
	    if(embedWidth >= MINWIDTH) {
		$('#oc_embed-costum-height-textinput').attr('value', embedHeight);
		$('#oc_embed-costum-height-textinput').css('background-color', '#ffffff');
		Opencast.Player.embedIFrame(embedWidth, embedHeight, true);
            } else
            {
		$('#oc_embed-costum-height-textinput').css('background-color', '#ff0000');
		$('#oc_embed-costum-height-textinput').attr('value', '');
		$('#oc_embed-textarea').val('Embed width too low. The minimum value is a width of ' + MINWIDTH + '.');
	    }
	} else {
	    $('#oc_embed-textarea').val('Embed width not valid. The minimum value is a width of ' + MINWIDTH + '.');
	}
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the new custom width
     */
    function setCostumEmbedWidth()
    {
        var embedHeight = $('#oc_embed-costum-height-textinput').val();
        if ($.isNumber(embedHeight))
        {
            var embedWidth = getAspectRatioWidth(embedHeight);
	    if(embedWidth >= MINWIDTH) {
		$('#oc_embed-costum-width-textinput').attr('value', embedWidth);
		$('#oc_embed-costum-width-textinput').css('background-color', '#ffffff');
		Opencast.Player.embedIFrame(embedWidth, embedHeight, true);
	    } else {
		$('#oc_embed-costum-width-textinput').css('background-color', '#ff0000');
		$('#oc_embed-costum-width-textinput').attr('value', '');
		$('#oc_embed-textarea').val('');
		$('#oc_embed-textarea').val('Embed width too low. The minimum value is a width of ' + MINWIDTH + '.');
	    }
	} else {
	    $('#oc_embed-textarea').val('Embed height not valid. The minimum value is a height of ' + MINWIDTH + '.');
	}
    }

    function getAspectRatioWidth(height) {
	var newOtherDivHeight = $('#oc_embed-costum-hide-controls-controlsNotVisible').is(':checked') ? OTHERDIVHEIGHT : 0;
	// new width = new height * original width / original height
        var width = Math.round(height * originalWidthMedia1 / originalHeightMedia1) + newOtherDivHeight;
	return width;
    }

    function getAspectRatioHeight(width) {
	var newOtherDivHeight = $('#oc_embed-costum-hide-controls-controlsNotVisible').is(':checked') ? OTHERDIVHEIGHT : 0;
	// new height = original height / original width * new width
        var height = Math.round(originalHeightMedia1 / originalWidthMedia1 * width) + newOtherDivHeight;
	return height;
    }
    
    function setEmbedButtons() {
        var embedWidhtOne = 620;
        var embedWidhtTwo = 540;
        var embedWidhtThree = 460
        var embedWidhtFour = 380;
        var embedWidhtFive = 300;
	
	var newOtherDivHeight = $('#oc_embed-costum-hide-controls-controlsNotVisible').is(':checked') ? OTHERDIVHEIGHT : 0;
	
	// new height = original height / original width * new width

        var embedHeightOne = getAspectRatioHeight(embedWidhtOne);
        var embedHeightTwo = getAspectRatioHeight(embedWidhtTwo);
        var embedHeightThree = getAspectRatioHeight(embedWidhtThree);
        var embedHeightFour = getAspectRatioHeight(embedWidhtFour);
        var embedHeightFive = getAspectRatioHeight(embedWidhtFive);
	
        $("#oc_embed-icon-one").css("width", "110px");
        $("#oc_embed-icon-one").css("height", "73px");
        $("#oc_embed-icon-one").attr(
        {
            alt: embedWidhtOne + ' x ' + embedHeightOne,
            title: embedWidhtOne + ' x ' + embedHeightOne,
            name: embedWidhtOne + ' x ' + embedHeightOne,
            value: embedWidhtOne + ' x ' + embedHeightOne
        });

        var embedWidth = $('#oc_embed-costum-width-textinput').val();
        var embedHeight = $('#oc_embed-costum-height-textinput').val();

        $('#oc_embed-icon-one').click(function ()
        {
	    embedWidth = embedWidhtOne;
	    embedHeight = embedHeightOne;
            Opencast.Player.embedIFrame(embedWidth, embedHeight, true);
        });
        $("#oc_embed-icon-two").css("width", "100px");
        $("#oc_embed-icon-two").css("height", "65px");
        $("#oc_embed-icon-two").attr(
        {
            alt: embedWidhtTwo + ' x ' + embedHeightTwo,
            title: embedWidhtTwo + ' x ' + embedHeightTwo,
            name: embedWidhtTwo + ' x ' + embedHeightTwo,
            value: embedWidhtTwo + ' x ' + embedHeightTwo
        });
        $('#oc_embed-icon-two').click(function ()
        {
	    embedWidth = embedWidhtTwo;
	    embedHeight = embedHeightTwo;
            Opencast.Player.embedIFrame(embedWidth, embedHeight, true);
        });
        $("#oc_embed-icon-three").css("width", "90px");
        $("#oc_embed-icon-three").css("height", "58px");
        $("#oc_embed-icon-three").attr(
        {
            alt: embedWidhtThree + ' x ' + embedHeightThree,
            title: embedWidhtThree + ' x ' + embedHeightThree,
            name: embedWidhtThree + ' x ' + embedHeightThree,
            value: embedWidhtThree + ' x ' + embedHeightThree
        });
        $('#oc_embed-icon-three').click(function ()
        {
	    embedWidth = embedWidhtThree;
	    embedHeight = embedHeightThree;
            Opencast.Player.embedIFrame(embedWidth, embedHeight, true);
        });
        $("#oc_embed-icon-four").css("width", "80px");
        $("#oc_embed-icon-four").css("height", "50px");
        $("#oc_embed-icon-four").attr(
        {
            alt: embedWidhtFour + ' x ' + embedHeightFour,
            title: embedWidhtFour + ' x ' + embedHeightFour,
            name: embedWidhtFour + ' x ' + embedHeightFour,
            value: embedWidhtFour + ' x ' + embedHeightFour
        });
        $('#oc_embed-icon-four').click(function ()
        {
	    embedWidth = embedWidhtFour;
	    embedHeight = embedHeightFour;
            Opencast.Player.embedIFrame(embedWidth, embedHeight, true);
        });
        $("#oc_embed-icon-five").css("width", "70px");
        $("#oc_embed-icon-five").css("height", "42px");
        $("#oc_embed-icon-five").attr(
        {
            alt: embedWidhtFive + ' x ' + embedHeightFive,
            title: embedWidhtFive + ' x ' + embedHeightFive,
            name: embedWidhtFive + ' x ' + embedHeightFive,
            value: embedWidhtFive + ' x ' + embedHeightFive
        });
        $('#oc_embed-icon-five').click(function ()
        {
	    embedWidth = embedWidhtFive;
	    embedHeight = embedHeightFive;
            Opencast.Player.embedIFrame(embedWidth, embedHeight, true);
        });
    }

    /**
     * @memberOf Opencast.Player
     * @description Set the embed height and width
     */
    function setEmbed()
    {
        if (formatSingle != 0)
        {
            setMaxFormat(formatSingle);
        }
        else if (formatOne > formatTwo)
        {
            setMaxFormat(formatOne);
        }
        else
        {
            setMaxFormat(formatTwo);
        }
        // you must not divide by zero
        if (getMaxFormat() == 0)
        {
            setMaxFormat(1);
        }

        $('.oc_embed-icon').click(function ()
        {
            $('#oc_embed-textarea').select();
            $(this).removeClass('ui-state-focus');
        });
        var embedCustomMinHeight = Math.round(MINWIDTH / getMaxFormat()) + OTHERDIVHEIGHT;
        $("#oc_embed-costum-height-textinput").attr(
        {
            name: 'Custom Height min ' + embedCustomMinHeight + 'px',
            alt: 'Custom Height min ' + embedCustomMinHeight + 'px',
            title: 'Custom Height min ' + embedCustomMinHeight + 'px'
        });
    }

    /**
     * @memberOf Opencast.Player
     * @description Set media resuliton of the videos
     * @param Number mediaResolutionOne, Number mediaResolutionTwo
     */
    function setMediaResolution(mediaResolutionOne, mediaResolutionTwo)
    {
        var mediaResolutionOneString = mediaResolutionOne,
            mediaResolutionTwoString = mediaResolutionTwo,
            mediaOneWidth = 1,
            mediaOneHeight = 1,
            mediaTwoWidth = 1,
            mediaTwoHeight = 1,
            mediaSingleWidth = 1,
            mediaSingleHeight = 1;
        // Parse first string
        var mediaResolutionOneArray = mediaResolutionOneString.split('x');
        var v1 = parseInt(mediaResolutionOneArray[0], 10);
        var v2 = parseInt(mediaResolutionOneArray[1], 10);
        var arr1IsUndef = isNaN(v1) || isNaN(v2);
        if (!arr1IsUndef)
        {
            mediaOneWidth = v1;
            mediaOneHeight = v2;
            mediaSingleWidth = v1;
            mediaSingleHeight = v2;
            formatSingle = (mediaSingleHeight != 0) ? (mediaSingleWidth / mediaSingleHeight) : mediaSingleWidth;
        }
        if (!arr1IsUndef && (mediaResolutionTwoString != ''))
        {
            // Parse second string
            var mediaResolutionTwoArray = mediaResolutionTwoString.split('x');
            var v3 = parseInt(mediaResolutionTwoArray[0], 10);
            var v4 = parseInt(mediaResolutionTwoArray[1], 10);
            var arr2IsUndef = isNaN(v3) || isNaN(v4);
            if (!arr2IsUndef)
            {
                mediaTwoWidth = v3;
                mediaTwoHeight = v4;
                formatOne = (mediaOneHeight >= 0) ? (mediaOneWidth / mediaOneHeight) : mediaOneWidth;
                formatTwo = (mediaTwoHeight >= 0) ? (mediaTwoWidth / mediaTwoHeight) : mediaTwoWidth;
            }
        }
    }
    return {
        bindVidSize: bindVidSize,
        doResize: doResize,
        dropdownVideo_open: dropdownVideo_open,
        dropdown_timer: dropdown_timer,
        setPlayerReady: setPlayerReady,
        onPlayerReady: onPlayerReady,
        init: init,
	setEmbedButtons: setEmbedButtons,
	setEmbed: setEmbed,
        setMediaResolution: setMediaResolution
    };
}());
