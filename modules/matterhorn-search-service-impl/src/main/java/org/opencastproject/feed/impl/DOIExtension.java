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

package org.opencastproject.feed.impl;

import org.opencastproject.feed.api.FeedExtension;

/**
 * DOI Extension module.
 */
public class DOIExtension implements FeedExtension {

  /**
   * The DOI module uri
   */
      public static final String URI = "http://www.doi.org";

  private String doi;
  private String structuralType;
  private String mode;
  private String registrationAgency;
  private String issueNumber;
  private String isCompiledBy;
  private String isAlsoPublishedAs;

  public String getUri() {
    return URI;
  }

  public String getDoi() {
    return doi;
  }

  public void setDoi(String doi) {
    this.doi = doi;
  }

  public String getStructuralType() {
    return structuralType;
  }

  public void setStructuralType(String structuralType) {
    this.structuralType = structuralType;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public String getRegistrationAgency() {
    return registrationAgency;
  }

  public void setRegistrationAgency(String registrationAgency) {
    this.registrationAgency = registrationAgency;
  }

  public String getIssueNumber() {
    return issueNumber;
  }

  public void setIssueNumber(String issueNumber) {
    this.issueNumber = issueNumber;
  }

  public String getCompiledBy() {
    return isCompiledBy;
  }

  public void setCompiledBy(String compiledBy) {
    isCompiledBy = compiledBy;
  }

  public String getAlsoPublishedAs() {
    return isAlsoPublishedAs;
  }

  public void setAlsoPublishedAs(String alsoPublishedAs) {
    isAlsoPublishedAs = alsoPublishedAs;
  }

}
