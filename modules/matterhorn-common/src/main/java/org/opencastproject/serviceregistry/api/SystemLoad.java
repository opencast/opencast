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
package org.opencastproject.serviceregistry.api;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Mappings between the registered hosts and their load factors.
 */
@XmlType(name = "load", namespace = "http://serviceregistry.opencastproject.org")
@XmlRootElement(name = "load", namespace = "http://serviceregistry.opencastproject.org")
@XmlAccessorType(XmlAccessType.NONE)
public class SystemLoad {

  /** No-arg constructor needed by JAXB */
  public SystemLoad() {
  }

  /** The list of nodes and their current load */
  @XmlElementWrapper(name = "nodes")
  @XmlElement(name = "node")
  protected List<NodeLoad> nodeLoads;

  /**
   * Get the list of nodes and their current loadfactor.
   *
   * @return the nodeLoads
   */
  public List<NodeLoad> getNodeLoads() {
    return nodeLoads;
  }

  /**
   * Sets the list of nodes and their current loadfactor.
   *
   * @param nodeLoads
   *          the nodeLoads to set
   */
  public void setNodeLoads(List<NodeLoad> nodeLoads) {
    this.nodeLoads = nodeLoads;
  }

  /** A record of a node in the cluster and its load factor */
  @XmlType(name = "nodetype", namespace = "http://serviceregistry.opencastproject.org")
  @XmlRootElement(name = "nodetype", namespace = "http://serviceregistry.opencastproject.org")
  @XmlAccessorType(XmlAccessType.NONE)
  public static class NodeLoad {

    /** No-arg constructor needed by JAXB */
    public NodeLoad() {
    }

    /** This node's base URL */
    @XmlAttribute
    protected String host;

    /** This node's current load */
    @XmlAttribute
    protected float loadFactor;

    /**
     * @return the host
     */
    public String getHost() {
      return host;
    }

    /**
     * @param host
     *          the host to set
     */
    public void setHost(String host) {
      this.host = host;
    }

    /**
     * @return the loadFactor
     */
    public float getLoadFactor() {
      return loadFactor;
    }

    /**
     * @param loadFactor
     *          the loadFactor to set
     */
    public void setLoadFactor(float loadFactor) {
      this.loadFactor = loadFactor;
    }
  }
}
