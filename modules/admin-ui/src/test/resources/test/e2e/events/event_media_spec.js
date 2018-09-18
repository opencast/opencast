var page = new (require('./events_page').EventsPage)(),
    mocks = require('./event_mocks');

describe('event media', function () {

    beforeEach(function () {
        if (browser.useMocks) {
            browser.addMockModule('httpBackendMock', mocks.httpBackendMock);
        }
        page.getDetails('c3a4f68d-14d4-47e2-8981-8eb2fb300d3a');
        page.waitFor(page.modal.header);
        page.modal.mediaTab.click();
    });

    it('gets data from server', function () {
        expect(page.media.firstTd.getText()).toEqual('track-1');
    });

    it('changes content when switching records', function () {
        page.modal.next.click();
        expect(page.media.firstRow.isPresent()).toBeFalsy();
    });

    it('loads content back when navigating to an existing record', function () {
        page.modal.next.click();
        expect(page.media.firstRow.isPresent()).toBeFalsy();
        page.modal.previous.click();
        expect(page.media.firstRow.isPresent()).toBeTruthy();
    });

    describe('event media details ', function () {
        beforeEach(function () {
            page.media.detailLinks.first().click();
        });

        it('fetches media details and follows sub navigation link', function () {
            expect(page.media.detailsSubNavTab.isDisplayed()).toBeTruthy();
        });

        it('asserts that the displayed id comes from the server', function () {
            expect(element(by.css('div.modal-content.active')).getText()).toContain('370597b6-35ec-45de-9101-ccb87b873ee7');
        });
    });
});
