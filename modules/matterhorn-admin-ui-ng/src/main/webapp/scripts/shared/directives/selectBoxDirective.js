angular.module('adminNg.directives')
.directive('adminNgSelectBox', [function () {
    return {
        restrict: 'E',
        templateUrl: 'shared/partials/selectContainer.html',
        scope: {
            resource: '=',
            groupBy:  '@',
            disabled: '='
        },
        controller: function ($scope) {
            $scope.move = function (key, from, to) {
                var j = 0;
                for (; j < from.length; j++) {
                    if (from[j].name === key) {
                        to.push(from[j]);
                        from.splice(j, 1);
                        return;
                    }
                }
            };

            $scope.add = function () {
                if (angular.isUndefined(this.markedForAddition)) { return; }

                for (var i = 0; i < this.markedForAddition.length; i++) {
                    this.move(this.markedForAddition[i].name, this.resource.available, this.resource.selected);
                }

                this.markedForAddition.splice(0);
            };

            $scope.remove = function () {
                if (angular.isUndefined(this.markedForRemoval)) { return; }

                for (var i = 0; i < this.markedForRemoval.length; i++) {
                    this.move(this.markedForRemoval[i].name, this.resource.selected, this.resource.available);
                }

                this.markedForRemoval.splice(0);
            };
        }
    };
}]);
