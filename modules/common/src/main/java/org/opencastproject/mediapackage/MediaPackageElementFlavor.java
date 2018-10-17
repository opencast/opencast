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

package org.opencastproject.mediapackage;

import static java.lang.String.format;

import org.opencastproject.util.data.Function;

import java.io.Serializable;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * ELement flavors describe {@link MediaPackageElement}s in a semantic way. They reveal or give at least a hint about
 * the meaning of an element.
 *
 */
@XmlJavaTypeAdapter(MediaPackageElementFlavor.FlavorAdapter.class)
public class MediaPackageElementFlavor implements Cloneable, Comparable<MediaPackageElementFlavor>, Serializable {

  /**
   * Wildcard character used in type and subtype
   */
  public static final String WILDCARD = "*";

  /**
   * Serial version uid
   */
  private static final long serialVersionUID = 1L;

  /**
   * Character that separates both parts of a flavor
   */
  private static final String SEPARATOR = "/";

  /**
   * String representation of type
   */
  private String type = null;

  /**
   * String representation of subtype
   */
  private String subtype = null;


  private MediaPackageElementFlavor() {
  }

  /**
   * Creates a new element type with the given type and subtype.
   *
   * @param type
   *          the major type
   * @param subtype
   *          minor type
   * @param description
   *          an optional description
   */
  public MediaPackageElementFlavor(String type, String subtype) {
    this.type = checkPartSyntax(type);
    this.subtype = checkPartSyntax(subtype);
  }

  /**
   * Checks that any of the parts this flavor consists of abide to the syntax restrictions
   *
   * @param part
   * @return
   */
  private String checkPartSyntax(String part) {
    // Parts may not be null
    if (part == null)
      throw new IllegalArgumentException("Flavor parts may not be null!");

    // Parts may not contain the flavor separator character
    if (part.contains(SEPARATOR))
      throw new IllegalArgumentException(
              format("Invalid flavor part \"%s\". Flavor parts may not contain '%s'!", part, SEPARATOR));

    // Parts may not contain leading and trailing blanks, and may only consist of lowercase letters
    String adaptedPart = part.trim().toLowerCase();

    // Parts may not be empty
    if (adaptedPart.isEmpty())
      throw new IllegalArgumentException(
              format("Invalid flavor part \"%s\". Flavor parts may not be blank or empty!", part));

    return adaptedPart;
  }

  /** Constructor function for {@link #MediaPackageElementFlavor(String, String)}. */
  public static MediaPackageElementFlavor flavor(String type, String subtype) {
    return new MediaPackageElementFlavor(type, subtype);
  }

  /**
   * Returns the major type of this element type. Major types are more of a technical description.
   * The major type is never null.
   * <p>
   * For example, if the element type is a presentation movie which is represented as <code>presentation/source</code>,
   * this method will return <code>track</code>.
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Returns the minor type of this element type. Minor types define the meaning.
   * The minor type is never null.
   * <p>
   * For example, if the element type is a presentation movie which is represented as <code>presentation/source</code>,
   * this method will return <code>presentation</code>.
   *
   * @return the subtype
   */
  public String getSubtype() {
    return subtype;
  }

  /**
   * "Applies" this flavor to the given target flavor. E.g. applying '*\/preview' to 'presenter/source' yields
   * 'presenter/preview', applying 'presenter/*' to 'foo/source' yields 'presenter/source', and applying 'foo/bar' to
   * 'presenter/source' yields 'foo/bar'.
   *
   * @param target The target flavor to apply this flavor to.
   *
   * @return The resulting flavor.
   */
  public MediaPackageElementFlavor applyTo(MediaPackageElementFlavor target) {
    String subtype = this.subtype;
    String type = this.type;
    if ("*".equals(this.subtype)) {
      subtype = target.getSubtype();
    }
    if ("*".equals(this.type)) {
      type = target.getType();
    }
    return new MediaPackageElementFlavor(type, subtype);
  }

  /**
   * @see java.lang.Object#clone()
   */
  @Override
  public MediaPackageElementFlavor clone() throws CloneNotSupportedException {
    MediaPackageElementFlavor m = (MediaPackageElementFlavor) super.clone();
    m.type = this.type;
    m.subtype = this.subtype;
    return m;
  }

  /**
   * Defines equality between flavors and strings.
   *
   * @param flavor
   *          string of the form "type/subtype"
   */
  public boolean eq(String flavor) {
    return flavor != null && flavor.equals(toString());
  }

  /**
   * @see java.lang.String#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(MediaPackageElementFlavor m) {
    return toString().compareTo(m.toString());
  }

  /**
   * Returns the flavor as a string "type/subtype".
   */
  @Override
  public String toString() {
    return type + SEPARATOR + subtype;
  }

  /**
   * Creates a new media package element flavor.
   *
   * @param s
   *          the media package flavor
   * @return the media package element flavor object
   * @throws IllegalArgumentException
   *           if the string <code>s</code> does not contain a <i>dash</i> to divide the type from subtype.
   */
  public static MediaPackageElementFlavor parseFlavor(String s) throws IllegalArgumentException {
    if (s == null)
      throw new IllegalArgumentException("Unable to create element flavor from 'null'");
    String[] parts = s.split(SEPARATOR);
    if (parts.length != 2)
      throw new IllegalArgumentException(format("Unable to create element flavor from \"%s\"", s));
    return new MediaPackageElementFlavor(parts[0], parts[1]);
  }

  public static final Function<String, MediaPackageElementFlavor> parseFlavor = new Function<String, MediaPackageElementFlavor>() {
    @Override
    public MediaPackageElementFlavor apply(String s) {
      return parseFlavor(s);
    }
  };

  /** Check if <code>type</code> is a {@link #WILDCARD wildcard}. */
  public static boolean isWildcard(String type) {
    return WILDCARD.equals(type);
  }

  /** Check if type or subtype of <code>flavor</code> is a wildcard. */
  public static boolean hasWildcard(MediaPackageElementFlavor flavor) {
    return isWildcard(flavor.getType()) || isWildcard(flavor.getSubtype());
  }

  /**
   * JAXB adapter implementation.
   */
  static class FlavorAdapter extends XmlAdapter<String, MediaPackageElementFlavor> {
    @Override
    public String marshal(MediaPackageElementFlavor flavor) throws Exception {
      if (flavor == null) {
        return null;
      } else {
        return flavor.toString();
      }
    }

    @Override
    public MediaPackageElementFlavor unmarshal(String str) throws Exception {
      MediaPackageElementFlavor f = parseFlavor(str);
      return f;
    }
  }

  public boolean matches(MediaPackageElementFlavor other) {
    return (other != null) && matchParts(type, other.type) && matchParts(subtype, other.subtype);
  }

  private boolean matchParts(String part1, String part2) {
    return part1.equals(part2) || isWildcard(part1) || isWildcard(part2);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + subtype.hashCode();
    result = prime * result + type.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object other) {
    return (other != null) && (other instanceof MediaPackageElementFlavor)
      && type.equals(((MediaPackageElementFlavor) other).type)
      && subtype.equals(((MediaPackageElementFlavor) other).subtype);
  }

}
