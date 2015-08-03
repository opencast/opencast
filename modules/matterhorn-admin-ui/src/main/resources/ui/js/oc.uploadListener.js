/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
var ocUploadListener = ocUploadListener || {};

ocUploadListener.jobId = "";
ocUploadListener.shortFilename = "";
ocUploadListener.appletPresent = false;
ocUploadListener.updateInterval = null;
ocUploadListener.updateRequested = false;

ocUploadListener.initialized = function() {
  ocUtils.log('Uploader initialized');
  $('#track').val();
  $('#BtnBrowse').attr('disabled', false);
  ocUploadListener.appletPresent = true;
}

ocUploadListener.resumeReady = function(jobId) {
  ocUtils.log('resume negotiation successful');
  var filename = document.Uploader.getFilename();
}


ocUploadListener.fileSelectedAjax = function(filename,jobId) {
  ocUtils.log("File selected for job " + jobId + ": " + filename);
  ocUploadListener.shortFilename = filename;
  ocUploadListener.jobId = jobId;
  $('#track').val(filename);
  var uploadForm = document.getElementById("fileChooserAjax").contentWindow.document.uploadForm;
  ocUpload.checkRequiredFields();
}

ocUploadListener.uploadStarted = function(uploadingFile) {
  ocUtils.log('upload started');
  if (uploadingFile) {
    ocUploadListener.updateInterval = window.setInterval('ocUploadListener.getProgress()', 1000);
  } else {
    ocUpload.setProgress('0%','moving file form Inbox to MediaPackage',' ', ' ');
  }
}

ocUploadListener.getProgress = function() {
  if (!ocUploadListener.updateRequested) {
    ocUploadListener.updateRequested = true;
    $.ajax({
      url        : '../ingest/getProgress/' + ocUploadListener.jobId,
      type       : 'GET',
      dataType   : 'json',
      error      : function(XHR,status,e){
        ocUtils.log('failed to get progress information from ' + '../ingest/getProgress/' + ocUploadListener.jobId);
        window.clearInterval(ocUploadListener.updateInterval); // ie in case of inbox ingest
      },
      success    : function(data, status) {
        ocUploadListener.updateRequested = false;
        ocUploadListener.uploadProgress(data.total, data.received);
      }
    });
  }
}

ocUploadListener.uploadProgress = function(total, transfered) {
  var MEGABYTE = 1024 * 1024;
  var percentage = 0;
  var megaBytes = 0;
  var totalMB = 0;
  if (transfered > 0) {
    percentage = transfered / total * 100;
    percentage = percentage.toFixed(2);
    percentage = percentage + '%';
    megaBytes = transfered / MEGABYTE;
    megaBytes = megaBytes.toFixed(2);
    totalMB = total / MEGABYTE;
    totalMB = totalMB.toFixed(2);
  }
  ocUtils.log("transfered: " + transfered + " of " + total + " MB, " + percentage + "%");
  ocUpload.setProgress(percentage,percentage,'Total: '+totalMB+' MB',megaBytes+' MB sent');
}

ocUploadListener.uploadComplete = function() {
  ocUploadListener.updateRequested = false;
  window.clearInterval(ocUploadListener.updateInterval);
  ocUtils.log("upload complete");
  var uploadFrame = document.getElementById("fileChooserAjax");
  var mp = uploadFrame.contentWindow.document.getElementById("mp").value;
  ocIngest.addCatalog(mp, ocIngest.createDublinCoreCatalog(ocIngest.metadata), 'dublincore/episode');
}

ocUploadListener.uploadFailed = function() {
  ocUploadListener.updateRequested = false;
  window.clearInterval(ocUploadListener.updateInterval);
  ocUtils.log('ERROR: media fileupload has failed');
  ocUpload.showFailedScreen("Media file upload has failed.");
}

ocUploadListener.error = function(message) {
  ocUploadListener.updateRequested = false;
  window.clearInterval(ocUploadListener.updateInterval);
  ocUtils.log('ERROR: ' + message);
}
