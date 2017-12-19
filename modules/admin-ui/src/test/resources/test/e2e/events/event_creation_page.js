// Set up the Page Object
//
// See https://code.google.com/p/selenium/wiki/PageObjects
//
exports.NewEventPage = function () {
    this.get = function () {
        browser.get('#/events/events');
    };

    this.newEventButton         = element(by.css('button[data-open-modal="new-event-modal"]'));
    this.wizard = {};
    this.wizard.header          = element(by.id('add-event-modal'));
    this.wizard.activeStep      = element(by.css('nav.step-by-step a.active'));
    this.wizard.nextButton      = element(by.css('admin-ng-wizard a.submit'));

    this.metadata = {};
    this.metadata.editableCell    = element.all(by.css('.modal tr td.editable'));
    this.metadata.editableInput   = element.all(by.css('.modal tr td.editable input'));

    this.waitFor = function (element) {
        protractor.getInstance().wait(function () {
            return element.isDisplayed();
        });
    };
    this.pressEnter = function () {
        // Press both enter key variants as different webdrivers provide
        // different key symbols.
        protractor.getInstance().actions().sendKeys(protractor.Key.RETURN).perform();
        protractor.getInstance().actions().sendKeys(protractor.Key.ENTER).perform();
    };
};
