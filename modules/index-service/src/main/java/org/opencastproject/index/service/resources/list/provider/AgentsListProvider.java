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

package org.opencastproject.index.service.resources.list.provider;

import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.AgentState;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.list.api.ResourceListProvider;
import org.opencastproject.list.api.ResourceListQuery;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

public class AgentsListProvider implements ResourceListProvider {

  private static final String PROVIDER_PREFIX = "AGENTS";
  public static final String NAME = PROVIDER_PREFIX + ".NAME";
  public static final String STATUS = PROVIDER_PREFIX + ".STATUS";

  private static final String[] NAMES = { NAME, STATUS };
  private static final Logger logger = LoggerFactory.getLogger(AgentsListProvider.class);

  /** The capture agent service */
  private CaptureAgentStateService agentsService;

  protected void activate(BundleContext bundleContext) {
    logger.info("Capture-agents list provider activated!");
  }

  /** OSGi callback for capture-agents services. */
  public void setCaptureAgentService(CaptureAgentStateService service) {
    this.agentsService = service;
  }

  @Override
  public String[] getListNames() {
    return NAMES;
  }

  @Override
  public Map<String, String> getList(String listName, ResourceListQuery query) {
    Map<String, String> result = new TreeMap<String, String>();

    if (STATUS.equals(listName)) {
      for (String state : AgentState.KNOWN_STATES) {
        result.put(state, AgentState.TRANSLATION_PREFIX + state.toUpperCase());
      }
    } else {
      Map<String, Agent> knownAgents = agentsService.getKnownAgents();
      for (Agent agent : knownAgents.values()) {
        result.put(agent.getName(), agent.getName());
      }

    }

    return result;
  }

  @Override
  public boolean isTranslatable(String listName) {
    return STATUS.equals(listName);
  }

  @Override
  public String getDefault() {
    return null;
  }
}
