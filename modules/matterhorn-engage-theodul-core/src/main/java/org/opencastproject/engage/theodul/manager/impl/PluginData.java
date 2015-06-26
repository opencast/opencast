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

import org.opencastproject.engage.theodul.api.EngagePlugin;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class PluginData {

    private ServiceReference sref;
    private static int runningId = 1;
    private int id;
    private String name;
    private String description;
    private boolean providesStaticResources;
    private boolean providesRestEndpoint;
    private ServiceRegistration staticResourceRegistration = null;
    private ServiceRegistration restEndpointRegistration = null;

    public PluginData(ServiceReference sref) {
        this.sref = sref;

        try {
            // get plugin display name
            name = (String) sref.getProperty(EngagePlugin.PROPKEY_PLUGIN_NAME);
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Name not set or empty.");
            }

            // get plugin description. if not availabel generate description (eg. shown in welcome page)
            description = (String) sref.getProperty(EngagePlugin.PROPKEY_PLUGIN_DESCRIPTION);
            description = description == null ? "Engage Plugin " + name : description;

            // check if plugin bundle has static resources
            providesStaticResources = sref.getBundle().getEntry(EngagePlugin.STATIC_RESOURCES_PATH) != null;

            // check if plugin wants to register REST endpoint
            String providesRest = (String) sref.getProperty(EngagePlugin.PROPKEY_PLUGIN_REST);
            providesRestEndpoint = (providesRest != null) && ("true".equalsIgnoreCase(providesRest) || "yes".equalsIgnoreCase(providesRest));

            // if all information could be retrieved, generate ID for plugin
            id = runningId++;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to get plugin data from service registration.", e);
        }
    }

    public boolean providesStaticResources() {
        return providesStaticResources;
    }

    public boolean providesRestEndpoint() {
        return providesRestEndpoint;
    }

    public int getPluginID() {
        return id;
    }

    public String getStaticResourcesPath() {
        return Integer.toString(id) + "/" + EngagePlugin.STATIC_RESOURCES_PATH;
    }

    public String getRestPath() {
        return Integer.toString(id) + "/" + EngagePlugin.REST_ENDPOINT_PATH;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ServiceReference getServiceReference() {
        return sref;
    }

    public ServiceRegistration getStaticResourceRegistration() {
        return staticResourceRegistration;
    }

    public void setStaticResourceRegistration(ServiceRegistration staticResourceRegistration) {
        this.staticResourceRegistration = staticResourceRegistration;
    }

    public ServiceRegistration getRestEndpointRegistration() {
        return restEndpointRegistration;
    }

    public void setRestEndpointRegistration(ServiceRegistration restEndpointRegistration) {
        this.restEndpointRegistration = restEndpointRegistration;
    }
}
