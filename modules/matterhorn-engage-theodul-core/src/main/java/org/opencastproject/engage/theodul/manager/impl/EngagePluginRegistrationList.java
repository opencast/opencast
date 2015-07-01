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

package org.opencastproject.engage.theodul.manager.impl;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.opencastproject.engage.theodul.api.EngagePluginRegistration;

/**
 * An JAX-B annotated class that wraps around a list of
 * <code>EngagePluginRegistration</code> so that we can generate an XML
 * representation of the list via JAX-B.
 */
@XmlType(name = "pluginlist", namespace = "http://engageplugin.opencastproject.org")
@XmlRootElement(name = "pluginlist", namespace = "http://engageplugin.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
public class EngagePluginRegistrationList {

    @XmlElement(name = "plugins")
    private List<EngagePluginRegistrationImpl> plugins = new ArrayList<EngagePluginRegistrationImpl>();

    public EngagePluginRegistrationList() {
        plugins = new ArrayList<EngagePluginRegistrationImpl>();
    }

    public EngagePluginRegistrationList(List<EngagePluginRegistration> plugins) {
        for (EngagePluginRegistration reg : plugins) {
            this.plugins.add((EngagePluginRegistrationImpl) reg);
        }
    }

    public List<EngagePluginRegistrationImpl> getPlugins() {
        return plugins;
    }
}
