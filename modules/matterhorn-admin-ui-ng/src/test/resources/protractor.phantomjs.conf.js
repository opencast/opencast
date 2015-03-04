// Protractor configuration.
//
// See https://github.com/angular/protractor/blob/master/referenceConf.js
//

exports.config = {
    capabilities: {
        browserName: 'phantomjs',
        'phantomjs.binary.path': './node_modules/phantomjs/bin/phantomjs'
    },
    baseUrl: 'http://localhost:9007/',
    seleniumServerJar: 'selenium-server-standalone.jar',
    specs: [
        'test/e2e/**/*_spec.js',
    ],
    onPrepare: function () {
        browser.driver.manage().window().setSize(1280, 1024);
    }
};
