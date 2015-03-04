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
package org.opencastproject.series.impl.persistence;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Enitity object for storing series in persistence storage. Series ID is stored as primary key, DUBLIN_CORE field is
 * used to store serialized Dublin core and ACCESS_CONTROL field is used to store information about access control
 * rules.
 *
 */
@Entity(name = "SeriesEntity")
@Table(name = "mh_series")
@NamedQueries({
        @NamedQuery(name = "Series.findAll", query = "select s from SeriesEntity s"),
        @NamedQuery(name = "Series.getCount", query = "select COUNT(s) from SeriesEntity s"),
        @NamedQuery(name = "seriesById", query = "select s from SeriesEntity as s where s.seriesId=:seriesId and s.organization=:organization"),
        @NamedQuery(name = "allSeriesInOrg", query = "select s from SeriesEntity as s where s.organization=:organization") })
public class SeriesEntity {

  /** Series ID, primary key */
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", length = 128)
  protected String seriesId;

  /** Series ID, primary key */
  @Id
  @Column(name = "organization", length = 128)
  protected String organization;

  /** Serialized Dublin core */
  @Lob
  @Column(name = "dublin_core", length = 65535)
  protected String dublinCoreXML;

  /** Serialized access control */
  @Lob
  @Column(name = "access_control", length = 65535)
  protected String accessControl;

  /** Opt-out status */
  @Column(name = "opt_out")
  protected boolean optOut = false;

  @ElementCollection
  @MapKeyColumn(name = "name")
  @Column(name = "value")
  @CollectionTable(name = "mh_series_property", joinColumns = {
          @JoinColumn(name = "series", referencedColumnName = "id"),
          @JoinColumn(name = "organization", referencedColumnName = "organization") })
  protected Map<String, String> properties;

  @ElementCollection
  @MapKeyColumn(name = "type")
  @Column(name = "data")
  @CollectionTable(name = "mh_series_elements", joinColumns = {
          @JoinColumn(name = "series", referencedColumnName = "id"),
          @JoinColumn(name = "organization", referencedColumnName = "organization") })
  protected Map<String, byte[]> elements;

  /**
   * Default constructor without any import.
   */
  public SeriesEntity() {
  }

  /**
   * Returns series ID.
   *
   * @return series ID
   */
  public String getSeriesId() {
    return seriesId;
  }

  /**
   * Sets series ID. ID length limit is 128 characters.
   *
   * @param seriesId
   */
  public void setSeriesId(String seriesId) {
    if (seriesId == null)
      throw new IllegalArgumentException("Series id can't be null");
    if (seriesId.length() > 128)
      throw new IllegalArgumentException("Series id can't be longer than 128 characters");
    this.seriesId = seriesId;
  }

  /**
   * Returns serialized Dublin core.
   *
   * @return serialized Dublin core
   */
  public String getDublinCoreXML() {
    return dublinCoreXML;
  }

  /**
   * Sets serialized Dublin core.
   *
   * @param dublinCoreXML
   *          serialized Dublin core
   */
  public void setSeries(String dublinCoreXML) {
    this.dublinCoreXML = dublinCoreXML;
  }

  /**
   * Returns serialized access control
   *
   * @return serialized access control
   */
  public String getAccessControl() {
    return accessControl;
  }

  /**
   * Sets serialized access control.
   *
   * @param accessControl
   *          serialized access control
   */
  public void setAccessControl(String accessControl) {
    this.accessControl = accessControl;
  }

  /**
   * @return the organization
   */
  public String getOrganization() {
    return organization;
  }

  /**
   * @param organization
   *          the organization to set
   */
  public void setOrganization(String organization) {
    this.organization = organization;
  }

  /**
   * @return the opt out status
   */
  public boolean isOptOut() {
    return optOut;
  }

  /**
   * @param optOut
   *          the opt out status to set
   */
  public void setOptOut(boolean optOut) {
    this.optOut = optOut;
  }

  public Map<String, String> getProperties() {
    return new TreeMap<String, String>(properties);
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public Map<String, byte[]> getElements() {
    return Collections.unmodifiableMap(elements);
  }

  public void addElement(String type, byte[] data) {
    elements.put(type, data);
  }

  public void removeElement(String type) {
    elements.remove(type);
  }
}
