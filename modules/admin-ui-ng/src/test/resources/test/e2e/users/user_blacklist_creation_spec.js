var page = new (require('./user_blacklist_creation_page').NewUserBlacklistPage)(),
    mocks = require('./blacklist_mocks');

describe('user blacklist creation', function () {

    describe('creating user blacklist', function () {

        describe('on success', function () {
            beforeEach(function () {
                if (browser.useMocks) {
                    browser.addMockModule('httpBackendMock', mocks.BlacklistCreationSuccessMock);
                }
                page.get();
                page.newBlacklistButton.click();
                page.waitFor(page.wizard.header);
                page.fillIn();
            });

            it('redirects to the index and displays a notification', function () {
                page.wizard.nextButton.click();
                expect(page.tableNotification.getAttribute('class')).toContain('success');
                expect(page.tableNotification.getText()).toContain('created successfully');

                page.newBlacklistButton.click();
                page.waitFor(page.wizard.header);
                expect(page.wizard.header.getText()).not.toContain('•mock• PM User 1');
            });
        });

        describe('on error', function () {
            beforeEach(function () {
                browser.clearMockModules();
                if (browser.useMocks) {
                    browser.addMockModule('httpBackendMock', mocks.BlacklistCreationErrorMock);
                }
                page.get();
                page.newBlacklistButton.click();
                page.waitFor(page.wizard.header);
                page.fillIn();
            });

            it('keeps the modal open and displays a notification', function () {
                page.wizard.nextButton.click();
                expect(page.modalNotification.getAttribute('class')).toContain('error');
                expect(page.modalNotification.getText()).toContain('could not be created');
            });
        });
    });
});
