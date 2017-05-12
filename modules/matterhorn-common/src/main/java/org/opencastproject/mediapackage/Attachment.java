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

import org.opencastproject.mediapackage.attachment.AttachmentImpl;

import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * This interface describes methods and fields for attachments as part of a media package.
 */
@XmlJavaTypeAdapter(AttachmentImpl.Adapter.class)
public interface Attachment extends MediaPackageElement {

  /** Media package element type */
  Type TYPE = Type.Attachment;

  /** Element flavor definition */
  MediaPackageElementFlavor FLAVOR = new MediaPackageElementFlavor("attachment", "(unkown)", "Unspecified attachment");


  /**
   * Returns <code>true</code> if the media package element contains the given property.
   *
   * @param propertyName
   *          name of the property
   * @return <code>true</code> if the element contains the property
   */
  boolean containsProperty(String propertyName);

  /**
   * Returns a map containing the properties for this media package element or an empty map if there are no properties.
   *
   * @return the properties
   */
  Map<String, String> getProperties();

  /**
   * Returns the value of the property for this media package element or null if it does not contain that property.
   *
   * @param propertyName name of the property
   * @return the properties
   */
  String getPropertyValue(String propertyName);

  /**
   * Adds a property to the media package element.
   *
   * @param name
   *          name of the property
   * @param value
   *          value of the property
   */
  void addProperty(String name, String value);

  /**
   * Removes the property from the media package element.
   *
   * @param propertyName
   *          name of the property
   * @return true if the media package element contained the specified property
   *
   */
  boolean removeProperty(String propertyName);

  /**
   * Removes all properties associated with this element
   */
  void clearProperties();
}
