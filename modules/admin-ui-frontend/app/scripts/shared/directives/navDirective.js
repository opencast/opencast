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

angular.module('adminNg.directives')
.directive('adminNgNav', ['HotkeysService', 'StatisticsResource', function (HotkeysService,
  StatisticsResource) {
  return {
    restrict: 'E',
    replace: true,
    templateUrl: 'shared/partials/mainNav.html',
    link: function (scope, element) {
      // Menu roll up
      var menu = element.find('#roll-up-menu'),
          marginTop = element.height() + 1,
          isMenuOpen = false;

      StatisticsResource.query({
        resourceType: 'organization',
      }).$promise.then(function (providers) {
        scope.hasStatisticsProviders = providers.length !== 0;
      });

      scope.toggleMenu = function () {
        var menuItems = element.find('#nav-container'),
            mainView = angular.element('.main-view'),
            mainViewLeft = '130px';
        if (isMenuOpen) {
          menuItems.animate({opacity: 0}, 50, function () {
            $(this).css('display', 'none');
            menu.animate({opacity: 0}, 50, function () {
              $(this).css('overflow', 'visible');
              mainView.animate({marginLeft: '20px'}, 100);
            });
            isMenuOpen = false;
          });
        } else {
          mainView.animate({marginLeft: mainViewLeft}, 100, function () {
            menu.animate({marginTop: marginTop, opacity: 1}, 50, function () {
              $(this).css('overflow', 'visible');
              menuItems.animate({opacity: 1}, 50, function () {
                $(this).css('display', 'block');
                menu.css('margin-right', '20px');
              });
              isMenuOpen = true;
            });
          });
        }
      };

      HotkeysService.activateHotkey(scope, 'general.main_menu', function (event) {
        event.preventDefault();
        scope.toggleMenu();
      });
    }
  };
}]);
