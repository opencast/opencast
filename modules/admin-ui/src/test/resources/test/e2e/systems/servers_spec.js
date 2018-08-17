// Protractor Jasmine end-to-end (e2e) test.
//
// See https://github.com/angular/protractor/blob/master/docs/getting-started.md
//

var page  = new (require('./servers_page').ServersPage)(),
    mocks = require('./servers_mocks');

describe('servers', function () {

    beforeEach(function () {
        if (browser.useMocks) {
            browser.addMockModule('httpBackendMock', mocks.httpBackendMock);
        }
        page.get();
    });

    describe('entering maintenance mode', function () {
        it('toggles the maintenance state', function () {
            expect(page.maintenanceButton.getAttribute('checked')).toBeFalsy();

            page.maintenanceButton.click();

            expect(page.maintenanceButton.getAttribute('checked')).toBeTruthy();

            page.maintenanceButton.click();

            expect(page.maintenanceButton.getAttribute('checked')).toBeFalsy();
        });
    });
});
