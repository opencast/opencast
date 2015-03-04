// Protractor Jasmine end-to-end (e2e) test.
//
// See https://github.com/angular/protractor/blob/master/docs/getting-started.md
//

var UsersPage = require('./users_page').UsersPage;
var page = new UsersPage();

describe('users', function () {

    beforeEach(function () {
        page.get();
    });

    it('shows the users table', function () {
        expect(page.table.isDisplayed()).toBe(true);
    });

    it('displays roles', function () {
        expect(page.table.getText()).toContain('ROLE_');
    });
});
