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

package org.opencastproject.mediapackage;

import org.opencastproject.util.data.Function;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * ELement flavors describe {@link MediaPackageElement}s in a semantic way. They reveal or give at least a hint about
 * the meaning of an element.
 *
 */
@XmlJavaTypeAdapter(MediaPackageElementFlavor.FlavorAdapter.class)
public class MediaPackageElementFlavor implements Cloneable, Comparable<MediaPackageElementFlavor>, Serializable {

  /**
   * Serial version uid
   */
  private static final long serialVersionUID = 1L;

  /**
   * String representation of type
   */
  private String type = null;

  /**
   * String representation of subtype
   */
  private String subtype = null;

  /**
   * Alternate representations for type/subtype
   */
  private List<ElementTypeEquivalent> equivalents = new ArrayList<ElementTypeEquivalent>();

  /**
   * Main description
   */
  private String description = null;

  /**
   * Creates a new element type with the given type, subtype and a description.
   *
   * @param type
   *          the major type
   * @param subtype
   *          minor type
   * @param description
   *          an optional description
   */
  public MediaPackageElementFlavor(String type, String subtype, String description) {
    if (type == null)
      throw new IllegalArgumentException("Argument 'type' of element type may not be null!");
    if (subtype == null)
      throw new IllegalArgumentException("Argument 'subtype' of element type may not be null!");
    this.type = type.trim().toLowerCase();
    this.subtype = subtype.trim().toLowerCase();
    this.description = description;
  }

  public MediaPackageElementFlavor() {
  }

  /**
   * Creates a new element type with the given type and subtype.
   *
   * @param type
   *          the major type
   * @param subtype
   *          minor type
   */
  public MediaPackageElementFlavor(String type, String subtype) {
    this(type, subtype, null);
  }

  /** Constructor function for {@link #MediaPackageElementFlavor(String, String)}. */
  public static MediaPackageElementFlavor flavor(String type, String subtype) {
    return new MediaPackageElementFlavor(type, subtype);
  }

  /**
   * Returns the major type of this element type. Major types are more of a technical description.
   * <p/>
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
   * <p/>
   * For example, if the element type is a presentation movie which is represented as <code>presentation/source</code>,
   * this method will return <code>presentation</code>.
   *
   * @return the subtype
   */
  public String getSubtype() {
    return subtype;
  }

  /**
   * Returns the element type description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the element type description.
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Adds an equivalent type / subtype definition for this element type.
   *
   * @param type
   *          major type
   * @param subtype
   *          minor type
   * @throws IllegalArgumentException
   *           if any of the arguments is <code>null</code>
   */
  public void addEquivalent(String type, String subtype) throws IllegalArgumentException {
    if (type == null)
      throw new IllegalArgumentException("Type must not be null!");
    if (subtype == null)
      throw new IllegalArgumentException("Subtype must not be null!");

    equivalents.add(new ElementTypeEquivalent(type, subtype));
  }

  /**
   * @see java.lang.Object#clone()
   */
  @Override
  public MediaPackageElementFlavor clone() throws CloneNotSupportedException {
    MediaPackageElementFlavor m = (MediaPackageElementFlavor) super.clone();
    m.type = this.type;
    m.subtype = this.subtype;
    m.description = this.description;
    m.equivalents.addAll(equivalents);
    return m;
  }

  /**
   * Returns <code>true</code> if this element type is an equivalent for the specified type and subtype.
   * <p/>
   * For example, a gzipped file may have both of these element types defined, <code>application/x-compressed</code> or
   * <code>application/x-gzip</code>.
   *
   * @return <code>true</code> if this mime type is an equivalent
   */
  public boolean isEquivalentTo(String type, String subtype) {
    if (this.type.equalsIgnoreCase(type) && this.subtype.equalsIgnoreCase(subtype))
      return true;
    if (equivalents != null) {
      for (ElementTypeEquivalent equivalent : equivalents) {
        if (equivalent.matches(type, subtype))
          return true;
      }
    }
    return false;
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
  public int compareTo(MediaPackageElementFlavor m) {
    return toString().compareTo(m.toString());
  }

  /**
   * Returns the flavor as a string "type/subtype".
   */
  @Override
  public String toString() {
    return type + "/" + subtype;
  }

  /**
   * Creates a new media package element flavor.
   *
   * @param s
   *          the media package flavor
   * @return the media package element flavor object
   * @throws IllegalArgumentException
   *           if the string <code>s</code> does not contain a <t>dash</t> to divide the type from subtype.
   */
  public static MediaPackageElementFlavor parseFlavor(String s) throws IllegalArgumentException {
    if (s == null)
      throw new IllegalArgumentException("Unable to create element flavor from 'null'");
    String[] parts = s.split("/");
    if (parts.length < 2)
      throw new IllegalArgumentException("Unable to create element flavor from '" + s + "'");
    return new MediaPackageElementFlavor(parts[0], parts[1]);
  }

  public static final Function<String, MediaPackageElementFlavor> parseFlavor =
          new Function<String, MediaPackageElementFlavor>() {
            @Override public MediaPackageElementFlavor apply(String s) {
              return parseFlavor(s);
            }
          };

  /**
   * Helper class to store type/subtype equivalents for a given element type.
   */
  private class ElementTypeEquivalent implements Serializable {

    /**
     * Serial version uid
     */
    private static final long serialVersionUID = 1L;

    private String innerType;

    private String innerSubtype;

    ElementTypeEquivalent(String type, String subtype) {
      innerType = type.trim().toLowerCase();
      innerSubtype = subtype.trim().toLowerCase();
    }

    boolean matches(String type, String subtype) {
      return innerType.equalsIgnoreCase(type) && innerSubtype.equalsIgnoreCase(subtype);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((innerSubtype == null) ? 0 : innerSubtype.hashCode());
      result = prime * result + ((innerType == null) ? 0 : innerType.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof ElementTypeEquivalent))
        return false;
      ElementTypeEquivalent other = (ElementTypeEquivalent) obj;
      if (!getOuterType().equals(other.getOuterType()))
        return false;
      if (innerSubtype == null) {
        if (other.innerSubtype != null)
          return false;
      } else if (!innerSubtype.equals(other.innerSubtype))
        return false;
      if (innerType == null) {
        if (other.innerType != null)
          return false;
      } else if (!innerType.equals(other.innerType))
        return false;
      return true;
    }

    private MediaPackageElementFlavor getOuterType() {
      return MediaPackageElementFlavor.this;
    }

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
    if (other == null)
      return false;
    if (this == other)
      return true;
    if (subtype == null) {
      if (other.subtype != null && !"*".equals(other.subtype))
        return false;
    } else if (!subtype.equals(other.subtype) && (!"*".equals(subtype) && !"*".equals(other.subtype)))
      return false;
    if (type == null) {
      if (other.type != null && !"*".equals(other.type))
        return false;
    } else if (!type.equals(other.type) && (!"*".equals(type) && !"*".equals(other.type)))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((subtype == null) ? 0 : subtype.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof MediaPackageElementFlavor))
      return false;
    MediaPackageElementFlavor other = (MediaPackageElementFlavor) obj;
    if (subtype == null) {
      if (other.subtype != null)
        return false;
    } else if (!subtype.equals(other.subtype))
      return false;
    if (type == null) {
      if (other.type != null)
        return false;
    } else if (!type.equals(other.type))
      return false;
    return true;
  }

}
