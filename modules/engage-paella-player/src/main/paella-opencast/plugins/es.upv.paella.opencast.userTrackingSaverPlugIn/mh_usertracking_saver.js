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
paella.addPlugin(function() {
  return class userTrackingSaverPlugIn extends paella.userTracking.SaverPlugIn{
    getName() { return 'es.upv.paella.opencast.userTrackingSaverPlugIn'; }

    checkEnabled(onSuccess) {
      paella.ajax.get({url:'/usertracking/detailenabled'},
        function(data, contentType, returnCode) {
          if (data == 'true') {
            onSuccess(true);
          }
          else {
            onSuccess(false);
          }
        },
        function(data, contentType, returnCode) {
          onSuccess(false);
        }
      );
    }

    log(event, params) {
      paella.player.videoContainer.currentTime().then(function(ct){
        var videoCurrentTime = parseInt(ct + paella.player.videoContainer.trimStart());
        var opencastLog = {
          _method: 'PUT',
          'id': paella.player.videoIdentifier,
          'type': undefined,
          'in': videoCurrentTime,
          'out': videoCurrentTime,
          'playing': !paella.player.videoContainer.paused()
        };

        switch (event) {
        case paella.events.play:
          opencastLog.type = 'PLAY';
          break;
        case paella.events.pause:
          opencastLog.type = 'PAUSE';
          break;
        case paella.events.seekTo:
        case paella.events.seekToTime:
          opencastLog.type = 'SEEK';
          break;
        case paella.events.resize:
          opencastLog.type = 'RESIZE-TO-' + params.width + 'x' + params.height;
          break;
        case 'paella:searchService:search':
          opencastLog.type = 'SEARCH-' + params;
          break;
        default:
          opencastLog.type = event;
          var opt = params;
          if (opt != undefined) {
            if (typeof(params) == 'object') {
              opt = JSON.stringify(params);
            }
            opencastLog.type = event + ';' + opt;
          }
          break;
        }
        opencastLog.type = opencastLog.type.substr(0, 128);
        paella.ajax.get( {url: '/usertracking/', params: opencastLog});
      });
    }
  };
});
