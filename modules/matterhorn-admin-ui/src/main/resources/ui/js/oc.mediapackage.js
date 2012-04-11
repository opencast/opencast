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
var ocMediapackage = ocMediapackage || {};

var MEDIAPACKAGE_REST_URL = "/mediapackage/";

ocMediapackage.addCatalog = function(mediapackage, catalogUri) {
  var data = {
    mediapackage: mediapackage,
    catalogUri: catalogUri
  }
  return getUpdatedMediapackage("addCatalog", data);
}

ocMediapackage.removeCatalog = function(mediapackage, catalogId) {
  var data = {
    mediapackage: mediapackage,
    catalogId: catalogId
  }
  return getUpdatedMediapackage("removeCatalog", data);
}

ocMediapackage.addTrack = function(mediapackage, trackUri, flavor) {
  ocUtils.log("Attempting to add track to MediaPackage: " + trackUri + " flavor=" + flavor);
  var data = {
    mediapackage: mediapackage,
    trackUri: trackUri,
    flavor: flavor
  }
  return getUpdatedMediapackage("addTrack", data)
}

ocMediapackage.removeTrack = function(mediapackage, trackId) {
  var data = {
    mediapackage: mediapackage,
    trackId: trackId
  }
  return getUpdatedMediapackage("removeTrack", data);
}

ocMediapackage.addAttachment = function (mediapackage, flavor, mimeType, attachmentUri) {
  var data = {
    mediapackage: mediapackage,
    flavor: flavor,
    mimeType: mimeType,
    attachmentUri: attachmentUri
  }
  return getUpdatedMediapackage("addAttachment", data);
}

ocMediapackage.removeAttachment = function(mediapackage, attachmentId) {
  var data =  {
    mediapackage: mediapackage,
    attachmentId: attachmentId
  }
  return getUpdatedMediapackage("removeAttachment", data);
}

ocMediapackage.newMediapackage = function() {
  var data = {};
  return getUpdatedMediapackage("new", data);
}

ocMediapackage.addContributor = function(mediapackage, contributor) {
  var data =  {
    mediapackage: mediapackage,
    contributor: contributor
  }
  return getUpdatedMediapackage("addContributor", data);
}

ocMediapackage.removeContributor = function(mediapackage, contributor) {
  var data =  {
    mediapackage: mediapackage,
    contributor: contributor
  }
  return getUpdatedMediapackage("removeContributor", data);
}

ocMediapackage.addCreator = function(mediapackage, creator) {
  var data = {
    mediapackage: mediapackage,
    creator: creator
  }
  return getUpdatedMediapackage("addCreator", data);
}

ocMediapackage.removeCreator = function(mediapackage, creator) {
  var data = {
    mediapackage: mediapackage,
    creator: creator
  }
  return getUpdatedMediapackage("removeCreator", data);
}

ocMediapackage.addSubject = function(mediapackage, subject) {
  var data = {
    mediapackage: mediapackage,
    subject: subject
  }
  return getUpdatedMediapackage("addSubject", data);
}

ocMediapackage.removeSubject = function(mediapackage, subject) {
  var data = {
    mediapackage: mediapackage,
    subject: subject
  }
  return getUpdatedMediapackage("removeSubject", data);
}

function getUpdatedMediapackage(url, data) {
  var result = "";
  $.ajax({
    url: MEDIAPACKAGE_REST_URL + url,
    data: data,
    type: 'POST',
    async: false,
    dataType: "text",
    success: function(data) {
      result = data;
    }
  });
  return result;
}
  