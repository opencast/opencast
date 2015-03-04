// Set up the Page Object
//
// See https://code.google.com/p/selenium/wiki/PageObjects
//
exports.ServicesPage = function () {
    this.get = function () {
        browser.get('#/systems/services');
    };

    this.sanitizeButton = element(by.css('a.sanitize'));
};
