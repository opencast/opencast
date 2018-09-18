var page = new (require('./events_page').EventsPage)(),
    mocks = require('./event_mocks');

describe('event publications', function () {

    beforeEach(function () {
        if (browser.useMocks) {
            browser.addMockModule('httpBackendMock', mocks.httpBackendMock);
        }
        page.getDetails('c3a4f68d-14d4-47e2-8981-8eb2fb300d3a');
        page.waitFor(page.modal.header);
        page.modal.publicationsTab.click();
    });

    it('gets data from server', function () {
        expect(page.publications.publications.count()).toBe(1);
        expect(page.publications.publications.get(0).getText()).toBe('Engage');
    });
});
