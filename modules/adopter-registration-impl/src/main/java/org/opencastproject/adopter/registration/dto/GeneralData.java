/*
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

package org.opencastproject.adopter.registration.dto;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * DTO for the general data of an adopter.
 */
public class GeneralData {

  /** JSON parser */
  private static final Gson gson = new Gson();

  //================================================================================
  // Properties
  //================================================================================

  /** The unique identification key for an adopter. */
  @SerializedName("adopter_key")
  private String adopterKey;

  /** The organisation of the adopter. */
  @SerializedName("organisation_name")
  private final String organisationName;

  /** Department name of the adopter. */
  @SerializedName("department_name")
  private final String departmentName;

  /** The first name of the adopter. */
  @SerializedName("first_name")
  private final String firstName;

  /** The last name of the adopter. */
  @SerializedName("last_name")
  private final String lastName;

  /** Organization country. */
  private final String country;

  /** Organization city. */
  private final String city;

  /** Organization postal code. */
  @SerializedName("postal_code")
  private final String postalCode;

  /** Organization street name. */
  private final String street;

  /** Organization street number. */
  @SerializedName("street_no")
  private final String streetNo;

  /** The E-Mail address of the adopter. */
  private final String email;

  /** Whether we can contact the adopter */
  @SerializedName("contact_me")
  private final boolean allowContact;

  /** Which type of system is this */
  @SerializedName("system_type")
  private final String systemType;

  /** Whether we can send error reports */
  @SerializedName("send_errors")
  private final boolean allowErrorReports;

  /** Whether we can contact statistics */
  @SerializedName("send_usage")
  private final boolean allowStatistics;


  //================================================================================
  // Constructor and Methods
  //================================================================================

  public GeneralData(Adopter adopter) {
    this.adopterKey = adopter.getAdopterKey();
    this.organisationName = adopter.getOrganisationName();
    this.departmentName = adopter.getDepartmentName();
    this.firstName = adopter.getFirstName();
    this.lastName = adopter.getLastName();
    this.country = adopter.getCountry();
    this.city = adopter.getCity();
    this.postalCode = adopter.getPostalCode();
    this.street = adopter.getStreet();
    this.streetNo = adopter.getStreetNo();
    this.email = adopter.getEmail();
    this.allowContact = adopter.allowsContacting();
    this.systemType = adopter.systemType();
    this.allowErrorReports = adopter.allowsErrorReports();
    this.allowStatistics = adopter.allowsStatistics();
  }

  /**
   * Creates a JSON string from an instance of this class.
   * @return This class as a JSON string.
   */
  public String jsonify() {
    return gson.toJson(this);
  }


  //================================================================================
  // Getter and Setter
  //================================================================================

  public String getAdopterKey() {
    return adopterKey;
  }

  public void setAdopterKey(String key) {
    this.adopterKey = key;
  }

  public String getOrganisationName() {
    return organisationName;
  }

  public String getDepartmentName() {
    return departmentName;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getCountry() {
    return country;
  }

  public String getCity() {
    return city;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public String getStreet() {
    return street;
  }

  public String getStreetNo() {
    return streetNo;
  }

  public String getEmail() {
    return email;
  }

  public String getContactMe() {
    return Boolean.toString(allowContact);
  }

  public String getSystemType() {
    return systemType;
  }

  public String getErrorReports() {
    return Boolean.toString(allowErrorReports);
  }

  public String getStatisticss() {
    return Boolean.toString(allowStatistics);
  }
}
