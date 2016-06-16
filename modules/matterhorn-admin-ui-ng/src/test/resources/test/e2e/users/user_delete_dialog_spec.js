// Protractor Jasmine end-to-end (e2e) test.
//
// See https://github.com/angular/protractor/blob/master/docs/getting-started.md
//

var UsersPage = require('./users_page').UsersPage,
    page = new UsersPage();

xdescribe('user delete dialog', function () {

    beforeEach(function () {
        page.get();
    });

    it('shows the user delete button', function () {
        expect(page.deleteButton.isDisplayed()).toBe(true);
    });

    describe('opening the dialog', function () {

        beforeEach(function () {
            page.deleteButton.click();
            page.waitFor(page.modal.header);
        });

        it('displays the user delete confirmation dialog', function () {
            expect(page.modal.header.getText()).toContain('Confirm');
        });

        describe('and confirming the action', function () {

            beforeEach(function () {
                page.modal.confirmButton.click();
            });

            it('removes the dialog', function () {
                expect(page.isModalPresent()).not.toBe(true);
            });

            xit('deletes the record', function () {
            });
        });

        describe('and closing the dialog', function () {

            beforeEach(function () {
                page.modal.closeButton.click();
            });

            it('removes the dialog', function () {
                expect(page.isModalPresent()).not.toBe(true);
            });
        });
    });
});
