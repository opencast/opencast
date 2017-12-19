angular.module('adminNg.directives')
    .directive('focushere', ['$timeout', function($timeout) {
    return {
      restrict: 'A',
      link : function($scope, $element, $attributes) {
        if ($attributes.focushere === undefined || $attributes.focushere === "1" || $attributes.focushere === false) {
          $timeout(function() {
            if (angular.element($element[0]).hasClass("chosen-container")) {
              angular.element($element[0]).trigger('chosen:activate');
            } else {
              $element[0].focus();
            }
          });
        }
      }
    }
}]);
