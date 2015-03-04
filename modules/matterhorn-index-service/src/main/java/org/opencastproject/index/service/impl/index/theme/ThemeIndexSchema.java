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

package org.opencastproject.index.service.impl.index.theme;

import org.opencastproject.matterhorn.search.impl.IndexSchema;

/**
 * Interface defining the mapping between data and field names in the search index.
 */
public interface ThemeIndexSchema extends IndexSchema {

  /** The unique identifier */
  String UID = "uid";

  /** The organization that owns this theme. */
  String ORGANIZATION = "organization";

  String OBJECT = "object";

  /** The date and time the theme was created in UTC format e.g. 2011-07-16T20:39:05Z */
  String CREATION_DATE = "creation_date";

  /** Whether this theme is the default for this organization */
  String DEFAULT = "default";

  /** The description of the theme */
  String DESCRIPTION = "description";

  /** The name of the theme. */
  String NAME = "name";

  /** The username of the creator of this theme */
  String CREATOR = "creator";

  /** Whether the bumber should be used for this theme. */
  String BUMPER_ACTIVE = "bumper_active";

  /** The id of the file to use as a bumper for this theme. */
  String BUMPER_FILE = "bumper_file";

  /** Whether to use the trailer associated with this theme. */
  String TRAILER_ACTIVE = "trailer_active";

  /** The id of the file to use as a trailer for this theme. */
  String TRAILER_FILE = "trailer_file";

  /** Whether to use a title slide with this theme. */
  String TITLE_SLIDE_ACTIVE = "title_slide_active";

  /** The metadata fields to include in the title slide for this theme. */
  String TITLE_SLIDE_METADATA = "title_slide_metadata";

  /** The id of the file to use as a background to the title slides. */
  String TITLE_SLIDE_BACKGROUND = "title_slide_background";

  /** Whether to use a license slide in this theme. */
  String LICENSE_SLIDE_ACTIVE = "license_slide_active";

  /** The license description to use on the license slide for this theme. */
  String LICENSE_SLIDE_DESCRIPTION = "license_slide_description";

  /** The id of the file to use as a background to the license slide. */
  String LICENSE_SLIDE_BACKGROUND = "license_slide_background";

  /** Whether the watermark should be applied to videos with this theme. */
  String WATERMARK_ACTIVE = "watermark_active";

  /** The id of the file to use as a watermark for the videos. */
  String WATERMARK_FILE = "watermark_file";

  /** The position to place the watermark on the video */
  String WATERMARK_POSITION = "watermark_position";

}
