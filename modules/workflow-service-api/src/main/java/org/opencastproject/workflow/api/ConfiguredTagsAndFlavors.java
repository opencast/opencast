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

import org.opencastproject.mediapackage.MediaPackageElementFlavor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for Handling source/target tags and flavors.
 * Consists of four lists each containing a number of configured source/target tags and flavors.
 * Tags are stored as strings, while flavors ar stored as flavor objects.
 */
public class ConfiguredTagsAndFlavors {

  private List<String> srcTags;
  private List<String> targetTags;
  private List<MediaPackageElementFlavor> srcFlavors;
  private List<MediaPackageElementFlavor> targetFlavors;

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ConfiguredTagsAndFlavors.class);

  protected ConfiguredTagsAndFlavors() {
    this.srcTags = new ArrayList<>();
    this.targetTags = new ArrayList<>();
    this.srcFlavors = new ArrayList<>();
    this.targetFlavors = new ArrayList<>();
  }

  /**
   * Return all configured source-tags as a list
   */
  public List<String> getSrcTags() {
    return this.srcTags;
  }

  /**
   * Return all configured target-tags as a list
   */
  public List<String> getTargetTags() {
    return this.targetTags;
  }

  /**
   * Return all configured source-flavors as a list
   */
  public List<MediaPackageElementFlavor> getSrcFlavors() {
    return this.srcFlavors;
  }

  /**
   * Return all configured target-flavors as a list
   */
  public List<MediaPackageElementFlavor> getTargetFlavors() {
    return this.targetFlavors;
  }

  /**
   * Return a single source tag
   * Only use this, if there should be exactly one source-tag
   */
  public String getSingleSrcTag() {
    if (this.srcTags.isEmpty()) {
      throw new IllegalStateException("No source-tag was configured!");
    }
    if (this.srcTags.size() > 1) {
      throw new IllegalStateException("More than one source-tag was configured!");
    }
    return this.srcTags.get(0);
  }

  /**
   * Return a single target tag
   * Only use this, if there should be exactly one target-tag
   */
  public String getSingleTargetTag() {
    if (this.targetTags.isEmpty()) {
      throw new IllegalStateException("No target-tag was configured!");
    }
    if (this.targetTags.size() > 1) {
      throw new IllegalStateException("More than one target-tag was configured!");
    }
    return this.targetTags.get(0);
  }

  /**
   * Return a single source flavor
   * Only use this, if there should be exactly one source-flavor
   */
  public MediaPackageElementFlavor getSingleSrcFlavor() {
    if (this.srcFlavors.isEmpty()) {
      throw new IllegalStateException("No source-flavor was configured!");
    }
    if (this.srcFlavors.size() > 1) {
      throw new IllegalStateException("More than one source-flavor was configured!");
    }
    return this.srcFlavors.get(0);
  }

  /**
   * Return a single target flavor
   * Only use this, if there should be exactly one target-flavor
   */
  public MediaPackageElementFlavor getSingleTargetFlavor() {
    if (this.targetFlavors.isEmpty()) {
      throw new IllegalStateException("No target-flavor was configured!");
    }
    if (this.targetFlavors.size() > 1) {
      throw new IllegalStateException("More than one target-flavor was configured!");
    }
    return this.targetFlavors.get(0);
  }

  /**
   * Setter for srcTags list
   */
  protected void setSrcTags(List<String> srcTags) {
    this.srcTags = srcTags;
    logger.debug("Added " + srcTags.size() + " elements to srcTags list");
  }

  /**
   * Setter for targetTags list
   */
  protected void setTargetTags(List<String> targetTags) {
    this.targetTags = targetTags;
    logger.debug("Added " + targetTags.size() + " elements to targetTags list");
  }

  /**
   * Setter for srcFlavors list
   */
  protected void setSrcFlavors(List<MediaPackageElementFlavor> srcFlavors) {
    this.srcFlavors = srcFlavors;
    logger.debug("Added " + srcFlavors.size() + " elements to srcFlavors list");
  }

  /**
   * Setter for targetFlavor list
   */
  protected void setTargetFlavors(List<MediaPackageElementFlavor> targetFlavors) {
    this.targetFlavors = targetFlavors;
    logger.debug("Added " + targetFlavors.size() + " elements to targetFlavors list");
  }
}
