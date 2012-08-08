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
ocNavigationMenu = new (function() {
	
	this.init = function(visualizations, callback) {
		loadTemplate();		
		loadSeries(callback);
		loadVisualizations(visualizations, callback);
	};

	// Loads a template from a tpl file. 
	function loadTemplate() {
		$('#addMenu').jqotesubtpl("templates/menu.tpl", {});
	}

	// Load the series information from the server so that we can populate the series selector. 
	function loadSeries(callback) {	
		$.getJSON(seriesURL, function(data) {
			displaySeries(callback, data);
		});	
	}

	// Add the different series to the selector. 
	function displaySeries(callback, data) {
		for(var i = 0; i < data.catalogs.length; i++) {
			var series = data.catalogs[i];
			series = series["http://purl.org/dc/terms/"];
			$("#series").append($("<option></option>").attr("value", series.identifier[0].value).text(series.title[0].value));
		}
		if(callback != null) {
			$("#series").change(function() {
				callback();
			});
			// Now that we have the series let's refresh. 
			callback();
		}
	}

	// Load the visualizations into the visualization selector from the visualization switcher. 
	function loadVisualizations(visualizations, callback) {
		
		for(var i = 0 ; i < visualizations.length; i++) {
			var visualization = visualizations[i];	
			$("#addMenu").append("<a href='#/" + visualization.getPath() + "'>"+ visualization.getTitle() + "</a></br>");
		}		
	}

	// Get the currently selected series. 
	this.getSeries = function() {
		return $("#series").val();
	};

	// Get the currently selected visualization. 
	this.getVisualization = function() {
		// substring to get rid of leading /
		var path = $.address.value().substring(1);
		var indexOfVisualization = -1;
		for(var i = 0; ocVisualizationSwitcher.visualizations[i] != undefined; i++){
			if(ocVisualizationSwitcher.visualizations[i].getPath() == path){
				indexOfVisualization = i;
			}
		}
		
		if(indexOfVisualization >= 0) {
			return indexOfVisualization;
		}
		else {
			return 0;
		}
	};
})();
