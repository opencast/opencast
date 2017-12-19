// Set up the Page Object
//
// See https://code.google.com/p/selenium/wiki/PageObjects
//
exports.NewLocationBlacklistPage = function () {
    this.get = function () {
        browser.get('http://localhost:9007/#/recordings/locationblacklists');
    };

    this.newBlacklistButton     = element(by.css('button[data-open-modal="location-blacklist-modal"]'));
    this.wizard                 = {};
    this.wizard.header          = element(by.id('location-blacklist-modal'));
    this.wizard.activeStep      = element(by.css('nav.step-by-step a.active'));
    this.wizard.nextButton      = element(by.css('admin-ng-wizard a.submit'));

    this.wizard.items           = {};
    this.wizard.items.select    = element(by.css('[data-modal-tab-content="items"] .chosen-single'));
    this.wizard.items.options   = element.all(by.css('[data-modal-tab-content="items"] .chosen-results li'));

    this.wizard.dates           = {};
    this.wizard.dates.fromDate  = element(by.model('wizard.step.ud.fromDate'));
    this.wizard.dates.fromTime  = element(by.model('wizard.step.ud.fromTime'));
    this.wizard.dates.toDate    = element(by.model('wizard.step.ud.toDate'));
    this.wizard.dates.toTime    = element(by.model('wizard.step.ud.toTime'));

    this.wizard.reason          = {};
    this.wizard.reason.select   = element(by.css('[data-modal-tab-content="reason"] .chosen-single'));
    this.wizard.reason.options  = element.all(by.css('[data-modal-tab-content="reason"] .chosen-results li'));

    this.tableNotification      = element(by.css('.main-view ul li div.alert'));
    this.modalNotification      = element(by.css('.modal-content ul li div.alert'));

    this.waitFor = function (element) {
        protractor.getInstance().wait(function () {
            return element.isDisplayed();
        });
    };

    this.fillIn = function () {
        this.wizard.items.select.click();
        this.waitFor(this.wizard.items.options.first());
        this.wizard.items.options.first().click();

        this.wizard.nextButton.click();

        this.wizard.dates.fromDate.sendKeys('07/03/2014');
        this.wizard.dates.fromTime.sendKeys('1000PM');
        this.wizard.dates.toDate.sendKeys('09/20/2014');
        this.wizard.dates.toTime.sendKeys('1000AM');

        this.wizard.nextButton.click();

        this.wizard.reason.select.click();
        this.waitFor(this.wizard.reason.options.first());
        this.wizard.reason.options.first().click();

        this.wizard.nextButton.click();
    };
};
