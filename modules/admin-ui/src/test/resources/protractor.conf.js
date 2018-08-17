// Protractor configuration.
//
// See https://github.com/angular/protractor/blob/master/referenceConf.js
//
var Jasmine2HtmlReporter = require('protractor-jasmine2-html-reporter');
var jasmineReporters = require('jasmine-reporters');

exports.config = {
    chromeOnly: true,
    chromeDriver: '../../../node_modules/chromedriver/lib/chromedriver/chromedriver',
    framework: 'jasmine2',
    specs: [
        'test/e2e/**/*_spec.js'
    ],
    baseUrl: 'http://localhost:9007/',
    seleniumServerJar: '../../../node_modules/selenium-server-standalone-jar/jar/selenium-server-standalone-2.51.0.jar',
    onPrepare: function () {
        browser.useMocks = true;
        browser.driver.manage().window().maximize();

        // returning the promise makes protractor wait for the reporter config before executing tests
        return browser.getProcessedConfig().then(function(config) {
            // you could use other properties here if you want, such as platform and version
            var browserName = config.capabilities.browserName;

            var junitReporter = new jasmineReporters.JUnitXmlReporter({
                consolidateAll: true,
                savePath: 'testresults',
                // this will produce distinct xml files for each capability
                filePrefix: browserName + '-xmloutput',
                modifySuiteName: function (generatedSuiteName, suite) {
                    // this will produce distinct suite names for each capability,
                    // e.g. 'firefox.login tests' and 'chrome.login tests'
                    return browserName + '.' + generatedSuiteName;
                }
            });
            jasmine.getEnv().addReporter(junitReporter);
            jasmine.getEnv().addReporter(
                new Jasmine2HtmlReporter({
                    savePath: 'target/reports/'
                })
            );
        });
    },
    allScriptsTimeout: 25000
};
