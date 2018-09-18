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

package org.opencastproject.composer.api;

import org.opencastproject.composer.layout.Dimension;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.util.data.Option;

import java.util.List;
import java.util.Map;

/**
 * Encodes media and (optionally) periodically alerts a statusService endpoint of the status of this encoding job.
 */
public interface ComposerService {

  String JOB_TYPE = "org.opencastproject.composer";

  /** Used as mediaType to mark the source to omit processing of audio or video stream for process smil */
  String AUDIO_ONLY = "a";
  String VIDEO_ONLY = "v";

  /**
   * Encode one track, using that track's audio and video streams.
   *
   * @param sourceTrack
   *          The source track
   * @param profileId
   *          The profile to use for encoding
   * @return The receipt for this encoding job. The receipt can be used with ComposerService#getJob to
   *         obtain the status of an encoding job.
   * @throws EncoderException
   * @throws MediaPackageException
   */
  Job encode(Track sourceTrack, String profileId) throws EncoderException, MediaPackageException;

  /**
   * Encode the video stream from one track and the audio stream from another, into a new Track.
   *
   * @param sourceVideoTrack
   *          The source video track
   * @param sourceAudioTrack
   *          The source audio track
   * @param profileId
   *          The profile to use for encoding
   * @return The receipt for this encoding job
   * @throws EncoderException
   *           if encoding fails
   * @throws MediaPackageException
   *           if the mediapackage is invalid
   */
  Job mux(Track sourceVideoTrack, Track sourceAudioTrack, String profileId) throws EncoderException,
          MediaPackageException;

  /**
   * Compose two videos into one with an optional watermark.
   *
   * @param compositeTrackSize
   *          The composite track dimension
   * @param upperTrack
   *          an optional upper track of the composition
   * @param lowerTrack
   *          lower track of the composition
   * @param watermark
   *          The optional watermark attachment
   * @param profileId
   *          The encoding profile to use
   * @param background
   *          The background color
   * @return The receipt for this composite job
   * @throws EncoderException
   *           if encoding fails
   * @throws MediaPackageException
   *           if the mediapackage is invalid
   */
  Job composite(Dimension compositeTrackSize, Option<LaidOutElement<Track>> upperTrack, LaidOutElement<Track> lowerTrack,
          Option<LaidOutElement<Attachment>> watermark, String profileId, String background) throws EncoderException,
          MediaPackageException;

  /**
   * Concat multiple tracks to a single track.
   *
   * @param profileId
   *          The encoding profile to use
   * @param outputDimension
   *          The output dimensions
   * @param sameCodec Defines if lossless concat should be used
   * @param tracks
   *          an array of track to concat in order of the array
   * @return The receipt for this concat job
   * @throws EncoderException
   *           if encoding fails
   * @throws MediaPackageException
   *           if the mediapackage is invalid
   */
  Job concat(String profileId, Dimension outputDimension, boolean sameCodec, Track... tracks) throws EncoderException,
          MediaPackageException;

  /**
   * Concat multiple tracks to a single track. Required ffmpeg version 1.1
   *
   * @param profileId The encoding profile to use
   * @param outputDimension The output dimensions
   * @param outputFrameRate The output frame rate
   * @param sameCodec Defines if lossless concat should be used
   * @param tracks an array of track to concat in order of the array
   * @return The receipt for this concat job
   * @throws EncoderException if encoding fails
   * @throws MediaPackageException if the mediapackage is invalid
   */
  Job concat(String profileId, Dimension outputDimension, float outputFrameRate, boolean sameCodec, Track... tracks) throws EncoderException,
          MediaPackageException;

  /**
   * Transforms an image attachment to a video track
   *
   * @param sourceImageAttachment
   *          The source image attachment
   * @param profileId
   *          The profile to use for encoding
   * @param duration
   *          the length of the resulting video track in seconds
   * @return The receipt for this image to video job
   * @throws EncoderException
   *           if encoding fails
   * @throws MediaPackageException
   *           if the mediapackage is invalid
   */
  Job imageToVideo(Attachment sourceImageAttachment, String profileId, double duration) throws EncoderException,
          MediaPackageException;

  /**
   * Trims the given track to the given start time and duration.
   *
   * @param sourceTrack
   *          The source track
   * @param profileId
   *          The profile to use for trimming
   * @param start
   *          start time in miliseconds
   * @param duration
   *          duration in miliseconds
   * @return The receipt for this encoding job. The receipt can be used with ComposerService#getJob to
   *         obtain the status of an encoding job.
   * @throws EncoderException
   *           if trimming fails
   * @throws MediaPackageException
   *           if the mediapackage is invalid
   */
  Job trim(Track sourceTrack, String profileId, long start, long duration) throws EncoderException,
          MediaPackageException;

  /**
   * Extracts an image from the media package element identified by <code>sourceVideoTrackId</code>. The image is taken
   * at the timepoint <code>time</code> seconds into the movie.
   *
   * @param sourceTrack
   *          the source video track
   * @param profileId
   *          identifier of the encoding profile
   * @param time
   *          number of seconds into the video
   * @return the extracted image as an attachment
   * @throws EncoderException
   *           if image extraction fails
   * @throws MediaPackageException
   *           if the mediapackage is invalid
   */
  // TODO revise
  Job image(Track sourceTrack, String profileId, double... time) throws EncoderException, MediaPackageException;

  /**
   * Synchronously extracts images from the source track. The images are taken at the given timepoints (seconds into
   * the movie). Please note that synchronously doing this means, that the workload cannot be distributed amongst all
   * nodes. This should be used rarely.
   *
   * @param sourceTrack
   *          the source video track
   * @param profileId
   *          identifier of the encoding profile
   * @param time
   *          number of seconds into the video
   * @return the extracted images as attachments
   * @throws EncoderException
   *           if image extraction fails
   * @throws MediaPackageException
   *           if the mediapackage is invalid
   */
  List<Attachment> imageSync(Track sourceTrack, String profileId, double... time) throws EncoderException,
      MediaPackageException;

  /**
   * Extracts an image from the media package element identified by <code>sourceTrack</code>. The image is taken by the
   * given properties and the corresponding encoding profile.
   *
   * @param sourceTrack
   *          the source video track
   * @param profileId
   *          identifier of the encoding profile
   * @param properties
   *          the properties applied to the encoding profile
   * @return the extracted image as an attachment
   * @throws EncoderException
   *           if image extraction fails
   * @throws MediaPackageException
   *           if the mediapackage is invalid
   */
  Job image(Track sourceTrack, String profileId, Map<String, String> properties) throws EncoderException,
          MediaPackageException;

  /**
   * Converts the given image to a different image format using the specified image profile.
   *
   * @param image
   *          the image
   * @param profileId
   *          the profile to use for conversion
   * @return the job for the image conversion
   * @throws EncoderException
   *           if image conversion fails
   * @throws MediaPackageException
   *           if the mediapackage is invalid
   */
  Job convertImage(Attachment image, String profileId) throws EncoderException, MediaPackageException;


  /**
   * Synchronously converts the given image to a different image format using the specified image profile. Please note that
   * synchronously doing this means, that the workload cannot be distributed amongst all nodes.
   *
   * @param image
   *          the image
   * @param profileId
   *          the profile to use for conversion
   * @return the converted image
   * @throws EncoderException
   *           if image conversion fails
   * @throws MediaPackageException
   *           if the mediapackage is invalid
   */
  Attachment convertImageSync(Attachment image, String profileId) throws EncoderException, MediaPackageException;


  /**
   * @return All registered {@link EncodingProfile}s.
   */
  EncodingProfile[] listProfiles();

  /**
   * Gets a profile by its ID
   *
   * @param profileId
   *          The profile ID
   * @return The encoding profile, or null if no profile is registered with that ID
   */
  EncodingProfile getProfile(String profileId);

  /**
   * Encode one track to multiple other tracks in one encoding operation, using that track's audio and video streams.
   *
   * @param sourceTrack
   *          The source track
   * @param profileId
   *          The profile to use for encoding
   * @throws EncoderException
   * @throws MediaPackageException
   */
  Job parallelEncode(Track sourceTrack, String profileId) throws EncoderException, MediaPackageException;

  /**
   * Demux a multi-track source into 2 media as defined by the encoding profile, the results are flavored and tagged
   * positionally. eg: One ffmpeg operation to produce presenter/work and presentation/work
   *
   * @param sourceTrack
   * @param profileId
   * @return Receipt for this demux based on the profile
   * @throws EncoderException
   * @throws MediaPackageException
   */
  Job demux(Track sourceTrack, String profileId) throws EncoderException, MediaPackageException;

  /**
   * Reads a smil definition and create one media track in multiple delivery formats. The track in the smil is selected
   * by "trackParamGroupId" which is the paramGroup in the smil The multiple delivery formats are determined by a list
   * of encoding profiles by name. The resultant tracks will be tagged by profile name. The smil file can contain more
   * than one source track but they must have the same dimension. This is used mainly on smil.xml from the editor. There
   * is a configurable fadein/fadeout between each clip (default is 2s).
   *
   * @param smil
   *          - Describes one media (can contain multiple source in ws) and editing instructions (in out points of video
   *          clips) for concatenation into one video with transitions
   * @param trackParamGroupId
   *          - track group id to process, if missing, will process first track found in smil
   * @param mediaType
   *          - v for videoOnly, a for audioOnly, anything else is AudioVisual
   * @param profileIds
   *          - Encoding profiles for each output from this media
   * @return Receipt for this processing based on the smil file and the list of profiles
   * @throws EncoderException
   * @throws MediaPackageException
   */

  Job processSmil(Smil smil, String trackParamGroupId, String mediaType, List<String> profileIds)
          throws EncoderException, MediaPackageException;

  /**
   * Encodes a track to set of media targets as defined by a list of encoding profiles
   * 
   * @param track
   *          - video or audio track
   * @param profileIds
   *          - a list of encoding profiles by name
   * @return Receipt for this processing based on the inputs
   * @throws EncoderException
   *           if it fails
   * @throws MediaPackageException
   *           if adding files to a mediapackage produces errors
   */
  Job multiEncode(Track track, List<String> profileIds) throws EncoderException, MediaPackageException;

}
