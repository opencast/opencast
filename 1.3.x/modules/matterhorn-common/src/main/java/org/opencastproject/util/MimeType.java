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

package org.opencastproject.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
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
public final class MimeType implements Cloneable, Comparable<MimeType>, Serializable {
  private static final Logger logger = LoggerFactory.getLogger(MimeType.class);

  /** Serial version UID */
  private static final long serialVersionUID = -2895494708659187394L;

  /** String representation of type */
  private String type = null;

  /** String representation of subtype */
  private String subtype = null;

  /** Alternate representations for type/subtype */
  private List<MIMEEquivalent> equivalents = null;

  /** Main file suffix */
  private String suffix = null;

  /** List of suffixes */
  private List<String> suffixes = null;

  /** Main description */
  private String description = null;

  /** The mime type flavor */
  private String flavor = null;

  /** The mime type flavor description */
  private String flavorDescription = null;

  public MimeType() {
    this("", "", null);
  }

  /**
   * Creates a new mime type with the given type and subtype.
   * 
   * @param type
   *          the major type
   * @param subtype
   *          minor type
   */
  public MimeType(String type, String subtype) {
    this(type, subtype, null);
  }

  /**
   * Creates a new mime type with the given type, subtype and main file suffix.
   * 
   * @param type
   *          the major type
   * @param subtype
   *          minor type
   * @param suffix
   *          main file suffix
   */
  public MimeType(String type, String subtype, String suffix) {
    if (type == null)
      throw new IllegalArgumentException("Argument 'type' of mime type may not be null!");
    if (subtype == null)
      throw new IllegalArgumentException("Argument 'subtype' of mime type may not be null!");
    equivalents = new ArrayList<MIMEEquivalent>();
    this.type = type.trim().toLowerCase().replaceAll("/", "");
    this.subtype = subtype.trim().toLowerCase().replaceAll("/", "");
    this.suffixes = new ArrayList<String>();
    if (suffix != null) {
      suffix = suffix.trim().toLowerCase().replaceAll("/", "");
      addSuffix(suffix);
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
  public String getSuffix() {
    return suffix;
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
   * Adds the suffix to the list of file suffixes.
   * 
   * @param suffix
   *          the suffix
   */
  public void addSuffix(String suffix) {
    if (suffix != null && !suffixes.contains(suffix))
      suffixes.add(suffix.trim().toLowerCase());
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
  public String getDescription() {
    return this.description;
  }

  /**
   * Sets the mime type description.
   * 
   * @param description
   */
  public void setDescription(String description) {
    this.description = description;
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
  public String getFlavor() {
    return flavor;
  }

  /**
   * Returns the flavor description.
   * 
   * @return the flavor description
   */
  public String getFlavorDescription() {
    return flavorDescription;
  }

  /**
   * Sets the flavor of this mime type.
   * 
   * @param flavor
   *          the flavor
   */
  public void setFlavor(String flavor) {
    setFlavor(flavor, null);
  }

  /**
   * Sets the flavor of this mime type along with a flavor description.
   * 
   * @param flavor
   *          the flavor
   * @param description
   *          the flavor description
   */
  public void setFlavor(String flavor, String description) {
    if (flavor == null)
      throw new IllegalArgumentException("Flavor must not be null!");
    this.flavor = flavor.trim();
    this.flavorDescription = description;
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
   * Adds an equivalent type / subtype definition for this mime type.
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

    if (equivalents == null)
      equivalents = new ArrayList<MIMEEquivalent>();
    equivalents.add(new MIMEEquivalent(type, subtype));
  }

  /**
   * Returns the MimeType as a string of the form <code>type/subtype</code>
   */
  public String asString() {
    return type + "/" + subtype;
  }

  /**
   * @see java.lang.Object#clone()
   */
  @Override
  public MimeType clone() throws CloneNotSupportedException {
    MimeType m = new MimeType(type, subtype, suffix);
    m.equivalents.addAll(equivalents);
    m.suffixes.addAll(suffixes);
    m.flavor = flavor;
    m.flavorDescription = flavorDescription;
    return m;
  }

  /**
   * Returns <code>true</code> if this mime type is an equivalent for the specified type and subtype.
   * <p>
   * For example, a gzipped file may have both of these mime types defined, <code>application/x-compressed</code> or
   * <code>application/x-gzip</code>.
   * 
   * @return <code>true</code> if this mime type is equal
   */
  public boolean isEquivalentTo(String type, String subtype) {
    if (this.type.equalsIgnoreCase(type) && this.subtype.equalsIgnoreCase(subtype))
      return true;
    if (equivalents != null) {
      for (MIMEEquivalent equivalent : equivalents) {
        if (equivalent.matches(type, subtype))
          return true;
      }
    }
    return false;
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(MimeType m) {
    return toString().compareTo(m.toString());
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    if (flavorDescription != null)
      return flavorDescription;
    else if (description != null)
      return description;
    else
      return type + "/" + subtype;
  }

  /**
   * Helper class to store type/subtype equivalents for a given mime type.
   */
  private class MIMEEquivalent {

    private String innerType;

    private String innerSubtype;

    MIMEEquivalent(String type, String subtype) {
      innerType = type.trim().toLowerCase();
      innerSubtype = subtype.trim().toLowerCase();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      MIMEEquivalent other = (MIMEEquivalent) obj;
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

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((innerSubtype == null) ? 0 : innerSubtype.hashCode());
      result = prime * result + ((innerType == null) ? 0 : innerType.hashCode());
      return result;
    }

    boolean matches(String type, String subtype) {
      return innerType.equalsIgnoreCase(type) && innerSubtype.equalsIgnoreCase(subtype);
    }

    private MimeType getOuterType() {
      return MimeType.this;
    }

  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((subtype == null) ? 0 : subtype.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof MimeType))
      return false;
    MimeType other = (MimeType) obj;
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
