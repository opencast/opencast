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

var Opencast = Opencast || {};

Opencast.Plugin_Controller =
    (function ()
     {
	 var plugins = [];
	 
	 /**
	  * registers a plugin
	  * @param plugin Plugin to register
	  * @return true when plugin has been registered successfully, false else
	  **/
	 function registerPlugin(plugin)
	 {
	     if(!plugin)
	     {
		 return false;
	     }

	     if(!isRegistered(plugin))
	     {
		 plugins[plugins.length] = plugin;
		 return true;
	     }
	     return false;
	 }

	 /**
	  * unregisters a plugin
	  * @param plugin plugin to unregister
	  * @return true when plugin has been unregistered successfully, false else
	  **/
	 function unregisterPlugin(plugin)
	 {
	     if(!plugin)
	     {
		 return false;
	     }

	     for(var i = 0; i < plugins.length; ++i)
	     {
		 if(plugins[i] == plugin)
		 {
		     var pl = plugins.pop();
		     if(i != plugins.length)
		     {
			 plugins[i] = pl;
		     }
		     return true;
		 }
	     }
	     return false;
	 }

	 /**
	  * checks whether the plugin is registered
	  * @param plugin plugin check
	  * @return true when plugin is registered, false else
	  **/
	 function isRegistered(plugin)
	 {
	     if(!plugin)
	     {
		 return false;
	     }

	     for(var i = 0; i < plugins.length; ++i)
	     {
		 if(plugins[i] == plugin)
		 {
		     return true;
		 }
	     }
	     return false;
	 }

	 /**
	  * tries to show a plugin by calling the 'show' function
	  * @param plugin plugin to show
	  * @return true when plugin has been successfully shown, false else
	  **/
	 function show(plugin)
	 {
	     if(plugin && plugin.show)
	     {
		 plugin.show();
		 return true;
	     }
	     return false;
	 }

	 /**
	  * tries to hide a plugin by calling the 'hide' function
	  * @param plugin plugin to hide
	  * @return true when plugin has been successfully hidden, false else
	  **/
	 function hide(plugin)
	 {
	     if(plugin && plugin.hide)
	     {
		 plugin.hide();
		 return true;
	     }
	     return false;
	 }

	 /**
	  * tries to hide all plugins
	  * @param pluginToExclude (optional) does not hide that specific plugin
	  **/
	 function hideAll(pluginToExclude)
	 {
	     for(var i = 0; i < plugins.length; ++i)
	     {
		 if(pluginToExclude && (plugins[i] != pluginToExclude))
		 {
		     hide(plugins[i]);
		 }
	     }
	 }
	 
	 return {
	     registerPlugin: registerPlugin,
	     unregisterPlugin: unregisterPlugin,
	     isRegistered: isRegistered,
	     hide: hide,
	     hideAll: hideAll
	 };
     }());