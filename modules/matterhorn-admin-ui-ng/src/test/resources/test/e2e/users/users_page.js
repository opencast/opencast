// Set up the Page Object
//
// See https://code.google.com/p/selenium/wiki/PageObjects
//
exports.UsersPage = function () {
    this.get = function () {
        browser.get('#/users/users');
    };

    this.table        = element(by.css('table'));
    this.deleteButton = element(by.css('table a.remove'));
    this.addButton    = element(by.css('button.add[data-open-modal="user-modal"]'));
    this.successAlert = element(by.css('li div.alert.success'));

    this.modal = {};
    this.modal.header           = element(by.css('section.modal header'));
    this.modal.content          = element(by.css('section.modal'));
    this.modal.closeButton      = element(by.css('a.close-modal'));
    this.modal.confirmButton    = element(by.css('a.confirm'));
    this.modal.roleTabButton    = element(by.linkText('Roles'));
    this.modal.name             = element(by.model('user.name'));
    this.modal.username         = element(by.model('user.username'));
    this.modal.email            = element(by.model('user.email'));
    this.modal.password         = element(by.model('user.password'));
    this.modal.repeatedPassword = element(by.model('user.repeatedPassword'));
    this.modal.submitButton     = element(by.css('a.submit'));
    this.modal.availableRoles   = element(by.model('markedForAddition'));
    this.modal.selectedRoles    = element(by.model('markedForRemoval'));
    this.modal.errorAlert       = element(by.css('#user-modal div.alert.error'));
    this.modal.successAlert     = element(by.css('#user-modal div.alert.success'));

    this.isModalPresent = function () {
        return protractor.getInstance().isElementPresent(by.css('section.modal.active'));
    };
    this.waitFor = function (element) {
        protractor.getInstance().wait(function () {
            return element.isDisplayed();
        });
    };
};
