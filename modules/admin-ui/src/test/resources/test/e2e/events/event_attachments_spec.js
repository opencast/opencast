var page = new (require('./events_page').EventsPage)(),
    mocks = require('./mocks');

describe('event media', function () {
    beforeEach(function () {
        if (browser.useMocks) {
            browser.addMockModule('httpBackendMock', mocks.httpBackendMock);
        }
        page.getDetails('c3a4f68d-14d4-47e2-8981-8eb2fb300d3a');
        page.waitFor(page.modal.header);
        page.modal.attachmentsTab.click();
    });

    it('gets data from server', function () {
        expect(page.attachments.firstAttachmentTd.getText()).toEqual('cover');
    });
});
