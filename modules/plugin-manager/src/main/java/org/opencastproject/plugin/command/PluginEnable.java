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

package org.opencastproject.plugin.command;

import org.opencastproject.plugin.PluginManager;
import org.opencastproject.plugin.command.completers.PluginNameCompleter;
import org.opencastproject.plugin.impl.PluginManagerImpl;

import org.apache.karaf.config.core.ConfigRepository;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.osgi.service.cm.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Command(scope = "opencast", name = "plugin-enable", description = "Enable a plugin with the specified name")
public class PluginEnable implements Action {

  @Reference
  private ConfigRepository configRepository;

  @Reference
  private PluginManager pluginManager;

  @Argument(name = "plugins", description = "The name of the plugin to enable.", required = true, multiValued = true)
  @Completion(PluginNameCompleter.class)
  private List<String> plugins;

  @Option(name = "--persist", description = "Persist changes", required = false, multiValued = false)
  private boolean persist = false;

  @Override
  public Object execute() throws Exception {
    var properties = configRepository.getConfigAdmin()
        .getConfiguration(PluginManagerImpl.class.getName()).getProperties();

    Set<String> available = pluginManager.listAvailablePlugins();
    for (String plugin : plugins) {
      if (!available.contains(plugin)) {
        throw new IllegalArgumentException("Plugin " + plugin + " is invalid");
      } else {
        properties.put(plugin, "on");
      }
    }

    if (persist) {
      Map<String, Object> propertiesMap = Collections.list(properties.keys())
          .stream()
          .collect(Collectors.toMap(Function.identity(), properties::get));
      configRepository.update(PluginManagerImpl.class.getName(), propertiesMap);
    } else {
      Configuration c = configRepository.getConfigAdmin().getConfiguration(PluginManagerImpl.class.getName());
      c.update(properties);
    }
    return null;
  }
}
