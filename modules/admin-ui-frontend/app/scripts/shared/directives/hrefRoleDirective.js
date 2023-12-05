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
 * @name adminNg.directives.adminNgHrefRole
 * @description
 * Overrides the 'href' attribute of an element depending on the user's roles.
 *
 * @example
 * <a admin-ng-href-role="{'STUDENT': '/student', 'TEACHER': '/teacher'}" href="/general" />
 */
angular.module('adminNg.directives')
.directive('adminNgHrefRole', ['AuthService', function (AuthService) {
  return {
    scope: {
      rolesHref: '@adminNgHrefRole'
    },
    link: function ($scope, element) {
      var href;
      angular.forEach($scope.$eval($scope.rolesHref), function(value, key) {
        if (AuthService.userIsAuthorizedAs(key) && angular.isUndefined(href)) {
          href = value;
        }
      });

      if (angular.isDefined(href)) {
        element.attr('href', href);
      }
    }
  };
}]);
