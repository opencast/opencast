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
import org.opencastproject.util.data.Option;

import java.util.Map;

/**
 * Encodes media and (optionally) periodically alerts a statusService endpoint of the status of this encoding job.
 */
public interface ComposerService {

  String JOB_TYPE = "org.opencastproject.composer";

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
   * Concat multiple tracks to a single track. Required ffmpeg version 1.1
   *
   * @param profileId
   *          The encoding profile to use
   * @param outputDimension
   *          The output dimensions
   * @param source files all have the same codecs and timebase - no re-encode
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
   * @param source files all have the same codecs and timebase - no re-encode
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

}
