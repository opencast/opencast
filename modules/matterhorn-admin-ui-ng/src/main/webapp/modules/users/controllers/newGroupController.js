angular.module('adminNg.controllers')
.controller('NewGroupCtrl', ['$scope', 'NewGroupStates', 'ResourcesListResource', 'GroupsResource', 'Notifications', 'Modal',
    function ($scope, NewGroupStates, ResourcesListResource, GroupsResource, Notifications, Modal) {

        $scope.states = NewGroupStates.get();

        $scope.submit = function () {
          $scope.group = {
            users       : [],
            roles       : [],
            name        : $scope.states[0].stateController.metadata.name,
            description : $scope.states[0].stateController.metadata.description
          };

          angular.forEach($scope.states[2].stateController.users.selected, function (value) {
            $scope.group.users.push(value.value);
          });

          angular.forEach($scope.states[1].stateController.roles.selected, function (value) {
            $scope.group.roles.push(value.value);
          });

          GroupsResource.create($scope.group, function () {
              Notifications.add('success', 'GROUP_ADDED');
              Modal.$scope.close();
          }, function () {
              Notifications.add('error', 'GROUP_NOT_SAVED', 'add-group-form');
          });

        };
    }
]);
