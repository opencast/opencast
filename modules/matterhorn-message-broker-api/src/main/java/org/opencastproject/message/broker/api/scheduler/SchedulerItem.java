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
package org.opencastproject.message.broker.api.scheduler;

import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.scheduler.api.SchedulerService.ReviewStatus;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.Properties;

/**
 * {@link Serializable} class that represents all of the possible messages sent through a SchedulerService queue.
 */
public class SchedulerItem implements Serializable {
  private static final long serialVersionUID = 6061069989788904237L;

  public static final String SCHEDULER_QUEUE_PREFIX = "SCHEDULER.";

  public static final String SCHEDULER_QUEUE = SCHEDULER_QUEUE_PREFIX + "QUEUE";

  private final String mediaPackageId;
  private final String event;
  private final String properties;
  private final String acl;
  private final Boolean optOut;
  private final Boolean blacklisted;
  private final String reviewStatus;
  private final Date reviewDate;
  private final Type type;

  public enum Type {
    UpdateCatalog, UpdateProperties, UpdateAcl, UpdateOptOut, UpdateBlacklist, UpdateReviewStatus, Delete
  };

  /**
   * @param mediaPackageId
   *          The unique id for the event to update.
   * @param event
   *          The event details to update to.
   * @return Builds {@link SchedulerItem} for updating a scheduled event.
   */
  public static SchedulerItem updateCatalog(String mediaPackageId, DublinCoreCatalog event) {
    return new SchedulerItem(mediaPackageId, event);
  }

  /**
   * @param mediaPackageId
   *          The unique id for the event to update.
   * @param properties
   *          The new properties to update to.
   * @return Builds {@link SchedulerItem} for updating the properties of an event.
   */
  public static SchedulerItem updateProperties(String mediaPackageId, Properties properties) {
    return new SchedulerItem(mediaPackageId, properties);
  }

  /**
   * @param mediaPackageId
   *          The unique id of the event to delete.
   * @return Builds {@link SchedulerItem} for deleting an event.
   */
  public static SchedulerItem delete(final String mediaPackageId) {
    return new SchedulerItem(mediaPackageId);
  }

  /**
   * @param mediaPackageId
   *          the mediapackage id
   * @param accessControlList
   *          the access control list
   * @return Builds {@link SchedulerItem} for updating the access control list of an event.
   */
  public static Serializable updateAcl(String mediaPackageId, AccessControlList accessControlList) {
    return new SchedulerItem(mediaPackageId, accessControlList);
  }

  /**
   * @param mediaPackageId
   *          the mediapackage id
   * @param optOut
   *          the opt out status
   * @return Builds {@link SchedulerItem} for updating the opt out status of an event.
   */
  public static SchedulerItem updateOptOut(String mediaPackageId, boolean optOut) {
    return new SchedulerItem(mediaPackageId, optOut);
  }

  /**
   * @param mediaPackageId
   *          the mediapackage id
   * @param blacklisted
   *          the blacklist status
   * @return Builds {@link SchedulerItem} for updating the blacklist status of an event.
   */
  public static SchedulerItem updateBlacklist(String mediapackageId, boolean blacklisted) {
    return new SchedulerItem(blacklisted, mediapackageId);
  }

  /**
   * @param mediaPackageId
   *          the mediapackage id
   * @param reviewStatus
   *          the review status
   * @param reviewDate
   *          the review date
   * @return Builds {@link SchedulerItem} for updating the review status of an event.
   */
  public static SchedulerItem updateReviewStatus(String mediaPackageId, ReviewStatus reviewStatus, Date reviewDate) {
    return new SchedulerItem(mediaPackageId, reviewStatus, reviewDate);
  }

  /**
   * Constructor to build an update event {@link SchedulerItem}.
   *
   * @param event
   *          The event details to update.
   */
  public SchedulerItem(String mediaPackageId, DublinCoreCatalog event) {
    this.mediaPackageId = mediaPackageId;
    try {
      this.event = event.toXmlString();
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    this.properties = null;
    this.acl = null;
    this.optOut = null;
    this.blacklisted = null;
    this.reviewStatus = null;
    this.reviewDate = null;
    this.type = Type.UpdateCatalog;
  }

  /**
   * Constructor to build an update properties for an event {@link SchedulerItem}.
   *
   * @param mediaPackageId
   *          The id of the event to update.
   * @param properties
   *          The properties to update.
   */
  public SchedulerItem(String mediaPackageId, Properties properties) {
    this.mediaPackageId = mediaPackageId;
    this.event = null;
    try {
      this.properties = serializeProperties(properties);
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    this.acl = null;
    this.reviewStatus = null;
    this.reviewDate = null;
    this.optOut = null;
    this.blacklisted = null;
    this.type = Type.UpdateProperties;
  }

  /**
   * Constructor to build a delete event {@link SchedulerItem}.
   *
   * @param mediaPackageId
   *          The id of the event to delete.
   */
  public SchedulerItem(String mediaPackageId) {
    this.mediaPackageId = mediaPackageId;
    this.event = null;
    this.properties = null;
    this.acl = null;
    this.optOut = null;
    this.blacklisted = null;
    this.reviewStatus = null;
    this.reviewDate = null;
    this.type = Type.Delete;
  }

  /**
   * Constructor to build an update access control list event {@link SchedulerItem}.
   *
   * @param mediaPackageId
   *          The mediapackage id
   * @param accessControlList
   *          The access control list
   */
  public SchedulerItem(String mediaPackageId, AccessControlList accessControlList) {
    this.mediaPackageId = mediaPackageId;
    this.event = null;
    this.properties = null;
    try {
      this.acl = AccessControlParser.toJson(accessControlList);
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    this.reviewStatus = null;
    this.reviewDate = null;
    this.optOut = null;
    this.blacklisted = null;
    this.type = Type.UpdateAcl;
  }

  /**
   * Constructor to build an update opt out status event {@link SchedulerItem}.
   *
   * @param mediaPackageId
   *          The mediapackage id
   * @param optOut
   *          The opt out status
   */
  public SchedulerItem(String mediaPackageId, boolean optOut) {
    this.mediaPackageId = mediaPackageId;
    this.event = null;
    this.properties = null;
    this.acl = null;
    this.optOut = optOut;
    this.blacklisted = null;
    this.reviewStatus = null;
    this.reviewDate = null;
    this.type = Type.UpdateOptOut;
  }

  /**
   * Constructor to build an update blacklist status event {@link SchedulerItem}.
   *
   * @param blacklisted
   *          The blacklist status
   * @param mediaPackageId
   *          The mediapackage id
   */
  public SchedulerItem(boolean blacklisted, String mediaPackageId) {
    this.mediaPackageId = mediaPackageId;
    this.event = null;
    this.properties = null;
    this.acl = null;
    this.optOut = null;
    this.blacklisted = blacklisted;
    this.reviewStatus = null;
    this.reviewDate = null;
    this.type = Type.UpdateBlacklist;
  }

  /**
   * Constructor to build an update review status event {@link SchedulerItem}.
   *
   * @param mediaPackageId
   *          The mediapackage id
   * @param reviewStatus
   *          The review status
   * @param reviewDate
   *          The review date
   */
  public SchedulerItem(String mediaPackageId, ReviewStatus reviewStatus, Date reviewDate) {
    this.mediaPackageId = mediaPackageId;
    this.event = null;
    this.properties = null;
    this.acl = null;
    this.optOut = null;
    this.blacklisted = null;
    this.reviewStatus = reviewStatus.toString();
    this.reviewDate = reviewDate;
    this.type = Type.UpdateReviewStatus;
  }

  public String getMediaPackageId() {
    return mediaPackageId;
  }

  public DublinCoreCatalog getEvent() {
    if (StringUtils.isBlank(event))
      return null;

    return DublinCoreUtil.fromXml(event).getOrElseNull();
  }

  public Properties getProperties() {
    try {
      return parseProperties(properties);
    } catch (IOException e) {
      throw new IllegalStateException();
    }
  }

  public AccessControlList getAcl() {
    try {
      return acl == null ? null : AccessControlParser.parseAcl(acl);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public Boolean getOptOut() {
    return optOut;
  }

  public Boolean getBlacklisted() {
    return blacklisted;
  }

  public ReviewStatus getReviewStatus() {
    return ReviewStatus.valueOf(reviewStatus);
  }

  public Date getReviewDate() {
    return reviewDate;
  }

  public Type getType() {
    return type;
  }

  /**
   * Serializes Properties to String.
   *
   * @param caProperties
   *          Properties to be serialized
   * @return serialized properties
   * @throws IOException
   *           if serialization fails
   */
  private String serializeProperties(Properties caProperties) throws IOException {
    StringWriter writer = new StringWriter();
    caProperties.store(writer, "Capture Agent specific data");
    return writer.toString();
  }

  /**
   * Parses Properties represented as String.
   *
   * @param serializedProperties
   *          properties to be parsed.
   * @return parsed properties
   * @throws IOException
   *           if parsing fails
   */
  private Properties parseProperties(String serializedProperties) throws IOException {
    Properties caProperties = new Properties();
    caProperties.load(new StringReader(serializedProperties));
    return caProperties;
  }

}
