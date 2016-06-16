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
            var decodeUrl = function (url) {
                return JSON.parse(decodeURIComponent(url.split('=')[1]).replace(/\+/g, ' '));
            },
            comment = [{
                id: 1,
                author: {
                    name:     'The Test Suite',
                    username: 'testsuite',
                    email:    null
                },
                creationDate: '2010-09-29T15:59:00Z',
                text:         'Existing comment',
                reason:       'EVENTS.EVENT.DETAILS.COMMENTS.REASONS.ONE',
                resolved:     false,
                replies:      [{
                    id: 3,
                    creationDate: '2014-06-17T13:38:16Z',
                    author: {
                        username: 'admin',
                        email: null,
                        name: 'Admin User'
                    },
                    text: 'My reply',
                    modificationDate: '2014-06-17T13:38:16Z'
                }]
            }],
            metadata = [{
                id: 'title',
                label: 'EVENTS.EVENTS.DETAILS.METADATA.TITLE',
                value: '•mock• Test Title',
                type: 'text',
                readOnly: false
            }, {
                id: 'uid',
                label: 'EVENTS.EVENTS.DETAILS.METADATA.UID',
                value: '78753d04-18a4-4327-9a61-b6d93816a7d2',
                type: 'text',
                readOnly: true
            }, {
                id: 'presenters',
                label: 'EVENTS.EVENTS.DETAILS.METADATA.PRESENTER',
                value: ['matt.smith', 'chuck.norris'],
                type: 'text',
                collection: 'users',
                readOnly: false
            }, {
                id: 'series',
                label: 'EVENTS.EVENTS.DETAILS.METADATA.SERIES',
                value: 'phy325',
                type: 'text',
                collection: { phy325: 'Physics325', edu123: 'Education123' },
                readOnly: false
            }, {
                id: 'recordingDate',
                label: 'EVENTS.EVENTS.DETAILS.METADATA.RECORDING_DATE',
                value: '2012-11-20',
                type: 'date',
                readOnly: false
            }, {
                id: 'contributors',
                label: 'EVENTS.EVENTS.DETAILS.METADATA.CONTRIBUTORS',
                value: ['franz.kafka', 'a.morris' ],
                type: 'text',
                collection: 'users',
                readOnly: false
            }, {
                id: 'subject',
                label: 'EVENTS.EVENTS.DETAILS.METADATA.SUBJECT',
                value: ['Test Subject', 'Second Subject'],
                type: 'text',
                readOnly: false
            }, {
                id: 'language',
                label: 'EVENTS.EVENTS.DETAILS.METADATA.LANGUAGE',
                value: 'fr',
                type: 'text',
                collection: { en: 'English', de: 'German', fr: 'French' },
                readOnly: false
            }, {
                id: 'description',
                label: 'EVENTS.EVENTS.DETAILS.METADATA.DESCRIPTION',
                value: 'A mocked event description.',
                type: 'text',
                readOnly: false
            }, {
                id: 'startDate',
                label: 'EVENTS.EVENTS.DETAILS.METADATA.START_DATE',
                value: '2012-12-01',
                type: 'date',
                readOnly: false
            }, {
                id: 'startTime',
                label: 'EVENTS.EVENTS.DETAILS.METADATA.START_TIME',
                value: '09:30',
                type: 'time',
                readOnly: false
            }, {
                id: 'duration',
                label: 'EVENTS.EVENTS.DETAILS.METADATA.DURATION',
                value: '01:05:00',
                type: 'text',
                readOnly: true
            }, {
                id: 'agent',
                label: 'EVENTS.EVENTS.DETAILS.METADATA.AGENT',
                value: 'epi001',
                type: 'text',
                collection: { ahg008: 'AHG 008', epi001: 'EPI 001' },
                readOnly: false
            }, {
                id: 'source',
                label: 'EVENTS.EVENTS.DETAILS.METADATA.SOURCE',
                value: 'Manual',
                type: 'text',
                readOnly: true
            }, {
                id: 'created',
                label: 'EVENTS.EVENTS.DETAILS.METADATA.CREATED',
                value: '2014-04-15',
                type: 'date',
                readOnly: true
            }, {
                id: 'createdBy',
                label: 'EVENTS.EVENTS.DETAILS.METADATA.CREATED_BY',
                value: 'Matt Smith',
                type: 'text',
                readOnly: true
            }, {
                id: 'license',
                label: 'EVENTS.EVENTS.DETAILS.METADATA.LICENSE',
                value: 'ccnd',
                type: 'text',
                collection: { gpl: 'GPL', ccnd: 'CCND', bsd: 'BSD' },
                readOnly: false
            }];

            $httpBackend.whenPOST('/admin-ng/event/40518/comment')
            .respond(function (method, url, data) {
                comment.push({
                    id: 2,
                    author: {
                        name:     'The Test Suite',
                        username: 'testsuite',
                        email:    null
                    },
                    creationDate: '2010-09-29T17:07:00Z',
                    text:         'Dynamically added comment',
                    reason:       'EVENTS.EVENT.DETAILS.COMMENTS.REASONS.ONE',
                    resolved:     false,
                    replies:      []
                });
                return [200, data, {}];
            });

            $httpBackend.whenPOST('/admin-ng/event/40518/comment/1/reply')
            .respond(function (method, url, data) {
                if (data.indexOf('resolved=true') > -1) {
                    comment[0].resolvedStatus = true;
                } else {
                    comment[0].resolvedStatus = false;
                }

                return [200, data, {}];
            });

            $httpBackend.whenDELETE('/admin-ng/event/40518/comment/1')
            .respond(function (method, url, data) {
                comment.splice(0, 1);
                return [200, data, {}];
            });

            $httpBackend.whenGET('/admin-ng/event/40518/comments')
            .respond(function () {
                return [200, JSON.stringify(comment), {}];
            });

            $httpBackend.whenPUT(/event\/.*\/metadata/)
            .respond(function (method, url, data) {
                var editedMetadata = angular.copy(metadata),
                    entries = decodeUrl(data);
                angular.forEach(editedMetadata, function (entry, index) {
                    if (entry.id === entries[0].id) {
                        editedMetadata[index] = entries[0];
                    }
                });
                return [200, JSON.stringify(editedMetadata), {}];
            });

            $httpBackend.whenGET(/.*/).passThrough();
        });
    });
};
