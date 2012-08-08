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
ocUserSummaryVisualization = new (function() {
	// title is what shows up in the selector for visualization and the title bar. 	
	this.title = "User Summary";
	this.sortField = 'Name';
    this.sortOrder = 'ASC';
	
    var SORT_FIELDS = {
    		'Name' : 'userId',
    		'Sessions' : 'sessionCount',
    		'UniqueVideos' : 'uniqueMediapackages',
    		'TimeWatched' : 'length',
    		'LastWatched' : 'last'
    }
    
	this.getTitle = function () {
		return this.title;
	}
	
	this.summaries = new Array();
	
	/** Create a comma separated value file that can be downloaded by the user for ease of importing into a spreadsheet. **/
	this.setupCsvDownload = function(){
		var str = 'Name,Sessions,Unique Videos,Total Time Watched,Date Last Watched,Time Last Watched,Time Zone\r\n';
		for ( var i = 0; i < ocUserSummaryVisualization.summaries.summary.length; i++) {
			var last = ocUserSummaryVisualization.summaries.summary[i].last; 
			var date = last.substring(0, last.indexOf('T'));
			var time = last.substring(last.indexOf('T') + 1, last.indexOf('T') + 9);
			var timeZone = last.substring(last.indexOf('T') + 9 + 5);
			var line = '';
			line += ocUserSummaryVisualization.summaries.summary[i].userId + ",";
			line += ocUserSummaryVisualization.summaries.summary[i].sessionCount + ",";
			line += ocUserSummaryVisualization.summaries.summary[i].uniqueMediapackages + ",";
			line += ocUserSummaryVisualization.summaries.summary[i].length + ",";
			line += date + ",";
			line += time + ",";
			line += timeZone;
			line.slice(0, line.Length - 1);
			str += line + '\r\n';
        }
        window.open( "data:text/csv;charset=utf-8," + escape(str));
	};
	
	this.init = function() {
		
	};

	this.alerts = function () {
		alert("Something has called this!");
	}
	
	
	// When the series or visualization changes in the ui refresh should be called. 
	this.refresh = function (series, visualizationIndex) {
		$("#addContent").mask("Loading...", 500);		
		loadUserSummaries(series, visualizationIndex);
	};
	
	function loadUserSummaries(series, visualizationIndex) {
		var url = substitute(getUsersSummaryURL, {seriesID: series, type: "HEARTBEAT"});
		$.getJSON(url, function(data, textStatus, jqXHR) {
			parseUserSummaries(data, series, visualizationIndex);
		});
	}
	
	function parseUserSummaries(data, series, visualizationIndex) {
		ocUserSummaryVisualization.summaries = new Object();
		ocUserSummaryVisualization.summaries.summary = new Array();
		
		if(data.summaries.summary != undefined && data.summaries.summary[0] == undefined) {
			ocUserSummaryVisualization.summaries.summary.push(data.summaries.summary);
		}
		else if (data.summaries.summary != undefined && data.summaries.summary[0] != undefined ) {
			for (var i = 0; data.summaries.summary[i] != undefined; i++) {
				ocUserSummaryVisualization.summaries.summary.push(data.summaries.summary[i]);
			}
		}

				
		 // sorting if specified
		if (ocUserSummaryVisualization.sortField != null) {
			ocUserSummaryVisualization.summaries.summary.sort(
					function(a, b) {
						var sort = SORT_FIELDS[ocUserSummaryVisualization.sortField]; 
						if (ocUserSummaryVisualization.sortOrder == 'DESC') {
							if(a[sort].toString().toLowerCase() < b[sort].toString().toLowerCase()) 
								return 1;
							else if(a[sort].toString().toLowerCase() > b[sort].toString().toLowerCase())
								return -1;
							else
								return 0;
						}
						else {
							if(a[sort].toString().toLowerCase() < b[sort].toString().toLowerCase()) 
								return -1;
							else if(a[sort].toString().toLowerCase() > b[sort].toString().toLowerCase())
								return 1;
							else
								return 0;
						}
					});
		}
		displayUserSummaries(visualizationIndex, ocUserSummaryVisualization);
	}
	


	function displayUserSummaries(visualizationIndex, data) {
		$('#vis' + visualizationIndex).jqotesubtpl(
				"templates/visualization-user-summary.tpl", data);
		// $('#vis' + visualizationIndex).empty();
		
		// if results are sorted, display icon indicating sort order in
		// respective table header cell
		if (ocUserSummaryVisualization.sortField != null) {
			var th = $('#sort' + ocUserSummaryVisualization.sortField);
			$(th).find('.sort-icon').removeClass('ui-icon-triangle-2-n-s');
			if (ocUserSummaryVisualization.sortOrder == 'ASC') {
				$(th).find('.sort-icon').addClass('ui-icon-circle-triangle-n');
			} else if (ocUserSummaryVisualization.sortOrder == 'DESC') {
				$(th).find('.sort-icon').addClass('ui-icon-circle-triangle-s');
			}
		}
		

				$('.sortable').click(
				function() {
					var series = ocNavigationMenu.getSeries();
					var visualization = ocNavigationMenu.getVisualization();
					var sortDesc = $(this).find('.sort-icon').hasClass(
							'ui-icon-circle-triangle-s');
					var sortField = ($(this).attr('id')).substr(4);
					$('#summaryTable th .sort-icon').removeClass(
							'ui-icon-circle-triangle-s').removeClass(
							'ui-icon-circle-triangle-n').addClass(
							'ui-icon-triangle-2-n-s');
					if (sortDesc) {
						ocUserSummaryVisualization.sortField = sortField;
						ocUserSummaryVisualization.sortOrder = 'ASC';
						ocUserSummaryVisualization.refresh(series, visualization);
					} else {
						ocUserSummaryVisualization.sortField = sortField;
						ocUserSummaryVisualization.sortOrder = 'DESC';
						ocUserSummaryVisualization.refresh(series, visualization);
					}
				});
		setupCsvDownload();
		$("#addContent").unmask();
	}
	
	function setupCsvDownload(){
		$('#downloadCSVButton').click(ocUserSummaryVisualization.setupCsvDownload);
	}
})();
