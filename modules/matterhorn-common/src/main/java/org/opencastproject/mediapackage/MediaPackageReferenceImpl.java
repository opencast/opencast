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

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Default implementation for a {@link MediaPackageReference}.
 */
public class MediaPackageReferenceImpl implements MediaPackageReference {

  /** Convenience reference that matches any media package */
  public static final MediaPackageReference ANY_MEDIAPACKAGE = new MediaPackageReferenceImpl(TYPE_MEDIAPACKAGE, ANY);

  /** Convenience reference that matches the current media package */
  public static final MediaPackageReference SELF_MEDIAPACKAGE = new MediaPackageReferenceImpl(TYPE_MEDIAPACKAGE, SELF);

  /** Convenience reference that matches any series */
  public static final MediaPackageReference ANY_SERIES = new MediaPackageReferenceImpl(TYPE_SERIES, "*");

  /** The reference identifier */
  protected String identifier = null;

  /** The reference type */
  protected String type = null;

  /** External representation */
  private String externalForm = null;

  /** The properties that describe this reference */
  private Map<String, String> properties = null;

  /**
   * Creates a reference to the containing media package (<code>self</code>).
   */
  public MediaPackageReferenceImpl() {
    this(TYPE_MEDIAPACKAGE, SELF);
  }

  /**
   * Creates a reference to the specified media package.
   *
   * @param mediaPackage
   *          the media package to refer to
   */
  public MediaPackageReferenceImpl(MediaPackage mediaPackage) {
    if (mediaPackage == null)
      throw new IllegalArgumentException("Parameter media package must not be null");
    type = TYPE_MEDIAPACKAGE;
    if (mediaPackage.getIdentifier() != null)
      identifier = mediaPackage.getIdentifier().toString();
    else
      identifier = SELF;
    properties = new HashMap<String, String>();
  }

  /**
   * Creates a reference to the specified media package element.
   * <p>
   * Note that the referenced element must already be part of the media package, otherwise a
   * <code>MediaPackageException</code> will be thrown as the object holding this reference is added to the media
   * package.
   *
   * @param mediaPackageElement
   *          the media package element to refer to
   */
  public MediaPackageReferenceImpl(MediaPackageElement mediaPackageElement) {
    if (mediaPackageElement == null)
      throw new IllegalArgumentException("Parameter media package element must not be null");
    this.type = mediaPackageElement.getElementType().toString().toLowerCase();
    this.identifier = mediaPackageElement.getIdentifier();
    if (identifier == null)
      throw new IllegalArgumentException("Media package element must have an identifier");
    this.properties = new HashMap<String, String>();
  }

  /**
   * Creates a reference to the entity identified by <code>type</code> and <code>identifier</code>.
   *
   * @param type
   *          the reference type
   * @param identifier
   *          the reference identifier
   */
  public MediaPackageReferenceImpl(String type, String identifier) {
    if (type == null)
      throw new IllegalArgumentException("Parameter type must not be null");
    if (identifier == null)
      throw new IllegalArgumentException("Parameter identifier must not be null");
    this.type = type;
    this.identifier = identifier;
    this.properties = new HashMap<String, String>();
  }

  /**
   * Returns a media package reference from the given string.
   *
   * @return the media package reference
   * @throws IllegalArgumentException
   *           if the string is malformed
   */
  public static MediaPackageReference fromString(String reference) throws IllegalArgumentException {
    if (reference == null)
      throw new IllegalArgumentException("Reference is null");

    MediaPackageReference ref = null;

    String[] parts = reference.split(";");
    String elementReference = parts[0];

    // Check for special reference
    if ("self".equals(elementReference))
      ref = new MediaPackageReferenceImpl(MediaPackageReference.TYPE_MEDIAPACKAGE, "self");
    else {
      String[] elementReferenceParts = elementReference.split(":");
      if (elementReferenceParts.length != 2)
        throw new IllegalArgumentException("Reference " + reference + " is malformed");
      ref = new MediaPackageReferenceImpl(elementReferenceParts[0], elementReferenceParts[1]);
    }

    // Process the reference properties
    for (int i = 1; i < parts.length; i++) {
      String[] propertyParts = parts[i].split("=");
      if (propertyParts.length != 2)
        throw new IllegalStateException("malformatted reference properties");
      String key = propertyParts[0];
      String value = propertyParts[1];
      ref.setProperty(key, value);
    }

    return ref;
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageReference#getIdentifier()
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageReference#getType()
   */
  public String getType() {
    return type;
  }

  /**
   * @return the properties
   */
  public Map<String, String> getProperties() {
    return properties;
  }

  /**
   * @param properties
   *          the properties to set
   */
  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.MediaPackageReference#getProperty(java.lang.String)
   */
  @Override
  public String getProperty(String key) {
    return properties.get(key);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.mediapackage.MediaPackageReference#setProperty(java.lang.String, java.lang.String)
   */
  @Override
  public void setProperty(String key, String value) {
    if (value == null)
      this.properties.remove(key);
    this.properties.put(key, value);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageReference#matches(org.opencastproject.mediapackage.MediaPackageReference)
   */
  public boolean matches(MediaPackageReference reference) {
    if (reference == null)
      return false;

    // type
    if (!type.equals(reference.getType()))
      return false;

    // properties
    if (properties != null && !properties.equals(reference.getProperties()))
      return false;
    else if (reference.getProperties() != null && !reference.getProperties().equals(properties))
      return false;

    // identifier
    if (identifier.equals(reference.getIdentifier()))
      return true;
    else if (ANY.equals(identifier) || ANY.equals(reference.getIdentifier()))
      return true;
    else if (SELF.equals(identifier) || SELF.equals(reference.getIdentifier()))
      return true;

    return false;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#clone()
   */
  @Override
  public Object clone() {
    MediaPackageReferenceImpl clone = new MediaPackageReferenceImpl(type, identifier);
    clone.getProperties().putAll(properties);
    return clone;
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof MediaPackageReference))
      return false;
    MediaPackageReference ref = (MediaPackageReference) obj;
    return type.equals(ref.getType()) && identifier.equals(ref.getIdentifier());
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    if (externalForm == null) {
      StringBuffer buf = new StringBuffer();
      if (TYPE_MEDIAPACKAGE.equals(type) && SELF.equals(identifier)) {
        buf.append("self");
      } else {
        buf.append(type);
        buf.append(":");
        buf.append(identifier);
      }
      if (properties.size() > 0) {
        for (Map.Entry<String, String> entry : properties.entrySet()) {
          buf.append(";");
          buf.append(entry.getKey());
          buf.append("=");
          buf.append(entry.getValue());
        }
      }
      externalForm = buf.toString();
    }
    return externalForm;
  }

  public static class Adapter extends XmlAdapter<String, MediaPackageReference> {
    @Override
    public String marshal(MediaPackageReference ref) throws Exception {
      if (ref == null)
        return null;
      return ref.toString();
    }

    @Override
    public MediaPackageReference unmarshal(String ref) throws Exception {
      if (ref == null)
        return null;
      return MediaPackageReferenceImpl.fromString(ref);
    }
  }
}
