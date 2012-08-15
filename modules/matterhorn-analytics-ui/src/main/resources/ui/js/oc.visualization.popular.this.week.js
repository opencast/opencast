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
	// The array that will be used to store all of the episodes. 
	this.episodes = new Array();

	// Constants	
	// title is what shows up in the selector for visualization and the title bar. 	
	this.title = "Popular This Week";

	this.getTitle = function () {
		return this.title;
	}

	this.init = function() {

	};

	// When the series or visualization changes in the ui refresh should be called. 
	this.refresh = function (series, visualizationIndex) {
		$("#addContent").mask("Loading...", 500);		
		ocPopularThisWeekVisualization.episodes = new Array();
		loadTemplate(series, visualizationIndex);
		// Pass in loadViewsAndWatches as a callback function when all async calls are made, same with displayVisualization is called after loadViewsAndWatches finishes. 
		loadEpisodes(series, visualizationIndex, loadedEpisodesCallback);
	}

	function loadedEpisodesCallback(series, visualizationIndex, episodes) {
		ocPopularThisWeekVisualization.episodes = episodes;
		loadViewsAndWatches(series, visualizationIndex, displayVisualization); 
	}
	
	
	// Load the template from a tpl file and add all of the visualizations to it. 
	function loadTemplate(series, visualizationIndex) {	
		$('#vis' + visualizationIndex).jqotesubtpl("templates/visualization-popular-this-week.tpl", {});
	}

	function loadViewsAndWatches(series, visualizationIndex, callback) {
		ocPopularThisWeekVisualization.asyncCalls = 0;		
		for(var i = 0; i < ocPopularThisWeekVisualization.episodes.length; i++) {	
			setupGetViewsAndWatches(i, visualizationIndex, callback);	
		}
	}

	function setupGetViewsAndWatches(i, visualizationIndex, callback) {
		var episode = ocPopularThisWeekVisualization.episodes[i];
		var currentTime = new Date();
		var weekAgo = new Date();
		weekAgo.setDate(currentTime.getDate() - 7);
		
		var weekAgoString = getTime(weekAgo); 
		var currentString = getTime(currentTime);
		
		var url = substitute(getEpisodeViewsAndWatchesURL, {episodeID: episode.result.mediapackage.id, start: weekAgoString, end: currentString, interval: weekInSeconds});
		$.getJSON(url, function(data, textStatus, jqXHR) {
			parseViewsAndWatches(data, episode, visualizationIndex, callback);
		});
	}
		
	function parseViewsAndWatches(data, episode, visualizationIndex, callback) {
		episode.views = data["view-collection"].views;
		episode.played = data["view-collection"].played;
		ocPopularThisWeekVisualization.asyncCalls++;
		if(ocPopularThisWeekVisualization.asyncCalls >= ocPopularThisWeekVisualization.episodes.length) {
			callback(visualizationIndex);
		}
	}

	function displayVisualization(visualizationIndex) {
		ocPopularThisWeekVisualization.episodes.sort(function (a, b) {
			var result = b.played - a.played;
			if(result === 0) {
				result = b.views - a.views;
			}
			return result;
		});
		var vis = "vis" + visualizationIndex;
		var episode = null;
		for(var i = 0;  i < ocPopularThisWeekVisualization.episodes.length; i++) {
			episode = ocPopularThisWeekVisualization.episodes[i];
			$("#" + vis).append(episode.generateHTML());
		}
		// Set the slides to transition
		$('.cyclePopularity').cycle({
			fx: 'fade' // choose your transition type, ex: fade, scrollUp, shuffle, etc...
		});
		$("#addContent").unmask();
	}
})();
