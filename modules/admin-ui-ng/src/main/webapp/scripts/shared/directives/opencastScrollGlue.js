/* Adapted from:
 * angularjs Scroll Glue
 * version 2.0.6
 * https://github.com/Luegg/angularjs-scroll-glue
 * An AngularJs directive that automatically scrolls to the bottom of an element on changes in it's scope.
*/

(function(angular, undefined){
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
