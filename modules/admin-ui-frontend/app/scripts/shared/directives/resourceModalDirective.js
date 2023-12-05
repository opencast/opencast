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
 * @name adminNg.directives.openModal
 * @description
 * Opens the modal with the name specified directive value.
 *
 * Optionally, a resource ID can be passed to the {@link adminNg.modal.ResourceModal service's show method}.
 * by setting the data attribute `resource-id` on the same element.
 *
 * @example
 * <a data-open-modal="recording-details" data-resource-id="82">details</a>
 */
angular.module('adminNg.directives')
.directive('openModal', ['ResourceModal', function (ResourceModal) {
  return {
    link: function ($scope, element, attr) {
      element.bind('click', function () {
        ResourceModal.show(attr.openModal, attr.resourceId, attr.tab, attr.action);
      });

      $scope.$on('$destroy', function () {
        element.unbind('click');
      });
    }
  };
}]);
