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
 * @name adminNg.modal.confirmationModal
 * @description
 * Opens the modal with the name specified in the `confirmation-modal` attribute.
 *
 * The success callback name- and id defined in the `callback` and `id` data
 * attributes will be sent to the {@link adminNg.modal.ConfirmationModal service's show method}.
 *
 * @example
 * <a data-confirmation-modal="confirm-deletion-modal"
 *    data-success-callback="delete" data-success-id="82">delete
 * </a>
 */
angular.module('adminNg.directives')
.directive('confirmationModal', ['ConfirmationModal', function (ConfirmationModal) {
  return {
    scope: {
      callback: '=',
      object:   '='
    },
    link: function ($scope, element, attr) {
      element.bind('click', function () {
        ConfirmationModal.show(attr.confirmationModal, $scope.callback, $scope.object);
      });

      $scope.$on('$destroy', function () {
        element.unbind('click');
      });
    }
  };
}]);
