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

class DownloadTrack {
  constructor(ocTrack) {
    if (ocTrack.audio == undefined && ocTrack.video == undefined) {
      throw 'No audio and video';
    }
    this.flavor = ocTrack.type;
    this.mimetype = paella.dictionary.translate(ocTrack.mimetype);
    this.url = ocTrack.url;
    if (ocTrack.audio) {
      this.type = 'audio';
      this.audioBitrate = ocTrack.audio.bitrate;
    }
    if (ocTrack.video) {
      this.type = 'video';
      this.videoResolution = ocTrack.video.resolution;
      this.videoFramerate = ocTrack.video.framerate;
    }
  }
}

/*global Class*/
paella.addPlugin(function() {
  return class DownloadsButtonPlugin extends paella.ButtonPlugin {

    constructor() {
      super();
      this._tags = [];
      this._flavors = [];
      this._tagsAndFlavors = false;
      this._episode = undefined;
      this._tracks = [];
    }

    getSubclass() { return 'downloadsButton'; }
    getName() { return 'es.upv.paella.opencast.downloadsButtonPlugin'; }
    getIndex() { return 1000; }
    getDefaultToolTip() { return paella.dictionary.translate('Downloads'); }
    getAlignment() { return 'right'; }
    getButtonType() { return paella.ButtonPlugin.type.popUpButton; }
    getIconClass() { return 'icon-folder-download'; }

    setup() {
      var self = this;
      if (self.config) {
        self._tags = [];
        if (self.config.tags) {
          self._tags = self.config.tags;
        }
        self._flavors = [];
        if (self.config.flavors) {
          self._flavors = self.config.flavors;
        }
        self._tagsAndFlavors = false;
        if (self.config.tagsAndFlavors) {
          self._tagsAndFlavors = self.config.tagsAndFlavors;
        }
      }
    }

    checkEnabled(onSuccess) {
      var self = this;
      if (paella.player.isLiveStream()) {
        onSuccess(false);
      } else {
        paella.opencast.getEpisode()
        .then(function(episode) {
          self._episode = episode;
          // we can't filter tracks by tags and flavors
          // because the setup function will be called after checkEnabled
          onSuccess(true);
        })
        .catch(function(error) {
          base.log.error('opencast downloads button plugin disabled: ' + error);
          onSuccess(false);
        });
      }
    }

    buildContent(domElement) {
      var self = this;
      if (self._episode !== undefined) {
        self.loadTracksFromEpisode();
      }
      self.renderTracks(domElement);
    }

    loadTracksFromEpisode() {
      var self = this;
      var tracks = self._episode.mediapackage.media.track;
      if (!(tracks instanceof Array)) { tracks = [tracks]; }

      var parsedTracks = [];
      for (const track of tracks) {
        // only show static file content over http
        // skip streaming tracks
        if (track.url.startsWith('http')
            && (track.mimetype.indexOf('video') >= 0) || track.mimetype.indexOf('audio') >= 0) {

          // skip live events
          if (track.live != undefined) {
            if (track.live) { continue; }
          }

          // skip and tags
          if (self._tags.length > 0 && (track.tags == undefined || track.tags.tag === undefined
                || track.tags.tag.length < 1)) {
            continue;
          }
          var tagsMatched = self._tags.find(tag => track.tags.tag.indexOf(tag) >= 0) !== undefined;
          // skip by flavor
          var flavorMatched = self._flavors.find(flavor => flavor == track.type) !== undefined;
          if (self._tags.length > 0 && self._flavors.length > 0) {
            if (self._tagsAndFlavors) {
              if (!(tagsMatched && flavorMatched)) { continue; }
            } else {
              if (!(tagsMatched || flavorMatched)) { continue; }
            }
          } else {
            if (self._tags.length > 0 && !tagsMatched) { continue; }
            if (self._flavors.length > 0 && !flavorMatched) { continue; }
          }

          var parsedTrack = new DownloadTrack(track);
          parsedTracks.push(parsedTrack);
        }
      }
      self._tracks = parsedTracks;
    }

    renderTracks(domElement) {
      var self = this;
      let container = document.createElement('div');
      container.className = 'downloadsButtonContainer';

      if (self._episode === undefined || self._tracks.length < 1) {
        container.innerHTML = '<div class="downloadsItemContainer"><p class="noTracks">'
                                + paella.dictionary.translate('No downloads available') + '</p></div';
      } else {
        let trackTypes = ['video', 'audio'];
        let flavors = new Set(self._tracks.map(track => track.flavor));

        // group by flavor
        for (const flavor of flavors) {
          let flavorContainer = document.createElement('div');
          flavorContainer.className = 'downloadsButtonItemContainer';

          let flavorNameContainer = document.createElement('p');
          flavorNameContainer.innerText = flavor;
          flavorContainer.appendChild(flavorNameContainer);

          // order by type (audio or video)
          for (const trackType of trackTypes) {
            let tracks = self._tracks.filter(_track => _track.type === trackType);

            // order by mimetype
            let trackMimetypes = Array.from(new Set(tracks.map(_track => _track.mimetype)));
            trackMimetypes.sort();

            // order by video resolution or audio bitrate
            for (const mimetype of trackMimetypes) {
              let filteredTracks = tracks.filter(_track => _track.flavor === flavor)
                .filter(track => track.mimetype === mimetype);
              filteredTracks.sort(function(t1, t2) {
                if (t1.type === t2.type) {
                  if ('video' === t1.type) {
                    let t1Height = t1.videoResolution.split('x')[1];
                    let t2Height = t2.videoResolution.split('x')[1];
                    return parseInt(t2Height) - parseInt(t1Height);
                  } else if ('audio' === t1.type) {
                    return parseInt(t2.audioBitrate) - parseInt(t1.audioBitrate);
                  }
                }
                // this case shouldn't happen
                return 0;
              });

              // render download links
              for (const track of filteredTracks) {
                let a = document.createElement('a');
                a.href = track.url;
                a.setAttribute('download', '');

                if ('video' === track.type) {
                  let videoFormatElement = document.createElement('span');
                  videoFormatElement.className = 'downloadsButtonItemLinkSpanFormat';
                  videoFormatElement.innerText = `[${paella.dictionary.translate(mimetype)}]`;
                  a.appendChild(videoFormatElement);

                  let videoResolutionElement = document.createElement('span');
                  videoResolutionElement.className = 'downloadsButtonItemLinkSpanResolution';
                  videoResolutionElement.innerText = `${track.videoResolution}@${track.videoFramerate}`;
                  a.appendChild(videoResolutionElement);
                } else if ('audio' === track.type) {
                  let audioFormatElement = document.createElement('span');
                  audioFormatElement.className = 'downloadsButtonItemLinkSpanFormat';
                  audioFormatElement.innerText = `[${paella.dictionary.translate(mimetype)}]`;
                  a.appendChild(audioFormatElement);

                  let audioBitrateElement = document.createElement('span');
                  audioBitrateElement.className = 'downloadsButtonItemLinkSpanAudioBitrate';
                  audioBitrateElement.innerText = `${Math.floor(track.audioBitrate / 1000)}kbps`;
                  a.appendChild(audioBitrateElement);
                }
                flavorContainer.appendChild(a);
              }
            }
            container.appendChild(flavorContainer);
          }
        }
      }
      domElement.appendChild(container);
    }
  };
});
