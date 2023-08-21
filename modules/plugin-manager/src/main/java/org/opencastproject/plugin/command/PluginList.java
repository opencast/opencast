/*
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

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Row;
import org.apache.karaf.shell.support.table.ShellTable;

import java.util.ArrayList;
import java.util.Set;

@Service
@Command(scope = "opencast", name = "plugin-list", description = "List available plugins")
public class PluginList implements Action {

  @Reference
  private FeaturesService featuresService;

  @Reference
  private PluginManager pluginManager;

  @Option(name = "--installed", description = "Show only installed plugins", required = false, multiValued = false)
  private boolean installed;

  @Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
  private boolean noFormat;

  @Override
  public Object execute() throws Exception {
    ShellTable table = new ShellTable();
    table.column("ID");
    table.column("State");

    Set<String> plugins;
    if (installed) {
      plugins = pluginManager.listInstalledPlugins();
    } else {
      plugins = pluginManager.listAvailablePlugins();
    }

    for (String plugin: plugins) {
      ArrayList<Object> rowData = new ArrayList<>();
      rowData.add(plugin);
      rowData.add(featuresService.getState(featuresService.getFeature(plugin).getId()));
      Row row = table.addRow();
      row.addContent(rowData);
    }
    table.print(System.out, !noFormat);
    return null;
  }

}
