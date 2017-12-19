// Protractor Jasmine end-to-end (e2e) test.
//
// See https://github.com/angular/protractor/blob/master/docs/getting-started.md
//

var page  = new (require('./login_page').LoginPage)();

describe('user login', function () {

    beforeEach(function () {
        page.get();
    });

    it('greets the user', function () {
        expect(page.form.getText()).toContain('Welcome to Matterhorn');
    });

    it('shows the version string', function () {
        expect(page.version.getText()).toContain('1.5.0.MOCKED-SNAPSHOT - build 3fba397');
    });
});
