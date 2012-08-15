/**
 *  Copyright 2009 The Regents of the University of California
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

/* @namespace Holds functions and properites related to visualization and series selection UIs. */
ocPopularThisWeekVisualization = new (function() {
	// title is what shows up in the selector for visualization and the title bar. 	
	this.title = "Change this title to show up in the visualization selector and title bar.";
	this.getTitle = function () {
		return this.title;
	}

	this.init = function() {
		loadTemplate();
	};

	// Load the template from a tpl file and add all of the visualizations to it. 
	function loadTemplate() {
		alert("Starting up " + this.title + " templating!");	
	}

	// When the series or visualization changes in the ui refresh should be called. 
	this.refresh = function (series, visualization) {
		alert("Starting up up " + this.title + "  refreshing!");
	};
})();
