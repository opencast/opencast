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
  return class DownloadsPlugin extends paella.TabBarPlugin {
    getSubclass() { return 'downloadsTabBar'; }
    getName() { return 'es.upv.paella.opencast.downloadsPlugin'; }
    getTabName() { return paella.dictionary.translate('Downloads'); }
    getIndex() { return 30; }
    getDefaultToolTip() { return paella.dictionary.translate('Downloads'); }


    get domElement() { return this._domElement; }
    set domElement(v) { this._domElement = v; }

    checkEnabled(onSuccess) {
      var self = this;
      paella.opencast.getEpisode()
      .then(
        function(episode) {
          self._episode = episode;
          onSuccess(true);
        },
        function() { onSuccess(false); }
      );
    }

    buildContent(domElement) {
      this.domElement = domElement;
      this.loadContent();
    }

    action(tab) {
    }

    loadContent() {
      var self = this;
      var container = document.createElement('div');
      container.className = 'downloadsTabBarContainer';


      var tracks = self._episode.mediapackage.media.track;
      if (!(tracks instanceof Array)) { tracks = [tracks]; }

      for (var i = 0; i < tracks.length; ++i) {
        var track = tracks[i];
        var download = false;
        if (track.tags != undefined && track.tags.tag != undefined
            && track.mimetype.indexOf('video') >= 0
            && track.url.indexOf('rtmp://') < 0) {
          for (var j = 0; j < track.tags.tag.length; j++) {
            if (track.tags.tag[j] === 'engage-download') {
              download = true;
              break;
            }
          }
        }
        if (download) {
          paella.debug.log(track.type);
          container.appendChild(this.createLink(track, i));
        }
      }
      this.domElement.appendChild(container);
    }

    createLink(track, tabindexcount) {
      var elem = document.createElement('div');
      elem.className = 'downloadsLinkContainer';
      var link = document.createElement('a');
      link.className = 'downloadsLinkItem';
      link.innerHTML = this.getTextInfo(track);
      link.setAttribute('tabindex', 4000+tabindexcount);
      link.href = track.url;

      elem.appendChild(link);

      return elem;
    }

    getTextInfo(track){
      var text = '';

      if (track.video) {
        text = '<span class="downloadLinkText TypeFile Video">' + paella.dictionary.translate('Video file') + '</span>';
      }
      else if (track.audio){
        text = '<span class="downloadLinkText TypeFile Audio">' + paella.dictionary.translate('Audio file') + '</span>';
      }
      // track
      var trackText= '<span class="downloadLinkText Track">' + track.type + '</span>';

      // Resolution
      var resolution = '';
      if (track.video) {
        if ( track.video.resolution){
          resolution = track.video.resolution;
        }
        if (track.video.framerate){
          resolution +=  '@' + track.video.framerate + 'fps';
        }
      }

      // mimetype
      var mimetype = '';
      if (track.mimetype) {
        mimetype = track.mimetype;
      }

      if (mimetype)
        text += ' <span class="downloadLinkText MIMEType">[' + paella.dictionary.translate(mimetype) + ']' + '</span>';
      text += ': ' + trackText;
      if (resolution)
        text += ' <span class="downloadLinkText Resolution">(' + resolution + ')' + '</span>';

      return text;
    }
  };
});

