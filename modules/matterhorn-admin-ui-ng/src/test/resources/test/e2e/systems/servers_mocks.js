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

exports.httpBackendMock = function () {
    $.getScript('lib/angular/angular-mocks.js', function () {
        angular.module('httpBackendMock', ['ngMockE2E'])
        .run(function ($httpBackend) {
            var serversGETcount = 0,
            serverMaintenance = JSON.stringify({
                count: 1,
                limit: 0,
                offset: 0,
                total: 1,
                results: [{
                    completed: 2,
                    cores: 2,
                    maintenance: true,
                    meanQueueTime: 4,
                    meanRunTime: 200,
                    name: '•mock• host3',
                    online: true,
                    queued: 4,
                    running: 2
                }]
            }),
            serverRunning = JSON.stringify({
                count: 1,
                limit: 0,
                offset: 0,
                total: 1,
                results: [{
                    completed: 2,
                    cores: 2,
                    maintenance: false,
                    meanQueueTime: 4,
                    meanRunTime: 200,
                    name: '•mock• host3',
                    online: true,
                    queued: 4,
                    running: 2
                }]
            });

            // Allow toggling of the maintenance button
            $httpBackend.whenGET(/servers.json/)
            .respond(function () {
                serversGETcount += 1;
                if (serversGETcount % 2 === 0) {
                    return [200, serverMaintenance];
                } else {
                    return [200, serverRunning];
                }
            });

            $httpBackend.whenPOST('/admin-ng/services/maintenance.json').respond(204);

            $httpBackend.whenGET(/.*/).passThrough();
        });
    });
};
