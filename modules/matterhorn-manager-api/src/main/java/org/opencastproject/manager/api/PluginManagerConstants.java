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

package org.opencastproject.manager.api;

/**
 * This interface represents some constatn's.
 * 
 * @author Leonid Oldenburger
 */
public interface PluginManagerConstants {
	
	// pathes
	String PLUGIN_TMP_PATH = "etc/plugins/TMP/";
	String PLUGIN_PATH  = "etc/plugins/plugins/";
	String PLUGIN_BACKUP_PATH = "etc/plugins/backup/";
	String MARKET_PATH = "etc/plugins/market/";
	String BUNDLE_DIR_PATH = "lib/matterhorn/";
	String WORKFLOWS_PATH = "etc/workflows/";
	String FELIX_CACHE_PATH = "work/felix-cache";
	
	// states
	String INSTALL_PLUGIN = "install";
	String DEINSTALL_PLUGIN = "deinstall";
	
	String ACTIVATE_PLUGIN = "activate";
	String DEACTIVATE_PLUGIN = "deactivate";
	
	String UPDATE_PLUGIN = "update";
	String RESTORE_PLUGIN = "restore";
	String DELETE_RESTORED = "delete";
	
	String RESTART_SYSTEM = "restart";
	
	// index.html constant
	String BACKUP_DATA = "backup_data";
	String GLOBAL_DATA = "global_data";
	String PLUGINS_DATA = "plugins_data";
	String PLUGINS_INFO_DATA = "plugins_info_data";
	
	// plug-ins information
	String PLUGIN_ID = "id";
	String PLUGIN_NAME = "plugin_name";
	String PLUGIN_VERSION = "plugin_version";
	String PLUGIN_STATE = "plugin_state";
	
	String PLUGIN_MANIFEST_FILE = "Manifest.xml";
	
	int PLUGIN_VALUES = 0;
	int PLUGIN_BACKUP_VALUES = 1;
}
