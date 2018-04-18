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

package org.opencastproject.message.broker.api.series;

import org.opencastproject.message.broker.api.MessageItem;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreXmlFormat;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;

import java.io.IOException;
import java.io.Serializable;

/**
 * {@link Serializable} class that represents all of the possible messages sent through a SeriesService queue.
 */
public final class SeriesItem implements MessageItem, Serializable {

  private static final long serialVersionUID = 3275142857854793612L;

  public static final String SERIES_QUEUE_PREFIX = "SERIES.";

  public static final String SERIES_QUEUE = SERIES_QUEUE_PREFIX + "QUEUE";

  private final Type type;
  private final String seriesId;
  private final String series;
  private final String acl;
  private final String propertyName;
  private final String propertyValue;
  private final Boolean optOut;
  private final String element;
  private final String elementType;

  public enum Type {
    UpdateCatalog, UpdateElement, UpdateAcl, UpdateOptOut, UpdateProperty, Delete
  };

  /**
   * @param series
   *          The series to update.
   * @return Builds {@link SeriesItem} for updating a series.
   */
  public static SeriesItem updateCatalog(DublinCoreCatalog series) {
    return new SeriesItem(Type.UpdateCatalog, null, series, null, null, null, null, null, null);
  }

  /**
   * @param seriesId
   *          The unique id for the series to update.
   * @param type
   *          The type of series element.
   * @param data
   *          The series element data.
   * @return Builds {@link SeriesItem} for updating series element.
   */
  public static SeriesItem updateElement(String seriesId, String type, String data) {
    return new SeriesItem(Type.UpdateElement, seriesId, null, null, null, null, null, type, data);
  }

  /**
   * @param seriesId
   *          The unique id for the series to update.
   * @param acl
   *          The new access control list to update to.
   * @return Builds {@link SeriesItem} for updating the access control list of a series.
   */
  public static SeriesItem updateAcl(String seriesId, AccessControlList acl) {
    return new SeriesItem(Type.UpdateAcl, seriesId, null, AccessControlParser.toJsonSilent(acl),
            null, null, null, null, null);
  }

  /**
   * @param seriesId
   *          The unique id for the series to update.
   * @param optOut
   *          The new opt out status.
   * @return Builds {@link SeriesItem} for updating the opt out status.
   */
  public static SeriesItem updateOptOut(String seriesId, boolean optOut) {
    return new SeriesItem(Type.UpdateOptOut, seriesId, null, null, null, null, optOut, null, null);
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
    return new SeriesItem(Type.UpdateProperty, seriesId, null, null, propertyName, propertyValue, null, null, null);
  }

  /**
   * @param seriesId
   *          The unique id of the series to update.
   * @param optedOut
   *          The opt out status.
   * @return Builds {@link SeriesItem} for updating the opt out status of a series.
   */
  public static SeriesItem delete(String seriesId, boolean optedOut) {
    return new SeriesItem(Type.UpdateOptOut, seriesId, null, null, null, null, optedOut, null, null);
  }

  /**
   * @param seriesId
   *          The unique id of the series to delete.
   * @return Builds {@link SeriesItem} for deleting a series.
   */
  public static SeriesItem delete(String seriesId) {
    return new SeriesItem(Type.Delete, seriesId, null, null, null, null, null, null, null);
  }

  /**
   * Constructor to build an {@link SeriesItem} with given parameters.
   *
   * @param type
   *          The update type.
   * @param seriesId
   *          The series ID to update. If you provide both, the seriesId and the series DublinCore catalog,
   *          the seriesId must match the value of {@link DublinCore.PROPERTY_IDENTIFIER}.
   * @param series
   *          The series DublinCore catalog to update. The value of {@link DublinCore.PROPERTY_IDENTIFIER} must match
   *          the value of seriesId if you provide both.
   * @param acl
   *          The series ACL to update. Note: the series ID must be also provided.
   * @param propertyName
   *          The name of the series property to update. Note: the series ID and property value must be also provided.
   * @param propertyValue
   *          The value of the series property to update. Note: the series ID and property name must be also provided.
   * @param optOut
   *          The series opt out status to update. Note: the series ID must be also provided.
   * @param elementType
   *          The type of the series element to update. Note: the series ID and element must be also provided.
   * @param element
   *          The series element to update. Note: the series ID and element type must be also provided.
   *
   * @throws IllegalStateException
   *          If the series ID and the series are not provided or the series ID and the value of
   *          {@link DublinCore.PROPERTY_IDENTIFIER} in the series catalog does not match.
   */
  private SeriesItem(Type type, String seriesId, DublinCoreCatalog series, String acl,
          String propertyName, String propertyValue, Boolean optOut, String elementType, String element) {
    if (seriesId != null && series != null && !seriesId.equals(series.getFirst(DublinCore.PROPERTY_IDENTIFIER)))
      throw new IllegalStateException("Provided series ID and dublincore series ID does not match");

    this.type = type;
    if (series != null) {
      try {
        this.series = series.toXmlString();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    } else {
      this.series = null;
    }
    if (seriesId != null)
      this.seriesId = seriesId;
    else if (series != null)
      this.seriesId = series.getFirst(DublinCore.PROPERTY_IDENTIFIER);
    else
      throw new IllegalStateException("Neither series nor series ID is provided");
    this.acl = acl;
    this.propertyName = propertyName;
    this.propertyValue = propertyValue;
    this.optOut = optOut;
    this.elementType = elementType;
    this.element = element;
  }

  @Override
  public String getId() {
    return seriesId;
  }

  public Type getType() {
    return type;
  }

  public String getSeriesId() {
    return seriesId;
  }

  public DublinCoreCatalog getMetadata() {
    return DublinCoreXmlFormat.readOpt(series).orNull();
  }

  public DublinCoreCatalog getExtendedMetadata() {
    try {
      return DublinCoreXmlFormat.read(element);
    } catch (Exception ex) {
      return null;
    }
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


  public String getElement() {
    return element;
  }

  public String getElementType() {
    return elementType;
  }
}
