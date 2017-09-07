/** Upload asset resource
 This resource performs the POST communication with the server
 to start the workflow to process the upload files  */
angular.module('adminNg.resources')
.factory('EventUploadAssetResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id/assets', {}, {
        save: {
        method: 'POST',
        // By setting ‘Content-Type’: undefined, the browser sets the
        // Content-Type to multipart/form-data for us and fills in the
        // correct boundary. Manually setting ‘Content-Type’:
        // multipart/form-data will fail to fill in the boundary parameter
        // of the request.
        // multi-part file must be POST (not PUT) per ServletFileUpload.isMultipartContent
        headers: { 'Content-Type': undefined },
        responseType: 'text',
        transformResponse: [],
        transformRequest: function (data) {
                var workflowConfiguration = {};

                if (angular.isUndefined(data)) {
                    return data;
                }
                // The end point expects a multipart request payload with two fields
                // 1. A form field called 'metadata' containing asset data
                // 2. A non-form field which contains a File object
                var fd = new FormData(), assets, tempAssetList = [], flavorList = [];
                var assetMetadata = data['metadata'].assets;
                if (data['upload-asset']) {
                  assets = data['upload-asset'];
                }

                if (assets) {
                  angular.forEach(assets, function(files, name) {
                    angular.forEach(files, function (file, index) {
                      fd.append(name + "." + index, file);
                      tempAssetList.push(name);
                    });
                  });
                }

                // get source flavors
               assetMetadata.forEach(function(dataItem) {
                   if (tempAssetList.indexOf(dataItem.id) >= 0) {
                     flavorList.push(dataItem.flavorType + "/" + dataItem.flavorSubType);
                     // Special case to flag workflow to skip the "search+preview" image operation.
                     // If more than one special case comes up in the future,
                     // consider generalizing variable creation with
                     //   camelCase('uploaded', flavor, subflavor)
                     if (dataItem.flavorSubType == "search+preview") {
                       workflowConfiguration["uploadedSearchPreview"] = "true";
                     }
                   }
                });

                // set workflow boolean param and flavor list param
                if (flavorList.length > 0) {
                    workflowConfiguration["downloadSourceflavorsExist"] = "true";
                    workflowConfiguration["download-source-flavors"] = flavorList.join(", ");
                }

                // Add metadata form field
                fd.append('metadata', JSON.stringify({
                    assets: {
                      options: assetMetadata
                    },
                    processing: {
                      workflow: data['workflow'],
                      configuration: workflowConfiguration
                    }
                }));

                return fd;
            }
        }
    });
}]);
