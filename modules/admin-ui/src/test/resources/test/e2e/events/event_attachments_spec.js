var page = new (require('./events_page').EventsPage)(),
    mocks = require('./mocks');

describe('event media', function () {
    beforeEach(function () {
        if (browser.useMocks) {
            browser.addMockModule('httpBackendMock', mocks.httpBackendMock);
        }
        page.getDetails('40518');
        page.waitFor(page.modal.header);
        page.modal.attachmentsTab.click();
    });

    it('gets data from server', function () {
        expect(page.attachments.firstAttachmentTd.getText()).toEqual('cover');
    });
});
