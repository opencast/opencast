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

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * To be implemented by everyone who wishes to contribute to the media package manifest.
 */
public interface ManifestContributor {

  /**
   * This method returns an XML serialization of the object to be stored in the media package manifest. It should be
   * possible to reconstruct the object from this data.
   * <p>
   * For creating <em>{@link MediaPackageElement}s</em> from a manifest, please use
   * {@link MediaPackageElementBuilder#elementFromManifest(org.w3c.dom.Node, MediaPackageSerializer)}. All other objects
   * shall provide their own implementation specific reconstruction mechanism.
   *
   * @param document
   *          the parent
   * @param serializer
   *          the media package serializer
   * @return the object's xml representation
   * @throws MediaPackageException
   *           if the mediapackage can't be serialized
   */
  Node toManifest(Document document, MediaPackageSerializer serializer) throws MediaPackageException;

}
