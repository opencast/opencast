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

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * The presentation element describes where a media package can be consumed.
 * An entry of this type id the result of a distribution to a distribution channel.
 */
@XmlJavaTypeAdapter(PublicationImpl.Adapter.class)
public interface Publication extends MediaPackageElement {

  /** Returns the channel id. */
  String getChannel();

  /**
   * Returns the tracks that are part of this publication.
   *
   * @return the tracks
   */
  Track[] getTracks();

  /**
   * Adds a track to this publication.
   *
   * @param track
   *          the track to add
   */
  void addTrack(Track track);

  /**
   * Returns the attachments that are part of this publication.
   *
   * @return the attachments
   */
  Attachment[] getAttachments();

  /**
   * Adds an attachment to this publication.
   *
   * @param attachment
   *          the attachment to add
   */
  void addAttachment(Attachment attachment);

  void removeAttachmentById(String attachmentId);

  /**
   * Returns the catalogs associated with this publication.
   *
   * @return the catalogs
   */
  Catalog[] getCatalogs();

  /**
   * Adds a catalog to this publication.
   *
   * @param catalog
   *          the catalog to add
   */
  void addCatalog(Catalog catalog);

}
