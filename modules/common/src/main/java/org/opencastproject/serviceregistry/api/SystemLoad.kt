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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.serviceregistry.api

import org.opencastproject.util.NotFoundException
import java.util.LinkedHashMap

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElementWrapper
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType

/**
 * Mappings between the registered hosts and their load factors.
 */
@XmlType(name = "load", namespace = "http://serviceregistry.opencastproject.org")
@XmlRootElement(name = "load", namespace = "http://serviceregistry.opencastproject.org")
@XmlAccessorType(XmlAccessType.NONE)
class SystemLoad {

    /** The list of nodes and their current load  */
    protected var nodeLoads: MutableMap<String, NodeLoad>

    /** No-arg constructor needed by JAXB  */
    init {
        nodeLoads = LinkedHashMap()
    }

    /**
     * Get the list of nodes and their current loadfactor.
     *
     * @return the nodeLoads
     */
    @XmlElementWrapper(name = "nodes")
    @XmlElement(name = "node")
    fun getNodeLoads(): Collection<NodeLoad> {
        return nodeLoads.values
    }

    /**
     * Sets the list of nodes and their current loadfactor.
     *
     * @param newLoads
     * the nodeLoads to set
     */
    fun setNodeLoads(newLoads: Collection<NodeLoad>) {
        for (node in newLoads) {
            nodeLoads[node.host] = node
        }
    }

    /**
     * Updates the load factor for a node
     * @param host
     * The host to update
     * @param modifier
     * The modifier to apply to the load
     */
    @Throws(NotFoundException::class)
    fun updateNodeLoad(host: String, modifier: Float) {
        if (!nodeLoads.containsKey(host)) {
            throw NotFoundException("Host $host not in this load object")
        }
        val current = nodeLoads[host]
        current.loadFactor = current.loadFactor + modifier
    }

    /**
     * Adds a node to the map of load values, overwriting an existing entry if the node is already present in the map
     * @param load
     * the nodeLoad to add
     */
    fun addNodeLoad(load: NodeLoad) {
        nodeLoads[load.host] = load
    }

    /**
     * Gets a specific host from the map, if present.  If the node is not present, or the host value is null, this method returns null.
     * @param host
     * The hostname of the host you are interested in.
     * @return
     * The specific host from the map, if present.  If the node is not present, or the host value is null, this method returns null.
     */
    operator fun get(host: String?): NodeLoad? {
        return if (host != null && this.containsHost(host)) nodeLoads[host] else null
    }

    /**
     * Returns true if the load map contains the host in question.
     * @param host
     * The hostname of the host you are interested in.
     * @return
     * True if the host is present, false if the host is not, or the host variable is null.
     */
    fun containsHost(host: String?): Boolean {
        return if (host != null) nodeLoads.containsKey(host) else false
    }

    /** A record of a node in the cluster and its load factor  */
    @XmlType(name = "nodetype", namespace = "http://serviceregistry.opencastproject.org")
    @XmlRootElement(name = "nodetype", namespace = "http://serviceregistry.opencastproject.org")
    @XmlAccessorType(XmlAccessType.NONE)
    class NodeLoad : Comparable<NodeLoad> {

        /** This node's base URL  */
        /**
         * @return the host
         */
        /**
         * @param host
         * the host to set
         */
        @XmlAttribute
        var host: String

        /** This node's current load  */
        /**
         * @return the loadFactor
         */
        /**
         * @param loadFactor
         * the loadFactor to set
         */
        @XmlAttribute
        var loadFactor: Float = 0.toFloat()

        /** No-arg constructor needed by JAXB  */
        constructor() {}

        constructor(host: String, load: Float) {
            this.host = host
            this.loadFactor = load
        }

        override fun compareTo(other: NodeLoad): Int {
            return if (other.loadFactor > this.loadFactor) {
                1
            } else if (this.loadFactor > other.loadFactor) {
                -1
            } else {
                0
            }
        }

        fun exceeds(other: NodeLoad): Boolean {
            return if (this.compareTo(other) > 1) {
                true
            } else false
        }
    }
}
