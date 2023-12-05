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
 * @ngdoc service
 * @name adminNg.modal.Modal
 * @description
 * Provides a service for displaying a confirmation dialog.
 * partial.
 */
angular.module('adminNg.services.modal')
.factory('ConfirmationModal', ['$location', 'Modal', function ($location, Modal) {
  var ConfirmationModal = function () {
    var me = this;

    /**
         * @ngdoc function
         * @name Modal.show
         * @methodOf adminNg.modal.Modal
         * @description
         * Displays a modal and its overlay.
         *
         * Loads markup via AJAX from 'partials/modals/{{ modalId }}.html'.
         *
         * @param {string} modalId Identifier for the modal.
         * @param {string} callback Name of the function to call when confirmed
         * @param {string} object Hash for the success callback function
         */
    this.show = function (modalId, callback, object) {
      Modal.show(modalId);
      me.$scope = Modal.$scope;

      me.$scope.confirm  = me.confirm;
      me.$scope.callback = callback;
      me.$scope.object   = object;
      me.$scope.name = 'undefined';
      me.$scope.type = 'UNKNOWN';
      if (!angular.isUndefined(object)) {
        me.$scope.id = object.id;
        if (object.title) {
          me.$scope.name = object.title;
        } else if (object.name) {
          me.$scope.name = object.name;
        }
        if (object.type) {
          me.$scope.type = object.type;
        }
      }
      //64 picked by random experimentation
      if (me.$scope.name.length > 64) {
        me.$scope.name = me.$scope.name.substr(0,61);
        me.$scope.name = me.$scope.name + '...';
      }
    };

    this.confirm = function () {
      me.$scope.callback(me.$scope.object);
      me.$scope.close();
    };
  };

  return new ConfirmationModal();
}]);
