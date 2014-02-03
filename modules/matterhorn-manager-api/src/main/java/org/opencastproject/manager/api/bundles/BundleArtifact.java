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

package org.opencastproject.manager.api.bundles;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * This class represents a BundleArtifact.
 *
 * @author Leonid Oldenburger
 */
@XmlType(name = "bundle-artifact", namespace = "http://bundle-artifact.opencastproject.org")
@XmlRootElement(name = "bundle-artifact", namespace = "http://bundle-artifact.opencastproject.org")
@XmlAccessorType(XmlAccessType.NONE)
public class BundleArtifact {

    /**
     * The file's name.
     */
  @XmlElement
  private String fileName;

    /**
     * The bundles name.
     */
  @XmlElement
  private String bundleName;

    /**
     * The bundles version.
     */
  @XmlElement
  private String bundleVersion;

    /**
     * Sets the bundles file name.
     */
  public void setFileName(String fileName) {
     this.fileName = fileName;
  }

    /**
     * Returns the file's name.
     *
     * @return the file name
     */
  public String getFileName() {
    return fileName;
  }

    /**
     * Sets the bundles name.
     */
  public void setBundleName(String bundleName) {
    this.bundleName = bundleName;
  }

    /**
     * Returns the bundles name.
     *
     * @return the bundle name
     */
  public String getBundleName() {
    return bundleName;
  }

    /**
     * Sets the bundles version.
     */
  public void setVersion(String bundleVersion) {
    this.bundleVersion = bundleVersion;
  }

    /**
     * Returns the bundles version.
     *
     * @return the bundles version
     */
  public String getVersion() {
    return bundleVersion;
  }
}