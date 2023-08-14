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
 * @ngdoc directive
 * @name adminNg.modal.deleteSingleSeriesModal
 * @description
 * Opens the delete modal for a single series.
 *
 * The success callback name- and id defined in the `callback` and `id` data
 * attributes will be sent to the {@link adminNg.modal.DeleteSingleSeriesModal service's show method}.
 */
angular.module('adminNg.directives')
.directive('deleteSingleSeriesModal', ['DeleteSingleSeriesModal', function (DeleteSingleSeriesModal) {
  return {
    scope: {
      callback: '=',
      object:   '='
    },
    link: function ($scope, element, attr) {
      element.bind('click', function () {
        DeleteSingleSeriesModal.show(attr.deleteSingleSeriesModal, $scope.callback, $scope.object);
      });

      $scope.$on('$destroy', function () {
        element.unbind('click');
      });
    }
  };
}]);
