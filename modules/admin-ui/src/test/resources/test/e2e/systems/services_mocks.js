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
            var servicesGETcount = 0,
            serviceSanitizable = JSON.stringify({
                count: 1,
                limit: 0,
                offset: 0,
                total: 1,
                results: [{
                    host: 'http:\/\/mh-allinone.localdomain',
                    queued: '0',
                    status: 'ERROR',
                    name: '•mock• ch.entwine.annotations',
                    meanQueueTime: '0',
                    running: '0',
                    meanRunTime: '0',
                    completed: '0'
                }]
            }),
            serviceRunning = JSON.stringify({
                count: 1,
                limit: 0,
                offset: 0,
                total: 1,
                results: [{
                    host: 'http:\/\/mh-allinone.localdomain',
                    queued: '0',
                    status: 'NORMAL',
                    name: '•mock• ch.entwine.annotations',
                    meanQueueTime: '0',
                    running: '0',
                    meanRunTime: '0',
                    completed: '0'
                }]
            });

            $httpBackend.whenGET(/services.json/)
            .respond(function () {
                servicesGETcount += 1;
                if (servicesGETcount % 2 === 0) {
                    return [200, serviceRunning];
                } else {
                    return [200, serviceSanitizable];
                }
            });

            $httpBackend.whenPOST('/admin-ng/services/sanitize').respond(204);

            $httpBackend.whenGET(/.*/).passThrough();
        });
    });
};
