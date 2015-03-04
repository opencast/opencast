angular.module('adminNg.controllers')
.controller('GroupCtrl', ['$scope', 'UserRolesResource', 'ResourcesListResource', 'GroupResource', 'GroupsResource', 'Notifications', 'Modal',
    function ($scope, UserRolesResource, ResourcesListResource, GroupResource, GroupsResources, Notifications, Modal) {

        $scope.role = {
            available: ResourcesListResource.query({ resource: 'ROLES'}),
            selected:  [],
            i18n: 'USERS.GROUPS.DETAILS.ROLES',
            searchable: true
        };

        $scope.user = {
            available: ResourcesListResource.query({ resource: 'USERS.INVERSE'}),
            selected:  [],
            i18n: 'USERS.GROUPS.DETAILS.USERS',
            searchable: true
        };

        if ($scope.action === 'edit') {
            $scope.caption = 'USERS.GROUPS.DETAILS.EDITCAPTION';
            $scope.group = GroupResource.get({ id: $scope.resourceId }, function (group) {

              $scope.role.available.$promise.then(function() {
                  // Now that we have the user roles and the available roles populate the selected and available
                  angular.forEach(group.roles, function (role) {
                      $scope.role.selected.push({name: role, value: role});
                  });
                  // Filter the selected from the available list
                  $scope.role.available = _.filter($scope.role.available, function(role){ return !_.findWhere($scope.role.selected, {name: role.name}); });
              });

              $scope.user.available.$promise.then(function() {
                  // Now that we have the user users and the available users populate the selected and available
                  angular.forEach(group.users, function (user) {
                      $scope.user.selected.push({name: user.name, value: user.username});
                  });
                  // Filter the selected from the available list
                  $scope.user.available = _.filter($scope.user.available, function(user) { 
                      return !_.findWhere($scope.user.selected, {name: user.name}); 
                  });
              });
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
              $scope.group.roles.push(item.value);
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
            }, function () {
                Notifications.add('error', 'GROUP_NOT_SAVED', 'group-form');
            });
          }
        };
    }
]);
