/* eslint-disable header/header */
/**
 * Copyright (C) 2013 Luegg
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/* Adapted from:
 * angularjs Scroll Glue
 * version 2.0.6
 * https://github.com/Luegg/angularjs-scroll-glue
 * An AngularJs directive that automatically scrolls to the bottom of an element on changes in it's scope.
*/

(function(angular){
  'use strict';

  function createActivationState($parse, attr, scope){
    function unboundState(initValue){
      var activated = initValue;
      return {
        getValue: function(){
          return activated;
        },
        setValue: function(value){
          activated = value;
        }
      };
    }

    return unboundState(true);
  }

  function createDirective(module, attrName, direction){
    module.directive(attrName, ['$parse', '$window', '$timeout', function($parse, $window, $timeout){
      return {
        priority: 1,
        restrict: 'A',
        link: function(scope, $el, attrs){
          var el = $el[0],
              scrollPos = 0,
              activationState = createActivationState($parse, attrs[attrName], scope);


          function scrollIfGlued() {
            if(activationState.getValue() && !direction.isAttached(el, scrollPos)){
              direction.scroll(el, scrollPos);
            }
          }

          function onScroll() {
            scrollPos = el.scrollTop;
            activationState.setValue(direction.isAttached(el, scrollPos));
          }

          scope.$watch(scrollIfGlued);

          $timeout(scrollIfGlued, 0, false);

          $window.addEventListener('resize', scrollIfGlued, false);

          $el.bind('scroll', onScroll);


          // Remove listeners on directive destroy
          $el.on('$destroy', function() {
            $el.unbind('scroll', onScroll);
          });

          scope.$on('$destroy', function() {
            $window.removeEventListener('resize',scrollIfGlued, false);
          });
        }
      };
    }]);
  }

  var to = {
    isAttached: function(el, pos){
      return el.scrollTop == pos;
    },
    scroll: function(el, pos){
      el.scrollTop = pos;
    }
  };

  var module = angular.module('opencast.directives', []);

  createDirective(module, 'opencastScrollGlue', to);
}(angular));
