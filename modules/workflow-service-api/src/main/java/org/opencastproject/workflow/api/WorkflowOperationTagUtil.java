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

package org.opencastproject.workflow.api;

import org.opencastproject.mediapackage.Track;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class WorkflowOperationTagUtil {
  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(WorkflowOperationTagUtil.class);

  public static class TagDiff {
    private final List<String> removeTags;
    private final List<String> addTags;
    private final List<String> overrideTags;

    TagDiff(final List<String> removeTags, final List<String> addTags, final List<String> overrideTags) {
      this.removeTags = removeTags;
      this.addTags = addTags;
      this.overrideTags = overrideTags;
    }
  }

  private WorkflowOperationTagUtil() {

  }

  private static final String PLUS = "+";
  private static final String MINUS = "-";

  public static TagDiff createTagDiff(final String tagList) {
    final String[] targetTags = StringUtils.split(tagList, ",");

    final List<String> removeTags = new ArrayList<>();
    final List<String> addTags = new ArrayList<>();
    final List<String> overrideTags = new ArrayList<>();

    for (final String tag : targetTags) {
      if (tag.startsWith(MINUS)) {
        removeTags.add(tag);
      } else if (tag.startsWith(PLUS)) {
        addTags.add(tag);
      } else {
        overrideTags.add(tag);
      }
    }

    return new TagDiff(removeTags, addTags, overrideTags);
  }

  public static void applyTagDiff(final TagDiff td, final Track t) {
    // Add the target tags
    if (!td.overrideTags.isEmpty()) {
      t.clearTags();
      for (final String tag : td.overrideTags) {
        logger.trace("Tagging composed track with '{}'", tag);
        t.addTag(tag);
      }
    } else {
      for (final String tag : td.removeTags) {
        logger.trace("Remove tagging '{}' from composed track", tag);
        t.removeTag(tag.substring(MINUS.length()));
      }
      for (final String tag : td.addTags) {
        logger.trace("Add tagging '{}' to composed track", tag);
        t.addTag(tag.substring(PLUS.length()));
      }
    }
  }
}
