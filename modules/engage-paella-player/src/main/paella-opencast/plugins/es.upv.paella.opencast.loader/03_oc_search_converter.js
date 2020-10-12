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
class OpencastToPaellaConverter {

  constructor() {
    this._config = paella.player.config.plugins.list['es.upv.paella.opencast.loader'] || {};
    this._orderTracks = this._config.orderTracks ||
          ['presenter/delivery', 'presenter/preview', 'presentation/delivery', 'presentation/preview'];
  }

  getFilterStream() {
    var filterStream;

    var streams = this._config.streams || [];
    streams.some(function(curretStream){
      return curretStream.filter.system.some(function(currentFilter) {
        if ((currentFilter == '*') || base.userAgent.system[currentFilter] ) {
          filterStream = curretStream;
          return true;
        }
      });
    });

    if (!filterStream) {
      filterStream = {
        'filter': {
          'system': ['*']
        },
        'tracks': {
          'flavors': ['*/*'],
          'tags': ['*']
        }
      };
    }

    return filterStream;
  }

  getAudioTagConfig() {
    return  this._config.audioTag || { '*/*': '*' };
  }

  getSourceTypeFromTrack(track) {
    var sourceType = null;

    var protocol = /^(.*):\/\/.*$/.exec(track.url);
    if (protocol) {
      switch(protocol[1]) {
      case 'rtmp':
      case 'rtmps':
        switch (track.mimetype) {
        case 'video/mp4':
        case 'video/ogg':
        case 'video/webm':
        case 'video/x-flv':
          sourceType = 'rtmp';
          break;
        default:
          paella.debug.log(`OpencastToPaellaConverter: MimeType (${track.mimetype}) not supported!`);
          break;
        }
        break;
      case 'http':
      case 'https':
        switch (track.mimetype) {
        case 'video/mp4':
        case 'video/ogg':
        case 'video/webm':
          sourceType = track.mimetype.split('/')[1];
          break;
        case 'video/x-flv':
          sourceType = 'flv';
          break;
        case 'application/x-mpegURL':
          sourceType = 'hls';
          break;
        case 'application/dash+xml':
          sourceType = 'mpd';
          break;
        case 'audio/m4a':
          sourceType = 'audio';
          break;
        default:
          paella.debug.log(`OpencastToPaellaConverter: MimeType (${track.mimetype}) not supported!`);
          break;
        }
        break;
      default:
        paella.debug.log(`OpencastToPaellaConverter: Protocol (${protocol[1]}) not supported!`);
        break;
      }
    }

    return sourceType;
  }

  getStreamSourceFromTrack(track) {
    var res = new Array(0,0);
    if (track.video instanceof Object) {
      res = track.video.resolution.split('x');
    }

    var src = track.url;
    var urlSplit = /^(rtmps?:\/\/[^/]*\/[^/]*)\/(.*)$/.exec(track.url);
    if (urlSplit != null) {
      var rtmp_server =  urlSplit[1];
      var rtmp_stream =  urlSplit[2];
      src = {
        server: encodeURIComponent(rtmp_server),
        stream: encodeURIComponent(rtmp_stream)
      };
    }

    var source = {
      src:  src,
      isLiveStream: (track.live === true)
    };

    if(track.mimetype != 'audio/m4a') {
      source.mimetype = track.mimetype;
      source.res = {w:res[0], h:res[1]};
    }

    return source;
  }

  getAudioTagFromTrack(currentTrack) {
    let audioTagConfig = this.getAudioTagConfig();
    let audioTag;

    let tags = [];
    if ( (currentTrack.tags) && (currentTrack.tags.tag) ) {
      tags = currentTrack.tags.tag;
      if (!(tags instanceof Array)) {
        tags = [tags];
      }
    }
    tags.some(function(tag){
      if (tag.startsWith('audioTag:')){
        audioTag = tag.slice(9);
        return true;
      }
    });
    if (!audioTag) {
      Object.entries(audioTagConfig).some(function(atc){
        let sflavor = currentTrack.type.split('/');
        let smask = atc[0].split('/');

        if (((smask[0] == '*') || (smask[0] == sflavor[0])) && ((smask[1] == '*') || (smask[1] == sflavor[1]))) {
          audioTag = (atc[1] == '*') ? base.dictionary.currentLanguage() : atc[1];
          return true;
        }
      });
    }

    return audioTag;
  }

  /**
   * Extract a stream identified by a given flavor from the media packages track list and try to find a corresponding
   * image attachment for the selected track.
   * @param episode   result structure from search service
   * @param flavor    flavor used for track selection
   * @param subFlavor subflavor used for track selection
   */
  getStreamFromFlavor(episode, flavor, subFlavor) {
    var currentStream = { sources:{}, preview: '', content: flavor };

    var tracks = episode.mediapackage.media.track;
    var attachments = episode.mediapackage.attachments.attachment;
    if (!(tracks instanceof Array)) { tracks = tracks ? [tracks] : []; }
    if (!(attachments instanceof Array)) { attachments = attachments ? [attachments] : []; }

    // Read the tracks!!
    tracks.forEach((currentTrack) => {
      if (currentTrack.type == flavor + '/' + subFlavor) {
        var sourceType = this.getSourceTypeFromTrack(currentTrack);
        if (sourceType){
          if ( !(currentStream.sources[sourceType]) || !(currentStream.sources[sourceType] instanceof Array)){
            currentStream.sources[sourceType] = [];
          }
          if (currentTrack.audio) {
            currentStream.audioTag = this.getAudioTagFromTrack(currentTrack);
          }
          currentStream.sources[sourceType].push(this.getStreamSourceFromTrack(currentTrack));

          if (currentTrack.video) {
            currentStream.type = 'video';
          }
          else if (currentTrack.audio && currentStream.type !== 'video') {
            currentStream.type = 'audio';
          }
        }
      }
    });

    // Read the attachments
    var duration = parseInt(episode.mediapackage.duration / 1000);
    var imageSource =   {type:'image/jpeg', frames:{}, count:0, duration: duration, res:{w:320, h:180}};
    var imageSourceHD = {type:'image/jpeg', frames:{}, count:0, duration: duration, res:{w:1280, h:720}};
    attachments.forEach((currentAttachment) => {
      if (currentAttachment.type == `${flavor}/player+preview`) {
        currentStream.preview = currentAttachment.url;
      }
      else if (currentAttachment.type == `${flavor}/segment+preview+hires`) {
        if (/time=T(\d+):(\d+):(\d+)/.test(currentAttachment.ref)) {
          time = parseInt(RegExp.$1) * 60 * 60 + parseInt(RegExp.$2) * 60 + parseInt(RegExp.$3);
          imageSourceHD.frames['frame_' + time] = currentAttachment.url;
          imageSourceHD.count = imageSourceHD.count + 1;
        }
      }
      else if (currentAttachment.type == `${flavor}/segment+preview`) {
        if (/time=T(\d+):(\d+):(\d+)/.test(currentAttachment.ref)) {
          var time = parseInt(RegExp.$1) * 60 * 60 + parseInt(RegExp.$2) * 60 + parseInt(RegExp.$3);
          imageSource.frames['frame_' + time] = currentAttachment.url;
          imageSource.count = imageSource.count + 1;
        }
      }
    });

    var imagesArray = [];
    if (imageSource.count > 0) {
      imagesArray.push(imageSource);
    }
    if (imageSourceHD.count > 0) {
      imagesArray.push(imageSourceHD);
    }
    if (imagesArray.length > 0) {
      currentStream.sources.image = imagesArray;
    }

    return currentStream;
  }

  getContentToImport(episode) {
    var filterStream = this.getFilterStream();

    var flavors = [];
    var tracks = episode.mediapackage.media.track;
    if (!(tracks instanceof Array)) { tracks = [tracks]; }

    tracks.forEach((currentTrack) => {
      let importF = filterStream.tracks.flavors.some(function(cFlavour) {
        let smask = cFlavour.split('/');
        let sflavour = currentTrack.type.split('/');

        return (((smask[0] == '*') || (smask[0] == sflavour[0])) && ((smask[1] == '*') || (smask[1] == sflavour[1])));
      });

      let importT = false;
      let tags = [];
      if ( (currentTrack.tags) && (currentTrack.tags.tag) ) {
        tags = [currentTrack.tags.tag];
        if (!(currentTrack.tags.tag instanceof Array)) {
          tags = [currentTrack.tags.tag];
        }
      }
      importT = filterStream.tracks.tags.some(function(cTag) {
        return (cTag == '*') || tags.some(function(t){ return (cTag == t); });
      });

      if (importF || importT) {
        if (flavors.indexOf(currentTrack.type) < 0) {
          flavors.push(currentTrack.type);
        }
      }
    });

    // Sort the streams
    for (let i = this._orderTracks.length - 1; i >= 0; i--) {
      let flavor = this._orderTracks[i];
      if (flavors.indexOf(flavor) > 0) {
        flavors.splice(flavors.indexOf(flavor), 1);
        flavors.unshift(flavor);
      }
    }

    return flavors;
  }

  getStreams(episode) {
    // Get the streams
    var paellaStreams = [];
    var flavors = this.getContentToImport(episode);
    flavors.forEach((flavorStr) => {
      var [flavor, subFlavor] = flavorStr.split('/');
      var stream = this.getStreamFromFlavor(episode, flavor, subFlavor);
      paellaStreams.push(stream);
    });
    return paellaStreams;
  }

  getCaptions(episode) {
    var captions = [];

    var attachments = episode.mediapackage.attachments.attachment;
    var catalogs = episode.mediapackage.metadata.catalog;
    if (!(attachments instanceof Array)) { attachments = attachments ? [attachments] : []; }
    if (!(catalogs instanceof Array)) { catalogs = catalogs ? [catalogs] : []; }


    // Read the attachments
    attachments.forEach((currentAttachment) => {
      try {
        let captions_regex = /^captions\/([^+]+)(\+(.+))?/g;
        let captions_match = captions_regex.exec(currentAttachment.type);

        if (captions_match) {
          let captions_format = captions_match[1];
          let captions_lang = captions_match[3];

          // TODO: read the lang from the dfxp file
          //if (captions_format == "dfxp") {}

          if (!captions_lang && currentAttachment.tags && currentAttachment.tags.tag) {
            if (!(currentAttachment.tags.tag instanceof Array)) {
              currentAttachment.tags.tag = [currentAttachment.tags.tag];
            }
            currentAttachment.tags.tag.forEach((tag)=>{
              if (tag.startsWith('lang:')){
                let split = tag.split(':');
                captions_lang = split[1];
              }
            });
          }

          let captions_label = captions_lang || 'unknown language';
          //base.dictionary.translate("CAPTIONS_" + captions_lang);

          captions.push({
            id: currentAttachment.id,
            lang: captions_lang,
            text: captions_label,
            url: currentAttachment.url,
            format: captions_format
          });
        }
      }
      catch (err) {/**/}
    });

    // Read the catalogs
    catalogs.forEach((currentCatalog) => {
      try {
        // backwards compatibility:
        // Catalogs flavored as 'captions/timedtext' are assumed to be dfxp
        if (currentCatalog.type == 'captions/timedtext') {
          let captions_lang;

          if (currentCatalog.tags && currentCatalog.tags.tag) {
            if (!(currentCatalog.tags.tag instanceof Array)) {
              currentCatalog.tags.tag = [currentCatalog.tags.tag];
            }
            currentCatalog.tags.tag.forEach((tag)=>{
              if (tag.startsWith('lang:')){
                let split = tag.split(':');
                captions_lang = split[1];
              }
            });
          }

          let captions_label = captions_lang || 'unknown language';
          captions.push({
            id: currentCatalog.id,
            lang: captions_lang,
            text: captions_label,
            url: currentCatalog.url,
            format: 'dfxp'
          });
        }
      }
      catch (err) {/**/}
    });

    return captions;
  }

  getSegments(episode) {
    var segments = [];

    var attachments = episode.mediapackage.attachments.attachment;
    if (!(attachments instanceof Array)) { attachments = attachments ? [attachments] : []; }

    // Read the attachments
    var opencastFrameList = {};
    attachments.forEach((currentAttachment) => {
      try {
        if (currentAttachment.type == 'presentation/segment+preview+hires') {
          if (/time=T(\d+):(\d+):(\d+)/.test(currentAttachment.ref)) {
            time = parseInt(RegExp.$1) * 60 * 60 + parseInt(RegExp.$2) * 60 + parseInt(RegExp.$3);

            if (!(opencastFrameList[time])){
              opencastFrameList[time] = {
                id: 'frame_' + time,
                mimetype: currentAttachment.mimetype,
                time: time,
                url: currentAttachment.url,
                thumb: currentAttachment.url
              };
            }
            opencastFrameList[time].url = currentAttachment.url;
          }
        }
        else if (currentAttachment.type == 'presentation/segment+preview') {
          if (/time=T(\d+):(\d+):(\d+)/.test(currentAttachment.ref)) {
            var time = parseInt(RegExp.$1) * 60 * 60 + parseInt(RegExp.$2) * 60 + parseInt(RegExp.$3);
            if (!(opencastFrameList[time])){
              opencastFrameList[time] = {
                id: 'frame_' + time,
                mimetype: currentAttachment.mimetype,
                time: time,
                url: currentAttachment.url,
                thumb: currentAttachment.url
              };
            }
            opencastFrameList[time].thumb = currentAttachment.url;
          }
        }
      }
      catch (err) {/**/}
    });

    Object.keys(opencastFrameList).forEach((key, index) => {
      segments.push(opencastFrameList[key]);
    });
    return segments;
  }

  getPreviewImage(episode) {
    let presenterPreview;
    let presentationPreview;
    let otherPreview;

    var attachments = episode.mediapackage.attachments.attachment;
    if (!(attachments instanceof Array)) { attachments = attachments ? [attachments] : []; }
    attachments.forEach((currentAttachment) => {
      if (currentAttachment.type == 'presenter/player+preview') {
        presenterPreview = currentAttachment.url;
      }
      if (currentAttachment.type == 'presentation/player+preview') {
        presentationPreview = currentAttachment.url;
      }
      if (currentAttachment.type.endsWith('/player+preview')) {
        otherPreview = currentAttachment.url;
      }
    });

    return presentationPreview || presenterPreview || otherPreview;
  }

  convertToDataJson(episode) {
    var streams = this.getStreams(episode);
    var captions = this.getCaptions(episode);
    var segments = this.getSegments(episode);

    var data =  {
      metadata: {
        title: episode.mediapackage.title,
        duration: episode.mediapackage.duration / 1000,
        preview: this.getPreviewImage(episode)
      },
      streams: streams,
      frameList: segments,
      captions: captions
    };

    return data;
  }
}
