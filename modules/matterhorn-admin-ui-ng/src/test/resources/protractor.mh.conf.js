// Protractor configuration.
//
// See https://github.com/angular/protractor/blob/master/referenceConf.js
//

exports.config = {
    chromeOnly: true,
    chromeDriver: 'chromedriver',
    suites: {
        standalone: 'test/e2e/{events/events,language/change_language}_spec.js',
        write: 'test/e2e/{events/event_creation,events/series_creation}_spec.js',
    },
    baseUrl: 'http://mh-allinone.localdomain/admin-ng/',
    onPrepare: function () {
        browser.driver.manage().window().maximize();

        browser.get('#/events/events');
        element(by.css('input[name="j_username"]')).sendKeys('admin');
        element(by.css('input[name="j_password"]')).sendKeys('opencast');
        element(by.css('button#submit')).click();
    }
};
