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
        mediaPackageIdAvailable = true,
        durationSetSuccessfully = false,
        mediaPackageId;
        
    var analyticsURL = "",
        annotationURL = "",
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
        $.getJSON('js/servicedata.json', function(data)
        {
            analyticsURL = data.plugin_urls.analytics;
            annotationURL = data.plugin_urls.annotation;
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
            $.log("Description (Episode) URL: " + descriptionEpisodeURL);
            $.log("Description (Stats) URL: " + descriptionStatsURL);
            $.log("Search URL: " + searchURL);
            $.log("Segments (Text) URL: " + segmentsTextURL);
            $.log("Segments (UI) URL: " + segmentsUIURL);
            $.log("Segments URL: " + segmentsURL);
            $.log("Series (Series) URL: " + seriesSeriesURL);
            $.log("Series (Episode) URL: " + seriesEpisodeURL);
            
            mediaPackageId = (data.mediaDebugInfo.mediaPackageId == "") ? $.getURLParameter('id') : data.mediaDebugInfo.mediaPackageId;
            mediaUrlOne = (data.mediaDebugInfo.mediaUrlOne == "") ? null : data.mediaDebugInfo.mediaUrlOne;
            mediaUrlTwo = (data.mediaDebugInfo.mediaUrlTwo == "") ? null : data.mediaDebugInfo.mediaUrlTwo;
            mediaResolutionOne = (data.mediaDebugInfo.mediaResolutionOne == "") ? null : data.mediaDebugInfo.mediaResolutionOne;
            mediaResolutionTwo = (data.mediaDebugInfo.mediaResolutionTwo == "") ? null : data.mediaDebugInfo.mediaResolutionTwo;
            mimetypeOne = (data.mediaDebugInfo.mimetypeOne == "") ? null : data.mediaDebugInfo.mimetypeOne;
            mimetypeTwo = (data.mediaDebugInfo.mimetypeTwo == "") ? null : data.mediaDebugInfo.mimetypeTwo;
            
            $.log("Media Debug Info");
            $.log("Mediapackage ID: " + mediaPackageId);
            $.log("Media URL 1: " + mediaUrlOne);
            $.log("Media URL 2: " + mediaUrlTwo);
            $.log("Media resolution 1: " + mediaResolutionOne);
            $.log("Media resolution 1: " + mediaResolutionTwo);
            $.log("Mimetype 1: " + mimetypeOne);
            $.log("Mimetype 2: " + mimetypeTwo);
            
            if(advancedPlayer)
            {
                // Hide Screen Settings until clicked 'play'
                $("#oc_btn-dropdown").css("display", 'none');
                $("#oc_player_video-dropdown").css("display", 'none');
                var userId = $.getURLParameter('user');
                var restEndpoint = Opencast.engage.getSearchServiceEpisodeIdURL() + mediaPackageId;
                Opencast.Player.setSessionId(Opencast.engage.getCookie("JSESSIONID"));
                Opencast.Player.setUserId(userId);
                // Set MediaPackage ID's in the Plugins
                Opencast.Player.setMediaPackageId(mediaPackageId);
                Opencast.Annotation_Chapter.setMediaPackageId(mediaPackageId);
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
                $('body').css('background-color', '#FFFFFF');
                $('body').html('<span id="initializing-matter">matter</span><span id="initializing-horn">horn</span><span id="initializing">&nbsp;The media is not available.</span>');
                $('#initializing').css('color', '#000000');
                return;
            }
        }
        if(advancedPlayer)
        {
            // set the title of the page
            document.title = $('#oc-title').html() + " | Opencast Matterhorn - Media Player";
            var dcExtent = parseInt($('#dc-extent').html());
            Opencast.Analytics.setDuration(parseInt(parseInt(dcExtent) / 1000));
            Opencast.Analytics.initialize();
            Opencast.Annotation_Chapter.setDuration(parseInt(parseInt(dcExtent) / 1000));
            Opencast.Annotation_Chapter.initialize();
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
        if(mediaUrlTmp != null)
        {
            $.log('Set Video URL 1 manually');
        }
        mediaUrlTmp = $.getURLParameter('videoUrl2');
        mediaUrlTwo = (mediaUrlTmp == null) ? mediaUrlTwo : mediaUrlTmp;
        if(mediaUrlTmp != null)
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
        
        $.log("Final Mediadata");
        $.log("Mediapackage ID: " + mediaPackageId);
        $.log("Media URL 1: " + mediaUrlOne);
        $.log("Media URL 2: " + mediaUrlTwo);
        $.log("Media resolution 1: " + mediaResolutionOne);
        $.log("Media resolution 1: " + mediaResolutionTwo);
        $.log("Mimetype 1: " + mimetypeOne);
        $.log("Mimetype 2: " + mimetypeTwo);
        
        if(advancedPlayer)
        {
            // init the segements
            Opencast.segments.initialize();
            // init the segements_text
            Opencast.segments_text.initialize();
            slideLength = Opencast.segments.getSlideLength();
            Opencast.Player.setMediaURL(coverUrlOne, coverUrlTwo, mediaUrlOne, mediaUrlTwo, mimetypeOne, mimetypeTwo, PLAYERSTYLE, slideLength);
            if (mediaUrlOne !== '' && mediaUrlTwo !== '')
            {
                Opencast.Player.setVideoSizeList(MULTIPLAYER);
            }
            else if (mediaUrlOne !== '' && mediaUrlTwo === '')
            {
                var pos = mimetypeOne.lastIndexOf("/");
                var fileType = mimetypeOne.substring(0, pos);
                if (fileType === 'audio')
                {
                    Opencast.Player.setVideoSizeList(AUDIOPLAYER);
                }
                else
                {
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
            Opencast.Player.setMediaURL(coverUrlOne, coverUrlTwo, mediaUrlOne, mediaUrlTwo, mimetypeOne, mimetypeTwo, PLAYERSTYLE, slideLength);
            if (mediaUrlOne !== '' && mediaUrlTwo !== '')
            {
                Opencast.Initialize.setMediaResolution(mediaResolutionOne, mediaResolutionTwo);
                Opencast.Player.setVideoSizeList(SINGLEPLAYERWITHSLIDES);
                Opencast.Player.videoSizeControlMultiOnlyLeftDisplay();
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

        if(advancedPlayer)
        {
            $('#oc_player_video-dropdown').hide();
        }
        // Opencast.ariaSpinbutton.initialize has to be called after #oc_video-player-controls is visible!
        Opencast.ariaSpinbutton.initialize('oc_volume-container', 'oc_volume-back', 'oc_volume-front', 8, 0, 100, true);
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
        Opencast.Player.addEvent("SEEK-SEGMENT");
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
        $('#oc_client_shortcuts').append("<span tabindex=\"0\">Control + Alt + T = the current time for the screen reader</span><br/>");
        $('#oc_client_shortcuts').append('<a href="javascript: " id="oc_btn-leave_shortcut" onclick="$(\'#oc_shortcut-button\').trigger(\'click\');" class="handcursor ui-helper-hidden-accessible" title="Leave shortcut dialog" role="button">Leave embed dialog</a>');
        switch ($.client.os)
        {
        case "Windows":
            $('#oc_client_shortcuts').append("Windows Control + = to zoom in the player<br/>");
            $('#oc_client_shortcuts').append("Windows Control - = to minimize in the player<br/>");
            break;
        case "Mac":
            $('#oc_client_shortcuts').append("cmd + = to zoom in the player<br/>");
            $('#oc_client_shortcuts').append("cmd - = to minimize the player<br/>");
            break;
        case "Linux":
            break;
        }
    }
    
    return {
        getAnalyticsURL: getAnalyticsURL,
        getAnnotationURL: getAnnotationURL,
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
