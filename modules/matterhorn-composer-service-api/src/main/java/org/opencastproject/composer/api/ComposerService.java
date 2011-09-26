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
package org.opencastproject.composer.api;

import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;

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
   * @return The receipt for this encoding job. The receipt can be used with {@link ComposerService#getJob(long)} to
   *         obtain the status of an encoding job.
   * @throws EncoderException
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
   * @return The receipt for this encoding job. The receipt can be used with {@link ComposerService#getJob(long)} to
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
  Job image(Track sourceTrack, String profileId, long... time) throws EncoderException, MediaPackageException;

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
   * Insert captions in media package element identified by <code>mediaTrack</code> from catalog which contains
   * captions.
   * 
   * @param mediaTrack
   *          media track to which captions will be embedded
   * @param captions
   *          captions to be inserted
   * @param language
   *          caption language
   * @return Receipt for this embedding job
   * @throws EmbedderException
   *           if exception occurs during embedding process
   * @throws MediaPackageException
   *           if the track is invalid
   */
  Job captions(Track mediaTrack, Catalog[] captions) throws EmbedderException, MediaPackageException;

  /**
   * Insert captions in media package element identified by <code>mediaTrack</code> from catalog which contains
   * captions.
   * 
   * @param mediaTrack
   *          media track to which captions will be embedded
   * @param watermark
   *          Watermark image to be applied to the video
   * @param profileId
   *          identifier of the encoding profile
   * @return Receipt for this embedding job
   * @throws EmbedderException
   *           if exception occurs during embedding process
   * @throws MediaPackageException
   *           if the track is invalid
   */
  Job watermark(Track mediaTrack, String watermark, String profileId) throws EncoderException, MediaPackageException;  
  
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

}
