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
    },{"id": "attachment_text_vtt",
      "title": "captions VTT",
      "flavorType": "text",
      "flavorSubType": "vtt",
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
    self.requiredMetadata = {};
    self.ud = {};
    self.ud.assets = {};
    self.ud.defaults = {};
    self.ud.namemap = {};
    self.ud.assetlistforsummary = [];

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
        if (!self.wizard.sharedData) {
           self.wizard.sharedData = {};
        }
        self.wizard.sharedData.uploadAssetOptions = data.options;
        angular.forEach(data.options, function(option) {
          self.ud.namemap[option.id] = option;
          // options exist, Ok to be visible
          self.visible = true;
        });
        self.wizard.sharedData.uploadNameMap = self.ud.namemap;
      });
     }

    // This step is visible when event.upload.asset.options.properties
    // listprovider contains options for asset upload.
    self.checkIfVisible = function () {
      self.visible = false;
      if (self.wizard.getStateControllerByName("source").isUpload()) {
        angular.forEach(self.ud.defaults, function(option) {
          self.visible = true;
        });
      } else {
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
