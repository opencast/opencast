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
package org.opencastproject.manager.api.plugins;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * This class represents a plug-in artifact.
 *
 * @author Leonid Oldenburger
 */
@XmlType(name = "plugin-artifact", namespace = "http://plugin-artifact.opencastproject.org")
@XmlRootElement(name = "plugin-artifact", namespace = "http://plugin-artifact.opencastproject.org")
@XmlAccessorType(XmlAccessType.NONE)
public class PluginArtifact {

    /**
     * The plug-in file's name.
     */
  @XmlElement
  private String fileName;

    /**
     * The plug-in name.
     */
  @XmlElement
  private String pluginName;

    /**
     * The plug-in creation date.
     */
  @XmlElement
  private String pluginDate;

    /**
     * The plug-in thumbnail's.
     */
  @XmlElement
  private String pluginThumbnail;

    /**
     * The plug-in id.
     */
  @XmlAttribute()
  private String pluginID;

    /**
     * The plug-in state.
     */
  @XmlAttribute()
  private String pluginState;

    /**
     * The plug-in version.
     */
  @XmlAttribute()
  private String pluginVersion;

    /**
     * The plug-in server.
     */
  @XmlElement
  private String pluginServer;

    /**
     * The plug-in type.
     */
  @XmlElement
  private String pluginType;

    /**
     * The plug-in description.
     */
  @XmlElement
  private String pluginDescription;

    /**
     * The plug-in origin.
     */
  @XmlElement
  private String pluginOrigin;

    /**
     * Sets the plug-in file name.
     */
  public void setFileName(String fileName) {
     this.fileName = fileName;
  }

    /**
     * Returns the plug-in file's name.
     *
     * @return the plug-in file name
     */
  public String getFileName() {
    return fileName;
  }

    /**
     * Sets the plug-in file name.
     */
  public void setPluginName(String pluginName) {
     this.pluginName = pluginName;
  }

    /**
     * Returns the plug-in name.
     *
     * @return the plug-in name
     */
  public String getPluginName() {
    return pluginName;
  }

    /**
     * Sets the plug-ins server.
     */
  public void setPluginServer(String server) {
     this.pluginServer = server;
  }

    /**
     * Returns the plug-in server.
     *
     * @return the plug-in server
     */
  public String getPluginServer() {
     return this.pluginServer;
  }

    /**
     * Sets the plug-in type.
     */
  public void setPluginType(String type) {
     this.pluginType = type;
  }

    /**
     * Returns the plug-in type.
     *
     * @return the plug-in type
     */
  public String getPluginType() {
     return this.pluginType;
  }

    /**
     * Sets the plug-in description.
     */
  public void setPluginDescription(String description) {
     this.pluginDescription = description;
  }

    /**
     * Returns the plug-in description.
     *
     * @return the plug-in description
     */
  public String getPluginDescription() {
     return this.pluginDescription;
  }

    /**
     * Sets the plug-in origin.
     */
  public void setPluginOrigin(String origin) {
     this.pluginOrigin = origin;
  }

    /**
     * Returns the plug-in origin.
     *
     * @return the plug-in origin
     */
  public String getPluginOrigin() {
     return this.pluginOrigin;
  }

    /**
     * Sets the plug-ins thumbnail's.
     */
  public void setThumbnail(String pluginThumbnail) {
    this.pluginThumbnail = pluginThumbnail;
  }

    /**
     * Returns the plug-in thumbnail's.
     *
     * @return the plug-in thumbnail's
     */
  public String getThumbnail() {
    return pluginThumbnail;
  }

    /**
     * Sets the plug-in id.
     */
  public void setID(String id) {
    this.pluginID = id;
  }

    /**
     * Returns the plug-in id.
     *
     * @return the plug-in id
     */
  public String getID() {
    return pluginID;
  }

    /**
     * Sets the plug-in state.
     */
  public void setState(String pluginState) {
    this.pluginState = pluginState;
  }

    /**
     * Returns the plug-in state.
     *
     * @return the plug-in state
     */
  public String getState() {
    return pluginState;
  }

    /**
     * Sets the plug-in version.
     */
  public void setVersion(String pluginVersion) {
    this.pluginVersion = pluginVersion;
  }

    /**
     * Returns the plug-in version.
     *
     * @return the plug-in version
     */
  public String getVersion() {
    return pluginVersion;
  }

    /**
     * Sets the plug-in date.
     */
  public void setDate(String pluginDate) {
    this.pluginDate = pluginDate;
  }

    /**
     * Returns the plug-in date.
     *
     * @return the plug-in date
     */
  public String getDate() {
    return pluginDate;
  }
}