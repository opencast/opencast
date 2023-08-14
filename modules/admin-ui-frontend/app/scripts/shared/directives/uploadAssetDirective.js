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

// Asset upload html UI directive
// Used in new event and existing event asset upload
angular.module('adminNg.directives')
.directive('adminNgUploadAsset', ['$filter', function ($filter) {
  return {
    restrict: 'A',
    scope: {
      assetoptions: '=',
      assets: '=filebindcontainer',
      onexitscope: '='
    },
    templateUrl: 'shared/partials/uploadAsset.html',
    link: function(scope, elem, attrs) {
      scope.removeFile = function(elemId) {
        delete scope.assets[elemId];
        angular.element(document.getElementById(elemId)).val('');
      };
      scope.addFile = function(file) {
        scope.assets[elem.id] = file;
      };
      // Allows sorting list on traslated title/description/caption
      scope.translatedTitle = function(asset) {
        return $filter('translateOverrideFallback')(asset);
      };
      //The "onexitscope"'s oldValue acts as the callback when the scope of the directive is exited.
      //The callback allow the parent scope to do work (i.e. make a summary map) based on
      //  activies performed in this directive.
      scope.$watch('onexitscope', function(newValue, oldValue) {
        if (oldValue) {
          oldValue();
        }
      });
    }
  };
}]);
