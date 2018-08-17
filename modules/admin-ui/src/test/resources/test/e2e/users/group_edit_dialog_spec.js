// Protractor Jasmine end-to-end (e2e) test.
//
// See https://github.com/angular/protractor/blob/master/docs/getting-started.md
//

var GroupsPage = require('./groups_page').GroupsPage,
    page = new GroupsPage();

xdescribe('group edit dialog', function () {

    beforeEach(function () {
        page.get();
    });

    it('shows the group edit button', function () {
        expect(page.editButton.isDisplayed()).toBe(true);
    });

    describe('opening the dialog', function () {

        beforeEach(function () {
            page.editButton.click();
            page.waitFor(page.modal.header);
        });

        it('displays the group edit dialog', function () {
            expect(page.modal.header.getText()).toContain('Edit Group');
            expect(page.modal.description.getAttribute('value')).toContain('The IT Team');
        });

        it('changes the URL', function () {
            expect(page.currentUrl()).toContain('it_team');
        });

        describe('and closing the dialog', function () {

            beforeEach(function () {
                page.modal.closeButton.click();
            });

            it('removes the dialog', function () {
                expect(page.isModalPresent()).not.toBe(true);
            });

            it('changes the URL', function () {
                expect(page.currentUrl()).not.toContain('test_group');
            });
        });
    });

    describe('direct access via URL', function () {

        beforeEach(function () {
            page.getEditDialog('it_team');
            page.waitFor(page.modal.header);
        });

        it('displays the group edit dialog', function () {
            expect(page.modal.header.getText()).toContain('Edit Group');
            expect(page.modal.description.getAttribute('value')).toContain('The IT Team');
        });
    });
});
