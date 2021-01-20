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

/**
 * Dynamic Asset Upload Options List Service
 *
 * A service to retrieve and provide a list of available upload options (track, attachment, catalog).
 *
 * The asset upload options list is a customizable list of assets that can be uplaoded via admin-ng UI
 *
 * The production provider list exists at etc/listproviders/event.upload.asset.options.properties
 * The LOCAL Development surrogate testing file is <admin-ng module testing path>/resources/eventUploadAssetOptions.json
 *
 */
angular.module('adminNg.services').service('UploadAssetOptions',[ 'ResourcesListResource', '$q', function (
  ResourcesListResource, $q) {
  var _uploadOptions = undefined;
  var _result = undefined;
  var service = {};
  var _OptionPrefixSource = 'EVENTS.EVENTS.NEW.SOURCE.UPLOAD';
  var _OptionPrefixAsset = 'EVENTS.EVENTS.NEW.UPLOAD_ASSET.OPTION';
  var _WorkflowPrefix = 'EVENTS.EVENTS.NEW.UPLOAD_ASSET.WORKFLOWDEFID';

  service.getOptionsPromise = function(){
    // don't retrieve again if alreay retrieved for this session
    if (!_uploadOptions) {
      var deferred = $q.defer();
      ResourcesListResource.get({resource: 'eventUploadAssetOptions'},
        function (data) {
          _result = {};
          _uploadOptions = [];
          angular.forEach(data, function (assetOption, assetKey) {
            if (assetKey.charAt(0) !== '$') {
              if ((assetKey.indexOf(_OptionPrefixAsset) >= 0) || (assetKey.indexOf(_OptionPrefixSource) >= 0)) {
                // parse upload asset options
                var options = JSON.parse(assetOption);
                if (!options['title']) {
                  options['title'] = assetKey;
                }
                _uploadOptions.push(options);
              } else if (assetKey.indexOf(_WorkflowPrefix) >= 0) {
                // parse upload workflow definition id
                _result['workflow'] = assetOption;
              }
            }
          });
          _result['options'] = _uploadOptions;
          deferred.resolve(_result);
        });
      // set _uploadOptions to be a promise until result comeback
      _result = deferred.promise;
    }
    // This resolves immediately if options were already retrieved
    return $q.when(_result);
  };
  // return the AssetUploadOptions service
  return service;
}]);
