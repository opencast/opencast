module.exports = function (config) {
    config.set({
        basePath : './',

        files : [
            // bower:js
            '../bower_components/jquery/dist/jquery.js',
            '../bower_components/jquery-ui/jquery-ui.js',
            '../bower_components/jqueryui-timepicker-addon/dist/jquery-ui-timepicker-addon.js',
            '../bower_components/angular/angular.js',
            '../bower_components/angular-route/angular-route.js',
            '../bower_components/angular-resource/angular-resource.js',
            '../bower_components/angular-animate/angular-animate.js',
            '../bower_components/angular-messages/angular-messages.js',
            '../bower_components/angular-translate/angular-translate.js',
            '../bower_components/angular-translate-loader-static-files/angular-translate-loader-static-files.js',
            '../bower_components/angular-local-storage/dist/angular-local-storage.js',
            '../bower_components/angular-wizard/dist/angular-wizard.js',
            '../bower_components/angular-hotkeys/build/hotkeys.js',
            '../bower_components/angular-ui-sortable/sortable.js',
            '../bower_components/ng-file-upload/angular-file-upload.js',
            '../bower_components/chart.js/dist/Chart.js',
            '../bower_components/angular-chart.js/dist/angular-chart.js',
            '../bower_components/angular-mocks/angular-mocks.js',
            // endbower
      '../app/scripts/lib/chosen.jquery.js',
      '../app/scripts/lib/angular-chosen.js',
      '../app/scripts/lib/underscore-1.5.2.js',
      '../app/scripts/lib/video-js/video.js',
      '../app/scripts/lib/moment-with-locales.js',

      '../app/scripts/app.js',
      '../app/scripts/shared/filters/filters.js',
      '../app/scripts/shared/resources/resources.js',
      '../app/scripts/shared/directives/directives.js',
      '../app/scripts/shared/controllers/controllers.js',
      '../app/scripts/shared/services/services.js',
      '../app/scripts/modules/**/*.js',
      '../app/scripts/shared/**/*.js',
      '../app/**/*.html',

            'test/lib/jasmine-jquery.js',
            'test/lib/jquery-deparam.js',

            // fixtures
      {pattern: '../resources/public/**/*.json', watched: true, served: true, included: false},
      {pattern: 'app/GET/**/*', watched: true, served: true, included: false},

            'test/unit/setup.js',
            'test/unit/**/*Helper.js',
            'test/unit/**/*Spec.js'
        ],

        autoWatch : true,

        frameworks: ['jasmine'],

        browsers : ['PhantomJS'],

        customLaunchers: {
            FirefoxHeadless: {
                base: 'Firefox',
                flags: [ '-headless' ],
            },
        },

        plugins : [
            'karma-chrome-launcher',
            'karma-phantomjs-launcher',
            'karma-coverage',
            'karma-firefox-launcher',
            'karma-jasmine',
            'karma-ng-html2js-preprocessor'
        ],

        captureTimeout: 60000,
        browserDisconnectTimeout : 10000,
        browserDisconnectTolerance : 1,
        browserNoActivityTimeout : 60000, // by default 10000

        preprocessors: {
      '../app/scripts/shared/**/*.js': ['coverage'],
      '../app/scripts/modules/**/*.js': ['coverage'],
      '../app/**/*.html': ['ng-html2js']
        },

        reporters: ['progress'],

        coverageReporter: {
            type : 'html',
      dir : '../target/grunt/coverage/'
        },

        ngHtml2JsPreprocessor: {
            cacheIdFromPath: function (filepath) {
        var match = filepath.match(/.*app\/scripts\/(.*)/);
                if (match !== null) {
                    return match[1];
                } else {
                    console.log('no match');
                }
            }
        }
    });
};
