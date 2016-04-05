angular.module('adminNg.controllers')
.controller('UserCtrl', ['$scope', 'UserRolesResource', 'UserResource', 'UsersResource', 'JsHelper', 'Notifications', 'Modal', 'underscore',
    function ($scope, UserRolesResource, UserResource, UsersResource, JsHelper, Notifications, Modal, _) {
        $scope.manageable = true;

        $scope.role = {
            available: UserRolesResource.query(),
            selected:  [],
            i18n: 'USERS.USERS.DETAILS.ROLES',
            searchable: true
        };

        if ($scope.action === 'edit') {
            $scope.caption = 'USERS.USERS.DETAILS.EDITCAPTION';
            $scope.user = UserResource.get({ username: $scope.resourceId });
            $scope.user.$promise.then(function () {
                $scope.manageable = $scope.user.manageable;
                if (!$scope.manageable) {
                    Notifications.add('warning', 'USER_NOT_MANAGEABLE', 'user-form');
                }

                $scope.role.available.$promise.then(function() {
                    // Now that we have the user roles and the available roles populate the selected and available
                    angular.forEach($scope.user.roles, function (role) {
                        $scope.role.selected.push({name: role, value: role});
                    });
                    // Filter the selected from the available list
                    $scope.role.available = _.filter($scope.role.available, function(role){ return !_.findWhere($scope.role.selected, {name: role.name}); });
                });
            });
        }
        else {
            $scope.caption = 'USERS.USERS.DETAILS.NEWCAPTION';
        }


        $scope.submit = function () {
            $scope.user.roles = [];

            angular.forEach($scope.role.selected, function (value) {
              $scope.user.roles.push(value.value);
            });

            if ($scope.action === 'edit') {
                $scope.user.$update({ username: $scope.user.username }, function () {
                    Notifications.add('success', 'USER_UPDATED');
                    Modal.$scope.close();
                }, function () {
                    Notifications.add('error', 'USER_NOT_SAVED', 'user-form');
                });
            } else {
                UsersResource.create({ }, $scope.user, function () {
                    Notifications.add('success', 'USER_ADDED');
                    Modal.$scope.close();
                }, function () {
                    Notifications.add('error', 'USER_NOT_SAVED', 'user-form');
                });
            }
        };

        // Retrieve a list of user so the form can be validated for user
        // uniqueness.
        $scope.users = [];
        UsersResource.query(function (users) {
            $scope.users = JsHelper.map(users.rows, 'username');
        });

        $scope.checkUserUniqueness = function () {
            $scope.userForm.username.$setValidity('uniqueness',
                    $scope.users.indexOf($scope.user.username) > -1 ? false:true);
        };
    }
]);
