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

import java.util.List;
import javax.xml.bind.annotation.XmlType;
import java.util.Map;

/**
 * An encoding format encapsulates all the relevant configuration data for
 * encoding a media file to a certain encoding formats.
 */
public interface EncodingProfile {

  /**
   * Input and output formats.
   */
  @XmlType(name = "media-type", namespace = "http://composer.opencastproject.org")
  public enum MediaType {
    // Nothing is a special type that indicates that the encoding process does not produce any media
    Audio, Visual, AudioVisual, Stream, EnhancedAudio, Image, ImageSequence, Cover, Nothing;

    /**
     * Try to parse the argument <code>type</code> and produce a
     * {@link MediaType} out of it.
     *
     * @param type
     *          the type string representation
     * @return a track type
     */
    public static MediaType parseString(String type) {
      if (type == null || type.length() == 0)
        throw new IllegalArgumentException(type
                + " is not a valid track type definition");
      if ("audiovisual".equalsIgnoreCase(type))
        return AudioVisual;
      else if ("enhancedaudio".equalsIgnoreCase(type))
        return EnhancedAudio;
      else if ("imagesequence".equalsIgnoreCase(type))
        return ImageSequence;
      else {
        type = type.substring(0, 1).toUpperCase()
                + type.substring(1).toLowerCase();
      }
      return MediaType.valueOf(type.trim());
    }

  }

  /**
   * Returns the unique format identifier.
   *
   * @return the format identifier
   */
  String getIdentifier();

  /**
   * Returns the encoding format's name.
   *
   * @return the format name
   */
  String getName();


  /**
   * Returns the source object that provided this encoding profile
   *
   * @return the source object that provided this profile
   */
  Object getSource();

  /**
   * Returns the encoding format's media type, which is either video (plus
   * audio) or audio only.
   *
   * @return the format type
   */
  MediaType getOutputType();

  /**
   * Returns a suffix of the files. First tag found used if tags are used but not provided in the request
   *
   * @return the suffix
   */
  String getSuffix();

  /**
   * Returns a suffix of the files for a certain tag.
   *
   * @param tag a tag that describes the aoutput file
   * @return the suffix
   */
  String getSuffix(String tag);

  /**
   * Returns a list of the tags for output files used in this request
   * @return a list of the used tags
   */
  List<String> getTags();

  /**
   * Returns the media type's mime type.
   *
   * @return the mime type
   */
  String getMimeType();

  /**
   * Returns the media format that can be used with this encoding profile.
   *
   * @return the applicable input format
   */
  MediaType getApplicableMediaType();

  /**
   * Returns <code>true</code> if the profile is applicable for the given track
   * type.
   *
   * @param type
   *          the track type
   * @return <code>true</code> if the profile is applicable
   */
  boolean isApplicableTo(MediaType type);

  /**
   * Returns <code>true</code> if additional properties have been specified.
   *
   * @return <code>true</code> if there are additional properties
   */
  boolean hasExtensions();

  /**
   * Returns the extension specified by <code>key</code> or <code>null</code> if
   * no such key was defined.
   * <p>
   * Note that <code>key</code> must not contain the media format prefix, so if
   * the configured entry was <tt>mediaformat.format.xyz.test</tt>, then the key
   * to access the value must simply be <code>test</code>.
   * </p>
   *
   * @param key
   *          the extension key
   * @return the value or <code>null</code>
   */
  String getExtension(String key);

  /**
   * Returns a map containing the additional properties or an empty map if no
   * additional properties were found.
   *
   * @return the additional properties
   */
  Map<String, String> getExtensions();

}
