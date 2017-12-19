exports.LoginPage = function () {
    this.get = function () {
        browser.get('login.html');
    };

    this.loginButton = element(by.id('submit'));
    this.form        = element(by.css('form'));
    this.eventsTable = element(by.css('table'));
    this.version     = element(by.css('.login-form p span.ng-binding'));

    this.fillInCredentials = function (user, password) {
        element(by.id('email')).sendKeys(user);
        element(by.id('password')).sendKeys(password);
    };
    this.checkRemember = function () {
        element(by.id('remember')).click();
    };
    this.currentUrl = function () {
        return protractor.getInstance().getCurrentUrl();
    };
};
