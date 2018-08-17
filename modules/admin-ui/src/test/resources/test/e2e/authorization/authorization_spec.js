// Protractor Jasmine end-to-end (e2e) test.
//
// See https://github.com/angular/protractor/blob/master/docs/getting-started.md
//

var page  = new (require('./page').Page)(),
    mocks = require('./mocks');

describe('authorization', function () {

    describe('opening the menu', function () {

        describe('as an authorized user', function () {
            beforeEach(function () {
                if (browser.useMocks) {
                    browser.addMockModule('httpBackendMock', mocks.httpBackendMockAuthorized);
                }
                page.getStartPage();
                page.menuButton.click();
                page.waitFor(page.menuItems.first());
            });

            it('grants the user access to recordings', function () {
                expect(page.recordings.isPresent()).toBe(true);
            });
        });

        describe('as an unauthorized user', function () {
            beforeEach(function () {
                if (browser.useMocks) {
                    browser.addMockModule('httpBackendMock', mocks.httpBackendMockUnauthorized);
                }
                page.getStartPage();
                page.menuButton.click();
                page.waitFor(page.menuItems.first());
            });

            it('denies the user access to recordings', function () {
                expect(page.recordings.isPresent()).toBe(false);
            });
        });
    });

    describe('navigating to the recordings page', function () {

        describe('as an authorized user', function () {
            beforeEach(function () {
                if (browser.useMocks) {
                    browser.addMockModule('httpBackendMock', mocks.httpBackendMockAuthorized);
                }
                page.getStartPage();
                page.getRecordings();
            });

            it('grants the user access to recordings page', function () {
                expect(page.currentUrl()).not.toContain('events/events');
            });
        });

        describe('as an unauthorized user', function () {
            beforeEach(function () {
                if (browser.useMocks) {
                    browser.addMockModule('httpBackendMock', mocks.httpBackendMockUnauthorized);
                }
                page.getRecordings();
            });

            it('redirects to the start page', function () {
                expect(page.currentUrl()).toContain('events/events');
            });
        });
    });

    describe('loading the recordings page', function () {

        describe('as an authorized user', function () {
            beforeEach(function () {
                if (browser.useMocks) {
                    browser.addMockModule('httpBackendMock', mocks.httpBackendMockAuthorized);
                }
                page.getRecordings();
            });

            it('grants the user access to recordings page', function () {
                expect(page.currentUrl()).not.toContain('events/events');
            });
        });

        describe('as an unauthorized user', function () {
            beforeEach(function () {
                if (browser.useMocks) {
                    browser.addMockModule('httpBackendMock', mocks.httpBackendMockUnauthorized);
                }
                page.getRecordings();
            });

            it('redirects to the start page', function () {
                expect(page.currentUrl()).toContain('events/events');
            });
        });
    });
});
