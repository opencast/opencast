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

package org.opencastproject.userdirectory.brightspace.client.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BrightspaceUser {

  private String orgId;
  private String userId;
  private String firstName;
  private String middleName;
  private String lastName;
  private String userName;
  private String externalEmail;
  private String orgDefinedId;
  private String uniqueIdentifier;
  private Activation activation;
  private String displayName;

  @JsonCreator
  public BrightspaceUser(@JsonProperty("OrgId") String orgId, @JsonProperty("UserId") String userId, @JsonProperty("FirstName") String firstName, @JsonProperty("MiddleName") String middleName, @JsonProperty("LastName") String lastName, @JsonProperty("UserName") String userName, @JsonProperty("ExternalEmail") String externalEmail, @JsonProperty("OrgDefinedId") String orgDefinedId, @JsonProperty("UniqueIdentifier") String uniqueIdentifier, @JsonProperty("Activation") Activation activation, @JsonProperty("DisplayName") String displayName) {
    this.orgId = orgId;
    this.userId = userId;
    this.firstName = firstName;
    this.middleName = middleName;
    this.lastName = lastName;
    this.userName = userName;
    this.externalEmail = externalEmail;
    this.orgDefinedId = orgDefinedId;
    this.uniqueIdentifier = uniqueIdentifier;
    this.activation = activation;
    this.displayName = displayName;
  }

  public String getOrgId() {
    return this.orgId;
  }

  public String getUserId() {
    return this.userId;
  }

  public String getFirstName() {
    return this.firstName;
  }

  public String getMiddleName() {
    return this.middleName;
  }

  public String getLastName() {
    return this.lastName;
  }

  public String getUserName() {
    return this.userName;
  }

  public String getExternalEmail() {
    return this.externalEmail;
  }

  public String getOrgDefinedId() {
    return this.orgDefinedId;
  }

  public String getUniqueIdentifier() {
    return this.uniqueIdentifier;
  }

  public Activation getActivation() {
    return this.activation;
  }

  public String getDisplayName() {
    return this.displayName;
  }

  public String getFullName() {
    return this.firstName + " " + this.lastName;
  }
}
