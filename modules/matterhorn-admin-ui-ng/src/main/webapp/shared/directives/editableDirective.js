angular.module('adminNg.directives')
.directive('adminNgEditable', ['AuthService', 'ResourcesListResource', function (AuthService, ResourcesListResource) {
    return {
        restrict: 'A',
        templateUrl: 'shared/partials/editable.html',
        transclude: true,
        replace: false,
        scope: {
            params: '=',
            save:   '=',
            target: '@', // can be used to further specify how to save data
            requiredRole: '@'
        },
        link: function (scope, element) {
            if (scope.params.readOnly) {
                scope.mode = 'readOnly';
            } else {
                if (angular.isDefined(scope.requiredRole)) {
                    AuthService.userIsAuthorizedAs(scope.requiredRole, function () { }, function () {
                        scope.mode = 'readOnly';
                    });
                }

                if (scope.mode !== 'readOnly') {
                    if (typeof scope.params.collection === 'string') {
                        scope.collection = ResourcesListResource.get({ resource: scope.params.collection });
                    } else if (typeof scope.params.collection === 'object') {
                        scope.collection = scope.params.collection;
                    }

                    if (scope.params.type === 'boolean') {
                        scope.mode = 'booleanValue';
                    } else if (scope.params.type === 'date') {
                        scope.mode = 'dateValue';
                    } else {
                        if (scope.params.value instanceof Array) {
                            if (scope.collection) {
                                scope.mode = 'multiSelect';
                            } else {
                                scope.mode = 'multiValue';
                            }
                        } else {
                            if (scope.collection) {
                                scope.mode = 'singleSelect';
                            } else {
                                scope.mode = 'singleValue';
                            }
                        }
                    }
                }
            }
            if (scope.mode !== 'readOnly') {
                element.addClass('editable');
            }

        }
    };
}]);
