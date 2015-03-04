angular.module('adminNg.controllers')
.controller('NewAclCtrl', ['$scope', 'NewAclStates', 'ResourcesListResource', 'AclsResource', 'Notifications', 'Modal',
    function ($scope, NewAclStates, ResourcesListResource, AclsResource, Notifications, Modal) {

        $scope.states = NewAclStates.get();

        $scope.submit = function () {
          var access = $scope.states[1].stateController.ud,
              ace = [];

          angular.forEach(access.policies, function (policy) {
              if (angular.isDefined(policy.role)) {
                  if (policy.read) {
                      ace.push({
                          'action' : 'read',
                          'allow'  : policy.read,
                          'role'   : policy.role
                      });
                  }

                  if (policy.write) {
                      ace.push({
                          'action' : 'write',
                          'allow'  : policy.write,
                          'role'   : policy.role
                      });   
                  }
              }

          });

          $scope.acl = {
            name : $scope.states[0].stateController.metadata.name,
            acl  : {
              ace: ace
            }
          };

          AclsResource.create($scope.acl, function () {
              Modal.$scope.close();

              // Reset all states
              angular.forEach($scope.states, function(state)  {
                  if (angular.isDefined(state.stateController.reset)) {
                      state.stateController.reset();
                  }
              });

              Notifications.add('success', 'ACL_ADDED');
          }, function () {
              Notifications.add('error', 'ACL_NOT_SAVED', 'acl-form');
          });

        };
    }
]);
