// Set up the Page Object
//
// See https://code.google.com/p/selenium/wiki/PageObjects
//
exports.NewSeriesPage = function () {
    this.get = function () {
        browser.get('#/events/series');
    };

    this.newEventButton         = element(by.css('button[data-open-modal="new-series-modal"]'));
    this.wizard                 = {};
    this.wizard.header          = element(by.id('add-series-modal'));
    this.wizard.activeStep      = element(by.css('nav.step-by-step a.active'));
    this.wizard.nextButton      = element(by.css('admin-ng-wizard a.submit'));

    this.metadata               = {};
    this.metadata.editableCell  = element.all(by.css('.modal tr td.editable'));
    this.metadata.editableInput = element.all(by.css('.modal tr td.editable input'));

    this.access                 = {};
    this.access.select          = element(by.css('.chosen-single'));
    this.access.acl             = element.all(by.css('.chosen-results li'));

    this.summary                = {};
    this.summary.tables         = {};
    this.summary.tables.columns = element.all(by.css('.modal-content[data-modal-tab-content="summary"] .tbl-list .main-tbl td'));
    this.summary.notification   = element(by.css('.alert'));

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
    this.chooseACL = function (position) {
        this.access.select.click();
        this.waitFor(this.access.acl.get(position));
        this.access.acl.get(position).click();
    };
};
