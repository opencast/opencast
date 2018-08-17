/**
 * Created by tkgroot on 04.10.16.
 */
'use strict';

var app = angular.module("runtimeNg", []);
app.controller("LoginCtrl", function ($scope, $location) {
    $scope.isError = false;
    $scope.username = "Username";
    $scope.password = "Password";

    if ($location.absUrl().match(/\?error$/)) {
        $scope.isError = true;
    }
});