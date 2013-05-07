ocStatistics = new (function() {

  var SERVERS_STATS_URL = '/services/statistics.json';           // URL of server and services statistics endpoint
  var SERVER_MAINTENANCE_URL = '/services/maintenance';           // URL of service registry endpoint maintenance method
  var SERVICE_SANITIZE_URL = '/services/sanitize';           // URL of service registry endpoint sanitize method

  var STATISTICS_DELAY = 3000;     // time interval for statistics update

  this.serversView = {};
  
  this.servicesView = {};

  // components
  var self = this; // keep ocStatistics context

  var refreshing = false;      // indicates if ajax requesting recording data is in progress
  this.refreshingStats = false; // indicates if ajax requesting statistics data is in progress
  this.refreshInterval = null;
  this.statsInterval = null;

  this.disableRefresh = function() {
    if (self.refreshInterval !== null) {
      window.clearInterval(self.refreshInterval);
    }
  }

  this.updateRefreshInterval = function(enable, delay) {
    delay = delay < 5 ? 5 : delay;
    self.Configuration.refresh = delay;
    ocUtils.log('Setting Refresh to ' + enable + " - " + delay + " sec");
    self.Configuration.doRefresh = enable;
    self.disableRefresh();
    if (enable) {
    	self.refreshInterval = window.setInterval(refresh, delay * 1000);
    }
  }
  

  /**
   * The labels for the UI.  TODO: i18n
   */
  this.labels = {
    "org_opencastproject_caption"                : "Captioning",
    "org_opencastproject_textanalyzer"           : "Text analysis",
    "org_opencastproject_videosegmenter"         : "Video segmentation",
    "org_opencastproject_composer"               : "Encoding, image extraction, and trimming",
    "org_opencastproject_distribution_acl"       : "Media distribution (Access Control Lists)",
    "org_opencastproject_distribution_download"  : "Media distribution (local downloads)",
    "org_opencastproject_distribution_streaming" : "Media distribution (local streaming)",
    "org_opencastproject_distribution_itunesu"   : "Media distribution (iTunes)",
    "org_opencastproject_publication_youtube"    : "Media publication (YouTube)",
    "org_opencastproject_gstreamer"              : "GStreamer Launch Service",
    "org_opencastproject_inspection"             : "Media inspection",
    "org_opencastproject_workflow"               : "Workflow",
    "org_opencastproject_search"                 : "Engage"
  };

  /** Executed when directly when script is loaded: parses url parameters and
   *  returns the configuration object.
   */
  this.Configuration = new (function() {

    // default configuration
    this.state = 'servers';
    this.refresh = 5;
    this.doRefresh = 'true';
    this.sortField = null;
    this.sortOrder = null;

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

  /** Initiate new ajax call to workflow instances list endpoint
   */
  function refresh() {
    if (!refreshing) {
      refreshing = true;
      ocStatistics.serversView = {};
      ocStatistics.servicesView = {};
      var url = SERVERS_STATS_URL;
      $.ajax(
      {
        url: url,
        dataType: 'json',
        success: function (data)
        {
          ocStatistics.render(data);
        }
      });
    }
  }

  /** Ajax callback for calls to the workflow instances list endpoint.
   */
  this.render = function(data) {
    refreshing = false;
    var state = ocStatistics.Configuration.state;
    ocStatistics.buildServersView(data);
    ocStatistics.buildServicesView(data);
    $("#tableContainer").jqotesubtpl("templates/statistics-table-" + state + ".tpl", ocStatistics);
    $('a.service-sanitize').click(function(event) {
    	event.stopPropagation();
    	event.preventDefault();
    	$.ajax({
    		url: SERVICE_SANITIZE_URL,
    		type: 'POST',
    		data: $(this).attr('href'),
    		success: $.proxy(function(data, textStatus, jqXHR) {
    			$(this).prev().remove();
    			$(this).remove();
		        $.ajax({
		          url: SERVERS_STATS_URL,
		          dataType: 'json',
		          success: function (data) {
		            var servicesInWarningState = 0;
		            var badge = $('#statistics_badge');
		            $.each(data.statistics.service, function(index, serviceInstance) {
		         		if (serviceInstance.serviceRegistration.service_state != 'NORMAL') {
		                  servicesInWarningState ++;
		         		}
		            });
		            if (servicesInWarningState > 0) {
		                badge.html(servicesInWarningState);
		            } else {
		          	  badge.empty();
		            }
		          }
		        });
    		}, this)
    	});
    });
    $('input.server-maintenance').click(function(event) {
    	var setToMaintenance = $(this).is(":checked");
    	var hostName = $(this).attr('name');
    	$.ajax({
    		url: SERVER_MAINTENANCE_URL,
    		type: 'POST',
    		data: 'host=' + hostName + '&maintenance=' + setToMaintenance,
    		success: $.proxy(function (data, textStatus, jqXHR) {
    			$.ajax({
    				url: '/services/services.json?&host=' + hostName,
    		        dataType: 'json',
    		        success: $.proxy(function(data) {
    		        	var image = '/admin/img/icons/available.png';
    		        	var title = 'Online';
    		        	if(setToMaintenance) {
    		        		image = '/admin/img/icons/maintenance.png';
    		        		title = 'Maintenance Mode';
    		        	}
    		        	if(data.services.service != undefined && data.services.service.length > 0 && !data.services.service[0].online) {
    		        		image = '/admin/img/icons/offline.png';
    		        		title = 'Offline';
    		        	}
    		        	
    		        	var img = $(this).prev().find('img');
    		        	img.attr('src', image);
    		        	img.attr('title', title);
    		        }, this)
    			});
    		}, this)
    	});
    });
  }

  /** Make the page reload with the currently set configuration
   */
  this.reload = function() {
    var url = document.location.href.split('?', 2)[0];
    var pa = [];
    for (p in this.Configuration) {
      if (this.Configuration[p] != null) {
        pa.push(p + '=' + escape(this.Configuration[p]));
      }
    }
    url += '?' + pa.join('&');
    document.location.href = url;
  }
  
  /**
   * Builds the "server view" js model.
   */
  this.buildServersView = function(data) {
    $.each(data.statistics.service, function(index, serviceInstance) {
      var reg = serviceInstance.serviceRegistration;
      if(!reg.active)
        return true;
      var server = ocStatistics.serversView[reg.host];
      if(server == null) {
        server = {
          'host' : reg.host, 
          'online' : reg.online, 
          'maintenance' : reg.maintenance
        };
        ocStatistics.serversView[reg.host] = server;
        server.runningTotal = 0;
        server.queuedTotal = 0;
        server.meanRunTimeTotal = 0;
        server.meanQueueTimeTotal = 0;
        server.services = [];
      }
      // if the service is not a job producer, we don't show it here
      if(!reg.jobproducer) return true;

      server.runningTotal += parseInt(serviceInstance.running);
      server.meanRunTimeTotal += parseInt(serviceInstance.meanruntime);
      server.queuedTotal += parseInt(serviceInstance.queued);
      server.meanQueueTimeTotal += parseInt(serviceInstance.meanqueuetime);

      // Add the service type to this server
      var singleService = {};
      server.services.push(singleService);
      var serviceTypeIdentifier = reg.type.replace(/\./g, "_");
      singleService.id = serviceTypeIdentifier;
      singleService.path = reg.path;
      singleService.running = serviceInstance.running;
      var duration = ocUtils.getDuration(serviceInstance.meanruntime);
      singleService.meanRunTime = duration.substring(duration.indexOf(':')+1);
      singleService.queued = serviceInstance.queued;
      duration = ocUtils.getDuration(serviceInstance.meanqueuetime);
      singleService.meanQueueTime = duration.substring(duration.indexOf(':')+1);
      singleService.online = reg.online;
      singleService.maintenance = reg.maintenance;
      singleService.state = reg.service_state;
      singleService.type = reg.type;
    });

    $.each(ocStatistics.serversView,function(s,server){
      server.runningTotal = ocUtils.formatInt(server.runningTotal);
      server.queuedTotal = ocUtils.formatInt(server.queuedTotal);
      var duration = ocUtils.getDuration(server.meanRunTimeTotal);
      server.meanRunTimeTotal = duration.substring(duration.indexOf(':')+1);
      duration = ocUtils.getDuration(server.meanQueueTimeTotal);
      server.meanQueueTimeTotal = duration.substring(duration.indexOf(':')+1);
    });
  }
  
  /**
   * Builds the "services view" js model.
   */
  this.buildServicesView = function(data) {
    $.each(data.statistics.service, function(index, serviceInstance) {
      var reg = serviceInstance.serviceRegistration;
      if(!reg.active) return true;
  
      // if the service is not a job producer, we don't show it here
      if(!reg.jobproducer) return true;
  
      var serviceTypeIdentifier = reg.type.replace(/\./g, "_");
      var service = ocStatistics.servicesView[serviceTypeIdentifier];
      if(service == null) {
        service = {
          'id' : serviceTypeIdentifier
        }; //, 'online' : reg.online, 'maintenance' : reg.maintenance};
        service.servers = [];
        service.meanRunTimeTotal = 0;
        service.meanQueueTimeTotal = 0;
        service.runningTotal = 0;
        service.queuedTotal = 0;
        ocStatistics.servicesView[serviceTypeIdentifier] = service;
      }      

      // Add the server to this service
      var singleServer = {};
      service.servers.push(singleServer);
      singleServer.host = reg.host;
      singleServer.type = reg.type;

      service.meanRunTimeTotal += parseInt(serviceInstance.meanruntime);
      service.meanQueueTimeTotal += parseInt(serviceInstance.meanqueuetime);
      service.runningTotal += parseInt(serviceInstance.running);
      service.queuedTotal += parseInt(serviceInstance.queued);

      singleServer.online = reg.online;
      singleServer.state = reg.service_state;
      singleServer.maintenance = reg.maintenance;
      singleServer.running = ocUtils.formatInt(serviceInstance.running);
      singleServer.queued = ocUtils.formatInt(serviceInstance.queued);

      var duration = ocUtils.getDuration(serviceInstance.meanruntime);
      singleServer.meanRunTime = duration.substring(duration.indexOf(':')+1);      
      duration = ocUtils.getDuration(serviceInstance.meanqueuetime);
      singleServer.meanQueueTime = duration.substring(duration.indexOf(':')+1);
    });		

    $.each(ocStatistics.servicesView,function(i,service){		
      service.runningTotal = ocUtils.formatInt(service.runningTotal);
      service.queuedTotal = ocUtils.formatInt(service.queuedTotal);
      var duration = ocUtils.getDuration(service.meanRunTimeTotal);
      service.meanRunTimeTotal = duration.substring(duration.indexOf(':')+1);
      duration = ocUtils.getDuration(service.meanQueueTimeTotal);
      service.meanQueueTimeTotal = duration.substring(duration.indexOf(':')+1);
    });
  }

  /** $(document).ready()
   *
   */
  this.init = function() {
    
    $('#addHeader').jqotesubtpl('templates/statistics-header.tpl', {});

    // ocStatistics state selectors
    $( '#stats-' +  ocStatistics.Configuration.state).attr('checked', true);
    $( '.state-filter-container' ).buttonset();
    $( '.state-filter-container input' ).click( function() {
      ocStatistics.Configuration.state = $(this).val();
      ocStatistics.reload();
    })
    
    //set refresh
	ocStatistics.updateRefreshInterval(ocStatistics.Configuration.doRefresh, ocStatistics.Configuration.refresh);
	
	// Refresh Controls
	// set values according to config
	if (ocStatistics.Configuration.doRefresh == 'true') {		
	  $('#refreshEnabled').attr('checked', 'checked');
	  $('#refreshInterval').removeAttr('disabled');
	  $('#refreshControlsContainer span').removeAttr('style');
	} else {
	  $('#refreshEnabled').removeAttr('checked');
	  $('#refreshInterval').attr('disabled', 'true');
	  $('#refreshControlsContainer span').css('color', 'silver');
	}

	$('#refreshInterval').val(ocStatistics.Configuration.refresh);
	  
	// attatch event handlers
	$('#refreshEnabled').change(function() {
	  if ($(this).is(':checked')) {
	    $('#refreshInterval').removeAttr('disabled');
	    $('#refreshControlsContainer span').removeAttr('style');
	  } else {
	    $('#refreshInterval').attr('disabled', 'true');
	    $('#refreshControlsContainer span').css('color', 'silver');
	  }
	  ocStatistics.updateRefreshInterval($(this).is(':checked'), $('#refreshInterval').val());
	});

	$('#refreshInterval').change(function() {
	  ocStatistics.updateRefreshInterval($('#refreshEnabled').is(':checked'), $(this).val());
	});

    refresh();    // load and render data for currently set configuration

  };
  
  return this;
})();
