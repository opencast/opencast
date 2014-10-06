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

import org.opencastproject.composer.api.EncodingProfile.MediaType;
import org.opencastproject.util.data.Option;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Interface for encoding engines like ffmpeg or telestream's episode engine. EncoderEngines are thread unsafe. Use
 * {@link EncoderEngineFactory#newEncoderEngine(EncodingProfile)} for each encoding operation.
 */
public interface EncoderEngine {

  /**
   * Removes an encoder listener from this engine.
   *
   * @param listener
   *          the encoder listener
   */
  void addEncoderListener(EncoderListener listener);

  /**
   * Adds an encoder listener to this engine.
   *
   * @param listener
   *          the encoder listener
   */
  void removeEncoderListener(EncoderListener listener);

  /**
   * Encodes a file into the specified format.
   *
   * @param mediaSource
   *          the media file to use in encoding
   * @param format
   *          the media format definition
   * @return the encoded file or none if there is no resulting file. This may be the case when doing two pass encodings
   *         where the first run does not actually create a media file
   *
   * @throws EncoderException
   *           if an error occurs during encoding
   */
  Option<File> encode(File mediaSource, EncodingProfile format, Map<String, String> properties) throws EncoderException;

  /**
   * Encodes a file into the specified format.
   *
   * @param audioSource
   *          the audio file to use in encoding
   * @param videoSource
   *          the video file to use in encoding
   * @param format
   *          the media format definition
   * @param properties
   *          the encoder properties
   * @return the encoded file or none if there is no resulting file. This may be the case when doing two pass encodings
   *         where the first run does not actually create a media file
   *
   * @throws EncoderException
   *           if an error occurs during encoding
   */
  Option<File> mux(File audioSource, File videoSource, EncodingProfile format, Map<String, String> properties)
          throws EncoderException;

  /**
   * Encodes a file into the specified format.
   *
   * @param mediaSource
   *          the media file to use in encoding
   * @param format
   *          the media format definition
   * @param start
   *          the new start time in miliseconds
   * @param duration
   *          the new duration in miliseconds
   * @return the encoded file or none if there is no resulting file. This may be the case when doing two pass encodings
   *         where the first run does not actually create a media file
   *
   * @throws EncoderException
   *           if an error occurs during encoding
   */
  Option<File> trim(File mediaSource, EncodingProfile format, long start, long duration, Map<String, String> properties)
          throws EncoderException;

  /**
   * Extracts one or more image from video stream.
   *
   * @param mediaSource
   *          video stream used for extraction
   * @param format
   *          EncodingProfile defining extraction
   * @param times
   *          times in seconds from which to extract images
   * @return list of extracted files
   * @throws EncoderException
   *           if extraction fails
   */
  List<File> extract(File mediaSource, EncodingProfile format, Map<String, String> properties, double... times)
          throws EncoderException;

  /**
   * Returns <code>true</code> if the encoder engine supports multithreading.
   * <p>
   * If this is the case, the local node will suspend the current job once the work has been submitted to the engine and
   * thereby allow other jobs to submit work as well.
   * </p>
   *
   * @return <code>true</code> if the engine supports multiple jobs at once
   */
  boolean supportsMultithreading();

  /**
   * Returns <code>true</code> if the encoder engine supports encoding a track of the given type to the specified
   * profile.
   *
   * @param profile
   *          name of the encoding profile, e. g. <code>flash.http</code>
   * @param type
   *          track type
   *
   * @return <code>true</code> if the engine supports the profile for the given track
   */
  boolean supportsProfile(String profile, MediaType type);

  /**
   * Returns <code>true</code> if the encoder needs to work on a local work copy (put into the node's work directory).
   * <p>
   * Some encoders, such as the Telestream Episode Engine, will not work reliably off a network volume, so although some
   * time will be lost with copying, it is better to do the encoding off a local hard disk.
   *
   * @return <code>true</code> if the encoder needs a local copy
   */
  boolean needsLocalWorkCopy();

}
