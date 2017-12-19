angular.module('adminNg.controllers')
.controller('GroupCtrl', ['$scope', 'AuthService', 'UserRolesResource', 'ResourcesListResource', 'GroupResource', 'GroupsResource', 'Notifications', 'Modal',
    function ($scope, AuthService, UserRolesResource, ResourcesListResource, GroupResource, GroupsResources, Notifications, Modal) {

        var reloadSelectedUsers = function () {
            $scope.group.$promise.then(function() {
                $scope.user.available.$promise.then(function() {
                    // Now that we have the user users and the available users populate the selected and available
                    $scope.user.selected = [];
                    angular.forEach($scope.group.users, function (user) {
                        $scope.user.selected.push({name: user.name, value: user.username});
                    });
                    // Filter the selected from the available list
                    $scope.user.available = _.filter($scope.user.available, function(user) {
                        return !_.findWhere($scope.user.selected, {name: user.name});
                    });
                });
            });
        };

        var reloadSelectedRoles = function () {
            $scope.group.$promise.then(function() {
                $scope.role.available.$promise.then(function() {
                    // Now that we have the user roles and the available roles populate the selected and available
                    $scope.role.selected = [];
                    angular.forEach($scope.group.roles, function (role) {
                        $scope.role.selected.push({name: role, value: role});
                    });
                    // Filter the selected from the available list
                    $scope.role.available = _.filter($scope.role.available, function(role) {
                        return !_.findWhere($scope.role.selected, {name: role.name});
                    });
                });
            });
        };

        var reloadRoles = function () {
          $scope.role = {
              available: UserRolesResource.query({limit: 0, offset: 0, filter: 'role_target:USER'}),
              selected:  [],
              i18n: 'USERS.GROUPS.DETAILS.ROLES',
              searchable: true
          };
          reloadSelectedRoles();
        };

        var reloadUsers = function (current_user) {
          $scope.orgProperties = {};
          if (angular.isDefined(current_user) && angular.isDefined(current_user.org) && angular.isDefined(current_user.org.properties)) {
               $scope.orgProperties = current_user.org.properties;
          }
          $scope.user = {
              available: ResourcesListResource.query({ resource: $scope.orgProperties['adminui.user.listname'] || 'USERS.INVERSE.WITH.USERNAME'}),
              selected:  [],
              i18n: 'USERS.GROUPS.DETAILS.USERS',
              searchable: true
          };
          reloadSelectedUsers();
        };

        if ($scope.action === 'edit') {
            $scope.caption = 'USERS.GROUPS.DETAILS.EDITCAPTION';
            $scope.group = GroupResource.get({ id: $scope.resourceId }, function () {
                reloadSelectedRoles();
                reloadSelectedUsers();
            });
        } else {
            $scope.caption = 'USERS.GROUPS.DETAILS.NEWCAPTION';
        }

        $scope.submit = function () {
            $scope.group.users = [];
            $scope.group.roles = [];

            angular.forEach($scope.user.selected, function (item) {
              $scope.group.users.push(item.value);
            });

            angular.forEach($scope.role.selected, function (item) {
              $scope.group.roles.push(item.name);
            });

          if ($scope.action === 'edit') {
            GroupResource.save({ id: $scope.group.id }, $scope.group, function () {
                Notifications.add('success', 'GROUP_UPDATED');
                Modal.$scope.close();
            }, function () {
                Notifications.add('error', 'GROUP_NOT_SAVED', 'group-form');
            });
          } else {
            GroupsResources.create($scope.group, function () {
                Notifications.add('success', 'GROUP_ADDED');
                Modal.$scope.close();
            }, function (response) {
                if(response.status === 409) {
                    Notifications.add('error', 'GROUP_CONFLICT', 'group-form');
                } else {
                    Notifications.add('error', 'GROUP_NOT_SAVED', 'group-form');
                }
            });
          }
        };

        $scope.getSubmitButtonState = function () {
          return $scope.groupForm.$valid ? 'active' : 'disabled';
        };

        reloadRoles();
        AuthService.getUser().$promise.then(function(current_user) {
          reloadUsers(current_user);
        });
    }
]);
