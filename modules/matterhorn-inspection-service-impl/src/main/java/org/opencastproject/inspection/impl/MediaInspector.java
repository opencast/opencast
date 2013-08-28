/**
 *  Copyright 2009, 2010 The Regents of the University of California
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
package org.opencastproject.inspection.impl;

import org.apache.commons.io.FilenameUtils;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.opencastproject.inspection.api.MediaInspectionException;
import org.opencastproject.inspection.impl.api.AudioStreamMetadata;
import org.opencastproject.inspection.impl.api.MediaAnalyzer;
import org.opencastproject.inspection.impl.api.MediaAnalyzerException;
import org.opencastproject.inspection.impl.api.MediaContainerMetadata;
import org.opencastproject.inspection.impl.api.VideoStreamMetadata;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Stream;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.UnsupportedElementException;
import org.opencastproject.mediapackage.track.AudioStreamImpl;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.mediapackage.track.VideoStreamImpl;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UnknownFileTypeException;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workspace.api.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import static org.opencastproject.util.data.Collections.map;

/**
 * Contains the business logic for media inspection.
 * Its primary purpose is to decouple the inspection logic from all OSGi/MH job management boilerplate.
 */
public class MediaInspector {
  private static final Logger logger = LoggerFactory.getLogger(MediaInspector.class);

  private final Workspace workspace;
  /** The Apache Tika parser */
  private final Parser tikaParser;
  private final String mediaInfoPath;

  public MediaInspector(Workspace workspace, Parser tikaParser, String mediaInfoPath) {
    this.workspace = workspace;
    this.tikaParser = tikaParser;
    this.mediaInfoPath = mediaInfoPath;
  }

  /**
   * Inspects the element that is passed in as uri.
   *
   * @param trackURI
   *         the element uri
   * @return the inspected track
   * @throws org.opencastproject.inspection.api.MediaInspectionException
   *         if inspection fails
   */
  public Track inspectTrack(URI trackURI) throws MediaInspectionException {
    logger.debug("inspect(" + trackURI + ") called, using workspace " + workspace);

    try {
      // Get the file from the URL (runtime exception if invalid)
      File file = null;
      try {
        file = workspace.get(trackURI);
      } catch (NotFoundException notFound) {
        throw new MediaInspectionException("Unable to find resource " + trackURI, notFound);
      } catch (IOException ioe) {
        throw new MediaInspectionException("Error reading " + trackURI + " from workspace", ioe);
      }

      // Make sure the file has an extension. Otherwise, tools like ffmpeg will not work.
      // TODO: Try to guess the extension from the container's metadata
      if ("".equals(FilenameUtils.getExtension(file.getName()))) {
        throw new MediaInspectionException("Can not inspect files without a filename extension");
      }

      MediaContainerMetadata metadata = getFileMetadata(file);
      if (metadata == null) {
        throw new MediaInspectionException("Media analyzer returned no metadata from " + file);
      } else {
        MediaPackageElementBuilder elementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
        TrackImpl track;
        MediaPackageElement element;
        try {
          element = elementBuilder.elementFromURI(trackURI, MediaPackageElement.Type.Track, null);
        } catch (UnsupportedElementException e) {
          throw new MediaInspectionException("Unable to create track element from " + file, e);
        }
        track = (TrackImpl) element;

        // Duration
        if (metadata.getDuration() != null && metadata.getDuration() > 0)
          track.setDuration(metadata.getDuration());

        // Checksum
        try {
          track.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, file));
        } catch (IOException e) {
          throw new MediaInspectionException("Unable to read " + file, e);
        }

        // Mimetype
        InputStream is = null;
        try {
          // Try to get the Mimetype from Apache Tika
          is = new FileInputStream(file);
          MimeType mimeType = extractContentType(is);

          // If Mimetype could not be extracted try to get it from opencast
          if (mimeType == null) {
            mimeType = MimeTypes.fromURL(file.toURI().toURL());

            // The mimetype library doesn't know about audio/video metadata, so the type might be wrong.
            if ("audio".equals(mimeType.getType()) && metadata.hasVideoStreamMetadata()) {
              mimeType = MimeTypes.parseMimeType("video/" + mimeType.getSubtype());
            } else if ("video".equals(mimeType.getType()) && !metadata.hasVideoStreamMetadata()) {
              mimeType = MimeTypes.parseMimeType("audio/" + mimeType.getSubtype());
            }
          }
          track.setMimeType(mimeType);
        } catch (Exception e) {
          logger.error("Unable to find mimetype for {}", file.getAbsolutePath());
        } finally {
          IoSupport.closeQuietly(is);
        }

        // Audio metadata
        try {
          addAudioStreamMetadata(track, metadata);
        } catch (Exception e) {
          throw new MediaInspectionException("Unable to extract audio metadata from " + file, e);
        }

        // Videometadata
        try {
          addVideoStreamMetadata(track, metadata);
        } catch (Exception e) {
          throw new MediaInspectionException("Unable to extract video metadata from " + file, e);
        }

        return track;
      }
    } catch (Exception e) {
      logger.warn("Error inspecting " + trackURI, e);
      if (e instanceof MediaInspectionException) {
        throw (MediaInspectionException) e;
      } else {
        throw new MediaInspectionException(e);
      }
    }
  }

  /**
   * Enriches the given element's mediapackage.
   *
   * @param element
   *         the element to enrich
   * @param override
   *         <code>true</code> to override existing metadata
   * @return the enriched element
   * @throws MediaInspectionException
   *         if enriching fails
   */
  public MediaPackageElement enrich(MediaPackageElement element, boolean override)
          throws MediaInspectionException {
    if (element instanceof Track) {
      final Track originalTrack = (Track) element;
      return enrichTrack(originalTrack, override);
    } else {
      return enrichElement(element, override);
    }
  }

  /**
   * Enriches the track's metadata and can be executed in an asynchronous way.
   *
   * @param originalTrack
   *         the original track
   * @param override
   *         <code>true</code> to override existing metadata
   * @return the media package element
   * @throws MediaInspectionException
   */
  private MediaPackageElement enrichTrack(final Track originalTrack, final boolean override)
          throws MediaInspectionException {
    try {
      URI originalTrackUrl = originalTrack.getURI();
      MediaPackageElementFlavor flavor = originalTrack.getFlavor();
      logger.debug("enrich(" + originalTrackUrl + ") called");

      // Get the file from the URL
      File file = null;
      try {
        file = workspace.get(originalTrackUrl);
      } catch (NotFoundException e) {
        throw new MediaInspectionException("File " + file + " was not found and can therefore not be inspected", e);
      } catch (IOException e) {
        throw new MediaInspectionException("Error accessing " + file, e);
      }

      // Make sure the file has an extension. Otherwise, tools like ffmpeg will not work.
      // TODO: Try to guess the extension from the container's metadata
      if ("".equals(FilenameUtils.getExtension(file.getName()))) {
        throw new MediaInspectionException("Can not inspect files without a filename extension");
      }

      MediaContainerMetadata metadata = getFileMetadata(file);
      if (metadata == null) {
        throw new MediaInspectionException("Unable to acquire media metadata for " + originalTrackUrl);
      } else {
        TrackImpl track = null;
        try {
          track = (TrackImpl) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
                  .elementFromURI(originalTrackUrl, MediaPackageElement.Type.Track, flavor);
        } catch (UnsupportedElementException e) {
          throw new MediaInspectionException("Unable to create track element from " + file, e);
        }

        // init the new track with old
        track.setChecksum(originalTrack.getChecksum());
        track.setDuration(originalTrack.getDuration());
        track.setElementDescription(originalTrack.getElementDescription());
        track.setFlavor(flavor);
        track.setIdentifier(originalTrack.getIdentifier());
        track.setMimeType(originalTrack.getMimeType());
        track.setReference(originalTrack.getReference());
        track.setSize(file.length());
        track.setURI(originalTrackUrl);
        for (String tag : originalTrack.getTags()) {
          track.addTag(tag);
        }

        // enrich the new track with basic info
        if (track.getDuration() == null || override)
          track.setDuration(metadata.getDuration());
        if (track.getChecksum() == null || override) {
          try {
            track.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, file));
          } catch (IOException e) {
            throw new MediaInspectionException("Unable to read " + file, e);
          }
        }

        // Add the mime type if it's not already present
        if (track.getMimeType() == null || override) {
          try {
            MimeType mimeType = MimeTypes.fromURI(track.getURI());

            // The mimetype library doesn't know about audio/video metadata, so the type might be wrong.
            if ("audio".equals(mimeType.getType()) && metadata.hasVideoStreamMetadata()) {
              mimeType = MimeTypes.parseMimeType("video/" + mimeType.getSubtype());
            } else if ("video".equals(mimeType.getType()) && !metadata.hasVideoStreamMetadata()) {
              mimeType = MimeTypes.parseMimeType("audio/" + mimeType.getSubtype());
            }
            track.setMimeType(mimeType);
          } catch (UnknownFileTypeException e) {
            logger.info("Unable to detect the mimetype for track {} at {}", track.getIdentifier(), track.getURI());
          }
        }

        // find all streams
        Dictionary<String, Stream> streamsId2Stream = new Hashtable<String, Stream>();
        for (Stream stream : originalTrack.getStreams()) {
          streamsId2Stream.put(stream.getIdentifier(), stream);
        }

        // audio list
        try {
          addAudioStreamMetadata(track, metadata);
        } catch (Exception e) {
          throw new MediaInspectionException("Unable to extract audio metadata from " + file, e);
        }

        // video list
        try {
          addVideoStreamMetadata(track, metadata);
        } catch (Exception e) {
          throw new MediaInspectionException("Unable to extract video metadata from " + file, e);
        }

        logger.info("Successfully inspected track {}", track);
        return track;
      }
    } catch (Exception e) {
      logger.warn("Error enriching track " + originalTrack, e);
      if (e instanceof MediaInspectionException) {
        throw (MediaInspectionException) e;
      } else {
        throw new MediaInspectionException(e);
      }
    }
  }

  /**
   * Enriches the media package element metadata such as the mime type, the file size etc.
   * The method mutates the argument element.
   *
   * @param element
   *         the media package element
   * @param override
   *         <code>true</code> to overwrite existing metadata
   * @return the enriched element
   * @throws MediaInspectionException
   *         if enriching fails
   */
  private MediaPackageElement enrichElement(final MediaPackageElement element, final boolean override)
          throws MediaInspectionException {
    try {
      File file;
      try {
        file = workspace.get(element.getURI());
      } catch (NotFoundException e) {
        throw new MediaInspectionException("Unable to find " + element.getURI() + " in the workspace", e);
      } catch (IOException e) {
        throw new MediaInspectionException("Error accessing " + element.getURI() + " in the workspace", e);
      }

      // Checksum
      if (element.getChecksum() == null || override) {
        try {
          element.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, file));
        } catch (IOException e) {
          throw new MediaInspectionException("Error generating checksum for " + element.getURI(), e);
        }
      }

      // Mimetype
      if (element.getMimeType() == null || override) {
        try {
          element.setMimeType(MimeTypes.fromURI(file.toURI()));
        } catch (UnknownFileTypeException e) {
          logger.info("unable to determine the mime type for {}", file.getName());
        }
      }

      logger.info("Successfully inspected element {}", element);

      return element;
    } catch (Exception e) {
      logger.warn("Error enriching element " + element, e);
      if (e instanceof MediaInspectionException) {
        throw (MediaInspectionException) e;
      } else {
        throw new MediaInspectionException(e);
      }
    }
  }

  /**
   * Asks the media analyzer to extract the file's metadata.
   *
   * @param file
   *         the file
   * @return the file container metadata
   * @throws MediaInspectionException
   *         if metadata extraction fails
   */
  private MediaContainerMetadata getFileMetadata(File file) throws MediaInspectionException {
    if (file == null)
      throw new IllegalArgumentException("file to analyze cannot be null");
    try {
      MediaAnalyzer analyzer = new MediaInfoAnalyzer();
      analyzer.setConfig(map(Tuple.<String, Object>tuple(MediaInfoAnalyzer.MEDIAINFO_BINARY_CONFIG, mediaInfoPath)));
      return analyzer.analyze(file);
    } catch (MediaAnalyzerException e) {
      throw new MediaInspectionException(e);
    }
  }

  /**
   * Adds the video related metadata to the track.
   *
   * @param track
   *         the track
   * @param metadata
   *         the container metadata
   * @throws Exception
   *         Media analysis is fragile, and may throw any kind of runtime exceptions due to inconsistencies in the
   *         media's metadata
   */
  private Track addVideoStreamMetadata(TrackImpl track, MediaContainerMetadata metadata) throws Exception {
    List<VideoStreamMetadata> videoList = metadata.getVideoStreamMetadata();
    if (videoList != null && !videoList.isEmpty()) {
      for (int i = 0; i < videoList.size(); i++) {
        VideoStreamImpl video = new VideoStreamImpl("video-" + (i + 1));
        VideoStreamMetadata v = videoList.get(i);
        video.setBitRate(v.getBitRate());
        video.setFormat(v.getFormat());
        video.setFormatVersion(v.getFormatVersion());
        video.setFrameHeight(v.getFrameHeight());
        video.setFrameRate(v.getFrameRate());
        video.setFrameWidth(v.getFrameWidth());
        video.setScanOrder(v.getScanOrder());
        video.setScanType(v.getScanType());
        // TODO: retain the original video metadata
        track.addStream(video);
      }
    }
    return track;
  }

  /**
   * Adds the audio related metadata to the track.
   *
   * @param track
   *         the track
   * @param metadata
   *         the container metadata
   * @throws Exception
   *         Media analysis is fragile, and may throw any kind of runtime exceptions due to inconsistencies in the
   *         media's metadata
   */
  private Track addAudioStreamMetadata(TrackImpl track, MediaContainerMetadata metadata) throws Exception {
    List<AudioStreamMetadata> audioList = metadata.getAudioStreamMetadata();
    if (audioList != null && !audioList.isEmpty()) {
      for (int i = 0; i < audioList.size(); i++) {
        AudioStreamImpl audio = new AudioStreamImpl("audio-" + (i + 1));
        AudioStreamMetadata a = audioList.get(i);
        audio.setBitRate(a.getBitRate());
        audio.setChannels(a.getChannels());
        audio.setFormat(a.getFormat());
        audio.setFormatVersion(a.getFormatVersion());
        audio.setBitDepth(a.getResolution());
        audio.setSamplingRate(a.getSamplingRate());
        // TODO: retain the original audio metadata
        track.addStream(audio);
      }
    }
    return track;
  }

  /**
   * Determines the content type of an input stream. This method reads part of the stream, so it is typically best to
   * close the stream immediately after calling this method.
   *
   * @param in
   *         the input stream
   * @return the content type
   */
  private MimeType extractContentType(InputStream in) {
    try {
      // Find the content type, based on the stream content
      BodyContentHandler contenthandler = new BodyContentHandler();
      Metadata metadata = new Metadata();
      ParseContext context = new ParseContext();
      tikaParser.parse(in, contenthandler, metadata, context);
      String mimeType = metadata.get(HttpHeaders.CONTENT_TYPE);
      if (mimeType == null)
        return null;
      return MimeTypes.parseMimeType(mimeType);
    } catch (Exception e) {
      logger.warn("Unable to extract mimetype from input stream, ", e);
      return null;
    }
  }
}
