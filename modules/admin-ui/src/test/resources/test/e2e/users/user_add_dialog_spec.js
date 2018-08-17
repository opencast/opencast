// Protractor Jasmine end-to-end (e2e) test.
//
// See https://github.com/angular/protractor/blob/master/docs/getting-started.md
//

var page  = new (require('./users_page').UsersPage)(),
    mocks = require('./user_mocks');

xdescribe('user add dialog', function () {

    beforeEach(function () {
        if (browser.useMocks) {
            browser.addMockModule('httpBackendMock', mocks.httpBackendMock);
        }
        page.get();
    });

    it('shows the add user button', function () {
        expect(page.addButton.isDisplayed()).toBeTruthy();
    });

    describe('opening the dialog', function () {

        beforeEach(function () {
            page.addButton.click();
            page.waitFor(page.modal.header);
        });

        it('displays the user form by default', function () {
            expect(page.modal.header.getText()).toContain('Create New User');
        });

        describe('in the role tab', function () {
            beforeEach(function () {
                page.modal.roleTabButton.click();
            });

            it('displays the roles form in another tab', function () {
                expect(page.modal.availableRoles.getText()).toContain('ROLE_ADMIN');
                expect(page.modal.selectedRoles.getText()).not.toContain('ROLE_ADMIN');
            });
        });

        describe('in the user tab', function () {
            beforeEach(function () {
                page.modal.username.sendKeys('first.last');
                page.modal.name.sendKeys('First Last');
                page.modal.email.sendKeys('email@mail.com');
                page.modal.password.sendKeys('pw');
                page.modal.repeatedPassword.sendKeys('pw');
            });

            it('does not allow to send the form unless everything is filled in', function () {
                page.modal.name.clear();
                expect(page.modal.submitButton.getAttribute('class')).toContain('disabled');
                expect(page.modal.submitButton.getAttribute('class')).not.toContain('active');
                expect(page.modal.name.getAttribute('class')).toContain('error');
            });

            it('complains if the passwords do not match', function () {
                page.modal.repeatedPassword.sendKeys('something');
                expect(page.modal.submitButton.getAttribute('class')).toContain('disabled');
                expect(page.modal.submitButton.getAttribute('class')).not.toContain('active');
                expect(page.modal.repeatedPassword.getAttribute('class')).toContain('error');
            });

            it('becomes submittable as soon as all fields are present', function () {
                expect(page.modal.submitButton.getAttribute('class')).toContain('active');
            });

            describe('on submission error', function () {
                beforeEach(function () {
                    page.modal.username.clear();
                    page.modal.username.sendKeys('taken.name');
                });

                it('displays an error alert', function () {
                    expect(page.modal.errorAlert.isPresent()).toBe(false);
                    page.modal.submitButton.click();
                    expect(page.modal.errorAlert.isDisplayed()).toBe(true);
                });
            });

            describe('on submission success', function () {

                it('closes the modal and displays an alert', function () {
                    expect(page.successAlert.isPresent()).toBe(false);
                    page.modal.submitButton.click();
                    expect(page.successAlert.isDisplayed()).toBe(true);
                });
            });
        });
    });
});
