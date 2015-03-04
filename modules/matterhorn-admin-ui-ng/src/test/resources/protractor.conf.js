// Protractor configuration.
//
// See https://github.com/angular/protractor/blob/master/referenceConf.js
//

exports.config = {
    chromeOnly: true,
    chromeDriver: 'chromedriver',
    specs: [
        'test/e2e/**/*_spec.js',
    ],
    baseUrl: 'http://localhost:9007/',
    onPrepare: function () {
        browser.useMocks = true;
        browser.driver.manage().window().maximize();
    }
};
