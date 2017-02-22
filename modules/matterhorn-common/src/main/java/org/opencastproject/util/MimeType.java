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


package org.opencastproject.util;

import static org.opencastproject.util.EqualsUtil.eqObj;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;

import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * This class implements the mime type. Note that mime types should not be instantiated directly but be retreived from
 * the mime type registry {@link MimeTypes}.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "mimetype", namespace = "http://mediapackage.opencastproject.org")
@XmlJavaTypeAdapter(MimeType.Adapter.class)
public final class MimeType implements Comparable<MimeType>, Serializable {
  private static final Logger logger = LoggerFactory.getLogger(MimeType.class);

  /** Serial version UID */
  private static final long serialVersionUID = -2895494708659187394L;

  /** String representation of type */
  private final String type;

  /** String representation of subtype */
  private final String subtype;

  /** Alternate representations for type/subtype */
  private final List<MimeType> equivalents;

  /** List of suffixes, the first is the main one. */
  private final List<String> suffixes;

  /** Main description */
  private final Option<String> description;

  /** The mime type flavor */
  private final Option<String> flavor;

  /** The mime type flavor description */
  private final Option<String> flavorDescription;

  /**
   * Creates a new mime type with the given type and subtype.
   *
   * @param type
   *          the major type
   * @param subtype
   *          minor type
   */
  private MimeType(String type, String subtype, List<String> suffixes,
                  List<MimeType> equivalents,
                  Option<String> description,
                  Option<String> flavor, Option<String> flavorDescription) {
    this.type = type;
    this.subtype = subtype;
    this.suffixes = suffixes;
    this.equivalents = equivalents;
    this.description = description;
    this.flavor = flavor;
    this.flavorDescription = flavorDescription;
  }

  public static MimeType mimeType(String type, String subtype, List<String> suffixes,
                                  List<MimeType> equivalents,
                                  Option<String> description,
                                  Option<String> flavor, Option<String> flavorDescription) {
    return new MimeType(type, subtype, suffixes, equivalents, description, flavor, flavorDescription);
  }

  public static MimeType mimeType(String type, String subtype, String suffix) {
    return new MimeType(type, subtype, list(suffix), Collections.<MimeType>nil(), none(""), none(""), none(""));
  }

  public static MimeType mimeType(String type, String subtype) {
    return new MimeType(type, subtype, Collections.<String>nil(), Collections.<MimeType>nil(), none(""), none(""), none(""));
  }

  public static MimeType determineMimeType(String filename) {
    String extension = filename.substring(filename.lastIndexOf(".") + 1);
    switch (extension.toLowerCase()) {
      case "html":
      case "htm": return mimeType("text", "html", "html");
      case "txt":
      case "text": return mimeType("text", "plain", "txt");
      case "css": return mimeType("text", "css", "css");
      case "xml": return mimeType("text", "xml", "xml");
      case "srt":
      case "vtt": return mimeType("text", "vtt", "vtt");
      case "gif": return mimeType("image", "gif", "gif");
      case "ief": return mimeType("image", "ief", "ief");
      case "jpg":
      case "jpe":
      case "jpeg": return mimeType("image", "jpeg", "jpg");
      case "tiff":
      case "tif": return mimeType("image", "tiff", "tif");
      case "png": return mimeType("image", "png", "png");
      case "xwd": return mimeType("image", "x-xwindowdump", "xwd");
      case "svg": return mimeType("image", "svg+xml", "svg");
      case "json": return mimeType("application", "json", "json");
      case "eps":
      case "es":
      case "ai": return mimeType("application", "postscript", "eps");
      case "rtf": return mimeType("application", "rtf", "rtf");
      case "tex": return mimeType("application", "x-tex", "tex");
      case "texi":
      case "texinfo": return mimeType("application", "x-textinfo", "texi");
      case "tr":
      case "roff":
      case "t": return mimeType("application", "x-troff", "tr");
      case "js": return mimeType("application", "x-javascript", "js");
      case "ttf": return mimeType("application", "x-font-truetype", "ttf");
      case "otf": return mimeType("application", "x-font-opentype", "otf");
      case "woff": return mimeType("application", "font-woff", "woff");
      case "eot": return mimeType("application", "vnd.ms-fontobject", "eot");
      case "m3u8": return mimeType("application", "x-mpegURL", "m3u8");
      case "f4m": return mimeType("application", "f4m+xml", "f4m");
      case "mpd": return mimeType("application", "dash+xml", "mpd");
      case "au": return mimeType("audio", "basic", "au");
      case "midi":
      case "mid": return mimeType("audio", "midi", "mid");
      case "aifc": return mimeType("audio", "x-aifc", "aifc");
      case "aiff":
      case "aif": return mimeType("audio", "x-aiff", "aif");
      case "flac": return mimeType("audio", "x-flac", "flac");
      case "wav": return mimeType("audio", "x-wav", "wav");
      case "mp3": return mimeType("audio", "mpeg", "mp3");
      case "aac":
      case "m4a": return mimeType("audio", "x-aac", "m4a");
      case "mpg":
      case "mpe":
      case "mpeg": return mimeType("video", "mpeg", "mpg");
      case "qt":
      case "mov": return mimeType("video", "quicktime", "mov");
      case "avi": return mimeType("video", "x-msvideo", "avi");
      case "m4v":
      case "mp4": return mimeType("video", "mp4", "mp4");
      case "mkv": return mimeType("video", "x-matroska" , "mkv");
      case "webm": return mimeType("video", "webm", "webm");
      case "zip": return mimeType("application", "zip", "zip");


      default: return null;
    }
  }

  /**
   * Returns the major type of this mimetype.
   * <p>
   * For example, if the mimetype is ISO Motion JPEG 2000 which is represented as <code>video/mj2</code>, this method
   * will return <code>video</code>.
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Returns the minor type of this mimetype.
   * <p>
   * For example, if the mimetype is ISO Motion JPEG 2000 which is represented as <code>video/mj2</code>, this method
   * will return <code>mj2</code>.
   *
   * @return the subtype
   */
  public String getSubtype() {
    return subtype;
  }

  /**
   * Returns the main suffix for this mime type, that identifies files containing data of this flavor.
   * <p>
   * For example, files with the suffix <code>mj2</code> will contain data of type <code>video/mj2</code>.
   *
   * @return the file suffix
   */
  public Option<String> getSuffix() {
    return mlist(suffixes).headOpt();
  }

  /**
   * Returns the registered suffixes for this mime type, that identify files containing data of this flavor. Note that
   * the list includes the main suffix returned by <code>getSuffix()</code>.
   * <p>
   * For example, files containing ISO Motion JPEG 2000 may have file suffixes <code>mj2</code> and <code>mjp2</code>.
   *
   * @return the registered file suffixes
   */
  public String[] getSuffixes() {
    return suffixes.toArray(new String[suffixes.size()]);
  }

  /**
   * Returns <code>true</code> if the mimetype supports the specified suffix.
   *
   * @return <code>true</code> if the suffix is supported
   */
  public boolean supportsSuffix(String suffix) {
    return suffixes.contains(suffix.toLowerCase());
  }

  /**
   * Returns the mime type description.
   *
   * @return the description
   */
  public Option<String> getDescription() {
    return this.description;
  }

  /**
   * Returns the flavor of this mime type.
   * <p>
   * A flavor is a hint on a specialized variant of a general mime type. For example, a dublin core file will have a
   * mime type of <code>text/xml</code>. Adding a flavor of <code>mpeg-7</code> gives an additional hint on the file
   * contents.
   *
   * @return the file's flavor
   */
  public Option<String> getFlavor() {
    return flavor;
  }

  /**
   * Returns the flavor description.
   *
   * @return the flavor description
   */
  public Option<String> getFlavorDescription() {
    return flavorDescription;
  }

  /**
   * Returns <code>true</code> if the file has the given flavor associated.
   *
   * @return <code>true</code> if the file has that flavor
   */
  public boolean hasFlavor(String flavor) {
    if (flavor == null)
      return false;
    return flavor.equalsIgnoreCase(flavor);
  }

  /**
   * Returns the MimeType as a string of the form <code>type/subtype</code>
   * @deprecated use {@link #toString()} instead
   */
  public String asString() {
    return toString();
  }

  /** Two mime types are considered equal if type and subtype are equal. */
  public boolean eq(MimeType other) {
    return eq(other.getType(), other.getSubtype());
  }

  /** Two mime types are considered equal if type and subtype are equal. */
  public boolean eq(String type, String subtype) {
    return this.type.equalsIgnoreCase(type) && this.subtype.equalsIgnoreCase(subtype);
  }

  /** {@link #eq(org.opencastproject.util.MimeType)} as a function. */
  // CHECKSTYLE:OFF
  public final Function<MimeType, Boolean> eq = new Function<MimeType, Boolean>() {
    @Override public Boolean apply(MimeType other) {
      return eq(other);
    }
  };
  // CHECKSTYLE:ON

  /**
   * Returns <code>true</code> if this mime type is an equivalent for the specified type and subtype.
   * <p>
   * For example, a gzipped file may have both of these mime types defined, <code>application/x-compressed</code> or
   * <code>application/x-gzip</code>.
   *
   * @return <code>true</code> if this mime type is equal
   */
  public boolean isEquivalentTo(String type, String subtype) {
    return eq(type, subtype) || mlist(equivalents).exists(eq);
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(MimeType m) {
    return toString().compareTo(m.toString());
  }

  /**
   * Returns the MimeType as a string of the form <code>type/subtype</code>
   */
  @Override
  public String toString() {
    return type + "/" + subtype;
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(type, subtype);
  }

  @Override
  public boolean equals(Object that) {
    return (this == that) || (that instanceof MimeType && eqFields((MimeType) that));
  }

  private boolean eqFields(MimeType that) {
    return eqObj(this.type, that.type)
            && eqObj(this.subtype, that.subtype);
  }

  static class Adapter extends XmlAdapter<String, MimeType> {
    @Override
    public String marshal(MimeType mimeType) throws Exception {
      return mimeType.type + "/" + mimeType.subtype;
    }

    @Override
    public MimeType unmarshal(String str) throws Exception {
      try {
        return MimeTypes.parseMimeType(str);
      } catch (Exception e) {
        logger.info("unable to parse mimetype {}", str);
        return null;
      }
    }
  }
}
