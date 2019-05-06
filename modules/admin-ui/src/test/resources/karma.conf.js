module.exports = function (config) {
    config.set({
        basePath : './',

        files : [
            // bower:js
            '../../../bower_components/jquery/jquery.js',
            '../../../bower_components/jquery-ui/jquery-ui.js',
            '../../../bower_components/angular/angular.js',
            '../../../bower_components/angular-route/angular-route.js',
            '../../../bower_components/angular-resource/angular-resource.js',
            '../../../bower_components/angular-animate/angular-animate.js',
            '../../../bower_components/angular-messages/angular-messages.js',
            '../../../bower_components/angular-translate/angular-translate.js',
            '../../../bower_components/angular-translate-loader-static-files/angular-translate-loader-static-files.js',
            '../../../bower_components/angular-local-storage/dist/angular-local-storage.js',
            '../../../bower_components/angular-wizard/dist/angular-wizard.js',
            '../../../bower_components/angular-hotkeys/build/hotkeys.js',
            '../../../bower_components/angular-ui-sortable/sortable.js',
            '../../../bower_components/ng-file-upload/angular-file-upload.js',
            '../../../bower_components/chart.js/dist/Chart.js',
            '../../../bower_components/angular-chart.js/dist/angular-chart.js',
            '../../../bower_components/angular-mocks/angular-mocks.js',
            // endbower
            '../../main/webapp/scripts/lib/chosen.jquery.js',
            '../../main/webapp/scripts/lib/angular-chosen.js',
            '../../main/webapp/scripts/lib/underscore-1.5.2.js',
            '../../main/webapp/scripts/lib/video-js/video.js',
            '../../main/webapp/scripts/lib/moment-with-locales.js',
            '../../main/webapp/scripts/lib/timepicker/jquery-ui-timepicker-addon.js',

            '../../main/webapp/scripts/app.js',
            '../../main/webapp/scripts/shared/filters/filters.js',
            '../../main/webapp/scripts/shared/resources/resources.js',
            '../../main/webapp/scripts/shared/directives/directives.js',
            '../../main/webapp/scripts/shared/controllers/controllers.js',
            '../../main/webapp/scripts/shared/services/services.js',
            '../../main/webapp/scripts/modules/**/*.js',
            '../../main/webapp/scripts/shared/**/*.js',
            '../../main/webapp/**/*.html',

            'test/lib/jasmine-jquery.js',
            'test/lib/jquery-deparam.js',

            // fixtures
            {pattern: '../../main/resources/public/**/*.json', watched: true, served: true, included: false},
            {pattern: 'test/unit/fixtures/**/*.json', watched: true, served: true, included: false},
            {pattern: 'app/GET/**/*', watched: true, served: true, included: false},

            'test/unit/setup.js',
            'test/unit/**/*Helper.js',
            'test/unit/**/*Spec.js'
        ],

        autoWatch : true,

        frameworks: ['jasmine'],

        browsers : ['PhantomJS'],

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
            '../../main/webapp/scripts/shared/**/*.js': ['coverage'],
            '../../main/webapp/scripts/modules/**/*.js': ['coverage'],
            '../../main/webapp/**/*.html': ['ng-html2js']
        },

        reporters: ['progress'],

        coverageReporter: {
            type : 'html',
            dir : '../../../target/coverage/'
        },

        ngHtml2JsPreprocessor: {
            cacheIdFromPath: function (filepath) {
                var match = filepath.match(/.*src\/main\/webapp\/scripts\/(.*)/);
                if (match !== null) {
                    return match[1];
                } else {
                    console.log('no match');
                }
            }
        }
    });
};
