/**
 * Copyright 2009-2013 The Regents of the University of California
 * Licensed under the Educational Community License, Version 2.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */

'use strict';

describe('Login Controller', function () {
    var $scope, $location, $httpBackend, $controller, Language;

    beforeEach(module('adminNg'));
    beforeEach(module('adminNg.controllers'));

    beforeEach(inject(function ($rootScope, _$controller_, _$location_, _$httpBackend_, _Language_) {
        Language = _Language_;
        $scope = $rootScope.$new();
        $location = _$location_;
        $httpBackend = _$httpBackend_;
        $controller = _$controller_;
    }));

    beforeEach(function () {
        $httpBackend.whenGET('/i18n/languages.json')
            .respond('{"fallbackLanguage":{"code":"en_US"},"bestLanguage":{"code":"en_US"}}');
        $httpBackend.whenGET('public/org/opencastproject/adminui/languages/lang-en_US.json')
            .respond('');
    });

    describe('when authentication failed', function () {

        beforeEach(function () {
            $location.url('/login?error');
        });

        it('should set the error flag', function () {
            $controller('LoginCtrl', {$scope: $scope});
            expect($scope.isError).toBeTruthy();
        });
    });

    describe('when authentication was successful', function () {

        it('should unset the error flag', function () {
            $controller('LoginCtrl', {$scope: $scope});
            expect($scope.isError).toBeFalsy();
        });

        it('should fetch version information', function () {
            $httpBackend.expectGET('/sysinfo/bundles/version?prefix=matterhorn')
                .respond('');
            $controller('LoginCtrl', {$scope: $scope});
            $httpBackend.flush();
        });

        it('sets the version string', function () {
            $httpBackend.whenGET('/sysinfo/bundles/version?prefix=matterhorn')
                .respond('{"consistent":true,"version":"1.5.0.MOCKED-SNAPSHOT","buildNumber":"3fba397"}');
            $controller('LoginCtrl', {$scope: $scope});
            $httpBackend.flush();
            expect($scope.version.version).toEqual('1.5.0.MOCKED-SNAPSHOT');
        });
    });

    describe('#toLanguageClass', function () {
        beforeEach(function () {
            $controller('LoginCtrl', {$scope: $scope});
        });

        it('wraps Language.$convertLanguageToCode', function () {
            spyOn(Language, '$convertLanguageToCode');
            $scope.toLanguageClass({ code: 'de_DE' });
            expect(Language.$convertLanguageToCode).toHaveBeenCalledWith('de_DE');
        });
    });

    describe('#changeLanguage', function () {
        beforeEach(function () {
            $controller('LoginCtrl', {$scope: $scope});
        });

        it('wraps Language.changeLanguage', function () {
            spyOn(Language, 'changeLanguage');
            $scope.changeLanguage('fr_FR');
            expect(Language.changeLanguage).toHaveBeenCalledWith('fr_FR');
        });
    });
});
