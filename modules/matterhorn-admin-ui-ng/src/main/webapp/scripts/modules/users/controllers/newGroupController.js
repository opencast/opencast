angular.module('adminNg.controllers')
.controller('NewGroupCtrl', ['$scope', '$timeout', 'Table', 'NewGroupStates', 'ResourcesListResource', 'GroupsResource', 'Notifications', 'Modal',
    function ($scope, $timeout, Table, NewGroupStates, ResourcesListResource, GroupsResource, Notifications, Modal) {

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
            $scope.group.roles.push(value.name);
          });

          GroupsResource.create($scope.group, function () {
              // Fetching immediately does not work
              $timeout(function () {
                  Table.fetch();
              }, 500);
              Modal.$scope.close();

              // Reset all states
              angular.forEach($scope.states, function(state)  {
                if (angular.isDefined(state.stateController.reset)) {
                    state.stateController.reset();
                }
              });

              Notifications.add('success', 'GROUP_ADDED');

          }, function (response) {
        	  if(response.status === 409) {
                  Notifications.add('error', 'GROUP_CONFLICT', 'add-group-form');
              } else {
                  Notifications.add('error', 'GROUP_NOT_SAVED', 'add-group-form');
              }
          });

        };

        // Reload tab resource on tab changes
        $scope.$parent.$watch('tab', function (value) {
          angular.forEach($scope.states, function (state) {
            if (value === state.name && !angular.isUndefined(state.stateController.reload)) {
              state.stateController.reload();
            }
          });
        });
    }
]);
