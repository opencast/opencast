var ocUpload = (function() {

  this.DEFAULT_WORKFLOW_DEFINITION = 'full';
  this.WORKFLOW_DEFINITION_URL = '/workflow/definitions.json';
  this.WORKFLOW_PANEL_URL = '/workflow/configurationPanel?definitionId=';
  this.SERIES_SEARCH_URL = '/series/series.json';
  this.SERIES_URL = '/series'
  this.INGEST_CREATE_MP_URL = '/ingest/createMediaPackage';
  this.INGEST_ADD_CATALOG_URL = '/ingest/addDCCatalog';
  this.INGEST_PROGRESS_URL = '/ingest/getProgress';
  this.INGEST_START_URL = '/ingest/ingest';
  this.UPLOAD_PROGRESS_INTERVAL = 2000;
  this.KILOBYTE = 1024;
  this.MEGABYTE = 1024 * 1024;
  this.ANOYMOUS_URL = "/info/me.json";

  /** $(document).ready()
   *
   */
  this.init = function() {
    ocUtils.log('Initializing UI');
    $('#addHeader').jqotesubtpl('templates/upload.tpl', {});
    $('.unfoldable-header').click(ocUpload.UI.toggleUnfoldable);
    $('.dc-metadata-field').change(ocUpload.UI.formFieldChanged);
    $('.uploadtype-select').click(ocUpload.UI.selectUploadType);
    $('.file-source-select').click(ocUpload.UI.selectFileSource);
    $('.flavor-presentation-checkbox').change(ocUpload.UI.selectFlavor);
    $('#workflowSelector').change(ocUpload.UI.selectWorkflowDefinition);
    $('#submitButton').button().click(startUpload);
    $('#cancelButton').button().click(backToRecordings);

    var initializerDate;
    initializerDate = new Date();

    $('#startTimeHour').val(initializerDate.getHours());
    $('#startTimeMin').val(initializerDate.getMinutes());

    $('#recordDate').datepicker({
      showOn: 'both',
      buttonImage: 'img/icons/calendar.gif',
      buttonImageOnly: true,
      dateFormat: 'yy-mm-dd'
    });
    $('#recordDate').datepicker('setDate', initializerDate);

    ocUpload.UI.loadWorkflowDefinitions();
    initSeriesAutocomplete();
  }

  function initSeriesAutocomplete() {
    ocUtils.log('Initializing autocomplete for series field')
    $('#series').autocomplete({
      source: function(request, response) {
        $.ajax({
          url: ocUpload.SERIES_SEARCH_URL + '?q=' + request.term + '&edit=true',
          dataType: 'json',
          type: 'GET',
          success: function(data) {
            data = data.catalogs;
            var series_list = [];
            $.each(data, function(){
              series_list.push({value: this['http://purl.org/dc/terms/']['title'][0].value,
                                id: this['http://purl.org/dc/terms/']['identifier'][0].value});
            });
            series_list.sort(function stringComparison(a, b)	{
              a = a.value;
              a = a.toLowerCase();
              a = a.replace(/ä/g,"a");
              a = a.replace(/ö/g,"o");
              a = a.replace(/ü/g,"u");
              a = a.replace(/ß/g,"s");

              b = b.value;
              b = b.toLowerCase();
              b = b.replace(/ä/g,"a");
              b = b.replace(/ö/g,"o");
              b = b.replace(/ü/g,"u");
              b = b.replace(/ß/g,"s");

              return(a==b)?0:(a>b)?1:-1;
            });
            response(series_list);
          },
          error: function() {
            ocUtils.log('could not retrieve series_data');
          }
        });
      },
      select: function(event, ui){
        $('#ispartof').val(ui.item.id);
      },
      change: function(event, ui){
          if($('#ispartof').val() === '' && $('#series').val() !== ''){
              ocUtils.log("Searching for series in series endpoint");
              $.ajax({
                  url : ocUpload.SERIES_SEARCH_URL + '?seriesTitle=' + $('#series').val(),
                  type : 'get',
                  dataType : 'json',
                  success : function(data) {
                      var DUBLIN_CORE_NS_URI  = 'http://purl.org/dc/terms/',
                          series_input = $('#series').val(),
                          series_list = data["catalogs"],
                          series_title,
                          series_id;

                          if(series_list.length !== 0){
                              series_title = series_list[0][DUBLIN_CORE_NS_URI]["title"] ? series_list[0][DUBLIN_CORE_NS_URI]["title"][0].value : "";
                              series_id = series_list[0][DUBLIN_CORE_NS_URI]["identifier"] ? series_list[0][DUBLIN_CORE_NS_URI]["identifier"][0].value : "";
                              $('#ispartof').val(series_id);
                          }
                  }
              });
          }
      },
      search: function(){
        $('#ispartof').val('');
      }
    });
  }

  this.checkRequiredFields = function() {
    ocUtils.log('Checking for missing inputs');
    var missing = [];

    if ($.trim($('#titleField').val()) == '') {
      ocUtils.log('Missing input: title');
      missing.push('title');
    }

    if ($.trim($('#recordDate').val()) == '') {
      ocUtils.log('Missing input: recordDate');
      missing.push('recordDate');
    }

    var fileSelected = false;
    $('.uploadForm-container:visible').each(function() {
      var file = $(this).contents().find('.file-selector').val();
      if (file !== undefined && file !== '') {
        fileSelected |= true;
      }
    });
    if (!fileSelected) {
      ocUtils.log('Missing input: no file selected');
      missing.push('track');
    }

    return missing.length > 0 ? missing : false;
  }

  function startUpload() {
    var missingFields = ocUpload.checkRequiredFields();
    ocUpload.UI.collectFormData();

    if (missingFields === false) {
      ocUpload.Ingest.begin();
    } else {
      ocUpload.UI.showMissingFieldsNotification();
      updateMissingFieldNotification(missingFields);
    }
  }

  this.backToRecordings = function() {
    location.href = "/admin/index.html#/recordings?" + window.location.hash.split('?')[1];
  }

  return this;
})();


/** @namespace UI functions
 *
 */
ocUpload.UI = (function() {

  /**
   * collected form data
   */
  var metadata = new Array();

  this.showMissingFieldsNotification = function() {
    $('#missingFieldsContainer').show();
  }

  this.updateMissingFieldNotification = function(missingFields) {
    ocUtils.log('Updating missing fields notification');
    if (missingFields == false) {
      $('#missingFieldsContainer').hide();
    } else {
      $('#missingFieldsContainer').find('.missing-fields-item').each(function() {
        var fieldname = $(this).attr('id').substr(5);
        if ($.inArray(fieldname, missingFields) != -1) {
          $(this).show();
        } else {
          $(this).hide();
        }
      });
    }
  }

  this.loadWorkflowDefinitions = function() {
    ocUtils.log('Loading workflow definitions');
    $.ajax({
      method: 'GET',
      url: ocUpload.WORKFLOW_DEFINITION_URL,
      dataType: 'json',
      success: function(data) {
        var defs = [];
        for (i in data.workflow_definitions) {
          var $selector = $('#workflowSelector');
          var workflow = data.workflow_definitions[i];
          if ( workflow.id != 'error' ) {
            defs.push(workflow.id);
            var $newOption = $('<option></option>')
            .attr('value', workflow.id)
            .text(workflow.title);
            if (workflow.id == ocUpload.DEFAULT_WORKFLOW_DEFINITION) {
              $newOption.attr('selected', 'true');
            }
            $selector.append($newOption);
          }
        }
        ocUtils.log('Loaded workflow definitions: ' + defs.join(', '));
      }
    });
    $('.workflowConfigContainer').load(ocUpload.WORKFLOW_PANEL_URL + ocUpload.DEFAULT_WORKFLOW_DEFINITION);
  }

  this.toggleUnfoldable = function() {
    $(this).next('.unfoldable-content').toggle();
    $(this).find('.unfoldable-icon')
    .toggleClass('ui-icon-triangle-1-e')
    .toggleClass('ui-icon-triangle-1-s');
  }

  this.formFieldChanged = function() {
    ocUpload.UI.updateMissingFieldNotification(ocUpload.checkRequiredFields());
  }

  this.selectUploadType = function() {
    var $toHide = [];
    var $toShow = [];
    if ($(this).hasClass('uploadType-single')) {
      $toShow = $('#uploadContainerSingle');
      $toHide = $('#uploadContainerMulti');
    } else if ($(this).hasClass('uploadType-multi')) {
      $toShow = $('#uploadContainerMulti');
      $toHide = $('#uploadContainerSingle');
    }
    $toHide.hide();
    $toShow.show();
  }

  this.selectFileSource = function() {
    var location = $(this).val();
    var $container = $(this).parent().next('li').find('iframe');
    $container.attr('src', '../ingest/filechooser-' + location + '.html');
  }

  this.selectFlavor = function() {
    var $flavorField = $(this).parent().find('.track-flavor');
    if ($(this).is(':checked')) {
      $flavorField.val('presentation/source');
    } else {
      $flavorField.val('presenter/source');
    }
  }

  this.selectWorkflowDefinition = function() {
    var defId = $(this).val();
    var $container = $(this).parent().next('.workflowConfigContainer');
    $container.load(ocUpload.WORKFLOW_PANEL_URL + defId);
  }

  this.showProgressDialog = function() {
    $('#grayOut').css('display','block');
    $('#progressStage').dialog(
    {
      modal: true,
      width: 450,
      height: 'auto',
      position: ['center', 'center'],
      title: 'Uploading File',
      create: function (event, ui)
      {
        $('.ui-dialog-titlebar-close').hide();
      },
      resizable: false,
      draggable: false,
      disabled: true
    });
  }

  this.hideProgressDialog = function() {
    $('#progressStage').dialog( "destroy" );
    $('#grayOut').css('display','block');
  }

  this.setProgress = function(message) {
    var $progress = $('#progressStage');

    if (message.filename !== undefined) {         // status message or upload progress?
      var percentage = ((message.received / message.total) * 100).toFixed(1) + '%';
      var total = (message.total / ocUpload.MEGABYTE).toFixed(2) + ' MB';
      var received = (message.received / ocUpload.MEGABYTE).toFixed(2) + ' MB';

      $progress.find('.progress-label-top').text('Uploading ' + message.filename.replace("C:\\fakepath\\", ""));
      $progress.find('.progressbar-indicator').css('width', percentage);
      $progress.find('.progressbar-label > span').text(percentage);
      $progress.find('.progress-label-left').text(received + ' received');
      $progress.find('.progress-label-right').text(total + ' total');
    } else {
      $progress.find('.upload-label').text(' ');
      $progress.find('.progressbar-indicator').css('width', '0%');
      $progress.find('.progressbar-label > span').text(message);
    }
  }

  this.showSuccess = function() {
    ocUpload.UI.hideProgressDialog();
    ocUpload.UI.showSuccesScreen();
    //ocUpload.backToRecordings();
    //window.location = '/admin';
  }

  this.showFailure = function(message) {
    ocUpload.UI.hideProgressDialog();
    alert("Ingest failed:\n" + message);
    //ocUpload.backToRecordings();
    window.location = '/admin/index.html#/recordings?' + window.location.hash.split('?')[1];;
  }

  /**
   * collects metadata to show in sucess screen
   *
   * @return array metadata
   */
  this.collectFormData = function() {
      ocUtils.log("Collecting metadata");

      var metadata = new Array;
      metadata['files'] = new Array();

      $('.oc-ui-form-field').each( function() { //collect text input
          metadata[$(this).attr('name')] = $(this).val();
      });
      $('.uploadForm-container:visible').each(function() { //collect file names
          var file = $(this).contents().find('.file-selector').val();
          if(file != undefined) {
              metadata['files'].push(file);
          }

      });
      this.metadata = metadata;
  }

  /**
   * loads success screen template and fills with data
   */
  this.showSuccesScreen = function() {
      var data = this.metadata;
      $('#stage').load('complete.html', function() {
        for (var key in data) {
          if (data[key] != "" && key != 'files') { //print text, not file names
            $('#field-'+key).css('display','block');
            if (data[key] instanceof Array) {
              $('#field-'+key).children('.fieldValue').text(data[key].join(', '));
            } else {
              $('#field-'+key).children('.fieldValue').text(data[key]);
            }
          }
        }
        $('.field-filename').each(function() { //print file names
            var file = data['files'].shift();
            if(file) {
                $(this).children('.fieldValue').text(file.replace("C:\\fakepath\\", ""));
            } else {
                $(this).hide();
            }
        });
        //When should it show this heading?
        //$('#heading-metadata').text('Your recording with the following information has been resubmitted');
      });
  }



  return this;
})();

/** @namespace Ingest logic
 *
 */
ocUpload.Ingest = (function() {

  var ELEMENT_TYPE = {
    CATALOG : 1,
    TRACK : 2
  };

  var MediaPackage = {
    document : '',
    elements : []
  };

  var Workflow = {
    definition : false,
    properties : {}
  };

  /** Constructor for MediaPackageElement
   */
  function MediaPackageElement(id, type, flavor, payload) {
    this.id = id;
    this.type = type;
    this.flavor = flavor;
    this.payload = payload;
    this.done = false;
  }

  this.begin = function() {
    ocUpload.UI.showProgressDialog();
    ocUpload.UI.setProgress("Constructing Media Package...");

    // enqueue Episode Dublin Core
    MediaPackage.elements.push(
      new MediaPackageElement('episodeDC', ELEMENT_TYPE.CATALOG, 'dublincore/episode', createDublinCoreDocument()));
    ocUtils.log("Added Dublin Core catalog for episode");

    // enqueue Series Dublin Core
    var series = $('#series').val();
    //var seriesId = $('#ispartof').val();
    if (series !== '') {
      var seriesId = $('#ispartof').val();
      if (seriesId === '') {
        seriesId = createSeries(series);
      }
      MediaPackage.elements.push(
        new MediaPackageElement('seriesDC', ELEMENT_TYPE.CATALOG, 'dublincore/series', getSeriesCatalog(seriesId)));
      ocUtils.log("Added Dublin Core catalog for series");
    }

    // enqueue Tracks
    $('.upload-widget:visible').each(function() {
      var $uploader = $(this).find('.uploadForm-container');
      if ($uploader.contents().find('.file-selector').val() != '') {
        var id = $uploader.contents().find('.track-id').val();
        var flavor = $(this).find('.track-flavor').val();
        MediaPackage.elements.push(new MediaPackageElement(id, ELEMENT_TYPE.TRACK, flavor, $uploader));
        ocUtils.log('Added Track (' + flavor + ')');
      }
    });

    // get workflow configuration
    Workflow.definition = $('#workflowSelector').val();
    Workflow.properties = ocUpload.Ingest.getWorkflowConfiguration($('#workflowConfigContainer'));

    createMediaPackage();   // begin by creating the initial MediaPackage
  }

  function proceed() {
    ocUtils.log('Proceeding with ingest');
    var nextElement = false;

    // search for element to be submitted
    $(MediaPackage.elements).each(function(index, element){
      if (nextElement === false && element.done === false) {
        nextElement = element;
      }
    });

    // submit next MediaPackageElement
    if (nextElement !== false) {
      switch(nextElement.type) {
        case ELEMENT_TYPE.CATALOG:
          addCatalog(nextElement);
          break;
        case ELEMENT_TYPE.TRACK:
          addTrack(nextElement);
          break;
        default:
          break;
      }
    } else {          // all elements added
      ocUtils.log('No more elements to add');
      startIngest();  // start Ingest
    }
  }

  function startIngest() {
    ocUtils.log('Starting Ingest');
    ocUpload.UI.setProgress('Starting Processing...');

    var workflowData = Workflow.properties;
    workflowData['mediaPackage'] = MediaPackage.document;
    $.ajax({
      url : ocUpload.INGEST_START_URL + '/' + Workflow.definition,
      async : true,
      type : 'post',
      data : workflowData,
      error : function() {
        ocUpload.Listener.ingestError('Failed to start Processing');
      },
      success : function() {
        ocUpload.UI.showSuccess();
      }
    });
  }

  this.discardIngest = function() {
    if (MediaPackage.document !== null) {
      // TODO call discardMediaPackage method
    }
  }

  function createMediaPackage() {
    ocUpload.UI.setProgress("Creating Media Package on Server...");
    $.ajax({
      url        : INGEST_CREATE_MP_URL,
      type       : 'GET',
      dataType   : 'xml',                     // TODO try to take the response directly as string
      error      : function(XHR,status,e){
        ocUpload.Listener.ingestError('Could not create MediaPackage');
      },
      success    : function(data, status) {
        MediaPackage.document = ocUtils.xmlToString(data);
        proceed();
      }
    });
  }

  function createDublinCoreDocument() {
    dcDoc = ocUtils.createDoc('dublincore', 'http://www.opencastproject.org/xsd/1.0/dublincore/');
    $(dcDoc.documentElement).attr('xmlns:dcterms', 'http://purl.org/dc/terms/');
    $('.dc-metadata-field').each(function() {
      $field = $(this);
      var $newElm = $(dcDoc.createElement('dcterms:' + $field.attr('name')));
      $newElm.text($field.val());
      $(dcDoc.documentElement).append($newElm);
    });
    var $created = $(dcDoc.createElement('dcterms:created'))
    var date = $('#recordDate').datepicker('getDate').getTime();
    date += $('#startTimeHour').val() * 60 * 60 * 1000;
    date += $('#startTimeMin').val() * 60 * 1000;
    $created.text(ocUtils.toISODate(new Date(date)));
    $(dcDoc.documentElement).append($created);
    var out = ocUtils.xmlToString(dcDoc);
    return out;
  }

  function addCatalog(catalog) {
    ocUtils.log('Uploading Dublin Core Catalog (' + catalog.flavor + ')');
    ocUpload.UI.setProgress("Uploading Catalog (" + catalog.flavor + ")...");
    $.ajax({
      url        : INGEST_ADD_CATALOG_URL,
      type       : 'POST',
      dataType   : 'xml',
      data       : {
        flavor       : catalog.flavor,
        mediaPackage : MediaPackage.document,
        dublinCore   : catalog.payload
      },
      error      : function(XHR,status,e){
        ocUpload.Listener.ingestError('Could not add DublinCore catalog to MediaPackage.');
      },
      success    : function(data) {
        MediaPackage.document = ocUtils.xmlToString(data);
        catalog.done = true;
        proceed();
      }
    });
  }

  function addTrack(track) {
    var $uploader = track.payload;
    var filename = $uploader.contents().find('.file-selector').val();

    ocUtils.log('Uploading ' + filename.replace("C:\\fakepath\\", "") + ' (' + track.id + ')');
    ocUpload.UI.setProgress("Uploading " + filename.replace("C:\\fakepath\\", ""));

    var checkBox = $('input.file-source-select:checked');
    var checkBoxId = $(checkBox).attr('id');

    // set flavor and mediapackage in upload form before submit
    $uploader.contents().find('#flavor').val(track.flavor);
    $uploader.contents().find('#mediapackage').val(MediaPackage.document);
    $uploader.contents().find('#uploadForm').submit();

    if(checkBoxId == 'fileSourceSingleA')
    	ocUpload.Listener.startProgressUpdate(track.id);
  }

  this.trackDone = function(jobId) {
    var track = null;
    $(MediaPackage.elements).each(function(index, element) {
      if (element.id == jobId) {
        track = element;
      }
    });
    $uploader = track.payload;
    MediaPackage.document = $uploader.contents().find('#mp').val();
    track.done = true;
    proceed();
  }

  function createSeries(name) {
    var id = false;
    var seriesXml = '<dublincore xmlns="http://www.opencastproject.org/xsd/1.0/dublincore/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:oc="http://www.opencastproject.org/matterhorn"><dcterms:title xmlns="">' + name + '</dcterms:title></dublincore>';
    var anonymous_role = 'anonymous';

    ocUpload.UI.setProgress("Creating Series " + name);
    $.ajax({
        url: ocUpload.ANOYMOUS_URL,
        type: 'GET',
        dataType: 'json',
        async: false,
        error: function () {
          if (ocUtils !== undefined) {
            ocUtils.log("Could not retrieve anonymous role " + ocUpload.ANOYMOUS_URL);
          }
        },
        success: function(data) {
        	anonymous_role = data.org.anonymousRole;
        }
    });
    $.ajax({
      async: false,
      type: 'POST',
      url: ocUpload.SERIES_URL,
      data: {
        series: seriesXml,
        acl: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><ns2:acl xmlns:ns2="org.opencastproject.security"><ace><role>' + anonymous_role + '</role><action>read</action><allow>true</allow></ace></ns2:acl>'
      },
      dataType : 'xml',
      error: function() {
        ocUpload.Listener.ingestError('Could not create Series ' + name);
      },
      success: function(data){
        window.debug = data;
        //id = $('identifier', data).text();
        id = $(data).find('[nodeName="dcterms:identifier"]').text();
      }
    });
    return id;
  }

  function getSeriesCatalog(id) {
    var catalog = null;
    ocUpload.UI.setProgress("Loading Series Catalog");
    $.ajax({
      url : '/series/' + id + '.xml',
      type : 'get',
      async : false,
      dataType : 'xml',              // TODO try to take the response directly as string
      error : function() {
        ocUpload.Listener.ingestError("Could not get Series Catalog");
      },
      success : function(data) {
        catalog = ocUtils.xmlToString(data);
      }
    });
    return catalog;
  }

  this.getWorkflowConfiguration = function($container) {
    var out = new Object();
    $container.find('.configField').each( function(idx, elm) {
      if ($(elm).is('[type=checkbox]')) {
        if ($(elm).is(':checked')) {
          out[$(elm).attr('id')] = $(elm).val();
        }
      } else {
        out[$(elm).attr('id')] = $(elm).val();
      }
    });
    return out;
  }

  return this;
})();

/** @namespace Listener for Upload Events
 *
 */
ocUpload.Listener = (function() {

  var Update = {
    id : false,
    jobId : null,
    inProgress : false
  }

  this.uploadComplete = function(jobId) {
    destroyUpdateInterval();
    ocUtils.log("Upload complete " + jobId);
    ocUpload.UI.setProgress('Upload successful');
    ocUpload.Ingest.trackDone(jobId);
  }

  this.uploadFailed = function(jobId) {
    ocUtils.log("ERROR: Upload failed " + jobId);
    destroyUpdateInterval();
    ocUpload.Listener.ingestError('Upload has failed!');
  }

  this.ingestError = function(message) {
    ocUpload.Ingest.discardIngest();
    ocUpload.UI.showFailure(message);
  }

  this.startProgressUpdate = function(jobId) {
    Update.inProgress = false;
    Update.jobId = jobId;
    Update.id = window.setInterval(requestUpdate, ocUpload.UPLOAD_PROGRESS_INTERVAL);
  };

  function requestUpdate() {
    if (!Update.inProgress && Update.id !== null) {
      Update.inProgress = true;
      $.ajax({
        url : ocUpload.INGEST_PROGRESS_URL + '/' + Update.jobId,
        type : 'get',
        dataType : 'json',
        success : receiveUpdate
      });
    }
  }

  function receiveUpdate(data) {
    Update.inProgress = false;
    ocUpload.UI.setProgress(data);
  }

  function destroyUpdateInterval() {
    window.clearInterval(Update.id);
    Update.id = null;
  }

  return this;
})();
