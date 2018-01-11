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
package org.opencastproject.oaipmh.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity(name = "OaiPmhElementEntity")
@Table(name = "oc_oaipmh_elements")
public class OaiPmhElementEntity {

  /** The auto generated unique database ID */
  @Id
  @GeneratedValue
  @Column(name = "id")
  private long id;

  /** The type of the media package element. Only Catalog and Attachment are supported currently. */
  @Column(name = "element_type", length = 16, nullable = false)
  private String elementType;

  /** The flavor of the media package element */
  @Column(name = "flavor", length = 255, nullable = false)
  private String flavor;

  /** The XML serialized media package element content */
  @Lob
  @Column(name = "xml", length = 65535, nullable = false)
  private String xml;

  /** The OAI-PMH entity belongs to this element */
  @ManyToOne(optional = false)
  @JoinColumns({
    @JoinColumn(name = "mp_id", referencedColumnName = "mp_id", nullable = false, table = "oc_oaipmh_elements"),
    @JoinColumn(name = "organization", referencedColumnName = "organization", nullable = false, table = "oc_oaipmh_elements"),
    @JoinColumn(name = "repo_id", referencedColumnName = "repo_id", nullable = false, table = "oc_oaipmh_elements")
  })
  private OaiPmhEntity oaiPmhEntity;

  public OaiPmhElementEntity() { }

  /**
   * Constructor
   *
   * @param elementType the type of the media package element. Only Catalog and Attachment are supported currently
   * @param flavor the flavor of the media package element
   * @param xml the XML serialized media package element content
   */
  public OaiPmhElementEntity(String elementType, String flavor, String xml) {
    this.elementType = elementType;
    this.flavor = flavor;
    this.xml = xml;
  }

  /**
   * @return the type of the media package element
   */
  public String getElementType() {
    return elementType;
  }

  /**
   * Set thetype of the media package element. Only Catalog and Attachment are supported currently
   * @param elementType the media package elementy type to set
   */
  public void setElementType(String elementType) {
    this.elementType = elementType;
  }

  /**
   * @return the media package element flavor
   */
  public String getFlavor() {
    return flavor;
  }

  /**
   * @param flavor the media package element flavor to set
   */
  public void setFlavor(String flavor) {
    this.flavor = flavor;
  }

  /**
   * @return the XML serialized content of the media package element
   */
  public String getXml() {
    return xml;
  }

  /**
   * @param xml the XML serialized content of the media package element to set
   */
  public void setXml(String xml) {
    this.xml = xml;
  }

  /**
   * @return the OAI-PMH entity
   */
  public OaiPmhEntity getOaiPmhEntity() {
    return oaiPmhEntity;
  }

  /**
   * @param oaiPmhEntity the OAI-PMH entity to set
   */
  public void setOaiPmhEntity(OaiPmhEntity oaiPmhEntity) {
    this.oaiPmhEntity = oaiPmhEntity;
  }
}
