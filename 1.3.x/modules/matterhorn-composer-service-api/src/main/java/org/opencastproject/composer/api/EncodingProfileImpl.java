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


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

/**
 * Default implementation for encoding profiles.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "profile", namespace = "http://composer.opencastproject.org")
@XmlRootElement(name = "profile", namespace = "http://composer.opencastproject.org")
public class EncodingProfileImpl implements EncodingProfile {

  /** The profile identifier, e. g. flash.http */
  @XmlAttribute(name = "id")
  @XmlID
  protected String identifier = null;

  /** Format description */
  @XmlElement(name = "name")
  protected String name = null;

  @XmlTransient
  protected Object source;
  
  /** Format type */
  @XmlElement(name = "outputmediatype")
  protected MediaType outputType = null;

  /** Suffix */
  @XmlElement(name = "suffix")
  protected String suffix = null;

  /** Mime type */
  @XmlElement(name = "mimetype")
  protected String mimeType = null;

  /** The track type that this profile may be applied to */
  @XmlElement(name = "inputmediatype")
  protected MediaType applicableType = null;

  /** Installation-specific properties */
  @XmlElement(name = "extension")
  protected HashMap<String, String> extension = null;

  /**
   * Private, since the profile should be created using the static factory
   * method.
   * 
   * @param identifier
   *          the profile identifier
   * @param name
   *          the profile name
   */
  public EncodingProfileImpl(String identifier, String name, Object source) {
    this.identifier = identifier;
    this.name = name;
    this.source = source;
  }

  // Needed by JAXB
  public EncodingProfileImpl() {
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.EncodingProfile#getIdentifier()
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.EncodingProfile#getName()
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the output type.
   * 
   * @param type
   *          the output type
   */
  public void setOutputType(MediaType type) {
    this.outputType = type;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.EncodingProfile#getOutputType()
   */
  public MediaType getOutputType() {
    return outputType;
  }

  /**
   * Sets the suffix for encoded file names.
   * 
   * @param suffix
   *          the file suffix
   */
  public void setSuffix(String suffix) {
    this.suffix = suffix;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.EncodingProfile#getSuffix()
   */
  public String getSuffix() {
    return suffix;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.EncodingProfile#getMimeType()
   */
  public String getMimeType() {
    return mimeType;
  }

  /**
   * Sets the type that is applicable for that profile. For example, an audio
   * only-track hardly be applicable to a jpeg-slide extraction.
   * 
   * @param type
   *          applicable track type
   */
  public void setApplicableTo(MediaType type) {
    this.applicableType = type;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.EncodingProfile#getApplicableMediaType()
   */
  public MediaType getApplicableMediaType() {
    return applicableType;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.EncodingProfile#isApplicableTo(org.opencastproject.composer.api.EncodingProfile.MediaType)
   */
  public boolean isApplicableTo(MediaType type) {
    if (type == null)
      throw new IllegalArgumentException("Type must not be null");
    if (applicableType == null)
      return false;
    return applicableType.equals(type);
  }

  /**
   * Sets the extension properties for that profile. These properties may be
   * intepreted by the encoder engine.
   * 
   * @param extension
   *          the extension properties
   */
  public void setExtension(Map<String, String> extension) {
    this.extension = new HashMap<String, String>(extension);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.EncodingProfile#getExtension(java.lang.String)
   */
  public String getExtension(String key) {
    if (extension == null)
      return null;
    return extension.get(key);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.EncodingProfile#getExtensions()
   */
  public Map<String, String> getExtensions() {
    if (extension == null)
      return Collections.emptyMap();
    return extension;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.EncodingProfile#hasExtensions()
   */
  public boolean hasExtensions() {
    return extension != null && extension.size() > 0;
  }

  /**
   * Adds the given key-value pair to the extended configuration space of this
   * media profile.
   * 
   * @param key
   *          the property key
   * @param value
   *          the property value
   */
  public void addExtension(String key, String value) {
    if (key == null)
      throw new IllegalArgumentException("Argument 'key' must not be null");
    if (value == null)
      throw new IllegalArgumentException("Argument 'value' must not be null");
    if (extension == null)
      extension = new HashMap<String, String>();
    extension.put(key, value);
  }

  /**
   * Removes the specified property from the extended configuation space and
   * returns either the property value or <code>null</code> if no property was
   * found.
   * 
   * @param key
   *          the property key
   * @return the property value or <code>null</code>
   */
  public String removeExtension(String key) {
    if (extension == null)
      return null;
    return extension.remove(key);
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return identifier.hashCode();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof EncodingProfile) {
      EncodingProfile mf = (EncodingProfile) obj;
      return identifier.equals(mf.getIdentifier());
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return identifier;
  }

  /**
   * Returns the extended profile definitions.
   * 
   * @return the profile extensions
   */
  public HashMap<String, String> getExtension() {
    return extension;
  }

  /**
   * Sets the profile extensions.
   * 
   * @param extensions the extensions
   */
  public void setExtension(HashMap<String, String> extensions) {
    this.extension = extensions;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  /**
   * @return the applicableType
   */
  public MediaType getApplicableType() {
    return applicableType;
  }

  /**
   * @param applicableType the applicableType to set
   */
  public void setApplicableType(MediaType applicableType) {
    this.applicableType = applicableType;
  }
  

  /**
   * {@inheritDoc}
   * @see org.opencastproject.composer.api.EncodingProfile#getSource()
   */
  @Override
  public Object getSource() {
    return source;
  }
}
