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
package org.opencastproject.engage.theodul.manager.impl;

import org.opencastproject.engage.theodul.api.EngagePluginRegistration;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

// TODO improve this with respect to a cleaner JAXB approach
@XmlType(name = "plugin", namespace = "http://engageplugin.opencastproject.org")
@XmlRootElement(name = "plugin", namespace = "http://engageplugin.opencastproject.org")
@XmlAccessorType(XmlAccessType.NONE)
public class EngagePluginRegistrationImpl implements EngagePluginRegistration {

    @XmlAttribute()
    private Integer id;
    @XmlAttribute()
    private String name;
    @XmlElement(name = "description")
    private String description;
    @XmlElement(name = "static-path")
    private String staticPath = null;
    @XmlElement(name = "rest-path")
    private String restPath = null;
    private boolean hasStaticResources;
    private boolean hasRestEndpoint;

    public EngagePluginRegistrationImpl() {
    }

    public EngagePluginRegistrationImpl(Integer id, String name, String description, String staticPath, String restPath) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.staticPath = staticPath;
        this.restPath = restPath;
        this.hasStaticResources = (staticPath != null);
        this.hasRestEndpoint = (restPath != null);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getStaticPath() {
        return staticPath;
    }

    @Override
    public String getRestPath() {
        return restPath;
    }

    @Override
    public boolean hasStaticResources() {
        return hasStaticResources;
    }

    @Override
    public boolean hasRestEndpoint() {
        return hasRestEndpoint;
    }

    @Override
    public boolean equals(Object other) {
        return this.hashCode() == other.hashCode();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this.getName() != null ? this.getName().hashCode() : 0);
        return hash;
    }
}
