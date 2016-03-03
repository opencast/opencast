// Protractor configuration.
//
// See https://github.com/angular/protractor/blob/master/referenceConf.js
//

exports.config = {
    chromeOnly: true,
    chromeDriver: '../../../node_modules/chromedriver/lib/chromedriver/chromedriver',
    specs: [
        'test/e2e/**/*_spec.js'
    ],
    baseUrl: 'http://localhost:9007/',
    seleniumServerJar: '../../../node_modules/selenium-server-standalone-jar/jar/selenium-server-standalone-2.50.1.jar',
    onPrepare: function () {
        browser.useMocks = true;
        browser.driver.manage().window().maximize();
    },
    allScriptsTimeout: 25000
};
