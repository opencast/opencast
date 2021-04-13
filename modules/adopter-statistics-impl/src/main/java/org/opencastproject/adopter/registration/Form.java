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

package org.opencastproject.adopter.registration;

import org.opencastproject.security.api.Organization;
import org.opencastproject.util.EqualsUtil;

import java.util.Date;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * JPA-annotated registration form object.
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "oc_adopter_registration")
@NamedQueries({
        @NamedQuery(name = "Form.findAll", query = "SELECT f FROM Form f"),
        @NamedQuery(name = "Form.deleteAll", query = "DELETE FROM Form f")
})
public class Form implements IForm {


  //================================================================================
  // Properties
  //================================================================================

  @Id
  @Column(name = "adopter_key", length = 64)
  private String adopterKey;

  @Column(name = "statistic_key")
  private String statisticKey;

  @Column(name = "organisation")
  private String organisationName;

  @Column(name = "department")
  private String departmentName;

  @Column(name = "first_name")
  private String firstName;

  @Column(name = "last_name")
  private String lastName;

  @Column(name = "email")
  private String email;

  @Column(name = "country")
  private String country;

  @Column(name = "postal_code")
  private String postalCode;

  @Column(name = "city")
  private String city;

  @Column(name = "street")
  private String street;

  @Column(name = "street_no")
  private String streetNo;

  @Column(name = "contact_me")
  private boolean contactMe;

  @Column(name = "allows_statistics")
  private boolean allowsStatistics;

  @Column(name = "allows_error_reports")
  private boolean allowsErrorReports;

  @Column(name = "created", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateCreated;

  @Column(name = "last_modified", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateModified;

  @Column(name = "agreed_to_policy")
  private boolean agreedToPolicy;

  @Column(name = "registered")
  private boolean registered;


  //================================================================================
  // Constructor and Methods
  //================================================================================

  /** No-arg constructor needed by JPA. */
  public Form() {

  }


  public void merge(IForm form) {
    Form f = (Form) form;
    this.organisationName = f.organisationName;
    this.departmentName = f.departmentName;
    this.firstName = f.firstName;
    this.lastName = f.lastName;
    this.email = f.email;
    this.country = f.country;
    this.postalCode = f.postalCode;
    this.city = f.city;
    this.street = f.street;
    this.streetNo = f.streetNo;
    this.contactMe = f.contactMe;
    this.allowsStatistics = f.allowsStatistics;
    this.allowsErrorReports = f.allowsErrorReports;
    this.agreedToPolicy = f.agreedToPolicy;
    if (!this.registered) {
      // overwrite this field only when an adopter isn't registered yet
      // once an adopter is registered, he stays registered
      this.registered = f.registered;
    }
    this.dateModified = new Date();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Organization)) {
      return false;
    }
    return ((Form) obj).adopterKey.equals(adopterKey);
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(adopterKey);
  }

  @Override
  public String toString() {
    return adopterKey;
  }


  //================================================================================
  // Getter and Setter
  //================================================================================

  public String getAdopterKey() {
    return adopterKey;
  }

  public void setAdopterKey(String adopterKey) {
    this.adopterKey = adopterKey;
  }

  public String getStatisticKey() {
    return statisticKey;
  }

  public void setStatisticKey(String statisticKey) {
    this.statisticKey = statisticKey;
  }

  public String getOrganisationName() {
    return organisationName;
  }

  public void setOrganisationName(String organisationName) {
    this.organisationName = organisationName;
  }

  public String getDepartmentName() {
    return departmentName;
  }

  public void setDepartmentName(String departmentName) {
    this.departmentName = departmentName;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getStreet() {
    return street;
  }

  public void setStreet(String street) {
    this.street = street;
  }

  public String getStreetNo() {
    return streetNo;
  }

  public void setStreetNo(String streetNo) {
    this.streetNo = streetNo;
  }

  public boolean allowsContacting() {
    return contactMe;
  }

  public void setContactMe(boolean contactMe) {
    this.contactMe = contactMe;
  }

  public boolean allowsStatistics() {
    return allowsStatistics;
  }

  public void setAllowsStatistics(boolean allowsStatistics) {
    this.allowsStatistics = allowsStatistics;
  }

  public boolean allowsErrorReports() {
    return allowsErrorReports;
  }

  public void setAllowsErrorReports(boolean allowsErrorReports) {
    this.allowsErrorReports = allowsErrorReports;
  }

  public Date getDateCreated() {
    return dateCreated;
  }

  public void setDateCreated(Date dateCreated) {
    this.dateCreated = dateCreated;
  }

  public Date getDateModified() {
    return dateModified;
  }

  public void setDateModified(Date dateModified) {
    this.dateModified = dateModified;
  }

  public boolean agreedToPolicy() {
    return agreedToPolicy;
  }

  public void setAgreedToPolicy(boolean agreedToPolicy) {
    this.agreedToPolicy = agreedToPolicy;
  }

  public boolean isRegistered() {
    return registered;
  }

  public void setRegistered(boolean registered) {
    this.registered = registered;
  }

}
