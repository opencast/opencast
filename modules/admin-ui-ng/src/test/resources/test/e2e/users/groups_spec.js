// Protractor Jasmine end-to-end (e2e) test.
//
// See https://github.com/angular/protractor/blob/master/docs/getting-started.md
//

var GroupsPage = require('./groups_page').GroupsPage,
    page = new GroupsPage();

describe('groups', function () {

    beforeEach(function () {
        page.get();
    });

    it('shows the groups table', function () {
        expect(page.table.isDisplayed()).toBe(true);
    });

    it('displays the role', function () {
        expect(page.table.getText()).toContain('ROLE_');
    });
});
