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

package org.opencastproject.adopterstatistics.registration;

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
 * JPA-annotated registration form object
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "oc_statistic_registration")
@NamedQueries({
        @NamedQuery(name = "Form.findByUsername", query = "Select f FROM Form f where f.username = :username"),
        @NamedQuery(name = "Form.findAllCount", query = "SELECT COUNT(f) FROM Form f")
})
public class Form implements IForm {

  @Id
  @Column(name = "adopter_key")
  private String adopterKey;

  @Column(name = "username")
  private String username;

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

  @Column(name = "allows_tech_data")
  private boolean allowsTechData;

  @Column(name = "created", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateCreated;

  @Column(name = "last_modified", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateModified;

  /**
   * No-arg constructor needed by JPA
   */
  public Form() {

  }

  /**
   * Constructor with all parameters.
   *
   * @param organisationName   Organisation name.
   * @param departmentName     Department name.
   * @param firstName          First name of the user.
   * @param lastName           Last name of the user.
   * @param email              E-Mail address of the user.
   * @param country            The country code (XX).
   * @param postalCode         The postal code.
   * @param city               The city name.
   * @param street             The street name.
   * @param streetNo           The street number.
   * @param contactMe          Are we allowed to contact the user.
   * @param allowsStatistics   Are we allowed to gather information for statistics.
   * @param allowsErrorReports Are we allowed to gather error reports.
   * @param allowsTechData     Are we allowed to gather tech data.
   */
  public Form(String organisationName, String departmentName, String firstName,
          String lastName, String email, String country, String postalCode, String city, String street, String streetNo,
          boolean contactMe, boolean allowsStatistics, boolean allowsErrorReports, boolean allowsTechData) {
    this.organisationName = organisationName;
    this.departmentName = departmentName;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.country = country;
    this.postalCode = postalCode;
    this.city = city;
    this.street = street;
    this.streetNo = streetNo;
    this.contactMe = contactMe;
    this.allowsStatistics = allowsStatistics;
    this.allowsErrorReports = allowsErrorReports;
    this.allowsTechData = allowsTechData;
    this.dateCreated = new Date();
    this.dateModified = new Date();
  }

  /**
   * Overwrites fields of this object.
   *
   * @param form The overwriting form fields.
   */
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
    this.allowsTechData = f.allowsTechData;
    this.dateModified = f.dateModified;
  }

  public String getAdopterKey() {
    return adopterKey;
  }

  public void setAdopterKey(String adopterKey) {
    this.adopterKey = adopterKey;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
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

  public boolean isContactMe() {
    return contactMe;
  }

  public void setContactMe(boolean contactMe) {
    this.contactMe = contactMe;
  }

  public boolean isAllowsStatistics() {
    return allowsStatistics;
  }

  public void setAllowsStatistics(boolean allowsStatistics) {
    this.allowsStatistics = allowsStatistics;
  }

  public boolean isAllowsErrorReports() {
    return allowsErrorReports;
  }

  public void setAllowsErrorReports(boolean allowsErrorReports) {
    this.allowsErrorReports = allowsErrorReports;
  }

  public boolean isAllowsTechData() {
    return allowsTechData;
  }

  public void setAllowsTechData(boolean allowsTechData) {
    this.allowsTechData = allowsTechData;
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

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Organization))
      return false;
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

}
