// Protractor Jasmine end-to-end (e2e) test.
//
// See https://github.com/angular/protractor/blob/master/docs/getting-started.md
//

var GroupsPage = require('./groups_page').GroupsPage,
    page = new GroupsPage();

xdescribe('group add dialog', function () {

    beforeEach(function () {
        page.get();
    });

    it('shows the add group button', function () {
        expect(page.addButton.isDisplayed()).toBeTruthy();
    });

    describe('opening the dialog', function () {

        beforeEach(function () {
            page.addButton.click();
            page.waitFor(page.modal.header);
        });

        it('displays the group form by default', function () {
            expect(page.modal.header.getText()).toContain('Create New Group');
        });

        describe('in the roles tab', function () {
            var availableRoles, selectedRoles, getRoles;
            getRoles = function () {
                availableRoles = page.modal.availableRoles.getText(),
                selectedRoles  = page.modal.selectedRoles.getText();
            };

            beforeEach(function () {
                page.modal.rolesTabButton.click();
                getRoles();
            });

            it('displays role assignment', function () {
                expect(availableRoles).toContain('•mock• ROLE_ADMIN');
                expect(availableRoles).toContain('•mock• ROLE_OAUTH_USER');
                expect(availableRoles).toContain('•mock• ROLE_USER');

                expect(selectedRoles).not.toContain('•mock• ROLE_ADMIN');
                expect(selectedRoles).not.toContain('•mock• ROLE_OAUTH_USER');
                expect(selectedRoles).not.toContain('•mock• ROLE_USER');
            });

            it('disables buttons by default', function () {
                expect(page.modal.addRoleButton.getAttribute('class')).toContain('disabled');
                expect(page.modal.removeRoleButton.getAttribute('class')).toContain('disabled');
            });

            describe('and adding roles', function () {

                beforeEach(function () {
                    page.optionByLabel('•mock• ROLE_ADMIN').click();
                    expect(page.modal.addRoleButton.getAttribute('class'))
                    .not.toContain('disabled');
                    page.modal.addRoleButton.click();

                    page.optionByLabel('•mock• ROLE_OAUTH_USER').click();
                    expect(page.modal.addRoleButton.getAttribute('class'))
                    .not.toContain('disabled');
                    page.modal.addRoleButton.click();
                    getRoles();
                });

                it('moves roles to the selected roles list', function () {
                    expect(availableRoles).not.toContain('•mock• ROLE_ADMIN');
                    expect(availableRoles).not.toContain('•mock• ROLE_OAUTH_USER');
                    expect(availableRoles).toContain('•mock• ROLE_USER');

                    expect(selectedRoles).toContain('•mock• ROLE_ADMIN');
                    expect(selectedRoles).toContain('•mock• ROLE_OAUTH_USER');
                    expect(selectedRoles).not.toContain('•mock• ROLE_USER');
                });
            });

            describe('and removing roles', function () {

                beforeEach(function () {
                    page.optionByLabel('•mock• ROLE_ADMIN').click();
                    page.modal.addRoleButton.click();
                    page.optionByLabel('•mock• ROLE_OAUTH_USER').click();
                    page.modal.addRoleButton.click();

                    page.optionByLabel('•mock• ROLE_OAUTH_USER').click();
                    expect(page.modal.removeRoleButton.getAttribute('class'))
                    .not.toContain('disabled');

                    page.modal.removeRoleButton.click();
                });

                it('moves roles to the selected roles list', function () {
                    var availableRoles = page.modal.availableRoles.getText(),
                    selectedRoles  = page.modal.selectedRoles.getText();

                    expect(availableRoles).not.toContain('•mock• ROLE_ADMIN');
                    expect(availableRoles).toContain('•mock• ROLE_OAUTH_USER');
                    expect(availableRoles).toContain('•mock• ROLE_USER');

                    expect(selectedRoles).toContain('•mock• ROLE_ADMIN');
                    expect(selectedRoles).not.toContain('•mock• ROLE_OAUTH_USER');
                    expect(selectedRoles).not.toContain('•mock• ROLE_USER');
                });
            });
        });

        describe('in the users tab', function () {
            var availableUsers, selectedUsers, getUsers;
            getUsers = function () {
                availableUsers = page.modal.availableUsers.getText(),
                selectedUsers  = page.modal.selectedUsers.getText();
            };

            beforeEach(function () {
                page.modal.usersTabButton.click();
                getUsers();
            });

            it('displays user assignment', function () {
                expect(page.modal.addUserButton.isDisplayed()).toBeTruthy();
                expect(page.modal.removeUserButton.isDisplayed()).toBeTruthy();

                expect(availableUsers).toContain('•mock• MH System Account');
                expect(availableUsers).toContain('•mock• SystemAdmin');
                expect(availableUsers).toContain('•mock• Xavier Butty');
            });

            it('groups users', function () {
                expect(page.modal.availableGroups.get(0).getAttribute('label')).toContain('system');
                expect(page.modal.availableGroups.get(1).getAttribute('label')).toContain('local');
            });

            describe('searching users', function () {

                beforeEach(function () {
                    page.searchUsers('Acco');
                    getUsers();
                });

                it('filters available users', function () {
                    expect(availableUsers).toContain('•mock• MH System Account');
                    expect(availableUsers).not.toContain('•mock• Xavier Butty');
                    expect(availableUsers).not.toContain('•mock• SystemAdmin');
                });

                it('groups users', function () {
                    expect(page.modal.availableGroups.get(0).getAttribute('label')).toContain('system');
                });
            });

            describe('and adding users', function () {

                beforeEach(function () {
                    page.optionByLabel('•mock• MH System Account').click();
                    expect(page.modal.addUserButton.getAttribute('class'))
                    .not.toContain('disabled');
                    page.modal.addUserButton.click();

                    page.optionByLabel('•mock• Xavier Butty').click();
                    expect(page.modal.addUserButton.getAttribute('class'))
                    .not.toContain('disabled');
                    page.modal.addUserButton.click();

                    getUsers();
                });

                it('moves users to the selected users list', function () {
                    expect(availableUsers).not.toContain('•mock• MH System Account');
                    expect(availableUsers).not.toContain('•mock• Xavier Butty');
                    expect(availableUsers).toContain('•mock• SystemAdmin');

                    expect(selectedUsers).toContain('•mock• MH System Account');
                    expect(selectedUsers).toContain('•mock• Xavier Butty');
                    expect(selectedUsers).not.toContain('•mock• SystemAdmin');
                });

                it('groups users', function () {
                    expect(page.modal.availableGroups.get(0).getAttribute('label')).toContain('local');
                    expect(page.modal.selectedGroups.get(0).getAttribute('label')).toContain('system');
                });
            });
        });
    });
});
