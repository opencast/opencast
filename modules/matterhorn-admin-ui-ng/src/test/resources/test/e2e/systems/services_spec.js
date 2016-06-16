// Protractor Jasmine end-to-end (e2e) test.
//
// See https://github.com/angular/protractor/blob/master/docs/getting-started.md
//

var page  = new (require('./services_page').ServicesPage)(),
    mocks = require('./services_mocks');

describe('services', function () {

    beforeEach(function () {
        if (browser.useMocks) {
            browser.addMockModule('httpBackendMock', mocks.httpBackendMock);
        }
        page.get();
    });

    describe('sanitizing a service', function () {
        it('updates the service', function () {
            expect(page.sanitizeButton.getAttribute('class')).toContain('fa-undo');
            expect(page.sanitizeButton.isPresent()).toBe(true);

            page.sanitizeButton.click();

            protractor.getInstance().sleep(1000);

            expect(page.sanitizeButton.isPresent()).toBe(false);
        });
    });
});
