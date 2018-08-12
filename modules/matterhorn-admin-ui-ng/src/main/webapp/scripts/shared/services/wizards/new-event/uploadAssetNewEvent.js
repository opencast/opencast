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
'use strict';

// Asset Upload service for New Events (MH-12085)
// This is used in the new events states wizard directive

angular.module('adminNg.services').factory('NewEventUploadAsset',['ResourcesListResource', 'UploadAssetOptions','JsHelper', 'Notifications', '$interval',
function (ResourcesListResource, UploadAssetOptions, JsHelper, Notifications, $interval) {

  // -- constants ------------------------------------------------------------------------------------------------- --
  var NOTIFICATION_CONTEXT = 'upload-asset';

  // Example of JSON used for the option list
  // The true values are retrieved by UploadAssetOptions from
  //  ./etc/listproviders/event.upload.asset.options.properties
  var exampleOfUploadAssetOptions = [
    {"id": "attachment_attachment_notes",
      "title": "class handout notes",
      "flavorType": "attachment",
      "flavorSubType": "notes",
      "type": "attachment"
    }, {"id":"catalog_captions_dfxp",
      "title": "captions DFXP",
      "flavorType": "captions",
      "flavorSubType": "timedtext",
      "type": "catalog"
    },{"id": "attachment_text_webvtt",
      "title": "Captions WebVTT",
      "flavorType": "text",
      "flavorSubType": "webvtt",
      "type": "attachment"
    },{"id":"attachment_presenter_search_preview",
      "title": "video list thumbnail",
      "flavorType": "presenter",
      "flavorSubType": "search+preview",
      "type": "attachment"
    }
  ];

  // -- instance -------------------------------------------------------------------------------------------------- --

  var NewEventUploadAsset = function () {

    var self = this;

    this.reset = function () {
      self.requiredMetadata = {};
      self.ud = {};
      self.ud.assets = {};
      self.ud.defaults = {};
      self.ud.namemap = {};
      self.ud.assetlistforsummary = [];
      self.ud.hasNonTrackOptions = false;
    };
    self.reset();

    // This is used as the callback from the uploadAssetDirective
    self.onAssetUpdate = function() {
       self.updateAssetsForSummary();
    },

    // Create an array of summary metadata for uploaded assets
    // to be used in the new event summary tab
    self.updateAssetsForSummary =  function () {
      self.ud.assetlistforsummary = [];
      angular.forEach(self.ud.assets, function ( value, key) {
         var item = {};
         var fileNames = [];
         item.id = key;
         item.title = self.ud.namemap[key].title;
         angular.forEach(value, function (file) {
           fileNames.push(file.name);
         });
         item.filename =  fileNames.join(", ");
         item.type = self.ud.namemap[key].type;
         item.flavor = self.ud.namemap[key].flavorType + "/" + self.ud.namemap[key].flavorSubType;
         self.ud.assetlistforsummary.push(item);
      });
    };

    // Retrieve the configured map of asset upload options
    // saved at the wizard level to make them available to all tabs and
    // prevents issues with the option ng-repeat.
    self.addSharedDataPromise = function() {
      UploadAssetOptions.getOptionsPromise().then(function(data){
        self.ud.defaults = data;
        self.visible = false;
        if (!self.wizard.sharedData) {
           self.wizard.sharedData = {};
        }
        self.wizard.sharedData.uploadAssetOptions = data.options;
        // Filter out asset options of type "track" for the asset upload tab
        // Track source options are uploaded on a different tab
        angular.forEach(data.options, function(option) {
          self.ud.namemap[option.id] = option;
          if (option.type !== 'track') {
            self.ud.hasNonTrackOptions = true;
            self.visible = true;
          }
        });
        self.wizard.sharedData.uploadNameMap = self.ud.namemap;
      });
     }

    // This step is visible when event.upload.asset.options.properties
    // listprovider contains options for asset upload.
    self.checkIfVisible = function () {
      // Prohibit uploading assets to scheduled events
      if (self.ud.hasNonTrackOptions && self.wizard.getStateControllerByName("source").isUpload()) {
        self.visible = true;
      } else {
        self.visible = false;
        self.ud.assets = {};
      }
    };

    self.isVisible = $interval(self.checkIfVisible, 1000);

    // Checks if the current state of this wizard is valid and we are
    // ready to move on.
    self.isValid = function () {
      var result = true;
      angular.forEach(self.requiredMetadata, function (item) {
        if (item === false) {
          result = false;
        }
      });
      return result;
    };

    // remove a selected asset
    self.deleteSelection = function (assetToDelete) {
      var index;

      angular.forEach(self.ud.assets, function (asset, idx) {
        if (idx === assetToDelete) {
          delete self.ud.assets[idx];
        }
      });
    };

    self.getUserEntries = function () {
      return self.ud;
   };

    self.getAssetUploadSummary = function() {
      return self.ud.assetlistforsummary;
    }

    self.hasAssetUploads = function () {
      var result = false;
      angular.forEach(self.ud.assets, function (asset, idx) {
          result = true;
       });
      return result;
    };
  };

  return new NewEventUploadAsset();
}]);
