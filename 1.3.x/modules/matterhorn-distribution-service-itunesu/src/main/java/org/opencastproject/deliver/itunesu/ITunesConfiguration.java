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
package org.opencastproject.deliver.itunesu;

/**
 * Singleton with ITunes configuration information.
 */
public class ITunesConfiguration {
  /** the site's URL at iTunesU */
  private String siteURL;
  /** debug suffix */
  private String debugSuffix;
  /** a signature string */
  private String sharedSecret;
  /** credential of the site administrator (BE CAREFUL!!!) */
  private String administratorCredential;

  /** the only instance */
  private static ITunesConfiguration configuration = new ITunesConfiguration();

  /** public method to get the instance */
  public static ITunesConfiguration getInstance() {
    return configuration;
  }

  /** private constructor */
  private ITunesConfiguration() {
  }

  // **** Accessors

  public String getSiteURL() {
    return siteURL;
  }

  public void setSiteURL(String siteURL) {
    this.siteURL = siteURL;
  }

  public String getDebugSuffix() {
    return debugSuffix;
  }

  public void setDebugSuffix(String debugSuffix) {
    this.debugSuffix = debugSuffix;
  }

  public String getSharedSecret() {
    return sharedSecret;
  }

  public void setSharedSecret(String sharedSecret) {
    this.sharedSecret = sharedSecret;
  }

  public String getAdministratorCredential() {
    return administratorCredential;
  }

  public void setAdministratorCredential(String administratorCredential) {
    this.administratorCredential = administratorCredential;
  }

  // **** String Representation

  @Override
  public String toString() {
    return "ITunesConfiguration{" +
           "siteURL='" + siteURL + '\'' +
           ", debugSuffix='" + debugSuffix + '\'' +
           ", sharedSecret='" + sharedSecret + '\'' +
           ", administratorCredential=" + administratorCredential +
           "}";
  }
}
