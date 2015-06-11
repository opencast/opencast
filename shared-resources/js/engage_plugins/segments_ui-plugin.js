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
 * @namespace the global Opencast namespace segments_ui_Plugin
 */
Opencast.segments_ui_Plugin = (function ()
{
    // The Templates to process
    var templateSegments1 = '<tr>' +
                            '{for s in segment}' +
                                '{if (parseInt(s.duration) > 0)}' +
                                    '<td role="button" class="segment-holder ui-widget ui-widget-content" ' +
                                        'id="segment${s.index}" ' +
                                        'onmouseover="Opencast.segments_ui.hoverSegment(${parseInt(s.hoverSegmentIndex)}, ${parseInt(s.index)})" ' +
                                        'onmouseout="Opencast.segments_ui.hoverOutSegment(${parseInt(s.index)})" ' +
                                        'alt="Slide ${parseInt(s.index) + 1} of ${segment.length}" ' +
                                        'onclick="Opencast.Watch.seekSegment(${parseInt(s.time) / 1000})" ' +
                                        'style="width: ${parseInt(s.duration) / parseInt(s.completeDuration) * 100}%;" ' +
                                     '>' +
                                        '<span class="segments-time" style="display: none">${parseInt(s.time) / 1000}</span>' +
                                     '</td>' +
                                 '{/if}' +
                             '{forelse}' +
                                 '<td style="width: 100%;" id="segment-holder-empty" class="segment-holder" />' +
                             '{/for}' +
                             '</tr>';

    
    var templateMedia1 =     '{for t in track}' +
                                 '{if t.type == "presenter/delivery"}' +
                                     '{if (t.mimetype == "video/x-flv" || t.mimetype == "video/mp4")}' +
                                         '{if (t.url.substring(0, 4) == "http") && (!rtmpAvailable || (rtmpAvailable && !preferStreaming))&& (!checkQuality || (checkQuality && ((quality == t.quality) || (t.quality == ""))))}' +
                                             '<div id="oc-video-presenter-delivery-x-flv-http" style="display: none">' +
                                                 '${t.url}' +
                                             '</div>' +
                                             '<div id="oc-resolution-presenter-delivery-x-flv-http" style="display: none">' +
                                                 '${t.video.resolution}' +
                                             '</div>' +
                                             '<div id="oc-mimetype-presenter-delivery-x-flv-http" style="display: none">' +
                                                 '${t.mimetype}' +
                                             '</div>' +
                                         '{/if}' +
                                     '{/if}' +
                                  '{/if}' +
     
                                  '{if !isVideo && (t.type == "presenter/delivery") && (t.mimetype == "audio/x-adpcm") && (t.url.substring(0, 4) == "http")}' +
                                      '<div id="oc-video-presenter-delivery-x-flv-http" style="display: none">' +
                                          '${t.url}' +
                                      '</div>' +
                                      /*
                                      '{if defined(t.video) && defined(t.video.resolution)}' +
                                          '<div id="oc-resolution-presenter-delivery-x-flv-http" style="display: none">' +
                                              '${t.video.resolution}' +
                                          '</div>' +
                                      '{/if}' +
                                      */
                                      '<div id="oc-mimetype-presenter-delivery-x-flv-http" style="display: none">' +
                                          '${t.mimetype}' +
                                      '</div>' +
                                  '{/if}' +
                                  
                                 '{if t.type == "presentation/delivery"}' +
                                     '{if (t.mimetype == "video/x-flv" || t.mimetype == "video/mp4")}' +
                                         '{if (t.url.substring(0, 4) == "http") && (!rtmpAvailable || (rtmpAvailable && !preferStreaming))&& (!checkQuality || (checkQuality && ((quality == t.quality) || (t.quality == ""))))}' +
                                             '<div id="oc-video-presentation-delivery-x-flv-http" style="display: none">' +
                                                 '${t.url}' +
                                             '</div>' +
                                             '<div id="oc-resolution-presentation-delivery-x-flv-http" style="display: none">' +
                                                 '${t.video.resolution}' +
                                             '</div>' +
                                             '<div id="oc-mimetype-presentation-delivery-x-flv-http" style="display: none">' +
                                                 '${t.mimetype}' +
                                             '</div>' +
                                         '{/if}' +
                                     '{/if}' +
                                  '{/if}' +
         
                                  '{if t.type == "presenter/delivery"}' +
                                      '{if (t.mimetype == "video/x-flv" || t.mimetype == "video/mp4")}' +
                                          '{if (t.url.substring(0, 4) == "rtmp") && (rtmpAvailable && preferStreaming) && (!checkQuality || (checkQuality && ((quality == t.quality) || (t.quality == ""))))}' +
                                              '<div id="oc-video-presenter-delivery-x-flv-rtmp" style="display: none">' +
                                                  '${t.url}' +
                                              '</div>' +
                                              '<div id="oc-resolution-presenter-delivery-x-flv-rtmp" style="display: none">' +
                                                  '${t.video.resolution}' +
                                              '</div>' +
                                              '<div id="oc-mimetype-presenter-delivery-x-flv-rtmp" style="display: none">' +
                                                  '${t.mimetype}' +
                                              '</div>' +
                                          '{/if}' +
                                      '{/if}' +
                                  '{/if}' +
         
                                  '{if t.type == "presentation/delivery"}' +
                                      '{if (t.mimetype == "video/x-flv" || t.mimetype == "video/mp4")}' +
                                          '{if (t.url.substring(0, 4) == "rtmp") && (rtmpAvailable && preferStreaming) && (!checkQuality || (checkQuality && ((quality == t.quality) || (t.quality == ""))))}' +
                                              '<div id="oc-video-presentation-delivery-x-flv-rtmp" style="display: none">' +
                                                  '${t.url}' +
                                              '</div>' +
                                              '<div id="oc-resolution-presentation-delivery-x-flv-rtmp" style="display: none">' +
                                                  '${t.video.resolution}' +
                                              '</div>' +
                                              '<div id="oc-mimetype-presentation-delivery-x-flv-rtmp" style="display: none">' +
                                                  '${t.mimetype}' +
                                              '</div>' +
                                          '{/if}' +
                                      '{/if}' +
                                  '{/if}' +
          
                                  '{if t.type == "presenter/source"}' +
                                      '{if (t.mimetype == "video/x-flv" || t.mimetype == "video/mp4")}' +
                                          '{if (t.url.substring(0, 4) == "http") && (!rtmpAvailable || (rtmpAvailable && !preferStreaming)) && (!checkQuality || (checkQuality && ((quality == t.quality) || (t.quality == ""))))}' +
                                              '<div id="oc-video-presenter-source-x-flv-http" style="display: none">' +
                                                  '${t.url}' +
                                              '</div>' +
                                              '<div id="oc-resolution-presenter-source-x-flv-http" style="display: none">' +
                                                  '${t.video.resolution}' +
                                              '</div>' +
                                              '<div id="oc-mimetype-presenter-source-x-flv-http" style="display: none">' +
                                                  '${t.mimetype}' +
                                              '</div>' +
                                          '{/if}' +
                                      '{/if}' +
                                  '{/if}' +
          
                                  '{if t.type == "presentation/source"}' +
                                      '{if (t.mimetype == "video/x-flv" || t.mimetype == "video/mp4")}' +
                                          '{if (t.url.substring(0, 4) == "http") && (!rtmpAvailable || (rtmpAvailable && !preferStreaming)) && (!checkQuality || (checkQuality && ((quality == t.quality) || (t.quality == ""))))}' +
                                              '<div id="oc-video-presentation-source-x-flv-http" style="display: none">' +
                                                  '${t.url}' +
                                              '</div>' +
                                              '<div id="oc-resolution-presentation-source-x-flv-http" style="display: none">' +
                                                  '${t.video.resolution}' +
                                              '</div>' +
                                              '<div id="oc-mimetype-presentation-source-x-flv-http" style="display: none">' +
                                                  '${t.mimetype}' +
                                              '</div>' +
                                          '{/if}' +
                                      '{/if}' +
                                  '{/if}' +
         
                                 '{if t.type == "presenter/source"}' +
                                     '{if (t.mimetype == "video/x-flv" || t.mimetype == "video/mp4")}' +
                                         '{if (t.url.substring(0, 4) == "rtmp") && (rtmpAvailable && preferStreaming) && (!checkQuality || (checkQuality && ((quality == t.quality) || (t.quality == ""))))}' +
                                             '<div id="oc-video-presenter-source-x-flv-rtmp" style="display: none">' +
                                                 '${t.url}' +
                                             '</div>' +
                                             '<div id="oc-resolution-presenter-source-x-flv-rtmp" style="display: none">' +
                                                 '${t.video.resolution}' +
                                             '</div>' +
                                             '<div id="oc-mimetype-presenter-source-x-flv-rtmp" style="display: none">' +
                                                 '${t.mimetype}' +
                                             '</div>' +
                                         '{/if}' +
                                     '{/if}' +
                                  '{/if}' +
         
                                 '{if t.type == "presentation/source"}' +
                                     '{if (t.mimetype == "video/x-flv" || t.mimetype == "video/mp4")}' +
                                         '{if (t.url.substring(0, 4) == "rtmp") && (rtmpAvailable && preferStreaming) && (!checkQuality || (checkQuality && ((quality == t.quality) || (t.quality == ""))))}' +
                                             '<div id="oc-video-presentation-source-x-flv-rtmp" style="display: none">' +
                                                 '${t.url}' +
                                             '</div>' +
                                             '<div id="oc-resolution-presentation-source-x-flv-rtmp" style="display: none">' +
                                                 '${t.video.resolution}' +
                                             '</div>' +
                                             '<div id="oc-mimetype-presentation-source-x-flv-rtmp" style="display: none">' +
                                                 '${t.mimetype}' +
                                             '</div>' +
                                         '{/if}' +
                                     '{/if}' +
                                '{/if}' + 
                             '{forelse}' +
                                 '' +
                             '{/for}';
                                 
    var templateData1 =      '<div id="oc-title" style="display: none">' +
                                 '{if defined("dcTitle")}' +
                                     '${dcTitle}' +
                                 '{else}' +
                                     'No Title' +
                                 '{/if}' +
                             '</div>' +
                             '<div id="dc-extent" style="display: none">' +
                                 '{if defined("dcExtent")}' +
                                     '${dcExtent}' +
                                 '{else}' +
                                     '0' +
                                 '{/if}' +
                             '</div>' +
                             '<div id="oc-creator" style="display: none">' +
                                 '{if defined("dcCreator")}' +
                                     '${dcCreator}' +
                                 '{else}' +
                                     'No Creator' +
                                 '{/if}' +
                             '</div>' +
                             '<div id="oc-date" style="display: none">' +
                                 '{if defined("dcCreated")}' +
                                     '${dcCreated}' +
                                 '{else}' +
                                     '' +
                                 '{/if}' +
                             '</div>';
        
    var templateMPAttach1 =     '{for a in attachment}' +
                                    '{if a.type == "presenter/player+preview"}' +
                                        '<div id="oc-cover-presenter" style="display: none">' +
                                            '${a.url}' +
                                        '</div>' +
                                    '{/if}' +
                                    '{if a.type == "presentation/player+preview"}' +
                                        '<div id="oc-cover-presentation" style="display: none">' +
                                            '${a.url}' +
                                        '</div>' +
                                    '{/if}' +
                                '{forelse}' +
                                    '' +
                                '{/for}';
        
    var templateData2 =      '<div id="dc-subject" style="display: none">' +
                                 '{if defined("dcSubject")}' +
                                     '${dcTitle}' +
                                 '{else}' +
                                     'No Subject' +
                                 '{/if}' +
                             '</div>' +
                             '<div id="dc-contributor" style="display: none">' +
                                 '{if defined("dcContributor")}' +
                                     '${dcContributor}' +
                                 '{else}' +
                                     'No Department information' +
                                 '{/if}' +
                             '</div>' +
                             '<div id="dc-description" style="display: none">' +
                                 '{if defined("dcDescription")}' +
                                     '${dcDescription}' +
                                 '{else}' +
                                     'No Description' +
                                 '{/if}' +
                             '</div>' +
                             '<div id="dc-language" style="display: none">' +
                                 '{if defined("dcLanguage")}' +
                                     '${dcLanguage}' +
                                 '{else}' +
                                     'No Language' +
                                 '{/if}' +
                             '</div>';
        
    var templateMPCatalog1 =    '{for c in catalog}' +
                                    '{if c.type == "captions/timedtext"}' +
                                        '{if c.mimetype == "text/xml"}' +
                                            '<div id="oc-captions" style="display: none">' +
                                                '${c.url}' +
                                            '</div>' +
                                        '{/if}' +
                                    '{/if}' +
                                '{forelse}' +
                                    '' +
                                '{/for}';
    
    var templateSegments2 = '{for s in segment}' +
                                '{if (parseInt(s.duration) > 0)}' +
                                    '<tr>' +
                                        '<td class="oc-segments-preview">' +
                                            '${s.previews.preview.$}' +
                                        '</td>' +
                                        '<td class="oc-segments-time">' +
                                            '<a class="oc_segments-time" ' +
                                                'onclick="Opencast.Watch.seekSegment(${Math.floor(parseInt(s.time) / 1000)})>" ' +
                                            '</a>' +
                                        '</td>' +
                                        '<td>' +
                                            '${s.text}' +
                                        '</td>' +
                                    '</tr>' +
                                '{/if}' +
                            '{forelse}' +
                                '<td style="width: 100%;" id="segment-holder-empty" class="segment-holder" />' +
                            '{/for}';

    // The Elements to put the div into
    var elementSegments1,
        elementMedia1,
        elementData1,
        elementMediaPackage1,
        elementData2,
        elementMediaPackage2,
        elementSegments2;
    
    // Data to process == search-result.results.result
    // 0 = everything
    // 1 = segments -- segment
    // 2 = mediapackage.media -- track
    // 3 = mediapackage.attachments --attachment
    // 4 = mediapackage.metadata -- catalog
    var segments_ui_data,
        segments_ui_dataSegments,
        segments_ui_dataMPMedia,
        segments_ui_dataMPAttach,
        segments_ui_dataMPCatalog;
        
    // Precessed Data
    var processedTemplateData;

    /**
     * @memberOf Opencast.segments_ui_Plugin
     * @description Add As Plug-in
     * @param elemSegments1 First Segments Element
     * @param elemMedia1 First Media Element
     * @param elemData1 First Data Element
     * @param elemMediaPackage1 First Media Package Element
     * @param elemData2 Second Data Element
     * @param elemMediaPackage2 Second Media Package Element
     * @param elemSegments2 Second Segments Element
     * @param data Data to fill the Elements with
     * @param withSegments boolean Flag if parse with Segments or without
     */
    function addAsPlugin(elemSegments1,
                         elemMedia1,
                         elemData1,
                         elemMediaPackage1,
                         elemData2,
                         elemMediaPackage2,
                         elemSegments2,
                         data,
                         withSegments) {
        elementSegments1 = elemSegments1;
        elementMedia1 = elemMedia1;
        elementData1 = elemData1;
        elementMediaPackage1 = elemMediaPackage1;
        elementData2 = elemData2;
        elementMediaPackage2 = elemMediaPackage2;
        elementSegments2 = elemSegments2;

        segments_ui_data = data;
        segments_ui_dataSegments = segments_ui_data.segments;
        segments_ui_dataMPMedia = segments_ui_data.mediapackage.media;
        segments_ui_dataMPAttach = segments_ui_data.mediapackage.attachments;
        segments_ui_dataMPCatalog = segments_ui_data.mediapackage.metadata;
        createSegments(withSegments);
    }

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
     * @memberOf Opencast.segments_ui_Plugin
     * @description Processes the Data and puts it into the Element
     * @param withSegments true if process with Segments, false if without Segments
     */
    function createSegments(withSegments) {
        var cs1 = false,
            cs2 = false,
            cs3 = false,
            cs4 = false,
            cs5 = false,
            cs6 = false,
            cs7 = false;

        var segments, tracks, attachments, catalogs;

            
        // Process Element Segments 1
        if (withSegments && 
            (elementSegments1 !== undefined) && 
            (segments_ui_dataSegments.segment !== undefined) && 
            ((segments = {segment: ensureArray(segments_ui_dataSegments.segment)}).segment.length > 0)) {

              processedTemplateData = templateSegments1.process(segments);
              elementSegments1.html(processedTemplateData);
              cs1 = true;
        }

        // Process Element Media 1
        if ((elementMedia1 !== undefined) && 
            (segments_ui_dataMPMedia.track !== undefined) && 
            ((tracks = {track: ensureArray(segments_ui_dataMPMedia.track)}).track.length > 0)) {

              tracks = $.extend({},segments_ui_dataMPMedia,tracks);
              processedTemplateData = templateMedia1.process(tracks);
              elementMedia1.html(processedTemplateData);
              cs2 = true;
        }

        // Process Element Data 1
        if ((elementData1 !== undefined) && (segments_ui_data !== undefined)) {
            processedTemplateData = templateData1.process(segments_ui_data);
            elementData1.html(processedTemplateData);
            cs3 = true;
        }

        // Process Element MediaPackage 1
        if ((elementMediaPackage1 !== undefined) && 
            (segments_ui_dataMPAttach.attachment !== undefined) && 
            ((attachments = {attachment: ensureArray(segments_ui_dataMPAttach.attachment)}).attachment.length > 0)) {

              processedTemplateData = templateMPAttach1.process(attachments);
              elementMediaPackage1.html(processedTemplateData);
              cs4 = true;
        }

        // Process Element Data 2
        if ((elementData2 !== undefined) && (templateData2 !== undefined)) {
            processedTemplateData = templateData2.process(segments_ui_data);
            elementData2.html(processedTemplateData);
            cs5 = true;
        }

        // Process Element MediaPackage 2
        if ((elementMediaPackage2 !== undefined) && 
            (segments_ui_dataMPCatalog.catalog !== undefined) && 
            ((catalogs = {catalog: ensureArray(segments_ui_dataMPCatalog.catalog)}).catalog.length > 0)) {

              processedTemplateData = templateMPCatalog1.process(catalogs);
              elementMediaPackage2.html(processedTemplateData);
              cs6 = true;
        }

        // Process Element Segments 2
        if (withSegments && (elementSegments2 !== undefined) && 
            (segments_ui_dataSegments.segment !== undefined) && 
            segments && (segments.segment.length > 0)) {

              processedTemplateData = templateSegments2.process(segments);
              elementSegments2.html(processedTemplateData);
              cs7 = true;
        }
        var tl = '' + (cs1 ? " 1 " : '') + (cs2 ? " 2 " : '') + (cs3 ? " 3 " : '') + (cs4 ? " 4 " : '') + (cs5 ? " 5 " : '') + (cs6 ? " 6 " : '') + (cs7 ? " 7 " : '');
        $.log("Segments UI Plugin: Following Templates have (successfully) been proceeded: " + tl + " from 7 Templates possible");
    }

    return {
        addAsPlugin: addAsPlugin
    };
}());