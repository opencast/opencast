exports.Page = function () {
    this.getStartPage = function () {
        browser.get('');
    };

    this.getRecordings = function () {
        browser.get('#/recordings/recordings');
    };

    this.menuButton = element(by.id('menu-toggle'));
    this.menuItems  = element.all(by.css('#nav-container a'));
    this.menu       = element(by.css('#nav-container'));
    this.recordings = element(by.css('#nav-container i.recordings'));

    this.currentUrl = function () {
        return protractor.getInstance().getCurrentUrl();
    };

    this.waitFor = function (element) {
        protractor.getInstance().wait(function () {
            return element.isDisplayed();
        });
    };
};
