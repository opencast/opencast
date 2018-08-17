var page = new (require('./events_page').EventsPage)(),
    mocks = require('./event_mocks');

describe('event general', function () {

    beforeEach(function () {
        if (browser.useMocks) {
            browser.addMockModule('httpBackendMock', mocks.httpBackendMock);
        }
        page.getDetails('40518');
        page.waitFor(page.modal.header);
        page.modal.generalTab.click();
    });

    it('gets data from server', function () {
        expect(page.general.publications.count()).toBe(1);
        expect(page.general.publications.get(0).getText()).toBe('Engage');
    });
});
