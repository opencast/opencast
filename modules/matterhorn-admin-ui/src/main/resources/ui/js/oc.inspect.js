var Opencast = Opencast || {};

Opencast.WorkflowInspect = (function() {

  this.WORKFLOW_INSTANCE_URL = '../workflow/instance/';
  this.SCHEDULER_URL = '../recordings/';

  var $container;       // id of the target container
  var templateId;
  var instanceView;     // view of the workflow instance data
  var targetView;       // indicates if technical details or info page should be rendered ('details' | 'info')
  var workflow;
  
  this.initialize = function()
  {
    $('#addHeader').jqotesubtpl('templates/viewinfo-header.tpl', {});
    var id = ocUtils.getURLParam('id');
    Opencast.WorkflowInspect.renderInfo(id, 'infoContainer', 'info');
  }
  
  this.getWorklfow = function()
  {
    return workflow;
  }
  
  this.initInspect = function()
  {
    
    $('#addHeader').jqotesubtpl('templates/inspect-header.tpl', {});
    var id = ocUtils.getURLParam('id');
    Opencast.WorkflowInspect.renderDetails(id, 'inspectContainer', 'inspect');
  }

  this.renderInfo = function(id, container, template) {
    targetView = 'info';
    templateId = template;
    $container = $('#' + container);
    requestWorkflow(id);
  }

  this.renderDetails = function(id, container, template) {
    targetView = 'details';
    templateId = template;
    $container = $('#' + container);
    requestWorkflow(id);
  }

  function requestWorkflow(id) {
    $.ajax({
      url : this.WORKFLOW_INSTANCE_URL + id + ".json",
      dataType: 'json',
      success: Opencast.WorkflowInspect.rx
    });
  }

  /** Ajax recieve function
   *
   */
  this.rx = function(data) {
    instanceView = buildInstanceView(data.workflow);
    workflow = data.workflow;
    if (targetView == 'details') {
      renderDetailsView(instanceView, $container);
    } else if (targetView == 'info') {
      renderInfoView(instanceView, $container);
    }
  }

  /** Build view of workflow instance data
   *
   */
  function buildInstanceView(workflow) {
    var out = Opencast.RenderUtils.extractScalars(workflow);
    out.config = buildConfigObject(workflow.configurations.configuration);

    // Operations
    var ops = Opencast.RenderUtils.ensureArray(workflow.operations.operation);
    $.each(ops, function(index, op) {
      // replace time in milli to date strings
      $.each(op, function(ind, opItem) { 
    	  if ( typeof(opItem) == "number" && opItem > 1000000000000) {
    		  op[ind] = ocUtils.makeLocaleDateString(opItem);  } 
        });
    	
      if (op.configurations !== undefined && op.configurations.configuration !== undefined) {
        op.configurations = buildConfigObject(op.configurations.configuration);
      } else {
        op.configurations = [];
      }
    });
    out.operations = ops;

    if (workflow.mediapackage) {
      var mp = workflow.mediapackage;

      // prepare info object for View Info
      out.info = {};
      out.info.title = mp.title;
      out.info.seriestitle = false;
      out.info.creators = '';
      out.info.start = '';
      out.info.episodeDC = false;
      out.info.seriesDC = false;

      if (mp.creators) {
        out.info.creators = Opencast.RenderUtils.ensureArray(mp.creators.creator).join(', ');
      }
      if (mp.seriestitle) {
        out.info.seriestitle = mp.seriestitle;
      }
      if (mp.start) {
        out.info.start = mp.start;
      }

      // Attachments
      mp.attachments = Opencast.RenderUtils.ensureArray(mp.attachments.attachment);

      // Tracks
      if (!mp.media) {
        mp.media = {};
      }
      mp.media.track = Opencast.RenderUtils.ensureArray(mp.media.track);

      // search for URL of episode / series dublincore catalogs, prefere catalogs from same domain so we can load them via ajax
      var thisHost = window.location.protocol + '//' + window.location.host;
      if (mp.metadata) {
        mp.metadata.catalog = Opencast.RenderUtils.ensureArray(mp.metadata.catalog);
        $.each(mp.metadata.catalog, function(index, catalog) {
          if (catalog.type == 'dublincore/episode') {
            if (out.info.episodeDC == false || out.info.episodeDC.sameHost == false) {
              out.info.episodeDC = {
                url : catalog.url,
                sameHost : (catalog.url.indexOf(thisHost) === 0)
              }
            }
          } else if (catalog.type == 'dublincore/series') {
            if (out.info.seriesDC == false || out.info.seriesDC.sameHost == false) {
              out.info.seriesDC = {
                url : catalog.url,
                sameHost : (catalog.url.indexOf(thisHost) === 0)
              }
            }
          }
        });
      } else {
        mp.metadata = {};
        mp.metadata.catalog = [];
      }

      // 'flatten' encoder and scantype properties
      try {
        $.each(mp.media.track, function(index, track) {
          if (track.audio && track.audio.encoder) {
            track.audio.encoder = track.audio.encoder.type;
          }
          if (track.video) {
            if (track.video.encoder) track.video.encoder = track.video.encoder.type;
            if (track.video.scantype) track.video.scantype = track.video.scantype.type;
          }
        });
      } catch (e) {
        ocUtils.log('Could not flatten encoder/scantype properties of tracks');
      }
      out.mediapackage = mp;
    } else {
      out.info = {};
      out.mediapackage = false;
    }

    // in case of an 'upcoming event' episode dublin core catalog is obtained from scheduler service
    if (workflow.template == 'scheduling') {
      out.info.episodeDC = {};
      out.info.episodeDC.url = this.SCHEDULER_URL + workflow.id + ".xml";//+"/dublincore";
      out.info.episodeDC.sameHost = true;
    }

    return {
      workflow : out
    };
  }

  /** Render workflow view to specified container
   *
   */
  function renderDetailsView(workflow, $target) {
    //    var result = TrimPath.processDOMTemplate(templateId, workflow);
    //    $target.append(result);
    $target.jqoteapptpl("templates/viewinfo-" + templateId + ".tpl", workflow);
    $target.tabs({
//      select: function (event, ui) {
//        if(ui.index == 3 && window.location.hash != '#performance')
//        {
//          window.location.hash = '#performance';
//          window.location.reload();
//        }
//      }
    });
    $('.unfoldable-tr').click(function() {
      var $content = $(this).find('.unfoldable-content');
      var unfolded = $content.is(':visible');
      $('.unfoldable-content').hide('fast');
      if (!unfolded) {
        $content.show('fast');
      }
    });
    renderWorkflowPerformance(workflow);
  }

  /** Render workflow info page (View Info) to specified container
   *
   */
  function renderInfoView(workflow, $target) {
    //var result = TrimPath.processDOMTemplate(templateId, workflow);
    //$target.append(result);
    Opencast.WorkflowInspect.workflow = workflow;
    $target.jqoteapptpl("templates/viewinfo-" + templateId + ".tpl", workflow);

    if (!$.isEmptyObject(workflow.workflow.mediapackage.media.track)){
      $.each(workflow.workflow.mediapackage.media.track,function(i,track){
        if (track.type==="presenter/source"){ 
          var fname = track.url.substring(track.url.lastIndexOf("/")+1,track.url.length);
          $('#uploadedFileContainer td.td-value').html(fname); 
        }	   
      });
    }

    // Render Episode DC if present
    if (workflow.workflow.info.episodeDC !== false) {
      if (workflow.workflow.info.episodeDC.sameHost == true) {
        $.ajax({
          url : workflow.workflow.info.episodeDC.url,
          type : 'GET',
          dataType : 'xml',
          error : function() {
            $('#episodeContainer').text('Error: Could not retrieve Episode Dublin Core Catalog');
          },
          success : function(data) {
            data = Opencast.RenderUtils.DCXMLtoObj(data);
            //            var episode = TrimPath.processDOMTemplate('episode', data);
            //            $('#episodeContainer').append(episode);
            $('#episodeContainer').jqoteapptpl("templates/viewinfo-episode.tpl", data);
            if (data.dc.license) {
              $('#licenseField').text(data.dc.license);
            }
          }
        });
      } else {	
        $('#episodeContainer').jqoteapptpl("templates/viewinfo-catalog.tpl", workflow.workflow.info.episodeDC);
      }
    }

    // Render Series DC if present
    if (workflow.workflow.info.seriesDC !== false) {
      if (workflow.workflow.info.seriesDC.sameHost == true) {
        $.ajax({
          url : workflow.workflow.info.seriesDC.url,
          type : 'GET',
          dataType : 'xml',
          error : function() {
            $('#episodeContainer').text('Error: Could not retrieve Episode Dublin Core Catalog');
          },
          success : function(data) {
            $('#seriesContainer').jqoteapptpl("templates/viewinfo-series.tpl", Opencast.RenderUtils.DCXMLtoObj(data));
          }
        });
      } else {
        $('#seriesContainer').jqoteapptpl("templates/viewinfo-catalog.tpl", workflow.workflow.info.seriesDC);
      }
    }

    // care for unfoldable boxes
    if (workflow.workflow.info.episodeDC || workflow.workflow.info.seriesDC) {
      $('.unfoldable-header').click(function() {
        var $content = $(this).next('.unfoldable-content');
        if ($content.is(':visible')) {
          $content.hide('fast');
          $(this).find('.fold-icon').removeClass('ui-icon-triangle-1-s').addClass('ui-icon-triangle-1-e');
        } else {
          $content.show('fast');
          $(this).find('.fold-icon').removeClass('ui-icon-triangle-1-e').addClass('ui-icon-triangle-1-s');
        }
      });
    }
  }

  /** render workflow performance chart
   */
  function renderWorkflowPerformance(data) {
    // Make a graph object with canvas id and width
    var g = new Bluff.SideStackedBar('graph', '600x300');

    // Set theme and options
    //g.theme_greyscale();
    g.title = 'Processing times for ' + data.workflow.mediapackage.title;
    g.x_axis_label = 'Seconds';

    // Add data and labels
    var queue = [];
    var run = [];
    var labels = {};
    jQuery.each(data.workflow.operations, function(index, operationInstance) {
      var op = data.workflow.operations[index];
      if(op.state == 'SUCCEEDED') {
        var runtime = (op.completed - op.started) / 1000;
        if(runtime < 1) {
          return;
        }
        run.push(runtime);
        queue.push(op['time-in-queue'] / 1000);
        labels['' + run.length-1] = op.id;
      }
    });

    g.data('Queue', queue);
    g.data('Run', run);
    g.labels = labels;

    // Render the graph
    g.draw();
  }

  /** Build an object that can be rendered easily from the Configuration objects
   *  of Workflow, Operation etc. If the same key is encountered twice or more
   *  the field is converted to an array.
   */
  function buildConfigObject(data) {
    var out = {};
    data = Opencast.RenderUtils.ensureArray(data);
    $.each(data, function(index, member) {
      if ($.isArray(out[member.key])) {
        out[member.key].push(member.value);
      } else if (out[member.key] !== undefined) {
        out[member.key] = [out[member.key], member.value];
      } else {
    	val = member['$'];
    	
    	if ( (val.length == 13 && parseInt(val) != NaN) || (typeof(val) == "number" && val > 1000000000000)) {
    		  out[member.key] = ocUtils.makeLocaleDateString(val);  } 
    	else {
    		out[member.key] = val;
    	}
      }
    });
    return out;
  }

  return this;
}());

Opencast.RenderUtils = (function() {

  /** Returns
   *    [obj] if an object is passed
   *    obj if obj is already an Array
   *    [] if obj === undefined or something eles goes wrong
   */
  this.ensureArray = function(obj) {
    try {
      if (obj === undefined) {
        return [];
      } else if ($.isArray(obj)) {
        return obj;
      } else {
        return [obj];
      }
    } catch (e) {
      return [];
    }
  }

  /** Returns either the value of obj if obj is a scalar or a ',' separated list
   *  of the scalar values of obj if obj is an Array
   */
  this.ensureString = function(obj) {
    if ($.isArray(obj)) {
      return obj.join(', ');
    } else {
      return '' + obj;
    }
  }

  /** Returns an object containing all scalar members of obj.
   *
   */
  this.extractScalars = function(obj) {
    var out = {};
    for (var key in obj) {
      var value = obj[key];
      if (typeof value == 'string' || typeof value == 'number') {
        out[key] = obj[key];
      }
    }
    return out;
  }

  /** Convert a DublinCore XML document to a javascript object
   *
   */
  this.DCXMLtoObj = function(data) {
    var out = {};
    $(data).find('dublincore').children().each(function() {
      var tagname = $(this).context.tagName.split(':')[1];
      out[tagname] = $(this).text();
    });
    return {
      dc:out
    };
  }

  return {
    DCXMLtoObj: DCXMLtoObj,
    extractScalars: extractScalars,
    ensureString: ensureString,
    ensureArray: ensureArray
  };
}());
