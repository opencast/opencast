angular.module('adminNg.controllers')
.controller('ThemeFormCtrl', ['$scope', '$timeout', 'FormNavigatorService', 'Notifications', 'ThemeResource', 'NewThemeResource', 'ThemeUsageResource', 'Table',
    function ($scope, $timeout, FormNavigatorService, Notifications, ThemeResource, NewThemeResource, ThemeUsageResource, Table) {
        var action = $scope.$parent.action;

        $scope.currentForm = 'generalForm';

        $scope.navigateTo = function (targetForm, currentForm, requiredForms) {
            // We have to set the currentForm property here in the controller.
            // The reason for that is that the footer sections in the partial are decorated with ng-if, which
            // creates a new scope each time they are activated. 
            $scope.currentForm = FormNavigatorService.navigateTo(targetForm, currentForm, requiredForms);
        };

        $scope.cancel = function () {
            $scope.close();
        };

        $scope.valid = function () {
            if (angular.isDefined($scope['theme-form'])) {
                return $scope['theme-form'].$valid;
            }
            return false;
        };

        if (action === 'add') {
            // Lets set some defaults first
            $scope.general = {
                'default': false
            };

            $scope.bumper = {
                'active': false
            };

            $scope.trailer = {
                'active': false
            };

            $scope.license = {
                'active': false
            };

            $scope.watermark = {
                'active': false,
                position: 'topRight'
            };

            $scope.titleslide = {
                'mode':'extract'
            };
        }

        if (action === 'edit') {
            // load resource
            ThemeResource.get({id: $scope.resourceId, format: '.json'}, function (response) {
                angular.forEach(response, function (obj, name) {
                    $scope[name] = obj;
                });
                $scope.themeLoaded = true;
            });


            $scope.usage = ThemeUsageResource.get({themeId:$scope.resourceId});

        }

        $scope.submit = function () {
            var messageId, userdata = {}, success, failure;
            success = function () {
                Notifications.add('success', 'THEME_CREATED');
                Notifications.remove(messageId);
                $timeout(function () {
                    Table.fetch();
                }, 1000);
            };

            failure = function () {
                Notifications.add('error', 'THEME_NOT_CREATED');
                Notifications.remove(messageId);
            };

            // add message that never disappears
            messageId = Notifications.add('success', 'THEME_UPLOAD_STARTED', 'global', -1);
            userdata = {
                general: $scope.general,
                bumper: $scope.bumper,
                trailer: $scope.trailer,
                license: $scope.license,
                titleslide: $scope.titleslide,
                watermark: $scope.watermark
            };
            if (action === 'add') {
                NewThemeResource.save({}, userdata, success, failure);
            }
            if (action === 'edit') {
                ThemeResource.update({id: $scope.resourceId}, userdata, success, failure);
            }
            // close will not fetch content yet....
            $scope.close(false);
        };

    }]);
    
