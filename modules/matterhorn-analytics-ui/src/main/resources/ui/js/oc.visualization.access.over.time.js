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
ocAccessOverTimeVisualization = new (function() {
	// The array that will be used to store all of the episodes. 
	this.episodes = new Array();

	// Constants	
	// title is what shows up in the selector for visualization and the title bar. 	
	this.title = "Access Over Time";

	this.getTitle = function () {
		return this.title;
	}
	
	// the saved start date, end date, and interval from the widgets
	var currentTime = new Date();
	//this.startDateString = getStartStringFromDate(currentTime);
	this.startDateString = "";
	this.endDateString = getTime(currentTime);
	this.interval = dayInSeconds;
	this.intervalSelection = "1day";
	
	this.init = function() {
		
	};
	
	// When the series or visualization changes in the ui refresh should be called. 
	this.refresh = function (series, visualizationIndex) {		
		$("#addContent").mask("Loading...", 500);
		if(ocAccessOverTimeVisualization.startDateString == undefined || ocAccessOverTimeVisualization.startDateString == "") {
			var url = substitute(firstViewForSeriesURL, {seriesID: series});			
			$.getJSON(url, function(data, textStatus, jqXHR) {
				// set the start date from the data returned											
				if(data.annotations != undefined && data.annotations.annotation != undefined && data.annotations.annotation.created != undefined){
					var firstDateOfViewDate = new Date(data.annotations.annotation.created);	
				} else {
					var firstDateOfViewDate = new Date();
				}		
				ocAccessOverTimeVisualization.startDateString = getTime(firstDateOfViewDate);				
				loadStartDateCallback(series, visualizationIndex);
			});
		} else {
			loadStartDateCallback(series, visualizationIndex);
		}
	}
	
	function loadStartDateCallback(series, visualizationIndex) {
		ocAccessOverTimeVisualization.episodes = new Array();
		loadTemplate(series, visualizationIndex);
		// Pass in loadViewsAndWatches as a callback function when all async calls are made, same with displayVisualization is called after loadViewsAndWatches finishes. 
		loadEpisodes(series, visualizationIndex, loadedEpisodesCallback);
	}

	function loadedEpisodesCallback(series, visualizationIndex, episodes) {
		ocAccessOverTimeVisualization.episodes = episodes;
		loadAccessOverTime(series, visualizationIndex, displayVisualization); 
	}	
	
	// Load the template from a tpl file and add all of the visualizations to it. 
	function loadTemplate(series, visualizationIndex) {	
		$('#vis' + visualizationIndex).jqotesubtpl("templates/visualization-access-over-time.tpl", {});
	}

	function loadAccessOverTime(series, visualizationIndex, callback) {
		ocAccessOverTimeVisualization.asyncCalls = 0;		
		for(var i = 0; i < ocAccessOverTimeVisualization.episodes.length; i++) {	
			setupGetViewsAndWatches(i, visualizationIndex, callback);	
		}
	}

	function setupGetViewsAndWatches(i, visualizationIndex, callback) {
		var episode = ocAccessOverTimeVisualization.episodes[i];			
		
		var url = substitute(getEpisodeViewsAndWatchesURL, {episodeID: episode.result.mediapackage.id, start: ocAccessOverTimeVisualization.startDateString, end: ocAccessOverTimeVisualization.endDateString, interval: ocAccessOverTimeVisualization.interval});
		$.getJSON(url, function(data, textStatus, jqXHR) {
			parseViewsAndWatches(data, episode, visualizationIndex, callback);
		});
	}
		
	function parseViewsAndWatches(data, episode, visualizationIndex, callback) {
		episode.viewItems = data["view-collection"]["view-item"];
		ocAccessOverTimeVisualization.asyncCalls++;
		if(ocAccessOverTimeVisualization.asyncCalls >= ocAccessOverTimeVisualization.episodes.length) {
			callback(visualizationIndex);
		}
	}

	function addTwoArrays(array1, array2) {
		var data = new Array();
		
		if(array2.length > array1.length) {
			var temp = array1;
			array1 = array2;
			array2 = temp;
		}		
		for(var i = 0; i < array1.length; i++) {
			var array1Value = array1[i];
			var array2Value = 0;
			if(i < array2.length) {
				array2Value = array2[i];
			}
			data.push(array1Value + array2Value);
		}
				
		return data;
	}
	function removeLeadingZeros(string) {
		if(string.substring(0, 1) == "0") {
			string = string.substring(1);
		}
		return string;
	}
	function getMonth(monthNum) {		
		switch(monthNum) {
		case "1":
			return "Jan";
			break;
		case "2":
			return "Feb";
			break;
		case "3":
			return "Mar";
			break;					
		case "4":
			return "Apr";
			break;
		case "5":
			return "May";
			break;
		case "6":
			return "Jun";
			break;
		case "7":
			return "Jul";
			break;
		case "8":
			return "Aug";
			break;
		case "9":
			return "Sep";
			break;
		case "10":
			return "Oct";
			break;
		case "11":
			return "Nov";
			break;
		case "12":
			return "Dec";
			break;
		default:
			return "";
			break;
		}
	}
	function getFormattedDate(date, addTo) {		
		try {
			date = date.toString();
			var year = date.substring(0, 4);		
			var month = getMonth(removeLeadingZeros(date.substring(4, 6)));		
			var day = removeLeadingZeros(date.substring(6, 8));				
			var hour = date.substring(8, 10);		
			var minute = date.substring(10);	
			var dateToReturn = month + " " + day + " " + year + " " + hour + ":" + minute;
			if(addTo) {
				dateToReturn = dateToReturn + " to";
			}
			return dateToReturn
		} catch(e) {			
			return "";
		}		
	}
	function subtractTwoArrays(array1, array2) {
		var data = new Array();
		for(var i = 0; i < array1.length; i++) {
			data.push(array1[i] - array2[i]);
		}
		
		return data;
	}
	
	function displayVisualization(visualizationIndex) {	
		var vis = "vis" + visualizationIndex;
		
		var justViews = new Array();				
		for(var i = 0;  i < ocAccessOverTimeVisualization.episodes.length; i++) {
			var episode = ocAccessOverTimeVisualization.episodes[i];
			var episodeViews = new Array();
			// find episode views from episode.views
			// Handle a single view item
			if (episode.viewItems.length == undefined) {
				episodeViews.push(episode.viewItems.views);
			} else {
				// Handle more than one view item
				for(var j = 0; j < episode.viewItems.length; j++) {
					episodeViews.push(episode.viewItems[j].views);
				}	
			}
			justViews.push(episodeViews);
		}
		
		var startDates = new Array();
		var endDates = new Array();
		
		if(ocAccessOverTimeVisualization.episodes.length > 0) {
			var firstEpisode = ocAccessOverTimeVisualization.episodes[0];
			if(firstEpisode.viewItems.length == undefined) {
				startDates.push(firstEpisode.viewItems.start);
				endDates.push(firstEpisode.viewItems.end);
			} else {
				for(var i = 0; i < firstEpisode.viewItems.length; i++) {
					startDates.push(firstEpisode.viewItems[i].start);
					endDates.push(firstEpisode.viewItems[i].end);
				}	
			}
						
		}
					
		var stackedGraphArray = new Array();
		stackedGraphArray.push(justViews[0]);
		for(var i = 1; i < justViews.length; i++) {
			var nextArray = addTwoArrays(stackedGraphArray[i - 1], justViews[i]);
			stackedGraphArray.push(nextArray);
		}
		
		if(stackedGraphArray.length <= 0) {
			return;
		}
		
		var linearData = new Array();
		for(var i = 0; i < stackedGraphArray.length; i++) {
			for(var j = 0; j < stackedGraphArray[i].length; j++) {
				linearData.push(stackedGraphArray[i][j]);
			}
		}
		var maxLength = stackedGraphArray[0].length;
		for(var i = 0; i < stackedGraphArray.length; i++) {
			if(stackedGraphArray[i].length > maxLength) {
				maxLength = stackedGraphArray[i].length;
			}
		}
						
		var w = $("#addContent").width() - 300,
		h = $("#addContent").height() - 200,
		margin = 60,
		radiusOfPoints = 4,
		y = d3.scale.linear().domain([0, d3.max(stackedGraphArray[stackedGraphArray.length -1])]).range([0 + margin, h - margin]),		
		rgbScale = d3.scale.linear().domain([0, ocAccessOverTimeVisualization.episodes.length - 1]).range(["yellow", "green", "black"]);	
		
		var x = d3.scale.linear().domain([0, maxLength - 1]).range([0 + margin, w - margin]);
		if(maxLength < 2) {
			x = d3.scale.linear().domain([0, 1]).range([0 + margin, w - margin]);
		}			
		
		var vis = d3.select("#aot")
			.append("svg:svg")
			.attr("width", w)
			.attr("height", h);
			
		var g = vis.append("svg:g")
			.attr("transform", "translate(0, " + h + ")");
			
		var area = d3.svg.area()
			.x(function(d) {return x(d, x)})
			.y0(function(d) {return -1 * y(d) })
			.y1(function(d) {return -1 * y(d) });
			
		var areas = new Array();
		var firstArea = d3.svg.area()
			.x(function(d, i) {return x(i)})
			.y1(function(d) {return -1 * y(d)})
			.y0(function() {return -1 * margin});
		areas.push(firstArea);	
		for(var i = 1; i < stackedGraphArray.length; i++) {
			areas.push(createArea(i));
		}
	
		function createArea(line) {
			var area = d3.svg.area()
				.x(function(d, i) {return x(i)})
				.y1(function(d) {return -1 * y(d)})
				.y0(function(d, i) {return -1 * y(linearData[(line - 1) * maxLength + i])});
			return area;
		}
	
		function color(i) {
			return rgb = d3.rgb(rgbScale(i));
		}
	
		for(var i = 0; i < areas.length; i++) {
			var data = getDataFromLinearData(i);
			g.append("svg:path")
				.attr("value", i)
				.attr("d", areas[i](data))
				.style("fill", color(i))
				.style("stroke", "black")
				.style("opacity", "0.4")
				.on("mouseover", fade(1))
				.on("mouseout", fade(0.5));
		}
	
		function getDataFromLinearData(line) {
			var data = new Array();
			for(var i = 0; i < maxLength; i++) {
				data.push(linearData[line * maxLength + i]);
			}
			return data;
		}
			
		var xAxisLine = g.append("svg:line")
			.attr("x1", x(0))
			.attr("y1", -1 * y(0))
			.attr("x2", x(w))			
			.attr("y2", -1 * y(0));
		g.append("svg:line")
			.attr("x1", x(0))
			.attr("y1", -1 * y(0))
			.attr("x2", x(0))
			.attr("y2", -1 * y(d3.max(stackedGraphArray[stackedGraphArray.length - 1])));			
				
		var numXTicks = 5;
		if(maxLength < 5) {
			numXTicks = maxLength;
		}		
		
		function isEven(value) {
			if(value % 2 == 0) {
				return true;
			} else {
				return false;
			}
		}
		
		g.selectAll(".xLabel")
				.data(x.ticks(numXTicks))
			.enter().append("svg:text")
				.attr("class", "xLabel")
				.text(function(d,i) {return getFormattedDate(startDates[d], true)})
				.attr("x", function(d) {return x(d)})
				.attr("y", function(d, i) {if(isEven(i)) {return -15} else {return -45}})
				.attr("font-size", "8pt")
				.attr("text-anchor", "middle");
		
		g.selectAll(".xLabel2")
				.data(x.ticks(numXTicks))
			.enter().append("svg:text")
				.attr("class", "xLabel")
				.text(function(d,i) {return getFormattedDate(endDates[d], false)})
				.attr("x", function(d) {return x(d)})
				.attr("y", function(d, i) {if(isEven(i)) {return 0} else {return -30}})
				.attr("font-size", "8pt")
				.attr("text-anchor", "middle");			
		
		function getMaxViews() {
			var maxViews = 0;
			for(var i = 0; i < linearData.length; i++) {
				if(linearData[i] > maxViews) {
					maxViews = linearData[i];
				}
			}
			return maxViews;
		}
		
		var numYTicks = 5;
		if(getMaxViews() < 3) {
			numYTicks = 1;
		}
		
		g.selectAll(".yLabel")
				.data(y.ticks(numYTicks))
			.enter().append("svg:text")
				.attr("class", "yLabel")
				.text(String)
				.attr("x", 15)
				.attr("y", function(d) { return -1 * y(d) })
				.attr("text-anchor", "right")
				.attr("font-size", "8pt")
				.attr("dy", 4);				
			
		var labels = new Array();					
		
		for(var j = 0; j < stackedGraphArray.length; j++) {
			var data = getDataFromLinearData(j);
			var difference = data;
			if(j > 0) {
				data2 = getDataFromLinearData(j - 1);
				difference = subtractTwoArrays(data, data2);	
			}
			
			g.selectAll("circle" + i)
				.data(data)
			.enter().append("svg:circle")
				.attr("fill", "black")
				.attr("cx", function(d, i) {return x(i);})
				.attr("cy", function(d, i) {return -1 * y(d);})
				.attr("r", function(d, i) {return radiusOfPoints;})
				//.attr("value", function(d, i) {var last = (i==maxLength -1?lastIntervalDate:dates[i + 1]); labels.push("Number of views for episode between " + getFormattedDate(dates[i]) + " and " + getFormattedDate(last) + ": " + difference[i]); var labelNumber = j * maxLength + i; return labelNumber})
				.attr("value", function(d, i) {
					labels.push("Number of views for episode between " + getFormattedDate(startDates[i]) + " and " + getFormattedDate(endDates[i]) + ": " + difference[i]);
					var labelNumber = j * maxLength + i;
					return labelNumber;
				})
				.style("opacity", 0);
		}				
		
		function fade(opacity) {
			return function(g, i) {
				d3.select(this)			
					.transition()
					.style("opacity", opacity);
			};
		}
	
		$("circle").tipsy({
			gravity: "w",
			html: true,
			title: function() {		
				var indexOfLabel = $(this).attr("value");		
				return labels[indexOfLabel];
			}
		});
	
		$("path").tipsy({
			gravity: "w",
			html: true,
			title: function() {
				var indexOfTitle = $(this).attr("value");
				return ocAccessOverTimeVisualization.episodes[indexOfTitle].result.mediapackage.title;
			}
		});	
		
		function validWidgetValues() {
			// start date must be before end date
			var selectedStartDate = new Date($("#startDate").val());
			var selectedEndDate = new Date($("#endDate").val());
			if(selectedStartDate > selectedEndDate) {
				alert("Please choose a start date before the end date");
				return false;
			}
			return true;
		}
		
		function updateWidgetValues() {
			var selectedStartDateString = $("#startDate").val();
			var selectedEndDateString = $("#endDate").val();
			var selectedStartDate = new Date(selectedStartDateString);
			selectedStartDate.setHours(0);
			selectedStartDate.setMinutes(0);			
			var selectedEndDate = new Date(selectedEndDateString);
			selectedEndDate.setHours(23);
			selectedEndDate.setMinutes(59);			
			
			ocAccessOverTimeVisualization.startDateString = getTime(selectedStartDate);
			ocAccessOverTimeVisualization.endDateString = getTime(selectedEndDate);
			var intervalString = $("#granularity").val();			
			ocAccessOverTimeVisualization.intervalSelection = intervalString;
			if("1hour" == intervalString) {
				ocAccessOverTimeVisualization.interval = hourInSeconds;
			} else if("12hour" == intervalString) {
				ocAccessOverTimeVisualization.interval = hourInSeconds * 12;
			} else if("1day" == intervalString) {
				ocAccessOverTimeVisualization.interval = dayInSeconds;
			} else if("2days" == intervalString) {
				ocAccessOverTimeVisualization.interval = dayInSeconds * 2;				
			} else if("1week" == intervalString) {
				ocAccessOverTimeVisualization.interval = weekInSeconds;
			}			
		}
		function getDateFromString(dateString) {
			// string will look like 201205300830 (yyyyMMddhhmm)
			var year = dateString.substring(0, 4);		
			var month = dateString.substring(4, 6) - 1;		
			var day = dateString.substring(6, 8);			
			return new Date(year, month, day);		
		}
		
		$("#startDate").datepicker({
	      showOn: "both",
	      buttonImage: "img/icons/calendar.gif",
	      buttonImageOnly: true,
	      dateFormat: "MM dd, yy"
	    });
		$("#startDate").datepicker("setDate", getDateFromString(ocAccessOverTimeVisualization.startDateString));
		$("#startDate").change(function() {
			if(validWidgetValues()) {
				updateWidgetValues();
				refresh();	
			}			
		});
		
		$("#endDate").datepicker({
		      showOn: "both",
		      buttonImage: "img/icons/calendar.gif",
		      buttonImageOnly: true,
		      dateFormat: "MM dd, yy"
	    });
		$("#endDate").datepicker("setDate", getDateFromString(ocAccessOverTimeVisualization.endDateString));
		$("#endDate").change(function() {
			if(validWidgetValues()) {
				updateWidgetValues();
				refresh();	
			}			
		});
		$("#granularity").change(function() {
			if(validWidgetValues()) {
				updateWidgetValues();
				refresh();	
			}			
		});
		$("#granularity").val(ocAccessOverTimeVisualization.intervalSelection);
		$("#addContent").unmask();
	}
})();
