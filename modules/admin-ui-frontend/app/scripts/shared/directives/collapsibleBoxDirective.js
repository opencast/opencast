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
 * @name adminNg.directives.collapsibleBox
 * @description
 * Generates a collapsible/expandable text field
 *
 * The collapsible box displays a text in a div with a max-height. If the content
 * is too large to fit within the max-height the collapsible box becomes expandable/
 * collapsible. The text overflow is hidden while collapsed.
 *
 * Single linebreaks, intendentions or multiple intendations will be removed. Empty
 * lines will be replaced by single line break.
 *
 * @example
 * <div class="collapsible-box" input=inputStringFromScope maxheight=100 />
 */
angular.module('adminNg.directives')
.directive(
  'collapsibleBox',
  function() {
    return {
      restrict : 'C',
      templateUrl : 'shared/partials/collapsibleBox.html',
      scope : {
        input : '=',
        maxheight : '@'
      },
      controller : [
        '$scope',
        '$element',
        '$filter',
        '$timeout',
        '$window',
        function($scope, $element, $filter, $timeout, $window) {

          $scope.initCollapsibleBox = function() {
            $scope.angleUp = false;
            $scope.angleDown = true;
            $scope.collapsibleStyle = { 'max-height' : $scope.maxheight + 'px' };
            $scope.isCollapsed = true;
            $timeout(function() {
              $scope.isOverflown = $scope.getIsOverflown();
              $scope.calcStyleProperties();
            }, 0);

            $scope.$watch('isOverflown', function() {
              $scope.calcStyleProperties();
            });

            $scope.$watch('input', function() {
              $scope.hasInput = !($scope.input == '' || $scope.input == undefined);
              if($scope.hasInput) {
                $scope.formatInput();
              } else {
                $scope.inputFormatted = '';
              }
              $timeout(function() {
                $scope.isOverflown = $scope.getIsOverflown();
                $scope.calcStyleProperties();
              });
            });

            angular.element($window).bind('resize', function() {
              $scope.$apply(function() {
                $scope.isOverflown = $scope.getIsOverflown();
              });
            });
          };

          $scope.getIsOverflown = function() {
            return $element.children().first()[0].scrollHeight > $scope.maxheight;
          };

          $scope.calcStyleProperties = function() {
            var overlayPadding = $scope.isOverflown ? $scope.maxheight / 4 : 0;
            $scope.collapsibleOverlayStyle = { padding : overlayPadding + 'px 0' };
          };

          $scope.toggle = function() {
            if ($scope.isOverflown
                && $window.getSelection().toString() == '') {
              $scope.isCollapsed = !$scope.isCollapsed;
              $scope.angleUp = !$scope.angleUp;
              $scope.angleDown = !$scope.angleDown;
              $scope.collapsibleTransparentOverlay = !$scope.collapsibleTransparentOverlay;
              if($scope.collapsibleStyle['max-height'] === $scope.maxheight + 'px') {
                $scope.collapsibleStyle['max-height'] = 'auto';
              } else {
                $scope.collapsibleStyle['max-height'] = $scope.maxheight + 'px';
              }
            }
          };

          $scope.formatInput = function(){
            var reEmptyLines = /\n^[\r\t\f ]*$/gm;
            var reWhitespaceChars = /[\r\t\f\n]|/g;
            var reMltplSpaces = /[ ]{2,}/g;
            var inputArr = $scope.input.split(reEmptyLines);
            for(var i = 0; i < inputArr.length; i++) {
              inputArr[i] = inputArr[i].replace(reWhitespaceChars, '').replace(reMltplSpaces, ' ');
            }
            $scope.inputFormatted = inputArr.join('\n');
          };
          $scope.initCollapsibleBox();
        }]
    };
  });
