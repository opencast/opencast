// Set up the Page Object
//
// See https://code.google.com/p/selenium/wiki/PageObjects
//
exports.GroupsPage = function () {
    this.get = function () {
        browser.get('#/users/groups');
    };
    this.getEditDialog = function (id) {
        browser.get('#/users/groups/?modal=group-modal&action=edit&resourceId=' + id);
    };

    this.table      = element(by.css('table'));
    this.editButton = element(by.css('table a.more'));
    this.addButton  = element(by.css('button.add[data-open-modal="group-modal"]'));

    this.modal = {};
    this.modal.header      = element(by.css('section.modal header'));
    this.modal.content     = element(by.css('.modal-content'));
    this.modal.closeButton = element(by.css('a.close-modal'));
    this.modal.previous    = element(by.css('i.fa-angle-left'));
    this.modal.next        = element(by.css('i.fa-angle-right'));

    this.modal.rolesTabButton   = element(by.linkText('Roles'));
    this.modal.usersTabButton   = element(by.linkText('Users'));
    this.modal.availableRoles   = element(by.css('[resource="role"] select.available'));
    this.modal.selectedRoles    = element(by.css('[resource="role"] select.selected'));
    this.modal.availableUsers   = element(by.css('[resource="user"] select.available'));
    this.modal.selectedUsers    = element(by.css('[resource="user"] select.selected'));
    this.modal.availableGroups  = element.all(by.css('[resource="user"] select.available optgroup'));
    this.modal.selectedGroups   = element.all(by.css('[resource="user"] select.selected optgroup'));
    this.modal.addRoleButton    = element(by.linkText('Add Role'));
    this.modal.removeRoleButton = element(by.linkText('Remove Role'));
    this.modal.addUserButton    = element(by.linkText('Add User'));
    this.modal.removeUserButton = element(by.linkText('Remove User'));
    this.modal.description      = element(by.model('group.description'));

    this.optionByLabel = function (label) {
        return element(by.xpath('//select//option[text()="' + label + '"]'));
    };
    this.searchUsers = function (term) {
        element(by.css('[resource="user"] input#search')).sendKeys(term);
    };
    this.currentUrl = function () {
        return protractor.getInstance().getCurrentUrl();
    };
    this.isModalPresent = function () {
        return protractor.getInstance().isElementPresent(by.css('section.modal.active'));
    };
    this.waitFor = function (element) {
        protractor.getInstance().wait(function () {
            return element.isDisplayed();
        });
    };
};
