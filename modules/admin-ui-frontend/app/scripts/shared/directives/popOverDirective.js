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
.directive('popOver', ['$timeout', function ($timeout) {
  return {
    restrict: 'A',
    link: function (scope, element) {
      var callToHide;

      element.mouseenter(function(){
        var popover = element.next('.js-popover');
        // make element visible in DOM,
        // otherwise positioning will not work
        popover.removeClass('is-hidden').removeClass('popover-left').removeClass('popover-right');

        // uses jquery-ui's #position
        popover.position({
          my: 'left+20 top-10',
          at: 'right',
          of: element
        });

        // add modifier
        if (popover.position().left < 0) {
          popover.addClass('popover-left');
        } else {
          popover.addClass('popover-right');
        }

        popover.mouseenter(function(){
          if(callToHide){
            // prevent hiding the popover bubble
            // because user has entered it
            $timeout.cancel(callToHide);
          }
        });

        popover.on('mouseleave click', function(){
          popover.addClass('is-hidden');
        });
      });

      element.mouseleave(function(){
        // don't hide immediately to allow user
        // to reach the popover bubble
        callToHide = $timeout(function(){
          var popover = element.next('.js-popover');
          popover.addClass('is-hidden');
        }, 200);
      });

      element.click(function(){
        // no-op
        return false;
      });
    }
  };
}]);
