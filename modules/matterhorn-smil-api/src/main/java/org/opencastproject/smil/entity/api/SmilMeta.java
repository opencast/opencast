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
package org.opencastproject.smil.entity.api;

/**
 * Represent a SMIL meta element.
 */
public interface SmilMeta extends SmilObject {

  String SMIL_META_NAME_WORKFLOW_ID = "workflow-id";
  String SMIL_META_NAME_MEDIA_PACKAGE_ID = "media-package-id";
  String SMIL_META_NAME_CATALOG_ID = "catalog-id";
  String SMIL_META_NAME_TRACK_ID = "track-id";
  String SMIL_META_NAME_TRACK_DURATION = "track-duration";

  /**
   * Returns meta content.
   *
   * @return the content
   */
  String getContent();

  /**
   * Returns meta name.
   *
   * @return the name
   */
  String getName();
}
