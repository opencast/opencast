// Set up the Page Object
//
// See https://code.google.com/p/selenium/wiki/PageObjects
//
exports.ServersPage = function () {
    this.get = function () {
        browser.get('#/systems/servers');
    };

    this.maintenanceButton = element(by.css('input.maintenance'));
};
