// HTTP mocks for protractor tests.
//
// Adds mocks library located at lib/angular/angular-mocks.js into the DOM
// using jQuery.getScript. This then allows the creation of a mock module
// defining HTTP mocks using Angular's $httpBackend service.
//
// See http://docs.angularjs.org/api/ngMockE2E/service/$httpBackend.
//
// Note: Mocked requests act as interceptors and won't reach the browser.
//
// Usage example:
// ```
// var mocks = require('./mocks');
//
// describe('something with HTTP mocks', function () {
//   beforeEach(function () {
//     browser.addMockModule('httpBackendMock', mocks.httpBackendMock);
//   });
// });
// ```

exports.httpBackendMockAuthorized = function () {
    $.getScript('lib/angular/angular-mocks.js', function () {
        angular.module('httpBackendMock', ['ngMockE2E'])
        .run(function ($httpBackend) {
            var orgAdmin = JSON.stringify({
                username: 'OrgAdmin',
                roles: [
                    'ROLE_USER',
                    'ROLE_ORGANIZATION_ADMIN'
                ]
            });

            $httpBackend.whenGET('/info/me.json').respond(orgAdmin);
            $httpBackend.whenGET(/.*/).passThrough();
        });
    });
};

exports.httpBackendMockUnauthorized = function () {
    $.getScript('lib/angular/angular-mocks.js', function () {
        angular.module('httpBackendMock', ['ngMockE2E'])
        .run(function ($httpBackend) {
            var orgAdmin = JSON.stringify({
                username: 'User',
                roles: [
                ]
            });

            $httpBackend.whenGET('/info/me.json').respond(orgAdmin);
            $httpBackend.whenGET(/.*/).passThrough();
        });
    });
};
