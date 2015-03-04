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
package org.opencastproject.message.broker.api.series;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;

import java.io.IOException;
import java.io.Serializable;

/**
 * {@link Serializable} class that represents all of the possible messages sent through a SeriesService queue.
 */
public class SeriesItem implements Serializable {

  private static final long serialVersionUID = 3275142857854793612L;

  public static final String SERIES_QUEUE_PREFIX = "SERIES.";

  public static final String SERIES_QUEUE = SERIES_QUEUE_PREFIX + "QUEUE";

  private final String seriesId;
  private final String series;
  private final String acl;
  private final String propertyName;
  private final String propertyValue;
  private final Boolean optOut;
  private final Type type;

  public enum Type {
    UpdateCatalog, UpdateAcl, UpdateOptOut, UpdateProperty, Delete
  };

  /**
   * @param series
   *          The series to update.
   * @return Builds {@link SeriesItem} for updating a series.
   */
  public static SeriesItem updateCatalog(DublinCoreCatalog series) {
    return new SeriesItem(series);
  }

  /**
   * @param seriesId
   *          The unique id for the series to update.
   * @param acl
   *          The new access control list to update to.
   * @return Builds {@link SeriesItem} for updating the access control list of a series.
   */
  public static SeriesItem updateAcl(String seriesId, AccessControlList acl) {
    return new SeriesItem(seriesId, acl);
  }

  /**
   * @param seriesId
   *          The unique id for the series to update.
   * @param optOut
   *          The new opt out status.
   * @return Builds {@link SeriesItem} for updating the opt out status.
   */
  public static SeriesItem updateOptOut(String seriesId, boolean optOut) {
    return new SeriesItem(seriesId, optOut);
  }

  /**
   * @param seriesId
   *          The unique id for the series to update.
   * @param propertyName
   *          the property name
   * @param propertyValue
   *          the property value
   * @return Builds {@link SeriesItem} for updating a series property.
   */
  public static SeriesItem updateProperty(String seriesId, String propertyName, String propertyValue) {
    return new SeriesItem(seriesId, propertyName, propertyValue);
  }

  /**
   * @param seriesId
   *          The unique id of the series to update.
   * @param optedOut
   *          The opt out status.
   * @return Builds {@link SeriesItem} for updating the opt out status of a series.
   */
  public static SeriesItem delete(String seriesId, boolean optedOut) {
    return new SeriesItem(seriesId, optedOut);
  }

  /**
   * @param seriesId
   *          The unique id of the series to delete.
   * @return Builds {@link SeriesItem} for deleting a series.
   */
  public static SeriesItem delete(String seriesId) {
    return new SeriesItem(seriesId);
  }

  /**
   * Constructor to build an update series {@link SeriesItem}.
   *
   * @param series
   *          The series to update.
   */
  public SeriesItem(DublinCoreCatalog series) {
    this.seriesId = series.getFirst(DublinCore.PROPERTY_IDENTIFIER);
    try {
      this.series = series.toXmlString();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    this.acl = null;
    this.optOut = null;
    this.propertyName = null;
    this.propertyValue = null;
    this.type = Type.UpdateCatalog;
  }

  /**
   * Constructor to build an update access control list for a series {@link SeriesItem}.
   *
   * @param seriesId
   *          The id of the series to update.
   * @param acl
   *          The access control list to update.
   */
  public SeriesItem(String seriesId, AccessControlList acl) {
    this.seriesId = seriesId;
    this.series = null;
    this.acl = AccessControlParser.toJsonSilent(acl);
    this.optOut = null;
    this.propertyName = null;
    this.propertyValue = null;
    this.type = Type.UpdateAcl;
  }

  /**
   * Constructor to build a update opt out status for a series {@link SeriesItem}.
   *
   * @param seriesId
   *          The id of the series to update.
   * @param optedOut
   *          The opt out status
   */
  public SeriesItem(String seriesId, boolean optedOut) {
    this.seriesId = seriesId;
    this.series = null;
    this.acl = null;
    this.optOut = optedOut;
    this.propertyName = null;
    this.propertyValue = null;
    this.type = Type.UpdateOptOut;
  }

  /**
   * Constructor to build a update property for a series {@link SeriesItem}.
   *
   * @param seriesId
   *          The id of the series to update.
   * @param propertyName
   *          The property name.
   * @param propertyValue
   *          The property value.
   */
  public SeriesItem(String seriesId, String propertyName, String propertyValue) {
    this.seriesId = seriesId;
    this.series = null;
    this.acl = null;
    this.optOut = null;
    this.propertyName = propertyName;
    this.propertyValue = propertyValue;
    this.type = Type.UpdateProperty;
  }

  /**
   * Constructor to build a delete series {@link SeriesItem}.
   *
   * @param seriesId
   *          The id of the series to delete.
   */
  public SeriesItem(String seriesId) {
    this.seriesId = seriesId;
    this.series = null;
    this.acl = null;
    this.optOut = null;
    this.propertyName = null;
    this.propertyValue = null;
    this.type = Type.Delete;
  }

  public String getSeriesId() {
    return seriesId;
  }

  public DublinCoreCatalog getSeries() {
    return DublinCoreUtil.fromXml(series).getOrElseNull();
  }

  public AccessControlList getAcl() {
    try {
      return acl == null ? null : AccessControlParser.parseAcl(acl);
    } catch (Exception e) {
      throw new IllegalStateException();
    }
  }

  public Boolean getOptOut() {
    return optOut;
  }

  public String getPropertyName() {
    return propertyName;
  }

  public String getPropertyValue() {
    return propertyValue;
  }

  public Type getType() {
    return type;
  }

}
