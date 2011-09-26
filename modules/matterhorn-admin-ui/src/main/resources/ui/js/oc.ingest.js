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
var ocIngest = ocIngest || {};

ocIngest.debug = true;
ocIngest.mediaPackage = null;
ocIngest.metadata = null;
ocIngest.seriesDC = null;
ocIngest.previousMediaPackage = null;
ocIngest.previousFiles = new Array();

ocIngest.createMediaPackage = function() {
  ocUtils.log("creating MediaPackage")
  ocUpload.setProgress('0%','creating MediaPackage',' ', ' ');
  $.ajax({
    url        : '../ingest/createMediaPackage',
    type       : 'GET',
    dataType   : 'xml',
    error      : function(XHR,status,e){
      showFailedScreen('Could not create MediaPackage on server.');
    },
    success    : function(data, status) {
      ocUtils.log("MediaPackage created");
      ocIngest.mediaPackage = data;
      if (ocUpload.retryId != '') {
        // add tracks from old mediaPackage to the new one
        ocUtils.log("adding files from previous mediaPackge");
        ocIngest.copyPreviousFiles(ocIngest.mediaPackage);
      } else {
        var uploadFrame = document.getElementById("fileChooserAjax");
        uploadFrame.contentWindow.document.uploadForm.flavor.value = $('#flavor').val();
        uploadFrame.contentWindow.document.uploadForm.mediaPackage.value = ocUtils.xmlToString(data);
        var uploadingFile = $('#ingestUpload').is(':checked');
        ocUploadListener.uploadStarted(uploadingFile);
        uploadFrame.contentWindow.document.uploadForm.submit();
      }
    }
  });
}

ocIngest.copyPreviousFiles = function(data) {
  if (ocIngest.previousFiles.length != 0) {
    var fileItem = ocIngest.previousFiles.pop();
    $.ajax({
      url        : '../ingest/addTrack',
      type       : 'POST',
      dataType   : 'xml',
      data       : {
        mediaPackage: ocUtils.xmlToString(ocIngest.mediaPackage),
        flavor: fileItem.flavor,
        url: fileItem.url
      },
      error      : function(XHR,status,e){
        ocUpload.showFailedScreen('Could not add DublinCore catalog to MediaPackage.');
      },
      success    : function(data, status) {
        ocIngest.mediaPackage = data;
        ocIngest.copyPreviousFiles(data);
      }
    });
  } else {
    ocIngest.addCatalog(ocUtils.xmlToString(ocIngest.mediaPackage), ocIngest.createDublinCoreCatalog(ocIngest.metadata), 'dublincore/episode');
  }
}

ocIngest.createDublinCoreCatalog = function(data) {
  var dc = ocUtils.createDoc('dublincore','http://www.opencastproject.org/xsd/1.0/dublincore/');
  dc.documentElement.setAttribute('xmlns:dcterms','http://purl.org/dc/terms/');
  var key = '';
  for (key in data) {
    if (data[key] instanceof Array) {
      jQuery.each(data[key], function(k,val) {
        var elm = dc.createElement('dcterms:' + key);
        elm.appendChild(dc.createTextNode(val));    // FIXME get rid of xmlns="" attribute
        dc.documentElement.appendChild(elm);  
      });
    } else {
      var elm = dc.createElement('dcterms:' + key);
      elm.appendChild(dc.createTextNode(data[key]));    // FIXME get rid of xmlns="" attribute
      dc.documentElement.appendChild(elm);
    }
  }
  return dc;
}

ocIngest.addCatalog = function(mediaPackage, dcCatalog, flavor) {
  ocUtils.log("Adding DublinCore catalog");
  ocUpload.setProgress('100%','adding Metadata',' ', ' ');
  $.ajax({
    url        : '../ingest/addDCCatalog',
    type       : 'POST',
    dataType   : 'xml',
    data       : {
      flavor : flavor,
      mediaPackage: mediaPackage,
      dublinCore  : ocUtils.xmlToString(dcCatalog)
    },
    error      : function(XHR,status,e){
      showFailedScreen('Could not add DublinCore catalog to MediaPackage.');
    },
    success    : function(data, status) {
      ocUtils.log("DublinCore catalog added");
      ocIngest.mediaPackage = data;
      var seriesId = $('#ispartof').val();
      if (seriesId && ocIngest.seriesDC == null) {
        ocIngest.addSeriesCatalog(seriesId);
      } else {
        ocIngest.startIngest(data);
      }
    }
  });
}

ocIngest.addSeriesCatalog = function(seriesId) {
  ocUtils.log("Getting series DublinCore");
  ocUpload.setProgress('100%','Getting series Metadata',' ', ' ');
  $.ajax({
    url        : '../series/'+seriesId+'/dublincore',
    type       : 'GET',
    error      : function(XHR,status,e){
      showFailedScreen('The metadata for the series you selected could not be retrieved.');
    },
    success    : function(data, status) {
      ocUtils.log("Adding series metadata");
      ocIngest.seriesDC = data;
      ocIngest.addCatalog(ocUtils.xmlToString(ocIngest.mediaPackage), data, 'dublincore/series');
    }
  });
}

ocIngest.startIngest = function(mediaPackage) {
  ocUtils.log("Starting Ingest on MediaPackage with Workflow " + $('#workflowSelector').val());
  ocUpload.setProgress('100%','starting Ingest',' ', ' ');
  var data = ocWorkflow.getConfiguration($('#workflowConfigContainer'));
  data['mediaPackage'] = ocUtils.xmlToString(mediaPackage);
  $.ajax({
    url        : '../ingest/ingest/' + $('#workflowSelector').val(),
    type       : 'POST',
    dataType   : 'text',
    data       : data,
    error      : function(XHR,status,e) {
      showFailedScreen("Could not start Ingest on MediaPackage");
    },
    success    : function(data, status) {
      if (ocUpload.retryId != '') {
        ocIngest.removeWorkflowInstance(ocUpload.retryId);
      } else {
        ocUpload.hideProgressStage();
        ocUpload.showSuccessScreen();
      }
    }
  });
}

ocIngest.removeWorkflowInstance = function(wfId) {
  $.ajax({
    url : '../workflow/stop/',
    data: {id: wfId},
    type: 'POST',
    error: function() {
      ocUpload.hideProgressStage();   // better than showing error since new workflow has already been successfully started at this point
      ocUpload.showSuccessScreen();
    },
    success: function() {
      ocUpload.hideProgressStage();
      ocUpload.showSuccessScreen();
    }
  });
}