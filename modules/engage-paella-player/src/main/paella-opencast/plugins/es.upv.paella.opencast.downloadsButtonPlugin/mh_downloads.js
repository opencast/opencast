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
  constructor(oc_track) {
    if (oc_track.audio == undefined && oc_track.video == undefined) {
      throw 'No audio and video';
    }
    this.flavor = oc_track.type;
    this.mimetype = paella.dictionary.translate(oc_track.mimetype);
    this.url = oc_track.url;
    if (oc_track.audio) {
      this.type = 'audio';
      this.audio_bitrate = oc_track.audio.bitrate;
    }
    if (oc_track.video) {
      this.type = 'video';
      this.video_resolution = oc_track.video.resolution;
      //this.video_quality = oc_track.video.resolution.split('x', 2)[1] + 'p';
      this.video_framerate = oc_track.video.framerate;
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
      this._tags_and_flavors = false;
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
        self._tags_and_flavors = false;
        if (self.config.tags_and_flavors) {
          self._tags_and_flavors = self.config.tags_and_flavors;
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
      for (var tracksIndex = 0; tracksIndex < tracks.length; tracksIndex++) {
        var track = tracks[tracksIndex];
        // only show static file content over http
        // skip streaming tracks
        if (track.url.startsWith('http')
            && (track.mimetype.indexOf('video') >= 0) || track.mimetype.indexOf('audio') >= 0) {

          // skip live events
          if (track.live != undefined) {
            if (track.live) { continue; }
          }

          // skip and tags
          if (self._tags.length > 0 && (track.tags == undefined || track.tags.tag == undefined
                || track.tags.tag.length < 1)) {
            continue;
          }

          var match_tags = false;
          for (var tagsIndex = 0; tagsIndex < self._tags.length; tagsIndex++) {
            var tag = self._tags[tagsIndex];
            if (track.tags.tag.indexOf(tag) >= 0) {
              match_tags = true;
              break;
            }
          }
          // skip by flavor
          var match_flavors = false;
          for (var flavorsIndex = 0; flavorsIndex < self._flavors.length; flavorsIndex++) {
            var flavor = self._flavors[flavorsIndex];
            if (track.type === flavor) {
              match_flavors = true;
              break;
            }
          }

          if (self._tags.length > 0 && self._flavors.length > 0) {
            if (self._tags_and_flavors) {
              if (!(match_tags && match_flavors)) { continue; }
            } else {
              if (!(match_tags || match_flavors)) { continue; }
            }
          } else {
            if (self._tags.length > 0 && !match_tags) { continue; }
            if (self._flavors.length > 0 && !match_flavors) { continue; }
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
        let flavors = self._tracks.reduce(function(result, track) {
          if (result.indexOf(track.flavor) < 0) {
            result.push(track.flavor);
          }
          return result;
        }, []);

        // group by flavor
        for (let flavorsIndex = 0; flavorsIndex < flavors.length; flavorsIndex++) {
          let flavor = flavors[flavorsIndex];
          let flavorContainer = document.createElement('div');
          flavorContainer.className = 'downloadsButtonItemContainer';

          let flavorNameContainer = document.createElement('p');
          flavorNameContainer.innerText = flavor;
          flavorContainer.appendChild(flavorNameContainer);

          // order by type (audio or video)
          for (let trackTypesIndex = 0; trackTypesIndex < trackTypes.length; trackTypesIndex++) {
            let trackType = trackTypes[trackTypesIndex];
            let tracks = self._tracks.filter(function(track) {
              return this === track.type;
            }, trackType);

            // order by mimetype
            let trackMimetypes = tracks.reduce(function(result, track) {
              if (result.indexOf(track.mimetype) < 0) {
                result.push(track.mimetype);
              }
              return result;
            }, []);

            trackMimetypes.sort(function(f1, f2) {
              return f1 === f2 ? 0 : f1 > f2 ? 1 : -1;
            });

            // order by video resolution or audio bitrate
            for (let trackMimetypesIndex = 0; trackMimetypesIndex < trackMimetypes.length; trackMimetypesIndex++) {
              let mimetype = trackMimetypes[trackMimetypesIndex];
              let filteredTracks = tracks.filter(function(track) {
                return track.flavor === this.flavor && track.mimetype === this.mimetype;
              }, { 'flavor': flavor, 'mimetype': mimetype });
              filteredTracks.sort(function(t1, t2) {
                if (t1.type === t2.type) {
                  if ('video' === t1.type) {
                    let t1_height = t1.video_resolution.split('x')[1];
                    let t2_height = t2.video_resolution.split('x')[1];
                    return parseInt(t2_height) - parseInt(t1_height);
                  } else if ('audio' === t1.type) {
                    return parseInt(t2.audio_bitrate) - parseInt(t1.audio_bitrate);
                  }
                }
                // this case shouldn't happen
                return 0;
              });

              // render download links
              for (let filteredTracksIndex = 0; filteredTracksIndex < filteredTracks.length; filteredTracksIndex++) {
                let track = filteredTracks[filteredTracksIndex];
                let a = document.createElement('a');
                a.href = track.url;

                if ('video' === track.type) {
                  let videoFormatElement = document.createElement('span');
                  videoFormatElement.className = 'downloadsButtonItemLinkSpanFormat';
                  videoFormatElement.innerText = `[${paella.dictionary.translate(mimetype)}]`;
                  a.appendChild(videoFormatElement);

                  let videoResolutionElement = document.createElement('span');
                  videoResolutionElement.className = 'downloadsButtonItemLinkSpanResolution';
                  videoResolutionElement.innerText = `${track.video_resolution}@${track.video_framerate}`;
                  a.appendChild(videoResolutionElement);
                } else if ('audio' === track.type) {
                  let audioFormatElement = document.createElement('span');
                  audioFormatElement.className = 'downloadsButtonItemLinkSpanFormat';
                  audioFormatElement.innerText = `[${paella.dictionary.translate(mimetype)}]`;
                  a.appendChild(audioFormatElement);

                  let audioBitrateElement = document.createElement('span');
                  audioBitrateElement.className = 'downloadsButtonItemLinkSpanAudioBitrate';
                  audioBitrateElement.innerText = `${Math.floor(track.audio_bitrate / 1000)}kbps`;
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
