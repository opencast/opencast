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

package org.opencastproject.metadata.mpeg7;

import java.util.Iterator;

/**
 * Root of an mpeg-7 document.
 *
 * <pre>
 * &lt;element name=&quot;Mpeg7&quot;&gt;
 *   &lt;complexType&gt;
 *       &lt;complexContent&gt;
 *           &lt;extension base=&quot;mpeg7:Mpeg7Type&quot;&gt;
 *               &lt;choice&gt;
 *                   &lt;element name=&quot;DescriptionUnit&quot; type=&quot;mpeg7:Mpeg7BaseType&quot;/&gt;
 *                   &lt;element name=&quot;Description&quot; type=&quot;mpeg7:CompleteDescriptionType&quot; minOccurs=&quot;1&quot; maxOccurs=&quot;unbounded&quot;/&gt;
 *               &lt;/choice&gt;
 *           &lt;/extension&gt;
 *       &lt;/complexContent&gt;
 *   &lt;/complexType&gt;
 * &lt;/element&gt;
 * </pre>
 */
public interface Mpeg7 {

  /**
   * Returns an iteration of the multimedia content container contained in this mpeg-7 document.
   *
   * @return the multimedia content container
   */
  Iterator<MultimediaContent<? extends MultimediaContentType>> multimediaContent();

  /**
   * Returns the multimedia content container element for tracks of the given type (either <code>Audio></code>,
   * <code>Video</code> or <code>Audiovisual</code>).
   *
   *
   * @return the multimedia content container of the specified type
   */
  MultimediaContent<? extends MultimediaContentType> getMultimediaContent(MultimediaContent.Type type);

  /**
   * Adds audio content to the catalog.
   *
   * @param id
   *          the audio track id
   * @param time
   *          the audio track time constraints
   * @param locator
   *          the track locator
   */
  Audio addAudioContent(String id, MediaTime time, MediaLocator locator);

  /**
   * Removes the audio content with the specified id.
   *
   * @param id
   *          the content id
   */
  Audio removeAudioContent(String id);

  /**
   * Returns <code>true</code> if the catalog contains multimedia content of type <code>AudioType</code>.
   *
   * @return <code>true</code> if audio content is contained
   */
  boolean hasAudioContent();

  /**
   * Returns an iteration of the tracks of type <code>Audio</code>.
   *
   * @return the audio tracks
   */
  Iterator<Audio> audioContent();

  /**
   * Adds video content to the catalog.
   *
   * @param id
   *          the video track id
   * @param time
   *          the video track time constraints
   * @param locator
   *          the track locator
   */
  Video addVideoContent(String id, MediaTime time, MediaLocator locator);

  /**
   * Removes the video content with the specified id.
   *
   * @param id
   *          the content id
   */
  Video removeVideoContent(String id);

  /**
   * Returns <code>true</code> if the catalog contains multimedia content of type <code>VideoType</code>.
   *
   * @return <code>true</code> if video content is contained
   */
  boolean hasVideoContent();

  /**
   * Returns an iteration of the tracks of type <code>Video</code>.
   *
   * @return the video tracks
   */
  Iterator<Video> videoContent();

  /**
   * Adds audiovisual content to the catalog.
   *
   * @param id
   *          the track id
   * @param time
   *          the track's time constraints
   * @param locator
   *          track's locator
   */
  AudioVisual addAudioVisualContent(String id, MediaTime time, MediaLocator locator);

  /**
   * Removes the audiovisual content with the specified id.
   *
   * @param id
   *          the content id
   */
  AudioVisual removeAudioVisualContent(String id);

  /**
   * Returns <code>true</code> if the catalog contains multimedia content of type <code>AudioVisualType</code>.
   *
   * @return <code>true</code> if audiovisual content is contained
   */
  boolean hasAudioVisualContent();

  /**
   * Returns an iteration of the tracks of type <code>AudioVisual</code>.
   *
   * @return the audiovisual tracks
   */
  Iterator<AudioVisual> audiovisualContent();

  /**
   * Returns the audio track with the given id or <code>null</code> if the track does not exist.
   *
   * @param id
   *          the audio track id
   * @return the audio track
   */
  Audio getAudioById(String id);

  /**
   * Returns the video track with the given id or <code>null</code> if the track does not exist.
   *
   * @param id
   *          the video track id
   * @return the video track
   */
  Video getVideoById(String id);

  /**
   * Returns the audiovisual track with the given id or <code>null</code> if the track does not exist.
   *
   * @param id
   *          the track id
   * @return the track
   */
  AudioVisual getAudioVisualById(String id);

}
