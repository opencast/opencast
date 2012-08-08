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

import org.apache.commons.lang.StringUtils;

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
  protected Map<String, String> extension = new HashMap<String, String>();

  /**
   * Private, since the profile should be created using the static factory method.
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
  @Override
  public String getIdentifier() {
    return identifier;
  }

  /**
   * Sets the identifier
   * 
   * @param id
   *          the identifier
   */
  public void setIdentifier(String id) {
    identifier = id;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.EncodingProfile#getName()
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Sets the profile name
   * 
   * @param name
   *          the profile name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.EncodingProfile#getSource()
   */
  @Override
  public Object getSource() {
    return source;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.EncodingProfile#getOutputType()
   */
  @Override
  public MediaType getOutputType() {
    return outputType;
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
   * @see org.opencastproject.composer.api.EncodingProfile#getSuffix()
   */
  @Override
  public String getSuffix() {
    return suffix;
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
   * @see org.opencastproject.composer.api.EncodingProfile#getMimeType()
   */
  @Override
  public String getMimeType() {
    return mimeType;
  }

  /**
   * Sets the Mimetype.
   * 
   * @param mimeType
   *          the Mimetype
   */
  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.EncodingProfile#getApplicableMediaType()
   */
  @Override
  public MediaType getApplicableMediaType() {
    return applicableType;
  }

  /**
   * Sets the applicable type.
   * 
   * @param applicableType
   *          the applicableType to set
   */
  public void setApplicableType(MediaType applicableType) {
    this.applicableType = applicableType;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.EncodingProfile#isApplicableTo(org.opencastproject.composer.api.EncodingProfile.MediaType)
   */
  @Override
  public boolean isApplicableTo(MediaType type) {
    if (type == null)
      throw new IllegalArgumentException("Type must not be null");
    return type.equals(applicableType);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.EncodingProfile#getExtension(java.lang.String)
   */
  @Override
  public String getExtension(String key) {
    return extension.get(key);
  }

  /**
   * Adds the given key-value pair to the extended configuration space of this media profile.
   * 
   * @param key
   *          the property key
   * @param value
   *          the property value
   */
  public void addExtension(String key, String value) {
    if (StringUtils.isBlank(key))
      throw new IllegalArgumentException("Argument 'key' must not be null");
    if (value == null)
      throw new IllegalArgumentException("Argument 'value' must not be null");
    extension.put(key, value);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.EncodingProfile#getExtensions()
   */
  @Override
  public Map<String, String> getExtensions() {
    return extension;
  }

  /**
   * Sets the extension properties for that profile. These properties may be intepreted by the encoder engine.
   * 
   * @param extension
   *          the extension properties
   */
  public void setExtensions(Map<String, String> extension) {
    if (extension == null)
      return;
    this.extension = extension;
  }

  /**
   * Removes the specified property from the extended configuation space and returns either the property value or
   * <code>null</code> if no property was found.
   * 
   * @param key
   *          the property key
   * @return the property value or <code>null</code>
   */
  public String removeExtension(String key) {
    return extension.remove(key);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.composer.api.EncodingProfile#hasExtensions()
   */
  @Override
  public boolean hasExtensions() {
    return extension != null && extension.size() > 0;
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

}
