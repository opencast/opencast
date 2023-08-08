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

package org.opencastproject.engage.theodul.plugin.tab.slidetext;

import org.opencastproject.engage.theodul.api.AbstractEngagePlugin;
import org.opencastproject.engage.theodul.api.EngagePlugin;
import org.opencastproject.engage.theodul.api.EngagePluginManager;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;

@Path("/")
@Component(
    property = {
        "opencast.engage.plugin.name=EngagePluginTabSlidetext",
        "opencast.engage.plugin.description=Simple implementation of a tab with the text of the slides",
        "opencast.engage.plugin.rest=false"
    },
    immediate = true,
    service = EngagePlugin.class
)
public class EngagePluginTabSlidetext extends AbstractEngagePlugin {

  private static final Logger log = LoggerFactory.getLogger(EngagePluginTabSlidetext.class);

  @Activate
  protected void activate(ComponentContext cc) {
    log.info("Activated engage plugin: SlideText.");
  }

  @Override
  @Reference
  protected void setPluginManager(final EngagePluginManager pluginManager) {
    this.pluginManager = pluginManager;
  }
}