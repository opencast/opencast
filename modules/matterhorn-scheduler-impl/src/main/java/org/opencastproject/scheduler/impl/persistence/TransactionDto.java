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
package org.opencastproject.scheduler.impl.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

/**
 * Entity object for storing scheduled transaction data in persistence storage.
 */
@IdClass(EventIdPK.class)
@Entity(name = "Transaction")
@NamedQueries({
        @NamedQuery(name = "Transaction.findAll", query = "SELECT e FROM Transaction e WHERE e.organization = :org"),
        @NamedQuery(name = "Transaction.findBySource", query = "SELECT e FROM Transaction e WHERE e.source = :source  AND e.organization = :org"),
        @NamedQuery(name = "Transaction.countAll", query = "SELECT COUNT(e) FROM Transaction e WHERE e.organization = :org") })
@Table(name = "mh_scheduled_transaction", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "id", "organization", "source" }) })
public class TransactionDto {

  /** Transaction ID, primary key */
  @Id
  @Column(name = "id", length = 128)
  private String mediaPackageId;

  /** Organization id, primary key */
  @Id
  @Column(name = "organization", length = 128)
  private String organization;

  @Column(name = "source")
  private String source;

  @Column(name = "last_modified")
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastModifiedDate;

  /**
   * Default constructor without any import.
   */
  public TransactionDto() {
  }

  /**
   * Returns the transaction ID
   *
   * @return the transaction ID
   */
  public String getId() {
    return mediaPackageId;
  }

  /**
   * Sets the transaction ID
   *
   * @param id
   *          the transaction ID
   */
  public void setId(String id) {
    this.mediaPackageId = id;
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
   * Returns the scheduling source
   *
   * @return the scheduling source
   */
  public String getSource() {
    return source;
  }

  /**
   * Sets the scheduling source
   *
   * @param source
   *          the scheduling source
   */
  public void setSource(String source) {
    this.source = source;
  }

  /**
   * Returns the last modified date
   *
   * @return the last modified date
   */
  public Date getLastModifiedDate() {
    return lastModifiedDate;
  }

  /**
   * Sets the last modified date
   *
   * @param lastModifiedDate
   *          the last modified date
   */
  public void setLastModifiedDate(Date lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

}
