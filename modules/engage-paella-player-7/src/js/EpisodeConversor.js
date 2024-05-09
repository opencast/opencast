/*
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
import { translate } from 'paella-core';

const g_streamTypes = [
  {
    enabled: true,
    streamType: 'mp4',
    conditions: {
      mimetype: 'video/mp4'
    },
    getSourceData: (track) => {
      const src = track.url;
      const mimetype = track.mimetype;
      const resolution = track.video?.resolution || '1x1';
      const resData = /(\d+)x(\d+)/.exec(resolution);
      const res = {
        w: 0,
        h: 0
      };

      if (resData) {
        res.w = resData[1];
        res.h = resData[2];
      }

      return { src, mimetype, res };
    }
  },
  {
    enabled: true,
    streamType: 'hls',
    conditions: {
      mimetype: 'application/x-mpegURL',
      live: false
    },
    getSourceData: (track) => {
      const src = track.url;
      const mimetype = track.mimetype;
      const master = track.master;
      return { src, mimetype, master };
    }
  },
  {
    enabled: true,
    streamType: 'hlsLive',
    conditions: {
      mimetype: 'application/x-mpegURL',
      live: true
    },
    getSourceData: (track) => {
      const src = track.url;
      const mimetype = track.mimetype;
      return { src, mimetype };
    }
  },
  {
    enabled: true,
    streamType: 'audio',
    conditions: {
      mimetype: 'audio/mpeg'
    },
    getSourceData: (track) => {
      const src = track.url;
      const mimetype = track.mimetype;
      return { src, mimetype };
    }
  }
];

function getStreamType(track) {
  const result = g_streamTypes.find(typeData => {
    let match = typeData.enabled;
    for (const condition in typeData.conditions) {
      if (!match) {
        break;
      }
      const value = typeData.conditions[condition];
      match = match && track[condition] == value;
    }
    return match;
  });
  return result;
}

function getTags(mpe) {
  let tags = [];
  if (mpe.tags && mpe.tags.tag) {
    tags = mpe.tags.tag;
    if (!(tags instanceof Array)) {
      tags = [tags];
    }
  }
  return tags;
}

function getSourceData(track, config) {
  let data = null;
  // Get substring of type before slash
  const type = track.type.split('/')[0];
  if (type) {
    const streamType = getStreamType(track, config);
    if (streamType) {
      data = {
        source: streamType.getSourceData(track, config),
        type: streamType.streamType,
        content: type,
        flavor: track.type,
        tags: getTags(track)
      };
    }
  }
  return data;
}

function ensureArray(thing) {
  //Ensure that thing is an array, if it's not wrap it into an array.  If it's null then return an empty array.
  return thing ?
    (Array.isArray(thing) ?
      thing :
      [ thing ]) :
    [];
}

function ensureSingle(thing) {
  //Ensure we get a single thing, either by taking the first element in an array, returning the input, or undefined
  return thing ?
    (Array.isArray(thing) ?
      thing[0] :
      thing) :
    undefined ;
}

function getMetadata(episode, config) {
  const dc = episode?.dc;

  const tracks = episode?.mediapackage?.media?.track
    ? (Array.isArray(episode.mediapackage.media.track)
      ? episode.mediapackage.media.track
      : [episode.mediapackage.media.track])
    : [];

  const isLive = tracks.some((track) => track.live === true);
  const visibleTimeLine = !(isLive && config?.hideTimeLineOnLive);

  const result = {
    title: ensureSingle(dc?.title),
    subject: ensureSingle(dc?.subject),
    description: ensureArray(dc?.description),
    language: ensureSingle(dc?.language),
    rights: ensureArray(dc?.rightsHolder),
    license: ensureSingle(dc?.license),
    series: ensureSingle(dc?.isPartOf),
    presenters: ensureArray(dc?.creator),
    contributors: ensureArray(dc?.contributor),
    startDate: new Date(dc?.created),
    duration: episode?.mediapackage?.duration / 1000, //FIXME: Parse from DC:extent (cf "PT9M57.002S")
    location: ensureSingle(episode?.dc?.spatial),
    UID: episode?.mediapackage?.id, //FIXME: Move this to the DC fields?
    opencast: {episode},
    visibleTimeLine
  };

  return result;
}

function splitFlavor(flavor) {
  const flavorTypes = flavor.split('/');
  if (flavorTypes.length != 2) {
    throw new Error('Invalid flavor');
  }
  return flavorTypes;
}

function matchesFlavor(flavor, configFlavor) {
  const [ flavorType, flavorSubtype ] = splitFlavor(flavor);
  const [ configFlavorType, configFlavorSubtype ] = splitFlavor(configFlavor);

  if ((configFlavorType === '*' || configFlavorType === flavorType) &&
  (configFlavorSubtype === '*' || configFlavorSubtype === flavorSubtype)) {
    return true;
  }
  return false;
}


function mergeSources(sources, config) {
  const streams = [];
  // Does the sources contain any flavour compatible with the main audio content?
  let audioContent = null;
  sources.find(sourceData => {
    const { content } = sourceData;
    if (content === config.mainAudioContent) {
      audioContent = config.mainAudioContent;
      return true;
    }
    else {
      audioContent = content;
    }
  });

  sources.forEach(sourceData => {
    const { content, type, source, flavor, tags } = sourceData;
    let stream = streams.find(s => s.content === content);

    if (!stream) {
      // check which video canvases to use
      let canvas = [];
      if (config.videoCanvas) {
        for (var key of Object.keys(config.videoCanvas)) {
          let canvasConfig = config.videoCanvas[key];
          // check if the flavor matches
          if (canvasConfig.flavor && matchesFlavor(flavor, canvasConfig.flavor)) {
            canvas.push(key);
            continue;
          }

          // check if a tag matches
          if (canvasConfig.tag) {
            for (let i = 0; i < tags.length; i++) {
              if (tags[i] === canvasConfig.tag) {
                canvas.push(key);
                break;
              }
            }
          }
        }
      }

      // fall back to default canvas if necessary
      if (!canvas.length) {
        canvas.push('video');
      }

      stream = {
        sources: {},
        content: content,
        canvas: canvas
      };

      if (content === audioContent) {
        stream.role = 'mainAudio';
        stream.canvas = ['audio']; // add canvas so that audio-only can be detected by paella-core functions
      }

      streams.push(stream);
    }

    stream.sources[type] = stream.sources[type] || [];
    stream.sources[type].push(source);
  });
  return streams;
}

function getStreams(episode, config) {
  let { track } = episode.mediapackage.media;
  if (!Array.isArray(track)) {
    track = [track];
  }

  let sources = [];

  track.forEach(track => {
    const sourceData = getSourceData(track, config);
    sourceData && sources.push(sourceData);
  });

  const hasMaster = sources.find((x)=> x.type == 'hls' && x.source.master == true);
  if (hasMaster) {
    sources = sources.filter((x)=> x.type == 'hls' ? x.source.master == true : true);
  }
  const streams = mergeSources(sources, config);
  return streams;
}

function processSegments(episode, manifest) {
  const { segments } = episode;
  if (segments) {
    manifest.transcriptions = manifest.transcriptions || [];
    if (!Array.isArray(segments.segment)) {
      segments.segment = [segments.segment];
    }
    segments.segment.forEach(({ index, previews, text, time, duration}) => {
      manifest.transcriptions.push({
        index,
        preview: previews?.preview?.$,
        text,
        time,
        duration
      });
    });
  }
}

// Extract the player preview to show prior to loading videos
export function getVideoPreview(mediapackage, config) {
  const { attachments } = mediapackage;
  let playerPreviewImage = null;

  let attachment = attachments?.attachment || [];
  if (!Array.isArray(attachment)) {
    attachment = [attachment];
  }

  const playerPreviewImageAttachments = config.playerPreviewImageAttachments || [
    'presenter/player+preview',
    'presentation/player+preview'
  ];

  attachment.forEach(att => {
    playerPreviewImageAttachments.some(validAttachment => {
      if (validAttachment === att.type) {
        playerPreviewImage = att.url;
      }
      return playerPreviewImage !== null;
    });
  });
  // Get first preview if no predefined was found
  if (playerPreviewImage === null) {
    const firstPreviewAttachment = attachment.find(att => {
      return att.type.split('/').pop() === 'player+preview';
    });
    playerPreviewImage = firstPreviewAttachment?.url ?? null;
  }

  return playerPreviewImage;
}

function processAttachments(episode, manifest, config) {
  const { attachments } = episode.mediapackage;
  const navigationPreviewImages = [];
  let playerPreviewImage = null;

  let attachment = attachments?.attachment || [];
  if (!Array.isArray(attachment)) {
    attachment = [attachment];
  }

  // Fallback array of segment preview images, when config flavor is not found
  let previewAttachments = config.previewAttachment || [
    'presenter/segment+preview',
    'presentation/segment+preview'
  ];
  // Ensure format is array, incase config flavor was a string
  if (!(previewAttachments instanceof Array)) {
    previewAttachments = [previewAttachments];
  }
  // Capture the video flavor of the segment preview images, used when
  // overlaying preview images on the video container
  let targetContent;
  attachment.forEach(att => {
    const timeRE = /time=T(\d+):(\d+):(\d+)/.exec(att.ref);
    // By default, Opencast workflows create segment preview slides for one media.
    // However, the following might cause duplicates if your workflow creates navigation
    // preview slides for all videos in a mediapackage.
    if (previewAttachments.includes(att.type) && timeRE) {
      const h = Number(timeRE[1]) * 60 * 60;
      const m = Number(timeRE[2]) * 60;
      const s = Number(timeRE[3]);
      const t = h + m + s;
      navigationPreviewImages.push({
        mimetype: att.mimetype,
        url: att.url,
        thumb: att.url,
        id: `frame_${t}`,
        time: t
      });
      // Capture the target flavor (e.g. 'presenter', 'presentation', etc)
      targetContent = att.type.split('/')[0];
    }
    else if (playerPreviewImage === null) {
      playerPreviewImage = getVideoPreview(episode.mediapackage, config);
    }
  });

  // Define frameList, even when there are no navigation segment preview slides
  // Include the video target, used to position the navigation frame
  // Otherwise the target video for navigation slides might not be found.
  // https://github.com/polimediaupv/paella-core/blob/main/src/js/core/ManifestParser.js#L101-L114
  manifest.frameList = {};
  manifest.frameList.frames = navigationPreviewImages || [];
  manifest.frameList.targetContent = targetContent;
  // Define manifest metadata even if a player preview image doesn't exist
  manifest.metadata = manifest.metadata || {};
  manifest.metadata.preview = playerPreviewImage;
}

function readCaptions(potentialNewCaptions, captions) {
  potentialNewCaptions.forEach((potentialCaption) => {
    try {
      let captions_regex = /^captions\/([^+]+)(\+(.+))?/g;
      let captions_match = captions_regex.exec(potentialCaption.type);

      if (captions_match) {
        // Fallback for captions which use the old flavor style, e.g. "captions/vtt+en"
        let captions_lang = captions_match[3];
        let captions_generated = '';
        let captions_closed = '';
        const captions_subtype = captions_match[1];

        let tags = getTags(potentialCaption);
        tags.forEach((tag)=>{
          if (tag.startsWith('lang:')){
            captions_lang = tag.substring('lang:'.length);
          }
          if (tag.startsWith('generator-type:') && tag.substring('generator-type:'.length) === 'auto') {
            captions_generated = ' (' + translate('automatically generated') + ')';
          }
          if (tag.startsWith('type:') && tag.substring('type:'.length) === 'closed-caption') {
            captions_closed = '[CC] ';
          }
        });

        let captions_format = potentialCaption.url.split('.').pop();
        // Backwards support for 'captions/dfxp' flavored xml files
        if (captions_subtype === 'dfxp' && captions_format === 'xml') {
          captions_format = captions_subtype;
        }

        let captions_description = translate('Undefined caption');
        if (captions_lang) {
          let languageNames = new Intl.DisplayNames([window.navigator.language], {type: 'language'});
          let captions_language_name = languageNames.of(captions_lang) || translate('Unknown language');
          captions_description = captions_closed + captions_language_name + captions_generated;
        }

        captions.push({
          id: potentialCaption.id,
          lang: captions_lang,
          text: captions_description,
          url: potentialCaption.url,
          format: captions_format
        });
      }
    }
    catch (err) {/**/}
  });
}

function getCaptions(episode) {
  var captions = [];

  var attachments = episode.mediapackage.attachments.attachment;
  var tracks = episode.mediapackage.media.track;
  if (!(attachments instanceof Array)) { attachments = attachments ? [attachments] : []; }
  if (!(tracks instanceof Array)) { tracks = tracks ? [tracks] : []; }

  // Read the attachments
  readCaptions(attachments, captions);

  // Read the tracks
  readCaptions(tracks, captions);

  return captions;
}

export function episodeToManifest(ocResponse, config) {
  const searchResults = ocResponse['result'];
  if (searchResults?.length === 1) {
    const episode = searchResults[0];
    const metadata = getMetadata(episode, config);
    const streams = getStreams(episode, config);
    const captions = getCaptions(episode, config);

    const result = {
      metadata,
      streams,
      captions
    };

    processAttachments(episode, result, config);
    processSegments(episode, result, config);



    return result;
  }
  else {
    return null;
  }
}

export default class EpisodeConversor {
  constructor(episodeJson, config = {}) {
    this._data = episodeToManifest(episodeJson, config);
  }

  get data() {
    return this._data;
  }
}
