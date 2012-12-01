ocMetrics = new (function() {
	
  var self = this;

  /** 
   * Executed when directly when script is loaded: parses url parameters and
   * returns the configuration object.
   */
  this.Configuration = new (function() {

    // default configuartion
    this.state = 'matterhorn';
    this.refresh = 10000;

    // parse url parameters
    try {
      var p = document.location.href.split('?', 2)[1] || false;
      if (p !== false) {
        p = p.split('&');
        for (i in p) {
          var param = p[i].split('=');
          if (this[param[0]] !== undefined) {
            this[param[0]] = unescape(param[1]);
          }
        }
      }
    } catch (e) {
      alert('Unable to parse url parameters:\n' + e.toString());
    }

    return this;
  })();
  
  this.JmxChartsFactory = function(keepHistorySec, pollInterval, columnsCount) {
	    var jolokia = new Jolokia("/jolokia");
	    var charts = [];
	    var series = [];
	    var monitoredMbeans = [];
	 
	    columnsCount = columnsCount || 1;
	    pollInterval = pollInterval || 5000;
	    var keepPoints = (keepHistorySec || 600) / (pollInterval / 1000);
	    
	    setupPortletsContainer();
	 
	    setInterval(function() {
	        pollAndUpdateCharts();
	    }, pollInterval);
	 
	    this.create = function(options, mbeans) {
	        mbeans = $.makeArray(mbeans);
	        var chart = createChart(options, mbeans);
	        charts.push(chart);
	        series = series.concat(chart.series);
	        monitoredMbeans = monitoredMbeans.concat(mbeans);
	    };
	    
	    this.createStack = function(options, mbeans) {
	    	mbeans = $.makeArray(mbeans);
	    	var chart = createStackChart(options, mbeans);
	        charts.push(chart);
	    	series = series.concat(chart.series);
	    	monitoredMbeans = monitoredMbeans.concat(mbeans);
	    };
	    
	    this.createPlotLine = function(options, mbeans) {
	    	mbeans = $.makeArray(mbeans);
	    	var chart = createPlotLineChart(options, mbeans);
	        charts.push(chart);
	    	series = series.concat(chart.series);
	    	monitoredMbeans = monitoredMbeans.concat(mbeans);
	    };
	    
	    this.createPercentageArea = function(options, mbeans) {
	    	mbeans = $.makeArray(mbeans);
	    	var chart = createPercentageAreaChart(options, mbeans);
	        charts.push(chart);
	    	series = series.concat(chart.series);
	    	monitoredMbeans = monitoredMbeans.concat(mbeans);
	    };
	    
	    this.getCharts = function() {
	    	return charts;
	    };
	    
	    function pollAndUpdateCharts() {
	        var requests = prepareBatchRequest();
	        var responses = jolokia.request(requests);
	        updateCharts(responses);
	    }
	 
	    function createNewPortlet(container, name) {
	    	name = name.substring(name.indexOf('=') + 1);
	    	var column = $('div#' + container + '-tableContainer').find('.column');
	    	var col = (column.find('.portlet').length % columnsCount);
	        return $('div#portlet-template').clone(true)
	            .appendTo(column[col])
	            .removeAttr('id')
	            .find('.title').text((name.length > 50? '...' : '') + name.substring(name.length - 50, name.length)).end()
	            .find('.portlet-content')[0];
	    }
	 
	    function setupPortletsContainer() {
	        var column = $('.column');
	        column.each(function(index, elem) {
		        for(var i = 1; i < columnsCount; ++i){
		            $(elem).clone().appendTo($(elem).parent());
		        }
	        });
	        
	        $(".portlet-header .ui-icon").click(function() {
	            $(this).toggleClass("ui-icon-minusthick").toggleClass("ui-icon-plusthick");
	            $(this).parents(".portlet:first").find(".portlet-content").toggle();
	        });
	        $(".column").disableSelection();
	    }
	 
	    function prepareBatchRequest() {
	        return $.map(monitoredMbeans, function(mbean) {
	            return {
	                type: "read",
	                mbean: mbean.name,
	                attribute: mbean.attribute,
	                path: mbean.path
	            };
	        });
	    }
	 
	    function updateCharts(responses) {
	        var curChart = 0;
	        $.each(responses, function() {
	            var point = {
	                x: this.timestamp * 1000,
	                y: parseFloat(this.value)
	            };
	            var curSeries = series[curChart++];
	            curSeries.addPoint(point, true, curSeries.data.length >= keepPoints);
	        });
	    }
	 
	    function createChart(options, mbeans) {
        	var decimals = options.decimals === undefined ? 0 : options.decimals;
        	var div = options.dividing === undefined ? 1 : options.dividing;
        	var decPoint = options.decPoint === undefined ? ',' : options.decPoint;
        	var thousandsSep = options.thousandsSep === undefined ? "'" : options.thousandsSep;
        	var typeShort = options.typeShort === undefined ? '' : options.typeShort;
	    	
	        return new Highcharts.Chart({
	            chart: {
	                renderTo: createNewPortlet(mbeans[0].container, mbeans[0].name),
	                animation: false,
	                defaultSeriesType: 'area',
	                shadow: false
	            },
	            title: { text: options.title },
	            xAxis: { type: 'datetime' },
	            yAxis: {
	                title: { text: options.type },
	                labels: {
	                    formatter: function() {
	                    	return Highcharts.numberFormat(this.value / div, decimals, decPoint, thousandsSep) + typeShort;
	                    }
	                }
	            },
	            legend: {
	                enabled: true,
	                borderWidth: 0
	            },
	            credits: {enabled: false},
	            exporting: { enabled: false },
	    		tooltip: {
	    			formatter: function() {
	    				return this.series.name + ': ' + Highcharts.numberFormat(this.y / div, decimals, decPoint, thousandsSep) + typeShort;
	    			}
	    		},
	            plotOptions: {
	                area: {
	                    marker: {
	                        enabled: false
	                    }
	                }
	            },
	            series: $.map(mbeans, function(mbean) {
	                return {
	                    data: [],
	                    name: mbean.path || mbean.attribute
	                }
	            })
	        })
	    }
	    
	    function createStackChart(options, mbeans) {
        	var decimals = options.decimals === undefined ? 0 : options.decimals;
        	var div = options.dividing === undefined ? 1 : options.dividing;
        	var decPoint = options.decPoint === undefined ? ',' : options.decPoint;
        	var thousandsSep = options.thousandsSep === undefined ? "'" : options.thousandsSep;
        	var typeShort = options.typeShort === undefined ? '' : options.typeShort;
	    	
	    	return new Highcharts.Chart({
	    		chart: {
	    			renderTo: createNewPortlet(mbeans[0].container, mbeans[0].name),
	    			animation: false,
	    			defaultSeriesType: 'area',
	    			shadow: false
	    		},
	    		title: { text: options.title },
	    		xAxis: { 
	    			type: 'datetime'
	    		},
	    		yAxis: {
	    			title: { text: options.type },
	    			labels: {
	    				formatter: function() {
	    					return Highcharts.numberFormat(this.value / div, decimals, decPoint, thousandsSep) + typeShort;
	    				}
	    			}
	    		},
	    		legend: {
	    			enabled: true,
	    			borderWidth: 0
	    		},
	    		credits: {enabled: false},
	    		exporting: { enabled: false },
	    		tooltip: {
	    			formatter: function() {
	    				return this.series.name + ': ' + Highcharts.numberFormat(this.y / div, decimals, decPoint, thousandsSep) + typeShort;
	    			}
	    		},
	    		plotOptions: {
	    			area: {
	    				stacking: 'normal',
	    				marker: {
	    					enabled: false
	    				}
	    			}
	    		},
	    		series: $.map(mbeans, function(mbean) {
	    			return {
	    				data: [],
	    				name: mbean.path || mbean.attribute
	    			}
	    		})
	    	})
	    }
	    
	    function createPlotLineChart(options, mbeans) {
        	var decimals = options.decimals === undefined ? 0 : options.decimals;
        	var div = options.dividing === undefined ? 1 : options.dividing;
        	var decPoint = options.decPoint === undefined ? ',' : options.decPoint;
        	var thousandsSep = options.thousandsSep === undefined ? "'" : options.thousandsSep;
        	var typeShort = options.typeShort === undefined ? '' : options.typeShort;
        	
	    	return new Highcharts.Chart({
	    		chart: {
	    			renderTo: createNewPortlet(mbeans[0].container, mbeans[0].name),
	    			animation: false,
	    			defaultSeriesType: 'spline',
	    			shadow: false
	    		},
	    		title: { text: options.title },
	    		xAxis: { type: 'datetime' },
	    		yAxis: {
	    			title: { text: options.type },
	    			labels: {
	    				formatter: function() {
	    					return Highcharts.numberFormat(this.value / div, decimals, decPoint, thousandsSep) + typeShort;
	    				}
	    			},
	    			min: 0
	    		},
	    		legend: {
	    			enabled: true,
	    			borderWidth: 0
	    		},
	    		credits: {enabled: false},
	    		exporting: { enabled: false },
	    		tooltip: {
	    			formatter: function() {
	    				return this.series.name + ': ' + Highcharts.numberFormat(this.y / div, decimals, decPoint, thousandsSep) + typeShort;
	    			}
	    		},
	    		plotOptions: {
	    			spline: {
	    				lineWidth: 4,
	    				states: {
	    					hover: {
	    						lineWidth: 5
	    					}
	    				},
	    				marker: {
	    					enabled: false,
	    					states: {
	    						hover: {
	    							enabled: true,
	    							symbol: 'circle',
	    							radius: 5,
	    							lineWidth: 1
	    						}
	    					}
	    				}
	    			}
	    		},
	    		series: $.map(mbeans, function(mbean) {
	    			return {
	    				data: [],
	    				name: mbean.path || mbean.attribute
	    			}
	    		})
	    	})
	    }
	    
	    function createPercentageAreaChart(options, mbeans) {
        	var decimals = options.decimals === undefined ? 0 : options.decimals;
        	var div = options.dividing === undefined ? 1 : options.dividing;
        	var decPoint = options.decPoint === undefined ? ',' : options.decPoint;
        	var thousandsSep = options.thousandsSep === undefined ? "'" : options.thousandsSep;
        	var typeShort = options.typeShort === undefined ? '' : options.typeShort;
        	
	    	return new Highcharts.Chart({
	    		chart: {
	    			renderTo: createNewPortlet(mbeans[0].container, mbeans[0].name),
	    			animation: false,
	    			defaultSeriesType: 'area',
	    			shadow: false
	    		},
	    		title: { text: options.title },
	    		xAxis: { type: 'datetime' },
	    		yAxis: {
	    			title: { text: 'Percent' }
	    		},
	    		legend: {
	    			enabled: true,
	    			borderWidth: 0
	    		},
	    		credits: {enabled: false},
	    		exporting: { enabled: false },
	    		tooltip: {
	    			formatter: function() {
						return this.series.name +': '+ Highcharts.numberFormat(this.percentage, 1) +'% ('+ Highcharts.numberFormat(this.y / div, decimals, decPoint, thousandsSep) + typeShort + ')';
	    			}
	    		},
	    		plotOptions: {
	    			area: {
	    				stacking: 'percent',
	    				lineColor: '#ffffff',
	    				lineWidth: 1,
	    				marker: {
	    					enabled: false,
	    					states: {
	    						hover: {
	    							enabled: true,
	    							symbol: 'circle',
	    							radius: 5,
	    							lineWidth: 1
	    						}
	    					}
	    				}
	    			}
	    		},
	    		series: $.map(mbeans, function(mbean) {
	    			return {
	    				data: [],
	    				name: mbean.path || mbean.attribute
	    			}
	    		})
	    	})
	    }
	    
	    return this;
  }
  
  this.render = function() {
      $('div.jmx-tableContainer').jqotesubtpl('templates/metrics.tpl', {});
      
  		self.factory = self.JmxChartsFactory(undefined, parseInt(ocMetrics.Configuration.refresh), undefined);
  		var factory = self.factory;
  		factory.createPlotLine({
  			title: "Heap Memory Usage",
  			type: "MBytes",
  			typeShort: " MB",
  			dividing: 1048576
  			}, [{
  	    		container: 'jvm',
  	            name: 'java.lang:type=Memory',
  	            attribute: 'HeapMemoryUsage',
  	            path: 'committed'
  	        },
  	        {
  	        	container: 'jvm',
  	            name: 'java.lang:type=Memory',
  	            attribute: 'HeapMemoryUsage',
  	            path: 'used'
  	        }
  	    ]);
  	    factory.createPlotLine({
	  			title: "Threading",
	  			type: "Threads"
  			}, {
	  	    	container: 'jvm',
	  	        name:     'java.lang:type=Threading',
	  	        attribute: 'ThreadCount'
  	    });
  	    factory.createPlotLine({
  				title: "System Load Average",
  				type: "Load",
  				decimals: 3
			}, [{
	  	    	container: 'os',
	  	    	name: 'java.lang:type=OperatingSystem',
	  	    	attribute: 'SystemLoadAverage'
  	    }
  	    ]);
    	factory.createPlotLine({
      			title: "Hosts",
      			type: "Number of"
  			},[{
            	container: 'matterhorn',
            	name: 'org.opencastproject.matterhorn:type=HostsStatistics',
            	attribute: 'OnlineCount'
            },
            {
            	container: 'matterhorn',
            	name: 'org.opencastproject.matterhorn:type=HostsStatistics',
            	attribute: 'InMaintenanceCount'
            },
            {
            	container: 'matterhorn',
            	name: 'org.opencastproject.matterhorn:type=HostsStatistics',
            	attribute: 'OfflineCount'
            }
        ]);
  	    factory.createPlotLine({
	  			title: "Jobs",
	  			type: "Number of"
			},[{
	        	container: 'matterhorn',
	            name: 'org.opencastproject.matterhorn:type=JobsStatistics',
	            attribute: 'RunningJobCount'
	        },
	        {
	        	container: 'matterhorn',
	            name: 'org.opencastproject.matterhorn:type=JobsStatistics',
	            attribute: 'FinishedJobCount'
	        },
	        {
	        	container: 'matterhorn',
	        	name: 'org.opencastproject.matterhorn:type=JobsStatistics',
	        	attribute: 'FailedJobCount'
	        },
	        {
	        	container: 'matterhorn',
	        	name: 'org.opencastproject.matterhorn:type=JobsStatistics',
	        	attribute: 'QueuedJobCount'
	        }
	    ]);
	    factory.createPlotLine({
	  			title: "Services",
	  			type: "Number of"
			},[{
	        	container: 'matterhorn',
	            name: 'org.opencastproject.matterhorn:type=ServicesStatistics',
	            attribute: 'NormalServiceCount'
	        },
	        {
	        	container: 'matterhorn',
	            name: 'org.opencastproject.matterhorn:type=ServicesStatistics',
	            attribute: 'WarningServiceCount'
	        },
	        {
	        	container: 'matterhorn',
	        	name: 'org.opencastproject.matterhorn:type=ServicesStatistics',
	        	attribute: 'ErrorServiceCount'
	        }
	    ]);
	    // TODO Just use it on Admin Node which normally is the node executing this
	    factory.createPlotLine({
				title: "Workflows",
				type: "Number of"
			},[{
	        	container: 'matterhorn',
	        	name: 'org.opencastproject.matterhorn:type=WorkflowsStatistics',
	        	attribute: 'Finished'
	        },
	        {
	        	container: 'matterhorn',
	        	name: 'org.opencastproject.matterhorn:type=WorkflowsStatistics',
	        	attribute: 'OnHold'
	        },
	        {
	        	container: 'matterhorn',
	        	name: 'org.opencastproject.matterhorn:type=WorkflowsStatistics',
	        	attribute: 'Running'
	        },
	        {
	        	container: 'matterhorn',
	        	name: 'org.opencastproject.matterhorn:type=WorkflowsStatistics',
	        	attribute: 'Instantiated'
	        },
	        {
	        	container: 'matterhorn',
	        	name: 'org.opencastproject.matterhorn:type=WorkflowsStatistics',
	        	attribute: 'Stopped'
	        },
	        {
	        	container: 'matterhorn',
	        	name: 'org.opencastproject.matterhorn:type=WorkflowsStatistics',
	        	attribute: 'Failing'
	        },
	        {
	        	container: 'matterhorn',
	        	name: 'org.opencastproject.matterhorn:type=WorkflowsStatistics',
	        	attribute: 'Failed'
	        }
        ]);
  	    factory.create({
	  			title: "Workspace Storage",
	  			type: "GBytes",
	  	  		typeShort: " GB",
	  	  		dividing: 1073741824,
	  	  		decimals: 4
			},[{
  	        	container: 'matterhorn',
  	      	    name: 'org.opencastproject.matterhorn:type=Workspace',
  	      	    attribute: 'TotalSpace'
  	      	},
  	      	{
  	      		container: 'matterhorn',
  	      		name: 'org.opencastproject.matterhorn:type=Workspace',
  	      		attribute: 'FreeSpace'
  	      	},
  	      	{
  	      		container: 'matterhorn',
  	      	    name: 'org.opencastproject.matterhorn:type=Workspace',
  	      	    attribute: 'UsedSpace'
  	      	}
  	    ]);
  	    factory.create({
	  			title: "Working File Repository Storage",
	  			type: "GBytes",
	  			typeShort: " GB",
	  			dividing: 1073741824,
	  			decimals: 4
			},[{
            	container: 'matterhorn',
            	name: 'org.opencastproject.matterhorn:type=WorkingFileRepository',
            	attribute: 'TotalSpace'
            },
  	      	{
  	      		container: 'matterhorn',
  	      		name: 'org.opencastproject.matterhorn:type=WorkingFileRepository',
  	      		attribute: 'FreeSpace'
  	      	},
            {
            	container: 'matterhorn',
            	name: 'org.opencastproject.matterhorn:type=WorkingFileRepository',
            	attribute: 'UsedSpace'
            }
        ]);
  	    factory.create({
	  	    	title: "Archive Storage",
	  	    	type: "GBytes",
	  	    	typeShort: " GB",
	  	    	dividing: 1073741824,
	  	    	decimals: 4
	  	    },[{
	  	    	container: 'matterhorn',
	  	    	name: 'org.opencastproject.matterhorn:type=ElementStore',
	  	    	attribute: 'TotalSpace'
	  	    },
  	      	{
  	      		container: 'matterhorn',
  	      		name: 'org.opencastproject.matterhorn:type=ElementStore',
  	      		attribute: 'FreeSpace'
  	      	},
	  	    {
	  	    	container: 'matterhorn',
	  	    	name: 'org.opencastproject.matterhorn:type=ElementStore',
	  	    	attribute: 'UsedSpace'
	  	    }
  	    ]);
  	    factory.createPlotLine({
	  			title: "Ingests",
	  			type: "Number of"
			},[{
            	container: 'matterhorn',
            	name: 'org.opencastproject.matterhorn:type=IngestStatistics',
            	attribute: 'SuccessfulIngestOperations'
            },
            {
            	container: 'matterhorn',
            	name: 'org.opencastproject.matterhorn:type=IngestStatistics',
            	attribute: 'FailedIngestOperations'
            }
        ]);
  	    factory.createPlotLine({
	  			title: "Ingest Traffic",
	  			type: "MBytes",
	  			typeShort: " MB",
	  			dividing: 1048576,
	  			decimals: 2
  			},[{
            	container: 'matterhorn',
                name: 'org.opencastproject.matterhorn:type=IngestStatistics',
                attribute: 'BytesInLastMinute'
            },
            {
            	container: 'matterhorn',
            	name: 'org.opencastproject.matterhorn:type=IngestStatistics',
                attribute: 'BytesInLastFiveMinutes'
            },
            {
            	container: 'matterhorn',
            	name: 'org.opencastproject.matterhorn:type=IngestStatistics',
            	attribute: 'BytesInLastFifteenMinutes'
            }
        ]);
  }

  /** 
   * $(document).ready()
   */
  this.init = function() {
	  
	  $('#addHeader').jqotesubtpl('templates/metrics-header.tpl', {});
	  
	  // ocStatistics state selectors
	  $( '#metrics-' +  ocMetrics.Configuration.state).attr('checked', true);
	  $( '.state-filter-container' ).buttonset();
	  $( '.state-filter-container input' ).click(function() {
		  ocMetrics.Configuration.state = $(this).val();
		  $('div.jmx-tableContainer').hide();
		  $('div#' + ocMetrics.Configuration.state + '-tableContainer').show();
		  
		  // Fix width of charts
		  $(self.factory.getCharts()).each(function(){
			  this.resize($(this.container).parent().width(), this.containerHeight);
		  });
	  });
	  
	  self.render();
	  $('div.jmx-tableContainer').hide();
	  $('div#' + ocMetrics.Configuration.state + '-tableContainer').show();
  };
  
  return this;
})();
