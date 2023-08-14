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
  MediaPackageElementFlavor FLAVOR = new MediaPackageElementFlavor("attachment", "(unkown)");

  /**
   * Returns a map containing the properties for this media package element or an empty map if there are no properties.
   *
   * @return the properties
   */
  Map<String, String> getProperties();
}
