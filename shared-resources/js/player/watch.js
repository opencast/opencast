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
 * @namespace the global Opencast namespace watch
 */
Opencast.Watch = (function ()
{
    var advancedPlayer = true;

    var MULTIPLAYER = "Multiplayer",
        SINGLEPLAYER = "Singleplayer",
        SINGLEPLAYERWITHSLIDES = "SingleplayerWithSlides",
        AUDIOPLAYER = "Audioplayer",
        PLAYERSTYLE = "advancedPlayer",
        mediaResolutionOne = "",
        mediaResolutionTwo = "",
        mediaUrlOne = "",
        mediaUrlTwo = "",
        mimetypeOne = "",
        mimetypeTwo = "",
        coverUrlOne = "",
        coverUrlTwo = "",
        slideLength = 0,
        timeoutTime = 400,
        duration = 0,
        bufferTime = parseInt(getURLParameter("bt")),
        mediaPackageIdAvailable = true,
        durationSetSuccessfully = false,
        mediaPackageId;

    var analyticsURL = "",
        annotationURL = "",
        annotationCommentURL = "",
        descriptionEpisodeURL = "",
        descriptionStatsURL = "",
        searchURL = "",
        segmentsTextURL = "",
        segmentsUIURL = "",
        segmentsURL = "",
        seriesSeriesURL = "",
        seriesEpisodeURL = "";

    /**
     * @memberOf Opencast.Watch
     * @description Returns a plugin URL
     * @return a plugin URL
     */
    function getAnalyticsURL()
    {
        return analyticsURL;
    }

    /**
     * @memberOf Opencast.Watch
     * @description Returns a plugin URL
     * @return a plugin URL
     */
    function getAnnotationURL()
    {
        return annotationURL;
    }

    /**
     * @memberOf Opencast.Watch
     * @description Returns a plugin URL
     * @return a plugin URL
     */
    function getAnnotationCommentURL()
    {
        return annotationCommentURL;
    }

    /**
     * @memberOf Opencast.Watch
     * @description Returns a plugin URL
     * @return a plugin URL
     */
    function getDescriptionEpisodeURL()
    {
        return descriptionEpisodeURL;
    }

    /**
     * @memberOf Opencast.Watch
     * @description Returns a plugin URL
     * @return a plugin URL
     */
    function getDescriptionStatsURL()
    {
        return descriptionStatsURL;
    }

    /**
     * @memberOf Opencast.Watch
     * @description Returns a plugin URL
     * @return a plugin URL
     */
    function getSearchURL()
    {
        return searchURL;
    }

    /**
     * @memberOf Opencast.Watch
     * @description Returns a plugin URL
     * @return a plugin URL
     */
    function getSegmentsTextURL()
    {
        return segmentsTextURL;
    }

    /**
     * @memberOf Opencast.Watch
     * @description Returns a plugin URL
     * @return a plugin URL
     */
    function getSegmentsUIURL()
    {
        return segmentsUIURL;
    }

    /**
     * @memberOf Opencast.Watch
     * @description Returns a plugin URL
     * @return a plugin URL
     */
    function getSegmentsURL()
    {
        return segmentsURL;
    }

    /**
     * @memberOf Opencast.Watch
     * @description Returns a plugin URL
     * @return a plugin URL
     */
    function getSeriesSeriesURL()
    {
        return seriesSeriesURL;
    }

    /**
     * @memberOf Opencast.Watch
     * @description Returns a plugin URL
     * @return a plugin URL
     */
    function getSeriesEpisodeURL()
    {
        return seriesEpisodeURL;
    }

    /**
     * @memberOf Opencast.Watch
     * @description Parses a query string
     */
    function parseQueryString(qs)
    {
        var urlParams =
        {
        };
        var currentUrl = window.location.href;
        var email_message = "mailto:?subject=I recommend you to take a look at this Opencast video&body=Please have a look at: "
        document.getElementById("oc_btn-email").setAttribute("href", email_message + currentUrl);
        var e, d = function (s)
        {
            return decodeURIComponent(s.replace(/\+/g, " "));
        },
            q = window.location.search.substring(1),
            r = /([^&=]+)=?([^&]*)/g;
        while (e = r.exec(q))
        {
            urlParams[d(e[1])] = d(e[2]);
        }
        return urlParams;
    }

    /**
     * @memberOf Opencast.Watch
     * @description Sets up the Plugins
     */
    function onPlayerReady()
    {
        var logsEnabled = ($.getURLParameter('log') == "true") ? true : false;
        $.enableLogging(logsEnabled);

        $.log("Player ready");

        // check if advanced player or embed player
        var loc = window.location.href;
        if(loc.search(/embed.html.+/g) != -1)
        {
            advancedPlayer = false;
            $.log("Player is: Embed Player");
            PLAYERSTYLE = "embedPlayer";
        } else
        {
            $.log("Player is: Advanced Player");
        }

        // Parse the plugin URLs
        $.getJSON('json/servicedata.json', function(data)
        {
            $.log("Start parsing servicedata.json");
            analyticsURL = data.plugin_urls.analytics;
            annotationURL = data.plugin_urls.annotation;
            annotationCommentURL = data.plugin_urls.annotationComment;
            descriptionEpisodeURL = data.plugin_urls.description.episode;
            descriptionStatsURL = data.plugin_urls.description.stats;
            searchURL = data.plugin_urls.search;
            segmentsTextURL = data.plugin_urls.segments_text;
            segmentsUIURL = data.plugin_urls.segments_ui;
            segmentsURL = data.plugin_urls.segments;
            seriesSeriesURL = data.plugin_urls.series.series;
            seriesEpisodeURL = data.plugin_urls.series.episode;

            $.log("Plugin URLs");
            $.log("Analytics URL: " + analyticsURL);
            $.log("Annotation URL: " + annotationURL);
            $.log("Annotation Comments URL: " + annotationCommentURL);
            $.log("Description (Episode) URL: " + descriptionEpisodeURL);
            $.log("Description (Stats) URL: " + descriptionStatsURL);
            $.log("Search URL: " + searchURL);
            $.log("Segments (Text) URL: " + segmentsTextURL);
            $.log("Segments (UI) URL: " + segmentsUIURL);
            $.log("Segments URL: " + segmentsURL);
            $.log("Series (Series) URL: " + seriesSeriesURL);
            $.log("Series (Episode) URL: " + seriesEpisodeURL);

            var URLParamId = $.getURLParameter('id');
            var URLParamMediaURL1 = $.getURLParameter('mediaUrl1');
            var URLParamMediaURL2 = $.getURLParameter('mediaUrl2');
            var URLParamRes1 = $.getURLParameter('mediaResolution1');
            var URLParamRes2 = $.getURLParameter('mediaResolution2');
            var URLParamMT1 = $.getURLParameter('mimetype1');
            var URLParamMT2 = $.getURLParameter('mimetype2');

            // prefer URL parameter, don't set any of it to empty string because then flash init fails
            mediaPackageId = (URLParamId == null) ? ((data.mediaDebugInfo.mediaPackageId == "") ? null : data.mediaDebugInfo.mediaPackageId) : URLParamId;
            mediaUrlOne = (URLParamMediaURL1 == null) ? ((data.mediaDebugInfo.mediaUrlOne == "") ? null : data.mediaDebugInfo.mediaUrlOne) : URLParamMediaURL1;
            mediaUrlTwo = (URLParamMediaURL2 == null) ? ((data.mediaDebugInfo.mediaUrlTwo == "") ? null : data.mediaDebugInfo.mediaUrlTwo) : URLParamMediaURL2;
            mediaResolutionOne = (URLParamRes1 == null) ? ((data.mediaDebugInfo.mediaResolutionOne == "") ? null : data.mediaDebugInfo.mediaResolutionOne) : URLParamRes1;
            mediaResolutionTwo = (URLParamRes2 == null) ? ((data.mediaDebugInfo.mediaResolutionTwo == "") ? null : data.mediaDebugInfo.mediaResolutionTwo) : URLParamRes2;
            mimetypeOne = (URLParamMT1 == null) ? ((data.mediaDebugInfo.mimetypeOne == "") ? null : data.mediaDebugInfo.mimetypeOne) : URLParamMT1;
            mimetypeTwo = (URLParamMT2 == null) ? ((data.mediaDebugInfo.mimetypeTwo == "") ? null : data.mediaDebugInfo.mimetypeTwo) : URLParamMT2;
            
            $.log("Media Debug Info");
            $.log("Mediapackage ID: " + mediaPackageId);
            $.log("Media URL 1: " + mediaUrlOne);
            $.log("Media URL 2: " + mediaUrlTwo);
            $.log("Media resolution 1: " + mediaResolutionOne);
            $.log("Media resolution 1: " + mediaResolutionTwo);
            $.log("Mimetype 1: " + mimetypeOne);
            $.log("Mimetype 2: " + mimetypeTwo);

            $.log("Successfully parsed servicedata.json");
            
            if(advancedPlayer)
            {
                var userId = $.getURLParameter('user');
                var restEndpoint = Opencast.engage.getSearchServiceEpisodeIdURL() + mediaPackageId;
                Opencast.Player.setSessionId(Opencast.engage.getCookie("JSESSIONID"));
                Opencast.Player.setUserId(userId);
                // Set MediaPackage ID's in the Plugins
                Opencast.Player.setMediaPackageId(mediaPackageId);
                Opencast.Annotation_Chapter.setMediaPackageId(mediaPackageId);
            	Opencast.Annotation_Comment.setMediaPackageId(mediaPackageId);
            	Opencast.Annotation_Comment_List.setMediaPackageId(mediaPackageId);
                Opencast.Analytics.setMediaPackageId(mediaPackageId);
                Opencast.Series.setMediaPackageId(mediaPackageId);
                Opencast.Description.setMediaPackageId(mediaPackageId);
                Opencast.segments_ui.setMediaPackageId(mediaPackageId);
                Opencast.segments.setMediaPackageId(mediaPackageId);
                Opencast.segments_text.setMediaPackageId(mediaPackageId);
                Opencast.search.setMediaPackageId(mediaPackageId);
                // Initialize Segments UI
                Opencast.segments_ui.initialize();
            } else
            {
                var userId = $.getURLParameter('user');
                if (mediaPackageId === null)
                {
                    mediaPackageIdAvailable = false;
                }
                var restEndpoint = Opencast.engage.getSearchServiceEpisodeIdURL() + mediaPackageId;
                restEndpoint = $.getURLParameter('videoUrl') !== null ? "preview.xml" : restEndpoint;
                Opencast.Player.setSessionId(Opencast.engage.getCookie("JSESSIONID"));
                Opencast.Player.setUserId(userId);
                if (mediaPackageIdAvailable)
                {
                    // Set MediaPackage ID's in the Plugins
                    Opencast.Player.setMediaPackageId(mediaPackageId);
                    Opencast.Series.setMediaPackageId(mediaPackageId);
                    Opencast.Description.setMediaPackageId(mediaPackageId);
                    Opencast.segments_ui.setMediaPackageId(mediaPackageId);
                    Opencast.segments.setMediaPackageId(mediaPackageId);
                    Opencast.segments_text.setMediaPackageId(mediaPackageId);
                    // Initialize Segments UI
                    Opencast.segments_ui.initialize();
                }
                else
                {
                    $('#oc_btn-skip-backward').hide();
                    $('#oc_btn-skip-forward').hide();
                    continueProcessing();
                }
            }
        });
    }

    /**
     * @memberOf Opencast.Watch
     * @description Sets up the html page after the player and the Plugins have been initialized.
     * @param error flag if error occured (=> display nothing, hide initialize); optional: set only if an error occured
     */
    function continueProcessing(error)
    {
        var err = error || false;
        $.log("Continue processing (" + (error ? "with error" : "without error") + ")");
        if (error)
        {
            if(advancedPlayer)
            {
                $('#oc_Videodisplay').hide();
                $('#initializing').html('The media is not available.');
                $('#oc_flash-player-loading').css('width', '60%');
                $('#loading-init').hide();
                return;
            } else
            {
                $('body').css('background-color', '#000000');
                $('body').html('<span id="initializing-matter">matter</span><span id="initializing-horn">horn</span><span id="initializing">&nbsp;The media is not available.</span>');
                $('#initializing').css('color', '#FFFFFF');
                return;
            }
        }
        if(advancedPlayer)
        {
            // set the title of the page
            document.title = $('#oc-title').html() + " | Opencast Matterhorn - Media Player";
            var dcExtent = parseInt($('#dc-extent').html());
            Opencast.Description.initialize();
            Opencast.Analytics.setDuration(parseInt(parseInt(dcExtent) / 1000));
            Opencast.Analytics.initialize();
            Opencast.Annotation_Chapter.setDuration(parseInt(parseInt(dcExtent) / 1000));
            Opencast.Annotation_Chapter.initialize();
	    	Opencast.Annotation_Comment.setDuration(parseInt(parseInt(dcExtent) / 1000));
            Opencast.Annotation_Comment.initialize();
            Opencast.Annotation_Comment_List.initialize();
            $('#oc_body').bind('resize', function ()
            {
                Opencast.AnalyticsPlugin.resizePlugin();
            });
            $('#oc_segment-table').html($('#oc-segments').html());
            $('#oc_search').show();
        } else
        {
            $(".segments").css("margin-top", "-3px");
        }
        $('#oc-segments').html("");
        // set the media URLs
        if(mediaUrlOne === null)
        {
            mediaUrlOne = $('#oc-video-presenter-delivery-x-flv-rtmp').html();
        }
        if(mediaUrlTwo === null)
        {
            mediaUrlTwo = $('#oc-video-presentation-delivery-x-flv-rtmp').html();
        }
        if(mediaResolutionOne === null)
        {
            mediaResolutionOne = $('#oc-resolution-presenter-delivery-x-flv-rtmp').html();
        }
        if(mediaResolutionTwo === null)
        {
            mediaResolutionTwo = $('#oc-resolution-presentation-delivery-x-flv-rtmp').html();
        }
        // set default mimetypes
        if(mimetypeOne === null)
        {
            mimetypeOne = "video/x-flv";
        }
        if(mimetypeTwo === null)
        {
            mimetypeTwo = "video/x-flv";
        }
        // mimetypeOne = "audio/x-flv";
        // mimetypeTwo = "audio/x-flv";
        coverUrlOne = $('#oc-cover-presenter').html();
        coverUrlTwo = $('#oc-cover-presentation').html();
        if (coverUrlOne === null)
        {
            coverUrlOne = coverUrlTwo;
            coverUrlTwo = '';
        }
        if (mediaUrlOne === null)
        {
            mediaUrlOne = $('#oc-video-presenter-delivery-x-flv-http').html();
            mediaResolutionOne = $('#oc-resolution-presenter-delivery-x-flv-http').html();
            mimetypeOne = $('#oc-mimetype-presenter-delivery-x-flv-http').html();
        }
        if (mediaUrlOne === null)
        {
            mediaUrlOne = $('#oc-video-presenter-source-x-flv-rtmp').html();
            mediaResolutionOne = $('#oc-resolution-presenter-source-x-flv-rtmp').html();
            mimetypeOne = $('#oc-mimetype-presenter-source-x-flv-rtmp').html();
        }
        if (mediaUrlOne === null)
        {
            mediaUrlOne = $('#oc-video-presenter-source-x-flv-http').html();
            mediaResolutionOne = $('#oc-resolution-presenter-source-x-flv-http').html();
            mimetypeOne = $('#oc-mimetype-presenter-source-x-flv-http').html();
        }
        if (mediaUrlTwo === null)
        {
            mediaUrlTwo = $('#oc-video-presentation-delivery-x-flv-http').html();
            mediaResolutionTwo = $('#oc-resolution-presentation-delivery-x-flv-http').html();
            mimetypeTwo = $('#oc-mimetype-presentation-delivery-x-flv-http').html();
        }
        if (mediaUrlTwo === null)
        {
            mediaUrlTwo = $('#oc-video-presentation-source-x-flv-rtmp').html();
            mediaResolutionTwo = $('#oc-resolution-presentation-source-x-flv-rtmp').html();
            mimetypeTwo = $('#oc-mimetype-presentation-source-x-flv-rtmp').html();
        }
        if (mediaUrlTwo === null)
        {
            mediaUrlTwo = $('#oc-video-presentation-source-x-flv-http').html();
            mediaResolutionTwo = $('#oc-resolution-presentation-source-x-flv-http').html();
            mimetypeTwo = $('#oc-mimetype-presentation-source-x-flv-http').html();
        }
        if (mediaUrlOne === null)
        {
            mediaUrlOne = mediaUrlTwo;
            mediaUrlTwo = null;
            mediaResolutionOne = mediaResolutionTwo;
            mediaResolutionTwo = null;
            mimetypeOne = mimetypeTwo;
            mimetypeTwo = null;
        }
        mediaUrlOne = mediaUrlOne === null ? '' : mediaUrlOne;
        mediaUrlTwo = mediaUrlTwo === null ? '' : mediaUrlTwo;
        coverUrlOne = coverUrlOne === null ? '' : coverUrlOne;
        coverUrlTwo = coverUrlTwo === null ? '' : coverUrlTwo;
        mimetypeOne = mimetypeOne === null ? '' : mimetypeOne;
        mimetypeTwo = mimetypeTwo === null ? '' : mimetypeTwo;
        mediaResolutionOne = mediaResolutionOne === null ? '' : mediaResolutionOne;
        mediaResolutionTwo = mediaResolutionTwo === null ? '' : mediaResolutionTwo;

        // Check for videoUrl and videoUrl2 URL Parameters
        var mediaUrlTmp = $.getURLParameter('videoUrl');
        mediaUrlOne = (mediaUrlTmp == null) ? mediaUrlOne : mediaUrlTmp;
        if(mediaUrlOne != null)
        {
            $.log('Set Video URL 1 manually');
        }
        mediaUrlTmp = $.getURLParameter('videoUrl2');
        mediaUrlTwo = (mediaUrlTmp == null) ? mediaUrlTwo : mediaUrlTmp;
        if(mediaUrlTwo != null)
        {
            $.log('Set Video URL 2 manually');
        }

        // If URL Parameter display exists and is set to revert
        var display = $.getURLParameter('display');
        if ((display != null) && (display.toLowerCase() == 'invert') && (mediaUrlTwo != ''))
        {
            $.log("Inverting the displays and its covers");
            // Invert the displays and its covers
            var tmpMediaURLOne = mediaUrlOne;
            var tmpCoverURLOne = coverUrlOne;
            var tmpMimetypeOne = mimetypeOne;
            var tmpMediaResolution = mediaResolutionOne;
            mediaUrlOne = mediaUrlTwo;
            coverUrlOne = coverUrlTwo;
            mimetypeOne = mimetypeTwo;
            mediaResolutionOne = mediaResolutionTwo;
            mediaUrlTwo = tmpMediaURLOne;
            coverUrlTwo = tmpCoverURLOne;
            mimetypeTwo = tmpMimetypeOne;
            mediaResolutionTwo = tmpMediaResolution;
        }

	var displayOneVideo = $.getURLParameter('displayOneVideo');
        if ((displayOneVideo != null) && (displayOneVideo.toLowerCase() == 'true'))
	{
	    displayOneVideo = true;
	} else
	{
	    displayOneVideo = false;
	}
	    
	$.log("-----");
        $.log("Final Mediadata");
        $.log("Mediapackage ID: " + mediaPackageId);
        $.log("Media URL 1: " + mediaUrlOne);
        $.log("Media URL 2: " + mediaUrlTwo);
        $.log("Media resolution 1: " + mediaResolutionOne);
        $.log("Media resolution 1: " + mediaResolutionTwo);
        $.log("Mimetype 1: " + mimetypeOne);
        $.log("Mimetype 2: " + mimetypeTwo);
	$.log("-----");

        if(advancedPlayer)
        {
            // init the segements
            Opencast.segments.initialize();
            // init the segements_text
            Opencast.segments_text.initialize();
            slideLength = Opencast.segments.getSlideLength();
            Opencast.Player.setMediaURL(coverUrlOne, coverUrlTwo, mediaUrlOne, mediaUrlTwo, mimetypeOne, mimetypeTwo, PLAYERSTYLE, slideLength, bufferTime);
            if (mediaUrlOne !== '' && mediaUrlTwo !== '')
            {
		$.log('Both media URLs are not empty, setting up a multiplayer');
                Opencast.Player.setVideoSizeList(MULTIPLAYER);
            }
            else if (mediaUrlOne !== '' && mediaUrlTwo === '')
            {
		$.log('Media URL one is not empty');
                var pos = mimetypeOne.lastIndexOf("/");
                var fileType = mimetypeOne.substring(0, pos);
                if (fileType === 'audio')
                {
		    $.log('File type is audio, setting up an audio player');
                    Opencast.Player.setVideoSizeList(AUDIOPLAYER);
                }
                else
                {
		    $.log('File type is not audio, setting up a single player');
                    Opencast.Player.setVideoSizeList(SINGLEPLAYER);
                }
            }
            Opencast.Initialize.setMediaResolution(mediaResolutionOne, mediaResolutionTwo);
        } else
        {
            if (mediaPackageIdAvailable)
            {
                // Initialize the Segements
                Opencast.segments.initialize();
                slideLength = Opencast.segments.getSlideLength();
            }
            else
            {
                slideLength = 0;
            }
            Opencast.Player.setMediaURL(coverUrlOne, coverUrlTwo, mediaUrlOne, mediaUrlTwo, mimetypeOne, mimetypeTwo, PLAYERSTYLE, slideLength, bufferTime);
            if (mediaUrlOne !== '' && mediaUrlTwo !== '')
            {
		if(displayOneVideo)
		{
                    Opencast.Initialize.setMediaResolution(mediaResolutionOne, mediaResolutionTwo);
		    Opencast.Player.setVideoSizeList(SINGLEPLAYERWITHSLIDES);
		    Opencast.Player.videoSizeControlMultiOnlyLeftDisplay();
		} else
		{
		    Opencast.Player.setVideoSizeList(SINGLEPLAYERWITHSLIDES);
		    $('#oc_player_video-dropdown').append('<input id="oc_btn-centerDisplay" class="oc_btn-centerDisplay" type="image" src="../../img/misc/space.png" name="show_presenter_and_presentation_equal" alt="Show presenter and presentation equal" title="Show presenter and presentation equal"  onclick="Opencast.Player.videoSizeControlMultiDisplay();" onfocus="Opencast.Initialize.dropdownVideo_open();" onblur="Opencast.Initialize.dropdown_timer();" /><br/>');
                    Opencast.Initialize.setMediaResolution(mediaResolutionOne, mediaResolutionTwo);
		}
            }
            else if (mediaUrlOne !== '' && mediaUrlTwo === '')
            {
                var pos = mediaUrlOne.lastIndexOf(".");
                var fileType = mediaUrlOne.substring(pos + 1);
                if (fileType === 'mp3')
                {
                    Opencast.Player.setVideoSizeList(AUDIOPLAYER);
                }
                else
                {
                    Opencast.Initialize.setMediaResolution(mediaResolutionOne, mediaResolutionTwo);
                    Opencast.Player.setVideoSizeList(SINGLEPLAYER);
                }
            }
            Opencast.Initialize.doResize();
        }
        // Set the caption
        // oc-captions using caption file generated by Opencaps
        var captionsUrl = $('#oc-captions').html();
        captionsUrl = captionsUrl === null ? '' : captionsUrl;
        Opencast.Player.setCaptionsURL(captionsUrl);
        // init the volume scrubber
        Opencast.Scrubber.init();
        // bind handler
        $('#scrubber').bind('keydown', 'left', function (evt)
        {
            Opencast.Player.doRewind();
        });
        $('#scrubber').bind('keyup', 'left', function (evt)
        {
            Opencast.Player.stopRewind();
        });
        $('#scrubber').bind('keydown', 'right', function (evt)
        {
            Opencast.Player.doFastForward();
        });
        $('#scrubber').bind('keyup', 'right', function (evt)
        {
            Opencast.Player.stopFastForward();
        });
        if(advancedPlayer)
        {
            // init the search
            Opencast.search.initialize();
            Opencast.Bookmarks.initialize();
        }
        getClientShortcuts();
        if(window.location.href.indexOf('/admin/embed') === -1) {
          Opencast.download.showLinks();
        }
        if(advancedPlayer)
        {
            // init
            Opencast.Initialize.init();
            // Segments Text View
            $('.segments-time').each(function ()
            {
                var seconds = $(this).html();
                $(this).html($.formatSeconds(seconds));
            });
            // Hide loading indicators
            if (parseQueryString(window.location.search.substring(1)).embed)
            {
                $('#oc_title-bar').hide();
                $('#oc_btn-embed').hide();
                $('#oc_slidetext').addClass('scroll');
            }
        }

        // Hide loading indicators
        $('#oc_flash-player-loading').hide();
        // Show video controls and data
        $('#data').show();
        if(advancedPlayer)
        {
            $('#oc_player-head-right').show();
            $('#oc_ui_tabs').show();
        $('#oc_video-player-controls').show();
        } else
        {
            $('#oc_video-time').show();
            $('#oc_sound').show();
        $('#oc_video-controls').show();
        }
        // Set Duration
        var durDiv = $('#dc-extent').html();
        if ((durDiv !== undefined) && (durDiv !== null) && (durDiv != ''))
        {
            duration = parseInt(parseInt(durDiv) / 1000);
            if ((!isNaN(duration)) && (duration > 0))
            {
                Opencast.Player.setDuration(duration);
            }
        }
        if(!advancedPlayer)
        {
            // adjust the slider height
            if(!(Opencast.segments.getNumberOfSegments() > 0))
            {
                $('.progress-list').height("6px");
            }
        }
        var formattedSecs = $.formatSeconds(Opencast.Player.getDuration());
        Opencast.Player.setTotalTime(formattedSecs);

        $.log("Media duration: " + formattedSecs);

        // Give the player a second to finish loading, then proceed
        setTimeout(function()
        {
            Opencast.Watch.durationSet();
        }, 1000);

        // Opencast.ariaSpinbutton.initialize has to be called after #oc_video-player-controls is visible!
        Opencast.ariaSpinbutton.initialize('oc_volume-container', 'oc_volume-back', 'oc_volume-front', 8, 0, 100, true);

	window.setTimeout(function(){
	    $('#oc_btn-play-pause').click();
	}, 100);
	window.setTimeout(function(){
	    $('#oc_btn-play-pause').click();
	}, 500);
    }

    /**
     * @memberOf Opencast.Watch
     * @description Checks and executes the URL Parameters 't' and 'play'
     *              Callback if duration time has been set
     */
    function durationSet()
    {
        if(!durationSetSuccessfully)
        {
            var playParam = $.getURLParameter('play');
            var timeParam = $.getURLParameter('t');
            var previewParam = $.getURLParameter('preview');
            previewParam = previewParam == null ? false : true;
            var durationStr = $('#oc_duration').text();
            var durTextSet = (durationStr != 'Initializing') && ($.getTimeInMilliseconds(durationStr) != 0);
            var autoplay = (playParam !== null) && (playParam.toLowerCase() == 'true');
            if(!advancedPlayer)
            {
                autoplay = ((playParam !== null) && (playParam.toLowerCase() == 'true')) || (!mediaPackageIdAvailable && !previewParam);
            }
            var time = (timeParam === null) ? 0 : $.parseSeconds(timeParam);
            time = (time < 0) ? 0 : time;
            var rdy = false;
            if(advancedPlayer)
            {
                // duration set
                if (durTextSet)
                {
                    // autoplay and jump to time OR autoplay and not jump to time
                    if (autoplay)
                    {
                        // attention: first call 'play', after that 'jumpToTime', otherwise nothing happens!
                        if (Opencast.Player.doPlay() && jumpToTime(time))
                        {
                            $.log("Autoplay: true");
                            rdy = true;
                        }
                    }
                    // not autoplay and jump to time
                    else
                    {
                        if (jumpToTime(time))
                        {
                            $.log("Autoplay: false");
                            rdy = true;
                        }
                    }
                }
                else
                {
                    rdy = false;
                }
            } else
            {
                // duration set
                if (durTextSet||!mediaPackageIdAvailable)
                {
                    // autoplay and jump to time OR autoplay and not jump to time
                    if (autoplay)
                    {
                        // attention: first call 'play', after that 'jumpToTime', otherwise nothing happens!
                        if (Opencast.Player.doPlay() && jumpToTime(time))
                        {
                            $.log("Autoplay: true");
                            rdy = true;
                        }
                    }
                    // not autoplay and jump to time
                    else
                    {
                        if(previewParam) {
                           Opencast.Player.doPause();
                        }
                        if (jumpToTime(time))
                        {
                            $.log("Autoplay: false");
                            rdy = true;
                        }
                    }
                }
                else
                {
                    rdy = false;
                }
            }
            if (!rdy)
            {
                // If duration time not set, yet: set a timeout and call again
                setTimeout(function ()
                {
                    Opencast.Watch.durationSet();
                }, timeoutTime);
            } else
            {
                durationSetSuccessfully = true;
            }
        }
    }

    /**
     * @memberOf Opencast.Watch
     * @description tries to jump to a given time
     * @return true if successfully jumped, false else
     */
    function jumpToTime(time)
    {
        if(time > 0)
        {
            $.log("Jump to time: true (" + time +"s)");
            var seekSuccessful = Videodisplay.seek(time);
            return seekSuccessful;
        } else
        {
            $.log("Jump to time: false");
            return true;
        }
    }

    /**
     * @memberOf Opencast.Watch
     * @description Seeks the video to the passed position. Is called when the
     *              user clicks on a segment
     * @param int
     *          seconds the position in the video
     */
    function seekSegment(seconds)
    {
        var eventSeek = Videodisplay.seek(seconds);
        Opencast.Player.addEvent(Opencast.logging.SEEK_SEGMENT);
    }

    /**
     * @memberOf Opencast.Watch
     * @description Gets the OS-specific shortcuts of the client
     */
    function getClientShortcuts()
    {
        $('#oc_client_shortcuts').append("<span tabindex=\"0\">Control + Alt + I = Toggle the keyboard shortcuts information between show or hide.</span><br/>");
        $('#oc_client_shortcuts').append("<span tabindex=\"0\">Control + Alt + P = Toggle the video between pause or play.</span><br/>");
        $('#oc_client_shortcuts').append("<span tabindex=\"0\">Control + Alt + S = Stop the video.</span><br/>");
        $('#oc_client_shortcuts').append("<span tabindex=\"0\">Control + Alt + M = Toggle between mute or unmute the video.</span><br/>");
        $('#oc_client_shortcuts').append("<span tabindex=\"0\">Control + Alt + U = Volume up</span><br/>");
        $('#oc_client_shortcuts').append("<span tabindex=\"0\">Control + Alt + D = Volume down</span><br/>");
        $('#oc_client_shortcuts').append("<span tabindex=\"0\">Control + Alt 0 - 9 = Seek the time slider</span><br/>");
        $('#oc_client_shortcuts').append("<span tabindex=\"0\">Control + Alt + C = Toggle between captions on or off.</span><br/>");
        $('#oc_client_shortcuts').append("<span tabindex=\"0\">Control + Alt + F = Forward the video.</span><br/>");
        $('#oc_client_shortcuts').append("<span tabindex=\"0\">Control + Alt + R = Rewind the video.</span><br/>");
        $('#oc_client_shortcuts').append("<span tabindex=\"0\">Control + Alt + T = Reads the current time aloud when using a screen reader</span><br/>");
        switch ($.client.os)
        {
        case "Windows":
            $('#oc_client_shortcuts').append("<span tabindex=\"0\">Windows Control + = to zoom in the player</span><br/>");
            $('#oc_client_shortcuts').append("<span tabindex=\"0\">Windows Control - = to minimize in the player</span><br/>");
            break;
        case "Mac":
            $('#oc_client_shortcuts').append("<span tabindex=\"0\">cmd + = Zoom into the player</span><br/>");
            $('#oc_client_shortcuts').append("<span tabindex=\"0\">cmd - = Zoom out of the player</span><br/>");
            break;
        case "Linux":
            break;
        }
		$('#oc_client_shortcuts').append('<a href="javascript: " id="oc_btn-leave_shortcut" onclick="$(\'#oc_shortcut-button\').trigger(\'click\');" class="handcursor" title="Leave shortcut dialog" role="button">Leave shortcut dialog</a>');
    }

    /**
     * Get the url parameter with the given name
     * @param  {String} name The name of the url parameter
     * @return {String}      The value of the url parameter
     */
    function getURLParameter(name) {
        return decodeURIComponent((new RegExp('[?|&]' + name + '=' + '([^&;]+?)(&|#|;|$)').exec(location.search)||[,""])[1].replace(/\+/g, '%20'))||null
    }

    return {
        getAnalyticsURL: getAnalyticsURL,
        getAnnotationURL: getAnnotationURL,
        getAnnotationCommentURL : getAnnotationCommentURL,
        getDescriptionEpisodeURL: getDescriptionEpisodeURL,
        getDescriptionStatsURL: getDescriptionStatsURL,
        getSearchURL: getSearchURL,
        getSegmentsTextURL:getSegmentsTextURL,
        getSegmentsUIURL: getSegmentsUIURL,
        getSegmentsURL: getSegmentsURL,
        getSeriesSeriesURL: getSeriesSeriesURL,
        getSeriesEpisodeURL: getSeriesEpisodeURL,
        onPlayerReady: onPlayerReady,
        seekSegment: seekSegment,
        continueProcessing: continueProcessing,
        durationSet: durationSet,
        getClientShortcuts: getClientShortcuts
    };
}());
