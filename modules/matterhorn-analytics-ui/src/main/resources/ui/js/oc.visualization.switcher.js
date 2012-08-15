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
ocVisualizationSwitcher = new (function() {

	// The collection of visualizations that we are able to provide. 	
	this.visualizations = [		
		{
			// title to show in the selector
			getTitle: function() {
				return ocPopularThisWeekVisualization.getTitle();
			},
			getPath: function() {
				return "popularThisWeek";
			},
			// initialization code to run
			init: function(series) {
				ocPopularThisWeekVisualization.init(series);
				document.title = ocPopularThisWeekVisualization.getTitle();
			},
			refresh: function(series, visualization) {
				ocPopularThisWeekVisualization.refresh(series, visualization);
				document.title = ocPopularThisWeekVisualization.getTitle();
			}
		},
		{
			title: "Accesses over time",
			getTitle: function() {
				return this.title;
			},
			getPath: function() {
				return "accessesOverTime";
			},
			init: function(series) {
				ocAccessOverTimeVisualization.init(series);
				document.title = ocAccessOverTimeVisualization.title;
			},
			refresh: function(series, visualization) {
				ocAccessOverTimeVisualization.refresh(series, visualization);
				document.title = ocAccessOverTimeVisualization.getTitle();
			}
		},
        {
			title: "User Summary",
			getTitle: function() {
				return this.title;
			},
			getPath: function() {
				return "userSummary";
			},
			init: function(series) {
				ocUserSummaryVisualization.init(series);
				document.title = ocUserSummaryVisualization.title;
			},
			refresh: function(series, visualization) {
				ocUserSummaryVisualization.refresh(series, visualization);
				document.title = ocUserSummaryVisualization.getTitle();
			}
        }
	];

	this.init = function() {
		var defaultPath = ocVisualizationSwitcher.visualizations[0].getPath();
		if ($.address.value() == '/') {
			$.address.value(defaultPath);
		}
		var path = $.address.value();
		     
		// Address handler
		$.address
			.init(function(event) {
		           

			})
			.change(function(event) {
				if ($.address.value() != path) {
					location.hash = "#" + $.address.value();
					location.reload();
				}
			})
			.externalChange(function(event) {
				if ($.address.value() != path) {
					location.hash = "#" + $.address.value();
					location.reload();
				}
			})
			.history(true);
		loadTemplate();
	};

	// Load the template from a tpl file and add all of the visualizations to it. 
	function loadTemplate() {
		$('#addContent').jqotesubtpl("templates/visualization-switcher.tpl", {});
		
		for(var i = 0; i < ocVisualizationSwitcher.visualizations.length; i++) {
			$('#addContent').append('<div id="vis' + i + '" class="hidden">'+ ocVisualizationSwitcher.visualizations[i].getTitle() + '</div>');	
		}
		
		// Since the selector will be on the first visualization set it to current and unhidden. 
		if(ocVisualizationSwitcher.visualizations.length > 0) {
			$('#vis0').removeClass("hidden");
			$('#vis0').addClass("current");	
		}
	}

	// When the series or visualization changes in the ui refresh should be called. 
	this.refresh = function (series, visualization) {
		$("#instructions").addClass("hidden");
		
		if(series != null) {
			ocVisualizationSwitcher.visualizations[visualization].init(series)
		}

		if(series !== null && visualization !== null) {
			$(".current").addClass("hidden");
			$(".current").removeClass("current");
			$("#vis" + visualization).removeClass("hidden");
			$("#vis" + visualization).addClass("current");	
			ocVisualizationSwitcher.visualizations[visualization].refresh(series, visualization)
		}
	};	
})();
