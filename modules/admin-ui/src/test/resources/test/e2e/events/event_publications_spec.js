var page = new (require('./events_page').EventsPage)(),
    mocks = require('./event_mocks');

describe('event publications', function () {

    beforeEach(function () {
        if (browser.useMocks) {
            browser.addMockModule('httpBackendMock', mocks.httpBackendMock);
        }
        page.getDetails('40518');
        page.waitFor(page.modal.header);
        page.modal.publicationsTab.click();
    });

    it('gets data from server', function () {
        expect(page.publications.publications.count()).toBe(1);
        expect(page.publications.publications.get(0).getText()).toBe('Engage');
    });
});
