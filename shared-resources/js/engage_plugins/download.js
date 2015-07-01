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
 * @namespace the global Opencast namespace download
*/
Opencast.download = (function() {

    var that = {};


    /** If obj is an array just returns obj else returns Array with obj as content.
     *  If obj === undefined returns empty Array.
     *
     */
    function ensureArray(obj) {
      if (obj === undefined) return [];
      if ($.isArray(obj)) {
        return obj;
      } else {
        return [obj];
      }
    }

    /**
     * @memberOf Opencast.download
     * @description Shows download links for files
    */

    that.showLinks = function() {

        $.ajax(
            {
            url: Opencast.Watch.getDescriptionEpisodeURL(),
            data: "id=" + Opencast.Player.getMediaPackageId(),
            dataType: 'json',
            success: function(data)
            {
                var i,
                    tracks = data["search-results"] ? ensureArray(data["search-results"]["result"]["mediapackage"]["media"]["track"]) : [],
                    video_files = [],
                    audio_files = [],
                    video_markup = "",
                    audio_markup = "";

                    var buildLinks = function(media){
                        var myHTML = "",
                        media_href,
                        media_type,
                        media_resolution,
                        media_description,
                        j,
                        DISPLAY_DESCRIPTION = [
                           ["1280x720", "High Definition"],
                           ["1024x768", "High Resolution"],
                           ["320x240", "Low Resolution"]
                        ];

                        for( i = 0; i < media.length; i+=1 ){
                            if ( media[i]["tags"]["tag"].indexOf('engage') === -1 ){
                                media_href = media[i].url;
                                media_type = media[i].type;
                                media_type = (media_type === 'presenter/delivery') ? "Presenter " : "Presentation ";
                                media_resolution = media[i].video ? media[i].video.resolution : " ";
                                media_description = " ";

                                for( j = 0; j < DISPLAY_DESCRIPTION.length; j+=1 ){
                                    if (DISPLAY_DESCRIPTION[j][0] === media_resolution){
                                        media_description = DISPLAY_DESCRIPTION[j][1];
                                    }
                                }

                                media_format = media_href.split(".");
                                myHTML += " <a href="+ media_href + ">" +  media_type + " - " + media_description  + " [ File extension : <em>"+  media_format[media_format.length - 1] +"</em> ] " + "</a><br>";
                            }
                        }

                        return myHTML;
                    };

                // Separate Video and Audio files
                for( i = 0; i < tracks.length; i+=1 ){
                    tracks[i].video ? video_files.push(tracks[i]) : audio_files.push(tracks[i]);
                }

                video_markup = buildLinks(video_files);
                audio_markup = buildLinks(audio_files);

                $('#oc_client_downloads').append("<span><b>Video Files</b></span><br>");
                $('#oc_client_downloads').append("<div id=\"oc_download_video\"></div>");
                (video_files.length > 0 && video_markup !== "") ?  $('div#oc_download_video').html(video_markup) : $('div#oc_download_video').html("No video files available for download.");

                $('#oc_client_downloads').append("<br><span><b>Audio Files</b></span><br>");
                $('#oc_client_downloads').append("<div id=\"oc_download_audio\"></div>");
                (audio_files.length > 0 && audio_markup !== "") ? $('div#oc_download_audio').html(audio_markup) : $('div#oc_download_audio').html("No audio files available for download.");

                $('#oc_client_downloads').append("<br><span><b>Please Note :</b></span><br>");
                $('#oc_client_downloads').append("<span>Some files may not download automatically, you may need to \"Right click\" the download link and select \"Save Link As..\" to save the file. In some browsers where the \"Right click\" is disabled - e.g. Firefox 8 on Mac OS X - you may have to press ALT and then click the download link to prompt a download.</span><br>");

            },
            error: function(a, b, c){}
        });
    };

    return that;
}());


